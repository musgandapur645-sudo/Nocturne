package com.never.localmusic.desktop.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AmbientMeshBackground(
    accentColor: Color = Color(0xFFE11D48),
    modifier: Modifier = Modifier,
) {
    val animatedAccent by animateColorAsState(accentColor, tween(1000))
    val c1 = animatedAccent.copy(alpha = 0.45f)
    val c2 = Color(0xFF4F46E5).copy(alpha = 0.35f) // Indigo
    val c3 = Color(0xFF7C3AED).copy(alpha = 0.30f) // Violet
    val c4 = Color(0xFF0D9488).copy(alpha = 0.25f) // Teal

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .blur(80.dp),
    ) {
        drawRect(Color(0xFF07080B))

        // Blob 1: Top-Right (Accent)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c1, Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.15f),
                radius = size.width * 0.55f,
            ),
            radius = size.width * 0.55f,
            center = Offset(size.width * 0.85f, size.height * 0.15f),
        )

        // Blob 2: Bottom-Left (Indigo)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c2, Color.Transparent),
                center = Offset(size.width * 0.2f, size.height * 0.85f),
                radius = size.width * 0.6f,
            ),
            radius = size.width * 0.6f,
            center = Offset(size.width * 0.2f, size.height * 0.85f),
        )

        // Blob 3: Center-Right (Violet)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c3, Color.Transparent),
                center = Offset(size.width * 0.65f, size.height * 0.55f),
                radius = size.width * 0.5f,
            ),
            radius = size.width * 0.5f,
            center = Offset(size.width * 0.65f, size.height * 0.55f),
        )

        // Blob 4: Top-Left (Teal)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c4, Color.Transparent),
                center = Offset(size.width * 0.15f, size.height * 0.25f),
                radius = size.width * 0.45f,
            ),
            radius = size.width * 0.45f,
            center = Offset(size.width * 0.15f, size.height * 0.25f),
        )

        // Soft vignette scrim for crisp typography contrast
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.15f),
                    Color.Black.copy(alpha = 0.55f),
                ),
            ),
        )
    }
}
