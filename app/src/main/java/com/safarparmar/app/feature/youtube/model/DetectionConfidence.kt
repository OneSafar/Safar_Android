package com.safarparmar.app.feature.youtube.model

/**
 * Confidence score for YouTube content detection fused from Accessibility
 * and MediaSession signals.
 */
enum class DetectionConfidence {
    HIGH,
    MEDIUM,
    LOW,
    UNKNOWN,
}
