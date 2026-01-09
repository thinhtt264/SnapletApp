package com.thinh.snaplet.network

import com.thinh.snaplet.data.repository.device.DeviceRepository
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FingerprintInterceptor @Inject constructor(
    private val deviceRepository: DeviceRepository
) : Interceptor {

    companion object {
        private const val HEADER_NAME = "X-Client-Fingerprint"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val fingerprint = deviceRepository.getFingerprintSync()

        val requestWithFingerprint = originalRequest.newBuilder()
            .addHeader(HEADER_NAME, fingerprint)
            .build()

        return chain.proceed(requestWithFingerprint)
    }
}
