package com.thinh.snaplet.ui.screens.home

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.thinh.snaplet.data.model.Post
import com.thinh.snaplet.ui.components.PermissionHandler
import com.thinh.snaplet.ui.screens.home.components.BottomAction
import com.thinh.snaplet.ui.screens.home.components.CameraPage
import com.thinh.snaplet.ui.screens.home.components.EmptyMediaPage
import com.thinh.snaplet.ui.screens.home.components.MediaPage
import com.thinh.snaplet.ui.screens.home.components.TopAction
import com.thinh.snaplet.utils.permission.Permission
import kotlinx.coroutines.launch

private const val CAMERA_PAGE_INDEX = 0

@Immutable
data class CameraActions(
    val onImageCaptureReady: (ImageCapture) -> Unit,
    val onSnapshotHandlerReady: (() -> Bitmap?) -> Unit,
    val onCapturePhoto: () -> Unit,
    val onSwitchCamera: () -> Unit,
    val onCancelCapture: () -> Unit,
    val onUploadPost: () -> Unit,
    val onCaptionChange: (String) -> Unit,
    val onRequestPermission: () -> Unit
)

@Composable
fun Home(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val pageCount = 1 + if (uiState.posts.isEmpty()) 1 else uiState.posts.size
    val pagerState = rememberPagerState(
        initialPage = CAMERA_PAGE_INDEX, pageCount = { pageCount })

    var snapshotHandler by remember { mutableStateOf<(() -> Bitmap?)?>(null) }

    val cameraActions = remember(viewModel) {
        CameraActions(
            onImageCaptureReady = viewModel::setImageCapture,
            onSnapshotHandlerReady = { snapshotHandler = it },
            onCapturePhoto = { viewModel.onCapturePhoto(context) },
            onSwitchCamera = viewModel::onSwitchCamera,
            onCancelCapture = viewModel::onCancelCapture,
            onUploadPost = viewModel::onUploadPost,
            onCaptionChange = viewModel::updateCurrentCaption,
            onRequestPermission = viewModel::onScreenInitialized
        )
    }

    CameraBindingEffect(
        pagerState = pagerState,
        shouldBindCamera = uiState.cameraState.shouldBindCamera,
        snapshotHandler = snapshotHandler,
        onCameraPageVisible = viewModel::onCameraPageVisible,
        onCameraPageHidden = viewModel::onCameraPageHidden,
        onSnapshotCaptured = viewModel::setPreviewSnapshot
    )

    val snackBarHostState = remember { SnackbarHostState() }

    PermissionHandler(
        permission = Permission.Camera, onPermissionResult = viewModel::onPermissionResult
    ) { requestPermission ->

        UiEventEffect(
            viewModel = viewModel,
            context = context,
            snackBarHostState = snackBarHostState,
            requestPermission = requestPermission,
            onScrollToFirstPost = { pagerState.animateScrollToPage(1) })

        HomeScreen(
            pagerState = pagerState,
            uiState = uiState,
            cameraActions = cameraActions,
            onNavigateToCameraPage = {
                scope.launch { pagerState.animateScrollToPage(CAMERA_PAGE_INDEX) }
            },
            onItemVisible = viewModel::onItemVisible,
            onMoreClick = viewModel::onShowMoreOptions
        )

        SnackbarHost(hostState = snackBarHostState)
    }
}

@Composable
private fun CameraBindingEffect(
    pagerState: PagerState,
    shouldBindCamera: Boolean,
    snapshotHandler: (() -> Bitmap?)?,
    onCameraPageVisible: () -> Unit,
    onCameraPageHidden: () -> Unit,
    onSnapshotCaptured: (Bitmap) -> Unit
) {
    val isOnCameraPage = pagerState.currentPage == CAMERA_PAGE_INDEX

    LaunchedEffect(isOnCameraPage, pagerState.isScrollInProgress) {
        when {
            isOnCameraPage && !pagerState.isScrollInProgress -> {
                onCameraPageVisible()
            }

            !isOnCameraPage && shouldBindCamera -> {
                snapshotHandler?.invoke()?.let(onSnapshotCaptured)
                onCameraPageHidden()
            }
        }
    }
}

@Composable
private fun UiEventEffect(
    viewModel: HomeViewModel,
    context: Context,
    snackBarHostState: SnackbarHostState,
    requestPermission: () -> Unit,
    onScrollToFirstPost: suspend () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.onScreenInitialized()

        viewModel.uiEvent.collect { event ->
            when (event) {
                is HomeUiEvent.RequestPermission -> {
                    requestPermission()
                }

                is HomeUiEvent.ShowError -> {
                    snackBarHostState.showSnackbar(
                        message = event.message.asString(context),
                        duration = SnackbarDuration.Short,
                    )
                }

                is HomeUiEvent.ShowSuccess -> {
                    snackBarHostState.showSnackbar(
                        message = event.message.asString(context),
                        duration = SnackbarDuration.Short,
                    )
                }

                is HomeUiEvent.ScrollToFirstPost -> onScrollToFirstPost()
            }
        }
    }
}

@Composable
private fun HomeScreen(
    pagerState: PagerState,
    uiState: HomeUiState,
    cameraActions: CameraActions,
    onNavigateToCameraPage: () -> Unit,
    onItemVisible: (currentIndex: Int) -> Unit,
    onMoreClick: () -> Unit,
) {
    val showGlobalBottomAction by remember {
        derivedStateOf {
            val absolutePosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
            absolutePosition > 1.0f
        }
    }

    val userScrollEnabled = !uiState.cameraState.isEditMode

    LaunchedEffect(pagerState.currentPage) {
        val currentPage = pagerState.currentPage
        if (currentPage > CAMERA_PAGE_INDEX) {
            val postIndex = currentPage - 1
            onItemVisible(postIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomePager(
            pagerState = pagerState,
            posts = uiState.posts,
            uploadStatuses = uiState.uploadStatuses,
            cameraState = uiState.cameraState,
            currentCaption = uiState.currentCaption,
            isUploading = uiState.isUploading,
            showLocalBottomAction = !showGlobalBottomAction,
            userScrollEnabled = userScrollEnabled,
            cameraActions = cameraActions,
            onNavigateToCameraPage = onNavigateToCameraPage,
            onMoreClick = onMoreClick
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

        if (showGlobalBottomAction) {
            BottomAction(
                onGridClick = { /* TODO */ },
                onCaptureClick = onNavigateToCameraPage,
                onMoreClick = onMoreClick,
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
private fun HomePager(
    pagerState: PagerState,
    posts: List<Post>,
    uploadStatuses: Map<String, UploadStatus>,
    cameraState: CameraState,
    currentCaption: String?,
    isUploading: Boolean,
    showLocalBottomAction: Boolean,
    userScrollEnabled: Boolean,
    cameraActions: CameraActions,
    onNavigateToCameraPage: () -> Unit,
    onMoreClick: () -> Unit
) {
    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = userScrollEnabled,
        horizontalAlignment = Alignment.CenterHorizontally,
        beyondViewportPageCount = 1,
        key = { page ->
            when {
                page == CAMERA_PAGE_INDEX -> "camera"
                posts.isEmpty() -> "empty_media"
                else -> posts.getOrNull(page - 1)?.id ?: "unknown_$page"
            }
        }) { page ->
        when (page) {
            CAMERA_PAGE_INDEX -> CameraPage(
                cameraState = cameraState,
                currentCaption = currentCaption,
                isUploading = isUploading,
                cameraActions = cameraActions
            )

            else -> {
                if (posts.isEmpty()) {
                    EmptyMediaPage(onAddFriendClick = { /* TODO: navigate to add friend */ })
                } else {
                    val post = posts[page - 1]
                    MediaPage(
                        post = post,
                        uploadStatus = uploadStatuses[post.id],
                        showBottomAction = showLocalBottomAction,
                        onGridClick = { /* TODO */ },
                        onCaptureClick = onNavigateToCameraPage,
                        onMoreClick = onMoreClick
                    )
                }
            }
        }
    }
}