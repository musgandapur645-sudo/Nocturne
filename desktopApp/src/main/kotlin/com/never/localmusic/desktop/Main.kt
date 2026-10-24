package com.never.localmusic.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.never.localmusic.core.TrackView
import com.never.localmusic.shared.LocalPlayerSnapshot
import com.never.localmusic.shared.Playlist
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun main() = application {
    val player = remember {
        DesktopLocalPlayer().also {
            DesktopMain.chooseFolder = { chooseFolder() }
        }
    }

    Window(
        onCloseRequest = {
            player.dispose()
            exitApplication()
        },
        title = "Local Music Player",
        state = rememberWindowState(width = 860.dp, height = 780.dp),
    ) {
        DarkGlassTheme {
            Box(modifier = Modifier.fillMaxSize().background(GlassColors.Background)) {
                LocalMusicApp(player)
            }
        }
    }
}

@Composable
fun LocalMusicApp(player: DesktopLocalPlayer) {
    val snap by player.snapshotFlow.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedNav by rememberSaveable { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var folderName by remember { mutableStateOf<String?>(null) }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var addToPlaylistTrack by remember { mutableStateOf<TrackView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(bottom = 96.dp)) {
            // Header with App Title, Search & Folder Picker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "SoundVault",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassColors.TextPrimary,
                    )
                    Text(
                        folderName?.let { "Folder: $it" } ?: "No folder selected",
                        fontSize = 12.sp,
                        color = GlassColors.TextMuted,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            player.search(it)
                        },
                        placeholder = {
                            Text(
                                "Search songs, artists...",
                                color = GlassColors.TextMuted,
                                fontSize = 13.sp,
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.width(260.dp).height(48.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GlassColors.Accent,
                            unfocusedBorderColor = GlassColors.Border,
                            focusedContainerColor = GlassColors.Surface,
                            unfocusedContainerColor = GlassColors.Surface,
                            focusedTextColor = GlassColors.TextPrimary,
                            unfocusedTextColor = GlassColors.TextPrimary,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )

                    Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val folder = chooseFolder()
                                if (folder != null) withContext(Dispatchers.Main) {
                                    folderName = folder.name
                                    player.updateSelectedFolder(folder)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassColors.Accent),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Open Folder", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Navigation Pills (Home, Playlists, Favorites, History)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Home & Library", "Playlists", "Favorites", "History").forEachIndexed { index, label ->
                    val isSelected = selectedNav == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) GlassColors.Accent else GlassColors.Surface)
                            .clickable { selectedNav = index }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            label,
                            color = if (isSelected) Color.White else GlassColors.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }

            HorizontalDivider(
                color = GlassColors.Border,
                modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
            )

            // Main Content Area
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                when (selectedNav) {
                    0 -> HomeLibraryView(
                        snap = snap,
                        onPlay = { player.playSelected(it) },
                        onToggleFav = { player.toggleFavorite(it) },
                        onAddToPlaylist = if (snap.playlists.isEmpty()) null else { { addToPlaylistTrack = it } },
                    )
                    1 -> PlaylistsView(
                        snap = snap,
                        onCreatePlaylist = { showNewPlaylistDialog = true },
                        onPlayPlaylist = { player.playPlaylist(it) },
                        onDeletePlaylist = { player.deletePlaylist(it) },
                    )
                    2 -> FavoritesView(
                        snap = snap,
                        onPlay = { player.playSelected(it) },
                        onToggleFav = { player.toggleFavorite(it) },
                    )
                    3 -> HistoryView(
                        snap = snap,
                        onPlay = { trackId ->
                            snap.library.firstOrNull { it.id == trackId }?.let { player.playSelected(it) }
                        },
                        onPause = { player.pauseHistory() },
                        onResume = { player.resumeHistory() },
                        onClear = { player.clearHistory() },
                    )
                }
            }
        }

        // Floating Glass Now Playing Bar at the Bottom
        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
            FloatingNowPlayingBar(
                snap = snap,
                onTogglePlayPause = { player.togglePlayPause() },
                onNext = { player.playNext() },
                onPrevious = { player.playPrevious() },
                onSeek = { player.seekTo(it) },
                onToggleShuffle = { player.toggleShuffle() },
                onToggleRepeat = { player.toggleRepeat() },
            )
        }

        if (showNewPlaylistDialog) {
            CreatePlaylistDialog(onDismiss = { showNewPlaylistDialog = false }) { name ->
                player.createPlaylist(name)
                showNewPlaylistDialog = false
            }
        }

        addToPlaylistTrack?.let { track ->
            AddToPlaylistDialog(
                playlists = snap.playlists,
                onDismiss = { addToPlaylistTrack = null },
                onConfirm = { playlistId ->
                    player.addToPlaylist(playlistId, track)
                    addToPlaylistTrack = null
                },
            )
        }
    }
}

@Composable
fun HomeLibraryView(
    snap: LocalPlayerSnapshot,
    onPlay: (TrackView) -> Unit,
    onToggleFav: (String) -> Unit,
    onAddToPlaylist: ((TrackView) -> Unit)? = null,
) {
    // The header search writes snap.searchQuery; keep the shelf list in sync with it.
    val tracks = if (snap.searchQuery.isBlank()) snap.library else snap.filteredLibrary

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        // Shelf 1: "Jump Back In" (Recent from History)
        if (snap.history.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Jump Back In",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassColors.TextPrimary,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        snap.history.take(6).forEach { entry ->
                            val track = snap.library.firstOrNull { it.id == entry.trackId }
                            Box(
                                modifier = Modifier
                                    .width(150.dp)
                                    .glassPanel()
                                    .clickable { track?.let { onPlay(it) } }
                                    .padding(12.dp),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TrackArtwork(
                                        artPath = track?.artPath,
                                        modifier = Modifier
                                            .size(126.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        fallbackIconSize = 36.sp,
                                    )
                                    Text(
                                        entry.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GlassColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        entry.artist ?: "Unknown Artist",
                                        fontSize = 11.sp,
                                        color = GlassColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Shelf 2: All Songs Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "All Tracks (${tracks.size})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassColors.TextPrimary,
                )
            }
        }

        // Track rows
        if (tracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (snap.library.isEmpty())
                            "No audio tracks found. Click 'Open Folder' to scan your music."
                        else
                            "No tracks match \u201c${snap.searchQuery}\u201d.",
                        color = GlassColors.TextMuted,
                    )
                }
            }
        } else {
            itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                val isPlayingThis = snap.nowPlaying?.id == track.id
                val isFav = snap.favorites.contains(track.id)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassPanel(
                            cornerRadius = 10.dp,
                            backgroundColor = if (isPlayingThis) GlassColors.GlassCardHover else GlassColors.Surface,
                            borderColor = if (isPlayingThis) GlassColors.Accent else GlassColors.Border,
                        )
                        .clickable { onPlay(track) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${index + 1}",
                        fontSize = 12.sp,
                        color = if (isPlayingThis) GlassColors.Accent else GlassColors.TextMuted,
                        modifier = Modifier.width(32.dp),
                    )

                    TrackArtwork(
                        artPath = track.artPath,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        fallbackIconSize = 14.sp,
                    )
                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            track.title,
                            fontSize = 14.sp,
                            fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Medium,
                            color = if (isPlayingThis) GlassColors.Accent else GlassColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            track.artist ?: "Unknown Artist",
                            fontSize = 12.sp,
                            color = GlassColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Text(
                        formatDuration(track.durationMs),
                        fontSize = 12.sp,
                        color = GlassColors.TextMuted,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )

                    if (onAddToPlaylist != null) {
                        IconButton(
                            onClick = { onAddToPlaylist(track) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Text("+", fontSize = 16.sp, color = GlassColors.TextMuted)
                        }
                    }

                    IconButton(onClick = { onToggleFav(track.id) }, modifier = Modifier.size(28.dp)) {
                        Text(if (isFav) "\u2764\uFE0F" else "\uD83E\uDD0D", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingNowPlayingBar(
    snap: LocalPlayerSnapshot,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
) {
    val track = snap.nowPlaying

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(
                cornerRadius = 16.dp,
                backgroundColor = Color(0xEE161A26),
                borderColor = GlassColors.Border,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Track metadata thumbnail
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TrackArtwork(
                        artPath = track?.artPath,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        fallbackIconSize = 20.sp,
                    )
                    Column {
                        Text(
                            track?.title ?: "No track playing",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GlassColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            track?.artist ?: "Select a song from library",
                            fontSize = 11.sp,
                            color = GlassColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Transport Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onToggleShuffle, modifier = Modifier.size(32.dp)) {
                        Text(
                            "\uD83D\uDD00",
                            fontSize = 13.sp,
                            color = if (snap.isShuffle) GlassColors.Accent else GlassColors.TextPrimary,
                        )
                    }
                    IconButton(onClick = onPrevious, modifier = Modifier.size(34.dp)) {
                        Text("\u23EE", fontSize = 16.sp, color = GlassColors.TextPrimary)
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GlassColors.Accent)
                            .clickable { onTogglePlayPause() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (snap.isPlaying) "\u23F8" else "\u25B6",
                            fontSize = 16.sp,
                            color = Color.White,
                        )
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(34.dp)) {
                        Text("\u23ED", fontSize = 16.sp, color = GlassColors.TextPrimary)
                    }
                    IconButton(onClick = onToggleRepeat, modifier = Modifier.size(32.dp)) {
                        Text(
                            "\uD83D\uDD01",
                            fontSize = 13.sp,
                            color = if (snap.isRepeat) GlassColors.Accent else GlassColors.TextPrimary,
                        )
                    }
                }
            }

            // Scrubber Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(formatDuration(snap.positionMs), fontSize = 11.sp, color = GlassColors.TextMuted)
                Slider(
                    value = snap.positionMs.toFloat(),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..(snap.durationMs.coerceAtLeast(1L)).toFloat(),
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = GlassColors.Accent,
                        activeTrackColor = GlassColors.Accent,
                        inactiveTrackColor = GlassColors.Border,
                    ),
                )
                Text(formatDuration(snap.durationMs), fontSize = 11.sp, color = GlassColors.TextMuted)
            }
        }
    }
}

/**
 * Loads embedded cover art from [artPath] and shows a music-note fallback when
 * the file is missing, unreadable, or not a decodable image.
 */
@Composable
fun TrackArtwork(
    artPath: String?,
    modifier: Modifier = Modifier,
    fallbackIconSize: androidx.compose.ui.unit.TextUnit = 24.sp,
) {
    val bitmap = remember(artPath) {
        if (!artPath.isNullOrBlank()) {
            runCatching {
                val file = File(artPath)
                if (file.exists() && file.canRead()) {
                    file.inputStream().buffered().use { loadImageBitmap(it) }
                } else null
            }.getOrNull()
        } else null
    }

    Box(
        modifier = modifier.background(GlassColors.Surface),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Cover Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text("\uD83C\uDFB5", fontSize = fallbackIconSize)
        }
    }
}

@Composable
fun PlaylistsView(
    snap: LocalPlayerSnapshot,
    onCreatePlaylist: () -> Unit,
    onPlayPlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Your Playlists",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GlassColors.TextPrimary,
            )
            Button(
                onClick = onCreatePlaylist,
                colors = ButtonDefaults.buttonColors(containerColor = GlassColors.Accent),
                shape = RoundedCornerShape(10.dp),
            ) { Text("Create Playlist", fontSize = 12.sp) }
        }

        if (snap.playlists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No playlists yet. Create one to organize your favorites!",
                    color = GlassColors.TextMuted,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(snap.playlists, key = { it.id }) { pl ->
                    Row(
                        modifier = Modifier.fillMaxWidth().glassPanel().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                pl.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassColors.TextPrimary,
                            )
                            Text(
                                "${pl.tracks.size} tracks",
                                fontSize = 12.sp,
                                color = GlassColors.TextSecondary,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = { onPlayPlaylist(pl.id) },
                                enabled = pl.tracks.isNotEmpty(),
                            ) {
                                Text("Play", fontSize = 12.sp, color = GlassColors.TextSecondary)
                            }
                            TextButton(onClick = { onDeletePlaylist(pl.id) }) {
                                Text("Delete", fontSize = 12.sp, color = GlassColors.TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesView(
    snap: LocalPlayerSnapshot,
    onPlay: (TrackView) -> Unit,
    onToggleFav: (String) -> Unit,
) {
    val favTracks = snap.library.filter { snap.favorites.contains(it.id) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Favorite Tracks (${favTracks.size})",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = GlassColors.TextPrimary,
        )
        if (favTracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No favorites yet. Tap the heart on any track to add it here.",
                    color = GlassColors.TextMuted,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(favTracks, key = { it.id }) { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassPanel()
                            .clickable { onPlay(track) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                track.title,
                                fontWeight = FontWeight.SemiBold,
                                color = GlassColors.TextPrimary,
                            )
                            Text(
                                track.artist ?: "Unknown",
                                fontSize = 12.sp,
                                color = GlassColors.TextSecondary,
                            )
                        }
                        IconButton(onClick = { onToggleFav(track.id) }) {
                            Text("\u2764\uFE0F")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryView(
    snap: LocalPlayerSnapshot,
    onPlay: (String) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Listening History",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GlassColors.TextPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = if (snap.isHistoryPaused) onResume else onPause,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(if (snap.isHistoryPaused) "Resume" else "Pause", fontSize = 11.sp)
                }
                OutlinedButton(onClick = onClear, shape = RoundedCornerShape(8.dp)) {
                    Text("Clear", fontSize = 11.sp)
                }
            }
        }

        if (snap.history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("No listening history recorded yet.", color = GlassColors.TextMuted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(snap.history, key = { "${it.trackId}-${it.playedAtMs}" }) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassPanel()
                            .clickable { onPlay(entry.trackId) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                entry.title,
                                fontWeight = FontWeight.SemiBold,
                                color = GlassColors.TextPrimary,
                            )
                            Text(
                                entry.artist ?: "Unknown",
                                fontSize = 12.sp,
                                color = GlassColors.TextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Playlist", color = GlassColors.TextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist Name") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = GlassColors.Surface,
    )
}

@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist", color = GlassColors.TextPrimary) },
        text = {
            LazyColumn {
                items(playlists, key = { it.id }) { pl ->
                    TextButton(
                        onClick = { onConfirm(pl.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(pl.name) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = GlassColors.Surface,
    )
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

private fun chooseFolder(): File? {
    var result: File? = null
    SwingUtilities.invokeAndWait {
        val chooser = JFileChooser().apply {
            dialogTitle = "Choose a music folder"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            result = chooser.selectedFile?.takeIf { it.isDirectory && it.canRead() }
        }
    }
    return result
}
