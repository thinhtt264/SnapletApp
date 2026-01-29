package com.thinh.snaplet.ui.screens.home

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import com.thinh.snaplet.data.model.Post

typealias TempPost = Post

data class HomeUiState(
    val cameraState: CameraState,
    val currentCaption: String? = null, // Caption for current capture

    val posts: List<Post> = emptyList(),
    val isLoadingPosts: Boolean = false,
    val isUploading: Boolean = false,

    val error: String? = null,

    val uploadStatuses: Map<String, UploadStatus> = emptyMap(),
    val tempPosts: List<TempPost> = emptyList() // List of temp posts with all info for retry
)

sealed class UploadStatus {
    object Uploading : UploadStatus()
    data class Failed(val errorMessage: String) : UploadStatus()
    object Success : UploadStatus()
}

data class CameraState(
    val isCameraActive: Boolean = false,
    val isCapturing: Boolean = false,
    val showCameraPreview: Boolean = false,
    val lastPreviewSnapshot: Bitmap? = null,
    val hasCameraPermission: Boolean = false,
    val lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    val capturedImagePath: String? = null
)

