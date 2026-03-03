package com.thinh.snaplet.data.repository

import com.thinh.snaplet.data.model.Relationship
import com.thinh.snaplet.data.model.RelationshipStatus
import com.thinh.snaplet.data.model.RelationshipWithUser
import com.thinh.snaplet.data.model.UserProfile
import com.thinh.snaplet.utils.network.ApiResult
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    suspend fun getUserProfile(userName: String): ApiResult<UserProfile>
    
    suspend fun sendFriendRequest(userId: String): ApiResult<Relationship>

    suspend fun getRelationshipWithUser(userId: String): ApiResult<Relationship?>

    suspend fun getFriendsCount(): ApiResult<Int>

    suspend fun getMyFriendList(): ApiResult<List<RelationshipWithUser>>

    suspend fun getRelationshipsByStatuses(statuses: List<RelationshipStatus>): ApiResult<List<RelationshipWithUser>>

    suspend fun acceptFriendRequest(relationshipId: String): ApiResult<Relationship>

    suspend fun removeFriend(relationshipId: String): ApiResult<Unit>

    fun observeMyUserProfile(): Flow<UserProfile?>
    
    suspend fun getCurrentUserProfile(): UserProfile?
}

