package com.thinh.snaplet.ui.screens.home

import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.thinh.snaplet.data.model.Post
import com.thinh.snaplet.ui.components.PermissionHandler
import com.thinh.snaplet.ui.screens.home.components.BottomAction
import com.thinh.snaplet.ui.screens.home.components.CameraPage
import com.thinh.snaplet.ui.screens.home.components.MediaPage
import com.thinh.snaplet.ui.screens.home.components.TopAction
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.permission.Permission
import kotlinx.coroutines.launch

private const val CAMERA_PAGE_INDEX = 0

@Composable
fun Home() {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    val pageCount = 1 + uiState.posts.size
    val pagerState = rememberPagerState(
        initialPage = CAMERA_PAGE_INDEX, pageCount = { pageCount })

    PermissionHandler(
        permission = Permission.Camera, onPermissionResult = viewModel::onPermissionResult
    ) { requestPermission ->
        UiEventHandler(
            requestPermission = requestPermission,
            onScrollToFirstPost = { pagerState.animateScrollToPage(1) })

        HomeContent(
            pagerState = pagerState, uiState = uiState
        )
    }
}

@Composable
private fun UiEventHandler(
    requestPermission: () -> Unit, onScrollToFirstPost: suspend () -> Unit
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
    pagerState: PagerState, uiState: HomeUiState
) {
    val viewModel: HomeViewModel = hiltViewModel()

    val scope = rememberCoroutineScope()

    var shouldBindCamera by remember { mutableStateOf(true) }
    var captureSnapshotHandler by remember { mutableStateOf<(() -> Bitmap?)?>(null) }

    val cameraCallbacks = rememberCameraCallbacks(
        viewModel = viewModel, onSnapshotHandlerChanged = { handler ->
            captureSnapshotHandler = handler
        })

    val isGlobalBottomAction by remember {
        derivedStateOf {
            val absolutePosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
            absolutePosition > 1.0f
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        Logger.d("${pagerState.currentPage}")
    }

    CameraBindingController(
        pagerState = pagerState,
        shouldBindCamera = shouldBindCamera,
        captureSnapshotHandler = captureSnapshotHandler,
        onShouldBindCameraChanged = { shouldBind -> shouldBindCamera = shouldBind },
        onSnapshotCaptured = viewModel::setPreviewSnapshot
    )

    Box(modifier = Modifier.fillMaxSize()) {
        MediaPager(
            pagerState = pagerState,
            posts = uiState.posts,
            uploadStatuses = uiState.uploadStatuses,
            shouldBindCamera = shouldBindCamera,
            cameraCallbacks = cameraCallbacks,
            showLocalBottomAction = !isGlobalBottomAction,
            onCaptureClick = {
                scope.launch {
                    pagerState.animateScrollToPage(0)
                }
            }
        )

        TopAction(
            onProfileClick = { /* TODO */ },
            onFriendsClick = { /* TODO */ },
            onChatClick = { /* TODO */ },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(all = 16.dp)
        )

        if (isGlobalBottomAction) {
            BottomAction(
                onGridClick = { /* TODO */ },
                onCaptureClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                },
                onMoreClick = { /* TODO */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            )
        }
    }
}

@Composable
private fun rememberCameraCallbacks(
    viewModel: HomeViewModel, onSnapshotHandlerChanged: ((() -> Bitmap?) -> Unit)
): CameraCallbacks {
    return remember {
        CameraCallbacks(onImageCaptureReady = { imageCapture ->
            viewModel.setImageCapture(imageCapture)
        }, onSnapshotHandlerReady = { handler ->
            onSnapshotHandlerChanged(handler)
        })
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
    posts: List<Post>,
    uploadStatuses: Map<String, UploadStatus>,
    shouldBindCamera: Boolean,
    cameraCallbacks: CameraCallbacks,
    showLocalBottomAction: Boolean,
    onCaptureClick: () -> Unit
) {
    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        beyondViewportPageCount = 1,
        key = { page -> getPageKey(page, posts) }) { page ->
        when (page) {
            CAMERA_PAGE_INDEX -> {
                CameraPage(
                    onImageCaptureReady = cameraCallbacks.onImageCaptureReady,
                    onSnapshotHandlerReady = cameraCallbacks.onSnapshotHandlerReady,
                    shouldBindCamera = shouldBindCamera,
                )
            }

            else -> {
                val postIndex = page - 1
                if (postIndex < posts.size) {
                    val post = posts[postIndex]
                    MediaPage(
                        post = post,
                        uploadStatus = uploadStatuses[post.id],
                        showBottomAction = showLocalBottomAction,
                        onGridClick = { /* TODO */ },
                        onCaptureClick = onCaptureClick,
                        onMoreClick = { /* TODO */ }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun isOnCameraPage(pagerState: PagerState): Boolean {
    return pagerState.currentPage == CAMERA_PAGE_INDEX
}

private fun getPageKey(page: Int, posts: List<Post>): String {
    return if (page == CAMERA_PAGE_INDEX) {
        "camera_section"
    } else {
        posts.getOrNull(page - 1)?.id ?: "unknown_$page"
    }
}

private fun captureAndSaveSnapshot(
    captureSnapshotHandler: (() -> Bitmap?)?, onSnapshotCaptured: (Bitmap) -> Unit
) {
    captureSnapshotHandler?.invoke()?.let { bitmap ->
        onSnapshotCaptured(bitmap)
    } ?: Logger.e("❌ Failed to capture snapshot")
}

private data class CameraCallbacks(
    val onImageCaptureReady: (ImageCapture) -> Unit,
    val onSnapshotHandlerReady: (() -> Bitmap?) -> Unit
)