package com.never.nocturne.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * Standard Liquid Glass styling inspired by Convx and iOS Cupertino design.
 */
object GlassTheme {
    val Background = Color(0xFF090B10)
    val SurfaceDark = Color(0x600E1422)
    val SurfaceCard = Color(0x35141D30)
    val SurfaceFloating = Color(0x75101626)
    
    val BorderSpecular = Brush.verticalGradient(
        listOf(
            Color(0x40FFFFFF),
            Color(0x10FFFFFF),
            Color(0x05FFFFFF),
        )
    )

    val BorderSubtle = Color(0x22FFFFFF)
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)
    val Accent: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary
    val AccentGlow: Color
        @Composable
        @ReadOnlyComposable
        get() = Accent.copy(alpha = 0.25f)
    val AccentPurple: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.secondary
}

/**
 * Applies a Liquid Glass effect:
 * 1. Haze blur effect sampling behind content
 * 2. Translucent surface tint
 * 3. Specular upper border highlight
 * 4. Subtle soft shadow
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun Modifier.liquidGlass(
    hazeState: HazeState? = null,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = GlassTheme.SurfaceFloating,
    elevation: Dp = 12.dp,
    hasBorder: Boolean = true,
): Modifier {
    val hazeMod = if (hazeState != null) {
        Modifier.hazeEffect(state = hazeState, style = HazeMaterials.ultraThin())
    } else {
        Modifier
    }

    val borderMod = if (hasBorder) {
        Modifier.border(
            width = 1.dp,
            brush = GlassTheme.BorderSpecular,
            shape = shape,
        )
    } else {
        Modifier
    }

    return this
        .shadow(elevation, shape = shape, spotColor = Color(0x40000000), ambientColor = Color(0x20000000))
        .then(hazeMod)
        .clip(shape)
        .background(backgroundColor)
        .then(borderMod)
}
