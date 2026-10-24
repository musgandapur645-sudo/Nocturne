package com.never.localmusic.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.never.localmusic.desktop.ui.AmbientMeshBackground
import com.never.localmusic.desktop.ui.PlayerIcons
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
        title = "SoundVault",
        state = rememberWindowState(width = 1120.dp, height = 820.dp),
    ) {
        DarkGlassTheme {
            LocalMusicApp(player)
        }
    }
}

private val navTitles = listOf(
    "Home",
    "Artists",
    "Albums",
    "Play Queue",
    "Your Playlists",
    "Favorites",
    "Listening History",
)

// Muted icon/label tint used by unselected sidebar rows and secondary controls.
private val NavIdle = Color(0xFF94A3B8)
private val NavDim = Color(0xFF64748B)

@Composable
fun LocalMusicApp(player: DesktopLocalPlayer) {
    val snap by player.snapshotFlow.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedNav by rememberSaveable { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var folderName by remember { mutableStateOf<String?>(null) }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var addToPlaylistTrack by remember { mutableStateOf<TrackView?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(GlassColors.Background)) {
        // 1. Ambient blurred dynamic mesh backdrop
        AmbientMeshBackground(
            accentColor = if (snap.isPlaying) GlassColors.Accent else Color(0xFF6366F1),
        )

        // 2. Floating Island layout: Sidebar + Main Content Area
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 104.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {

            // ── Left Sidebar Floating Glass Island ──
            Column(
                modifier = Modifier
                    .width(230.dp)
                    .fillMaxHeight()
                    .glassPanel(
                        cornerRadius = 16.dp,
                        backgroundColor = Color(0x600E121D),
                        borderColor = Color(0x35FFFFFF),
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // App Logo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(GlassColors.Accent, Color(0xFF9333EA)),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                PlayerIcons.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Column {
                            Text(
                                "SoundVault",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassColors.TextPrimary,
                                letterSpacing = 0.5.sp,
                            )
                            Text(
                                folderName ?: "No library loaded",
                                fontSize = 11.sp,
                                color = GlassColors.TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    HorizontalDivider(
                        color = Color(0x15FFFFFF),
                        modifier = Modifier.padding(vertical = 10.dp),
                    )

                    // Nav section label
                    Text(
                        "BROWSE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassColors.TextMuted,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )

                    // Nav items
                    SidebarNavItem(PlayerIcons.Home, "Home", selectedNav == 0) { selectedNav = 0 }
                    SidebarNavItem(PlayerIcons.Artists, "Artists", selectedNav == 1) { selectedNav = 1 }
                    SidebarNavItem(PlayerIcons.Albums, "Albums", selectedNav == 2) { selectedNav = 2 }
                    SidebarNavItem(
                        icon = PlayerIcons.Queue,
                        label = "Queue",
                        isSelected = selectedNav == 3,
                        badgeCount = snap.queue.size,
                    ) { selectedNav = 3 }

                    HorizontalDivider(
                        color = Color(0x15FFFFFF),
                        modifier = Modifier.padding(vertical = 10.dp),
                    )

                    Text(
                        "LIBRARY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassColors.TextMuted,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )

                    SidebarNavItem(PlayerIcons.Playlists, "Playlists", selectedNav == 4) { selectedNav = 4 }
                    SidebarNavItem(
                        icon = PlayerIcons.Favorite,
                        label = "Favorites",
                        isSelected = selectedNav == 5,
                        badgeCount = snap.favorites.size,
                    ) { selectedNav = 5 }
                    SidebarNavItem(PlayerIcons.History, "History", selectedNav == 6) { selectedNav = 6 }
                }

                // Open Folder button at the bottom
                OutlinedButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val folder = chooseFolder()
                            if (folder != null) withContext(Dispatchers.Main) {
                                folderName = folder.name
                                player.updateSelectedFolder(folder)
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0x20FFFFFF),
                        contentColor = GlassColors.TextPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                ) {
                    Icon(
                        PlayerIcons.Folder,
                        contentDescription = null,
                        tint = GlassColors.Accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Folder", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // ── Main Content Floating Glass Island ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .glassPanel(
                        cornerRadius = 16.dp,
                        backgroundColor = Color(0x400E121D),
                        borderColor = Color(0x28FFFFFF),
                    ),
            ) {
                // Top bar with search
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Current view title
                    Text(
                        navTitles[selectedNav],
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassColors.TextPrimary,
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            player.search(it)
                        },
                        placeholder = {
                            Text(
                                "Search songs, artists, albums...",
                                color = NavDim,
                                fontSize = 13.sp,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                PlayerIcons.Search,
                                contentDescription = null,
                                tint = NavDim,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.width(320.dp).height(44.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GlassColors.Accent,
                            unfocusedBorderColor = Color(0x28FFFFFF),
                            focusedContainerColor = Color(0x35141926),
                            unfocusedContainerColor = Color(0x20141926),
                            focusedTextColor = GlassColors.TextPrimary,
                            unfocusedTextColor = GlassColors.TextPrimary,
                        ),
                        shape = RoundedCornerShape(22.dp),
                    )
                }

                HorizontalDivider(color = Color(0x15FFFFFF))

                // View content
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
                    when (selectedNav) {
                        0 -> HomeLibraryView(
                            snap = snap,
                            onPlay = { player.playSelected(it) },
                            onToggleFav = { player.toggleFavorite(it) },
                            onAddToPlaylist = if (snap.playlists.isEmpty()) null else { { addToPlaylistTrack = it } },
                        )
                        1 -> ArtistsView(
                            snap = snap,
                            onPlay = { player.playSelected(it) },
                        )
                        2 -> AlbumsView(
                            snap = snap,
                            onPlay = { player.playSelected(it) },
                        )
                        3 -> QueueView(
                            snap = snap,
                            onPlay = { player.playSelected(it) },
                            onClearQueue = { player.clearQueue() },
                        )
                        4 -> PlaylistsView(
                            snap = snap,
                            onCreatePlaylist = { showNewPlaylistDialog = true },
                            onPlayPlaylist = { player.playPlaylist(it) },
                            onDeletePlaylist = { player.deletePlaylist(it) },
                        )
                        5 -> FavoritesView(
                            snap = snap,
                            onPlay = { player.playSelected(it) },
                            onToggleFav = { player.toggleFavorite(it) },
                        )
                        6 -> HistoryView(
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
        }

        // Floating Glass Now Playing Bar at the Bottom
        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            FloatingNowPlayingBar(
                snap = snap,
                onTogglePlayPause = { player.togglePlayPause() },
                onNext = { player.playNext() },
                onPrevious = { player.playPrevious() },
                onSeek = { player.seekTo(it) },
                onToggleShuffle = { player.toggleShuffle() },
                onToggleRepeat = { player.toggleRepeat() },
                onToggleFavorite = { trackId -> player.toggleFavorite(trackId) },
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
fun SidebarNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    badgeCount: Int? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) Color(0x25E11D48) else Color.Transparent,
            )
            .then(
                if (isSelected) Modifier.border(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(Color(0x60E11D48), Color(0x15E11D48)),
                    ),
                    RoundedCornerShape(10.dp),
                ) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) GlassColors.Accent else NavIdle,
            )
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) GlassColors.TextPrimary else NavIdle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (badgeCount != null && badgeCount > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x20FFFFFF))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(
                    badgeCount.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassColors.TextSecondary,
                )
            }
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
                                    .glassPanel(
                                        cornerRadius = 12.dp,
                                        backgroundColor = GlassColors.GlassCard,
                                        borderColor = GlassColors.BorderSubtle,
                                    )
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPlayingThis) Color(0x35E11D48) else Color.Transparent)
                        .clickable { onPlay(track) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
                            Icon(
                                PlayerIcons.Playlists,
                                contentDescription = "Add to playlist",
                                tint = NavDim,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }

                    IconButton(onClick = { onToggleFav(track.id) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            if (isFav) PlayerIcons.Favorite else PlayerIcons.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFav) GlassColors.Accent else NavDim,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistsView(
    snap: LocalPlayerSnapshot,
    onPlay: (TrackView) -> Unit,
) {
    val artistGroups = remember(snap.library) {
        snap.library.groupBy { it.artist ?: "Unknown Artist" }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
    }
    var expandedArtist by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        item {
            Text(
                "Artists (${artistGroups.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GlassColors.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        artistGroups.forEach { (artist, tracks) ->
            val isExpanded = expandedArtist == artist

            item(key = "artist-$artist") {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassPanel(
                                cornerRadius = 10.dp,
                                backgroundColor = GlassColors.GlassCard,
                                borderColor = GlassColors.BorderSubtle,
                            )
                            .clickable { expandedArtist = if (isExpanded) null else artist }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Artist icon using first track's art
                            TrackArtwork(
                                artPath = tracks.firstOrNull()?.artPath,
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)),
                                fallbackIconSize = 18.sp,
                            )
                            Column {
                                Text(
                                    artist,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GlassColors.TextPrimary,
                                )
                                Text(
                                    "${tracks.size} track${if (tracks.size != 1) "s" else ""}",
                                    fontSize = 12.sp,
                                    color = GlassColors.TextSecondary,
                                )
                            }
                        }
                        Text(
                            if (isExpanded) "\u25B2" else "\u25BC",
                            color = GlassColors.TextMuted,
                            fontSize = 12.sp,
                        )
                    }

                    if (isExpanded) {
                        Column(
                            modifier = Modifier.padding(start = 20.dp, top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            tracks.forEach { track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onPlay(track) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    TrackArtwork(
                                        artPath = track.artPath,
                                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)),
                                        fallbackIconSize = 14.sp,
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            track.title,
                                            fontSize = 13.sp,
                                            color = GlassColors.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        track.album?.let {
                                            Text(it, fontSize = 11.sp, color = GlassColors.TextMuted, maxLines = 1)
                                        }
                                    }
                                    Text(
                                        formatDuration(track.durationMs),
                                        fontSize = 11.sp,
                                        color = GlassColors.TextMuted,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumsView(
    snap: LocalPlayerSnapshot,
    onPlay: (TrackView) -> Unit,
) {
    val albumGroups = remember(snap.library) {
        snap.library.groupBy { it.album ?: "Unknown Album" }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
    }
    var expandedAlbum by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        item {
            Text(
                "Albums (${albumGroups.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GlassColors.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        albumGroups.forEach { (album, tracks) ->
            val isExpanded = expandedAlbum == album
            val albumArtist = tracks.mapNotNull { it.artist }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }?.key ?: "Various Artists"

            item(key = "album-$album") {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassPanel(
                                cornerRadius = 10.dp,
                                backgroundColor = GlassColors.GlassCard,
                                borderColor = GlassColors.BorderSubtle,
                            )
                            .clickable { expandedAlbum = if (isExpanded) null else album }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TrackArtwork(
                                artPath = tracks.firstOrNull()?.artPath,
                                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)),
                                fallbackIconSize = 22.sp,
                            )
                            Column {
                                Text(
                                    album,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GlassColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    albumArtist,
                                    fontSize = 12.sp,
                                    color = GlassColors.TextSecondary,
                                    maxLines = 1,
                                )
                                Text(
                                    "${tracks.size} track${if (tracks.size != 1) "s" else ""}",
                                    fontSize = 11.sp,
                                    color = GlassColors.TextMuted,
                                )
                            }
                        }
                        Text(if (isExpanded) "\u25B2" else "\u25BC", color = GlassColors.TextMuted, fontSize = 12.sp)
                    }

                    if (isExpanded) {
                        Column(
                            modifier = Modifier.padding(start = 20.dp, top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            tracks.forEachIndexed { index, track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onPlay(track) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text(
                                        "${index + 1}",
                                        fontSize = 12.sp,
                                        color = GlassColors.TextMuted,
                                        modifier = Modifier.width(24.dp),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            track.title,
                                            fontSize = 13.sp,
                                            color = GlassColors.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        track.artist?.let {
                                            Text(it, fontSize = 11.sp, color = GlassColors.TextMuted)
                                        }
                                    }
                                    Text(
                                        formatDuration(track.durationMs),
                                        fontSize = 11.sp,
                                        color = GlassColors.TextMuted,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QueueView(
    snap: LocalPlayerSnapshot,
    onPlay: (TrackView) -> Unit,
    onClearQueue: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Up Next (${snap.queue.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GlassColors.TextPrimary,
            )
            if (snap.queue.isNotEmpty()) {
                OutlinedButton(onClick = onClearQueue, shape = RoundedCornerShape(8.dp)) {
                    Text("Clear Queue", fontSize = 11.sp)
                }
            }
        }

        // Now Playing highlight
        snap.nowPlaying?.let { current ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Now Playing",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GlassColors.Accent,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassPanel(
                            cornerRadius = 12.dp,
                            backgroundColor = Color(0x35E11D48),
                            borderColor = Color(0x60E11D48),
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TrackArtwork(
                        artPath = current.artPath,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                    )
                    Column {
                        Text(
                            current.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassColors.Accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            current.artist ?: "Unknown Artist",
                            fontSize = 12.sp,
                            color = GlassColors.TextSecondary,
                        )
                    }
                }
            }
        }

        if (snap.queue.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Queue is empty. Play a track to start.", color = GlassColors.TextMuted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(snap.queue, key = { i, t -> "${t.id}-$i" }) { index, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassPanel(
                                cornerRadius = 8.dp,
                                backgroundColor = GlassColors.GlassCard,
                                borderColor = GlassColors.BorderSubtle,
                            )
                            .clickable { onPlay(track) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "${index + 1}",
                            fontSize = 12.sp,
                            color = GlassColors.TextMuted,
                            modifier = Modifier.width(28.dp),
                        )
                        TrackArtwork(
                            artPath = track.artPath,
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)),
                            fallbackIconSize = 14.sp,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                track.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = GlassColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                track.artist ?: "Unknown",
                                fontSize = 11.sp,
                                color = GlassColors.TextSecondary,
                            )
                        }
                        Text(
                            formatDuration(track.durationMs),
                            fontSize = 11.sp,
                            color = GlassColors.TextMuted,
                        )
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
    onToggleFavorite: (String) -> Unit,
) {
    val track = snap.nowPlaying
    val isFav = track != null && snap.favorites.contains(track.id)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(
                cornerRadius = 18.dp,
                backgroundColor = Color(0x80101524),
                borderColor = Color(0x38FFFFFF),
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
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

                    if (track != null) {
                        IconButton(
                            onClick = { onToggleFavorite(track.id) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                if (isFav) PlayerIcons.Favorite else PlayerIcons.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) GlassColors.Accent else NavDim,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                // Transport Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onToggleShuffle, modifier = Modifier.size(32.dp)) {
                        Icon(
                            PlayerIcons.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (snap.isShuffle) GlassColors.Accent else NavDim,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                        Icon(
                            PlayerIcons.SkipPrev,
                            contentDescription = "Previous",
                            tint = GlassColors.TextPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GlassColors.Accent)
                            .clickable { onTogglePlayPause() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (snap.isPlaying) PlayerIcons.Pause else PlayerIcons.Play,
                            contentDescription = if (snap.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                        Icon(
                            PlayerIcons.SkipNext,
                            contentDescription = "Next",
                            tint = GlassColors.TextPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(onClick = onToggleRepeat, modifier = Modifier.size(32.dp)) {
                        Icon(
                            PlayerIcons.Repeat,
                            contentDescription = "Repeat",
                            tint = if (snap.isRepeat) GlassColors.Accent else NavDim,
                            modifier = Modifier.size(16.dp),
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
                    modifier = Modifier.weight(1f).height(12.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = GlassColors.TextPrimary,
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
            Icon(
                PlayerIcons.MusicNote,
                contentDescription = null,
                tint = Color(0xFF475569),
                modifier = Modifier.size(fallbackIconSize.value.dp),
            )
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassPanel(
                                cornerRadius = 10.dp,
                                backgroundColor = GlassColors.GlassCard,
                                borderColor = GlassColors.BorderSubtle,
                            )
                            .padding(14.dp),
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
                            .glassPanel(
                                cornerRadius = 10.dp,
                                backgroundColor = GlassColors.GlassCard,
                                borderColor = GlassColors.BorderSubtle,
                            )
                            .clickable { onPlay(track) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TrackArtwork(
                            artPath = track.artPath,
                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(6.dp)),
                            fallbackIconSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
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
                            Icon(
                                PlayerIcons.Favorite,
                                contentDescription = "Remove from favorites",
                                tint = GlassColors.Accent,
                                modifier = Modifier.size(16.dp),
                            )
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
                            .glassPanel(
                                cornerRadius = 10.dp,
                                backgroundColor = GlassColors.GlassCard,
                                borderColor = GlassColors.BorderSubtle,
                            )
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
