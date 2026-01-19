package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pressScaleClickable

private val CAPTURE_BUTTON_SIZE = 100.dp
private val ICON_SIZE = 44.dp
private val CAPTURE_BUTTON_BORDER_WIDTH = 6.dp
private const val CAPTURE_BUTTON_SCALE = 0.85f
private const val CAPTURE_ANIMATION_DURATION = 150

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraAction(
    modifier: Modifier = Modifier,
    onCapturePhoto: () -> Unit,
    onSwitchCamera: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            Icons.Outlined.AddPhotoAlternate,
            contentDescription = "Upload from gallery",
            tint = colorScheme.onBackground,
            modifier = Modifier
                .size(ICON_SIZE)
                .pressScaleClickable(onClick = {}),
        )

        CaptureButton(onClick = onCapturePhoto)

        Icon(
            Icons.Outlined.Cached,
            contentDescription = "Switch camera lens",
            tint = colorScheme.onBackground,
            modifier = Modifier
                .size(ICON_SIZE)
                .pressScaleClickable(onClick = onSwitchCamera),
        )
    }
}


@Composable
private fun CaptureButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
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

    Box(
        modifier = modifier
            .size(CAPTURE_BUTTON_SIZE)
            .border(
                width = CAPTURE_BUTTON_BORDER_WIDTH,
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ), contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .background(
                    color = backgroundColor, shape = CircleShape
                )
                .pressScaleClickable(
                    onClick = {
                        delay(2000)
                        onClick()
                    }, scaleOnPress = CAPTURE_BUTTON_SCALE, interactionSource = interactionSource
                )
        )
    }
}
