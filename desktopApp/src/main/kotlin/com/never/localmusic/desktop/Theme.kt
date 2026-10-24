package com.never.localmusic.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
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
 *
 * Three layers make a surface read as glass over the mesh backdrop:
 * 1. a soft drop shadow that lifts the slab off the ambient field,
 * 2. a translucent tint fill so the blurred colors show through,
 * 3. a specular stroke — bright at the top edge, fading to nothing — that
 *    catches the light the way Convx's floating panels do.
 */
fun Modifier.glassPanel(
    cornerRadius: Dp = 16.dp,
    backgroundColor: Color = GlassColors.GlassCard,
    borderColor: Color = GlassColors.Border,
    elevation: Dp = 8.dp,
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val specularBrush = Brush.verticalGradient(
        colors = listOf(
            borderColor,
            borderColor.copy(alpha = borderColor.alpha * 0.15f),
        ),
    )
    return this
        .shadow(elevation, shape, ambientColor = Color(0x44000000), spotColor = Color(0x55000000))
        .clip(shape)
        .background(backgroundColor)
        // Top-edge highlight: white light pooling at the top of the slab.
        .drawBehind {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.07f),
                    0.3f to Color.Transparent,
                ),
            )
        }
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
