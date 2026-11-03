package com.never.localmusic.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.never.localmusic.shared.AccentTokens
import com.never.localmusic.shared.AppSettings
import com.never.localmusic.shared.GlassTokens
import com.never.localmusic.shared.ThemePresets
import com.never.localmusic.shared.accentTokens
import com.never.localmusic.shared.glassTokens

/**
 * Everything the desktop UI paints with, resolved from [AppSettings].
 *
 * The palette is a value so the whole window recolours in one recomposition
 * when the user picks a preset, an accent, or a glass intensity.
 * See docs/customization-spec.md.
 */
data class GlassPalette(
    val background: Color,
    val surface: Color,
    val glassCard: Color,
    val glassCardHover: Color,
    val glassCardActive: Color,
    val border: Color,
    val borderSubtle: Color,
    val borderHighlight: Color,
    val accent: Color,
    val accentGlow: Color,
    val accentHover: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val glass: GlassTokens,
    /** Preset the palette resolves from, for the appearance screen. */
    val presetSlug: String,
) {
    val presetName: String get() = ThemePresets.bySlug(presetSlug).displayName
}

internal fun buildGlassPalette(settings: AppSettings): GlassPalette {
    val normalized = settings.normalized()
    val accent: AccentTokens = accentTokens(normalized.preset, normalized.accent)
    return GlassPalette(
        background = Color(0xFF07080B),
        // Frosted translucent glass panels (Convx liquid glass aesthetic)
        surface = Color(0x600F131E),
        glassCard = Color(0x35141926),
        glassCardHover = Color(0x55222B40),
        glassCardActive = Color(0x752C3754),
        // Specular highlight borders (Apple/Convx top light reflections)
        border = Color(0x2EFFFFFF),
        borderSubtle = Color(0x18FFFFFF),
        borderHighlight = Color(0x45FFFFFF),
        accent = Color(accent.accent),
        accentGlow = Color(accent.accentGlow),
        accentHover = Color(accent.accentHover),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        textMuted = Color(0xFF94A3B8),
        glass = glassTokens(normalized.glassIntensity),
        presetSlug = normalized.preset,
    )
}

val LocalGlassPalette = staticCompositionLocalOf { buildGlassPalette(AppSettings()) }

/**
 * Colour tokens, read from the active palette.
 *
 * These stay as property names so the existing UI code keeps reading them
 * without threading a palette parameter through every composable.
 */
object GlassColors {
    val Background: Color @Composable @ReadOnlyComposable get() = LocalGlassPalette.current.background
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalGlassPalette.current.surface
    val GlassCard: Color @Composable @ReadOnlyComposable get() = LocalGlassPalette.current.glassCard
    val GlassCardHover: Color @Composable @ReadOnlyComposable get() = LocalGlassPalette.current.glassCardHover
    val GlassCardActive: Color @Composable @ReadOnlyComposable get() = LocalGlassPalette.current.glassCardActive
    val Border: Color @Composable @ReadOnlyComposable get() = LocalGlassPalette.current.border
    val BorderSubtle: Color @Composable @ReadOnlyComposable get() = LocalGlassPalette.current.borderSubtle
    val BorderHighlight: Color @Composable @ReadOnlyComposable get() = LocalGlassPalette.current.borderHighlight
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalGlassPalette.current.accent
    val AccentGlow: Color @Composable @ReadOnlyComposable get() = LocalGlassPalette.current.accentGlow
    val AccentHover: Color @Composable @ReadOnlyComposable get() = LocalGlassPalette.current.accentHover
    val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalGlassPalette.current.textPrimary
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalGlassPalette.current.textSecondary
    val TextMuted: Color @Composable @ReadOnlyComposable get() = LocalGlassPalette.current.textMuted
}

/**
 * Convx / Apple-style refractive glass panel modifier.
 *
 * Three layers make a surface read as glass over the mesh backdrop:
 * 1. a soft drop shadow that lifts the slab off the ambient field,
 * 2. a translucent tint fill so the blurred colors show through,
 * 3. a specular stroke — bright at the top edge, fading to nothing — that
 *    catches the light the way Convx's floating panels do.
 *
 * The fill and stroke alphas are scaled by the user's glass intensity, so 60
 * (the default) reproduces the original look exactly.
 */
@Composable
fun Modifier.glassPanel(
    cornerRadius: Dp = 16.dp,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    elevation: Dp = 8.dp,
): Modifier {
    val palette = LocalGlassPalette.current
    val glass = palette.glass
    val fillBase = backgroundColor ?: palette.glassCard
    val strokeBase = borderColor ?: palette.border
    val fill = fillBase.copy(alpha = (fillBase.alpha * glass.fillFactor).coerceIn(0f, 1f))
    val stroke = strokeBase.copy(alpha = (strokeBase.alpha * glass.edgeFactor).coerceIn(0f, 1f))
    val highlightAlpha = (0.07f * glass.edgeFactor).coerceIn(0f, 0.3f)

    val shape = RoundedCornerShape(cornerRadius)
    val specularBrush = Brush.verticalGradient(
        colors = listOf(
            stroke,
            stroke.copy(alpha = stroke.alpha * 0.15f),
        ),
    )
    return this
        .shadow(elevation, shape, ambientColor = Color(0x44000000), spotColor = Color(0x55000000))
        .clip(shape)
        .background(fill)
        // Top-edge highlight: white light pooling at the top of the slab.
        .drawBehind {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = highlightAlpha),
                    0.3f to Color.Transparent,
                ),
            )
        }
        .border(1.dp, specularBrush, shape)
}

@Composable
fun DarkGlassTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit,
) {
    val palette = remember(settings) { buildGlassPalette(settings) }
    val colorScheme = darkColorScheme(
        background = palette.background,
        surface = Color.Transparent,
        primary = palette.accent,
        onPrimary = Color.White,
        onBackground = palette.textPrimary,
        onSurface = palette.textPrimary,
    )
    CompositionLocalProvider(LocalGlassPalette provides palette) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
