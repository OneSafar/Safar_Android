package com.safarparmar.app.ui.ekagra

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

@androidx.annotation.Keep
data class PendingEkagraSessionSave(
    val clientSessionId: String,
    val mode: String,
    val startedAt: String,
    val endedAt: String,
    val plannedDurationMinutes: Int,
    val actualDurationMinutes: Int,
    val actualDurationSeconds: Int? = null,
    val goalId: String?,
    val goalTitle: String?,
    val topicId: String? = null,
    val planId: String? = null,
    val topicTitle: String? = null,
    val taskTitle: String,
    val shieldEnabled: Boolean,
    val markGoalComplete: Boolean = false,
    val markTopicDone: Boolean = false,
    val endReason: String? = null,
    val ownerId: String? = null,
    val serverId: String? = null,
    val completedFocusRounds: Int = 0,
    val targetFocusRounds: Int = 0,
    val periodMode: String? = null,
)

object EkagraPendingSessionSaveStore {
    private const val PREFS_NAME = "ekagra_pending_session_saves"
    private const val KEY_QUEUE_JSON = "queue_json"

    fun enqueue(context: Context, session: PendingEkagraSessionSave) =
        EkagraSessionJournal.get(context).enqueue(session, session.ownerId)

    suspend fun getAll(context: Context): List<PendingEkagraSessionSave> =
        EkagraSessionJournal.get(context).pending()

    internal fun legacySessions(context: Context): List<PendingEkagraSessionSave> {
        val raw = prefs(context).getString(KEY_QUEUE_JSON, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val clientSessionId = item.optString("clientSessionId").takeIf { it.isNotBlank() } ?: continue
                    val mode = item.optString("mode", TimerMode.FOCUS.toApiMode())
                    add(
                        PendingEkagraSessionSave(
                            clientSessionId = clientSessionId,
                            mode = mode,
                            startedAt = item.optString("startedAt"),
                            endedAt = item.optString("endedAt"),
                            plannedDurationMinutes = if (mode.equals("stopwatch", ignoreCase = true)) 0
                                else item.optInt("plannedDurationMinutes", 1).coerceAtLeast(1),
                            actualDurationMinutes = item.optInt("actualDurationMinutes", 0).coerceAtLeast(0),
                            actualDurationSeconds = if (item.has("actualDurationSeconds")) item.optInt("actualDurationSeconds").coerceAtLeast(0) else null,
                            goalId = item.optionalString("goalId"),
                            goalTitle = item.optionalString("goalTitle"),
                            topicId = item.optionalString("topicId"),
                            planId = item.optionalString("planId"),
                            topicTitle = item.optionalString("topicTitle"),
                            taskTitle = item.optString("taskTitle", "Untitled").ifBlank { "Untitled" },
                            shieldEnabled = item.optBoolean("shieldEnabled", false),
                            markGoalComplete = item.optBoolean("markGoalComplete", false),
                            markTopicDone = item.optBoolean("markTopicDone", false),
                        ),
                    )
                }
            }
        }.getOrThrow()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun JSONObject.optionalString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}
