package com.thinh.snaplet.data.repository

import com.thinh.snaplet.data.datasource.local.datastore.DataStoreManager
import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.data.model.Relationship
import com.thinh.snaplet.data.model.RelationshipStatus
import com.thinh.snaplet.data.model.RelationshipWithUser
import com.thinh.snaplet.data.model.RelationshipWithUserDto
import com.thinh.snaplet.data.model.UpdateRelationshipRequest
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

    override suspend fun getRelationshipWithUser(userId: String): ApiResult<Relationship?> {
        return safeApiCall(
            apiCall = {
                apiService.getRelationshipWithUser(mapOf("targetUserId" to userId))
            }
        )
    }

    override suspend fun getFriendsCount(): ApiResult<Int> {
        return safeApiCall(
            apiCall = { apiService.getFriendsCount() },
            transform = { it.count }
        )
    }

    override suspend fun getMyFriendList(): ApiResult<List<RelationshipWithUser>> {
        return safeApiCall(
            apiCall = { apiService.getRelationshipsByStatus(RelationshipStatus.ACCEPTED.value) },
            transform = { list: List<RelationshipWithUserDto> -> list.map { it.toDomain() } }
        )
    }

    override suspend fun getRelationshipsByStatuses(statuses: List<RelationshipStatus>): ApiResult<List<RelationshipWithUser>> {
        if (statuses.isEmpty()) return ApiResult.Success(emptyList())
        val statusesQuery = statuses.joinToString(",") { it.value }
        return safeApiCall(
            apiCall = { apiService.getRelationshipsByStatus(statusesQuery) },
            transform = { list: List<RelationshipWithUserDto> -> list.map { it.toDomain() } }
        )
    }

    override suspend fun acceptFriendRequest(relationshipId: String): ApiResult<Relationship> {
        return safeApiCall(
            apiCall = {
                apiService.updateRelationship(
                    relationshipId,
                    UpdateRelationshipRequest(status = RelationshipStatus.ACCEPTED.value)
                )
            }
        )
    }

    override suspend fun removeFriend(relationshipId: String): ApiResult<Unit> {
        return safeApiCall(
            apiCall = { apiService.removeRelationship(relationshipId) }
        )
    }

    override fun observeMyUserProfile(): Flow<UserProfile?> {
        return dataStoreManager.getUserProfileFlow()
    }

    override suspend fun getCurrentUserProfile(): UserProfile? {
        return dataStoreManager.loadUserProfile()
    }
}

