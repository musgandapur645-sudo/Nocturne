package com.never.nocturne

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.never.nocturne.shared.AppSettings
import com.never.nocturne.shared.accentTokens

/**
 * Material 3 colours resolved from the shared appearance settings.
 *
 * Glass intensity maps to surface opacity. Supports both Dark and Light Glass.
 */
@Composable
fun NocturneTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val accent = accentTokens(settings.preset, settings.accent)
    val intensity = settings.glassIntensity.coerceIn(0, 100) / 100f
    val surfaceAlpha = 1f - 0.45f * intensity
    val isLight = settings.preset.lowercase() == "light"

    val scheme = if (isLight) {
        lightColorScheme(
            primary = Color(accent.accent),
            onPrimary = Color.White,
            primaryContainer = Color(accent.accent).copy(alpha = 0.15f),
            onPrimaryContainer = Color(0xFF0F172A),
            secondary = Color(accent.accentHover),
            background = Color(0xFFF8FAFC),
            onBackground = Color(0xFF0F172A),
            surface = Color(0xFFFFFFFF).copy(alpha = (0.75f * surfaceAlpha).coerceIn(0.55f, 0.95f)),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFE2E8F0).copy(alpha = (0.85f * surfaceAlpha).coerceIn(0.65f, 0.95f)),
            onSurfaceVariant = Color(0xFF334155),
            outline = Color(accent.accent).copy(alpha = 0.45f),
        )
    } else {
        darkColorScheme(
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
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
