package com.safarparmar.app.ui.ekagra

import androidx.compose.ui.graphics.Color
import com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession

val DEFAULT_EKAGRA_TAGS = listOf(
    "Study",
    "Workout",
    "Yoga"
)

val TAG_COLOR_PALETTE = listOf(
    "#10B981", // Emerald
    "#0EA5E9", // Sky Blue
    "#6366F1", // Indigo
    "#8B5CF6", // Violet / Purple
    "#EC4899", // Pink
    "#F43F5E", // Rose
    "#F59E0B", // Amber
    "#F97316", // Orange
    "#14B8A6", // Teal
    "#64748B"  // Slate
)

val DEFAULT_TAG_COLORS = mapOf(
    "Study" to "#10B981",
    "Workout" to "#F97316",
    "Yoga" to "#8B5CF6"
)

data class EkagraTagStat(
    val tag: String,
    val totalSeconds: Long,
    val sessionCount: Int,
)

object EkagraTagUtils {
    val DEFAULT_EKAGRA_TAGS = com.safarparmar.app.ui.ekagra.DEFAULT_EKAGRA_TAGS
    val TAG_COLOR_PALETTE = com.safarparmar.app.ui.ekagra.TAG_COLOR_PALETTE
    val DEFAULT_TAG_COLORS = com.safarparmar.app.ui.ekagra.DEFAULT_TAG_COLORS

    private val TAG_REGEX = Regex("""^\s*\[(.*?)\]\s*(.*)$""")

    fun formatTaggedTask(tag: String?, task: String?): String {
        val cleanTag = tag?.trim()?.takeIf { it.isNotEmpty() }
        val cleanTask = task?.trim()?.takeIf { it.isNotEmpty() }

        return when {
            cleanTag != null && cleanTask != null -> "[$cleanTag] $cleanTask"
            cleanTag != null -> "[$cleanTag]"
            cleanTask != null -> cleanTask
            else -> "Untitled"
        }
    }

    fun parseTagAndTask(rawText: String?): Pair<String?, String?> {
        if (rawText.isNullOrBlank()) return Pair(null, null)
        val match = TAG_REGEX.find(rawText) ?: return Pair(null, rawText.trim().takeIf { it.isNotEmpty() })
        val tag = match.groupValues.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
        val task = match.groupValues.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() }
        return Pair(tag, task)
    }

    fun calculateTagStats(sessions: List<EkagraAnalyticsFocusSession>): List<EkagraTagStat> {
        val secondsMap = mutableMapOf<String, Long>()
        val countMap = mutableMapOf<String, Int>()

        for (session in sessions) {
            // Rule: goal-linked sessions are off-limits / excluded from standalone tag stats
            if (session.isGoalLinked || !session.associatedGoalId.isNullOrBlank()) continue

            val (tag, _) = parseTagAndTask(session.taskText)
            if (!tag.isNullOrBlank()) {
                val secs = exactElapsedSeconds(session)
                secondsMap[tag] = (secondsMap[tag] ?: 0L) + secs
                countMap[tag] = (countMap[tag] ?: 0) + 1
            }
        }

        return secondsMap.map { (tag, secs) ->
            EkagraTagStat(tag = tag, totalSeconds = secs, sessionCount = countMap[tag] ?: 0)
        }.sortedByDescending { it.totalSeconds }
    }

    fun parseHexColor(hex: String): Color {
        return try {
            val clean = hex.removePrefix("#")
            val colorInt = when (clean.length) {
                6 -> (0xFF000000 or clean.toLong(16)).toInt()
                8 -> clean.toLong(16).toInt()
                else -> 0xFF10B981.toInt()
            }
            Color(colorInt)
        } catch (e: Exception) {
            Color(0xFF10B981)
        }
    }

    fun getTagColorHex(tag: String?, customColors: Map<String, String>? = null): String {
        if (tag.isNullOrBlank()) return "#64748B"
        val clean = tag.trim()
        customColors?.entries?.firstOrNull { it.key.equals(clean, ignoreCase = true) }?.value?.let {
            return it
        }
        DEFAULT_TAG_COLORS.entries.firstOrNull { it.key.equals(clean, ignoreCase = true) }?.value?.let {
            return it
        }
        val hash = Math.abs(clean.hashCode()) % TAG_COLOR_PALETTE.size
        return TAG_COLOR_PALETTE[hash]
    }

    fun getTagColor(tag: String?, customColors: Map<String, String>? = null): Color {
        return parseHexColor(getTagColorHex(tag, customColors))
    }
}
