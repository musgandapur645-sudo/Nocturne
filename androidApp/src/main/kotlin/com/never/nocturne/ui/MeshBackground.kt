package com.never.nocturne.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Convx-inspired ambient mesh backdrop.
 * Four artwork-derived blobs drift slowly under a heavy blur.
 * Combined with Haze blur, this makes every floating panel refract rich colour
 * instead of looking like a flat grey box.
 */

private val FallbackColors = listOf(
    Color(0xFF6D1B33), // Deep Wine
    Color(0xFF27324F), // Midnight Navy
    Color(0xFF3B2354), // Royal Violet
    Color(0xFF14464B), // Forest Teal
)

private val BlobAnchors = listOf(
    Offset(0.20f, 0.25f),
    Offset(0.80f, 0.20f),
    Offset(0.75f, 0.80f),
    Offset(0.25f, 0.75f),
)

private val BlobRadii = listOf(0.48f, 0.52f, 0.50f, 0.46f)
private val BlobSpeeds = listOf(1f, -0.75f, 0.85f, -1.15f)

@Composable
fun rememberArtworkColors(artPath: String?): List<Color> {
    var colors by remember { mutableStateOf(FallbackColors) }

    LaunchedEffect(artPath) {
        if (artPath.isNullOrBlank()) {
            colors = FallbackColors
            return@LaunchedEffect
        }
        val extracted = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(artPath)
                if (file.exists() && file.canRead()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        extractPaletteFromBitmap(bitmap)
                    } else null
                } else null
            }.getOrNull()
        }
        colors = extracted ?: FallbackColors
    }

    return colors
}

private fun extractPaletteFromBitmap(bitmap: Bitmap): List<Color> {
    val palette = Palette.from(bitmap).maximumColorCount(16).generate()
    val swatches = listOfNotNull(
        palette.vibrantSwatch,
        palette.darkVibrantSwatch,
        palette.dominantSwatch,
        palette.mutedSwatch,
        palette.darkMutedSwatch,
        palette.lightVibrantSwatch,
    )

    if (swatches.isEmpty()) return FallbackColors

    return (0 until 4).map { idx ->
        val swatch = swatches.getOrNull(idx) ?: swatches.first()
        Color(swatch.rgb)
    }
}

/**
 * Artwork swatches are often bright (a plain orange cover, for example). Clamp
 * them to a dark, slightly desaturated tint so the backdrop stays a backdrop and
 * the glass panels keep reading as dark glass rather than a colour theme.
 */
private fun Color.forBackdrop(): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    hsl[1] = (hsl[1] * 0.80f).coerceAtMost(0.65f)
    hsl[2] = hsl[2].coerceIn(0.10f, 0.24f)
    return Color(ColorUtils.HSLToColor(hsl))
}

@Composable
fun AndroidMeshBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val animatedColors = (0 until 4).map { i ->
        val target = colors.getOrElse(i) { FallbackColors[i] }.forBackdrop()
        animateColorAsState(
            targetValue = target,
            animationSpec = tween(1200, easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)),
            label = "meshColor$i",
        ).value
    }

    val transition = rememberInfiniteTransition(label = "meshDrift")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(64.dp)
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .then(blurModifier)
            // Colours are already clamped dark, so keep the layer near-opaque;
            // a low alpha here would wash them into the app background.
            .graphicsLayer { alpha = 0.85f },
    ) {
        val w = size.width
        val h = size.height

        // Dark base
        drawRect(Color(0xFF07090E))

        // 4 drifting radial blobs
        for (i in 0 until 4) {
            val anchor = BlobAnchors[i]
            val speed = BlobSpeeds[i]
            val p = phase * speed

            val cx = (anchor.x + 0.14f * cos(p)) * w
            val cy = (anchor.y + 0.14f * sin(p)) * h
            val radius = BlobRadii[i] * kotlin.math.max(w, h)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedColors[i].copy(alpha = 0.65f),
                        animatedColors[i].copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
            )
        }
    }
}
