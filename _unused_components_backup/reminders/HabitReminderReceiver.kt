package com.safarparmar.app.feature.habits.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class HabitReminderReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies { fun habitReminderScheduler(): HabitReminderScheduler }

    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val scheduler = EntryPointAccessors.fromApplication(context.applicationContext,
                    Dependencies::class.java).habitReminderScheduler()
                if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
                    intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                    intent?.action == Intent.ACTION_TIME_CHANGED ||
                    intent?.action == Intent.ACTION_TIMEZONE_CHANGED ||
                    intent?.action == Intent.ACTION_DATE_CHANGED) {
                    val dataStore = com.safarparmar.app.data.local.SafarDataStore(context)
                    val owner = dataStore.userId.first()
                    if (owner != null) scheduler.rescheduleAll(owner)
                } else {
                    val owner = intent?.getStringExtra("owner")
                    val habitId = intent?.getStringExtra("habit_id")
                    if (owner != null && habitId != null) scheduler.deliver(owner, habitId,
                        intent.getStringExtra("due_date"))
                }
            } catch (error: Exception) {
                Log.e("HabitReminder", "Reminder delivery or reschedule failed", error)
            } finally {
                pending.finish()
            }
        }
    }
}
