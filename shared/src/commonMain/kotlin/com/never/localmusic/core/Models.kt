package com.never.localmusic.core

/**
 * Stable local identity for a single track across the app and the index.
 *
 * For local files this is the file's path. For provider tracks it would be a
 * provider-scoped id; that is a later phase. The compare/hash code only use
 * [id], so two TrackView objects with the same id are treated as the same
 * track even if metadata has changed.
 */
data class TrackView(
    val id: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long = 0L,
    val artPath: String? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is TrackView && id == other.id

    override fun hashCode(): Int = id.hashCode()
}

data class TrackSource(
    /** Primary identifier for sorting and dedup; for local files this is [TrackView.id]. */
    val id: String,
    /** User-facing text shown in library and search results. */
    val title: String,
    val artist: String? = null,
    val album: String,
    val durationMs: Long,
    /** Local filesystem path or provider URL used to actually open the file. */
    val source: String,
    /** Absolute path to cover art if one is available for this track. */
    val artPath: String? = null,
)
