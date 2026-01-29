package com.thinh.snaplet.data.datasource.remote

import com.thinh.snaplet.data.model.LoginRequest
import com.thinh.snaplet.data.model.LoginResponse
import com.thinh.snaplet.data.model.RegisterRequest
import com.thinh.snaplet.data.model.RefreshTokenRequest
import com.thinh.snaplet.data.model.TokenResponse
import com.thinh.snaplet.data.model.BaseResponse
import com.thinh.snaplet.data.model.PostsFeedData
import com.thinh.snaplet.data.model.Relationship
import com.thinh.snaplet.data.model.UserProfile
import com.thinh.snaplet.data.model.EmailAvailabilityData
import com.thinh.snaplet.data.model.UsernameAvailabilityData
import com.thinh.snaplet.data.model.media.RequestUploadRequest
import com.thinh.snaplet.data.model.media.UploadRequestData
import com.thinh.snaplet.data.model.media.ConfirmUploadRequest
import com.thinh.snaplet.data.model.media.ConfirmUploadData
import com.thinh.snaplet.data.model.CreatePostRequest
import com.thinh.snaplet.data.model.Post
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    
    @POST("auth/login")
    suspend fun login(
        @Body body: LoginRequest
    ): Response<BaseResponse<LoginResponse>>
    
    @POST("auth/register")
    suspend fun register(
        @Body body: RegisterRequest
    ): Response<BaseResponse<LoginResponse>>
    
    @GET("posts/feed")
    suspend fun getPostsFeed(
        @Query("limit") limit: Int = 10,
        @Query("cursor") cursor: String? = null
    ): Response<BaseResponse<PostsFeedData>>
    
    @GET("users/profile/{username}")
    suspend fun getUserProfile(
        @Path("username") username: String
    ): Response<BaseResponse<UserProfile>>
    
    @POST("relationships")
    suspend fun sendFriendRequest(
        @Body body: Map<String, String>
    ): Response<BaseResponse<Relationship>>
    
    @GET("users/email-availability")
    suspend fun checkEmailAvailability(
        @Query("email") email: String
    ): Response<BaseResponse<EmailAvailabilityData>>
    
    @GET("users/username-availability")
    suspend fun checkUsernameAvailability(
        @Query("username") username: String
    ): Response<BaseResponse<UsernameAvailabilityData>>
    
    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body body: RefreshTokenRequest
    ): Response<BaseResponse<TokenResponse>>
    
    @POST("media/upload/request")
    suspend fun requestUpload(
        @Body body: RequestUploadRequest
    ): Response<BaseResponse<UploadRequestData>>
    
    @POST("media/upload/confirm")
    suspend fun confirmUpload(
        @Body body: ConfirmUploadRequest
    ): Response<BaseResponse<ConfirmUploadData>>
    
    @POST("posts")
    suspend fun createPost(
        @Body body: CreatePostRequest
    ): Response<BaseResponse<Post>>
}
