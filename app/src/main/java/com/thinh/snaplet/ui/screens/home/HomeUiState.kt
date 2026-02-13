package com.thinh.snaplet.ui.screens.home

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import com.thinh.snaplet.data.model.Post
import com.thinh.snaplet.data.model.RelationshipWithUser
import com.thinh.snaplet.platform.share.ShareApp

data class HomeUiState(
    val cameraState: CameraState,
    val currentCaption: String? = null,

    val posts: List<Post> = emptyList(),
    val isLoadingPosts: Boolean = false,
    val isLoadingMore: Boolean = false,

    val nextCursor: String? = null, // Cursor for pagination, null means no more data

    val error: String? = null,

    val friendSheetState: FriendBottomSheetState = FriendBottomSheetState(),

    val uploadStatuses: Map<String, UploadStatus> = emptyMap(),
    val isDownloading: Boolean = false
) {
    /** Returns true if more data can be loaded (nextCursor is not null and not currently loading) */
    val canLoadMore: Boolean get() = nextCursor != null && !isLoadingMore && !isLoadingPosts
}

sealed class UploadStatus {
    object Uploading : UploadStatus()
    data class Failed(val errorMessage: String) : UploadStatus()
    object Success : UploadStatus()
}

data class CameraState(
    val isCameraActive: Boolean = false,
    val isCapturing: Boolean = false,
    val shouldBindCamera: Boolean = true,
    val lastPreviewSnapshot: Bitmap? = null,
    val hasCameraPermission: Boolean = false,
    val lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    val capturedImagePath: String? = null
) {
    val isEditMode: Boolean get() = capturedImagePath != null
}

data class FriendBottomSheetState(
    val friendsCount: Int? = null,
    val friendList: List<RelationshipWithUser> = emptyList(),
    val pendingList: List<RelationshipWithUser> = emptyList(),
    val isLoadingFriendList: Boolean = false,
    val shareApps: List<ShareApp> = emptyList(),
)