package com.safarparmar.app.ui.ekagra

import com.safarparmar.app.data.remote.dto.EkagraSession
import com.safarparmar.app.util.Resource
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class EkagraSessionSyncAttemptTest {
    @Test fun acknowledgementFailureReturnsRetryableErrorAndPreservesTheSameSessionForRetry() = runTest {
        val clientId = "local-two-hours"
        val queuedSeconds = 2 * 60 * 60
        val uploadedIds = mutableListOf<String>()
        val uploadedSeconds = mutableListOf<Int>()
        var acknowledged = false
        var storageAvailable = false

        suspend fun upload(): Resource<EkagraSession> = attemptEkagraSessionSync(
            onFailure = { Resource.Error("Retry from History") },
        ) {
            // The save has reached the server, but writing its acknowledgement
            // to the phone can still fail. The queue must remain retryable.
            uploadedIds += clientId
            uploadedSeconds += queuedSeconds
            if (!storageAvailable) throw IOException("disk full while acknowledging")
            acknowledged = true
            Resource.Success(EkagraSession(id = "server-two-hours"))
        }

        val failed = upload()
        assertTrue(failed is Resource.Error)
        assertFalse(acknowledged)

        storageAvailable = true
        val retried = upload()
        assertEquals("server-two-hours", (retried as Resource.Success).data.id)
        assertTrue(acknowledged)
        assertEquals(listOf(clientId, clientId), uploadedIds)
        assertEquals(listOf(7200, 7200), uploadedSeconds)
    }

    @Test fun queueReadFailureRequiresWorkerRetryInsteadOfReportingADrainAsSuccessful() = runTest {
        val drained = attemptEkagraSessionSync(onFailure = { false }) {
            throw IOException("journal could not be read")
            @Suppress("UNREACHABLE_CODE")
            true
        }
        assertFalse(drained)
    }

    @Test fun cancellationDoesNotBecomeASaveFailureOrSuccess() = runTest {
        val cancellation = CancellationException("screen was closed")
        var failureReported = false
        try {
            attemptEkagraSessionSync(onFailure = { failureReported = true; false }) {
                throw cancellation
            }
            fail("Cancellation must propagate")
        } catch (caught: CancellationException) {
            assertSame(cancellation, caught)
        }
        assertFalse(failureReported)
    }
}
