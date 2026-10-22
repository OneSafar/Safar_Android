package com.safarparmar.app.feature.habits.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepository @Inject constructor(
    private val remoteStore: HabitRemoteStore
) {
    val syncStatus get() = remoteStore.syncStatus
    suspend fun refresh() = remoteStore.refresh()

    fun observeHabits(): Flow<List<HabitEntity>> = remoteStore.observeActiveHabits()

    // Keep schedule/history transformations off the collector's UI thread.
    fun observeForDate(date: LocalDate): Flow<List<HabitWithCompletion>> =
        combine(
            remoteStore.observeActiveHabits(),
            remoteStore.observeCompletionsForDate(date),
            remoteStore.observeActiveScheduleRevisionsThrough(date)
        ) { habits, completions, revisions ->
            val doneIds = completions.asSequence().filter { it.completed }.map { it.habitId }.toSet()
            val revisionsByHabit = revisions.groupBy { it.habitId }
            habits.mapNotNull { habit ->
                val schedule = scheduleAt(habit, revisionsByHabit[habit.id].orEmpty(), date)
                if (schedule != null && date.dayOfWeek in schedule) {
                    HabitWithCompletion(habit, date, habit.id in doneIds)
                } else null
            }
        }.flowOn(Dispatchers.Default)

    fun observeForRange(start: LocalDate, end: LocalDate): Flow<List<HabitWithCompletions>> =
        combine(
            remoteStore.observeActiveHabits(),
            remoteStore.observeCompletionsBetween(start, end),
            remoteStore.observeActiveScheduleRevisionsThrough(end)
        ) { habits, completions, revisions ->
            val doneByHabit = completions
                .asSequence()
                .filter { it.completed }
                .groupBy({ it.habitId }, { it.date })
                .mapValues { it.value.toSet() }
            val revisionsByHabit = revisions.groupBy { it.habitId }

            habits.map { habit ->
                val habitRevisions = revisionsByHabit[habit.id].orEmpty()
                val doneDates = doneByHabit[habit.id].orEmpty()
                val completionByDate = buildCompletionByDate(habit, habitRevisions, doneDates, start, end)
                HabitWithCompletions(habit, completionByDate)
            }
        }.flowOn(Dispatchers.Default)

    fun observeHabitsWithCompletionsForRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<HabitWithCompletions>> =
        combine(
            remoteStore.observeHabitsForRange(startDate, endDate),
            remoteStore.observeCompletionsBetween(startDate, endDate),
            remoteStore.observeScheduleRevisionsForRange(startDate, endDate)
        ) { habits, completions, revisions ->
            val doneByHabit = completions
                .asSequence()
                .filter { it.completed }
                .groupBy({ it.habitId }, { it.date })
                .mapValues { it.value.toSet() }
            val revisionsByHabit = revisions.groupBy { it.habitId }

            habits.map { habit ->
                val habitRevisions = revisionsByHabit[habit.id].orEmpty()
                val doneDates = doneByHabit[habit.id].orEmpty()
                val completionByDate = buildCompletionByDate(habit, habitRevisions, doneDates, startDate, endDate)
                HabitWithCompletions(habit, completionByDate)
            }
        }.flowOn(Dispatchers.Default)

    suspend fun getHabitsWithCompletionsForRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HabitWithCompletions> = withContext(Dispatchers.Default) {
        val habits = remoteStore.getHabitsForRange(startDate, endDate)
        val completions = remoteStore.getCompletionsBetween(startDate, endDate)
        val revisions = remoteStore.getScheduleRevisionsForRange(startDate, endDate)

        val doneByHabit = completions
            .asSequence()
            .filter { it.completed }
            .groupBy({ it.habitId }, { it.date })
            .mapValues { it.value.toSet() }
        val revisionsByHabit = revisions.groupBy { it.habitId }

        habits.map { habit ->
            val habitRevisions = revisionsByHabit[habit.id].orEmpty()
            val doneDates = doneByHabit[habit.id].orEmpty()
            val completionByDate = buildCompletionByDate(habit, habitRevisions, doneDates, startDate, endDate)
            HabitWithCompletions(habit, completionByDate)
        }
    }

    suspend fun toggle(
        habitId: Long,
        date: LocalDate,
        currentlyDone: Boolean
    ): ToggleResult {
        val habit = remoteStore.getHabitById(habitId) ?: return ToggleResult.HABIT_NOT_FOUND
        if (!currentlyDone) {
            if (date.isAfter(LocalDate.now())) return ToggleResult.FUTURE_BLOCKED
            val revisions = remoteStore.getScheduleRevisionsForHabit(habitId)
            val schedule = scheduleAt(habit, revisions, date)
            if (schedule == null || date.dayOfWeek !in schedule) return ToggleResult.NOT_SCHEDULED
        }

        if (currentlyDone) remoteStore.clearCompletion(habitId, date)
        else remoteStore.upsertCompletion(HabitCompletionEntity(habitId, date, completed = true))

        return if (!currentlyDone && date.isBefore(LocalDate.now())) {
            ToggleResult.RETROACTIVE_DONE
        } else ToggleResult.SUCCESS
    }

    suspend fun archiveHabit(habitId: Long) {
        remoteStore.archiveHabit(habitId, LocalDate.now())
    }

    suspend fun deleteHabit(habit: HabitEntity) = remoteStore.deleteHabit(habit)

    suspend fun createHabit(name: String, targetDays: Set<DayOfWeek>, isEveryDay: Boolean, order: Int, reminderTime: String? = null) {
        val today = LocalDate.now()
        remoteStore.insertHabit(HabitEntity(name = name, targetDays = targetDays,
            isEveryDay = isEveryDay, scheduledSince = today, order = order, reminderTime = reminderTime))
    }

    /**
     * Updates the visible habit row and records a schedule revision only when the
     * recurrence actually changed. Multiple edits on the same day replace that day's
     * revision, which is what users expect before the day has finished.
     */
    suspend fun editHabit(habit: HabitEntity) {
        remoteStore.updateHabit(habit)
    }

    suspend fun streaks(habit: HabitEntity): StreakInfo = withContext(Dispatchers.Default) {
        val doneDates = remoteStore.getCompletionsForHabit(habit.id).map { it.date }.toSet()
        val revisions = remoteStore.getScheduleRevisionsForHabit(habit.id)
        val today = LocalDate.now()
        val earliestDate = listOfNotNull(
            habit.scheduledSince,
            revisions.minOfOrNull { it.effectiveFrom },
            doneDates.minOrNull()
        ).minOrNull() ?: today
        val stop = minOf(earliestDate, today)

        var current = 0
        var cursor = today
        var firstScheduledSeen = false
        while (!cursor.isBefore(stop)) {
            val schedule = scheduleAt(habit, revisions, cursor)
            val scheduled = schedule != null && cursor.dayOfWeek in schedule
            if (scheduled) {
                if (!firstScheduledSeen && cursor == today && cursor !in doneDates) {
                    // An unfinished current day does not erase yesterday's valid streak.
                    firstScheduledSeen = true
                } else if (cursor in doneDates) {
                    current++
                    firstScheduledSeen = true
                } else {
                    break
                }
            }
            cursor = cursor.minusDays(1)
        }

        var best = 0
        var run = 0
        var d = stop
        while (!d.isAfter(today)) {
            val schedule = scheduleAt(habit, revisions, d)
            if (schedule != null && d.dayOfWeek in schedule) {
                if (d in doneDates) {
                    run++
                    if (run > best) best = run
                } else {
                    run = 0
                }
            }
            d = d.plusDays(1)
        }
        StreakInfo(current = current, best = maxOf(best, current))
    }

    private fun buildCompletionByDate(
        habit: HabitEntity,
        habitRevisions: List<HabitScheduleRevisionEntity>,
        doneDates: Set<LocalDate>,
        startDate: LocalDate,
        endDate: LocalDate
    ): Map<LocalDate, Boolean> = buildMap {
        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            val isArchivedOnDate = habit.isArchived && habit.archivedAt != null && cursor.isAfter(habit.archivedAt)
            if (!isArchivedOnDate) {
                val schedule = scheduleAt(habit, habitRevisions, cursor)
                if (schedule != null && cursor.dayOfWeek in schedule) {
                    put(cursor, cursor in doneDates)
                }
            }
            cursor = cursor.plusDays(1)
        }
    }

    private fun scheduleAt(
        habit: HabitEntity,
        revisions: List<HabitScheduleRevisionEntity>,
        date: LocalDate
    ): Set<DayOfWeek>? {
        val revision = revisions.lastOrNull { !it.effectiveFrom.isAfter(date) }
        if (revision != null) return revision.targetDays
        // A pre-v4 habit may not have a reconstructable schedule before scheduledSince.
        return if (!date.isBefore(habit.scheduledSince)) habit.targetDays else null
    }
}

enum class ToggleResult {
    SUCCESS,
    RETROACTIVE_DONE,
    FUTURE_BLOCKED,
    NOT_SCHEDULED,
    HABIT_NOT_FOUND
}

data class StreakInfo(val current: Int, val best: Int)

data class HabitWithCompletion(
    val habit: HabitEntity,
    val date: LocalDate,
    val completed: Boolean
)

data class HabitWithCompletions(
    val habit: HabitEntity,
    /** Keys exist only on dates where the habit was scheduled under the schedule revision active that day. */
    val completionByDate: Map<LocalDate, Boolean>
)
