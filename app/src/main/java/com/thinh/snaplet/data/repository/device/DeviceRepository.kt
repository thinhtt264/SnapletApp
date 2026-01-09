package com.thinh.snaplet.data.repository.device

interface DeviceRepository {

    suspend fun getOrCreateFingerprint(): String

    suspend fun clearFingerprint()

    fun getFingerprintSync(): String
}
