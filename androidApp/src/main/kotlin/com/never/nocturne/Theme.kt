package com.never.nocturne

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.never.nocturne.shared.AppSettings
import com.never.nocturne.shared.accentTokens

/**
 * Material 3 colours resolved from the shared appearance settings.
 *
 * Android has no blur, so glass intensity maps to how opaque the surface
 * colours are: 0 is fully solid, 100 lets the near-black backdrop through.
 * See docs/customization-spec.md.
 */
@Composable
fun NocturneTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val accent = accentTokens(settings.preset, settings.accent)
    val intensity = settings.glassIntensity.coerceIn(0, 100) / 100f
    val surfaceAlpha = 1f - 0.45f * intensity

    val scheme = darkColorScheme(
        primary = Color(accent.accent),
        onPrimary = Color(accent.onAccent),
        primaryContainer = Color(accent.accentGlow),
        onPrimaryContainer = Color(0xFFF8FAFC),
        secondary = Color(accent.accentHover),
        background = Color(0xFF07080B),
        onBackground = Color(0xFFF8FAFC),
        surface = Color(0xFF10141D).copy(alpha = surfaceAlpha),
        onSurface = Color(0xFFF8FAFC),
        surfaceVariant = Color(0xFF161B26).copy(alpha = surfaceAlpha),
        onSurfaceVariant = Color(0xFFB4BECD),
        outline = Color(accent.accent).copy(alpha = 0.6f),
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
