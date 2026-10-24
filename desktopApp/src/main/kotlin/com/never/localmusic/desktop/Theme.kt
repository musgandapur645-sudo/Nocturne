package com.never.localmusic.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object GlassColors {
    val Background = Color(0xFF07080B)
    
    // Frosted Translucent Glass Panels (Convx liquid glass aesthetic)
    val Surface = Color(0x600F131E)
    val GlassCard = Color(0x35141926)
    val GlassCardHover = Color(0x55222B40)
    val GlassCardActive = Color(0x752C3754)
    
    // Specular Highlight Borders (Apple/Convx top light reflections)
    val Border = Color(0x2EFFFFFF)
    val BorderSubtle = Color(0x18FFFFFF)
    val BorderHighlight = Color(0x45FFFFFF)
    
    // Crimson Glass Accent
    val Accent = Color(0xFFE11D48)
    val AccentGlow = Color(0x40E11D48)
    val AccentHover = Color(0xFFF43F5E)

    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)
}

/**
 * Convx / Apple-style refractive glass panel modifier.
 * Uses translucent surface fill and a specular highlight border.
 */
fun Modifier.glassPanel(
    cornerRadius: Dp = 16.dp,
    backgroundColor: Color = GlassColors.GlassCard,
    borderColor: Color = GlassColors.Border,
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val specularBrush = Brush.verticalGradient(
        colors = listOf(
            borderColor,
            borderColor.copy(alpha = borderColor.alpha * 0.15f),
        ),
    )
    return this
        .clip(shape)
        .background(backgroundColor)
        .border(1.dp, specularBrush, shape)
}

@Composable
fun DarkGlassTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        background = GlassColors.Background,
        surface = Color.Transparent,
        primary = GlassColors.Accent,
        onPrimary = Color.White,
        onBackground = GlassColors.TextPrimary,
        onSurface = GlassColors.TextPrimary,
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}
