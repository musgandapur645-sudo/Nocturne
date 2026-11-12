package com.never.nocturne.desktop.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// Convx mesh backdrop (ported from ui/player/MeshGradientBackground.kt)
//
// Four blobs drawn from the track's artwork drift slowly under a heavy blur.
// The whole dock/sidebar/main stack is translucent, so this color field is
// what makes every frosted surface read as glass instead of dark grey.
// ─────────────────────────────────────────────────────────────────────────────

private val SeamlessEasing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

/**
 * Near-black tuned fallbacks used when nothing has been played yet. They sit in
 * the same hue family as the accent palette so startup still shows a rich wash.
 */
private val FallbackColors = listOf(
    Color(0xFF6D1B33),
    Color(0xFF27324F),
    Color(0xFF3B2354),
    Color(0xFF14464B),
)

/**
 * How far the blobs travel in one settle — a shade under half a turn, far
 * enough that the backdrop reacts to a track change without orbits repeating.
 */
private const val DriftRadians = (PI * 0.45f).toFloat()

private val BlobAnchors = listOf(
    Offset(0.20f, 0.25f),
    Offset(0.80f, 0.20f),
    Offset(0.75f, 0.80f),
    Offset(0.25f, 0.75f),
)

private val BlobSpeeds = listOf(1f, -0.7f, 0.85f, -1.15f)

/** Mesh palette of [artPath]'s artwork, extracted off the UI thread. */
@Composable
fun rememberArtworkPalette(artPath: String?): List<Color> {
    var palette by remember { mutableStateOf<List<Color>?>(null) }
    LaunchedEffect(artPath) {
        palette = if (artPath.isNullOrBlank()) null else extractPalette(File(artPath))
    }
    return palette ?: FallbackColors
}

suspend fun extractPalette(file: File): List<Color>? = withContext(Dispatchers.Default) {
    if (!file.exists() || !file.canRead()) return@withContext null
    val bitmap = runCatching {
        file.inputStream().buffered().use { loadImageBitmap(it) }
    }.getOrNull() ?: return@withContext null

    val w = bitmap.width
    val h = bitmap.height
    if (w <= 0 || h <= 0) return@withContext null

    val pixels = IntArray(w * h)
    val readable = runCatching { bitmap.readPixels(pixels, 0, 0, w, h) }.isSuccess
    if (!readable || pixels.all { (it ushr 24) < 128 }) return@withContext null

    // 4 bits per channel keeps the histogram small and the buckets stable.
    val counts = IntArray(4096)
    val sumR = FloatArray(4096)
    val sumG = FloatArray(4096)
    val sumB = FloatArray(4096)
    for (pixel in pixels) {
        if ((pixel ushr 24) < 200) continue
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        val key = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
        counts[key]++
        sumR[key] += r
        sumG[key] += g
        sumB[key] += b
    }

    val swatches = counts.indices
        .filter { counts[it] > 0 }
        .sortedByDescending { counts[it] }
        .take(24)
        .map { key ->
            Color(
                red = sumR[key] / counts[key] / 255f,
                green = sumG[key] / counts[key] / 255f,
                blue = sumB[key] / counts[key] / 255f,
            )
        }

    when {
        swatches.isEmpty() -> null
        swatches.size >= 4 -> swatches.distinctEnough().take(4).ifEmpty { null }
        else -> swatches.expandedToFour()
    }
}

private fun List<Color>.distinctEnough(): List<Color> {
    val kept = mutableListOf<Color>()
    forEach { color -> if (kept.none { it.isCloseTo(color) }) kept += color }
    return kept
}

private fun Color.isCloseTo(other: Color): Boolean {
    val a = hsl()
    val b = other.hsl()
    val hueGap = abs(a[0] - b[0]).let { min(it, 360f - it) }
    return hueGap < 15f && abs(a[2] - b[2]) < 0.12f
}

private fun List<Color>.expandedToFour(): List<Color> {
    val out = distinctEnough().toMutableList()
    if (out.isEmpty()) return emptyList()
    var step = 1
    while (out.size < 4) {
        out += out[(out.size - 1) % out.size].shifted(24f * step, 0.12f * step)
        step++
    }
    return out
}

private fun Color.shifted(hue: Float, lightness: Float): Color {
    val hsl = hsl()
    hsl[0] = (hsl[0] + hue) % 360f
    hsl[2] = (hsl[2] + lightness).coerceIn(0.2f, 0.7f)
    return hslToColor(hsl)
}

/**
 * Desaturate and clamp lightness so artwork colours stay a tint behind the glass
 * instead of lighting the whole window. Bright covers used to turn the app green.
 */
internal fun Color.tuned(): Color {
    val hsl = hsl()
    hsl[1] = (hsl[1] * 0.80f).coerceIn(0f, 0.65f)
    hsl[2] = hsl[2].coerceIn(0.10f, 0.24f)
    return hslToColor(hsl)
}

/** The near-black the blobs are laid over, kept in the artwork's own hue. */
internal fun Color.dimmed(): Color {
    val hsl = hsl()
    hsl[2] = 0.07f
    return hslToColor(hsl)
}

private fun Color.hsl(): FloatArray {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    if (max == min) return floatArrayOf(0f, 0f, l)
    val delta = max - min
    val s = if (l > 0.5f) delta / (2f - max - min) else delta / (max + min)
    val h = when (max) {
        r -> ((g - b) / delta + (if (g < b) 6f else 0f)) * 60f
        g -> ((b - r) / delta + 2f) * 60f
        else -> ((r - g) / delta + 4f) * 60f
    }
    return floatArrayOf(h, s, l)
}

private fun hslToColor(hsl: FloatArray): Color {
    val h = ((hsl[0] % 360f) + 360f) % 360f
    val s = hsl[1].coerceIn(0f, 1f)
    val l = hsl[2].coerceIn(0f, 1f)
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}

/**
 * Builds a blurred mesh from four colors sampled from album art and crossfades
 * on track changes. Blobs drift through one settle on every track change; the
 * slow drift behind a full-screen blur every frame is skipped, like in Convx.
 */
@Composable
fun ConvxMeshBackground(
    palette: List<Color>,
    modifier: Modifier = Modifier,
    trackKey: Any? = null,
    driftMillis: Int = 8_000,
    transitionMillis: Int = 1_400,
) {
    val tuned = remember(palette) {
        (palette.ifEmpty { FallbackColors } + FallbackColors)
            .take(4)
            .map { it.tuned() }
    }

    val colorSpec = remember(transitionMillis) {
        tween<Color>(transitionMillis, easing = SeamlessEasing)
    }
    val animatedColors = tuned.mapIndexed { index, color ->
        animateColorAsState(color, colorSpec, label = "meshColor$index")
    }
    val baseColor = animateColorAsState(
        tuned.first().dimmed(),
        colorSpec,
        label = "meshBase",
    )

    val phase = remember { Animatable(0f) }
    LaunchedEffect(trackKey) {
        phase.animateTo(
            targetValue = phase.value + DriftRadians,
            animationSpec = tween(driftMillis, easing = FastOutSlowInEasing),
        )
    }

    // Scaled up (1.3x) so the blur's clamped edges never come into view, then
    // the whole layer is blurred — exactly the Convx layer setup.
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = 1.3f
                scaleY = 1.3f
            }
            .blur(64.dp),
    ) {
        val drift = phase.value
        drawRect(baseColor.value)

        animatedColors.forEachIndexed { index, colorState ->
            val color = colorState.value
            val anchor = BlobAnchors[index]
            val speed = BlobSpeeds[index]
            val center = Offset(
                x = (anchor.x + 0.16f * kotlin.math.cos(drift * speed + index * 1.7f)) * size.width,
                y = (anchor.y + 0.16f * kotlin.math.sin(drift * speed * 0.9f + index * 2.3f)) * size.height,
            )
            val radius = size.maxDimension * 0.62f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.6f), color.copy(alpha = 0f)),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }

        // Scrim so white text stays legible and the tint stays a backdrop, not a theme.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.30f),
                    Color.Black.copy(alpha = 0.55f),
                ),
            ),
        )
    }
}
