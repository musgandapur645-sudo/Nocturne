package com.never.nocturne

import android.app.Activity
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.never.nocturne.ui.AndroidMeshBackground
import com.never.nocturne.ui.CupertinoGlassButton
import com.never.nocturne.ui.CupertinoPillToggle
import com.never.nocturne.ui.CupertinoSeekSlider
import com.never.nocturne.ui.GlassTheme
import com.never.nocturne.ui.liquidGlass
import com.never.nocturne.ui.rememberArtworkColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.io.File
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.never.nocturne.android.AndroidLocalPlayer
import com.never.nocturne.core.TrackView
import com.never.nocturne.shared.AppSettings
import com.never.nocturne.shared.LocalPlayerSnapshot
import com.never.nocturne.shared.Playlist
import com.never.nocturne.shared.ThemePresets
import com.never.nocturne.shared.formatHex
import com.never.nocturne.shared.parseHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    companion object {
        private const val SCAN_MAX_ITEMS = 5_000
        private const val SCAN_MAX_MS = 30_000L
    }

    private lateinit var player: AndroidLocalPlayer
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            startFolderScan(uri)
        }
    }

    /** Message shown after an appearance import or export. */
    private var settingsMessage by mutableStateOf<String?>(null)

    private val importSettingsPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val text = runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        settingsMessage = when {
            text == null -> "Could not read the selected file."
            else -> player.importSettingsJson(text)
                ?.let { "Import failed: $it" }
                ?: "Imported settings."
        }
    }

    private val exportSettingsPicker = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val written = runCatching {
            contentResolver.openOutputStream(uri)
                ?.bufferedWriter()
                ?.use { it.write(player.exportSettingsJson()) }
                ?: error("The selected file could not be opened for writing.")
        }.isSuccess
        settingsMessage = if (written) "Exported settings." else "Could not write the selected file."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        player = AndroidLocalPlayer(
            context = this,
            serviceConnectionOnConnected = {},
            serviceConnectionOnDisconnected = {},
        )

        val persisted = contentResolver.persistedUriPermissions.firstOrNull { it.isReadPermission }
            ?: contentResolver.persistedUriPermissions.firstOrNull()
        if (persisted != null) {
            startFolderScan(persisted.uri)
        }

        setContent {
            val snap by player.snapshotFlow.collectAsState()
            NocturneTheme(snap.settings) {
                Surface(color = Color.Transparent) {
                    MainScreen(
                        snap = snap,
                        onChooseFolder = { openFolderPicker() },
                        onPlay = { player.playSelected(it) },
                        onTogglePlayPause = { player.togglePlayPause() },
                        onSeek = { player.seekTo(it) },
                        onNext = { player.playNext() },
                        onPrevious = { player.playPrevious() },
                        onToggleShuffle = { player.toggleShuffle() },
                        onToggleRepeat = { player.toggleRepeat() },
                        onSkipForward = { player.skipForward(10_000L) },
                        onSkipBackward = { player.skipBackward(10_000L) },
                        onSearch = { player.search(it) },
                        onToggleFavorite = { player.toggleFavorite(it) },
                        onCreatePlaylist = { player.createPlaylist(it) },
                        onDeletePlaylist = { player.deletePlaylist(it) },
                        onAddToPlaylist = { plId, track -> player.addToPlaylist(plId, track) },
                        onPlayPlaylist = { player.playPlaylist(it) },
                        onPauseHistory = { player.pauseHistory() },
                        onResumeHistory = { player.resumeHistory() },
                        onClearHistory = { player.clearHistory() },
                        onUpdateSettings = { player.updateSettings(it) },
                        onImportSettings = {
                            importSettingsPicker.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        onExportSettings = { exportSettingsPicker.launch("nocturne-settings.json") },
                        settingsMessage = settingsMessage,
                        onDismissSettingsMessage = { settingsMessage = null },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(
            this, android.content.ComponentName(this, PlaybackService::class.java)
        )
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                val ctrl = runCatching { future.get() }.getOrNull() ?: return@addListener
                player.attachController(ctrl)
            },
            Executor { command -> runOnUiThread(command) },
        )
    }

    override fun onStop() {
        player.detachController()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        super.onStop()
    }

    override fun onDestroy() {
        player.stop()
        super.onDestroy()
    }

    private fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
            )
        }
        folderPicker.launch(intent)
    }

    private fun startFolderScan(uri: Uri) {
        player.updateSelectedFolder(null)
        lifecycleScope.launch {
            val (tracks, error) = withContext(Dispatchers.IO) { scanFolder(uri) }
            tracks.forEach { player.addToLibrary(it) }
            player.setScanError(error)
        }
    }

    /**
     * Scan [folderUri] recursively for audio files. Returns the tracks found
     * and an optional error string if the scan was aborted early.
     *
     * Bounded: at most [SCAN_MAX_ITEMS] files, at most [SCAN_MAX_MS] wall-clock ms.
     * Depth limit: 10 levels.
     */
    private fun scanFolder(folderUri: Uri): Pair<List<TrackView>, String?> {
        val root = DocumentFile.fromTreeUri(this, folderUri) ?: return Pair(emptyList(), null)
        val tracks = mutableListOf<TrackView>()
        val deadline = System.currentTimeMillis() + SCAN_MAX_MS
        val error = collectAudioFiles(root, tracks, depth = 0, deadline = deadline)
        return Pair(tracks, error)
    }

    private fun collectAudioFiles(
        dir: DocumentFile,
        out: MutableList<TrackView>,
        depth: Int,
        deadline: Long,
    ): String? {
        if (depth > 10) return null
        for (child in dir.listFiles()) {
            if (System.currentTimeMillis() > deadline) {
                return "Scan stopped after ${SCAN_MAX_MS / 1_000}s \u2014 ${out.size} tracks found"
            }
            if (out.size >= SCAN_MAX_ITEMS) {
                return "Scan stopped at $SCAN_MAX_ITEMS tracks \u2014 folder may contain more"
            }
            when {
                child.isDirectory -> {
                    val err = collectAudioFiles(child, out, depth + 1, deadline)
                    if (err != null) return err
                }
                child.isFile -> {
                    val name = child.name ?: continue
                    val mime = child.type ?: continue
                    if (!mime.startsWith("audio/")) continue
                    out.add(readTrackMeta(child.uri, name))
                }
            }
        }
        return null
    }

    /** Read title/artist/album/duration and artwork from a SAF URI using [MediaMetadataRetriever]. */
    private fun readTrackMeta(uri: Uri, fileName: String): TrackView {
        return runCatching {
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(this, uri)
                val title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.takeIf { it.isNotBlank() }
                    ?: fileName.substringBeforeLast('.').ifBlank { fileName }
                val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?.takeIf { it.isNotBlank() }
                val album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    ?.takeIf { it.isNotBlank() }
                val durMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                val picture = mmr.embeddedPicture
                val artPath = if (picture != null) {
                    runCatching {
                        val artDir = File(cacheDir, "artwork").apply { mkdirs() }
                        val hash = uri.toString().hashCode()
                        val artFile = File(artDir, "art_$hash.jpg")
                        if (!artFile.exists() || artFile.length() == 0L) {
                            artFile.writeBytes(picture)
                        }
                        artFile.absolutePath
                    }.getOrNull()
                } else null

                TrackView(
                    id = uri.toString(),
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = durMs,
                    artPath = artPath,
                )
            } finally {
                mmr.release()
            }
        }.getOrElse {
            TrackView(
                id = uri.toString(),
                title = fileName.substringBeforeLast('.').ifBlank { fileName },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    snap: LocalPlayerSnapshot,
    onChooseFolder: () -> Unit,
    onPlay: (TrackView) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onSearch: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onAddToPlaylist: (String, TrackView) -> Unit,
    onPlayPlaylist: (String) -> Unit,
    onPauseHistory: () -> Unit,
    onResumeHistory: () -> Unit,
    onClearHistory: () -> Unit,
    onUpdateSettings: (AppSettings) -> Unit,
    onImportSettings: () -> Unit,
    onExportSettings: () -> Unit,
    settingsMessage: String?,
    onDismissSettingsMessage: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showFullPlayer by rememberSaveable { mutableStateOf(false) }
    val hazeState = remember { HazeState() }
    val ambientColors = rememberArtworkColors(snap.nowPlaying?.artPath)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassTheme.Background)
    ) {
        // 1. Convx ambient mesh backdrop derived from album art
        AndroidMeshBackground(colors = ambientColors)

        // 2. Foreground content layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Convx Liquid Glass Header
            val folderName = snap.library.firstOrNull()?.let {
                Uri.parse(it.id).lastPathSegment?.substringBeforeLast('/')
            } ?: "Music"

            ConvxHeader(
                hazeState = hazeState,
                folderName = folderName,
                trackCount = snap.library.size,
                onChooseFolder = onChooseFolder,
            )

            snap.scanError?.let { err ->
                Text(
                    text = "⚠️ $err",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }

            // Main Tab View with hazeSource for underlying blur
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .hazeSource(hazeState)
            ) {
                when (tab) {
                    0 -> LibraryTab(
                        snap = snap,
                        onChooseFolder = onChooseFolder,
                        onPlay = onPlay,
                        onSearch = onSearch,
                        onToggleFavorite = onToggleFavorite,
                        onAddToPlaylist = onAddToPlaylist,
                    )
                    1 -> PlaylistsTab(
                        snap = snap,
                        onCreatePlaylist = onCreatePlaylist,
                        onDeletePlaylist = onDeletePlaylist,
                        onPlayPlaylist = onPlayPlaylist,
                    )
                    2 -> FavoritesTab(
                        snap = snap,
                        onPlay = onPlay,
                        onToggleFavorite = onToggleFavorite,
                        onBrowseLibrary = { tab = 0 },
                    )
                    3 -> HistoryTab(
                        snap = snap,
                        onPlay = { id ->
                            val track = snap.library.firstOrNull { it.id == id } ?: return@HistoryTab
                            onPlay(track)
                        },
                        onPauseHistory = onPauseHistory,
                        onResumeHistory = onResumeHistory,
                        onClearHistory = onClearHistory,
                    )
                    4 -> AppearanceTab(
                        settings = snap.settings,
                        onUpdate = onUpdateSettings,
                        onImport = onImportSettings,
                        onExport = onExportSettings,
                    )
                }
            }

            // Bottom Dock: Floating Mini Player + Liquid Glass Bottom Nav
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (snap.nowPlaying != null) {
                    ConvxMiniPlayer(
                        hazeState = hazeState,
                        snap = snap,
                        isFavorite = snap.favorites.contains(snap.nowPlaying?.id ?: ""),
                        onTogglePlayPause = onTogglePlayPause,
                        onToggleFavorite = {
                            snap.nowPlaying?.let { onToggleFavorite(it.id) }
                        },
                        onClick = { showFullPlayer = true },
                    )
                }

                ConvxBottomNavBar(
                    hazeState = hazeState,
                    selectedTab = tab,
                    onSelectTab = { tab = it },
                )
            }
        }

        // Full Player Modal Dialog
        if (showFullPlayer && snap.nowPlaying != null) {
            ConvxFullPlayerDialog(
                hazeState = hazeState,
                snap = snap,
                isFavorite = snap.favorites.contains(snap.nowPlaying?.id ?: ""),
                onDismiss = { showFullPlayer = false },
                onTogglePlayPause = onTogglePlayPause,
                onSeek = onSeek,
                onNext = onNext,
                onPrevious = onPrevious,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat,
                onSkipForward = onSkipForward,
                onSkipBackward = onSkipBackward,
                onToggleFavorite = {
                    snap.nowPlaying?.let { onToggleFavorite(it.id) }
                },
            )
        }

        settingsMessage?.let { message ->
            AlertDialog(
                onDismissRequest = onDismissSettingsMessage,
                title = { Text("Appearance") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = onDismissSettingsMessage) { Text("OK") }
                },
            )
        }
    }
}

@Composable
private fun ConvxHeader(
    hazeState: HazeState,
    folderName: String,
    trackCount: Int,
    onChooseFolder: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .liquidGlass(
                hazeState = hazeState,
                shape = RoundedCornerShape(18.dp),
                backgroundColor = Color(0x35111728),
                elevation = 4.dp,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF101723))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.never.nocturne.R.drawable.nocturne_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            }
            Column {
                Text(
                    text = "Nocturne",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GlassTheme.TextPrimary,
                    letterSpacing = 0.5.sp,
                )
                Text(
                    text = if (trackCount > 0) "$folderName • $trackCount tracks" else "No folder selected",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassTheme.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x25FFFFFF))
                .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(20.dp))
                .clickable { onChooseFolder() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = PlayerIcons.Folder,
                contentDescription = "Folder",
                tint = GlassTheme.Accent,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Folder",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = GlassTheme.TextPrimary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ConvxBottomNavBar(
    hazeState: HazeState,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
) {
    // Keep destination names clear across phone sizes.
    val items = listOf(
        Pair(PlayerIcons.MusicNote, "Library"),
        Pair(PlayerIcons.Playlists, "Playlists"),
        Pair(PlayerIcons.Favorite, "Favorites"),
        Pair(PlayerIcons.History, "History"),
        Pair(PlayerIcons.Sliders, "Settings"),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                hazeState = hazeState,
                shape = RoundedCornerShape(30.dp),
                backgroundColor = Color(0x650D1322),
                elevation = 8.dp,
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, (icon, label) ->
                val isSelected = selectedTab == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectTab(index) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) GlassTheme.Accent else GlassTheme.TextMuted,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) GlassTheme.Accent else GlassTheme.TextMuted,
                        maxLines = 1,
                    )
                    // Active dot indicator
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 5.dp else 0.dp)
                            .clip(CircleShape)
                            .background(GlassTheme.Accent),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConvxMiniPlayer(
    hazeState: HazeState,
    snap: LocalPlayerSnapshot,
    isFavorite: Boolean,
    onTogglePlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
) {
    val now = snap.nowPlaying ?: return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                hazeState = hazeState,
                shape = RoundedCornerShape(20.dp),
                backgroundColor = Color(0x6510172A),
                elevation = 10.dp,
            )
            .clickable { onClick() },
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Artwork thumbnail with Coil AsyncImage or fallback
                val artPath = now.artPath
                if (!artPath.isNullOrBlank() && File(artPath).exists()) {
                    AsyncImage(
                        model = File(artPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(12.dp)),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF243048), Color(0xFF151B27))
                                )
                            )
                            .border(1.dp, Color(0x25FFFFFF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PlayerIcons.MusicNote,
                            contentDescription = null,
                            tint = GlassTheme.Accent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = now.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = GlassTheme.TextPrimary,
                    )
                    Text(
                        text = now.artist ?: "Local Track",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTheme.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = if (isFavorite) PlayerIcons.Favorite else PlayerIcons.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) GlassTheme.Accent else GlassTheme.TextSecondary,
                            modifier = Modifier.size(19.dp),
                        )
                    }

                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(GlassTheme.Accent),
                    ) {
                        Icon(
                            imageVector = if (snap.isPlaying) PlayerIcons.Pause else PlayerIcons.Play,
                            contentDescription = if (snap.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // Real-time micro progress indicator at the bottom
            val progress = if (snap.durationMs > 0) {
                (snap.positionMs.toFloat() / snap.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color(0x20FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(GlassTheme.Accent, GlassTheme.AccentPurple)
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun ConvxFullPlayerDialog(
    hazeState: HazeState,
    snap: LocalPlayerSnapshot,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val now = snap.nowPlaying ?: return
    val ambientColors = rememberArtworkColors(now.artPath)
    val albumName = now.album.orEmpty()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GlassTheme.Background)
        ) {
            // Ambient dynamic mesh behind full player
            AndroidMeshBackground(colors = ambientColors)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Drag handle pill at the top
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0x40FFFFFF))
                        .clickable { onDismiss() }
                )

                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0x25FFFFFF)),
                    ) {
                        Icon(
                            imageVector = PlayerIcons.Close,
                            contentDescription = "Close",
                            tint = GlassTheme.TextPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = "NOW PLAYING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp,
                        color = GlassTheme.TextSecondary,
                    )
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isFavorite) GlassTheme.Accent.copy(alpha = 0.25f) else Color(0x25FFFFFF)),
                    ) {
                        Icon(
                            imageVector = if (isFavorite) PlayerIcons.Favorite else PlayerIcons.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) GlassTheme.Accent else GlassTheme.TextPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // Big Album Artwork Card with ambient shadow & specular border
                val fullArtPath = now.artPath
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 360.dp)
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f)
                            .shadow(30.dp, RoundedCornerShape(30.dp), spotColor = ambientColors.firstOrNull() ?: GlassTheme.AccentGlow)
                            .clip(RoundedCornerShape(30.dp))
                            .background(Color(0x35141D30))
                            .border(1.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(30.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!fullArtPath.isNullOrBlank() && File(fullArtPath).exists()) {
                            AsyncImage(
                                model = File(fullArtPath),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF171E2E),
                                                Color(0xFF101722),
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = PlayerIcons.MusicNote,
                                    contentDescription = null,
                                    tint = GlassTheme.Accent,
                                    modifier = Modifier.size(88.dp),
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(14.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0x7A0F172A))
                                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(GlassTheme.Accent),
                                )
                                Text(
                                    text = "Now playing",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp,
                                    color = GlassTheme.TextPrimary,
                                )
                            }
                        }
                    }
                }

                // Track Title & Artist
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 18.dp),
                ) {
                    Text(
                        text = now.title,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = GlassTheme.TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = now.artist ?: "Local Track",
                        fontSize = 15.sp,
                        color = GlassTheme.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    if (albumName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = albumName,
                            fontSize = 12.sp,
                            color = GlassTheme.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x1AFFFFFF).copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Queue",
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GlassTheme.TextSecondary,
                        )
                        Text(
                            text = if (snap.queue.isNotEmpty()) "${snap.queue.size} up next" else "empty",
                            fontSize = 11.sp,
                            color = GlassTheme.TextMuted,
                        )
                    }
                }

                // Progress Bar & Duration Labels
                Column(modifier = Modifier.fillMaxWidth()) {
                    val progressFraction = if (snap.durationMs > 0) {
                        (snap.positionMs.toFloat() / snap.durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    CupertinoSeekSlider(
                        value = progressFraction,
                        onValueChange = { fraction ->
                            onSeek((fraction * snap.durationMs).toLong())
                        },
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = formatMs(snap.positionMs),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlassTheme.TextMuted,
                        )
                        Text(
                            text = formatMs(snap.durationMs),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlassTheme.TextMuted,
                        )
                    }
                }

                // Transport Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = PlayerIcons.SkipPrev,
                            contentDescription = "Previous",
                            tint = GlassTheme.TextPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    CupertinoGlassButton(
                        onClick = onTogglePlayPause,
                        size = 68.dp,
                        icon = if (snap.isPlaying) PlayerIcons.Pause else PlayerIcons.Play,
                        iconSize = 30.dp,
                        iconTint = Color.White,
                        backgroundColor = GlassTheme.Accent,
                        glowColor = GlassTheme.Accent,
                    )

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = PlayerIcons.SkipNext,
                            contentDescription = "Next",
                            tint = GlassTheme.TextPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                // Bottom Pills Row: Shuffle, -10s, +10s, Repeat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CupertinoPillToggle(
                        active = snap.isShuffle,
                        onClick = onToggleShuffle,
                        icon = PlayerIcons.Shuffle,
                    )

                    CupertinoPillToggle(
                        active = false,
                        onClick = onSkipBackward,
                        icon = PlayerIcons.SkipPrev,
                        label = "-10s",
                    )

                    CupertinoPillToggle(
                        active = false,
                        onClick = onSkipForward,
                        icon = PlayerIcons.SkipNext,
                        label = "+10s",
                    )

                    CupertinoPillToggle(
                        active = snap.isRepeat,
                        onClick = onToggleRepeat,
                        icon = PlayerIcons.Repeat,
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryTab(
    snap: LocalPlayerSnapshot,
    onChooseFolder: () -> Unit,
    onPlay: (TrackView) -> Unit,
    onSearch: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAddToPlaylist: (String, TrackView) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf<TrackView?>(null) }
    val currentTrackId = snap.nowPlaying?.id

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        // Convx Style Search Pill
        OutlinedTextField(
            value = snap.searchQuery,
            onValueChange = onSearch,
            placeholder = { Text("Search songs, artists, albums...", color = GlassTheme.TextMuted, fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = PlayerIcons.Search,
                    contentDescription = "Search",
                    tint = GlassTheme.TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            },
            trailingIcon = {
                if (snap.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearch("") }) {
                        Icon(
                            imageVector = PlayerIcons.Close,
                            contentDescription = "Clear search",
                            tint = GlassTheme.TextMuted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0x35141D30),
                unfocusedContainerColor = Color(0x20141D30),
                focusedBorderColor = GlassTheme.Accent.copy(alpha = 0.6f),
                unfocusedBorderColor = Color(0x22FFFFFF),
                cursorColor = GlassTheme.Accent,
                focusedTextColor = GlassTheme.TextPrimary,
                unfocusedTextColor = GlassTheme.TextPrimary,
                focusedLeadingIconColor = GlassTheme.Accent,
                unfocusedLeadingIconColor = GlassTheme.TextMuted,
                focusedTrailingIconColor = GlassTheme.TextMuted,
                unfocusedTrailingIconColor = GlassTheme.TextMuted,
                focusedPlaceholderColor = GlassTheme.TextMuted,
                unfocusedPlaceholderColor = GlassTheme.TextMuted,
            ),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )

        val visible = if (snap.searchQuery.isBlank()) snap.library else snap.filteredLibrary
        val recentTracks = if (snap.searchQuery.isBlank()) {
            snap.history
                .mapNotNull { entry -> snap.library.firstOrNull { it.id == entry.trackId } }
                .distinctBy { it.id }
                .take(8)
        } else {
            emptyList()
        }

        if (visible.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (snap.library.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF101823).copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 28.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFF121C2C))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = PlayerIcons.MusicNote,
                                    contentDescription = null,
                                    tint = GlassTheme.Accent,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Text(
                                text = "Your library starts here",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GlassTheme.TextPrimary,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "Choose a music folder to add your tracks.",
                                color = GlassTheme.TextSecondary,
                                textAlign = TextAlign.Center,
                            )
                            Button(
                                onClick = onChooseFolder,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GlassTheme.Accent,
                                    contentColor = Color.White,
                                ),
                            ) {
                                Text("Choose music folder")
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF121924).copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No results for \u201c${snap.searchQuery}\u201d",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        } else {
            if (recentTracks.isNotEmpty()) {
                val featuredTrack = recentTracks.first()
                val compactRecents = recentTracks.drop(1)

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Recently played",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = GlassTheme.TextPrimary,
                        )
                        TextButton(onClick = onChooseFolder) {
                            Text("Browse folder", color = GlassTheme.Accent)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF0F1728).copy(alpha = 0.96f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlay(featuredTrack) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                GlassTheme.Accent.copy(alpha = 0.35f),
                                                Color(0xFF121B2A),
                                            )
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                val artPath = featuredTrack.artPath
                                if (!artPath.isNullOrBlank() && File(artPath).exists()) {
                                    AsyncImage(
                                        model = File(artPath),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Icon(
                                        imageVector = PlayerIcons.MusicNote,
                                        contentDescription = null,
                                        tint = GlassTheme.Accent,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Text(
                                    text = "Resume listening",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GlassTheme.TextSecondary,
                                    letterSpacing = 0.8.sp,
                                )
                                Text(
                                    text = featuredTrack.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GlassTheme.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = featuredTrack.artist ?: "Unknown Artist",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GlassTheme.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = formatMs(featuredTrack.durationMs),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GlassTheme.TextMuted,
                                )
                            }

                            Button(
                                onClick = { onPlay(featuredTrack) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GlassTheme.Accent,
                                    contentColor = Color.White,
                                ),
                                modifier = Modifier.widthIn(min = 84.dp),
                            ) {
                                Text("Play")
                            }
                        }
                    }

                    if (compactRecents.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(compactRecents, key = { it.id }) { track ->
                                Column(
                                    modifier = Modifier
                                        .width(124.dp)
                                        .clickable { onPlay(track) },
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        GlassTheme.Accent.copy(alpha = 0.22f),
                                                        Color(0xFF121B2A),
                                                    )
                                                )
                                            )
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        val artPath = track.artPath
                                        if (!artPath.isNullOrBlank() && File(artPath).exists()) {
                                            AsyncImage(
                                                model = File(artPath),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        } else {
                                            Icon(
                                                imageVector = PlayerIcons.MusicNote,
                                                contentDescription = null,
                                                tint = GlassTheme.Accent,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GlassTheme.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = track.artist ?: "Unknown Artist",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GlassTheme.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (snap.searchQuery.isBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val stats = listOf(
                        "${visible.size} tracks" to "Library",
                        "${snap.favorites.size} favorites" to "Loved",
                        "${snap.history.size} recent" to "History",
                    )
                    stats.forEach { (value, label) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF121924).copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GlassTheme.TextPrimary,
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GlassTheme.TextSecondary,
                                )
                            }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visible, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        isFavorite = snap.favorites.contains(track.id),
                        isPlayingThis = track.id == currentTrackId,
                        onPlay = { onPlay(track) },
                        onToggleFavorite = { onToggleFavorite(track.id) },
                        onAddToPlaylist = if (snap.playlists.isNotEmpty()) {
                            { showAddDialog = track }
                        } else null,
                    )
                }
            }
        }
    }

    showAddDialog?.let { track ->
        AddToPlaylistDialog(
            playlists = snap.playlists,
            onDismiss = { showAddDialog = null },
            onConfirm = { plId ->
                onAddToPlaylist(plId, track)
                showAddDialog = null
            },
        )
    }
}

@Composable
private fun TrackRow(
    track: TrackView,
    isFavorite: Boolean,
    isPlayingThis: Boolean = false,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: (() -> Unit)?,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlayingThis) Color(0xFF141C2E).copy(alpha = 0.88f)
            else Color(0xFF121924).copy(alpha = 0.7f),
        ),
        border = BorderStroke(
            1.dp,
            if (isPlayingThis) GlassTheme.Accent.copy(alpha = 0.45f)
            else Color.White.copy(alpha = 0.06f)
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isPlayingThis) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 28.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(GlassTheme.Accent),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            // Artwork square
            val rowArt = track.artPath
            if (!rowArt.isNullOrBlank() && File(rowArt).exists()) {
                AsyncImage(
                    model = File(rowArt),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(10.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    if (isPlayingThis) GlassTheme.Accent.copy(alpha = 0.35f)
                                    else Color(0xFF222B3D),
                                    Color(0xFF141926),
                                )
                            )
                        )
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPlayingThis) PlayerIcons.Play else PlayerIcons.MusicNote,
                        contentDescription = null,
                        tint = if (isPlayingThis) GlassTheme.Accent else GlassTheme.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = GlassTheme.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                track.artist?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTheme.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) PlayerIcons.Favorite else PlayerIcons.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) GlassTheme.Accent else GlassTheme.TextMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (onAddToPlaylist != null) {
                    IconButton(onClick = onAddToPlaylist) {
                        Icon(
                            imageVector = PlayerIcons.Plus,
                            contentDescription = "Add to playlist",
                            tint = GlassTheme.TextMuted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistsTab(
    snap: LocalPlayerSnapshot,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onPlayPlaylist: (String) -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Playlists (${snap.playlists.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.clickable { showCreate = true },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = PlayerIcons.Plus,
                        contentDescription = "New",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (snap.playlists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No playlists yet \u2014 tap New to create one",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(snap.playlists, key = { it.id }) { pl ->
                    PlaylistRow(
                        pl = pl,
                        onPlay = { onPlayPlaylist(pl.id) },
                        onDelete = { onDeletePlaylist(pl.id) },
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreatePlaylistDialog(
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreate = false
            },
        )
    }
}

@Composable
private fun PlaylistRow(pl: Playlist, onPlay: () -> Unit, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF131824).copy(alpha = 0.6f),
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF222B3D), Color(0xFF141926))
                            )
                        )
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PlayerIcons.Playlists,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = pl.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${pl.tracks.size} track${if (pl.tracks.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun FavoritesTab(
    snap: LocalPlayerSnapshot,
    onPlay: (TrackView) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onBrowseLibrary: () -> Unit,
) {
    val favTracks = snap.library.filter { snap.favorites.contains(it.id) }
    val currentTrackId = snap.nowPlaying?.id

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (favTracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF101823).copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF121C2C))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PlayerIcons.Favorite,
                                contentDescription = null,
                                tint = GlassTheme.Accent,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Text(
                            text = "No favorites yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GlassTheme.TextPrimary,
                        )
                        Text(
                            text = "Save tracks from your library to find them here.",
                            color = GlassTheme.TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                        Button(
                            onClick = onBrowseLibrary,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GlassTheme.Accent,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text("Browse library")
                        }
                    }
                }
            }
        } else {
            Text(
                text = "${favTracks.size} favorite${if (favTracks.size != 1) "s" else ""}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(favTracks, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        isFavorite = true,
                        isPlayingThis = track.id == currentTrackId,
                        onPlay = { onPlay(track) },
                        onToggleFavorite = { onToggleFavorite(track.id) },
                        onAddToPlaylist = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(
    snap: LocalPlayerSnapshot,
    onPlay: (String) -> Unit,
    onPauseHistory: () -> Unit,
    onResumeHistory: () -> Unit,
    onClearHistory: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (snap.isHistoryPaused) {
                    TextButton(onClick = onResumeHistory) { Text("Resume") }
                } else {
                    TextButton(onClick = onPauseHistory) { Text("Pause") }
                }
                TextButton(onClick = onClearHistory) { Text("Clear") }
            }
        }
        if (snap.isHistoryPaused) {
            Text(
                text = "History recording paused",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        if (snap.history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF121924).copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                    ) {
                        Icon(
                            imageVector = PlayerIcons.History,
                            contentDescription = null,
                            tint = GlassTheme.Accent,
                            modifier = Modifier.size(32.dp),
                        )
                        Text(
                            text = "No history yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = GlassTheme.TextPrimary,
                        )
                        Text(
                            text = "Play a few tracks and your listening history will appear here.",
                            color = GlassTheme.TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(snap.history, key = { "${it.trackId}-${it.playedAtMs}" }) { entry ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF131824).copy(alpha = 0.5f),
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlay(entry.trackId) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(GlassTheme.Accent.copy(alpha = 0.32f), Color(0xFF1B2232))
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = PlayerIcons.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                                entry.artist?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

// ---------------------------------------------------------------------------
// Dialogs
// ---------------------------------------------------------------------------

@Composable
private fun CreatePlaylistDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist Name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(playlists, key = { it.id }) { pl ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(pl.id) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = PlayerIcons.Playlists,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = pl.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ---------------------------------------------------------------------------
// Appearance (customization phase)
// See docs/customization-spec.md
// ---------------------------------------------------------------------------

@Composable
private fun AppearanceSection(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceTab(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val activeSlug = ThemePresets.bySlug(settings.preset).slug
    val intensity = settings.glassIntensity.coerceIn(0, 100)
    val tier = when {
        intensity <= 24 -> "Solid"
        intensity <= 49 -> "Frosted"
        intensity <= 74 -> "Crystal"
        else -> "Vapor"
    }

    var hexDraft by remember(settings.accent, settings.preset) {
        mutableStateOf(formatHex(parseHex(settings.accent) ?: ThemePresets.bySlug(settings.preset).accent))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppearanceSection("PRESET")
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemePresets.all.forEach { preset ->
                val selected = preset.slug == activeSlug
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            width = 1.dp,
                            color = if (selected) colorOf(preset.accent) else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(10.dp),
                        )
                        .clickable { onUpdate(settings.copy(preset = preset.slug, accent = null)) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colorOf(preset.accent)),
                    )
                    Text(
                        preset.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }

        AppearanceSection("ACCENT")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemePresets.all.forEach { preset ->
                val current = formatHex(parseHex(settings.accent) ?: ThemePresets.bySlug(settings.preset).accent) ==
                    formatHex(preset.accent)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colorOf(preset.accent))
                        .border(
                            width = if (current) 2.dp else 1.dp,
                            color = if (current) Color.White else MaterialTheme.colorScheme.outline,
                            shape = CircleShape,
                        )
                        .clickable { onUpdate(settings.copy(accent = formatHex(preset.accent))) },
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = hexDraft,
                onValueChange = { hexDraft = it },
                singleLine = true,
                label = { Text("#RRGGBB") },
                modifier = Modifier.width(150.dp),
            )
            TextButton(onClick = {
                val parsed = parseHex(hexDraft)
                if (parsed != null) onUpdate(settings.copy(accent = formatHex(parsed)))
            }) { Text("Apply", color = accent) }
            if (settings.accent != null) {
                TextButton(onClick = { onUpdate(settings.copy(accent = null)) }) {
                    Text("Reset", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        AppearanceSection("GLASS \u00b7 $tier \u00b7 $intensity")
        Slider(
            value = intensity.toFloat(),
            onValueChange = { onUpdate(settings.copy(glassIntensity = it.roundToInt())) },
            valueRange = 0f..100f,
        )
        Text(
            "0 is solid; 100 is the most see-through.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AppearanceSection("SETTINGS FILE")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onExport) { Text("Export") }
            Button(onClick = onImport) { Text("Import") }
        }
        Text(
            "One JSON file holds preset, accent and glass level.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 0xAARRGGBB from the shared settings model to a Compose colour. */
private fun colorOf(argb: Long): Color = Color(argb)
