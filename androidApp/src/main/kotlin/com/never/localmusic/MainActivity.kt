package com.never.localmusic

import android.app.Activity
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.never.localmusic.android.AndroidLocalPlayer
import com.never.localmusic.core.TrackView
import com.never.localmusic.shared.LocalPlayerSnapshot
import com.never.localmusic.shared.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        player = AndroidLocalPlayer(
            context = this,
            serviceConnectionOnConnected = {},
            serviceConnectionOnDisconnected = {},
        )

        val persisted = contentResolver.persistedUriPermissions.firstOrNull()
        if (persisted != null) {
            startFolderScan(persisted.uri)
        }

        setContent {
            MaterialTheme {
                Surface {
                    val snap by player.snapshotFlow.collectAsState()
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

    /** Read title/artist/album/duration from a SAF URI using [MediaMetadataRetriever]. */
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
                TrackView(id = uri.toString(), title = title, artist = artist, album = album,
                    durationMs = durMs)
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
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Library", "Playlists", "Favorites", "History")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local Music") },
                actions = {
                    TextButton(onClick = onChooseFolder) { Text("Folder") }
                },
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            // Now-playing strip
            NowPlayingStrip(
                snap = snap,
                onTogglePlayPause = onTogglePlayPause,
                onSeek = onSeek,
                onNext = onNext,
                onPrevious = onPrevious,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat,
                onSkipForward = onSkipForward,
                onSkipBackward = onSkipBackward,
            )

            snap.scanError?.let { err ->
                Text(
                    text = "\u26a0\ufe0f $err",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            PrimaryTabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                }
            }

            when (tab) {
                0 -> LibraryTab(
                    snap = snap,
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
            }
        }
    }
}

@Composable
private fun NowPlayingStrip(
    snap: LocalPlayerSnapshot,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
) {
    val now = snap.nowPlaying
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (now != null) {
            Text(now.title, style = MaterialTheme.typography.titleSmall)
            now.artist?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (snap.durationMs > 0) {
                Slider(
                    value = snap.positionMs.toFloat(),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..snap.durationMs.toFloat(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatMs(snap.positionMs), style = MaterialTheme.typography.labelSmall)
                    Text(formatMs(snap.durationMs), style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            Text("No track playing", style = MaterialTheme.typography.bodySmall)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onSkipBackward) { Text("\u00ab10s") }
            TextButton(onClick = onPrevious) { Text("\u23ee") }
            Button(onClick = onTogglePlayPause) {
                Text(if (snap.isPlaying) "\u23f8" else "\u25b6")
            }
            TextButton(onClick = onNext) { Text("\u23ed") }
            TextButton(onClick = onSkipForward) { Text("10s\u00bb") }
            TextButton(onClick = onToggleShuffle) {
                Text(if (snap.isShuffle) "Shuf\u2713" else "Shuf")
            }
            TextButton(onClick = onToggleRepeat) {
                Text(if (snap.isRepeat) "Rep\u2713" else "Rep")
            }
        }
    }
}

@Composable
private fun LibraryTab(
    snap: LocalPlayerSnapshot,
    onPlay: (TrackView) -> Unit,
    onSearch: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAddToPlaylist: (String, TrackView) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf<TrackView?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = snap.searchQuery,
            onValueChange = onSearch,
            label = { Text("Search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        val visible = if (snap.searchQuery.isBlank()) snap.library else snap.filteredLibrary
        if (visible.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (snap.library.isEmpty())
                        "No tracks \u2014 tap Folder to add music"
                    else
                        "No results for \u201c${snap.searchQuery}\u201d"
                )
            }
        } else {
            Text(
                "${visible.size} track${if (visible.size != 1) "s" else ""}",
                style = MaterialTheme.typography.labelLarge,
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(visible, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        isFavorite = snap.favorites.contains(track.id),
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
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.bodyMedium)
            track.artist?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        TextButton(onClick = onToggleFavorite) {
            Text(if (isFavorite) "\u2665" else "\u2661")
        }
        if (onAddToPlaylist != null) {
            TextButton(onClick = onAddToPlaylist) { Text("+PL") }
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Playlists (${snap.playlists.size})", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = { showCreate = true }) { Text("New") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(snap.playlists, key = { it.id }) { pl ->
                PlaylistRow(
                    pl = pl,
                    onPlay = { onPlayPlaylist(pl.id) },
                    onDelete = { onDeletePlaylist(pl.id) },
                )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(pl.name, style = MaterialTheme.typography.bodyMedium)
            Text("${pl.tracks.size} track${if (pl.tracks.size != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onDelete) { Text("Del") }
    }
}

@Composable
private fun FavoritesTab(
    snap: LocalPlayerSnapshot,
    onPlay: (TrackView) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    val favTracks = snap.library.filter { snap.favorites.contains(it.id) }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (favTracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center) {
                Text("No favorites yet \u2014 tap \u2661 in the library")
            }
        } else {
            Text("${favTracks.size} favorite${if (favTracks.size != 1) "s" else ""}",
                style = MaterialTheme.typography.labelLarge)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(favTracks, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        isFavorite = true,
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("History", style = MaterialTheme.typography.titleSmall)
            if (snap.isHistoryPaused) {
                TextButton(onClick = onResumeHistory) { Text("Resume") }
            } else {
                TextButton(onClick = onPauseHistory) { Text("Pause") }
            }
            TextButton(onClick = onClearHistory) { Text("Clear") }
        }
        if (snap.isHistoryPaused) {
            Text("History recording paused",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary)
        }
        if (snap.history.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center) {
                Text("No history yet")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(snap.history, key = { "${it.trackId}-${it.playedAtMs}" }) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlay(entry.trackId) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.title, style = MaterialTheme.typography.bodyMedium)
                            entry.artist?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
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
        title = { Text("New Playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank(),
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
        title = { Text("Add to Playlist") },
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
    )
}
