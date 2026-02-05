package com.thinh.snaplet.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size

@Composable
fun AsyncImage(
    modifier: Modifier = Modifier,
    imageUrl: String,
    contentDescription: String?,
    contentScale: ContentScale = ContentScale.Crop,
    resizeSize: ImageSize = ImageSize.Small,
    showLoadingIndicator: Boolean = true,
    loadingBackgroundColor: Color = MaterialTheme.colorScheme.surface,
    errorBackgroundColor: Color = MaterialTheme.colorScheme.errorContainer,
    crossfadeDuration: Int = 300,
    errorPlaceholder: Painter? = null
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(crossfadeDuration)
            .size(resizeSize.toCoilSize())
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .allowHardware(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale
    ) {
        when (painter.state) {

            is AsyncImagePainter.State.Success -> {
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = modifier
                )
            }

            is AsyncImagePainter.State.Loading -> {
                if (showLoadingIndicator) {
                    LoadingState(
                        backgroundColor = loadingBackgroundColor
                    )
                }
            }

            else -> {
                ErrorState(
                    backgroundColor = errorBackgroundColor,
                    placeholder = errorPlaceholder
                )
            }
        }
    }
}

/**
 * Loading state composable
 */
@Composable
private fun LoadingState(
    backgroundColor: Color, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
    }
}

/**
 * Error state composable
 */
@Composable
private fun ErrorState(
    backgroundColor: Color, placeholder: Painter?, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (placeholder != null) {
            Icon(
                painter = placeholder,
                contentDescription = "Error loading image",
                modifier = Modifier.size(124.dp),
            )
        }
    }
}

/**
 * Image size presets for memory optimization
 *
 * Smaller sizes = Less memory usage = Better performance
 * Choose based on display size in UI
 */
enum class ImageSize(val pixels: Int) {
    /**
     * Thumbnail size (e.g., for small previews)
     * ~200KB in memory per image
     */
    Thumbnail(256),

    /**
     * Small size (e.g., for grid items)
     * ~500KB in memory per image
     */
    Small(512),

    /**
     * Medium size (e.g., for list items, default)
     * ~1MB in memory per image
     */
    Medium(1024),

    /**
     * Large size (e.g., for detail screens)
     * ~2MB in memory per image
     */
    Large(2048),

    /**
     * Original size (no resize)
     * Use sparingly - can be 5-10MB per image!
     */
    Original(Int.MAX_VALUE);

    /**
     * Convert to Coil's Size
     */
    fun toCoilSize(): Size {
        return if (this == Original) {
            Size.ORIGINAL
        } else {
            Size(pixels, pixels)
        }
    }
}