package com.never.localmusic.desktop

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.never.localmusic.core.TrackView
import com.never.localmusic.desktop.ui.ConvxMeshBackground
import com.never.localmusic.desktop.ui.PlayerIcons
import com.never.localmusic.desktop.ui.rememberArtworkPalette
import com.never.localmusic.shared.AppSettings
import com.never.localmusic.shared.LocalPlayerSnapshot
import com.never.localmusic.shared.Playlist
import com.never.localmusic.shared.ThemePresets
import com.never.localmusic.shared.formatHex
import com.never.localmusic.shared.parseHex
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

fun main(args: Array<String>) = application {
    val player = remember {
        DesktopLocalPlayer().also {
            DesktopMain.chooseFolder = { chooseFolder() }
            DesktopMain.chooseSettingsFileToOpen = { chooseSettingsFile("Import settings", save = false) }
            DesktopMain.chooseSettingsFileToSave = { name ->
                chooseSettingsFile("Export settings", save = true, suggestedName = name)
            }
        }
    }
    // Track whether the search field owns keyboard focus so the window-level
    // Space shortcut below can yield to it while typing.
    val searchFocus = remember { SearchFocusFlag() }

    // Optional startup folder: `desktopApp <musicDir> [--play]` boots the library
    // directly, skipping the JFileChooser dialog; --play also starts the first track.
    LaunchedEffect(Unit) {
        val folder = args.firstOrNull()?.let(::File)?.takeIf { it.isDirectory && it.canRead() }
            ?: return@LaunchedEffect
        player.updateSelectedFolder(folder)
        if (args.getOrNull(1) == "--play") {
            // The folder scan is async; wait (bounded) for it to surface tracks.
            withTimeoutOrNull(15_000) {
                while (player.snapshot.library.isEmpty()) delay(100)
            }
            player.snapshot.library.firstOrNull()?.let(player::playSelected)
        }
    }

    Window(
        onCloseRequest = {
            player.dispose()
            exitApplication()
        },
        title = "Nocturne",
        state = rememberWindowState(width = 1120.dp, height = 820.dp),
        // Window level so the shortcut works no matter which internal node has
        // focus. Global Space = play/pause, skipped while typing in search.
        // Ctrl/Cmd + Right = Next, Ctrl/Cmd + Left = Prev, Esc = Clear search.
        onKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown) {
                if (event.key == Key.Spacebar && !searchFocus.active) {
                    player.togglePlayPause()
                    true
                } else if (event.key == Key.Escape && (searchFocus.active || searchFocus.clearSearch != null)) {
                    searchFocus.clearSearch?.invoke()
                    searchFocus.active = false
                    true
                } else if ((event.isCtrlPressed || event.isMetaPressed) && event.key == Key.DirectionRight) {
                    player.playNext()
                    true
                } else if ((event.isCtrlPressed || event.isMetaPressed) && event.key == Key.DirectionLeft) {
                    player.playPrevious()
                    true
                } else false
            } else false
        },
    ) {
        // Settings drive the palette, so the whole window recolours as soon as
        // a preset, accent, or glass level changes.
        val settings by player.snapshotFlow.collectAsState()
        DarkGlassTheme(settings.settings) {
            LocalMusicApp(player, searchFocus)
        }
    }
}

/** Shared flag: does the search text field currently own keyboard focus? */
class SearchFocusFlag {
    var active by mutableStateOf(false)
    var clearSearch: (() -> Unit)? = null
}

private val navTitles = listOf(
    "Home",
    "Artists",
    "Albums",
    "Play Queue",
    "Your Playlists",
    "Favorites",
    "Listening History",
    "Appearance",
)

// Muted icon/label tint used by unselected sidebar rows and secondary controls.
private val NavIdle = Color(0xFF94A3B8)
private val NavDim = Color(0xFF94A3B8)

/** Displayed under the Nocturne logo; TrackView.id is the absolute file path. */
private fun LibraryFolderName(library: List<TrackView>): String {
    val folder = library.firstOrNull()?.let { File(it.id).parentFile } ?: return "No library loaded"
    return folder.name
}

@Composable
fun LocalMusicApp(player: DesktopLocalPlayer, searchFocus: SearchFocusFlag) {
    val snap by player.snapshotFlow.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedNav by rememberSaveable { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var addToPlaylistTrack by remember { mutableStateOf<TrackView?>(null) }
    var appearanceMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        searchFocus.clearSearch = {
            searchQuery = ""
            player.search("")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassColors.Background),
    ) {
        // Convx ambient mesh: four artwork-derived blobs drifting under a heavy blur.
        // All panels below are translucent, so this color field is what they refract.
        ConvxMeshBackground(
            palette = rememberArtworkPalette(snap.nowPlaying?.artPath),
            trackKey = snap.nowPlaying?.id,
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
                verticalArrangement = Arrangement.Top,
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
                                "Nocturne",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassColors.TextPrimary,
                                letterSpacing = 0.5.sp,
                            )
                            Text(
                                LibraryFolderName(snap.library),
                                fontSize = 12.sp,
                                color = GlassColors.TextSecondary,
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

                    HorizontalDivider(
                        color = Color(0x15FFFFFF),
                        modifier = Modifier.padding(vertical = 10.dp),
                    )

                    SidebarNavItem(PlayerIcons.Sliders, "Appearance", selectedNav == 7) { selectedNav = 7 }
                }

                // Open Folder button at the bottom
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val folder = chooseFolder()
                            if (folder != null) withContext(Dispatchers.Main) {
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
                    // Current view title. fill=false so it only takes what
                    // it needs and the search field gets the rest.
                    Text(
                        navTitles[selectedNav],
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            player.search(it)
                        },
                        placeholder = {
                            Text(
                                "Search songs, artists, albums\u2026",
                                color = NavIdle,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                PlayerIcons.Search,
                                contentDescription = null,
                                tint = NavIdle,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        player.search("")
                                    },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        PlayerIcons.Close,
                                        contentDescription = "Clear search",
                                        tint = NavIdle,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        } else null,
                        singleLine = true,
                        // Shared space: takes the room left next to the title,
                        // capped at 320dp. Never grows vertically: the
                        // placeholder stays one line and ellipsizes.
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = 160.dp, max = 320.dp)
                            .onFocusChanged { searchFocus.active = it.hasFocus },
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

                // Error banner (scan failure or playback failure)
                val errorMessage = snap.playbackError ?: snap.scanError
                if (errorMessage != null) {
                    var errorDismissed by remember(errorMessage) { mutableStateOf(false) }
                    if (!errorDismissed) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                                .glassPanel(
                                    cornerRadius = 8.dp,
                                    backgroundColor = Color(0x35E11D48),
                                    borderColor = Color(0x66E11D48),
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                errorMessage,
                                color = Color(0xFFFF8599),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { errorDismissed = true },
                                modifier = Modifier.size(20.dp),
                            ) {
                                Icon(
                                    PlayerIcons.Close,
                                    contentDescription = "Dismiss error",
                                    tint = Color(0xFFFF8599),
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                }

                // View content
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
                    when (selectedNav) {
                        0 -> HomeLibraryView(
                            snap = snap,
                            onPlay = { player.playSelected(it) },
                            onToggleFav = { player.toggleFavorite(it) },
                            onAddToPlaylist = if (snap.playlists.isEmpty()) null else { { addToPlaylistTrack = it } },
                            onAddToQueue = { player.addToQueue(it) },
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
                            onRemoveFromQueue = { player.removeFromQueue(it) },
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
                        7 -> AppearanceView(
                            settings = snap.settings,
                            onUpdate = { player.updateSettings(it) },
                            onImport = {
                                scope.launch(Dispatchers.IO) {
                                    val file = DesktopMain.chooseSettingsFileToOpen?.invoke()
                                    val message = when {
                                        file == null -> null
                                        else -> runCatching { file.readText() }.fold(
                                            onSuccess = { text ->
                                                player.importSettingsJson(text)
                                                    ?.let { "Import failed: $it" }
                                                    ?: "Imported settings from ${file.name}"
                                            },
                                            onFailure = {
                                                "Could not read ${file.name}: ${it.message ?: "unknown error"}"
                                            },
                                        )
                                    }
                                    withContext(Dispatchers.Main) {
                                        message?.let { appearanceMessage = it }
                                    }
                                }
                            },
                            onExport = {
                                val text = player.exportSettingsJson()
                                scope.launch(Dispatchers.IO) {
                                    val file = DesktopMain.chooseSettingsFileToSave?.invoke("nocturne-settings.json")
                                    val message = when {
                                        file == null -> null
                                        else -> runCatching {
                                            file.writeText(text)
                                            "Exported settings to ${file.absolutePath}"
                                        }.getOrElse {
                                            "Could not write ${file.absolutePath}: ${it.message}"
                                        }
                                    }
                                    withContext(Dispatchers.Main) {
                                        message?.let { appearanceMessage = it }
                                    }
                                }
                            },
                            onMessage = { appearanceMessage = it },
                        )
                    }
                }
            }
        }

        // Floating Glass Now Playing Bar at the Bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
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

        appearanceMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { appearanceMessage = null },
                title = { Text("Settings", color = GlassColors.TextPrimary) },
                text = { Text(message, color = GlassColors.TextSecondary, fontSize = 13.sp) },
                confirmButton = {
                    TextButton(onClick = { appearanceMessage = null }) {
                        Text("OK", color = GlassColors.Accent)
                    }
                },
                containerColor = Color(0xF0141824),
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
    var isHovered by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    // Read the accent here: drawBehind runs outside composition.
    val accent = GlassColors.Accent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isSelected -> accent.copy(alpha = 0.15f)
                    isHovered -> Color.White.copy(alpha = 0.05f)
                    else -> Color.Transparent
                },
            )
            // Convx active accent indicator: a small rounded crimson bar on the left edge.
            .drawBehind {
                if (isSelected) {
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(0f, size.height / 2f - 9.dp.toPx() / 2f),
                        size = Size(3.dp.toPx(), 18.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                }
            }
            // Keyboard focus ring: instant, never animated (craft floor).
            .then(
                if (isFocused) Modifier.border(1.5.dp, GlassColors.Accent, RoundedCornerShape(10.dp))
                else Modifier
            )
            .onFocusChanged { isFocused = it.hasFocus }
            .hoverChanged { isHovered = true }
            .hoverChanged(false) { isHovered = false }
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
    onAddToQueue: ((TrackView) -> Unit)? = null,
) {
    // The header search writes snap.searchQuery; keep the shelf list in sync with it.
    val tracks = if (snap.searchQuery.isBlank()) snap.library else snap.filteredLibrary

    LazyColumn(
        // Rows carry their own 8dp padding; keep list rhythm tight so the
        // tracks read as one list, not as separated cards.
        verticalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Shelf 1: "Jump Back In" (Recent from History)
        if (snap.history.isNotEmpty()) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
                ) {
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
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp),
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
                            "No audio tracks found. Click \u2018Open Folder\u2019 to scan your music."
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
                var trackIsHovered by remember { mutableStateOf(false) }
                val accent = GlassColors.Accent

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 900.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isPlayingThis -> accent.copy(alpha = 0.12f)
                                trackIsHovered -> Color.White.copy(alpha = 0.04f)
                                else -> Color.Transparent
                            },
                        )
                        .hoverChanged { trackIsHovered = true }
                        .hoverChanged(false) { trackIsHovered = false }
                        .clickable { onPlay(track) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${index + 1}",
                        fontSize = 12.sp,
                        color = if (isPlayingThis) GlassColors.Accent else GlassColors.TextPrimary,
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
                        color = GlassColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )

                    if (onAddToQueue != null) {
                        IconButton(
                            onClick = { onAddToQueue(track) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                PlayerIcons.Queue,
                                contentDescription = "Add to queue",
                                tint = NavDim,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }

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
    onRemoveFromQueue: (Int) -> Unit = {},
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
                            backgroundColor = GlassColors.Accent.copy(alpha = 0.18f),
                            borderColor = GlassColors.Accent.copy(alpha = 0.35f),
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
                        IconButton(
                            onClick = { onRemoveFromQueue(index) },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                PlayerIcons.Close,
                                contentDescription = "Remove from queue",
                                tint = GlassColors.TextMuted,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
    val duration = snap.durationMs.coerceAtLeast(1L)
    val progress = (snap.positionMs.toFloat() / duration).coerceIn(0f, 1f)

    // Reads fresh values inside the long-lived drag gesture without restarting it.
    val seekTo by rememberUpdatedState(onSeek)
    val durationNow by rememberUpdatedState(duration)
    val progressNow by rememberUpdatedState(progress)
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    // Read the accent here: drawBehind runs outside composition.
    val accent = GlassColors.Accent

    Box(
        modifier = Modifier
            .widthIn(max = 1000.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .glassPanel(
                cornerRadius = 32.dp,
                backgroundColor = Color(0x8C0E1220),
                borderColor = Color(0x40FFFFFF),
                elevation = 14.dp,
            ),
    ) {
        // Playback progress strip: only rendered when a track is loaded.
        // Inset with horizontal padding so it stays on the flat bottom edge
        // and never bleeds through the dock's rounded pill corners.
        if (track != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, bottom = 4.dp)
                    .height(10.dp)
                    .align(Alignment.BottomCenter)
                    .pointerInput(track.id) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                            },
                            onDragEnd = {
                                dragFraction?.let { seekTo((it * durationNow).toLong()) }
                                dragFraction = null
                            },
                            onDragCancel = { dragFraction = null },
                        ) { change, dragAmount ->
                            change.consume()
                            dragFraction = ((dragFraction ?: progressNow) + dragAmount / size.width)
                                .coerceIn(0f, 1f)
                        }
                    }
                    .pointerInput(track.id) {
                        val w = size.width
                        detectTapGestures { offset ->
                            val f = (offset.x / w).coerceIn(0f, 1f)
                            seekTo((f * durationNow).toLong())
                        }
                    }
                    .drawBehind {
                        val barHeight = 3.dp.toPx()
                        val barTop = (size.height - barHeight) / 2f
                        val corner = CornerRadius(barHeight / 2f, barHeight / 2f)
                        // Background track
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.15f),
                            topLeft = Offset(0f, barTop),
                            size = Size(size.width, barHeight),
                            cornerRadius = corner,
                        )
                        // Filled progress
                        val fraction = dragFraction ?: progressNow
                        if (fraction > 0f) {
                            drawRoundRect(
                                color = accent,
                                topLeft = Offset(0f, barTop),
                                size = Size(size.width * fraction, barHeight),
                                cornerRadius = corner,
                            )
                        }
                    },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Artwork + typography
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrackArtwork(
                    artPath = track?.artPath,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    fallbackIconSize = 18.sp,
                )
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        track?.title ?: "No track playing",
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp,
                        color = GlassColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(
                            iterations = 1,
                            initialDelayMillis = 3000,
                            velocity = 30.dp,
                        ),
                    )
                    val subtitle = if (track != null) {
                        val artist = track.artist ?: "Unknown Artist"
                        if (snap.durationMs > 0) {
                            "$artist  \u00b7  ${formatDuration(snap.positionMs)} / ${formatDuration(snap.durationMs)}"
                        } else artist
                    } else "Select a song from library"

                    Text(
                        subtitle,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = GlassColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(
                            iterations = 1,
                            initialDelayMillis = 3000,
                            velocity = 30.dp,
                        ),
                    )
                }

                if (track != null) {
                    IconButton(
                        onClick = { onToggleFavorite(track.id) },
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            if (isFav) PlayerIcons.Favorite else PlayerIcons.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFav) GlassColors.Accent else GlassColors.TextSecondary,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }

            // Transport: spring-pressed icon buttons around the accent play circle.
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SpringIconButton(
                    icon = PlayerIcons.Shuffle,
                    description = "Shuffle",
                    tint = if (snap.isShuffle) GlassColors.Accent else GlassColors.TextSecondary,
                    iconSize = 18.dp,
                    onClick = onToggleShuffle,
                )
                SpringIconButton(
                    icon = PlayerIcons.SkipPrev,
                    description = "Previous",
                    tint = GlassColors.TextPrimary,
                    iconSize = 20.dp,
                    onClick = onPrevious,
                )

                val playInteraction = remember { MutableInteractionSource() }
                val playPressed by playInteraction.collectIsPressedAsState()
                val playScale by animateFloatAsState(
                    targetValue = if (playPressed) 0.88f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "playScale",
                )
                IconButton(
                    onClick = onTogglePlayPause,
                    interactionSource = playInteraction,
                    modifier = Modifier.size(42.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { scaleX = playScale; scaleY = playScale }
                            .clip(CircleShape)
                            .background(GlassColors.Accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (snap.isPlaying) PlayerIcons.Pause else PlayerIcons.Play,
                            contentDescription = if (snap.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                SpringIconButton(
                    icon = PlayerIcons.SkipNext,
                    description = "Next",
                    tint = GlassColors.TextPrimary,
                    iconSize = 20.dp,
                    onClick = onNext,
                )
                SpringIconButton(
                    icon = PlayerIcons.Repeat,
                    description = "Repeat",
                    tint = if (snap.isRepeat) GlassColors.Accent else GlassColors.TextSecondary,
                    iconSize = 18.dp,
                    onClick = onToggleRepeat,
                )
            }
        }
    }
}

/** Transport button that sinks under the finger with a spring, like Convx's icon clicks. */
@Composable
private fun SpringIconButton(
    icon: ImageVector,
    description: String,
    tint: Color = GlassColors.TextPrimary,
    iconSize: Dp = 18.dp,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "iconSpringScale",
    )
    IconButton(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier
            .size(34.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
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


/** Desktop-safe pointer enter/exit wiring for hover states. */
fun Modifier.hoverChanged(entered: Boolean = true, onHover: () -> Unit): Modifier =
    pointerInput(entered) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if ((event.type == PointerEventType.Enter && entered) ||
                    (event.type == PointerEventType.Exit && !entered)
                ) {
                    onHover()
                }
            }
        }
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

/**
 * Settings import/export picker.
 *
 * Runs on the Swing EDT like [chooseFolder]: blocking the Compose click handler
 * with a modal dialog crashes the desktop UI.
 */
private fun chooseSettingsFile(
    title: String,
    save: Boolean,
    suggestedName: String = "settings.json",
): File? {
    var result: File? = null
    SwingUtilities.invokeAndWait {
        val chooser = JFileChooser().apply {
            dialogTitle = title
            fileSelectionMode = JFileChooser.FILES_ONLY
            selectedFile = File(suggestedName)
            if (save) {
                fileFilter = javax.swing.filechooser.FileNameExtensionFilter("JSON settings", "json")
            }
        }
        val outcome = if (save) chooser.showSaveDialog(null) else chooser.showOpenDialog(null)
        if (outcome == JFileChooser.APPROVE_OPTION) {
            result = chooser.selectedFile?.let { picked ->
                if (save && !picked.name.endsWith(".json", ignoreCase = true)) {
                    File(picked.parentFile, picked.name + ".json")
                } else picked
            }
        }
    }
    return result
}

// ---------------------------------------------------------------------------
// Appearance (customization phase)
// See docs/customization-spec.md
// ---------------------------------------------------------------------------

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = GlassColors.TextMuted,
        letterSpacing = 1.5.sp,
    )
}

@Composable
fun AppearanceView(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val palette = LocalGlassPalette.current
    val glass = palette.glass
    val activeSlug = ThemePresets.bySlug(settings.preset).slug

    var hexDraft by remember(settings.accent, settings.preset) {
        mutableStateOf(formatHex(parseHex(settings.accent) ?: ThemePresets.bySlug(settings.preset).accent))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionLabel("PRESET")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ThemePresets.all.forEach { preset ->
                val selected = preset.slug == activeSlug
                val accent = Color(preset.accent)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selected) accent.copy(alpha = 0.18f) else Color(0x1AFFFFFF)
                        )
                        .border(
                            width = 1.dp,
                            color = if (selected) accent else Color(0x22FFFFFF),
                            shape = RoundedCornerShape(10.dp),
                        )
                        .clickable { onUpdate(settings.copy(preset = preset.slug, accent = null)) }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accent),
                    )
                    Text(
                        preset.displayName,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) GlassColors.TextPrimary else GlassColors.TextSecondary,
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        SectionLabel("ACCENT")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ThemePresets.all.forEach { preset ->
                val swatch = Color(preset.accent)
                val isCurrent = formatHex(parseHex(settings.accent) ?: ThemePresets.bySlug(settings.preset).accent) ==
                    formatHex(preset.accent)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(
                            width = if (isCurrent) 2.dp else 1.dp,
                            color = if (isCurrent) Color.White else Color(0x33FFFFFF),
                            shape = CircleShape,
                        )
                        .clickable { onUpdate(settings.copy(accent = formatHex(preset.accent))) },
                )
            }

            Spacer(Modifier.width(10.dp))
            OutlinedTextField(
                value = hexDraft,
                onValueChange = { hexDraft = it },
                singleLine = true,
                placeholder = { Text("#RRGGBB", color = NavDim, fontSize = 12.sp) },
                modifier = Modifier.width(120.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GlassColors.Accent,
                    unfocusedBorderColor = Color(0x28FFFFFF),
                    focusedContainerColor = Color(0x35141926),
                    unfocusedContainerColor = Color(0x20141926),
                    focusedTextColor = GlassColors.TextPrimary,
                    unfocusedTextColor = GlassColors.TextPrimary,
                ),
                shape = RoundedCornerShape(10.dp),
            )
            TextButton(onClick = {
                val parsed = parseHex(hexDraft)
                if (parsed == null) onMessage("Use a #RRGGBB value, for example #E11D48.") else {
                    onUpdate(settings.copy(accent = formatHex(parsed)))
                    onMessage("Accent set to ${formatHex(parsed)}.")
                }
            }) { Text("Apply", color = GlassColors.Accent, fontSize = 12.sp) }

            if (settings.accent != null) {
                TextButton(onClick = { onUpdate(settings.copy(accent = null)) }) {
                    Text("Use preset accent", color = GlassColors.TextSecondary, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("GLASS")
            Spacer(Modifier.width(10.dp))
            Text(
                "${glass.tier} \u00b7 ${glass.intensity}",
                fontSize = 12.sp,
                color = GlassColors.TextSecondary,
            )
        }
        Text(
            "0 is flat and solid; 100 lets the artwork glow through the panels most.",
            fontSize = 11.sp,
            color = NavDim,
        )
        Slider(
            value = glass.intensity.toFloat(),
            onValueChange = { onUpdate(settings.copy(glassIntensity = it.roundToInt())) },
            valueRange = 0f..100f,
            steps = 0,
            colors = SliderDefaults.colors(
                thumbColor = GlassColors.Accent,
                activeTrackColor = GlassColors.Accent,
                inactiveTrackColor = Color(0x33FFFFFF),
            ),
            modifier = Modifier.widthIn(max = 420.dp),
        )

        Spacer(Modifier.height(6.dp))
        SectionLabel("SETTINGS FILE")
        Text(
            "One JSON file holds the preset, accent, and glass level \u2014 see docs/customization-spec.md.",
            fontSize = 11.sp,
            color = NavDim,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onExport,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0x20FFFFFF),
                    contentColor = GlassColors.TextPrimary,
                ),
            ) { Text("Export\u2026", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }

            OutlinedButton(
                onClick = onImport,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0x20FFFFFF),
                    contentColor = GlassColors.TextPrimary,
                ),
            ) { Text("Import\u2026", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}
