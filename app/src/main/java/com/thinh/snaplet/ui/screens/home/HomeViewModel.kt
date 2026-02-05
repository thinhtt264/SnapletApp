package com.thinh.snaplet.ui.screens.home

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.data.model.Post
import com.thinh.snaplet.data.model.UserProfile
import com.thinh.snaplet.data.model.media.ImageTransform
import com.thinh.snaplet.data.model.media.Media
import com.thinh.snaplet.data.repository.MediaRepository
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.utils.FileUtils
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.onFailure
import com.thinh.snaplet.utils.permission.Permission
import com.thinh.snaplet.utils.permission.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val permissionManager: PermissionManager,
    private val mediaRepository: MediaRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            cameraState = CameraState(
                hasCameraPermission = permissionManager.hasPermission(Permission.Camera)
            )
        )
    )

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>(
        replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.SUSPEND
    )
    val uiEvent = _uiEvent.asSharedFlow()

    private val _imageCapture = mutableStateOf<ImageCapture?>(null)

    init {
        loadNewsfeed()
    }

    private fun loadNewsfeed(isLoadMore: Boolean = false) {
        val state = _uiState.value

        if (isLoadMore) {
            if (state.isLoadingMore || state.nextCursor == null) return
        } else {
            if (state.isLoadingPosts) return
        }

        viewModelScope.launch {
            _uiState.update {
                if (isLoadMore) it.copy(isLoadingMore = true)
                else it.copy(isLoadingPosts = true)
            }

            val cursor = if (isLoadMore) state.nextCursor else null

            mediaRepository.getNewsfeed(cursor = cursor).fold(
                onSuccess = { feedData ->
                    _uiState.update {
                        it.copy(
                            posts = if (isLoadMore) it.posts + feedData.data else feedData.data,
                            isLoadingPosts = false,
                            isLoadingMore = false,
                            nextCursor = feedData.pagination.nextCursor,
                            error = null
                        )
                    }
                    if (isLoadMore) {
                        Logger.d("✅ Loaded ${feedData.data.size} more posts, nextCursor: ${feedData.pagination.nextCursor?.take(20) ?: "null"}")
                    }
                },
                onFailure = { apiError ->
                    Logger.e("❌ Failed to load newsfeed: ${apiError.message}")
                    _uiState.update {
                        it.copy(
                            isLoadingPosts = false,
                            isLoadingMore = false,
                            error = apiError.message
                        )
                    }
                    emitEvent(HomeUiEvent.ShowError(apiError.message))
                }
            )
        }
    }

    fun onItemVisible(currentIndex: Int) {
        val totalItems = _uiState.value.posts.size
        // Trigger load more when 2 items away from the end
        // For 5 items: trigger at index 2 (when viewing 3rd item out of 5)
        val loadMoreThreshold = totalItems - 3

        if (currentIndex >= loadMoreThreshold && _uiState.value.canLoadMore) {
            loadNewsfeed(isLoadMore = true)
        }
    }

    fun onSwitchCamera() {
        updateCameraState { state ->
            val newLens = if (state.lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            state.copy(lensFacing = newLens)
        }
    }

    private fun updateCameraState(transform: (CameraState) -> CameraState) {
        _uiState.update { state ->
            state.copy(cameraState = transform(state.cameraState))
        }
    }

    fun updateCurrentCaption(caption: String) {
        _uiState.update { it.copy(currentCaption = caption) }
    }

    fun onScreenInitialized() {
        val hasPermission = permissionManager.hasPermission(Permission.Camera)
        updateCameraState { it.copy(hasCameraPermission = hasPermission) }

        if (!hasPermission) {
            emitEvent(HomeUiEvent.RequestPermission(Permission.Camera))
        }
    }

    fun onPermissionResult(granted: Boolean) {
        updateCameraState { it.copy(hasCameraPermission = granted) }
        if (!granted) {
            emitEvent(HomeUiEvent.ShowError("Camera permission is required"))
        }
    }

    fun setImageCapture(capture: ImageCapture) {
        _imageCapture.value = capture
        updateCameraState { it.copy(isCameraActive = true) }
    }

    fun setPreviewSnapshot(bitmap: Bitmap) {
        updateCameraState { it.copy(lastPreviewSnapshot = bitmap) }
    }

    fun onCameraPageVisible() {
        updateCameraState { it.copy(shouldBindCamera = true) }
    }

    fun onCameraPageHidden() {
        updateCameraState { it.copy(shouldBindCamera = false) }
    }

    fun onCapturePhoto(context: Context) {
        if (!_uiState.value.cameraState.hasCameraPermission) {
            emitEvent(HomeUiEvent.RequestPermission(Permission.Camera))
            return
        }

        if (!_uiState.value.cameraState.isCameraActive) {
            Logger.e("❌ Cannot capture: Camera not ready")
            emitEvent(HomeUiEvent.ShowError("Camera is not ready"))
            return
        }

        takePhoto(context)
    }

    private fun takePhoto(context: Context) {
        val capture = _imageCapture.value ?: run {
            Logger.e("❌ ImageCapture is null")
            emitEvent(HomeUiEvent.ShowError("Camera is not ready"))
            return
        }

        val executor = ContextCompat.getMainExecutor(context)

        updateCameraState { it.copy(isCapturing = true) }

        val photoFile = File(
            context.cacheDir, SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS", Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    updateCameraState {
                        it.copy(isCapturing = false, capturedImagePath = photoFile.absolutePath)
                    }
                    emitEvent(HomeUiEvent.ShowSuccess("Photo saved successfully"))
                }

                override fun onError(exception: ImageCaptureException) {
                    updateCameraState { it.copy(isCapturing = false) }
                    Logger.e(exception, "❌ Photo capture failed")
                    emitEvent(HomeUiEvent.ShowError("Failed to capture photo"))
                }
            })
    }

    fun onCancelCapture() {
        val imagePath = _uiState.value.cameraState.capturedImagePath
        FileUtils.deleteFileFromPath(imagePath)
        updateCameraState { it.copy(capturedImagePath = null) }
        _uiState.update { it.copy(currentCaption = null) }
    }

    fun onUploadPost() {
        val imagePath = _uiState.value.cameraState.capturedImagePath ?: run {
            emitEvent(HomeUiEvent.ShowError("No image to upload"))
            return
        }

        if (_uiState.value.isUploading) {
            return
        }

        viewModelScope.launch {
            val userProfile = userRepository.getCurrentUserProfile()
            if (userProfile == null) {
                emitEvent(HomeUiEvent.ShowError("User profile not found"))
                return@launch
            }

            val tempPostId = "temp_${System.currentTimeMillis()}"
            val caption = _uiState.value.currentCaption

            val isFrontCamera =
                _uiState.value.cameraState.lensFacing == CameraSelector.LENS_FACING_FRONT
            val transform = ImageTransform(
                rotation = 0,
                scaleX = if (isFrontCamera) -1f else 1f,
                scaleY = 1f
            )

            val tempPost = createTempPost(
                id = tempPostId,
                imagePath = imagePath,
                userProfile = userProfile,
                transform = transform,
                caption = caption
            )

            _uiState.update { state ->
                state.copy(
                    posts = listOf(tempPost) + state.posts,
                    cameraState = state.cameraState.copy(capturedImagePath = null),
                    currentCaption = null,
                    uploadStatuses = state.uploadStatuses + (tempPostId to UploadStatus.Uploading),
                    tempPosts = state.tempPosts + tempPost
                )
            }

            _uiEvent.emit(HomeUiEvent.ScrollToFirstPost)

            performUpload(tempPostId, imagePath, transform, caption)
        }
    }

    private fun performCratePost(mediaIds: List<String>, caption: String?) {
        viewModelScope.launch {
            mediaRepository.createPost(mediaIds, caption, "friend-only")
            emitEvent(HomeUiEvent.ShowSuccess("Post uploaded successfully"))
        }
    }

    private fun performUpload(
        tempPostId: String,
        imagePath: String,
        transform: ImageTransform,
        caption: String?
    ) {
        viewModelScope.launch {
            try {
                val uploadRequestData = mediaRepository.requestUpload(
                    items = listOf(imagePath),
                    transforms = listOf(transform)
                ).fold(onSuccess = { data -> data }, onFailure = { error ->
                    Logger.e("❌ Step 1 failed: ${error.message}")
                    setUploadStatus(
                        tempPostId,
                        UploadStatus.Failed("Upload request failed: ${error.message}")
                    )
                    emitEvent(HomeUiEvent.ShowError("Upload request failed: ${error.message}"))
                    return@launch
                })

                if (uploadRequestData.data.isEmpty()) {
                    setUploadStatus(tempPostId, UploadStatus.Failed("No upload URLs received"))
                    emitEvent(HomeUiEvent.ShowError("No upload URLs received"))
                    return@launch
                }

                val uploadItem = uploadRequestData.data.first()

                mediaRepository.uploadMedia(uploadItem.uploadUrl, imagePath)
                    .onFailure({ error ->
                        Logger.e("❌ Step 2 failed: ${error.message}")
                        setUploadStatus(
                            tempPostId,
                            UploadStatus.Failed("Upload failed: ${error.message}")
                        )
                        emitEvent(HomeUiEvent.ShowError("Upload failed: ${error.message}"))
                        return@launch
                    })

                mediaRepository.confirmUpload(listOf(uploadItem.mediaId))
                    .fold(onSuccess = { confirmData ->
                        performCratePost(confirmData.media.map { it.id }, caption)
                        setUploadStatus(tempPostId, UploadStatus.Success)
                    }, onFailure = { error ->
                        setUploadStatus(
                            tempPostId,
                            UploadStatus.Failed("Upload confirmation failed: ${error.message}")
                        )
                        emitEvent(HomeUiEvent.ShowError("Upload failed: ${error.message}"))
                    })
            } catch (e: Exception) {
                setUploadStatus(
                    tempPostId,
                    UploadStatus.Failed("Upload failed: ${e.message ?: "Unknown error"}")
                )
                emitEvent(HomeUiEvent.ShowError("Upload failed: ${e.message ?: "Unknown error"}"))
            }
        }
    }

    fun retryUpload(tempPostId: String) {
        val tempPost = _uiState.value.tempPosts.find { it.id == tempPostId } ?: run {
            Logger.e("❌ Cannot retry: TempPost not found: $tempPostId")
            emitEvent(HomeUiEvent.ShowError("Cannot retry upload: Post data not found"))
            return
        }

        val media = tempPost.media.firstOrNull() ?: run {
            Logger.e("❌ Cannot retry: Media not found in post: $tempPostId")
            emitEvent(HomeUiEvent.ShowError("Cannot retry upload: Media not found"))
            return
        }

        val imagePath = media.originalUrl?.removePrefix("file://") ?: run {
            Logger.e("❌ Cannot retry: Image path not found: $tempPostId")
            emitEvent(HomeUiEvent.ShowError("Cannot retry upload: Image path not found"))
            return
        }

        val transform = media.transform ?: ImageTransform(rotation = 0, scaleX = 1f, scaleY = 1f)

        setUploadStatus(tempPostId, UploadStatus.Uploading)

        performUpload(tempPostId, imagePath, transform, tempPost.caption)
    }

    private fun setUploadStatus(tempPostId: String, status: UploadStatus) {
        _uiState.update { state ->
            state.copy(
                uploadStatuses = state.uploadStatuses + (tempPostId to status)
            )
        }
    }

    private fun createTempPost(
        id: String,
        imagePath: String,
        userProfile: UserProfile,
        transform: ImageTransform,
        caption: String? = null
    ): Post {
        val file = File(imagePath)
        val fileUri = "file://${file.absolutePath}"

        val tempMedia = Media(
            id = "temp_media_$id",
            type = "image",
            originalUrl = fileUri,
            transform = transform,
            ownerId = userProfile.id
        )

        return Post(
            id = id,
            userId = userProfile.id,
            username = userProfile.userName,
            firstName = userProfile.firstName,
            lastName = userProfile.lastName,
            avatarUrl = userProfile.avatarUrl,
            media = listOf(tempMedia),
            caption = caption,
            visibility = "friend-only", // Default visibility
            createdAt = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()),
            isOwnPost = true
        )
    }

    private fun removeTempPost(tempPostId: String) {
        _uiState.update { state ->
            state.copy(
                posts = state.posts.filterNot { it.id == tempPostId },
                uploadStatuses = state.uploadStatuses - tempPostId,
                tempPosts = state.tempPosts.filterNot { it.id == tempPostId }
            )
        }
        Logger.d("🗑️ Removed temporary post: $tempPostId")
    }

    private fun emitEvent(event: HomeUiEvent) {
        viewModelScope.launch { _uiEvent.emit(event) }
    }
}