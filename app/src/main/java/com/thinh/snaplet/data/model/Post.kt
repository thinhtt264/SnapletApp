package com.thinh.snaplet.data.model

import com.google.gson.annotations.SerializedName

data class Post(
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

    @SerializedName("media")
    val media: List<Media>,

    @SerializedName("caption")
    val caption: String? = null,

    @SerializedName("visibility")
    val visibility: String,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("isOwnPost")
    val isOwnPost: Boolean,
) {
    val displayName: String
        get() = "$firstName $lastName"

    val url: String
        get() = media.firstOrNull()?.originalUrl ?: ""
}

typealias PostsFeedData = PaginatedResponse<Post>
