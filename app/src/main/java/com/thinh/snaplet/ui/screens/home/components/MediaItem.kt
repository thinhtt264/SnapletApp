package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.data.model.Post
import com.thinh.snaplet.ui.components.AsyncImage
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.ImageSize
import com.thinh.snaplet.ui.theme.Gray
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
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                imageUrl = post.url,
                contentDescription = "Post ${post.id}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                resizeSize = ImageSize.Large, // High quality for full-screen
                showLoadingIndicator = true,
                showErrorIcon = true
            )
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
                typography = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )

            BaseText(
                text = " ${formatTimeAgo(post.createdAt)}",
                typography = MaterialTheme.typography.bodyMedium,
                color = Gray
            )
        }
    }
}
