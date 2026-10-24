package com.never.localmusic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.never.localmusic.core.PlayerProbe
import com.never.localmusic.core.PlayerSnapshot

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

@Composable
fun SpikeScreen(probe: PlayerProbe?, onPickFile: () -> Unit) {
    val snapshot: PlayerSnapshot = probe?.snapshot?.collectAsState()?.value
        ?: PlayerSnapshot(status = "No probe on this platform")

    Surface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Phase 0 playback spike", style = MaterialTheme.typography.titleLarge)

            Button(onClick = onPickFile) { Text("Choose audio file") }

            Text(snapshot.fileName ?: "-", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)

            snapshot.meta?.let { meta ->
                Column {
                    Text("Title: ${meta.title ?: snapshot.fileName ?: "unknown"}")
                    Text("Artist: ${meta.artist ?: "unknown"}")
                    Text("Album: ${meta.album ?: "unknown"}")
                }
            }

            Text(
                "${formatMs(snapshot.positionMs)} / ${formatMs(snapshot.durationMs)}",
                fontFamily = FontFamily.Monospace,
            )

            if (snapshot.durationMs > 0) {
                Slider(
                    value = snapshot.positionMs.toFloat(),
                    onValueChange = { probe?.seekTo(it.toLong()) },
                    valueRange = 0f..snapshot.durationMs.toFloat(),
                    enabled = probe != null,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { probe?.togglePlayPause() }, enabled = snapshot.isReady) {
                    Text(if (snapshot.isPlaying) "Pause" else "Play")
                }
                OutlinedButton(
                    onClick = { probe?.seekTo((snapshot.positionMs - 10_000L).coerceAtLeast(0)) },
                    enabled = snapshot.isReady,
                ) { Text("Back 10s") }
                OutlinedButton(
                    onClick = { probe?.seekTo((snapshot.positionMs + 10_000L).coerceIn(0, snapshot.durationMs)) },
                    enabled = snapshot.isReady,
                ) { Text("Fwd 10s") }
            }

            Text(snapshot.error ?: snapshot.status, style = MaterialTheme.typography.bodySmall)
        }
    }
}
