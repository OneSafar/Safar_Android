package com.safarparmar.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.data.local.SafarDataStore
import androidx.work.ExistingPeriodicWorkPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reschedules all periodic WorkManager notification workers when:
 *  - The device boots (`BOOT_COMPLETED`, `QUICKBOOT_POWERON`)
 *  - The app is updated (`MY_PACKAGE_REPLACED`)
 *
 * **Why this is needed (Cat-7 P1 fix):**
 * Even though WorkManager persists across reboots and updates, the workers
 * use initial-delay to target a specific clock time (e.g. 06:30 morning nudge).
 * After a reboot or update that clears the process, the initial delay reference
 * is stale — workers may fire at the wrong time or not at all until the next
 * Application.onCreate() runs, which is not guaranteed to happen soon.
 *
 * This receiver uses `goAsync()` to safely run the coroutine off the main thread
 * within the 10-second BroadcastReceiver window. No Room queries, no network — just
 * DataStore reads and WorkManager enqueues (all fast and non-blocking).
 */
class NotificationRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action !in HANDLED_ACTIONS) return

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Received $action — rescheduling notification workers")
        }

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                rescheduleWorkers(context)
            } catch (e: Exception) {
                Log.e(TAG, "Worker reschedule failed after $action", e)
            }
            try {
                restoreKavachAlwaysOn(context)
            } catch (e: Exception) {
                // Kept separate from the worker reschedule above so one failing
                // cannot silently take the other down with it.
                Log.e(TAG, "Kavach Always On restore failed after $action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Brings KAVACH Always On back after a reboot or app update.
     *
     * A blocker the student has to remember to switch on again after every restart
     * is not a blocker — the one evening they forget is the evening it mattered.
     * Starting a foreground service from the background is normally refused on
     * Android 12+, but receiving BOOT_COMPLETED is one of the documented
     * exemptions; the start is still guarded in case an OEM disagrees.
     */
    private suspend fun restoreKavachAlwaysOn(context: Context) {
        val dataStore = SafarDataStore(context)
        val alwaysOn = dataStore.focusShieldAlwaysOnMode.first()
        val enabled = dataStore.focusShieldEnabled.first()
        val packages = dataStore.focusShieldBlockedPackages.first()

        val permitted =
            com.safarparmar.app.ui.ekagra.focusshield.FocusShieldPermissionHelper
                .hasUsageStatsPermission(context) &&
                com.safarparmar.app.ui.ekagra.focusshield.FocusShieldPermissionHelper
                    .hasOverlayPermission(context)

        if (!alwaysOn || !enabled || packages.isEmpty() || !permitted) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Always On not restored (on=$alwaysOn enabled=$enabled apps=${packages.size} permitted=$permitted)")
            }
            return
        }

        com.safarparmar.app.ui.ekagra.focusshield.KavachAlwaysOnService.start(context)
        if (BuildConfig.DEBUG) Log.d(TAG, "Kavach Always On restored")
    }

    private suspend fun rescheduleWorkers(context: Context) {
        val dataStore = SafarDataStore(context)
        val notificationsEnabled = dataStore.notificationsEnabled.first()
        val dailyReminderEnabled = dataStore.dailyStudyReminderEnabled.first()

        if (notificationsEnabled && dailyReminderEnabled) {
            val reminderTime = dataStore.dailyReminderTime.first()
            // Use KEEP so we don't reset an already-running periodic chain if the
            // worker survived the reboot/update on its own.
            val policy = ExistingPeriodicWorkPolicy.KEEP

            StudyReminderWorker.schedule(context, reminderTime, policy)
            PlannerAlertsWorker.schedule(context, reminderTime, policy)
            MorningNudgeWorker.schedule(context, 6, 30, policy)

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "All workers rescheduled (time=$reminderTime)")
            }
        } else {
            // Preferences say OFF — make sure nothing is lingering
            StudyReminderWorker.cancel(context)
            PlannerAlertsWorker.cancel(context)
            MorningNudgeWorker.cancel(context)

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Workers cancelled (notifications=$notificationsEnabled, daily=$dailyReminderEnabled)")
            }
        }

        // Also opportunistically ensure channels exist (cheap & idempotent)
        SafarNotificationChannels.createAll(context)
    }

    companion object {
        private const val TAG = "SafarReschedule"

        private val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",   // HTC/OEM variants
            "com.htc.intent.action.QUICKBOOT_POWERON",
        )
    }
}
