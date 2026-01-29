package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thinh.snaplet.ui.screens.home.HomeUiState
import com.thinh.snaplet.utils.Logger

@Composable
fun MediaPage(
    page: Int,
    uiState: HomeUiState
) {
    val mediaIndex = page - 1
    if (mediaIndex < uiState.posts.size) {
        val post = uiState.posts[mediaIndex]
        val uploadStatus = uiState.uploadStatuses[post.id]
        MediaItemPage(
            modifier = Modifier.fillMaxSize(),
            post = post,
            uploadStatus = uploadStatus,
            onMediaClick = { Logger.d("📷 Clicked post: ${it.id}") }
        )
    }
}
