package com.thinh.snaplet.ui.screens.home.components

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.AsyncImage
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.CameraPreview
import com.thinh.snaplet.ui.components.ImageSize
import com.thinh.snaplet.ui.screens.home.CameraState
import com.thinh.snaplet.ui.screens.home.HomeViewModel

private const val TOP_SPACE_RATIO = 0.12f

@Composable
fun CameraPage(
    onImageCaptureReady: (ImageCapture) -> Unit,
    onSnapshotHandlerReady: (() -> Bitmap?) -> Unit,
    shouldBindCamera: Boolean,
    modifier: Modifier = Modifier
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val cameraState = uiState.cameraState
    val currentCaption = uiState.currentCaption
    val isUploading = uiState.isUploading
    val focusManager = LocalFocusManager.current

    BoxWithConstraints(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        }
    ) {
        val screenHeight = maxHeight
        val density = LocalDensity.current
        val topPadding = screenHeight * TOP_SPACE_RATIO

        val imeHeightPx = WindowInsets.ime.getBottom(density).toFloat()

        val overlapPx = remember(imeHeightPx) {
            val mediaBottomPx = with(density) { (topPadding + MediaItemDimensions.MEDIA_HEIGHT).toPx() }
            val screenHeightPx = with(density) { screenHeight.toPx() }
            val availableSpacePx = screenHeightPx - imeHeightPx
            (mediaBottomPx - availableSpacePx).coerceAtLeast(0f)
        }
        val mediaOffsetPx by animateFloatAsState(
            targetValue = -overlapPx,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
            label = "media_offset"
        )

        val actionOffsetPx = remember {
            with(density) { (screenHeight * (2.2f / 3f)).toPx() }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topPadding)
                .fillMaxWidth()
                .height(MediaItemDimensions.MEDIA_HEIGHT)
                .graphicsLayer { translationY = mediaOffsetPx }
                .clip(RoundedCornerShape(MediaItemDimensions.MEDIA_CORNER_RADIUS))
        ) {
            MediaDisplaySection(
                cameraState = cameraState,
                currentCaption = currentCaption,
                onImageCaptureReady = onImageCaptureReady,
                onSnapshotHandlerReady = onSnapshotHandlerReady,
                shouldBindCamera = shouldBindCamera,
                onRequestPermission = viewModel::onScreenInitialized,
                onCaptionChange = viewModel::updateCurrentCaption,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer { translationY = actionOffsetPx }
        ) {
            CameraAction(
                modifier = Modifier.navigationBarsPadding(),
                capturedImagePath = cameraState.capturedImagePath,
                onCapturePhoto = { viewModel.onCapturePhoto(context) },
                onSwitchCamera = viewModel::onSwitchCamera,
                onCancelCapture = viewModel::onCancelCapture,
                onUploadPost = viewModel::onUploadPost,
                isUploading = isUploading,
            )
        }
    }
}

@Composable
private fun MediaDisplaySection(
    cameraState: CameraState,
    currentCaption: String?,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onSnapshotHandlerReady: (() -> Bitmap?) -> Unit,
    shouldBindCamera: Boolean,
    onRequestPermission: () -> Unit,
    onCaptionChange: (String) -> Unit,
) {
    val capturedImagePath = cameraState.capturedImagePath

    Box(modifier = Modifier.fillMaxSize()) {
        capturedImagePath?.let { path ->
            val imageUri = "file://$path".toUri()
            val isFrontCamera = cameraState.lensFacing == CameraSelector.LENS_FACING_FRONT
            Box(modifier = Modifier.zIndex(100f)) {
                AsyncImage(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (isFrontCamera) {
                                scaleX = -1f
                            }
                        },
                    imageUrl = imageUri.toString(),
                    contentDescription = "Captured image",
                    contentScale = ContentScale.Fit,
                    resizeSize = ImageSize.Small,
                    showLoadingIndicator = false,
                    showErrorIcon = false
                )

                CaptionInput(
                    caption = currentCaption ?: "",
                    onCaptionChange = onCaptionChange,
                    modifier = Modifier
                        .zIndex(100f)
                        .align(Alignment.BottomCenter)
                )
            }
        }

        if (cameraState.hasCameraPermission) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onImageCaptureReady = onImageCaptureReady,
                onSnapshotHandlerReady = onSnapshotHandlerReady,
                shouldBindCamera = shouldBindCamera,
                lensFacing = cameraState.lensFacing,
                placeholderBitmap = cameraState.lastPreviewSnapshot
            )
        } else {
            CameraPermissionDenied(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                onRequestPermission = onRequestPermission
            )
        }
    }
}

@Composable
private fun CaptionInput(
    modifier: Modifier = Modifier,
    caption: String,
    onCaptionChange: (String) -> Unit,
) {
    Box(
        modifier = modifier
            .padding(bottom = 12.dp)
            .padding(horizontal = 12.dp)
    ) {
        TextField(
            value = caption,
            onValueChange = onCaptionChange,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(0.6f),
                    shape = CircleShape
                ),
            placeholder = {
                BaseText(
                    text = stringResource(R.string.add_caption_placeholder),
                    textAlign = TextAlign.Center,
                    typography = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.onSurface
            ),
            maxLines = 2,
            shape = CircleShape
        )
    }
}