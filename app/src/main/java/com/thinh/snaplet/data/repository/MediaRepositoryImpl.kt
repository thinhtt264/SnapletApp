package com.thinh.snaplet.data.repository

import android.webkit.MimeTypeMap
import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.data.model.CreatePostRequest
import com.thinh.snaplet.data.model.Post
import com.thinh.snaplet.data.model.PostsFeedData
import com.thinh.snaplet.data.model.media.ConfirmUploadData
import com.thinh.snaplet.data.model.media.ConfirmUploadRequest
import com.thinh.snaplet.data.model.media.ImageTransform
import com.thinh.snaplet.data.model.media.RequestUploadRequest
import com.thinh.snaplet.data.model.media.UploadRequestData
import com.thinh.snaplet.data.model.media.UploadRequestItem
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.ApiError
import com.thinh.snaplet.utils.network.ApiResult
import com.thinh.snaplet.utils.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : MediaRepository {

    private val uploadClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    override suspend fun getNewsfeed(limit: Int, cursor: String?): ApiResult<PostsFeedData> {
        return safeApiCall(
            apiCall = {
                apiService.getPostsFeed(limit = limit, cursor = cursor)
            },
            onSuccess = { feedData ->
                Logger.d(
                    "✅ Fetched ${feedData.data.size} posts${
                        cursor?.let {
                            ", cursor: ${
                                it.take(
                                    20
                                )
                            }..."
                        } ?: ""
                    }")
            }
        )
    }

    override suspend fun requestUpload(
        items: List<String>,
        transforms: List<ImageTransform>?
    ): ApiResult<UploadRequestData> {
        if (items.size > 3) {
            return ApiResult.Failure(
                ApiError(
                    httpCode = 400,
                    message = "Maximum 3 items allowed"
                )
            )
        }

        if (transforms != null && transforms.size != items.size) {
            return ApiResult.Failure(
                ApiError(
                    httpCode = 400,
                    message = "Transforms count must match items count"
                )
            )
        }

        val uploadRequestItems = mutableListOf<UploadRequestItem>()
        items.forEachIndexed { index, filePath ->
            val file = File(filePath)
            if (!file.exists()) {
                return ApiResult.Failure(
                    ApiError(
                        httpCode = 400,
                        message = "File does not exist: $filePath"
                    )
                )
            }

            val extension = file.extension.lowercase()
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                ?: return ApiResult.Failure(
                    ApiError(
                        httpCode = 400,
                        message = "Cannot determine mime type for file: $filePath"
                    )
                )

            val fileSize = file.length()
            if (fileSize == 0L) {
                return ApiResult.Failure(
                    ApiError(
                        httpCode = 400,
                        message = "File is empty: $filePath"
                    )
                )
            }

            val transform = transforms?.get(index)
            uploadRequestItems.add(
                UploadRequestItem(
                    mimeType = mimeType,
                    size = fileSize,
                    transform = transform
                )
            )
        }

        Logger.d("📤 Requesting upload URLs for ${items.size} item(s)")
        return safeApiCall(
            apiCall = {
                apiService.requestUpload(RequestUploadRequest(items = uploadRequestItems))
            }
        )
    }

    override suspend fun uploadMedia(uploadUrl: String, filePath: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return@withContext ApiResult.Failure(
                        ApiError(
                            httpCode = 400,
                            message = "File does not exist: $filePath"
                        )
                    )
                }

                val extension = file.extension.lowercase()
                val contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                    ?: return@withContext ApiResult.Failure(
                        ApiError(
                            httpCode = 400,
                            message = "Cannot determine content type for file: $filePath"
                        )
                    )

                val requestBody = file.asRequestBody(contentType.toMediaType())

                val request = Request.Builder()
                    .url(uploadUrl)
                    .put(requestBody)
                    .addHeader("Content-Type", contentType)
                    .build()

                val response = uploadClient.newCall(request).execute()

                if (response.isSuccessful) {
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Failure(
                        ApiError(
                            httpCode = response.code,
                            message = "Upload failed: ${response.message}"
                        )
                    )
                }
            } catch (e: Exception) {
                ApiResult.Failure(
                    ApiError(
                        httpCode = 500,
                        message = "Upload failed: ${e.message ?: "Unknown error"}"
                    )
                )
            }
        }
    }

    override suspend fun confirmUpload(mediaIds: List<String>): ApiResult<ConfirmUploadData> {
        if (mediaIds.size > 3) {
            return ApiResult.Failure(
                ApiError(
                    httpCode = 400,
                    message = "Maximum 3 media IDs allowed"
                )
            )
        }

        return safeApiCall(
            apiCall = {
                apiService.confirmUpload(ConfirmUploadRequest(mediaIds = mediaIds))
            }
        )
    }

    override suspend fun createPost(
        mediaIds: List<String>,
        caption: String?,
        visibility: String
    ): ApiResult<Post> {
        if (mediaIds.isEmpty()) {
            return ApiResult.Failure(
                ApiError(
                    httpCode = 400,
                    message = "At least one media ID is required"
                )
            )
        }

        if (mediaIds.size > 3) {
            return ApiResult.Failure(
                ApiError(
                    httpCode = 400,
                    message = "Maximum 3 media IDs allowed"
                )
            )
        }

        return safeApiCall(
            apiCall = {
                apiService.createPost(
                    CreatePostRequest(
                        mediaIds = mediaIds,
                        caption = caption,
                        visibility = visibility
                    )
                )
            }
        )
    }

    override suspend fun deletePost(postId: String): ApiResult<Unit> {
        return safeApiCall(
            apiCall = { apiService.deletePost(postId) },
        )
    }
}