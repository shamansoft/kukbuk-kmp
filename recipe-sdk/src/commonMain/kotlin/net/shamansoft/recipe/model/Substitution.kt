package net.shamansoft.recipe.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a substitution for an ingredient.
 *
 * @property item Name of the substitute ingredient
 * @property amount Amount of the substitute (can be numeric or descriptive)
 * @property unit Unit of measurement
 * @property notes Additional notes about the substitution
 * @property ratio Conversion ratio like "1:1" or "2:1"
 */
@Serializable
data class Substitution(
    @SerialName("item")
    val item: String,

    @SerialName("amount")
    val amount: String? = null,

    @SerialName("unit")
    val unit: String? = null,

    @SerialName("notes")
    val notes: String? = null,

    @SerialName("ratio")
    val ratio: String? = null
)
