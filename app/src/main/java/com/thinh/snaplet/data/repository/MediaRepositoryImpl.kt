package com.thinh.snaplet.data.repository

import android.net.Uri
import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.data.model.Post
import com.thinh.snaplet.data.model.PostsFeedData
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.ApiError
import com.thinh.snaplet.utils.network.ApiResult
import com.thinh.snaplet.utils.network.safeApiCall
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : MediaRepository {

    override suspend fun getNewsfeed(limit: Int, cursor: String?): ApiResult<PostsFeedData> {
        return safeApiCall(
            apiCall = {
                apiService.getPostsFeed(limit = limit, cursor = cursor)
            },
            onSuccess = { feedData ->
                Logger.d("✅ Fetched ${feedData.data.size} posts, hasNext=${feedData.pagination.hasNext}")
            }
        )
    }

    override suspend fun uploadPhoto(uri: Uri): ApiResult<Post> {
        Logger.d("📤 Uploading photo: $uri")
        //TODO
        return ApiResult.Failure(
            ApiError(
                httpCode = 501,
                message = "Upload not implemented yet"
            )
        )
    }

    override suspend fun loadMoreMedia(cursor: String, limit: Int): ApiResult<PostsFeedData> {
        Logger.d("📡 Loading more media with cursor: ${cursor.take(20)}...")
        return getNewsfeed(limit = limit, cursor = cursor)
    }
}
