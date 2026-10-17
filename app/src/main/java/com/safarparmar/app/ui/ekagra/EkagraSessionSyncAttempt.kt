package com.safarparmar.app.ui.ekagra

import kotlinx.coroutines.CancellationException

/**
 * A journal read or acknowledgement can fail after the HTTP request succeeds.
 * Keep that failure in the retry flow instead of throwing into a UI coroutine.
 * Cancellation still belongs to the caller and must stop the attempt.
 */
internal suspend fun <T> attemptEkagraSessionSync(
    onFailure: (Exception) -> T,
    operation: suspend () -> T,
): T = try {
    operation()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    onFailure(error)
}
