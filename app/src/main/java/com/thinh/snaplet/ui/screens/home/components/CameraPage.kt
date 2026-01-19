package com.thinh.snaplet.ui.screens.home.components

import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.CameraPreview
import com.thinh.snaplet.ui.components.PrimaryButton
import com.thinh.snaplet.ui.screens.home.CameraState

private val CAPTURE_BUTTON_TOP_PADDING = 44.dp

@Composable
fun CameraPage(
    cameraState: CameraState,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onSnapshotHandlerReady: (() -> Bitmap?) -> Unit,
    shouldBindCamera: Boolean,
    onRequestPermission: () -> Unit,
    onCapturePhoto: () -> Unit,
    onSwitchCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CameraPreviewSection(
            cameraState = cameraState,
            onImageCaptureReady = onImageCaptureReady,
            onSnapshotHandlerReady = onSnapshotHandlerReady,
            shouldBindCamera = shouldBindCamera,
            onRequestPermission = onRequestPermission
        )

        CameraAction(
            onCapturePhoto = onCapturePhoto,
            onSwitchCamera = onSwitchCamera,
            modifier = Modifier.padding(top = CAPTURE_BUTTON_TOP_PADDING)
        )
    }
}

@Composable
private fun CameraPreviewSection(
    cameraState: CameraState,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onSnapshotHandlerReady: (() -> Bitmap?) -> Unit,
    shouldBindCamera: Boolean,
    onRequestPermission: () -> Unit
) {
    val cameraModifier = Modifier
        .fillMaxWidth()
        .height(MediaItemDimensions.MEDIA_HEIGHT)
        .padding(top = MediaItemDimensions.MEDIA_TOP_PADDING)
        .clip(RoundedCornerShape(MediaItemDimensions.MEDIA_CORNER_RADIUS))

    if (cameraState.hasCameraPermission) {
        CameraPreview(
            modifier = cameraModifier,
            onImageCaptureReady = onImageCaptureReady,
            onSnapshotHandlerReady = onSnapshotHandlerReady,
            shouldBindCamera = shouldBindCamera,
            lensFacing = cameraState.lensFacing,
            placeholderBitmap = cameraState.lastPreviewSnapshot
        )
    } else {
        CameraPermissionDenied(
            modifier = cameraModifier.background(MaterialTheme.colorScheme.surface),
            onRequestPermission = onRequestPermission
        )
    }
}

@Composable
private fun CameraPermissionDenied(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            BaseText(
                text = stringResource(R.string.camera_unavailable),
                typography = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            BaseText(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = stringResource(R.string.approve_camera),
                typography = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
            )
            PrimaryButton(
                onClick = onRequestPermission,
                title = stringResource(R.string.approve),
                titleColor = Color.Black,
                contentPadding = PaddingValues(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
            )
        }
    }
}