package com.thinh.snaplet.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.util.Rational
import android.view.Surface
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.thinh.snaplet.ui.theme.MotionTokens
import com.thinh.snaplet.utils.Logger

private const val STREAMING_READY_DELAY = 100L

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    shouldBindCamera: Boolean = true,
    lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    placeholderBitmap: Bitmap?,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onSnapshotHandlerReady: (() -> Bitmap?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentPreviewView by remember { mutableStateOf<PreviewView?>(null) }
    var isCameraStreamingReady by remember { mutableStateOf(false) }

    LaunchedEffect(shouldBindCamera) {
        if (!shouldBindCamera) {
            isCameraStreamingReady = false
        }
    }

    LaunchedEffect(currentPreviewView) {
        currentPreviewView?.let { previewView ->
            onSnapshotHandlerReady { captureSnapshot(previewView) }
        }
    }

    Box(modifier) {
        if (shouldBindCamera) {
            CameraPreviewView(
                context = context,
                lifecycleOwner = lifecycleOwner,
                lensFacing = lensFacing,
                onPreviewViewCreated = { currentPreviewView = it },
                onImageCaptureReady = onImageCaptureReady,
                onStreamingStateChanged = { isStreaming ->
                    isCameraStreamingReady = isStreaming
                }
            )
        }

        CameraPlaceholderOverlay(
            isVisible = !isCameraStreamingReady,
            snapshot = placeholderBitmap
        )
    }
}

@Composable
private fun CameraPreviewView(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    lensFacing: Int,
    onPreviewViewCreated: (PreviewView) -> Unit,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onStreamingStateChanged: (Boolean) -> Unit
) {
    val executor = remember { ContextCompat.getMainExecutor(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Initialize CameraProvider
    LaunchedEffect(context) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
        }, executor)
    }

    // Bind/Re-bind camera when dependencies change
    LaunchedEffect(cameraProvider, previewView, lensFacing) {
        val provider = cameraProvider
        val view = previewView
        if (provider != null && view != null) {
            bindCameraUseCases(
                cameraProvider = provider,
                previewView = view,
                lensFacing = lensFacing,
                lifecycleOwner = lifecycleOwner,
                onImageCaptureReady = onImageCaptureReady,
                onStreamingStateChanged = onStreamingStateChanged
            )
        }
    }

    AndroidView(
        factory = { ctx ->
            createPreviewView(ctx).also {
                previewView = it
                onPreviewViewCreated(it)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun CameraPlaceholderOverlay(
    isVisible: Boolean,
    snapshot: Bitmap?
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(MotionTokens.Slow)),
        exit = fadeOut(animationSpec = tween(MotionTokens.Slow))
    ) {
        snapshot?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Camera preview placeholder",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

private fun createPreviewView(context: Context): PreviewView {
    return PreviewView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        scaleType = PreviewView.ScaleType.FILL_CENTER
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    }
}

private fun bindCameraUseCases(
    cameraProvider: ProcessCameraProvider,
    previewView: PreviewView,
    lensFacing: Int,
    lifecycleOwner: LifecycleOwner,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onStreamingStateChanged: (Boolean) -> Unit
) {
    val width = previewView.width
    val height = previewView.height

    // If width or height is 0, we might need to wait for layout
    if (width <= 0 || height <= 0) {
        previewView.post {
            bindCameraUseCases(
                cameraProvider,
                previewView,
                lensFacing,
                lifecycleOwner,
                onImageCaptureReady,
                onStreamingStateChanged
            )
        }
        return
    }

    val preview = Preview.Builder()
        .build()
        .also { it.surfaceProvider = previewView.surfaceProvider }

    val aspectRatio = Rational(width, height)
    val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
    
    val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setTargetRotation(rotation)
        .build()

    onImageCaptureReady(imageCapture)

    val viewPort = ViewPort.Builder(aspectRatio, rotation).build()

    val useCaseGroup = UseCaseGroup.Builder()
        .addUseCase(preview)
        .addUseCase(imageCapture)
        .setViewPort(viewPort)
        .build()

    // Bind to lifecycle
    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()

    try {
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)
    } catch (e: Exception) {
        Logger.e("Camera binding failed: ${e.message}", e)
    }

    // Monitor streaming state
    observeStreamingState(previewView, lifecycleOwner, onStreamingStateChanged)
}

private fun observeStreamingState(
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    onStreamingStateChanged: (Boolean) -> Unit
) {
    var isStreamingReady = false

    previewView.previewStreamState.observe(lifecycleOwner) { streamState ->
        when (streamState) {
            PreviewView.StreamState.STREAMING -> {
                if (!isStreamingReady) {
                    previewView.postDelayed({
                        isStreamingReady = true
                        onStreamingStateChanged(true)
                    }, STREAMING_READY_DELAY)
                }
            }

            else -> {
                // Logger.d("📹 Camera stream state: $streamState")
                if (isStreamingReady) {
                    isStreamingReady = false
                    onStreamingStateChanged(false)
                }
            }
        }
    }
}

private fun captureSnapshot(previewView: PreviewView): Bitmap? {
    return previewView.bitmap ?: run {
        Logger.e("❌ Failed to capture bitmap - bitmap is null")
        null
    }
}