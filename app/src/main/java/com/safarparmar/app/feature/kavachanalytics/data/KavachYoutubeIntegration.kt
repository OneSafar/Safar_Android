package com.safarparmar.app.feature.kavachanalytics.data

import com.safarparmar.app.feature.kavachanalytics.domain.CategoryTotals
import com.safarparmar.app.feature.kavachanalytics.domain.AppCategory
import com.safarparmar.app.feature.kavachanalytics.data.local.YoutubeViewingIntervalEntity

/**
 * Replaces Android's single YouTube-app bucket with mutually exclusive content
 * buckets. Raw foreground time remains the floor. Explicit Study Mode browsing
 * is Others; time without a Study Mode observation follows the app category.
 */
internal fun youtubeCategoryTotals(
    rawYoutubeSeconds: Int,
    productiveSeconds: Int,
    distractingSeconds: Int,
    shortsSeconds: Int,
    unidentifiedSeconds: Int,
    fallbackCategory: AppCategory = AppCategory.UNCLASSIFIED,
): CategoryTotals {
    val productive = productiveSeconds.coerceAtLeast(0)
    val distracting = distractingSeconds.coerceAtLeast(0) + shortsSeconds.coerceAtLeast(0)
    val identifiedOthers = unidentifiedSeconds.coerceAtLeast(0)
    val measured = productive + distracting + identifiedOthers
    val total = maxOf(rawYoutubeSeconds.coerceAtLeast(0), measured)
    return CategoryTotals(
        productiveSeconds = productive,
        distractingSeconds = distracting,
        unclassifiedSeconds = identifiedOthers,
    ).add(fallbackCategory, total - measured)
}

/** Count only observed content inside the requested foreground/session windows. */
internal fun youtubeObservedTotals(
    viewing: List<YoutubeViewingIntervalEntity>,
    windows: List<LongRange>,
): CategoryTotals {
    val merged = UsageIntervalReconstructor.mergeWindows(windows)
    val milliseconds = mutableMapOf<AppCategory, Long>()
    viewing.forEach { row ->
        val category = when (row.category) {
            "productive" -> AppCategory.PRODUCTIVE
            "distracting", "shorts" -> AppCategory.DISTRACTING
            else -> AppCategory.UNCLASSIFIED
        }
        val duration = merged.sumOf {
            (minOf(row.endMs, it.last) - maxOf(row.startMs, it.first)).coerceAtLeast(0L)
        }
        milliseconds[category] = (milliseconds[category] ?: 0L) + duration
    }
    return milliseconds.entries.fold(CategoryTotals()) { total, (category, duration) ->
        total.add(category, (duration / 1000L).toInt())
    }
}
