package net.shamansoft.recipe.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a recipe instruction step.
 *
 * @property step Step number (integer starting from 1)
 * @property description Markdown-formatted instruction text (required)
 * @property time Duration for this step (e.g., "15m", "2h", "1h 30m")
 * @property temperature Temperature for this step (e.g., "180°C", "350°F")
 * @property media List of images or videos for this step
 */
@Serializable
data class Instruction(
    @SerialName("step")
    val step: Int? = null,

    @SerialName("description")
    val description: String,

    @SerialName("time")
    val time: String? = null,

    @SerialName("temperature")
    val temperature: String? = null,

    @SerialName("media")
    val media: List<Media>? = null
)
