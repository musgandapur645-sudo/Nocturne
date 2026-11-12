package com.never.nocturne.android

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.never.nocturne.core.TrackView
import com.never.nocturne.shared.AppSettings
import com.never.nocturne.shared.HistoryEntry
import com.never.nocturne.shared.LocalPlayer
import com.never.nocturne.shared.LocalPlayerSnapshot
import com.never.nocturne.shared.PersistedUserData
import com.never.nocturne.shared.Playlist
import com.never.nocturne.shared.SettingsCodec
import com.never.nocturne.shared.UserDataCodec
import com.never.nocturne.shared.toHistoryEntry
import com.never.nocturne.shared.toPersisted
import com.never.nocturne.shared.toPlaylist
import kotlinx.coroutines.channels.Channel
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
import java.io.File
import java.util.UUID

/**
 * Phase 1 Android player.
 *
 * Queue: [_playOrder] + [_cursor] index. Matches DesktopLocalPlayer semantics.
 *
 * Persistence: playlists, favorites, history, and isHistoryPaused are stored in
 * [Context.filesDir]/user_data.json (app-private, not accessible to other apps).
 * Loaded at construction; saved asynchronously after each mutation of persisted
 * state. Corrupt or absent files start with empty state. Music files are NOT
 * stored here — SAF folder access is unchanged.
 */
class AndroidLocalPlayer(
    private val context: Context,
    private val serviceConnectionOnConnected: (MediaController) -> Unit,
    private val serviceConnectionOnDisconnected: () -> Unit,
) : LocalPlayer {

    companion object {
        private const val POSITION_POLL_INTERVAL_MS = 500L
        private const val HISTORY_MAX = 500
    }

    // ── snapshot ──────────────────────────────────────────────────────────────

    private val _snapshotFlow = MutableStateFlow(LocalPlayerSnapshot())
    val snapshotFlow: StateFlow<LocalPlayerSnapshot> = _snapshotFlow.asStateFlow()
    override val snapshot: LocalPlayerSnapshot get() = _snapshotFlow.value

    // ── scopes ────────────────────────────────────────────────────────────────

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionPollJob: Job? = null

    // ── mutable state ─────────────────────────────────────────────────────────

    private val _library:   MutableList<TrackView>  = mutableListOf()
    private val _playOrder: MutableList<TrackView>  = mutableListOf()
    private var _cursor: Int = -1

    private val _playlists: MutableList<Playlist>     = mutableListOf()
    private val _favorites: MutableSet<String>        = mutableSetOf()
    private val _history:   MutableList<HistoryEntry> = mutableListOf()

    // ── persistence file ──────────────────────────────────────────────────────

    private val userDataFile: File get() = File(context.filesDir, "user_data.json")
    private val saveQueue: Channel<PersistedUserData> = Channel(Channel.UNLIMITED)

    /** Appearance settings; see docs/customization-spec.md. */
    private val settingsFile: File get() = File(context.filesDir, "settings.json")
    private val settingsQueue: Channel<AppSettings> = Channel(Channel.UNLIMITED)

    // ── init: load persisted data ─────────────────────────────────────────────

    init {
        loadUserData()
        loadSettings()
        // Single FIFO writers for user data and settings: preserves call order
        // so the last mutation is always the last write, avoiding race conditions.
        scope.launch(Dispatchers.IO) {
            for (data in saveQueue) {
                runCatching { userDataFile.writeText(UserDataCodec.encode(data)) }
            }
        }
        scope.launch(Dispatchers.IO) {
            for (settings in settingsQueue) {
                runCatching { settingsFile.writeText(SettingsCodec.encode(settings)) }
            }
        }
    }

    // ── MediaController ───────────────────────────────────────────────────────

    private var controller: MediaController? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val ctrl = controller ?: return
            val dur = ctrl.duration.takeIf { it != C.TIME_UNSET } ?: 0L
            _snapshotFlow.update {
                it.copy(
                    isPlaying = isPlaying && it.nowPlaying != null,
                    positionMs = ctrl.currentPosition.coerceAtLeast(0L),
                    durationMs = dur.coerceAtLeast(0L),
                )
            }
            if (isPlaying) startPositionPoll() else stopPositionPoll()
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) { stopPositionPoll(); handleTrackEnd() }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Transport
    // ─────────────────────────────────────────────────────────────────────────

    override fun playSelected(view: TrackView) {
        val idx = _playOrder.indexOfFirst { it.id == view.id }
        _cursor = if (idx >= 0) idx else { _playOrder.add(view); _playOrder.lastIndex }
        startMediaItem(view)
        _snapshotFlow.update {
            it.copy(nowPlaying = view, positionMs = 0L, durationMs = view.durationMs,
                isPlaying = true, queue = queueFromCursor())
        }
    }

    override fun togglePlayPause() {
        val ctrl = controller ?: return
        ctrl.playWhenReady = !ctrl.playWhenReady
    }

    override fun seekTo(ms: Long) {
        val bounded = ms.coerceAtLeast(0L)
        controller?.seekTo(bounded)
        _snapshotFlow.update { it.copy(positionMs = bounded) }
    }

    override fun playNext() {
        if (_playOrder.isEmpty()) return
        _cursor = (_cursor + 1).coerceAtMost(_playOrder.lastIndex)
        val next = _playOrder[_cursor]
        startMediaItem(next)
        _snapshotFlow.update {
            it.copy(nowPlaying = next, positionMs = 0L, durationMs = next.durationMs,
                isPlaying = true, queue = queueFromCursor())
        }
    }

    override fun playPrevious() {
        if (_playOrder.isEmpty()) return
        _cursor = (_cursor - 1).coerceAtLeast(0)
        val prev = _playOrder[_cursor]
        startMediaItem(prev)
        _snapshotFlow.update {
            it.copy(nowPlaying = prev, positionMs = 0L, durationMs = prev.durationMs,
                isPlaying = true, queue = queueFromCursor())
        }
    }

    override fun skipForward(ms: Long): Long {
        val dur = _snapshotFlow.value.durationMs
        if (dur <= 0L) return _snapshotFlow.value.positionMs   // duration unknown — do nothing
        val next = (_snapshotFlow.value.positionMs + ms).coerceAtMost(dur)
        seekTo(next); return next
    }

    override fun skipBackward(ms: Long): Long {
        val next = (_snapshotFlow.value.positionMs - ms).coerceAtLeast(0L)
        seekTo(next); return next
    }

    override fun toggleShuffle() {
        val current = _snapshotFlow.value.nowPlaying
        val shuffleOn = !_snapshotFlow.value.isShuffle
        if (shuffleOn) _playOrder.shuffle() else { _playOrder.clear(); _playOrder.addAll(_library) }
        _cursor = if (current != null)
            _playOrder.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
        else 0
        controller?.shuffleModeEnabled = shuffleOn
        _snapshotFlow.update { it.copy(isShuffle = shuffleOn, queue = queueFromCursor()) }
    }

    override fun toggleRepeat() {
        val current = controller?.repeatMode ?: Player.REPEAT_MODE_OFF
        val next = when (current) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller?.repeatMode = next
        _snapshotFlow.update { it.copy(isRepeat = next != Player.REPEAT_MODE_OFF) }
    }

    override fun clearQueue() {
        if (_cursor < _playOrder.lastIndex) {
            val keep = _playOrder.subList(0, _cursor + 1).toList()
            _playOrder.clear(); _playOrder.addAll(keep)
        }
        _snapshotFlow.update { it.copy(queue = emptyList()) }
    }

    override fun addToQueue(track: TrackView) {
        _playOrder.add(track)
        _snapshotFlow.update { it.copy(queue = queueFromCursor()) }
    }

    override fun removeFromQueue(index: Int) {
        val target = _cursor + 1 + index
        if (target in (_cursor + 1).._playOrder.lastIndex) {
            _playOrder.removeAt(target)
            _snapshotFlow.update { it.copy(queue = queueFromCursor()) }
        }
    }

    override fun openMusicFolder() {}   // SAF picker owned by MainActivity
    override fun onOpenFolderRequested() = openMusicFolder()

    override fun updateSelectedFolder(folder: File?) {
        _library.clear(); _playOrder.clear(); _cursor = -1
        _snapshotFlow.update {
            it.copy(library = emptyList(), filteredLibrary = emptyList(),
                queue = emptyList(), scanError = null)
        }
    }

    override fun addToLibrary(track: TrackView) {
        _library.add(track); _playOrder.add(track)
        _snapshotFlow.update { snap ->
            snap.copy(
                library = _library.toList(),
                filteredLibrary = applyFilter(_library, snap.searchQuery),
            )
        }
    }

    fun setScanError(error: String?) { _snapshotFlow.update { it.copy(scanError = error) } }

    // ─────────────────────────────────────────────────────────────────────────
    // Search
    // ─────────────────────────────────────────────────────────────────────────

    override fun search(query: String) {
        _snapshotFlow.update {
            it.copy(searchQuery = query, filteredLibrary = applyFilter(_library, query))
        }
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
        _snapshotFlow.update { it.copy(playlists = _playlists.toList()) }
        saveUserData()
    }

    override fun deletePlaylist(id: String) {
        _playlists.removeAll { it.id == id }
        _snapshotFlow.update { it.copy(playlists = _playlists.toList()) }
        saveUserData()
    }

    override fun addToPlaylist(playlistId: String, track: TrackView) {
        val idx = _playlists.indexOfFirst { it.id == playlistId }; if (idx < 0) return
        val pl = _playlists[idx]
        if (pl.tracks.any { it.id == track.id }) return
        _playlists[idx] = pl.copy(tracks = pl.tracks + track)
        _snapshotFlow.update { it.copy(playlists = _playlists.toList()) }
        saveUserData()
    }

    override fun removeFromPlaylist(playlistId: String, trackId: String) {
        val idx = _playlists.indexOfFirst { it.id == playlistId }; if (idx < 0) return
        _playlists[idx] = _playlists[idx].copy(tracks = _playlists[idx].tracks.filter { it.id != trackId })
        _snapshotFlow.update { it.copy(playlists = _playlists.toList()) }
        saveUserData()
    }

    override fun playPlaylist(playlistId: String) {
        val pl = _playlists.firstOrNull { it.id == playlistId } ?: return
        if (pl.tracks.isEmpty()) return
        _playOrder.clear()
        _playOrder.addAll(if (_snapshotFlow.value.isShuffle) pl.tracks.shuffled() else pl.tracks)
        _cursor = 0
        val first = _playOrder[0]
        startMediaItem(first)
        _snapshotFlow.update {
            it.copy(nowPlaying = first, positionMs = 0L, durationMs = first.durationMs,
                isPlaying = true, queue = queueFromCursor())
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Favorites
    // ─────────────────────────────────────────────────────────────────────────

    override fun toggleFavorite(trackId: String) {
        if (!_favorites.add(trackId)) _favorites.remove(trackId)
        _snapshotFlow.update { it.copy(favorites = _favorites.toSet()) }
        saveUserData()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // History
    // ─────────────────────────────────────────────────────────────────────────

    override fun pauseHistory() {
        _snapshotFlow.update { it.copy(isHistoryPaused = true) }
        saveUserData()
    }

    override fun resumeHistory() {
        _snapshotFlow.update { it.copy(isHistoryPaused = false) }
        saveUserData()
    }

    override fun clearHistory() {
        _history.clear()
        _snapshotFlow.update { it.copy(history = emptyList()) }
        saveUserData()
    }

    override fun setNowPlaying(view: TrackView?) { _snapshotFlow.update { it.copy(nowPlaying = view) } }

    // ─────────────────────────────────────────────────────────────────────────
    // Appearance settings
    // ─────────────────────────────────────────────────────────────────────────

    override fun updateSettings(settings: AppSettings) {
        val next = settings.normalized()
        _snapshotFlow.update { it.copy(settings = next) }
        saveSettings(next)
    }

    override fun exportSettingsJson(): String = SettingsCodec.encode(snapshot.settings)

    override fun importSettingsJson(text: String): String? {
        val parsed = SettingsCodec.decode(text)
            ?: return SettingsCodec.decodeError(text)
        updateSettings(parsed)
        return null
    }

    private fun loadSettings() {
        runCatching {
            val text = settingsFile.takeIf { it.exists() }?.readText() ?: return
            val parsed = SettingsCodec.decode(text) ?: return
            _snapshotFlow.update { it.copy(settings = parsed) }
        }
        // Absent or corrupt file → keep the defaults. No crash.
    }

    private fun saveSettings(settings: AppSettings) {
        settingsQueue.trySend(settings) // single FIFO writer drains in call order
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Controller lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    fun attachController(controller: MediaController) {
        this.controller = controller
        controller.addListener(playerListener)
        serviceConnectionOnConnected(controller)
        val mediaItem = controller.currentMediaItem ?: return
        val dur = controller.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L
        val trackId = mediaItem.mediaId.ifBlank { mediaItem.localConfiguration?.uri?.toString() ?: "" }
        // A SAF content URI's tail is percent-encoded ("primary%3AMusic%2F..."), and the
        // library rescan may not have replaced it yet on reopen. Keep the title already
        // known for this track; only build a fallback from the decoded URI when unknown.
        val title = _snapshotFlow.value.nowPlaying
            ?.takeIf { it.id == trackId }?.title
            ?: trackId.substringAfterLast('/')
                .let { runCatching { java.net.URLDecoder.decode(it.replace("+", "%2B"), java.nio.charset.StandardCharsets.UTF_8.name()) }.getOrDefault(it) }
                .substringAfterLast('/')                     // decoded %2F can hide a slash
                .substringBeforeLast('.').ifBlank { "Track" }
        _snapshotFlow.update {
            it.copy(
                nowPlaying = TrackView(id = trackId,
                    title = title),
                positionMs = controller.currentPosition.coerceAtLeast(0L),
                durationMs = dur,
                isPlaying  = controller.isPlaying,
                isShuffle  = controller.shuffleModeEnabled,
                isRepeat   = controller.repeatMode != Player.REPEAT_MODE_OFF,
            )
        }
        if (controller.isPlaying) startPositionPoll()
    }

    fun detachController() {
        stopPositionPoll()
        controller?.removeListener(playerListener)
        serviceConnectionOnDisconnected()
        controller = null
    }

    fun stop() { scope.cancel(); detachController() }

    // ─────────────────────────────────────────────────────────────────────────
    // Private
    // ─────────────────────────────────────────────────────────────────────────

    private fun startMediaItem(view: TrackView) {
        val item = MediaItem.Builder().setUri(Uri.parse(view.id)).setMediaId(view.id).build()
        controller?.apply { setMediaItem(item); prepare(); playWhenReady = true }
        recordHistoryPlay(view)
    }

    private fun handleTrackEnd() {
        val snap = _snapshotFlow.value
        val nowPlaying = snap.nowPlaying
        if (snap.isRepeat && nowPlaying != null) { startMediaItem(nowPlaying); return }
        if (_cursor < _playOrder.lastIndex) {
            _cursor++
            val next = _playOrder[_cursor]
            startMediaItem(next)
            _snapshotFlow.update {
                it.copy(nowPlaying = next, positionMs = 0L, durationMs = next.durationMs,
                    isPlaying = true, queue = queueFromCursor())
            }
        } else {
            _snapshotFlow.update { it.copy(isPlaying = false, positionMs = 0L) }
        }
    }

    private fun startPositionPoll() {
        positionPollJob?.cancel()
        positionPollJob = scope.launch {
            while (isActive) {
                delay(POSITION_POLL_INTERVAL_MS)
                val ctrl = controller ?: break
                if (!ctrl.isPlaying) break
                val pos = ctrl.currentPosition.coerceAtLeast(0L)
                val dur = ctrl.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L
                _snapshotFlow.update { it.copy(positionMs = pos, durationMs = dur) }
            }
        }
    }

    private fun stopPositionPoll() { positionPollJob?.cancel(); positionPollJob = null }

    private fun recordHistoryPlay(view: TrackView) {
        if (_snapshotFlow.value.isHistoryPaused) return
        _history.add(0, HistoryEntry(trackId = view.id, title = view.title, artist = view.artist,
            playedAtMs = System.currentTimeMillis()))
        if (_history.size > HISTORY_MAX) _history.removeAt(_history.lastIndex)
        _snapshotFlow.update { it.copy(history = _history.toList()) }
        saveUserData()
    }

    private fun queueFromCursor(): List<TrackView> {
        if (_cursor < 0 || _cursor >= _playOrder.size) return emptyList()
        return _playOrder.subList((_cursor + 1).coerceAtMost(_playOrder.size), _playOrder.size).toList()
    }

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
            _snapshotFlow.update {
                it.copy(
                    playlists = _playlists.toList(),
                    favorites = _favorites.toSet(),
                    history   = _history.toList(),
                    isHistoryPaused = data.isHistoryPaused,
                )
            }
        }
        // Missing or corrupt file → silent empty state.
    }

    private fun saveUserData() {
        // Snapshot mutable state on the caller's thread before enqueuing.
        val data = PersistedUserData(
            playlists = _playlists.map { it.toPersisted() },
            favorites = _favorites.toList(),
            history   = _history.map { it.toPersisted() },
            isHistoryPaused = _snapshotFlow.value.isHistoryPaused,
        )
        // Enqueue to the single FIFO writer instead of firing an independent
        // Dispatchers.IO write, eliminating race conditions.
        saveQueue.trySend(data)
    }
}
