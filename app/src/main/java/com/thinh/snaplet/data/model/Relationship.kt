package com.thinh.snaplet.data.model

import com.google.gson.annotations.SerializedName

/**
 * Relationship status: pending | accepted | blocked
 */
enum class RelationshipStatus(val value: String) {
    @SerializedName("pending")
    PENDING("pending"),

    @SerializedName("accepted")
    ACCEPTED("accepted"),

    @SerializedName("blocked")
    BLOCKED("blocked");

    companion object {
        fun from(value: String): RelationshipStatus? =
            entries.find { it.value.equals(value, ignoreCase = true) }
    }
}

data class UpdateRelationshipRequest(
    @SerializedName("status")
    val status: String
)

data class Relationship(
    @SerializedName("id")
    val id: String,

    @SerializedName("user1Id")
    val user1Id: String,

    @SerializedName("user2Id")
    val user2Id: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("initiator")
    val initiator: String,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String
)

data class FriendsCountData(
    @SerializedName("count")
    val count: Int
)

/**
 * API response item for GET /relationships (status as string from server).
 */
data class RelationshipWithUserDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("userId")
    val userId: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("firstName")
    val firstName: String,

    @SerializedName("lastName")
    val lastName: String,

    @SerializedName("avatarUrl")
    val avatarUrl: String? = null,

    @SerializedName("status")
    val status: String,

    @SerializedName("createdAt")
    val createdAt: String
) {
    fun toDomain(): RelationshipWithUser {
        val statusEnum = RelationshipStatus.from(status)
            ?: throw IllegalArgumentException("Unknown relationship status: $status")
        return RelationshipWithUser(
            id = id,
            userId = userId,
            username = username,
            firstName = firstName,
            lastName = lastName,
            avatarUrl = avatarUrl,
            status = statusEnum,
            createdAt = createdAt
        )
    }
}

data class RelationshipWithUser(
    val id: String,
    val userId: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String? = null,
    val status: RelationshipStatus,
    val createdAt: String
) {
    val displayName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { username }
}
