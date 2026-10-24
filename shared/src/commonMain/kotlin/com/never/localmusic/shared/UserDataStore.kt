package com.never.localmusic.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persisted user data: playlists, favorites, history, and whether history
 * recording is paused. This is everything that must survive app restart.
 *
 * The library and current playback state are NOT persisted — they are rebuilt
 * from the SAF/filesystem tree each session.
 *
 * Serialization uses kotlinx-serialization-json. Storage location is
 * platform-specific (see [UserDataStore.storageDir]).
 */
@Serializable
data class PersistedUserData(
    val playlists: List<PersistedPlaylist> = emptyList(),
    val favorites: List<String> = emptyList(),
    val history: List<PersistedHistoryEntry> = emptyList(),
    val isHistoryPaused: Boolean = false,
)

@Serializable
data class PersistedPlaylist(
    val id: String,
    val name: String,
    val trackIds: List<String> = emptyList(),
    /** Parallel list: title for each entry in [trackIds]. */
    val trackTitles: List<String> = emptyList(),
    val trackArtists: List<String?> = emptyList(),
    val trackAlbums: List<String?> = emptyList(),
    val trackDurationsMs: List<Long> = emptyList(),
)

@Serializable
data class PersistedHistoryEntry(
    val trackId: String,
    val title: String,
    val artist: String? = null,
    val playedAtMs: Long,
)

// ---------------------------------------------------------------------------
// JSON codec (no pretty-print to keep files compact)
// ---------------------------------------------------------------------------

private val json = Json {
    ignoreUnknownKeys = true   // forward-compatible with future fields
    encodeDefaults = true
}

object UserDataCodec {

    fun encode(data: PersistedUserData): String = json.encodeToString(data)

    fun decode(text: String): PersistedUserData =
        runCatching { json.decodeFromString<PersistedUserData>(text) }
            .getOrDefault(PersistedUserData())
}

// ---------------------------------------------------------------------------
// Conversion helpers: PersistedPlaylist ↔ Playlist + TrackView
// ---------------------------------------------------------------------------

fun Playlist.toPersisted(): PersistedPlaylist = PersistedPlaylist(
    id = id,
    name = name,
    trackIds = tracks.map { it.id },
    trackTitles = tracks.map { it.title },
    trackArtists = tracks.map { it.artist },
    trackAlbums = tracks.map { it.album },
    trackDurationsMs = tracks.map { it.durationMs },
)

fun PersistedPlaylist.toPlaylist(): Playlist {
    val tracks = trackIds.mapIndexed { i, id ->
        com.never.localmusic.core.TrackView(
            id = id,
            title = trackTitles.getOrElse(i) { id.substringAfterLast('/').ifBlank { id } },
            artist = trackArtists.getOrNull(i),
            album = trackAlbums.getOrNull(i),
            durationMs = trackDurationsMs.getOrElse(i) { 0L },
        )
    }
    return Playlist(id = id, name = name, tracks = tracks)
}

fun HistoryEntry.toPersisted(): PersistedHistoryEntry =
    PersistedHistoryEntry(
        trackId = trackId,
        title = title,
        artist = artist,
        playedAtMs = playedAtMs,
    )

fun PersistedHistoryEntry.toHistoryEntry(): HistoryEntry =
    HistoryEntry(
        trackId = trackId,
        title = title,
        artist = artist,
        playedAtMs = playedAtMs,
    )
