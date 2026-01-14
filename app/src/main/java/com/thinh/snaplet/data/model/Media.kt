package com.thinh.snaplet.data.model

import com.google.gson.annotations.SerializedName

enum class MediaType {
    @SerializedName("IMAGE")
    IMAGE,
    
    @SerializedName("VIDEO")
    VIDEO
}

data class Media(
    @SerializedName("id")
    val id: String,

    @SerializedName("type")
    val type: MediaType,

    @SerializedName("originalUrl")
    val originalUrl: String,

    @SerializedName("thumbnailUrl")
    val thumbnailUrl: String? = null,

    @SerializedName("width")
    val width: Int? = null,

    @SerializedName("height")
    val height: Int? = null,
)
