package com.thinh.snaplet.ui.screens.home

import android.graphics.Bitmap
import com.thinh.snaplet.data.model.Post

data class HomeUiState(
    val cameraState: CameraState,

    val posts: List<Post> = emptyList(),
    val isLoadingPosts: Boolean = false,

    val error: String? = null
)

data class CameraState(
    val isCameraActive: Boolean = false,
    val isCapturing: Boolean = false,
    val showCameraPreview: Boolean = false,
    val lastPreviewSnapshot: Bitmap? = null,
    val hasCameraPermission: Boolean = false,
)

