package com.never.localmusic.core

import kotlinx.coroutines.flow.StateFlow

data class TrackMeta(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
)

data class PlayerSnapshot(
    val fileName: String? = null,
    val meta: TrackMeta? = null,
    val isReady: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val status: String = "No file loaded",
    val error: String? = null,
)

/**
 * Phase 0 spike interface. Opens one user-chosen local audio file and exposes
 * basic playback state. Not the shipping player architecture yet.
 */
interface PlayerProbe {
    val snapshot: StateFlow<PlayerSnapshot>

    suspend fun open(uri: String)
    fun togglePlayPause()
    fun seekTo(ms: Long)
    suspend fun release()
}
