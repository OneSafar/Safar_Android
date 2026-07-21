package com.safarparmar.app.ui.studyplanner.logic

import com.safarparmar.app.domain.model.studyplanner.CalendarTopicItem
import com.safarparmar.app.domain.model.studyplanner.HeatmapPoint
import com.safarparmar.app.domain.model.studyplanner.PlannerAnalytics
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.domain.model.studyplanner.effortPoints
import com.safarparmar.app.domain.model.studyplanner.pointsToTopicEquivalents
import com.safarparmar.app.domain.model.studyplanner.remainingPoints
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToInt

object PlannerInsightsCalculator {

    fun compute(
        plan: StudyPlan,
        calendar: Map<String, List<CalendarTopicItem>>,
        analytics: PlannerAnalytics?,
        todayIso: String = todayKey(),
    ): PlannerInsights {
        val refs = plan.flattenTopics()
        val topics = refs.map { it.topic }
        val remainingTopics = topics.count { it.status != TopicStatus.DONE }
        // Remaining work in effort points (server formula: small=1, medium=2,
        // big=4, scaled by partial progress), shown to the user as
        // topic-equivalents (points ÷ 2) so labels keep saying "topics/day".
        val remainingEquivalents = pointsToTopicEquivalents(
            refs.filter { it.topic.status != TopicStatus.DONE }
                .map { it.topic.remainingPoints(it.chapter) }
                .sum(),
        )
        val totalTopics = topics.size
        val doneTopics = topics.count { it.status == TopicStatus.DONE }
        val completionPercent = weightedCompletionPercent(refs.map { it.topic to it.chapter })

        val examDate = parsePlannerDate(plan.examDate)
        val today = LocalDate.parse(todayIso)
        val daysUntilExam = examDate?.let { ChronoUnit.DAYS.between(today, it).toInt() }

        val dailyGoal = max(1, plan.dailyGoal ?: 1)
        val availableStudyDays = examDate?.let { countStudyDaysBetween(today, it, plan.offDays) }

        // Real completion ticks, keyed on the day a topic was actually finished.
        // Drives both consistency (streak/heatmap) and the honest recent-pace signal.
        val completedByDate = completedTopicsByDate(refs)
        val recentTopicsPerStudyDay =
            computeRecentTopicsPerStudyDay(completedByDate, plan.offDays, today)
        val velocityForecastIso = when {
            examDate == null -> null
            remainingTopics == 0 -> todayIso
            recentTopicsPerStudyDay != null && recentTopicsPerStudyDay > 0f ->
                simulateForecastAtRate(remainingEquivalents, recentTopicsPerStudyDay, plan.offDays, today)
            else -> null
        }

        val requiredTopicsPerStudyDay =
            when {
                availableStudyDays == null -> null
                availableStudyDays == 0 -> if (remainingTopics > 0) remainingEquivalents else 0f
                else -> (remainingEquivalents / availableStudyDays)
            }

        val forecastDateIso = if (examDate != null && remainingTopics > 0) {
            simulateForecastCompletionDate(remainingEquivalents, dailyGoal, plan.offDays, today)
        } else if (examDate != null && remainingTopics == 0) {
            todayIso
        } else null

        val daysBuffer = when {
            examDate != null && forecastDateIso != null -> {
                val forecast = LocalDate.parse(forecastDateIso.take(10))
                when {
                    !forecast.isAfter(examDate) ->
                        countStudyDaysBetween(forecast.plusDays(1), examDate, plan.offDays)
                    else ->
                        -countStudyDaysBetween(examDate.plusDays(1), forecast, plan.offDays)
                }
            }
            remainingTopics == 0 && examDate != null ->
                countStudyDaysBetween(today.plusDays(1), examDate, plan.offDays)
            else -> null
        }

        val unfinishedScheduledBeforeExam = examDate?.let { ex ->
            refs.count { ref ->
                val pd = ref.topic.plannedDate?.take(10) ?: return@count false
                ref.topic.status != TopicStatus.DONE &&
                    runCatching { LocalDate.parse(pd) }.getOrNull()?.let { !it.isAfter(ex) } == true
            }
        } ?: 0

        val scheduleCoveragePercent =
            if (plan.examDate.isNullOrBlank()) null
            else if (remainingTopics == 0) 100
            else if (remainingTopics > 0) ((unfinishedScheduledBeforeExam.toFloat() / remainingTopics) * 100).roundToInt().coerceIn(0, 100)
            else null

        val onTrackStatus = computeOnTrackStatus(
            requiredTopicsPerStudyDay,
            dailyGoal.toFloat(),
            daysBuffer,
            remainingTopics,
        )

        val summary = PlannerInsightSummary(
            completionPercent = completionPercent,
            remainingTopics = remainingTopics,
            daysUntilExam = daysUntilExam,
            availableStudyDays = availableStudyDays,
            requiredTopicsPerStudyDay = requiredTopicsPerStudyDay,
            onTrackStatus = onTrackStatus,
            forecastCompletionDate = forecastDateIso,
            daysBuffer = daysBuffer,
            scheduleCoveragePercent = scheduleCoveragePercent,
            recentTopicsPerStudyDay = recentTopicsPerStudyDay,
            velocityForecastCompletionDate = velocityForecastIso,
        )

        // Day loads are weighted by effort: a day is "overloaded" when its
        // planned points exceed the internal daily budget (dailyGoal × 2),
        // not when it merely holds many (possibly small) topics.
        val pointsByTopicId = refs.associate { it.topic.id to it.topic.effortPoints(it.chapter).toFloat() }
        val workload = buildWorkload(calendar, todayIso, pointsByTopicId, dailyGoal * 2f)

        val consistency = buildConsistency(completedByDate, analytics?.heatmap, todayIso, calendar)

        val subjectRows = plan.subjects.map { sub ->
            val stTopics = sub.chapters.flatMap { it.topics }
            val rem = stTopics.count { it.status != TopicStatus.DONE }
            PlannerInsightSubjectRow(
                subjectId = sub.id,
                subjectName = sub.name,
                subjectColor = sub.color,
                completionPercent = sub.percentDone(),
                remainingTopics = rem,
                overdueTopics = stTopics.count { t ->
                    val pd = t.plannedDate?.take(10) ?: return@count false
                    pd < todayIso && t.status != TopicStatus.DONE
                },
                revisionTopics = stTopics.count { it.status == TopicStatus.REVISION_NEEDED },
            )
        }

        val laggingChapters = plan.subjects.flatMap { sub ->
            sub.chapters.mapNotNull { ch ->
                val chTopics = ch.topics
                if (chTopics.isEmpty()) return@mapNotNull null
                val rem = chTopics.count { it.status != TopicStatus.DONE }
                val overdue = chTopics.count { t ->
                    val pd = t.plannedDate?.take(10) ?: return@count false
                    pd < todayIso && t.status != TopicStatus.DONE
                }
                if (rem == 0 && overdue == 0) return@mapNotNull null
                PlannerInsightLaggingChapter(
                    subjectName = sub.name,
                    chapterName = ch.name,
                    remainingTopics = rem,
                    completionPercent = ch.percentDone(),
                    overdueTopics = overdue,
                )
            }
        }.sortedByDescending { it.overdueTopics }.take(8)

        val backlog = buildBacklog(refs, todayIso)

        val recommendations = buildRecommendations(summary, workload, backlog, remainingTopics)

        return PlannerInsights(
            summary = summary,
            workload = workload,
            consistency = consistency,
            subjectRows = subjectRows,
            laggingChapters = laggingChapters,
            backlog = backlog,
            recommendations = recommendations,
        )
    }

    private fun computeOnTrackStatus(
        requiredPerDay: Float?,
        dailyGoal: Float,
        daysBuffer: Int?,
        remaining: Int,
    ): InsightTrackStatus {
        if (remaining == 0) return InsightTrackStatus.ON_TRACK
        if (requiredPerDay == null || daysBuffer == null) return InsightTrackStatus.NEEDS_DATA
        return when {
            requiredPerDay <= dailyGoal && (daysBuffer ?: 0) >= 0 -> InsightTrackStatus.ON_TRACK
            requiredPerDay <= dailyGoal * 1.15f -> InsightTrackStatus.AT_RISK
            requiredPerDay > dailyGoal * 1.15f -> InsightTrackStatus.BEHIND
            else -> InsightTrackStatus.ON_TRACK
        }
    }

    private fun countStudyDaysBetween(start: LocalDate, end: LocalDate, offDays: List<Int>): Int {
        if (end.isBefore(start)) return 0
        val off = offDays.toSet()
        var count = 0
        var cursor = start
        while (!cursor.isAfter(end)) {
            if (jsDayOfWeek(cursor) !in off) count++
            cursor = cursor.plusDays(1)
        }
        return count
    }

    private fun simulateForecastCompletionDate(
        remainingEquivalents: Float,
        dailyGoal: Int,
        offDays: List<Int>,
        startDate: LocalDate,
    ): String? {
        if (remainingEquivalents <= 0f) return startDate.toString()
        val goal = max(1, dailyGoal)
        val off = offDays.toSet()
        var cursor = startDate
        var topicsLeft = remainingEquivalents
        repeat(3660) {
            if (jsDayOfWeek(cursor) !in off) {
                topicsLeft -= goal
                if (topicsLeft <= 0f) return cursor.toString()
            }
            cursor = cursor.plusDays(1)
        }
        return null
    }

    private fun buildWorkload(
        calendar: Map<String, List<CalendarTopicItem>>,
        todayIso: String,
        pointsByTopicId: Map<String, Float> = emptyMap(),
        dailyPointBudget: Float = 6f,
    ): PlannerInsightWorkload {
        val today = LocalDate.parse(todayIso)
        val next14 = (0 until 14).map { offset ->
            val d = today.plusDays(offset.toLong())
            val key = d.toString()
            val items = calendar[key].orEmpty()
            PlannerInsightDayLoad(
                date = key,
                plannedCount = items.size,
                doneCount = items.count { it.status == TopicStatus.DONE },
            )
        }
        // Overload is judged in effort points against the daily budget, so a
        // day of many small topics is not flagged while one stacked with big
        // topics is.
        val overloadDays = (0 until 14).count { offset ->
            val key = today.plusDays(offset.toLong()).toString()
            val points = calendar[key].orEmpty().sumOf { item ->
                (pointsByTopicId[item.topicId] ?: 2f).toDouble()
            }
            points > dailyPointBudget
        }
        val emptyStudyDays = next14.count { it.plannedCount == 0 }
        val busiest = next14.maxByOrNull { it.plannedCount }?.takeIf { it.plannedCount > 0 }

        return PlannerInsightWorkload(
            next14Days = next14,
            overloadDays = overloadDays,
            emptyStudyDays = emptyStudyDays,
            busiestDay = busiest,
            busiestSubjectUpcoming = null,
        )
    }

    private fun completedTopicsByDate(refs: List<TopicRef>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        for (ref in refs) {
            if (ref.topic.status != TopicStatus.DONE) continue
            // Only count a real completion tick. A streak / heatmap must reflect
            // days the student actually finished something — falling back to
            // plannedDate would credit activity to days nothing was studied and
            // inflate the streak, so topics without a completedDate are skipped.
            val cd = ref.topic.completedDate?.take(10) ?: continue
            map[cd] = (map[cd] ?: 0) + 1
        }
        return map
    }

    /**
     * Actual pace: topics finished per study day across a recent trailing window.
     * Uses only real completion ticks, and divides by the study days in the
     * window (off-days excluded) so it is directly comparable to
     * [PlannerInsightSummary.requiredTopicsPerStudyDay]. Returns null when the
     * window contains no study days, 0f when nothing was completed lately.
     */
    private fun computeRecentTopicsPerStudyDay(
        completedByDate: Map<String, Int>,
        offDays: List<Int>,
        today: LocalDate,
        windowDays: Int = 14,
    ): Float? {
        val off = offDays.toSet()
        var studyDays = 0
        var completed = 0
        for (i in 0 until windowDays) {
            val d = today.minusDays(i.toLong())
            completed += completedByDate[d.toString()] ?: 0
            if (jsDayOfWeek(d) !in off) studyDays++
        }
        if (studyDays == 0) return null
        return completed.toFloat() / studyDays
    }

    /**
     * Forecast finish date if the student keeps completing [ratePerStudyDay]
     * topics on every future study day (off-days skipped). Fractional rates
     * accumulate across days so a pace of e.g. 1.5/day is honoured.
     */
    private fun simulateForecastAtRate(
        remainingTopics: Float,
        ratePerStudyDay: Float,
        offDays: List<Int>,
        startDate: LocalDate,
    ): String? {
        if (remainingTopics <= 0f) return startDate.toString()
        if (ratePerStudyDay <= 0f) return null
        val off = offDays.toSet()
        var cursor = startDate
        var accomplished = 0f
        repeat(3660) {
            if (jsDayOfWeek(cursor) !in off) {
                accomplished += ratePerStudyDay
                if (accomplished >= remainingTopics) return cursor.toString()
            }
            cursor = cursor.plusDays(1)
        }
        return null
    }

    private fun buildConsistency(
        completedByDate: Map<String, Int>,
        apiHeatmap: List<HeatmapPoint>?,
        todayIso: String,
        calendar: Map<String, List<CalendarTopicItem>>,
    ): PlannerInsightConsistency {
        val today = LocalDate.parse(todayIso)
        val heatmapCells = if (!apiHeatmap.isNullOrEmpty()) {
            apiHeatmap.map { HeatmapCell(it.date, it.count) }
        } else {
            (0 until 30).map { i ->
                val d = today.minusDays((29 - i).toLong())
                val k = d.toString()
                HeatmapCell(k, completedByDate[k] ?: 0)
            }
        }

        val activeLast14 = heatmapCells.takeLast(14).count { it.count > 0 }
        val activeLast30 = heatmapCells.count { it.count > 0 }

        val weekdayTotals = IntArray(7)
        for (cell in heatmapCells) {
            if (cell.count <= 0) continue
            val idx = jsDayOfWeek(LocalDate.parse(cell.date.take(10)))
            weekdayTotals[idx] += cell.count
        }
        val bestDow = weekdayTotals.indices.maxByOrNull { weekdayTotals[it] } ?: 0
        val dayLabels = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val bestLabel = dayLabels[bestDow.coerceIn(0, 6)]

        val streak = computeStreak(completedByDate, todayIso)

        val pastDays = calendar.keys
            .filter { it < todayIso }
            .sortedDescending()
            .map { key ->
                val items = calendar[key].orEmpty()
                PlannerInsightDayLoad(
                    date = key,
                    plannedCount = items.size,
                    doneCount = items.count { it.status == TopicStatus.DONE }
                )
            }
        val missedDays = pastDays.filter { it.plannedCount > 0 && it.doneCount < it.plannedCount }

        return PlannerInsightConsistency(
            studyStreak = streak,
            activeDaysLast14 = activeLast14,
            activeDaysLast30 = activeLast30,
            bestStudyWeekday = if (weekdayTotals.sum() > 0) bestLabel else "No recent study",
            heatmap = heatmapCells.takeLast(14),
            missedDays = missedDays,
        )
    }

    private fun computeStreak(
        completedByDate: Map<String, Int>,
        todayIso: String,
    ): Int {
        var streak = 0
        var cursor = LocalDate.parse(todayIso)

        // A study streak means at least one completed topic on a day. Keep an
        // in-progress today from breaking yesterday's live streak.
        if ((completedByDate[todayIso] ?: 0) <= 0) {
            cursor = cursor.minusDays(1)
        }

        repeat(730) {
            val key = cursor.toString()
            if ((completedByDate[key] ?: 0) <= 0) return streak
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun buildBacklog(refs: List<TopicRef>, todayIso: String): PlannerInsightBacklog {
        val today = LocalDate.parse(todayIso)
        var o1 = 0
        var o2 = 0
        var o3 = 0
        var unplanned = 0
        var rev = 0
        for (ref in refs) {
            val t = ref.topic
            if (t.status == TopicStatus.REVISION_NEEDED) rev++
            val pd = t.plannedDate?.take(10)
            if (t.status != TopicStatus.DONE && pd.isNullOrBlank()) unplanned++
            if (pd != null && t.status != TopicStatus.DONE) {
                val d = runCatching { LocalDate.parse(pd) }.getOrNull() ?: continue
                if (d.isBefore(today)) {
                    val daysPast = ChronoUnit.DAYS.between(d, today).toInt()
                    when {
                        daysPast in 1..3 -> o1++
                        daysPast in 4..7 -> o2++
                        daysPast >= 8 -> o3++
                    }
                }
            }
        }
        val overdueTotal = o1 + o2 + o3
        return PlannerInsightBacklog(
            overdueTotal = overdueTotal,
            overdue1to3 = o1,
            overdue4to7 = o2,
            overdue8Plus = o3,
            unplannedUnfinished = unplanned,
            revisionNeeded = rev,
        )
    }

    private fun buildRecommendations(
        summary: PlannerInsightSummary,
        workload: PlannerInsightWorkload,
        backlog: PlannerInsightBacklog,
        remaining: Int,
    ): List<String> {
        val out = mutableListOf<String>()
        if (remaining == 0) out += "Plan complete — keep revision cadence if exams are still ahead."
        if (summary.daysBuffer != null && summary.daysBuffer < 0) {
            out += "Forecast finishes after your exam date — raise daily pace or reschedule."
        }
        if (workload.overloadDays >= 3) {
            out += "Several upcoming days look overloaded — redistribute topics from Syllabus or reschedule."
        }
        if (backlog.overdueTotal > 0) {
            out += "Clear ${backlog.overdueTotal} overdue topics first (Today tab)."
        }
        if (backlog.unplannedUnfinished > 0) {
            out += "${backlog.unplannedUnfinished} topics still need dates — run Build Schedule or assign manually."
        }
        if (out.isEmpty()) out += "Stay consistent with your daily goal and review Insights after schedule changes."
        return out.distinct()
    }
}
