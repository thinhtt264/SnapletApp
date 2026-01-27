package com.thinh.snaplet.ui.screens.home

import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.thinh.snaplet.ui.components.PermissionHandler
import com.thinh.snaplet.ui.screens.home.components.CameraPage
import com.thinh.snaplet.ui.screens.home.components.MediaPage
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.permission.Permission

private const val CAMERA_PAGE_INDEX = 0

@Composable
fun Home() {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    val pageCount = 1 + uiState.posts.size
    val pagerState = rememberPagerState(
        initialPage = CAMERA_PAGE_INDEX,
        pageCount = { pageCount }
    )

    PermissionHandler(
        permission = Permission.Camera,
        onPermissionResult = viewModel::onPermissionResult
    ) { requestPermission ->
        UiEventHandler(
            requestPermission = requestPermission,
            onScrollToFirstPost = { pagerState.animateScrollToPage(1) }
        )

        HomeContent(
            pagerState = pagerState,
            uiState = uiState
        )
    }
}

@Composable
private fun UiEventHandler(
    requestPermission: () -> Unit,
    onScrollToFirstPost: suspend () -> Unit
) {
    val viewModel: HomeViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        viewModel.onScreenInitialized()

        viewModel.uiEvent.collect { event ->
            when (event) {
                is HomeUiEvent.RequestPermission -> {
                    Logger.d("🔐 Executing permission request from ViewModel")
                    requestPermission()
                }

                is HomeUiEvent.ShowError -> {
                    // TODO: Show error toast/snackbar
                    Logger.e("⚠️ Error: ${event.message}")
                }

                is HomeUiEvent.ShowSuccess -> {
                    // TODO: Show success toast/snackbar
                    Logger.d("✅ Success: ${event.message}")
                }

                is HomeUiEvent.ScrollToFirstPost -> onScrollToFirstPost()
            }
        }
    }
}

@Composable
private fun HomeContent(
    pagerState: PagerState,
    uiState: HomeUiState
) {
    val viewModel: HomeViewModel = hiltViewModel()

    var shouldBindCamera by remember { mutableStateOf(true) }
    var captureSnapshotHandler by remember { mutableStateOf<(() -> Bitmap?)?>(null) }

    val cameraCallbacks = rememberCameraCallbacks(
        viewModel = viewModel,
        onSnapshotHandlerChanged = { handler ->
            captureSnapshotHandler = handler
        }
    )

    CameraBindingController(
        pagerState = pagerState,
        shouldBindCamera = shouldBindCamera,
        captureSnapshotHandler = captureSnapshotHandler,
        onShouldBindCameraChanged = { shouldBind -> shouldBindCamera = shouldBind },
        onSnapshotCaptured = viewModel::setPreviewSnapshot
    )

    MediaPager(
        pagerState = pagerState,
        uiState = uiState,
        viewModel = viewModel,
        shouldBindCamera = shouldBindCamera,
        cameraCallbacks = cameraCallbacks
    )
}

@Composable
private fun rememberCameraCallbacks(
    viewModel: HomeViewModel,
    onSnapshotHandlerChanged: ((() -> Bitmap?) -> Unit)
): CameraCallbacks {
    return remember {
        CameraCallbacks(
            onImageCaptureReady = { imageCapture ->
                viewModel.setImageCapture(imageCapture)
            },
            onSnapshotHandlerReady = { handler ->
                onSnapshotHandlerChanged(handler)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CameraBindingController(
    pagerState: PagerState,
    shouldBindCamera: Boolean,
    captureSnapshotHandler: (() -> Bitmap?)?,
    onShouldBindCameraChanged: (Boolean) -> Unit,
    onSnapshotCaptured: (Bitmap) -> Unit
) {
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        when {
            isOnCameraPage(pagerState) && !pagerState.isScrollInProgress -> {
                onShouldBindCameraChanged(true)
            }

            !isOnCameraPage(pagerState) && shouldBindCamera -> {
                captureAndSaveSnapshot(
                    captureSnapshotHandler = captureSnapshotHandler,
                    onSnapshotCaptured = onSnapshotCaptured
                )
                onShouldBindCameraChanged(false)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaPager(
    pagerState: PagerState,
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    shouldBindCamera: Boolean,
    cameraCallbacks: CameraCallbacks
) {
    val context = LocalContext.current

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        beyondViewportPageCount = 1,
        key = { page -> getPageKey(page, uiState) }
    ) { page ->
        when (page) {
            CAMERA_PAGE_INDEX -> {
                CameraPage(
                    cameraState = uiState.cameraState,
                    onImageCaptureReady = cameraCallbacks.onImageCaptureReady,
                    onSnapshotHandlerReady = cameraCallbacks.onSnapshotHandlerReady,
                    shouldBindCamera = shouldBindCamera,
                    onRequestPermission = viewModel::onScreenInitialized,
                    onCapturePhoto = { viewModel.onCapturePhoto(context) },
                    onSwitchCamera = viewModel::onSwitchCamera,
                    onCancelCapture = viewModel::onCancelCapture,
                    onUploadPost = viewModel::onUploadPost,
                    isUploading = uiState.isUploading,
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                MediaPage(
                    page = page,
                    uiState = uiState
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun isOnCameraPage(pagerState: PagerState): Boolean {
    return pagerState.currentPage == CAMERA_PAGE_INDEX
}

private fun getPageKey(page: Int, uiState: HomeUiState): String {
    return if (page == CAMERA_PAGE_INDEX) {
        "camera_section"
    } else {
        uiState.posts[page - 1].id
    }
}

private fun captureAndSaveSnapshot(
    captureSnapshotHandler: (() -> Bitmap?)?,
    onSnapshotCaptured: (Bitmap) -> Unit
) {
    captureSnapshotHandler?.invoke()?.let { bitmap ->
        onSnapshotCaptured(bitmap)
        Logger.d("✅ Snapshot saved to ViewModel")
    } ?: Logger.e("❌ Failed to capture snapshot")
}

// Data classes
private data class CameraCallbacks(
    val onImageCaptureReady: (ImageCapture) -> Unit,
    val onSnapshotHandlerReady: (() -> Bitmap?) -> Unit
)