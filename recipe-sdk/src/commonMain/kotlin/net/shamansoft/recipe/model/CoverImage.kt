package net.shamansoft.recipe.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a cover image for a recipe.
 *
 * @property path URL or path to the image
 * @property alt Alternative text for the image
 */
@Serializable
data class CoverImage(
    @SerialName("path")
    val path: String,

    @SerialName("alt")
    val alt: String = ""
)
