package com.thinh.snaplet.data.model.media

import com.google.gson.annotations.SerializedName

/**
 * Image URLs for different sizes
 * Based on ImageSizeKey enum from backend:
 * - XS: 64x64 (1:1) - Thumbnail / Icon
 * - SM: 256x256 (1:1) - Preview / Avatar
 * - MD: 512x512 (1:1) - Standard Square
 * - XL: 768x768 (1:1) - High-Res Square
 */
data class ImageSizes(
    @SerializedName("xs")
    val xs: String = "",

    @SerializedName("sm")
    val sm: String = "",

    @SerializedName("md")
    val md: String = "",

    @SerializedName("xl")
    val xl: String = ""
)

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
    val transform: ImageTransform? = null,

    @SerializedName("images")
    val images: ImageSizes = ImageSizes()
)
