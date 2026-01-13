package com.thinh.snaplet.data.repository.device

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class DeviceInfo(
    @SerializedName("deviceId")
    val deviceId: String,
    
    @SerializedName("platform")
    val platform: String,
    
    @SerializedName("model")
    val model: String,
    
    @SerializedName("appVersion")
    val appVersion: String,
    
    @SerializedName("ip")
    val ip: String,
    
    @SerializedName("userAgent")
    val userAgent: String
)