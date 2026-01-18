package com.thinh.snaplet.network

import com.thinh.snaplet.data.datasource.local.datastore.DataStoreManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor for adding authentication headers to requests
 * 
 * This interceptor has a single responsibility: add auth headers.
 * All 401 handling and token refresh is delegated to TokenAuthenticator.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Get current access token
        val accessToken = dataStoreManager.getAccessToken()
        
        // Build request with standard headers
        val requestBuilder = originalRequest.newBuilder()
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")

        // Add Authorization header if token exists
        if (!accessToken.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $accessToken")
        }

        // Proceed with the request
        return chain.proceed(requestBuilder.build())
    }
}
