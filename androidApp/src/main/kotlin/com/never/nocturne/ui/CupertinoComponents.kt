package com.never.nocturne.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * iOS Cupertino-inspired sleek Seek Slider with spring thumb scale and glowing progress.
 */
@Composable
fun CupertinoSeekSlider(
    value: Float, // 0.0f .. 1.0f
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = GlassTheme.Accent,
    inactiveColor: Color = Color(0x35FFFFFF),
    trackHeight: Dp = 5.dp,
    thumbRadius: Dp = 8.dp,
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(value) }
    var trackWidthPx by remember { mutableFloatStateOf(0f) }

    val displayProgress = if (isDragging) dragProgress else value.coerceIn(0f, 1f)
    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.35f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "thumbScale",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .pointerInput(trackWidthPx) {
                detectTapGestures { offset ->
                    if (trackWidthPx > 0f) {
                        val newProgress = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                        dragProgress = newProgress
                        onValueChange(newProgress)
                    }
                }
            }
            .pointerInput(trackWidthPx) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        if (trackWidthPx > 0f) {
                            dragProgress = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        onValueChange(dragProgress)
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        if (trackWidthPx > 0f) {
                            val delta = dragAmount / trackWidthPx
                            dragProgress = (dragProgress + delta).coerceIn(0f, 1f)
                        }
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // Track background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(trackHeight / 2))
                .background(inactiveColor),
        )

        // Filled active progress
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = displayProgress.coerceIn(0f, 1f))
                .height(trackHeight)
                .clip(RoundedCornerShape(trackHeight / 2))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            activeColor,
                            GlassTheme.AccentPurple,
                        )
                    )
                ),
        )

        // Draggable glowing thumb
        val density = LocalDensity.current
        val thumbPx = with(density) { thumbRadius.toPx() }
        val thumbOffsetPx = (displayProgress * trackWidthPx) - thumbPx

        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffsetPx.roundToInt().coerceAtLeast(0), 0) }
                .size(thumbRadius * 2)
                .scale(thumbScale)
                .shadow(6.dp, CircleShape, spotColor = activeColor)
                .background(Color.White, CircleShape)
                .border(1.5.dp, Color(0x60FFFFFF), CircleShape),
        )
    }
}

/**
 * Elevated circular button with tactile spring bounce feedback on press.
 */
@Composable
fun CupertinoGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    icon: ImageVector,
    iconSize: Dp = 26.dp,
    iconTint: Color = Color.White,
    backgroundColor: Color = Color(0x35FFFFFF),
    borderColor: Brush = GlassTheme.BorderSpecular,
    glowColor: Color = GlassTheme.AccentGlow,
) {
    val coroutineScope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1f) }

    Box(
        modifier = modifier
            .size(size)
            .scale(scaleAnim.value)
            .shadow(8.dp, CircleShape, spotColor = glowColor)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.2.dp, borderColor, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        coroutineScope.launch {
                            scaleAnim.animateTo(0.88f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                        tryAwaitRelease()
                        coroutineScope.launch {
                            scaleAnim.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                    },
                    onTap = { onClick() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * Capsule toggle pill button (e.g. for Shuffle, Repeat, Favorite).
 */
@Composable
fun CupertinoPillToggle(
    active: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String? = null,
    modifier: Modifier = Modifier,
    activeColor: Color = GlassTheme.Accent,
    inactiveColor: Color = GlassTheme.TextSecondary,
) {
    val bgAnim by animateFloatAsState(
        targetValue = if (active) 0.30f else 0.10f,
        animationSpec = tween(250),
        label = "pillBg",
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) activeColor.copy(alpha = bgAnim) else Color(0x18FFFFFF))
            .border(
                1.dp,
                if (active) activeColor.copy(alpha = 0.5f) else Color(0x15FFFFFF),
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) activeColor else inactiveColor,
            modifier = Modifier.size(16.dp),
        )
        if (label != null) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                color = if (active) activeColor else inactiveColor,
            )
        }
    }
}
