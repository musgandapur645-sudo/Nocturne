package com.never.nocturne.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * Standard Liquid Glass styling inspired by Convx and iOS Cupertino design.
 * Adapts dynamically between Dark Glass and Frosted Light Glass.
 */
object GlassTheme {
    val isLight: Boolean
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val Background: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.background

    val SurfaceDark: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isLight) Color(0x65FFFFFF) else Color(0x600E1422)

    val SurfaceCard: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isLight) Color(0x80FFFFFF) else Color(0x35141D30)

    val SurfaceFloating: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isLight) Color(0xCCFFFFFF) else Color(0x75101626)

    val BorderSpecular: Brush
        @Composable
        @ReadOnlyComposable
        get() = if (isLight) {
            Brush.verticalGradient(
                listOf(
                    Color(0x30000000),
                    Color(0x10000000),
                    Color(0x05000000),
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0x40FFFFFF),
                    Color(0x10FFFFFF),
                    Color(0x05FFFFFF),
                )
            )
        }

    val BorderSubtle: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isLight) Color(0x18000000) else Color(0x22FFFFFF)

    val TextPrimary: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onBackground

    val TextSecondary: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    val TextMuted: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isLight) Color(0xFF64748B) else Color(0xFF64748B)

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
    backgroundColor: Color = Color.Unspecified,
    elevation: Dp = 12.dp,
    hasBorder: Boolean = true,
): Modifier {
    val actualBg = if (backgroundColor != Color.Unspecified) backgroundColor else GlassTheme.SurfaceFloating
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
        .background(actualBg)
        .then(borderMod)
}
