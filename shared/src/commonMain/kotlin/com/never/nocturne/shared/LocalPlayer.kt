package com.never.localmusic.shared

import com.never.localmusic.core.TrackView

/**
 * Phase 1 shared player interface.
 *
 * Covers local folder access, transport controls (play/pause/seek/next/prev/
 * shuffle/repeat/skip), queue, library search, playlists, favorites, and
 * on-device listening history.
 *
 * Platform implementations live in desktopMain and androidMain. The shared
 * data types used here (TrackView, Playlist, HistoryEntry) are also in
 * commonMain so the UI layer can depend only on shared.
 */
interface LocalPlayer {
    val snapshot: LocalPlayerSnapshot

    // ---- transport ----
    fun playSelected(view: TrackView)
    fun togglePlayPause()
    fun seekTo(ms: Long)
    fun playNext()
    fun playPrevious()
    fun skipForward(ms: Long): Long
    fun skipBackward(ms: Long): Long

    // ---- queue / mode ----
    fun toggleShuffle()
    fun toggleRepeat()
    fun clearQueue()
    fun addToQueue(track: TrackView)
    fun removeFromQueue(index: Int)

    // ---- library / folder ----
    fun openMusicFolder()
    fun updateSelectedFolder(folder: java.io.File?)
    fun addToLibrary(track: TrackView)

    // ---- search ----
    /** Filter the library by [query]; empty string returns all tracks. */
    fun search(query: String)

    // ---- playlists ----
    fun createPlaylist(name: String)
    fun deletePlaylist(id: String)
    fun addToPlaylist(playlistId: String, track: TrackView)
    fun removeFromPlaylist(playlistId: String, trackId: String)
    fun playPlaylist(playlistId: String)

    // ---- favorites ----
    fun toggleFavorite(trackId: String)

    // ---- history ----
    fun pauseHistory()
    fun resumeHistory()
    fun clearHistory()

    // ---- appearance ----
    /** Apply (and persist) a whole settings object. */
    fun updateSettings(settings: AppSettings)
    /** Current settings serialized as the documented JSON, for export. */
    fun exportSettingsJson(): String
    /** Replace settings from JSON text; null on success, else a reason. */
    fun importSettingsJson(text: String): String?

    // ---- misc ----
    fun onOpenFolderRequested()
    fun setNowPlaying(view: TrackView?)
}

data class Playlist(
    val id: String,
    val name: String,
    val tracks: List<TrackView> = emptyList(),
)

data class HistoryEntry(
    val trackId: String,
    val title: String,
    val artist: String?,
    /** Unix epoch milliseconds when this play was recorded. */
    val playedAtMs: Long,
)

data class LocalPlayerSnapshot(
    val nowPlaying: TrackView? = null,
    val library: List<TrackView> = emptyList(),
    /** Tracks visible after applying the current search query. */
    val filteredLibrary: List<TrackView> = emptyList(),
    val searchQuery: String = "",
    val queue: List<TrackView> = emptyList(),
    val isPlaying: Boolean = false,
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    /** Non-null when a folder scan failed or was partially aborted. */
    val scanError: String? = null,
    /** Non-null when the last playback attempt failed. */
    val playbackError: String? = null,
    /** All defined playlists. */
    val playlists: List<Playlist> = emptyList(),
    /** IDs of tracks the user has marked as favorites. */
    val favorites: Set<String> = emptySet(),
    /** Most-recent plays, newest first. */
    val history: List<HistoryEntry> = emptyList(),
    /** True while history recording is paused. */
    val isHistoryPaused: Boolean = false,
    /** Appearance: preset, accent override, glass intensity. */
    val settings: AppSettings = AppSettings(),
)
