package com.thinh.snaplet.data.repository

import com.thinh.snaplet.data.datasource.local.datastore.DataStoreManager
import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.data.model.Relationship
import com.thinh.snaplet.data.model.UserProfile
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.ApiResult
import com.thinh.snaplet.utils.network.safeApiCall
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
) : UserRepository {

    override suspend fun getUserProfile(userName: String): ApiResult<UserProfile> {
        return safeApiCall(
            apiCall = {
                apiService.getUserProfile(userName)
            }
        )
    }

    override suspend fun sendFriendRequest(userId: String): ApiResult<Relationship> {
        Logger.d("📤 Sending friend request to user ID: $userId")
        
        return safeApiCall(
            apiCall = {
                val requestBody = mapOf("targetUserId" to userId)
                apiService.sendFriendRequest(requestBody)
            },
            onSuccess = { relationship ->
                Logger.d("✅ Friend request sent successfully. Relationship ID: ${relationship.id}, Status: ${relationship.status}")
            }
        )
    }

    override fun observeMyUserProfile(): Flow<UserProfile?> {
        return dataStoreManager.getUserProfileFlow()
    }

    override suspend fun getCurrentUserProfile(): UserProfile? {
        return dataStoreManager.loadUserProfile()
    }
}

