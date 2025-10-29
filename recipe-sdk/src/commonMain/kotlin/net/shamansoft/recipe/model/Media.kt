package net.shamansoft.recipe.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents media content (image or video) in recipe instructions.
 * Uses a single type with optional fields for simplicity.
 *
 * @property type Type of media: "image" or "video"
 * @property path URL or path to the media file
 * @property alt Alternative text (used for images)
 * @property thumbnail Thumbnail path (used for videos)
 * @property duration Duration in MM:SS format (used for videos, e.g., "2:30")
 */
@Serializable
data class Media(
    @SerialName("type")
    val type: String,

    @SerialName("path")
    val path: String,

    @SerialName("alt")
    val alt: String? = null,

    @SerialName("thumbnail")
    val thumbnail: String? = null,

    @SerialName("duration")
    val duration: String? = null
) {
    /**
     * Helper to check if this is an image
     */
    val isImage: Boolean
        get() = type == "image"

    /**
     * Helper to check if this is a video
     */
    val isVideo: Boolean
        get() = type == "video"
}
