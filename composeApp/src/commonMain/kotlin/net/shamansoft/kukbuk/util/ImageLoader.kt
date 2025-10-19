package net.shamansoft.kukbuk.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Composable function for displaying recipe images as thumbnails
 *
 * Phase 1 Implementation: Shows placeholder emoji for now
 * Phase 2: Will integrate actual async image loading with caching
 *
 * @param url The image URL (currently used for logging)
 * @param contentDescription Description for accessibility
 * @param modifier Modifier for the image
 * @param cornerRadius The corner radius for the image
 */
@Composable
fun RecipeImage(
    url: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp
) {
    // Phase 1: Show placeholder card with emoji
    // URLs are preserved in metadata for future implementation
    if (!url.isNullOrEmpty()) {
        Logger.d("RecipeImage", "Image URL available: $url (will load in Phase 2)")
    }

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
