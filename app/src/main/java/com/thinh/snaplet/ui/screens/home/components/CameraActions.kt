package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.ui.theme.GoldenPollen
import kotlinx.coroutines.delay
import pressScaleClickable

private val CAPTURE_BUTTON_SIZE = 96.dp
private val ICON_SIZE = 46.dp
private val CAPTURE_BUTTON_BORDER_WIDTH = 4.dp
private const val CAPTURE_ANIMATION_DURATION = 200
private const val CAPTURE_BUTTON_MIN_SCALE = 0.85f
private const val CAPTURE_BUTTON_MAX_SCALE = 1f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraAction(
    modifier: Modifier = Modifier,
    capturedImagePath: String?,
    onCapturePhoto: () -> Unit,
    onCancelCapture: () -> Unit,
    onSwitchCamera: () -> Unit,
    onUploadPost: () -> Unit,
    isUploading: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val hasCaptureImage = capturedImagePath != null

        val onClickLeftImage = if (hasCaptureImage) {
            onCancelCapture
        } else {
            {}
        }

        Icon(
            imageVector = if (hasCaptureImage)
                Icons.Outlined.Close
            else
                Icons.Outlined.AddPhotoAlternate,
            contentDescription = if (hasCaptureImage) "Cancel capture" else "Upload from gallery",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .size(ICON_SIZE)
                .pressScaleClickable(onClick = onClickLeftImage),
        )

        if (hasCaptureImage) {
            Box(
                modifier = Modifier
                    .size(CAPTURE_BUTTON_SIZE)
                    .clip(shape = CircleShape)
                    .align(Alignment.CenterVertically)
                    .background(MaterialTheme.colorScheme.secondary)
                    .pressScaleClickable(
                        onClick = {
                            if (!isUploading) {
                                onUploadPost()
                            }
                        },
                        enabled = !isUploading
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.NearMe,
                    contentDescription = "Upload posts",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(ICON_SIZE)
                )
            }
        } else CaptureButton(modifier, onCapturePhoto)

        Icon(
            Icons.Outlined.Cached,
            contentDescription = "Switch camera lens",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .size(ICON_SIZE)
                .pressScaleClickable(onClick = onSwitchCamera),
        )
    }
}

@Composable
fun CaptureButton(modifier: Modifier, onCapturePhoto: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.secondary
        } else {
            Color.White
        },
        animationSpec = tween(durationMillis = CAPTURE_ANIMATION_DURATION),
        label = "capture_button_color"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) {
            CAPTURE_BUTTON_MIN_SCALE
        } else {
            CAPTURE_BUTTON_MAX_SCALE
        },
        animationSpec = spring(),
        label = "capture_button_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "infinite transition")
    val borderPulseWidth by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border pulse width animation"
    )

    Box(
        modifier = modifier
            .size(CAPTURE_BUTTON_SIZE), contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val strokeWidth = if (isPressed) {
                CAPTURE_BUTTON_BORDER_WIDTH.toPx() * borderPulseWidth
            } else {
                CAPTURE_BUTTON_BORDER_WIDTH.toPx()
            }
            val opacity = if (isPressed) {
                (borderPulseWidth - 1f) * 0.7f + 0.4f
            } else {
                1f
            }
            drawCircle(
                color = GoldenPollen.copy(alpha = opacity),
                radius = size.minDimension / 2f,
                style = Stroke(width = strokeWidth)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(buttonScale)
                .padding(6.dp)
                .background(
                    color = backgroundColor, shape = CircleShape
                )
                .pressScaleClickable(
                    onClick = {
                        delay(3000)
                        onCapturePhoto()
                    },
                    interactionSource = interactionSource
                )
        )
    }
}