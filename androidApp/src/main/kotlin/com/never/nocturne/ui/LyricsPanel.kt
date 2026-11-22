package com.never.nocturne.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.never.nocturne.core.LrcParser
import kotlinx.coroutines.launch
import java.io.File

/**
 * Scrolling synchronized lyrics panel.
 *
 * Looks for a sidecar .lrc file next to the audio file (same path, .lrc extension).
 * Active line is bright; surrounding lines fade to muted. Auto-scrolls so the
 * active line sits in the vertical centre of the panel.
 *
 * Shows a friendly message when no .lrc file is found.
 */
@Composable
fun LyricsPanel(
    trackPath: String?,
    positionMs: Long,
    modifier: Modifier = Modifier,
) {
    val lines = remember(trackPath) {
        if (trackPath.isNullOrBlank()) return@remember emptyList()
        val lrc = File(trackPath.replaceAfterLast('.', "lrc"))
        if (lrc.exists()) LrcParser.parse(lrc.readText()) else emptyList()
    }

    if (lines.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "No lyrics file found.\nPlace a .lrc file next to your audio file.",
                color = Color(0x55FFFFFF),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(32.dp),
            )
        }
        return
    }

    val activeIndex = LrcParser.activeIndex(lines, positionMs)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to keep active line centred
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            scope.launch {
                listState.animateScrollToItem(
                    index = (activeIndex - 3).coerceAtLeast(0),
                )
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 80.dp, horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(lines, key = { i, _ -> i }) { index, line ->
            val isActive = index == activeIndex
            val isPast = index < activeIndex
            val targetColor = when {
                isActive -> Color.White
                isPast -> Color(0x50FFFFFF)
                else -> Color(0x35FFFFFF)
            }
            val color by animateColorAsState(
                targetValue = targetColor,
                animationSpec = tween(durationMillis = 300),
                label = "lyricColor",
            )

            Text(
                text = line.text.ifBlank { "·" },
                color = color,
                fontSize = if (isActive) 22.sp else 18.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
