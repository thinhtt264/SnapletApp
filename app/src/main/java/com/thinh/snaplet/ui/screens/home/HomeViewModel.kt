package com.thinh.snaplet.ui.screens.home

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.data.repository.MediaRepository
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.permission.Permission
import com.thinh.snaplet.utils.permission.PermissionManager
import com.thinh.snaplet.utils.safeMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val permissionManager: PermissionManager,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            cameraState = CameraState(
                hasCameraPermission = permissionManager.hasPermission(Permission.Camera)
            )
        )
    )

    val uiState: StateFlow<HomeUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>(
        replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.SUSPEND
    )
    val uiEvent = _uiEvent.asSharedFlow()

    private val _imageCapture = mutableStateOf<ImageCapture?>(null)
    val imageCapture: ImageCapture? get() = _imageCapture.value

    init {
        loadNewsfeed()
    }

    private fun loadNewsfeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPosts = true) }

            mediaRepository.getNewsfeed().onSuccess { feedData ->
                Logger.d("📷 Loaded ${feedData.data.size} posts")
                _uiState.update {
                    it.copy(
                        posts = feedData.data, isLoadingPosts = false, error = null
                    )
                }
            }.onFailure { error ->
                Logger.e(error, "❌ Failed to load newsfeed")
                _uiState.update {
                    it.copy(
                        isLoadingPosts = false, error = error.message
                    )
                }
                emitEvent(HomeUiEvent.ShowError(error.safeMessage))
            }
        }
    }

    fun refreshNewsfeed() {
        loadNewsfeed()
    }

    private fun updateCameraState(transform: (CameraState) -> CameraState) {
        _uiState.update { state ->
            state.copy(cameraState = transform(state.cameraState))
        }
    }

    fun onScreenInitialized() {
        val hasPermission = permissionManager.hasPermission(Permission.Camera)
        updateCameraState { it.copy(hasCameraPermission = hasPermission) }

        if (!hasPermission) {
            Logger.d("🔐 Camera permission needed, requesting...")
            emitEvent(HomeUiEvent.RequestPermission(Permission.Camera))
        } else {
            Logger.d("✅ Camera permission already granted")
        }
    }

    fun onPermissionResult(granted: Boolean) {
        updateCameraState { it.copy(hasCameraPermission = granted) }
        Logger.d("📋 Permission result: $granted")

        if (!granted) {
            emitEvent(HomeUiEvent.ShowError("Camera permission is required"))
        }
    }

    fun setImageCapture(capture: ImageCapture) {
        _imageCapture.value = capture
        updateCameraState { it.copy(isCameraActive = true) }
        Logger.d("📷 Camera is ready")
    }

    fun setPreviewSnapshot(bitmap: Bitmap) {
        updateCameraState { it.copy(lastPreviewSnapshot = bitmap) }
        Logger.d("📸 Preview snapshot saved (${bitmap.width}x${bitmap.height})")
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
            context.cacheDir,
            SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS",
                Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        Logger.d("📸 Taking photo...")
        capture.takePicture(
            outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    updateCameraState { it.copy(isCapturing = false) }
                    Logger.d("✅ Photo saved: ${photoFile.absolutePath}")
                    Logger.d("📁 Photo URI: ${output.savedUri}")
                    emitEvent(HomeUiEvent.ShowSuccess("Photo saved successfully"))
                }

                override fun onError(exception: ImageCaptureException) {
                    updateCameraState { it.copy(isCapturing = false) }
                    Logger.e(exception, "❌ Photo capture failed")
                    emitEvent(HomeUiEvent.ShowError("Failed to capture photo"))
                }
            })
    }

    private fun emitEvent(event: HomeUiEvent) {
        viewModelScope.launch { _uiEvent.emit(event) }
    }
}