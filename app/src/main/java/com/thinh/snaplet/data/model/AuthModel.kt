package com.thinh.snaplet.data.model

import com.google.gson.annotations.SerializedName

/**
 * Authentication Models
 */

/**
 * Login Request
 */
data class LoginRequest(
    @SerializedName("email")
    val email: String,
    
    @SerializedName("password")
    val password: String
)

/**
 * Register Request
 */
data class RegisterRequest(
    @SerializedName("email")
    val email: String,
    
    @SerializedName("username")
    val username: String,
    
    @SerializedName("firstName")
    val firstName: String,
    
    @SerializedName("lastName")
    val lastName: String,
    
    @SerializedName("password")
    val password: String
)

/**
 * Token Response
 */
data class TokenResponse(
    @SerializedName("accessToken")
    val accessToken: String,
    
    @SerializedName("refreshToken")
    val refreshToken: String
)

/**
 * Login Response
 */
data class LoginResponse(
    @SerializedName("token")
    val token: TokenResponse,
    
    @SerializedName("user")
    val user: UserProfile
)

/**
 * Email Availability
 */
data class EmailAvailabilityData(
    @SerializedName("available")
    val available: Boolean
)

/**
 * Username Availability
 */
data class UsernameAvailabilityData(
    @SerializedName("available")
    val available: Boolean
)

/**
 * Refresh Token Request
 */
data class RefreshTokenRequest(
    @SerializedName("refreshToken")
    val refreshToken: String,
    
    @SerializedName("accessToken")
    val accessToken: String
)

