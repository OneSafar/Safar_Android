package com.safarparmar.app.ui.ekagra

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

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
)

object EkagraPendingSessionSaveStore {
    private const val PREFS_NAME = "ekagra_pending_session_saves"
    private const val KEY_QUEUE_JSON = "queue_json"

    @Synchronized
    fun enqueue(context: Context, session: PendingEkagraSessionSave) {
        val sessions = getAll(context)
            .filterNot { it.clientSessionId == session.clientSessionId }
            .toMutableList()
            .apply { add(session) }
        writeAll(context, sessions)
    }

    @Synchronized
    fun remove(context: Context, clientSessionId: String) {
        writeAll(context, getAll(context).filterNot { it.clientSessionId == clientSessionId })
    }

    @Synchronized
    fun getAll(context: Context): List<PendingEkagraSessionSave> {
        val raw = prefs(context).getString(KEY_QUEUE_JSON, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val clientSessionId = item.optString("clientSessionId").takeIf { it.isNotBlank() } ?: continue
                    add(
                        PendingEkagraSessionSave(
                            clientSessionId = clientSessionId,
                            mode = item.optString("mode", TimerMode.FOCUS.toApiMode()),
                            startedAt = item.optString("startedAt"),
                            endedAt = item.optString("endedAt"),
                            plannedDurationMinutes = item.optInt("plannedDurationMinutes", 1).coerceAtLeast(1),
                            actualDurationMinutes = item.optInt("actualDurationMinutes", 0).coerceAtLeast(0),
                            actualDurationSeconds = if (item.has("actualDurationSeconds")) item.optInt("actualDurationSeconds").coerceAtLeast(0) else null,
                            goalId = item.optString("goalId").takeIf { it.isNotBlank() },
                            goalTitle = item.optString("goalTitle").takeIf { it.isNotBlank() },
                            topicId = item.optString("topicId").takeIf { it.isNotBlank() },
                            planId = item.optString("planId").takeIf { it.isNotBlank() },
                            topicTitle = item.optString("topicTitle").takeIf { it.isNotBlank() },
                            taskTitle = item.optString("taskTitle", "Untitled").ifBlank { "Untitled" },
                            shieldEnabled = item.optBoolean("shieldEnabled", false),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeAll(context: Context, sessions: List<PendingEkagraSessionSave>) {
        val array = JSONArray()
        sessions.forEach { session ->
            array.put(
                JSONObject()
                    .put("clientSessionId", session.clientSessionId)
                    .put("mode", session.mode)
                    .put("startedAt", session.startedAt)
                    .put("endedAt", session.endedAt)
                    .put("plannedDurationMinutes", session.plannedDurationMinutes)
                    .put("actualDurationMinutes", session.actualDurationMinutes)
                    .apply { session.actualDurationSeconds?.let { put("actualDurationSeconds", it) } }
                    .put("goalId", session.goalId)
                    .put("goalTitle", session.goalTitle)
                    .put("topicId", session.topicId)
                    .put("planId", session.planId)
                    .put("topicTitle", session.topicTitle)
                    .put("taskTitle", session.taskTitle)
                    .put("shieldEnabled", session.shieldEnabled),
            )
        }
        // commit() (not apply()) — this queue must survive an immediate process death,
        // e.g. a session completing right before the OS kills the app for memory.
        prefs(context).edit().putString(KEY_QUEUE_JSON, array.toString()).commit()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
