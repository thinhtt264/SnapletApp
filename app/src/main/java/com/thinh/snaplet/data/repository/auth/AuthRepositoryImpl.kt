package com.thinh.snaplet.data.repository.auth

import AuthState
import com.thinh.snaplet.data.datasource.local.datastore.DataStoreManager
import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.data.model.LoginRequest
import com.thinh.snaplet.data.model.RegisterRequest
import com.thinh.snaplet.data.model.UserProfile
import com.thinh.snaplet.utils.network.ApiResult
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
        return safeApiCall(
            apiCall = {
                apiService.login(body = LoginRequest(email, password))
            },
            onSuccess = { result ->
                dataStoreManager.saveTokens(
                    result.token.accessToken, result.token.refreshToken
                )
                dataStoreManager.saveUserProfile(result.user)
            },
            transform = { response -> response.user }
        )
    }

    override suspend fun register(
        email: String, username: String, firstName: String, lastName: String, password: String
    ): ApiResult<UserProfile> {
        val request = RegisterRequest(
            email = email,
            username = username,
            firstName = firstName,
            lastName = lastName,
            password = password
        )

        return safeApiCall(
            apiCall = {
                apiService.register(body = request)
            },
            onSuccess = { result ->
                dataStoreManager.saveTokens(
                    accessToken = result.token.accessToken,
                    refreshToken = result.token.refreshToken
                )
                dataStoreManager.saveUserProfile(result.user)
            },
            transform = { response -> response.user }
        )
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
        return safeApiCall(
            apiCall = {
                apiService.checkEmailAvailability(email)
            },
            transform = { response -> response.available }
        )
    }

    override suspend fun checkUsernameAvailability(username: String): ApiResult<Boolean> {
        return safeApiCall(
            apiCall = {
                apiService.checkUsernameAvailability(username)
            },
            transform = { response -> response.available }
        )
    }
}