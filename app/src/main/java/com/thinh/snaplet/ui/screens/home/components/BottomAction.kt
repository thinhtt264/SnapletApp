package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pressScaleClickable
import thenIf

@Composable
fun BottomAction(
    modifier: Modifier = Modifier,
    onGridClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onMoreClick: () -> Unit,
    showMoreButtonLoading: Boolean = false,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.pressScaleClickable(onClick = onGridClick)
        ) {
            Icon(
                imageVector = Icons.Outlined.GridView,
                contentDescription = "Grid",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp)
            )
        }

        CaptureButton(
            modifier = Modifier.size(56.dp), onCapturePhoto = onCaptureClick
        )

        Box(
            modifier = Modifier
                .size(36.dp)
                .thenIf(!showMoreButtonLoading) { pressScaleClickable(onClick = onMoreClick) },
            contentAlignment = Alignment.Center
        ) {
            if (showMoreButtonLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Pending,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}