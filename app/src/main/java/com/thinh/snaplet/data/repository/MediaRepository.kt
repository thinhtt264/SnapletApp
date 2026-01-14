package com.thinh.snaplet.data.repository

import android.net.Uri
import com.thinh.snaplet.data.model.Post
import com.thinh.snaplet.data.model.PostsFeedData

interface MediaRepository {
    
    suspend fun getNewsfeed(limit: Int = 10, cursor: String? = null): Result<PostsFeedData>
    
    suspend fun uploadPhoto(uri: Uri): Result<Post>
    
    suspend fun loadMoreMedia(cursor: String, limit: Int = 10): Result<PostsFeedData>
}
