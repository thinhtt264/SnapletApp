package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.thinh.snaplet.data.model.Post
import com.thinh.snaplet.ui.components.AsyncImage
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.ImageSize
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.formatTimeAgo

@Composable
fun MediaItemPage(
    post: Post,
    onMediaClick: (Post) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Full-screen image/video
        val media = post.media.first()
        val transform = media.transform

        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                imageUrl = media.originalUrl.orEmpty(),
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
                contentScale = ContentScale.Crop,
                showLoadingIndicator = true,
                showErrorIcon = true
            )

            if(post.caption?.isNotBlank() == true) {
                Box(
                    modifier = Modifier
                        .zIndex(99f)
                        .align(Alignment.BottomCenter)
                        .offset(y = (-24).dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(0.6f),
                            shape = CircleShape
                        )
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    BaseText(
                        text = post.caption,
                        typography = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
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
                    resizeSize = ImageSize.Small,
                    showLoadingIndicator = true
                )
            }

            BaseText(
                text = post.firstName,
                typography = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            BaseText(
                text = " ${formatTimeAgo(post.createdAt)}",
                typography = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
