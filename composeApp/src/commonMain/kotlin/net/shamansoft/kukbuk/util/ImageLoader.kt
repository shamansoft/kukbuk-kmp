package net.shamansoft.kukbuk.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent

/**
 * Composable function for displaying recipe images with async loading
 *
 * Features:
 * - Async image loading with Coil
 * - Loading state with progress indicator
 * - Error state with placeholder emoji
 * - Automatic caching
 *
 * @param url The image URL to load
 * @param contentDescription Description for accessibility
 * @param modifier Modifier for the image
 * @param cornerRadius The corner radius for the image
 * @param contentScale How to scale the image content
 */
@Composable
fun RecipeImage(
    url: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    contentScale: ContentScale = ContentScale.Crop
) {
    // If no URL provided, show placeholder
    if (url.isNullOrEmpty()) {
        PlaceholderImage(modifier, cornerRadius)
        return
    }

    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier.clip(RoundedCornerShape(cornerRadius)),
        contentScale = contentScale
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading -> {
                // Show loading indicator while image loads
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            is AsyncImagePainter.State.Error -> {
                // Show placeholder on error
                PlaceholderImage(Modifier.matchParentSize(), cornerRadius)
            }
            is AsyncImagePainter.State.Success -> {
                // Show the loaded image
                SubcomposeAsyncImageContent()
            }
            else -> {
                // Empty state - should not happen
                PlaceholderImage(Modifier.matchParentSize(), cornerRadius)
            }
        }
    }
}

/**
 * Placeholder image with emoji when no image is available or loading fails
 */
@Composable
private fun PlaceholderImage(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🍽️",
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Media gallery for displaying images/videos in instruction steps
 * Shows images in a horizontal scrollable row
 *
 * @param media List of media items to display
 * @param modifier Modifier for the gallery container
 */
@Composable
fun MediaGallery(
    media: List<net.shamansoft.recipe.model.Media>,
    modifier: Modifier = Modifier
) {
    if (media.isEmpty()) return

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(media) { mediaItem ->
            MediaItem(media = mediaItem)
        }
    }
}

/**
 * Single media item (image or video thumbnail)
 */
@Composable
private fun MediaItem(
    media: net.shamansoft.recipe.model.Media,
    modifier: Modifier = Modifier
) {
    when (media.type.lowercase()) {
        "image" -> {
            RecipeImage(
                url = media.path,
                contentDescription = media.alt ?: "Step image",
                modifier = modifier
                    .width(200.dp)
                    .height(150.dp),
                cornerRadius = 8.dp,
                contentScale = ContentScale.Crop
            )
        }
        "video" -> {
            // For now, show video thumbnail (or placeholder if not available)
            // Future enhancement: Add video player
            RecipeImage(
                url = media.thumbnail ?: media.path,
                contentDescription = media.alt ?: "Video thumbnail",
                modifier = modifier
                    .width(200.dp)
                    .height(150.dp),
                cornerRadius = 8.dp,
                contentScale = ContentScale.Crop
            )
        }
        else -> {
            // Unknown media type - show as image
            RecipeImage(
                url = media.path,
                contentDescription = media.alt ?: "Media item",
                modifier = modifier
                    .width(200.dp)
                    .height(150.dp),
                cornerRadius = 8.dp,
                contentScale = ContentScale.Crop
            )
        }
    }
}
