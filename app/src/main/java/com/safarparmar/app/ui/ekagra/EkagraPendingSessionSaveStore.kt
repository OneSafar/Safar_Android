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
    val goalId: String?,
    val goalTitle: String?,
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
                            actualDurationMinutes = item.optInt("actualDurationMinutes", 1).coerceAtLeast(1),
                            goalId = item.optString("goalId").takeIf { it.isNotBlank() },
                            goalTitle = item.optString("goalTitle").takeIf { it.isNotBlank() },
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
                    .put("goalId", session.goalId)
                    .put("goalTitle", session.goalTitle)
                    .put("taskTitle", session.taskTitle)
                    .put("shieldEnabled", session.shieldEnabled),
            )
        }
        prefs(context).edit().putString(KEY_QUEUE_JSON, array.toString()).commit()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
