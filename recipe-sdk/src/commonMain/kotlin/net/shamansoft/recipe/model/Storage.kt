package net.shamansoft.recipe.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents storage instructions for a recipe.
 *
 * @property refrigerator How long the recipe can be stored in the refrigerator
 * @property freezer How long the recipe can be stored in the freezer
 * @property roomTemperature How long the recipe can be stored at room temperature
 */
@Serializable
data class Storage(
    @SerialName("refrigerator")
    val refrigerator: String? = null,

    @SerialName("freezer")
    val freezer: String? = null,

    @SerialName("room_temperature")
    val roomTemperature: String? = null
)
