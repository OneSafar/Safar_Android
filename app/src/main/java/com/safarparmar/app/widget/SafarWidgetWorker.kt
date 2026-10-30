package com.safarparmar.app.widget

import android.content.Context
import androidx.annotation.Keep
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.google.gson.Gson
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.domain.repository.HomeRepository
import com.safarparmar.app.domain.repository.EkagraRepository
import com.safarparmar.app.feature.kavachanalytics.data.local.KavachAnalyticsDao
import com.safarparmar.app.ui.ekagra.EkagraPendingSessionSaveStore
import com.safarparmar.app.ui.ekagra.mergeJournalHistory
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldRepository
import com.safarparmar.app.util.Resource
import com.safarparmar.app.util.assignedDateKey
import com.safarparmar.app.util.isGoalCompleted
import com.safarparmar.app.util.isVisibleInGoals
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import java.time.LocalDate

@Keep
internal data class SafarWidgetSnapshot(
    val owner: String,
    val day: String,
    val focusSeconds: Long? = null,
    val protectedSeconds: Long? = null,
    val kavachUsed: Boolean = false,
    val completed: Int? = null,
    val pending: Int? = null,
    val nextGoalTitle: String? = null,
    val streakDays: Int? = null,
    val userName: String? = null,
    val updatedAt: Long = 0L,
    val stale: Boolean = false,
)

internal object SafarWidgetStore {
    private fun prefs(context: Context) = context.getSharedPreferences("safar_today_widget", Context.MODE_PRIVATE)

    fun read(context: Context): SafarWidgetSnapshot? = runCatching {
        Gson().fromJson(prefs(context).getString("snapshot", null), SafarWidgetSnapshot::class.java)
    }.getOrNull()

    fun write(context: Context, value: SafarWidgetSnapshot) {
        prefs(context).edit().putString("snapshot", Gson().toJson(value)).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}

class SafarWidgetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun homeRepository(): HomeRepository
        fun ekagraRepository(): EkagraRepository
        fun kavachAnalyticsDao(): KavachAnalyticsDao
        fun dataStore(): SafarDataStore
    }

    override suspend fun doWork(): Result = coroutineScope {
        val hasToday = SafarTodayWidget.hasWidgets(applicationContext)
        val hasBento = SafarBentoWidget.hasWidgets(applicationContext)
        if (!hasToday && !hasBento) return@coroutineScope Result.success()

        val deps = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
        val dataStore = deps.dataStore()
        val owner = dataStore.userId.first().orEmpty()
        if (owner.isBlank() || !dataStore.isLoggedIn.first()) {
            SafarWidgetStore.clear(applicationContext)
            SafarTodayWidget.render(applicationContext, null)
            SafarBentoWidget.render(applicationContext, null)
            return@coroutineScope Result.success()
        }

        val today = LocalDate.now(widgetZone)
        val previous = SafarWidgetStore.read(applicationContext)
            ?.takeIf { it.owner == owner && it.day == today.toString() }
            ?: SafarWidgetSnapshot(owner, today.toString())
        SafarTodayWidget.render(applicationContext, previous)
        SafarBentoWidget.render(applicationContext, previous)

        try {
            // Refresh must bypass the short-lived dashboard read snapshot after a mutation.
            deps.homeRepository().invalidateReadSnapshots()
            val goalsAsync = async { deps.homeRepository().getGoals() }
            val focusAsync = async { deps.ekagraRepository().getEkagraAnalytics() }
            val streaksAsync = async { deps.homeRepository().getStreaks() }
            val userNameAsync = async { dataStore.userName.first() }

            val start = today.atStartOfDay(widgetZone).toInstant().toEpochMilli()
            val now = System.currentTimeMillis()
            val windows = deps.kavachAnalyticsDao().protectionWindowsOverlapping(start, now)
            val protected = widgetProtectedSeconds(windows.map { it.startMs to it.endMs }, start, now)

            val goalResult = goalsAsync.await()
            val focusResult = focusAsync.await()
            val streakResult = streaksAsync.await()
            val rawUserName = userNameAsync.await()
            val firstName = rawUserName?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }

            val streakCount = (streakResult as? Resource.Success)?.data?.let {
                it.checkInStreak.takeIf { s -> s > 0 } ?: it.loginStreak.takeIf { s -> s > 0 }
            }

            val goalList = (goalResult as? Resource.Success)?.data.orEmpty()
            val counts = (goalResult as? Resource.Success)?.data?.let { widgetGoalCounts(it, today) }
            val nextGoal = goalList.firstOrNull {
                it.deletedAt.isNullOrBlank() && !it.isGoalCompleted() && it.isVisibleInGoals() &&
                    (it.assignedDateKey() == today.toString() || it.assignedDateKey() == null)
            }?.title

            val seconds = (focusResult as? Resource.Success)?.data?.let {
                widgetFocusSeconds(
                    mergeJournalHistory(it, EkagraPendingSessionSaveStore.getAll(applicationContext)).focusSessions,
                    today,
                )
            }

            // A request begun under the old account must never paint the new account's widget.
            if (dataStore.userId.first() != owner || !dataStore.isLoggedIn.first()) return@coroutineScope Result.success()
            if (LocalDate.now(widgetZone) != today) {
                enqueue(applicationContext)
                return@coroutineScope Result.success()
            }

            val fresh = counts != null && seconds != null
            val snapshot = previous.copy(
                focusSeconds = seconds ?: previous.focusSeconds,
                protectedSeconds = protected,
                kavachUsed = windows.isNotEmpty() || FocusShieldRepository.ShieldPrefs.isActive(applicationContext),
                completed = counts?.completed ?: previous.completed,
                pending = counts?.pending ?: previous.pending,
                nextGoalTitle = nextGoal ?: previous.nextGoalTitle,
                streakDays = streakCount ?: previous.streakDays,
                userName = firstName ?: previous.userName,
                updatedAt = if (fresh) now else previous.updatedAt,
                stale = !fresh,
            )
            SafarWidgetStore.write(applicationContext, snapshot)
            SafarTodayWidget.render(applicationContext, snapshot)
            SafarBentoWidget.render(applicationContext, snapshot)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (dataStore.userId.first() == owner && dataStore.isLoggedIn.first()) {
                val stale = previous.copy(stale = true)
                SafarTodayWidget.render(applicationContext, stale)
                SafarBentoWidget.render(applicationContext, stale)
            }
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            if (!SafarTodayWidget.hasWidgets(context) && !SafarBentoWidget.hasWidgets(context)) return
            WorkManager.getInstance(context).enqueueUniqueWork(
                "safar_today_widget_refresh", ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<SafarWidgetWorker>().build(),
            )
        }
    }
}
