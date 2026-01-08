package com.thinh.snaplet.data.repository.auth

import AuthState
import com.thinh.snaplet.data.datasource.local.datastore.DataStoreManager
import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.data.model.LoginRequest
import com.thinh.snaplet.data.model.RegisterRequest
import com.thinh.snaplet.data.model.UserProfile
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.ApiResult
import com.thinh.snaplet.utils.network.mapSuccess
import com.thinh.snaplet.utils.network.safeApiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService, private val dataStoreManager: DataStoreManager
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)

    override val authState: StateFlow<AuthState> = _authState

    override suspend fun login(email: String, password: String): ApiResult<UserProfile> {
        return safeApiCall(apiCall = {
            apiService.login(body = LoginRequest(email, password))
        }, onSuccess = { result ->
            dataStoreManager.saveTokens(
                result.token.accessToken, result.token.refreshToken
            )
            dataStoreManager.saveUserProfile(result.user)
        }).mapSuccess { response ->
            response.user
        }
    }

    override suspend fun register(
        email: String, username: String, firstName: String, lastName: String, password: String
    ): Result<UserProfile> {
        return try {
            val request = RegisterRequest(
                email = email,
                username = username,
                firstName = firstName,
                lastName = lastName,
                password = password
            )
            val response = apiService.register(body = request)

            if (response.isSuccessful) {
                val body = response.body()

                if (body == null || body.status.code != 201) {
                    val errorMsg = body?.status?.message
                    return Result.failure(Exception(errorMsg))
                }

                val result = body.data

                dataStoreManager.saveTokens(
                    accessToken = result.token.accessToken, refreshToken = result.token.refreshToken
                )
                dataStoreManager.saveUserProfile(result.user)

                Result.success(result.user)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Logger.e("❌ Failed to register: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        dataStoreManager.clearSession()
        _authState.value = AuthState.Unauthenticated
    }

    override suspend fun isAuthenticated(): Boolean {
        val authenticated =
            dataStoreManager.loadAccessToken() != null && dataStoreManager.loadRefreshToken() != null && dataStoreManager.loadUserProfile() != null

        _authState.value = if (authenticated) AuthState.Authenticated
        else AuthState.Unauthenticated

        return authenticated
    }

    override suspend fun checkEmailAvailability(email: String): ApiResult<Boolean> {
        return safeApiCall(apiCall = {
            apiService.checkEmailAvailability(email)
        }).mapSuccess { response ->
            response.available
        }
    }

    override suspend fun checkUsernameAvailability(username: String): ApiResult<Boolean> {
        return safeApiCall(apiCall = {
            apiService.checkUsernameAvailability(username)
        }).mapSuccess { response ->
            response.available
        }
    }
}