package com.thinh.snaplet.data.model.media

import com.google.gson.annotations.SerializedName

/**
 * Image transformation parameters for display and processing
 */
data class ImageTransform(
    @SerializedName("rotation")
    val rotation: Int = 0,
    
    @SerializedName("scaleX")
    val scaleX: Float = 1f,
    
    @SerializedName("scaleY")
    val scaleY: Float = 1f
)
