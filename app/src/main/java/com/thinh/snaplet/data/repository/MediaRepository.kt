package com.thinh.snaplet.data.repository

import android.net.Uri
import com.thinh.snaplet.data.model.Post
import com.thinh.snaplet.data.model.PostsFeedData
import com.thinh.snaplet.utils.network.ApiResult

interface MediaRepository {
    
    suspend fun getNewsfeed(limit: Int = 10, cursor: String? = null): ApiResult<PostsFeedData>
    
    suspend fun uploadPhoto(uri: Uri): ApiResult<Post>
    
    suspend fun loadMoreMedia(cursor: String, limit: Int = 10): ApiResult<PostsFeedData>
}
