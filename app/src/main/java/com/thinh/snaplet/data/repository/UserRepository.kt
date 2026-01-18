package com.thinh.snaplet.data.repository

import com.thinh.snaplet.data.model.Relationship
import com.thinh.snaplet.data.model.UserProfile
import com.thinh.snaplet.utils.network.ApiResult
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    suspend fun getUserProfile(userName: String): ApiResult<UserProfile>
    
    suspend fun sendFriendRequest(userId: String): ApiResult<Relationship>
    
    fun observeMyUserProfile(): Flow<UserProfile?>
}

