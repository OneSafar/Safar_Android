package com.safarparmar.app.feature.youtubestudyv2

import java.util.concurrent.atomic.AtomicLong

/** A late Android service start must not resurrect a stopped protection notification. */
internal class YoutubeGuardStartRequests {
    private val generation = AtomicLong()

    fun requestStart(): Long = generation.incrementAndGet()

    fun invalidate() { generation.incrementAndGet() }

    fun canRun(request: Long, enabled: Boolean, connected: Boolean): Boolean =
        enabled && connected && request > 0L && request == generation.get()
}
