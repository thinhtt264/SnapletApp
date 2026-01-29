package com.thinh.snaplet.data.model.media

import com.google.gson.annotations.SerializedName

data class Media(
    @SerializedName("id")
    val id: String,

    @SerializedName("mimeType")
    val type: String,

    @SerializedName("originalUrl")
    val originalUrl: String? = "",

    @SerializedName("ownerId")
    val ownerId: String,

    @SerializedName("transform")
    val transform: ImageTransform? = null
)
