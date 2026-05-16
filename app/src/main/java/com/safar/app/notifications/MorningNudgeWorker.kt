package com.safar.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safar.app.data.local.SafarDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MorningNudgeWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val dataStore = SafarDataStore(context)
            
            // Respect the user's notification settings
            if (!dataStore.notificationsEnabled.first()) {
                return Result.success()
            }

            // Get user name for personalization
            val name = dataStore.userName.first()?.trim() ?: ""
            val greeting = if (name.isNotEmpty()) "Good Morning, $name! " else "Good Morning! "

            // 1. Instantiate notification manager (matching project pattern)
            val notificationManager = SafarNotificationManager(context)

            // 2. Pick a random quote from our assets
            val quote = getRandomQuote(context)

            // 3. Trigger the notification using Safar's existing UI standards
            notificationManager.show(
                title = quote.first,
                body = greeting + quote.second,
                channelId = SafarNotificationChannels.STUDY_REMINDERS,
                deepLink = "safar://dashboard" 
            )

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun getRandomQuote(context: Context): Pair<String, String> {
        return try {
            // Read from assets folder
            val inputStream = context.assets.open("nudge_quotes.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            
            val jsonArray = JSONArray(jsonString)
            val randomIndex = (0 until jsonArray.length()).random()
            val item = jsonArray.getJSONObject(randomIndex)
            
            Pair(
                item.getString("title"),
                item.getString("body")
            )
        } catch (e: Exception) {
            // Fallback nudge if the JSON fails to load
            Pair("Morning Reflection", "You are stronger than you know. Have a great day!")
        }
    }

    companion object {
        private const val WORK_NAME = "daily_morning_nudge"

        fun schedule(context: Context, targetHour: Int = 6, targetMinute: Int = 30) {
            val delay = calculateInitialDelayMinutes(targetHour, targetMinute)

            val request = PeriodicWorkRequestBuilder<MorningNudgeWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        private fun calculateInitialDelayMinutes(targetHour: Int, targetMinute: Int): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // If the target time has already passed today, schedule for tomorrow
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }

            val diffInMillis = target.timeInMillis - now.timeInMillis
            return TimeUnit.MILLISECONDS.toMinutes(diffInMillis).coerceAtLeast(1)
        }
    }
}
