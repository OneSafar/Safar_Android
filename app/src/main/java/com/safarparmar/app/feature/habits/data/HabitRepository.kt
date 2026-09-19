package com.safarparmar.app.feature.habits.data

import androidx.room.withTransaction
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
    private val dao: HabitDao,
    private val database: HabitDatabase? = null
) {
    fun observeHabits(): Flow<List<HabitEntity>> = dao.observeActiveHabits()

    // Keep schedule/history transformations off the collector's UI thread.
    fun observeForDate(date: LocalDate): Flow<List<HabitWithCompletion>> =
        combine(
            dao.observeActiveHabits(),
            dao.observeCompletionsForDate(date),
            dao.observeActiveScheduleRevisionsThrough(date)
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
            dao.observeActiveHabits(),
            dao.observeCompletionsBetween(start, end),
            dao.observeActiveScheduleRevisionsThrough(end)
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
            dao.observeHabitsForRange(startDate, endDate),
            dao.observeCompletionsBetween(startDate, endDate),
            dao.observeScheduleRevisionsForRange(startDate, endDate)
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
        val habits = dao.getHabitsForRange(startDate, endDate)
        val completions = dao.getCompletionsBetween(startDate, endDate)
        val revisions = dao.getScheduleRevisionsForRange(startDate, endDate)

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
        val habit = dao.getHabitById(habitId) ?: return ToggleResult.HABIT_NOT_FOUND
        if (!currentlyDone) {
            if (date.isAfter(LocalDate.now())) return ToggleResult.FUTURE_BLOCKED
            val revisions = dao.getScheduleRevisionsForHabit(habitId)
            val schedule = scheduleAt(habit, revisions, date)
            if (schedule == null || date.dayOfWeek !in schedule) return ToggleResult.NOT_SCHEDULED
        }

        if (currentlyDone) dao.clearCompletion(habitId, date)
        else dao.upsertCompletion(HabitCompletionEntity(habitId, date, completed = true))

        return if (!currentlyDone && date.isBefore(LocalDate.now())) {
            ToggleResult.RETROACTIVE_DONE
        } else ToggleResult.SUCCESS
    }

    suspend fun archiveHabit(habitId: Long) {
        dao.archiveHabit(habitId, LocalDate.now())
    }

    suspend fun deleteHabit(habit: HabitEntity) = dao.deleteHabit(habit)

    suspend fun createHabit(name: String, targetDays: Set<DayOfWeek>, isEveryDay: Boolean, order: Int) {
        val today = LocalDate.now()
        inTransaction {
            val id = dao.insertHabit(
                HabitEntity(
                    name = name,
                    targetDays = targetDays,
                    isEveryDay = isEveryDay,
                    scheduledSince = today,
                    order = order
                )
            )
            dao.upsertScheduleRevision(
                HabitScheduleRevisionEntity(
                    habitId = id,
                    effectiveFrom = today,
                    targetDays = targetDays,
                    isEveryDay = isEveryDay
                )
            )
        }
    }

    /**
     * Updates the visible habit row and records a schedule revision only when the
     * recurrence actually changed. Multiple edits on the same day replace that day's
     * revision, which is what users expect before the day has finished.
     */
    suspend fun editHabit(habit: HabitEntity) {
        val previous = dao.getHabitById(habit.id) ?: return
        val scheduleChanged = previous.targetDays != habit.targetDays || previous.isEveryDay != habit.isEveryDay
        val today = LocalDate.now()
        val updated = if (scheduleChanged) habit.copy(scheduledSince = today) else habit
        inTransaction {
            dao.updateHabit(updated)
            if (scheduleChanged) {
                dao.upsertScheduleRevision(
                    HabitScheduleRevisionEntity(
                        habitId = habit.id,
                        effectiveFrom = today,
                        targetDays = habit.targetDays,
                        isEveryDay = habit.isEveryDay
                    )
                )
            }
        }
    }

    suspend fun streaks(habit: HabitEntity): StreakInfo = withContext(Dispatchers.Default) {
        val doneDates = dao.getCompletionsForHabit(habit.id).map { it.date }.toSet()
        val revisions = dao.getScheduleRevisionsForHabit(habit.id)
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

    private suspend fun <R> inTransaction(block: suspend () -> R): R {
        return if (database != null) {
            database.withTransaction(block)
        } else {
            block()
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
