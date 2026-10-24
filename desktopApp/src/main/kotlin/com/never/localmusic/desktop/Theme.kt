package com.never.localmusic.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object GlassColors {
    val Background = Color(0xFF08090E)
    val Surface = Color(0xFF11141D)
    val GlassCard = Color(0xFF171B26)
    val GlassCardHover = Color(0xFF1F2433)
    val Border = Color(0xFF23293A)
    val BorderSubtle = Color(0x1FFFFFFF)

    // Crimson Glass Accent
    val Accent = Color(0xFFE11D48)
    val AccentHover = Color(0xFFF43F5E)

    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)
}

fun Modifier.glassPanel(
    cornerRadius: Dp = 14.dp,
    backgroundColor: Color = GlassColors.GlassCard,
    borderColor: Color = GlassColors.Border,
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(backgroundColor)
    .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))

@Composable
fun DarkGlassTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        background = GlassColors.Background,
        surface = GlassColors.Surface,
        primary = GlassColors.Accent,
        onPrimary = Color.White,
        onBackground = GlassColors.TextPrimary,
        onSurface = GlassColors.TextPrimary,
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}
