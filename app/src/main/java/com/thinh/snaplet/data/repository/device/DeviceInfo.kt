package com.thinh.snaplet.data.repository.device

/**
 * Device Information Model
 * Represents device fingerprint data structure
 */
data class DeviceInfo(
    val deviceId: String,
    val platform: String,
    val model: String,
    val appVersion: String,
    val ip: String,
    val userAgent: String
)
