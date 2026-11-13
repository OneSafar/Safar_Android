package com.safarparmar.app.feature.youtubestudyv2

/** PiP has no reliable owner row: only an identified productive channel or a live break may play. */
internal fun shouldBlockYoutubePip(
    quickUnlockActive: Boolean,
    classification: YoutubeChannelClassification?,
): Boolean = classification != YoutubeChannelClassification.PRODUCTIVE

/** Accessibility overlays must never cover the lock screen, calls, or another app. */
internal fun shouldKeepYoutubeBlockOverlay(
    screenAvailable: Boolean,
    youtubeForeground: Boolean,
): Boolean = screenAvailable && youtubeForeground

/** How long a floating player may play while a just-seen owner is still being evaluated. */
internal const val FLOATING_IDENTITY_GRACE_MS = 3_000L

internal enum class FloatingPlaybackDecision { ALLOW, WAIT, BLOCK }

/**
 * The user can minimise a video before its owner evaluation finishes. Then the
 * classification is still unknown (null), but the owner was already seen on
 * the watch page. Wait briefly for that result instead of blocking an allowed
 * channel. An explicit non-productive result, or no candidate, still blocks.
 */
internal fun floatingPlaybackDecision(
    classification: YoutubeChannelClassification?,
    candidateAgeMs: Long?,
): FloatingPlaybackDecision = when {
    classification == YoutubeChannelClassification.PRODUCTIVE -> FloatingPlaybackDecision.ALLOW
    classification == null && candidateAgeMs != null && candidateAgeMs in 0..FLOATING_IDENTITY_GRACE_MS ->
        FloatingPlaybackDecision.WAIT
    else -> FloatingPlaybackDecision.BLOCK
}
