package com.never.nocturne.core

/**
 * Minimal LRC file parser.
 *
 * Supports standard timed tags: [mm:ss.xx] and [mm:ss:xx] variants.
 * Returns lines sorted by timestamp so they can be binary-searched by
 * playback position.
 *
 * Usage:
 *   val lines = LrcParser.parse(File("song.lrc").readText())
 *   val active = LrcParser.activeIndex(lines, positionMs)
 */
object LrcParser {

    data class LrcLine(val timeMs: Long, val text: String)

    private val TAG_REGEX = Regex("""\[(\d{1,3}):(\d{2})[.:](\d{1,3})]""")

    fun parse(content: String): List<LrcLine> {
        val result = mutableListOf<LrcLine>()
        for (raw in content.lines()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            var cursor = 0
            val times = mutableListOf<Long>()
            while (cursor < line.length) {
                val match = TAG_REGEX.find(line, cursor) ?: break
                if (match.range.first != cursor) break
                val min = match.groupValues[1].toLongOrNull() ?: break
                val sec = match.groupValues[2].toLongOrNull() ?: break
                val frac = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: break
                times += min * 60_000L + sec * 1_000L + frac
                cursor = match.range.last + 1
            }
            if (times.isEmpty()) continue
            val text = line.substring(cursor).trim()
            times.forEach { result += LrcLine(it, text) }
        }
        return result.sortedBy { it.timeMs }
    }

    /**
     * Returns the index of the last line whose [timeMs] ≤ [positionMs],
     * or -1 if playback hasn't reached the first line yet.
     */
    fun activeIndex(lines: List<LrcLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var lo = 0; var hi = lines.lastIndex
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (lines[mid].timeMs <= positionMs) lo = mid else hi = mid - 1
        }
        return if (lines[lo].timeMs <= positionMs) lo else -1
    }
}
