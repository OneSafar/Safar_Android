package com.safarparmar.app.feature.habits.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@AndroidEntryPoint
class HabitReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: HabitReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(8_000) {
                    if (intent.action in restoreActions) scheduler.reconcile()
                    else {
                        val owner = intent.getStringExtra("owner") ?: return@withTimeout
                        val habit = intent.getStringExtra("habit") ?: return@withTimeout
                        val date = intent.getStringExtra("date") ?: return@withTimeout
                        val token = intent.getStringExtra("token") ?: return@withTimeout
                        scheduler.deliver(owner, habit, date, token)
                    }
                }
            } catch (e: Exception) {
                Log.e("HabitReminder", "Could not restore or deliver habit reminder", e)
            } finally { pending.finish() }
        }
    }

    companion object {
        private val restoreActions = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)
    }
}
