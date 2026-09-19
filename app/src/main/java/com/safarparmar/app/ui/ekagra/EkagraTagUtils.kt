package com.safarparmar.app.ui.ekagra

import com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession

val DEFAULT_EKAGRA_TAGS = listOf(
    "Study",
    "Workout",
    "Yoga"
)

data class EkagraTagStat(
    val tag: String,
    val totalSeconds: Long,
    val sessionCount: Int,
)

object EkagraTagUtils {
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
}
