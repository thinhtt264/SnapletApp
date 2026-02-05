package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.thinh.snaplet.data.model.Post
import com.thinh.snaplet.ui.common.CommonImages
import com.thinh.snaplet.ui.components.AsyncImage
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.ImageSize
import com.thinh.snaplet.ui.screens.home.UploadStatus
import com.thinh.snaplet.utils.formatTimeAgo

private const val TOP_SPACE_RATIO = 0.15f

@Composable
fun MediaPage(
    post: Post,
    uploadStatus: UploadStatus?,
    showBottomAction: Boolean = false,
    onGridClick: () -> Unit = {},
    onCaptureClick: () -> Unit = {},
    onMoreClick: () -> Unit = {}
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val topPadding = screenHeight * TOP_SPACE_RATIO

        Column(
            modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(topPadding))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MediaItemDimensions.MEDIA_HEIGHT)
                    .clip(RoundedCornerShape(MediaItemDimensions.MEDIA_CORNER_RADIUS))
            ) {
                PostMediaContent(post = post)
            }

            Spacer(Modifier.height(12.dp))

            PostMetadata(
                post = post, uploadStatus = uploadStatus
            )
        }

        // --- Bottom Actions ---
        if (showBottomAction) {
            BottomAction(
                onGridClick = onGridClick,
                onCaptureClick = onCaptureClick,
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
private fun PostMediaContent(post: Post) {
    val media = post.media.firstOrNull() ?: return
    val transform = media.transform

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            imageUrl = media.images.md.ifEmpty { media.originalUrl.orEmpty() },
            contentDescription = "Post ${post.id}",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transform?.let {
                        rotationZ = it.rotation.toFloat()
                        scaleX = it.scaleX
                        scaleY = it.scaleY
                    }
                },
            resizeSize = ImageSize.Small,
            contentScale = ContentScale.Crop,
            showLoadingIndicator = true,
            errorBackgroundColor = MaterialTheme.colorScheme.surface,
            errorPlaceholder = painterResource(CommonImages.PhotoPlaceholder)
        )

        if (!post.caption.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .zIndex(99f)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .padding(horizontal = 12.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.4f), shape = CircleShape
                    )
                    .padding(vertical = 8.dp, horizontal = 12.dp)
            ) {
                BaseText(
                    text = post.caption,
                    typography = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PostMetadata(
    modifier: Modifier = Modifier, post: Post, uploadStatus: UploadStatus?
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (uploadStatus is UploadStatus.Uploading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onBackground,
                strokeWidth = 2.dp
            )
            BaseText(
                text = "Uploading....",
                typography = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
            ) {
                AsyncImage(
                    imageUrl = post.avatarUrl ?: "",
                    contentDescription = "Avatar of ${post.displayName}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    resizeSize = ImageSize.Thumbnail,
                    showLoadingIndicator = true
                )
            }
            BaseText(
                text = post.firstName,
                typography = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        BaseText(
            text = " ${formatTimeAgo(post.createdAt)}",
            typography = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}