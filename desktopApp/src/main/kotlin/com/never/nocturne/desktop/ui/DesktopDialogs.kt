package com.never.nocturne.desktop.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.never.nocturne.core.EqPreset
import com.never.nocturne.core.EqualizerState
import com.never.nocturne.core.LrcParser
import com.never.nocturne.desktop.GlassColors
import com.never.nocturne.desktop.glassPanel
import kotlinx.coroutines.launch
import java.io.File

/**
 * Desktop Liquid Glass Equalizer Dialog.
 */
@Composable
fun DesktopEqualizerDialog(
    state: EqualizerState,
    onDismiss: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (EqPreset) -> Unit,
    onSetBassBoost: (Int) -> Unit,
    onSetBandGain: (Int, Int) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(460.dp)
                    .clickable(enabled = false) {}
                    .glassPanel(
                        cornerRadius = 24.dp,
                        backgroundColor = Color(0xDC111624),
                        elevation = 24.dp,
                    )
                    .padding(24.dp),
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "AUDIO EQUALIZER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.6.sp,
                                color = GlassColors.TextSecondary,
                            )
                            Text(
                                text = "Sound Profiles",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassColors.TextPrimary,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = if (state.isEnabled) "ON" else "OFF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isEnabled) GlassColors.Accent else GlassColors.TextMuted,
                            )
                            Switch(
                                checked = state.isEnabled,
                                onCheckedChange = onToggleEnabled,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = GlassColors.Accent,
                                    uncheckedThumbColor = GlassColors.TextMuted,
                                    uncheckedTrackColor = Color(0x30FFFFFF),
                                ),
                            )
                        }
                    }

                    // Presets
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "PRESETS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp,
                            color = GlassColors.TextMuted,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            EqPreset.entries.forEach { preset ->
                                val isSelected = state.preset == preset
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) GlassColors.Accent.copy(alpha = 0.25f)
                                            else Color(0x15FFFFFF)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) GlassColors.Accent else Color(0x20FFFFFF),
                                            RoundedCornerShape(10.dp),
                                        )
                                        .clickable(enabled = state.isEnabled) { onSelectPreset(preset) }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = preset.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = when {
                                            !state.isEnabled -> GlassColors.TextMuted.copy(alpha = 0.5f)
                                            isSelected -> GlassColors.Accent
                                            else -> GlassColors.TextPrimary
                                        },
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }

                    // Bass Boost
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "BASS BOOST",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp,
                                color = GlassColors.TextMuted,
                            )
                            Text(
                                text = "${state.bassBoost}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isEnabled) GlassColors.Accent else GlassColors.TextMuted,
                            )
                        }

                        Slider(
                            value = state.bassBoost.toFloat(),
                            onValueChange = { onSetBassBoost(it.toInt()) },
                            valueRange = 0f..100f,
                            enabled = state.isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = GlassColors.Accent,
                                activeTrackColor = GlassColors.Accent,
                                inactiveTrackColor = Color(0x20FFFFFF),
                            ),
                        )
                    }

                    // Frequency Bands
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "FREQUENCY BANDS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp,
                            color = GlassColors.TextMuted,
                        )

                        state.bands.forEach { band ->
                            val freqLabel = when {
                                band.centerFreqHz >= 1000 -> "${band.centerFreqHz / 1000} kHz"
                                else -> "${band.centerFreqHz} Hz"
                            }
                            val sign = if (band.gainDb > 0) "+" else ""

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = freqLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (state.isEnabled) GlassColors.TextSecondary else GlassColors.TextMuted,
                                    modifier = Modifier.width(58.dp),
                                )

                                Slider(
                                    value = band.gainDb.toFloat(),
                                    onValueChange = { onSetBandGain(band.index, it.toInt()) },
                                    valueRange = band.minGainDb.toFloat()..band.maxGainDb.toFloat(),
                                    steps = 23,
                                    enabled = state.isEnabled,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = GlassColors.Accent,
                                        activeTrackColor = GlassColors.Accent,
                                        inactiveTrackColor = Color(0x20FFFFFF),
                                    ),
                                )

                                Text(
                                    text = "$sign${band.gainDb} dB",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.isEnabled) GlassColors.TextPrimary else GlassColors.TextMuted,
                                    modifier = Modifier.width(46.dp),
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlassColors.Accent,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text("Done", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Desktop Liquid Glass Synchronized Lyrics Dialog.
 */
@Composable
fun DesktopLyricsDialog(
    trackPath: String?,
    trackTitle: String,
    artist: String?,
    positionMs: Long,
    onDismiss: () -> Unit,
) {
    val lines = remember(trackPath) {
        if (trackPath.isNullOrBlank()) return@remember emptyList()
        val lrc = File(trackPath.replaceAfterLast('.', "lrc"))
        if (lrc.exists()) LrcParser.parse(lrc.readText()) else emptyList()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(520.dp)
                    .height(600.dp)
                    .clickable(enabled = false) {}
                    .glassPanel(
                        cornerRadius = 24.dp,
                        backgroundColor = Color(0xEC101524),
                        elevation = 24.dp,
                    )
                    .padding(24.dp),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SYNCHRONIZED LYRICS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.6.sp,
                                color = GlassColors.TextSecondary,
                            )
                            Text(
                                text = trackTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassColors.TextPrimary,
                                maxLines = 1,
                            )
                            if (!artist.isNullOrBlank()) {
                                Text(
                                    text = artist,
                                    fontSize = 12.sp,
                                    color = GlassColors.TextMuted,
                                    maxLines = 1,
                                )
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x20FFFFFF),
                                contentColor = GlassColors.TextPrimary,
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text("Close", fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (lines.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No lyrics file found for this track.\nPlace a matching .lrc file in the same folder.",
                                color = GlassColors.TextMuted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                            )
                        }
                    } else {
                        val activeIndex = LrcParser.activeIndex(lines, positionMs)
                        val listState = rememberLazyListState()
                        val scope = rememberCoroutineScope()

                        LaunchedEffect(activeIndex) {
                            if (activeIndex >= 0) {
                                scope.launch {
                                    listState.animateScrollToItem(
                                        index = (activeIndex - 3).coerceAtLeast(0),
                                    )
                                }
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 40.dp, horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            itemsIndexed(lines, key = { i, _ -> i }) { index, line ->
                                val isActive = index == activeIndex
                                val isPast = index < activeIndex
                                val targetColor = when {
                                    isActive -> GlassColors.TextPrimary
                                    isPast -> GlassColors.TextMuted.copy(alpha = 0.5f)
                                    else -> GlassColors.TextMuted.copy(alpha = 0.3f)
                                }
                                val color by animateColorAsState(
                                    targetValue = targetColor,
                                    animationSpec = tween(durationMillis = 300),
                                    label = "desktopLyricColor",
                                )

                                Text(
                                    text = line.text.ifBlank { "·" },
                                    color = color,
                                    fontSize = if (isActive) 20.sp else 16.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 26.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
