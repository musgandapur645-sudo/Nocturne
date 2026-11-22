package com.never.nocturne.desktop

import com.never.nocturne.core.LrcParser
import kotlin.test.Test
import kotlin.test.assertEquals

class LrcParserTest {
    @Test
    fun parsesStandardLrcContent() {
        val lrc = """
            [00:12.50]Line 1
            [00:18.00]Line 2
            [01:05.123]Line 3
        """.trimIndent()

        val lines = LrcParser.parse(lrc)
        assertEquals(3, lines.size)
        assertEquals(12500L, lines[0].timeMs)
        assertEquals("Line 1", lines[0].text)
        assertEquals(18000L, lines[1].timeMs)
        assertEquals("Line 2", lines[1].text)
        assertEquals(65123L, lines[2].timeMs)
        assertEquals("Line 3", lines[2].text)
    }

    @Test
    fun findsActiveIndexAccurately() {
        val lines = listOf(
            LrcParser.LrcLine(10000L, "First"),
            LrcParser.LrcLine(20000L, "Second"),
            LrcParser.LrcLine(30000L, "Third"),
        )

        assertEquals(-1, LrcParser.activeIndex(lines, 5000L))
        assertEquals(0, LrcParser.activeIndex(lines, 10000L))
        assertEquals(0, LrcParser.activeIndex(lines, 15000L))
        assertEquals(1, LrcParser.activeIndex(lines, 20000L))
        assertEquals(1, LrcParser.activeIndex(lines, 29999L))
        assertEquals(2, LrcParser.activeIndex(lines, 30000L))
        assertEquals(2, LrcParser.activeIndex(lines, 50000L))
    }
}
