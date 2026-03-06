package com.thinh.snaplet.ui.screens.image_crop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun ImageCrop(
    cropImageCropViewModel: ImageCropViewModel = hiltViewModel(),
    onCropDone: (Bitmap) -> Unit = { bitmap -> Logger.d("done: $bitmap") }
) {
    val uiState by cropImageCropViewModel.uiState.collectAsStateWithLifecycle()
    val imageUri: Uri? = (uiState.imageUri)?.toUri()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111827)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (imageUri != null) {
                key(imageUri) {
                    AvatarCropper(
                        uri = imageUri, frameSize = 400.dp, onCropDone = onCropDone
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  AvatarCropper
// ─────────────────────────────────────────────────────────────

@Composable
fun AvatarCropper(
    modifier: Modifier = Modifier,
    uri: Uri,
    frameSize: Dp = 400.dp,
    onCropDone: (Bitmap) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var imageWidthPx by remember { mutableIntStateOf(0) }
    var imageHeightPx by remember { mutableIntStateOf(0) }

    val framePx = with(density) { frameSize.toPx() }

    var frameOffsetY by remember { mutableFloatStateOf(0f) }
    var imageScale by remember { mutableFloatStateOf(1f) }

    // minScale: scale tối thiểu để ảnh luôn phủ đủ frame về cả 2 chiều.
    // Ảnh landscape (height nhỏ hơn framePx) → minScale > 1 → ảnh tự scale lên.
    // Tính mỗi khi imageHeightPx thay đổi (sau khi Coil render xong).
    fun computeMinScale(imgH: Int): Float {
        if (imgH == 0) return 1f
        return maxOf(framePx / imgH, 1f)
    }

    fun clampFrame(offset: Float, scale: Float = imageScale): Float {
        if (imageHeightPx == 0) return 0f
        val scaledH = imageHeightPx * scale
        val halfFrame = framePx / 2f
        val halfImg = scaledH / 2f
        if (halfImg <= halfFrame) return 0f
        return offset.coerceIn(-(halfImg - halfFrame), halfImg - halfFrame)
    }

    val frameTop = imageHeightPx / 2f - framePx / 2f + frameOffsetY

    var isCropping by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (zoom != 1f) {
                            // minScale là lower bound — không cho zoom nhỏ hơn mức phủ frame
                            val minScale = computeMinScale(imageHeightPx)
                            val newScale = (imageScale * zoom).coerceIn(minScale, minScale * 5f)
                            frameOffsetY = clampFrame(frameOffsetY, newScale)
                            imageScale = newScale
                        } else {
                            frameOffsetY = clampFrame(frameOffsetY + pan.y)
                        }
                    }
                }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(uri).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        val newH = coords.size.height
                        val newW = coords.size.width
                        if (newH != imageHeightPx || newW != imageWidthPx) {
                            imageWidthPx = newW
                            imageHeightPx = newH
                            // Khi Coil render xong và đo được kích thước thực:
                            // nếu ảnh landscape (height < framePx) → apply minScale ngay
                            // để ảnh scale lên phủ frame, không lộ nền đen
                            val minScale = computeMinScale(newH)
                            if (imageScale < minScale) {
                                imageScale = minScale
                                frameOffsetY = 0f
                            }
                        }
                    }
                    .graphicsLayer {
                        scaleX = imageScale
                        scaleY = imageScale
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                    }
            )

            if (imageHeightPx > 0) {
                Canvas(
                    modifier = Modifier
                        .width(with(density) { imageWidthPx.toDp() })
                        .height(with(density) { imageHeightPx.toDp() })
                ) {
                    drawDimRects(frameTop, framePx)
                    drawGrid(frameTop, framePx)
                    drawCornerHandles(frameTop, framePx)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val snapImageScale = imageScale
                val snapImageW = imageWidthPx
                val snapImageH = imageHeightPx
                val snapFrameTop = frameTop

                isCropping = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        cropRegion(
                            context = context,
                            uri = uri,
                            displayImageW = snapImageW,
                            displayImageH = snapImageH,
                            displayScale = snapImageScale,
                            frameTop = snapFrameTop,
                            framePx = framePx
                        )
                    }
                    isCropping = false
                    if (result != null) onCropDone(result)
                }
            },
            enabled = !isCropping && imageHeightPx > 0,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
        ) {
            if (isCropping) {
                CircularProgressIndicator(
                    color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp
                )
            } else {
                BaseText("✂  Cắt ảnh")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Crop logic
// ─────────────────────────────────────────────────────────────

/**
 * Tính vùng ảnh gốc tương ứng với frame rồi decode bằng BitmapRegionDecoder.
 *
 * Khi imageScale > 1 (ảnh landscape đã được scale lên để phủ frame),
 * cần tính ngược scale để ra đúng pixel gốc:
 *
 *   scaledImgTop  = (displayH - displayH * scale) / 2   ← pivot center
 *   frameTopInScaled   = frameTop - scaledImgTop
 *   frameTopInDisplay  = frameTopInScaled / scale
 *   srcTop = frameTopInDisplay * (origH / displayH)
 */
private fun cropRegion(
    context: Context,
    uri: Uri,
    displayImageW: Int,
    displayImageH: Int,
    displayScale: Float,
    frameTop: Float,
    framePx: Float
): Bitmap? {
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
        val origW = opts.outWidth
        val origH = opts.outHeight

        val scaledH = displayImageH * displayScale
        val scaledImgTop = (displayImageH - scaledH) / 2f

        val frameTopInScaled = frameTop - scaledImgTop
        val frameBottomInScaled = frameTopInScaled + framePx

        val frameTopInDisplay = frameTopInScaled / displayScale
        val frameBottomInDisplay = frameBottomInScaled / displayScale

        val ratioH = origH.toFloat() / displayImageH

        val srcLeft = 0
        val srcTop = (frameTopInDisplay * ratioH).roundToInt().coerceIn(0, origH)
        val srcRight = origW
        val srcBottom = (frameBottomInDisplay * ratioH).roundToInt().coerceIn(0, origH)

        if (srcBottom <= srcTop) return null

        val srcRect = Rect(srcLeft, srcTop, srcRight, srcBottom)
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val decoder = BitmapRegionDecoder.newInstance(stream, false)
            decoder?.decodeRegion(srcRect, BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }).also { decoder?.recycle() }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// ─────────────────────────────────────────────────────────────
//  Drawing helpers
// ─────────────────────────────────────────────────────────────

private fun DrawScope.drawDimRects(frameTop: Float, frameH: Float) {
    val dim = Color(0x99000000)
    val frameBottom = frameTop + frameH
    if (frameTop > 0f) drawRect(dim, topLeft = Offset.Zero, size = Size(size.width, frameTop))
    if (frameBottom < size.height) drawRect(
        dim, topLeft = Offset(0f, frameBottom), size = Size(size.width, size.height - frameBottom)
    )
}

private fun DrawScope.drawGrid(frameTop: Float, frameH: Float) {
    clipRect(0f, frameTop, size.width, frameTop + frameH) {
        val color = Color.White.copy(alpha = .22f)
        val stroke = 1.dp.toPx()
        for (i in 1..2) {
            val x = size.width * i / 3f
            val y = frameTop + frameH * i / 3f
            drawLine(color, Offset(x, frameTop), Offset(x, frameTop + frameH), strokeWidth = stroke)
            drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
        }
    }
}

private fun DrawScope.drawCornerHandles(frameTop: Float, frameH: Float) {
    val color = Color.White
    val stroke = 3.dp.toPx()
    val len = 24.dp.toPx()
    val frameBottom = frameTop + frameH
    val right = size.width

    drawLine(color, Offset(0f, frameTop), Offset(len, frameTop), strokeWidth = stroke)
    drawLine(color, Offset(0f, frameTop), Offset(0f, frameTop + len), strokeWidth = stroke)
    drawLine(color, Offset(right, frameTop), Offset(right - len, frameTop), strokeWidth = stroke)
    drawLine(color, Offset(right, frameTop), Offset(right, frameTop + len), strokeWidth = stroke)
    drawLine(color, Offset(0f, frameBottom), Offset(len, frameBottom), strokeWidth = stroke)
    drawLine(color, Offset(0f, frameBottom), Offset(0f, frameBottom - len), strokeWidth = stroke)
    drawLine(
        color,
        Offset(right, frameBottom),
        Offset(right - len, frameBottom),
        strokeWidth = stroke
    )
    drawLine(
        color,
        Offset(right, frameBottom),
        Offset(right, frameBottom - len),
        strokeWidth = stroke
    )
}