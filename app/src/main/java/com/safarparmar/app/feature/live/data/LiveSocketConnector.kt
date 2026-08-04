package com.safarparmar.app.feature.live.data

import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.data.remote.socket.MehfilSocketManager
import com.safarparmar.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens the Mehfil socket for the live-session surfaces.
 *
 * The socket used to be connected only by MehfilViewModel, so a student who went
 * straight to a live session without opening Mehfil first sat on "Connecting…"
 * forever and every comment failed. Both the live session screen and the
 * fullscreen player need the same connection, so the logic lives here rather
 * than being copied into each.
 *
 * [MehfilSocketManager.connect] reuses an existing socket for the same user and
 * reconnects a dropped one, so calling this is safe even when Mehfil already has
 * one open.
 */
@Singleton
class LiveSocketConnector @Inject constructor(
    private val socketManager: MehfilSocketManager,
    private val dataStore: SafarDataStore,
    private val authRepository: AuthRepository,
) {
    sealed interface Result {
        /** A socket is connected, or a connection attempt is now under way. */
        data object Connecting : Result

        /** No usable credentials — the student needs to sign in again. */
        data class SignInRequired(val message: String) : Result
    }

    suspend fun ensureConnected(): Result {
        if (socketManager.isConnected()) return Result.Connecting

        // A lightweight authenticated call first, so the interceptor can refresh an
        // expired access token before Socket.IO authenticates the handshake.
        runCatching { authRepository.getMe() }

        val token = dataStore.authToken.first()
        val userId = dataStore.userId.first()
        if (token.isNullOrBlank() || userId.isNullOrBlank()) {
            return Result.SignInRequired("Please sign in again to use live chat.")
        }

        socketManager.connect(
            token = token,
            userId = userId,
            userName = dataStore.userName.first() ?: "Safarite",
            userAvatar = dataStore.userAvatar.first(),
        )
        return Result.Connecting
    }

    suspend fun currentUserName(): String = dataStore.userName.first() ?: "Student"
}
