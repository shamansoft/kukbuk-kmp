package net.shamansoft.recipe.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents nutritional information for a recipe.
 *
 * @property servingSize Serving size description
 * @property calories Calories per serving
 * @property protein Protein in grams
 * @property carbohydrates Carbohydrates in grams
 * @property fat Fat in grams
 * @property fiber Fiber in grams
 * @property sugar Sugar in grams
 * @property sodium Sodium in milligrams
 * @property notes Additional notes about nutritional information
 */
@Serializable
data class Nutrition(
    @SerialName("serving_size")
    val servingSize: String? = null,

    @SerialName("calories")
    val calories: Int? = null,

    @SerialName("protein")
    val protein: Double? = null,

    @SerialName("carbohydrates")
    val carbohydrates: Double? = null,

    @SerialName("fat")
    val fat: Double? = null,

    @SerialName("fiber")
    val fiber: Double? = null,

    @SerialName("sugar")
    val sugar: Double? = null,

    @SerialName("sodium")
    val sodium: Double? = null,

    @SerialName("notes")
    val notes: String? = null
)
