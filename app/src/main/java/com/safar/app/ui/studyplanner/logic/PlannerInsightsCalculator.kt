package com.safar.app.ui.studyplanner.logic

import com.safar.app.domain.model.studyplanner.CalendarTopicItem
import com.safar.app.domain.model.studyplanner.HeatmapPoint
import com.safar.app.domain.model.studyplanner.PlannerAnalytics
import com.safar.app.domain.model.studyplanner.StudyPlan
import com.safar.app.domain.model.studyplanner.TopicStatus
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
        val totalTopics = topics.size
        val doneTopics = topics.count { it.status == TopicStatus.DONE }
        val completionPercent = if (totalTopics == 0) 0 else ((doneTopics.toFloat() / totalTopics) * 100).roundToInt()

        val examDate = parsePlannerDate(plan.examDate)
        val today = LocalDate.parse(todayIso)
        val daysUntilExam = examDate?.let { ChronoUnit.DAYS.between(today, it).toInt() }

        val dailyGoal = max(1, plan.dailyGoal ?: 1)
        val availableStudyDays = examDate?.let { countStudyDaysBetween(today, it, plan.offDays) }

        val requiredTopicsPerStudyDay =
            when {
                availableStudyDays == null -> null
                availableStudyDays == 0 -> if (remainingTopics > 0) remainingTopics.toFloat() else 0f
                else -> (remainingTopics.toFloat() / availableStudyDays)
            }

        val forecastDateIso = if (examDate != null && remainingTopics > 0) {
            simulateForecastCompletionDate(remainingTopics, dailyGoal, plan.offDays, today)
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
        )

        val workload = buildWorkload(calendar, todayIso)

        val completedByDate = completedTopicsByDate(refs)
        val consistency = buildConsistency(completedByDate, analytics?.heatmap, todayIso)

        val subjectRows = plan.subjects.map { sub ->
            val stTopics = sub.chapters.flatMap { it.topics }
            val rem = stTopics.count { it.status != TopicStatus.DONE }
            PlannerInsightSubjectRow(
                subjectId = sub.id,
                subjectName = sub.name,
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
        remainingTopics: Int,
        dailyGoal: Int,
        offDays: List<Int>,
        startDate: LocalDate,
    ): String? {
        if (remainingTopics <= 0) return startDate.toString()
        val goal = max(1, dailyGoal)
        val off = offDays.toSet()
        var cursor = startDate
        var topicsLeft = remainingTopics
        repeat(3660) {
            if (jsDayOfWeek(cursor) !in off) {
                topicsLeft -= goal
                if (topicsLeft <= 0) return cursor.toString()
            }
            cursor = cursor.plusDays(1)
        }
        return null
    }

    private fun buildWorkload(
        calendar: Map<String, List<CalendarTopicItem>>,
        todayIso: String,
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
        val overloadDays = next14.count { it.plannedCount >= 5 }
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
            val cd = ref.topic.completedDate?.take(10) ?: continue
            map[cd] = (map[cd] ?: 0) + 1
        }
        return map
    }

    private fun buildConsistency(
        completedByDate: Map<String, Int>,
        apiHeatmap: List<HeatmapPoint>?,
        todayIso: String,
    ): PlannerInsightConsistency {
        val heatmapCells = if (!apiHeatmap.isNullOrEmpty()) {
            apiHeatmap.map { HeatmapCell(it.date, it.count) }
        } else {
            val today = LocalDate.parse(todayIso)
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

        return PlannerInsightConsistency(
            studyStreak = streak,
            activeDaysLast14 = activeLast14,
            activeDaysLast30 = activeLast30,
            bestStudyWeekday = if (weekdayTotals.sum() > 0) bestLabel else "No recent study",
            heatmap = heatmapCells.takeLast(14),
        )
    }

    private fun computeStreak(completedByDate: Map<String, Int>, todayIso: String): Int {
        var streak = 0
        var cursor = LocalDate.parse(todayIso)
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
