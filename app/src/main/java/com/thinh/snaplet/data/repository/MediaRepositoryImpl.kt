package com.thinh.snaplet.data.repository

import android.net.Uri
import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.data.model.Post
import com.thinh.snaplet.data.model.PostsFeedData
import com.thinh.snaplet.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : MediaRepository {

    override suspend fun getNewsfeed(limit: Int, cursor: String?): Result<PostsFeedData> = 
        withContext(Dispatchers.IO) {
            try {
                Logger.d("📡 Fetching posts feed: limit=$limit, cursor=${cursor?.take(20)}...")
                
                val response = apiService.getPostsFeed(limit = limit, cursor = cursor)

                if (!response.isSuccessful) {
                    val errorMsg = "API returned ${response.code()}: ${response.message()}"
                    Logger.e("❌ $errorMsg")
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val body = response.body()
                if (body == null) {
                    Logger.e("❌ Response body is null")
                    return@withContext Result.failure(Exception("Empty response from server"))
                }

                if (body.status.code != 200) {
                    Logger.e("❌ API error: ${body.status.message}")
                    return@withContext Result.failure(Exception(body.status.message))
                }

                val posts = body.data.data.filter { post ->
                    if (post.media.isEmpty()) {
                        Logger.w("⚠️ Post ${post.id} has no media items, skipping")
                        false
                    } else {
                        true
                    }
                }

                val feedData = PostsFeedData(
                    data = posts,
                    pagination = body.data.pagination
                )

                Logger.d("✅ Fetched ${posts.size} posts, hasNext=${body.data.pagination.hasNext}")
                Result.success(feedData)
            } catch (e: Exception) {
                Logger.e(e, "❌ Failed to fetch posts feed")
                Result.failure(e)
            }
        }

    override suspend fun uploadPhoto(uri: Uri): Result<Post> = withContext(Dispatchers.IO) {
        try {
            Logger.d("📤 Uploading photo: $uri")

            Result.failure(Exception("Upload not implemented yet"))

        } catch (e: Exception) {
            Logger.e(e, "❌ Upload failed")
            Result.failure(e)
        }
    }

    override suspend fun loadMoreMedia(cursor: String, limit: Int): Result<PostsFeedData> =
        withContext(Dispatchers.IO) {
            try {
                Logger.d("📡 Loading more media with cursor: ${cursor.take(20)}...")
                getNewsfeed(limit = limit, cursor = cursor)
            } catch (e: Exception) {
                Logger.e(e, "❌ Failed to load more media")
                Result.failure(e)
            }
        }
}
