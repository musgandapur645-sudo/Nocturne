package com.never.localmusic.desktop

import com.never.localmusic.core.TrackView
import com.never.localmusic.shared.HistoryEntry
import com.never.localmusic.shared.LocalPlayer
import com.never.localmusic.shared.LocalPlayerSnapshot
import com.never.localmusic.shared.PersistedUserData
import com.never.localmusic.shared.Playlist
import com.never.localmusic.shared.UserDataCodec
import com.never.localmusic.shared.toHistoryEntry
import com.never.localmusic.shared.toPersisted
import com.never.localmusic.shared.toPlaylist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent

/**
 * Phase 1 desktop player.
 *
 * Queue: [_playOrder] list + [_cursor] index. [playSelected] sets cursor.
 * [playNext]/[playPrevious] advance cursor. [toggleShuffle] re-orders in-place.
 * New folder rebuilds from scratch.
 *
 * Position: 500 ms coroutine reads [Clip.microsecondPosition].
 *
 * Scan: max 5 000 items / 30 s. Over-limit sets [scanError].
 *
 * Metadata: jaudiotagger for tags; filename fallback on error.
 *
 * Persistence: playlists, favorites, history, history-paused stored in
 * ~/.local/share/local-music-player/user_data.json (XDG_DATA_HOME honoured).
 * Loaded at startup; saved after every mutation of persisted state.
 * Corrupt or absent file silently starts with empty state.
 */
class DesktopLocalPlayer : LocalPlayer {

    init {
        Logger.getLogger("org.jaudiotagger").level = Level.WARNING
    }

    companion object {
        private const val SCAN_MAX_ITEMS = 5_000
        private const val SCAN_MAX_MS = 30_000L
        private const val POSITION_POLL_INTERVAL_MS = 500L
        private const val HISTORY_MAX = 500

        /** XDG data dir (~/.local/share) or fallback. */
        private val storageDir: File by lazy {
            val xdg = System.getenv("XDG_DATA_HOME")
            val base = if (!xdg.isNullOrBlank()) File(xdg)
                       else File(System.getProperty("user.home"), ".local/share")
            File(base, "local-music-player").also { it.mkdirs() }
        }

        private val userDataFile: File get() = File(storageDir, "user_data.json")

        /** Embedded cover art is copied here so the UI can load it as a plain file. */
        private val artCacheDir: File by lazy {
            File(storageDir, "art_cache").also { it.mkdirs() }
        }
    }

    // ── scope ────────────────────────────────────────────────────────────────

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var positionPollJob: Job? = null

    // ── snapshot  (must be declared before init block below) ─────────────────

    private val _flow = MutableStateFlow(LocalPlayerSnapshot())
    val snapshotFlow: StateFlow<LocalPlayerSnapshot> = _flow.asStateFlow()
    override val snapshot: LocalPlayerSnapshot get() = _flow.value

    // ── mutable state ─────────────────────────────────────────────────────────

    @Volatile private var currentClip: Clip? = null
    @Volatile private var isPaused: Boolean = false
    /** Incremented each time a new folder scan is launched. The scan coroutine
     *  captures this at start and drops its results if the value has changed
     *  by the time it tries to commit — so a stale scan cannot overwrite a
     *  more-recent folder selection. */
    @Volatile private var scanGeneration: Int = 0

    private val _library:   MutableList<TrackView>  = mutableListOf()
    private val _playOrder: MutableList<TrackView>  = mutableListOf()
    private var _cursor: Int = -1

    private val _playlists: MutableList<Playlist>     = mutableListOf()
    private val _favorites: MutableSet<String>        = mutableSetOf()
    private val _history:   MutableList<HistoryEntry> = mutableListOf()

    // ── init — load persisted data AFTER all fields are initialised ──────────

    init {
        loadUserData()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Transport
    // ─────────────────────────────────────────────────────────────────────────

    override fun playSelected(view: TrackView) {
        val idx = _playOrder.indexOfFirst { it.id == view.id }
        _cursor = if (idx >= 0) idx else { _playOrder.add(view); _playOrder.lastIndex }
        isPaused = false
        startClipFor(view)
    }

    override fun togglePlayPause() {
        val clip = currentClip ?: return
        if (clip.isRunning) {
            // Set isPaused BEFORE stopping the clip. The STOP event fires on
            // the JavaSound EventDispatcher thread; setting the flag first
            // ensures the listener sees isPaused=true and does not advance
            // the queue as if the track ended naturally.
            isPaused = true; clip.stop(); stopPositionPoll()
            emit { it.copy(isPlaying = false) }
        } else if (isPaused) {
            clip.start(); isPaused = false; startPositionPoll()
            emit { it.copy(isPlaying = true) }
        }
    }

    override fun seekTo(ms: Long) {
        val bounded = ms.coerceIn(0L, _flow.value.durationMs)
        currentClip?.microsecondPosition = bounded * 1_000L
        emit { it.copy(positionMs = bounded) }
    }

    override fun skipForward(ms: Long): Long {
        val next = (_flow.value.positionMs + ms).coerceAtMost(_flow.value.durationMs)
        seekTo(next); return next
    }

    override fun skipBackward(ms: Long): Long {
        val next = (_flow.value.positionMs - ms).coerceAtLeast(0L)
        seekTo(next); return next
    }

    override fun playNext() {
        if (_playOrder.isEmpty()) return
        _cursor = (_cursor + 1).coerceAtMost(_playOrder.lastIndex)
        val next = _playOrder[_cursor]
        emit { it.copy(nowPlaying = next, queue = queueFromCursor()) }
        startClipFor(next)
    }

    override fun playPrevious() {
        if (_playOrder.isEmpty()) return
        _cursor = (_cursor - 1).coerceAtLeast(0)
        val prev = _playOrder[_cursor]
        emit { it.copy(nowPlaying = prev, queue = queueFromCursor()) }
        startClipFor(prev)
    }

    override fun toggleShuffle() {
        val current = _flow.value.nowPlaying
        val on = !_flow.value.isShuffle
        if (on) _playOrder.shuffle() else { _playOrder.clear(); _playOrder.addAll(_library) }
        _cursor = if (current != null)
            _playOrder.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
        else 0
        emit { it.copy(isShuffle = on, queue = queueFromCursor()) }
    }

    override fun toggleRepeat() { emit { it.copy(isRepeat = !it.isRepeat) } }

    override fun clearQueue() {
        if (_cursor < _playOrder.lastIndex) {
            val keep = _playOrder.subList(0, _cursor + 1).toList()
            _playOrder.clear(); _playOrder.addAll(keep)
        }
        emit { it.copy(queue = emptyList()) }
    }

    override fun setNowPlaying(view: TrackView?) { emit { it.copy(nowPlaying = view) } }

    // ─────────────────────────────────────────────────────────────────────────
    // Folder / library
    // ─────────────────────────────────────────────────────────────────────────

    override fun openMusicFolder() {
        DesktopMain.chooseFolder?.invoke()?.let { updateSelectedFolder(it) }
    }

    override fun onOpenFolderRequested() = openMusicFolder()

    override fun updateSelectedFolder(folder: File?) {
        stopAndRelease()
        _library.clear(); _playOrder.clear(); _cursor = -1
        val gen = ++scanGeneration   // invalidates any in-flight scan
        emit {
            LocalPlayerSnapshot(
                isShuffle = it.isShuffle, isRepeat = it.isRepeat,
                playlists = it.playlists, favorites = it.favorites,
                history = it.history, isHistoryPaused = it.isHistoryPaused,
            )
        }
        if (folder != null && folder.isDirectory && folder.canRead()) {
            scope.launch(Dispatchers.Default) { indexFolder(folder, gen) }
        }
    }

    override fun addToLibrary(track: TrackView) {
        _library.add(track); _playOrder.add(track)
        emit { snap ->
            snap.copy(
                library = _library.toList(),
                filteredLibrary = applyFilter(_library, snap.searchQuery),
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Search
    // ─────────────────────────────────────────────────────────────────────────

    override fun search(query: String) {
        emit { it.copy(searchQuery = query, filteredLibrary = applyFilter(_library, query)) }
    }

    private fun applyFilter(lib: List<TrackView>, query: String): List<TrackView> {
        if (query.isBlank()) return lib
        val q = query.trim().lowercase()
        return lib.filter { t ->
            t.title.lowercase().contains(q) ||
                t.artist?.lowercase()?.contains(q) == true ||
                t.album?.lowercase()?.contains(q) == true
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Playlists
    // ─────────────────────────────────────────────────────────────────────────

    override fun createPlaylist(name: String) {
        _playlists.add(Playlist(id = UUID.randomUUID().toString(), name = name.trim()))
        emit { it.copy(playlists = _playlists.toList()) }
        saveUserData()
    }

    override fun deletePlaylist(id: String) {
        _playlists.removeAll { it.id == id }
        emit { it.copy(playlists = _playlists.toList()) }
        saveUserData()
    }

    override fun addToPlaylist(playlistId: String, track: TrackView) {
        val idx = _playlists.indexOfFirst { it.id == playlistId }; if (idx < 0) return
        val pl = _playlists[idx]
        if (pl.tracks.any { it.id == track.id }) return
        _playlists[idx] = pl.copy(tracks = pl.tracks + track)
        emit { it.copy(playlists = _playlists.toList()) }
        saveUserData()
    }

    override fun removeFromPlaylist(playlistId: String, trackId: String) {
        val idx = _playlists.indexOfFirst { it.id == playlistId }; if (idx < 0) return
        _playlists[idx] = _playlists[idx].copy(tracks = _playlists[idx].tracks.filter { it.id != trackId })
        emit { it.copy(playlists = _playlists.toList()) }
        saveUserData()
    }

    override fun playPlaylist(playlistId: String) {
        val pl = _playlists.firstOrNull { it.id == playlistId } ?: return
        if (pl.tracks.isEmpty()) return
        _playOrder.clear()
        _playOrder.addAll(if (_flow.value.isShuffle) pl.tracks.shuffled() else pl.tracks)
        _cursor = 0
        val first = _playOrder[0]
        emit { it.copy(nowPlaying = first, queue = queueFromCursor()) }
        startClipFor(first)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Favorites
    // ─────────────────────────────────────────────────────────────────────────

    override fun toggleFavorite(trackId: String) {
        if (!_favorites.add(trackId)) _favorites.remove(trackId)
        emit { it.copy(favorites = _favorites.toSet()) }
        saveUserData()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // History
    // ─────────────────────────────────────────────────────────────────────────

    override fun pauseHistory() {
        emit { it.copy(isHistoryPaused = true) }
        saveUserData()
    }

    override fun resumeHistory() {
        emit { it.copy(isHistoryPaused = false) }
        saveUserData()
    }

    override fun clearHistory() {
        _history.clear()
        emit { it.copy(history = emptyList()) }
        saveUserData()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private — indexing
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun indexFolder(dir: File, gen: Int) {
        val deadline = System.currentTimeMillis() + SCAN_MAX_MS
        val count = intArrayOf(0)
        // Heavy I/O stays on the Default dispatcher.
        val found = mutableListOf<TrackView>()
        val error = scanDirectory(dir, 0, deadline, count, found)
        // Commit only on the Main dispatcher so _library/_playOrder are always
        // mutated on the same thread as all other UI-initiated mutations.
        // Drop results if a newer folder was selected while we were scanning.
        withContext(Dispatchers.Main) {
            if (scanGeneration != gen) return@withContext   // stale — discard
            _library.clear(); _library.addAll(found)
            _playOrder.clear()
            _playOrder.addAll(
                if (_flow.value.isShuffle) _library.shuffled() else _library.toList()
            )
            emit { snap ->
                snap.copy(
                    library = _library.toList(),
                    filteredLibrary = applyFilter(_library, snap.searchQuery),
                    scanError = error,
                )
            }
        }
    }

    private fun scanDirectory(
        dir: File, depth: Int, deadline: Long, count: IntArray, out: MutableList<TrackView>
    ): String? {
        if (depth > 10) return null
        val entries = dir.listFiles() ?: return null
        for (entry in entries) {
            if (System.currentTimeMillis() > deadline)
                return "Scan stopped after ${SCAN_MAX_MS / 1_000}s — ${count[0]} tracks found so far"
            if (count[0] >= SCAN_MAX_ITEMS)
                return "Scan stopped at $SCAN_MAX_ITEMS tracks — folder may contain more"
            when {
                entry.isDirectory -> scanDirectory(entry, depth + 1, deadline, count, out)?.let { return it }
                entry.isFile && isAudioFile(entry) -> { out.add(readTrack(entry)); count[0]++ }
            }
        }
        return null
    }

    private fun isAudioFile(file: File) =
        file.extension.lowercase() in setOf("mp3","ogg","wav","aiff","aif","au")

    private fun readTrack(file: File): TrackView = runCatching {
        val af = AudioFileIO.read(file)
        val tag = af.tag; val hdr = af.audioHeader

        var artPath: String? = null
        val artwork = tag?.firstArtwork
        val artBytes = artwork?.binaryData
        if (artBytes != null && artBytes.isNotEmpty()) {
            runCatching {
                val artHash = (file.absolutePath + file.lastModified()).hashCode().toString()
                val artFile = File(artCacheDir, "$artHash.jpg")
                if (!artFile.exists() || artFile.length() == 0L) {
                    artFile.writeBytes(artBytes)
                }
                artPath = artFile.absolutePath
            }
        }

        TrackView(
            id = file.absolutePath,
            title  = tag?.getFirst(FieldKey.TITLE)?.takeIf  { it.isNotBlank() } ?: file.nameWithoutExtension,
            artist = tag?.getFirst(FieldKey.ARTIST)?.takeIf { it.isNotBlank() },
            album  = tag?.getFirst(FieldKey.ALBUM)?.takeIf  { it.isNotBlank() },
            durationMs = hdr.trackLength.toLong() * 1_000L,
            artPath = artPath,
        )
    }.getOrElse { TrackView(id = file.absolutePath, title = file.nameWithoutExtension.ifBlank { file.name }) }

    // ─────────────────────────────────────────────────────────────────────────
    // Private — playback
    // ─────────────────────────────────────────────────────────────────────────

    private fun startClipFor(view: TrackView) {
        stopAndRelease()
        val file = File(view.id)
        if (!file.isFile || !file.canRead()) {
            emit {
                it.copy(
                    nowPlaying = null,
                    isPlaying = false,
                    playbackError = "Cannot play '${file.name}': file is missing or unreadable",
                )
            }
            return
        }
        var clip: Clip? = null
        runCatching {
            val newClip = AudioSystem.getClip()
            clip = newClip
            newClip.addLineListener { event ->
                when (event.type) {
                    LineEvent.Type.STOP -> {
                        // All checks that use only @Volatile fields or the
                        // captured clip can run here on the JavaSound
                        // EventDispatcher thread as a fast pre-filter.
                        if (newClip !== currentClip) return@addLineListener
                        val dur = _flow.value.durationMs
                        val pos = newClip.microsecondPosition / 1_000L
                        if (isPaused || dur <= 0 || pos < dur - 200L) return@addLineListener
                        // Looks like natural completion. Dispatch to the Swing
                        // EDT so handleTrackEnd() can safely read and mutate
                        // _playOrder, _cursor, _history, and related state.
                        // Re-check identity and pause on the EDT: the user
                        // could have paused or replaced the clip in the window
                        // between this dispatch and its execution.
                        scope.launch(Dispatchers.Main) {
                            if (newClip !== currentClip || isPaused) return@launch
                            handleTrackEnd()
                        }
                    }
                    LineEvent.Type.CLOSE -> if (newClip === currentClip) currentClip = null
                    else -> {}
                }
            }
            AudioSystem.getAudioInputStream(file).use { encoded ->
                val format = encoded.format
                val durationMs = encoded.frameLength.takeIf { it > 0 }
                    ?.let { frames -> format.frameRate.takeIf { it > 0f }?.let { rate -> (frames / rate * 1_000L).toLong() } }
                    ?: 0L
                val pcm = if (format.encoding == AudioFormat.Encoding.PCM_SIGNED &&
                    format.sampleSizeInBits == 16 && format.frameSize == format.channels * 2) {
                    encoded
                } else {
                    val sampleRate = format.sampleRate.takeIf { it > 0f } ?: format.frameRate
                    require(sampleRate > 0f && format.channels > 0) { "Audio format cannot be decoded to PCM" }
                    val pcmFormat = AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        sampleRate,
                        16,
                        format.channels,
                        format.channels * 2,
                        sampleRate,
                        false,
                    )
                    AudioSystem.getAudioInputStream(pcmFormat, encoded)
                }
                if (pcm === encoded) newClip.open(encoded)
                else pcm.use { newClip.open(it) }
                currentClip = newClip; isPaused = false
                newClip.start()
                recordHistoryPlay(view)
                emit { it.copy(nowPlaying = view, isPlaying = true, positionMs = 0L, durationMs = durationMs, queue = queueFromCursor(), playbackError = null) }
            }
            startPositionPoll()
        }.onFailure {
            clip?.runCatching { close() }
            currentClip = null
            emit {
                it.copy(
                    nowPlaying = null,
                    isPlaying = false,
                    playbackError = "Cannot play '${file.name}': unsupported format, damaged file, or unavailable audio output",
                )
            }
        }
    }

    private fun handleTrackEnd() {
        stopPositionPoll()
        val snap = _flow.value
        val nowPlaying = snap.nowPlaying   // local val for smart cast
        if (snap.isRepeat && nowPlaying != null) { startClipFor(nowPlaying); return }
        if (_cursor < _playOrder.lastIndex) {
            _cursor++
            val next = _playOrder[_cursor]
            emit { it.copy(nowPlaying = next, queue = queueFromCursor()) }
            startClipFor(next)
        } else {
            emit { it.copy(isPlaying = false, positionMs = 0L) }
        }
    }

    private fun stopAndRelease() {
        stopPositionPoll()
        // Null currentClip BEFORE calling stop(). The JavaSound EventDispatcher
        // thread fires STOP events asynchronously; by clearing the field first,
        // the identity check in the listener (clip !== currentClip) fails for
        // any in-flight event from the old clip, so intentional stops never
        // advance the queue.
        val clip = currentClip
        currentClip = null
        isPaused = false
        clip?.runCatching { if (isRunning) stop(); close() }
        emit { it.copy(isPlaying = false) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private — position polling
    // ─────────────────────────────────────────────────────────────────────────

    private fun startPositionPoll() {
        positionPollJob?.cancel()
        positionPollJob = scope.launch {
            while (isActive) {
                delay(POSITION_POLL_INTERVAL_MS)
                val clip = currentClip ?: break
                if (!clip.isRunning) break
                emit { it.copy(positionMs = clip.microsecondPosition / 1_000L) }
            }
        }
    }

    private fun stopPositionPoll() { positionPollJob?.cancel(); positionPollJob = null }

    // ─────────────────────────────────────────────────────────────────────────
    // Private — history
    // ─────────────────────────────────────────────────────────────────────────

    private fun recordHistoryPlay(view: TrackView) {
        if (_flow.value.isHistoryPaused) return
        _history.add(0, HistoryEntry(trackId = view.id, title = view.title, artist = view.artist,
            playedAtMs = System.currentTimeMillis()))
        if (_history.size > HISTORY_MAX) _history.removeAt(_history.lastIndex)
        emit { it.copy(history = _history.toList()) }
        saveUserData()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private — helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun queueFromCursor(): List<TrackView> {
        if (_cursor < 0 || _cursor >= _playOrder.size) return emptyList()
        return _playOrder.subList((_cursor + 1).coerceAtMost(_playOrder.size), _playOrder.size).toList()
    }

    private inline fun emit(crossinline block: (LocalPlayerSnapshot) -> LocalPlayerSnapshot) =
        _flow.update { block(it) }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    fun dispose() { stopAndRelease(); scope.cancel() }

    // ─────────────────────────────────────────────────────────────────────────
    // Persistence
    // ─────────────────────────────────────────────────────────────────────────

    private fun loadUserData() {
        runCatching {
            val text = userDataFile.takeIf { it.exists() }?.readText() ?: return
            val data = UserDataCodec.decode(text)
            _playlists.clear();  _playlists.addAll(data.playlists.map { it.toPlaylist() })
            _favorites.clear();  _favorites.addAll(data.favorites)
            _history.clear();    _history.addAll(data.history.map { it.toHistoryEntry() })
            emit {
                it.copy(
                    playlists = _playlists.toList(),
                    favorites = _favorites.toSet(),
                    history   = _history.toList(),
                    isHistoryPaused = data.isHistoryPaused,
                )
            }
        }
        // Absent or corrupt file → silent empty state. No crash.
    }

    private fun saveUserData() {
        // Snapshot mutable state immediately (on the caller's thread) so the
        // coroutine writes a consistent view even if more mutations arrive quickly.
        val data = PersistedUserData(
            playlists = _playlists.map { it.toPersisted() },
            favorites = _favorites.toList(),
            history   = _history.map { it.toPersisted() },
            isHistoryPaused = _flow.value.isHistoryPaused,
        )
        scope.launch(Dispatchers.IO) {
            runCatching { userDataFile.writeText(UserDataCodec.encode(data)) }
        }
    }
}
