package com.safarparmar.app.ui.ekagra.focusshield

/**
 * Chooses how often Always On should ask the OS which app is in front.
 *
 * Timer-bound Kavach can afford to poll every 300 ms because a study session is
 * half an hour. Always On runs for the student's entire waking day, and polling
 * that hard for sixteen hours is a battery complaint and an Android vitals
 * problem — the exact reason an all-day blocker gets uninstalled.
 *
 * So the interval breathes: it stays fast while apps are being switched, and
 * relaxes once the foreground app has been sitting still. A student reading one
 * page for ten minutes does not need to be checked on 2,000 times, and the moment
 * anything changes the scheduler snaps back to fast so a blocked app is still
 * caught almost immediately.
 */
class AdaptivePollScheduler(
    private val fastIntervalMs: Long = FAST_MS,
    private val slowIntervalMs: Long = SLOW_MS,
    private val stableSamplesBeforeBackoff: Int = STABLE_SAMPLES,
) {
    private var lastPackage: String? = null
    private var stableCount: Int = 0

    /** Current wait before the next poll. */
    var intervalMs: Long = fastIntervalMs
        private set

    /**
     * Feeds the latest observation in and returns the interval to wait next.
     *
     * @param foregroundPackage what is in front now, or null when unknown.
     * @param isBlockedApp true when a blocked app is in front — never back off then,
     *   because that is precisely when we are about to act.
     */
    fun onSample(foregroundPackage: String?, isBlockedApp: Boolean): Long {
        if (isBlockedApp || foregroundPackage != lastPackage) {
            lastPackage = foregroundPackage
            stableCount = 0
            intervalMs = fastIntervalMs
            return intervalMs
        }

        stableCount += 1
        intervalMs = if (stableCount >= stableSamplesBeforeBackoff) {
            // Ramp rather than jumping straight to the slowest rate, so a student
            // who pauses briefly and then switches apps is still caught quickly.
            val steps = (stableCount - stableSamplesBeforeBackoff) / stableSamplesBeforeBackoff + 1
            (fastIntervalMs * (1L shl steps.coerceAtMost(MAX_DOUBLINGS))).coerceAtMost(slowIntervalMs)
        } else {
            fastIntervalMs
        }
        return intervalMs
    }

    /** Called when blocking state changes underneath us, e.g. the app list was edited. */
    fun reset() {
        lastPackage = null
        stableCount = 0
        intervalMs = fastIntervalMs
    }

    companion object {
        const val FAST_MS = 400L
        const val SLOW_MS = 2_000L
        private const val STABLE_SAMPLES = 5
        private const val MAX_DOUBLINGS = 4
    }
}
