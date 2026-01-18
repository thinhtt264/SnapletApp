package com.thinh.snaplet.network

import com.thinh.snaplet.data.repository.auth.AuthRepository
import com.thinh.snaplet.utils.Logger
import dagger.Lazy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRefreshCoordinator @Inject constructor(
    private val authRepository: Lazy<AuthRepository>
) {
    private val mutex = Mutex()

    // Shared deferred for all waiting requests
    private var ongoingRefresh: CompletableDeferred<String?>? = null

    suspend fun getNewAccessToken(): String? {
        mutex.withLock {
            // If there's already an ongoing refresh, return it
            val existingRefresh = ongoingRefresh
            if (existingRefresh != null) {
                Logger.d("⏳ Waiting for ongoing token refresh...")
                return existingRefresh.await()
            }

            // No ongoing refresh - start a new one
            Logger.d("🔒 Starting new token refresh...")
            val newRefresh = CompletableDeferred<String?>()
            ongoingRefresh = newRefresh

            return try {
                val result = authRepository.get().refreshToken()

                val newToken = result.fold(
                    onSuccess = { tokenResponse ->
                        Logger.d("✅ Token refreshed successfully")
                        tokenResponse.accessToken
                    },
                    onFailure = { error ->
                        Logger.e("❌ Token refresh failed: ${error.message}")
                        Logger.e("🚪 Forcing logout due to refresh failure")
                        authRepository.get().forceLogout()
                        null
                    }
                )

                newRefresh.complete(newToken)
                newToken
            } catch (e: Exception) {
                Logger.e("❌ Token refresh exception: ${e.message}")
                authRepository.get().forceLogout()
                newRefresh.complete(null)
                null
            } finally {
                ongoingRefresh = null
            }
        }
    }
}
