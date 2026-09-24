package com.safarparmar.app.ui.ekagra.focusshield

/** Main-thread-only ordering for service starts and asynchronous settings shutdowns. */
internal class KavachServiceLifecycle {
    private var latestStartId = 0

    fun start(startId: Int, promote: () -> Boolean): Boolean {
        latestStartId = startId
        return promote()
    }

    fun stopIfCurrent(startId: Int, stopSelfResult: (Int) -> Boolean): Boolean =
        startId == latestStartId && stopSelfResult(startId)
}
