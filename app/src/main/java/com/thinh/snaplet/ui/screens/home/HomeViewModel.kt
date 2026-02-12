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
import com.thinh.snaplet.R
import com.thinh.snaplet.data.model.Post
import com.thinh.snaplet.data.model.media.ImageTransform
import com.thinh.snaplet.domain.feed.GetNewsfeedUseCase
import com.thinh.snaplet.domain.feed.ShouldTriggerLoadMoreUseCase
import com.thinh.snaplet.domain.media.DownloadPostImageUseCase
import com.thinh.snaplet.domain.media.ValidateCaptureReadinessUseCase
import com.thinh.snaplet.domain.model.CaptureReadiness
import com.thinh.snaplet.domain.model.PostAction
import com.thinh.snaplet.domain.model.UploadPostResult
import com.thinh.snaplet.domain.post.CreateTempPostUseCase
import com.thinh.snaplet.domain.post.DeletePostUseCase
import com.thinh.snaplet.domain.post.GetAvailablePostActionsUseCase
import com.thinh.snaplet.domain.post.UploadPostUseCase
import com.thinh.snaplet.domain.post.ValidateRetryUploadUseCase
import com.thinh.snaplet.domain.post.ValidateUploadPostUseCase
import com.thinh.snaplet.platform.permission.Permission
import com.thinh.snaplet.platform.permission.PermissionManager
import com.thinh.snaplet.ui.common.UiText
import com.thinh.snaplet.ui.overlay.OverlayEventBus
import com.thinh.snaplet.ui.overlay.SheetOption
import com.thinh.snaplet.ui.theme.Red
import com.thinh.snaplet.utils.FileUtils
import com.thinh.snaplet.utils.network.onFailure
import com.thinh.snaplet.utils.network.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val permissionManager: PermissionManager,
    private val getNewsfeedUseCase: GetNewsfeedUseCase,
    private val shouldTriggerLoadMoreUseCase: ShouldTriggerLoadMoreUseCase,
    private val validateCaptureReadinessUseCase: ValidateCaptureReadinessUseCase,
    private val createTempPostUseCase: CreateTempPostUseCase,
    private val validateUploadPostUseCase: ValidateUploadPostUseCase,
    private val uploadPostUseCase: UploadPostUseCase,
    private val validateRetryUploadUseCase: ValidateRetryUploadUseCase,
    private val getAvailablePostActionsUseCase: GetAvailablePostActionsUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val downloadPostImageUseCase: DownloadPostImageUseCase
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

    private var currentPostVisible: Post? = null

    init {
        loadNewsfeed()
    }

    private fun loadNewsfeed(isLoadMore: Boolean = false) {
        val state = _uiState.value

        if (isLoadMore) {
            if (state.isLoadingMore || state.nextCursor == null) return
        } else if (state.isLoadingPosts) return

        viewModelScope.launch {
            _uiState.update {
                if (isLoadMore) it.copy(isLoadingMore = true)
                else it.copy(isLoadingPosts = true)
            }

            val cursor = if (isLoadMore) state.nextCursor else null

            getNewsfeedUseCase(cursor = cursor).fold(
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
                },
                onFailure = { apiError ->
                    _uiState.update {
                        it.copy(
                            isLoadingPosts = false,
                            isLoadingMore = false,
                            error = apiError.message
                        )
                    }
                    emitEvent(HomeUiEvent.ShowError(UiText.DynamicString(apiError.message)))
                }
            )
        }
    }

    fun onItemVisible(currentIndex: Int) {
        currentPostVisible = _uiState.value.posts.getOrNull(currentIndex)

        val state = _uiState.value
        val shouldLoad = shouldTriggerLoadMoreUseCase(
            currentIndex = currentIndex,
            totalItems = state.posts.size,
            canLoadMore = state.canLoadMore
        )
        if (shouldLoad) loadNewsfeed(isLoadMore = true)
    }

    fun onSwitchCamera() {
        updateCameraState { state ->
            val newLens = if (state.lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            state.copy(lensFacing = newLens, lastPreviewSnapshot = null)
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

    fun onRequestCameraPermission() {
        val hasPermission = permissionManager.hasPermission(Permission.Camera)
        updateCameraState { it.copy(hasCameraPermission = hasPermission) }

        if (!hasPermission) {
            emitEvent(HomeUiEvent.RequestPermission(Permission.Camera))
        }
    }

    fun onPermissionResult(granted: Boolean) {
        updateCameraState { it.copy(hasCameraPermission = granted) }
        if (!granted) {
            emitEvent(HomeUiEvent.ShowError(UiText.DynamicString("Camera permission is required")))
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
        val cameraState = _uiState.value.cameraState
        when (validateCaptureReadinessUseCase(
            cameraState.hasCameraPermission,
            cameraState.isCameraActive
        )) {
            CaptureReadiness.NeedPermission -> emitEvent(HomeUiEvent.RequestPermission(Permission.Camera))
            CaptureReadiness.CameraNotReady -> emitEvent(
                HomeUiEvent.ShowError(
                    UiText.DynamicString(
                        "Camera is not ready"
                    )
                )
            )

            CaptureReadiness.Ready -> takePhoto(context)
        }
    }

    private fun takePhoto(context: Context) {
        val capture = _imageCapture.value ?: run {
            emitEvent(HomeUiEvent.ShowError(UiText.DynamicString("Camera is not ready")))
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        updateCameraState { it.copy(isCapturing = true) }

        val photoFile = File(
            context.cacheDir,
            SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS",
                Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        val isFrontCamera =
            _uiState.value.cameraState.lensFacing == CameraSelector.LENS_FACING_FRONT

        capture.takePicture(
            outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    viewModelScope.launch {
                        val imagePath = withContext(Dispatchers.IO) {
                            FileUtils.processImageToWebp(photoFile, flipHorizontal = isFrontCamera)
                                ?: photoFile.absolutePath
                        }
                        withContext(Dispatchers.Main.immediate) {
                            updateCameraState {
                                it.copy(isCapturing = false, capturedImagePath = imagePath)
                            }
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    updateCameraState { it.copy(isCapturing = false) }
                    emitEvent(HomeUiEvent.ShowError(UiText.DynamicString("Failed to capture photo")))
                }
            }
        )
    }

    fun onCancelCapture() {
        val imagePath = _uiState.value.cameraState.capturedImagePath
        FileUtils.deleteFileFromPath(imagePath)
        updateCameraState { it.copy(capturedImagePath = null) }
        _uiState.update { it.copy(currentCaption = null) }
    }

    fun onUploadPost() {
        val state = _uiState.value
        viewModelScope.launch {
            val isUploading = state.uploadStatuses.values.any { it is UploadStatus.Uploading }
            when (val result = validateUploadPostUseCase(
                capturedImagePath = state.cameraState.capturedImagePath,
                caption = state.currentCaption,
                isUploading = isUploading
            )) {
                is ValidateUploadPostUseCase.ValidateUploadResult.Success -> {
                    val input = result.input
                    val tempPostId = "temp_${System.currentTimeMillis()}"
                    val transform = ImageTransform(rotation = 0, scaleX = 1f, scaleY = 1f)
                    val tempPost = createTempPostUseCase(
                        id = tempPostId,
                        imagePath = input.imagePath,
                        userProfile = input.userProfile,
                        transform = transform,
                        caption = input.caption
                    )

                    _uiState.update { s ->
                        s.copy(
                            posts = listOf(tempPost) + s.posts,
                            cameraState = s.cameraState.copy(capturedImagePath = null),
                            currentCaption = null,
                            uploadStatuses = s.uploadStatuses + (tempPostId to UploadStatus.Uploading),
                            tempPosts = s.tempPosts + tempPost
                        )
                    }

                    emitEvent(HomeUiEvent.ScrollToFirstPost)
                    runUploadAndUpdateStatus(tempPostId, input.imagePath, transform, input.caption)
                }

                is ValidateUploadPostUseCase.ValidateUploadResult.AlreadyUploading -> { /* no-op, already uploading */
                }

                is ValidateUploadPostUseCase.ValidateUploadResult.NoImage -> {
                    emitEvent(HomeUiEvent.ShowError(UiText.DynamicString("No image to upload")))
                }

                is ValidateUploadPostUseCase.ValidateUploadResult.UserProfileNotFound -> {
                    emitEvent(HomeUiEvent.ShowError(UiText.DynamicString("User profile not found")))
                }
            }
        }
    }

    private fun runUploadAndUpdateStatus(
        tempPostId: String,
        imagePath: String,
        transform: ImageTransform,
        caption: String?
    ) {
        viewModelScope.launch {
            when (val result = uploadPostUseCase(imagePath, transform, caption)) {
                is UploadPostResult.Success -> {
                    setUploadStatus(tempPostId, UploadStatus.Success)
                    emitEvent(HomeUiEvent.ShowSuccess(UiText.DynamicString("Post uploaded successfully")))
                }

                is UploadPostResult.Failed -> {
                    setUploadStatus(tempPostId, UploadStatus.Failed(result.message))
                    emitEvent(HomeUiEvent.ShowError(UiText.DynamicString(result.message)))
                }
            }
        }
    }

    fun onShowMoreOptions() {
        currentPostVisible?.let { onShowMoreOptions(it) }
    }

    fun onShowMoreOptions(post: Post) {
        val isUploading = _uiState.value.uploadStatuses[post.id] == UploadStatus.Uploading
        val actions = getAvailablePostActionsUseCase(post, isUploading)

        val options = actions.map { action ->
            when (action) {
                is PostAction.Share -> SheetOption(
                    id = "share",
                    label = UiText.StringResource(R.string.share),
                    onClick = { /* TODO: share */ }
                )

                is PostAction.Download -> SheetOption(
                    id = "download",
                    label = UiText.StringResource(R.string.download),
                    onClick = { downloadPostImage(post) }
                )

                is PostAction.Delete -> SheetOption(
                    id = "delete",
                    label = UiText.StringResource(R.string.delete),
                    color = Red,
                    onClick = {
                        OverlayEventBus.showConfirmDialog(
                            title = UiText.StringResource(R.string.delete_photo_title),
                            message = UiText.StringResource(R.string.delete_photo_message),
                            confirmText = UiText.StringResource(R.string.delete),
                            onConfirm = { deletePost(post.id) },
                        )
                    }
                )

                is PostAction.Report -> SheetOption(
                    id = "report",
                    label = UiText.StringResource(R.string.report),
                    color = Red,
                    onClick = { /* TODO: report */ }
                )

                is PostAction.Cancel -> SheetOption(
                    id = "cancel",
                    label = UiText.StringResource(R.string.cancel),
                    onClick = { /* dismiss only */ }
                )
            }
        }
        OverlayEventBus.showOptionsSheet(options = options)
    }

    fun retryUpload(tempPostId: String) {
        val tempPost = _uiState.value.tempPosts.find { it.id == tempPostId }
        when (val result = validateRetryUploadUseCase(tempPost)) {
            is ValidateRetryUploadUseCase.ValidateRetryResult.Success -> {
                val input = result.input
                setUploadStatus(input.tempPostId, UploadStatus.Uploading)
                runUploadAndUpdateStatus(
                    input.tempPostId,
                    input.imagePath,
                    input.transform,
                    input.caption
                )
            }

            is ValidateRetryUploadUseCase.ValidateRetryResult.PostNotFound -> {
                emitEvent(HomeUiEvent.ShowError(UiText.DynamicString("Cannot retry upload: Post data not found")))
            }

            is ValidateRetryUploadUseCase.ValidateRetryResult.MediaNotFound -> {
                emitEvent(HomeUiEvent.ShowError(UiText.DynamicString("Cannot retry upload: Media not found")))
            }

            is ValidateRetryUploadUseCase.ValidateRetryResult.ImagePathNotFound -> {
                emitEvent(HomeUiEvent.ShowError(UiText.DynamicString("Cannot retry upload: Image path not found")))
            }
        }
    }

    private fun setUploadStatus(tempPostId: String, status: UploadStatus) {
        _uiState.update { state ->
            state.copy(uploadStatuses = state.uploadStatuses + (tempPostId to status))
        }
    }

    private fun removeTempPost(tempPostId: String) {
        _uiState.update { state ->
            state.copy(
                posts = state.posts.filterNot { it.id == tempPostId },
                uploadStatuses = state.uploadStatuses - tempPostId,
                tempPosts = state.tempPosts.filterNot { it.id == tempPostId }
            )
        }
    }

    private fun deletePost(postId: String) {
        viewModelScope.launch {
            deletePostUseCase(postId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            posts = state.posts.filterNot { it.id == postId },
                            uploadStatuses = state.uploadStatuses - postId,
                            tempPosts = state.tempPosts.filterNot { it.id == postId }
                        )
                    }
                    emitEvent(HomeUiEvent.ShowSuccess(UiText.StringResource(R.string.post_deleted)))
                }
                .onFailure { error ->
                    emitEvent(HomeUiEvent.ShowError(UiText.DynamicString(error.message)))
                }
        }
    }

    fun downloadPostImage(post: Post) {
        if (_uiState.value.isDownloading) return
        val media = post.media.firstOrNull() ?: run {
            emitEvent(HomeUiEvent.ShowError(UiText.StringResource(R.string.download_failed)))
            return
        }
        val imageSource = media.originalUrl

        _uiState.update { it.copy(isDownloading = true) }
        viewModelScope.launch {
            downloadPostImageUseCase(imageSource)
                .onSuccess { _uiState.update { it.copy(isDownloading = false) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isDownloading = false) }
                    emitEvent(
                        HomeUiEvent.ShowError(
                            UiText.DynamicString(
                                e.message ?: "Download failed"
                            )
                        )
                    )
                }
        }
    }

    private fun emitEvent(event: HomeUiEvent) {
        viewModelScope.launch { _uiEvent.emit(event) }
    }
}
