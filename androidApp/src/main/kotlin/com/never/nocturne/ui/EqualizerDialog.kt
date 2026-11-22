package com.never.nocturne.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.never.nocturne.PlayerIcons
import com.never.nocturne.core.EqPreset
import com.never.nocturne.core.EqualizerState
import dev.chrisbanes.haze.HazeState

/**
 * Liquid Glass Audio Equalizer Dialog.
 *
 * Features:
 * - Master enable/disable toggle
 * - Preset selector chips (Flat, Bass Boost, Rock, Pop, Vocal)
 * - Bass boost slider (0 - 100%)
 * - 5-band graphic equalizer sliders (-12 dB to +12 dB)
 */
@Composable
fun EqualizerDialog(
    state: EqualizerState,
    hazeState: HazeState? = null,
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
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 480.dp)
                    .clickable(enabled = false) {} // Consume clicks inside
                    .liquidGlass(
                        hazeState = hazeState,
                        shape = RoundedCornerShape(28.dp),
                        elevation = 20.dp,
                    ),
                color = Color.Transparent,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    // Header Row
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
                                color = GlassTheme.TextSecondary,
                            )
                            Text(
                                text = "Sound Profiles",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTheme.TextPrimary,
                            )
                        }

                        // Master On/Off Switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = if (state.isEnabled) "ON" else "OFF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isEnabled) GlassTheme.Accent else GlassTheme.TextMuted,
                            )
                            Switch(
                                checked = state.isEnabled,
                                onCheckedChange = onToggleEnabled,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = GlassTheme.Accent,
                                    uncheckedThumbColor = GlassTheme.TextMuted,
                                    uncheckedTrackColor = Color(0x30FFFFFF),
                                ),
                            )
                        }
                    }

                    // Presets Row
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "PRESETS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp,
                            color = GlassTheme.TextMuted,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            EqPreset.entries.forEach { preset ->
                                val isSelected = state.preset == preset
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) GlassTheme.Accent.copy(alpha = 0.25f)
                                            else Color(0x15FFFFFF)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) GlassTheme.Accent else Color(0x20FFFFFF),
                                            RoundedCornerShape(12.dp),
                                        )
                                        .clickable(enabled = state.isEnabled) { onSelectPreset(preset) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = preset.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = when {
                                            !state.isEnabled -> GlassTheme.TextMuted.copy(alpha = 0.5f)
                                            isSelected -> GlassTheme.Accent
                                            else -> GlassTheme.TextPrimary
                                        },
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }

                    // Bass Boost Section
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                color = GlassTheme.TextMuted,
                            )
                            Text(
                                text = "${state.bassBoost}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isEnabled) GlassTheme.Accent else GlassTheme.TextMuted,
                            )
                        }

                        Slider(
                            value = state.bassBoost.toFloat(),
                            onValueChange = { onSetBassBoost(it.toInt()) },
                            valueRange = 0f..100f,
                            enabled = state.isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = GlassTheme.Accent,
                                activeTrackColor = GlassTheme.Accent,
                                inactiveTrackColor = Color(0x20FFFFFF),
                            ),
                        )
                    }

                    // 5-Band Equalizer Sliders
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "FREQUENCY BANDS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp,
                            color = GlassTheme.TextMuted,
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
                                    color = if (state.isEnabled) GlassTheme.TextSecondary else GlassTheme.TextMuted,
                                    modifier = Modifier.width(62.dp),
                                )

                                Slider(
                                    value = band.gainDb.toFloat(),
                                    onValueChange = { onSetBandGain(band.index, it.toInt()) },
                                    valueRange = band.minGainDb.toFloat()..band.maxGainDb.toFloat(),
                                    steps = 23, // 1 dB steps from -12 to +12
                                    enabled = state.isEnabled,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = GlassTheme.Accent,
                                        activeTrackColor = GlassTheme.Accent,
                                        inactiveTrackColor = Color(0x20FFFFFF),
                                    ),
                                )

                                Text(
                                    text = "$sign${band.gainDb} dB",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.isEnabled) GlassTheme.TextPrimary else GlassTheme.TextMuted,
                                    modifier = Modifier.width(50.dp),
                                )
                            }
                        }
                    }

                    // Done Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlassTheme.Accent,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = "Done",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
