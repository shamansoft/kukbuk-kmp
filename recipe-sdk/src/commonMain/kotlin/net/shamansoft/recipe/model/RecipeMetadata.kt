package net.shamansoft.recipe.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents metadata for a recipe.
 *
 * @property title Recipe name (required)
 * @property source Original recipe URL (required)
 * @property author Recipe author
 * @property language Language code (e.g., "en", "en/us")
 * @property dateCreated Date the recipe was created
 * @property category Recipe categories (e.g., "breakfast", "dessert")
 * @property tags Recipe tags (e.g., "chocolate", "vegan")
 * @property servings Number of servings (required)
 * @property prepTime Preparation time (e.g., "15m", "1h 30m")
 * @property cookTime Cooking time
 * @property totalTime Total time
 * @property difficulty Difficulty level ("easy", "medium", "hard")
 * @property coverImage Cover image for the recipe
 */
@Serializable
data class RecipeMetadata(
    @SerialName("title")
    val title: String,

    @SerialName("source")
    val source: String? = null,

    @SerialName("author")
    val author: String? = null,

    @SerialName("language")
    val language: String = "en",

    @SerialName("date_created")
    val dateCreated: LocalDate,

    @SerialName("category")
    val category: List<String> = emptyList(),

    @SerialName("tags")
    val tags: List<String> = emptyList(),

    @SerialName("servings")
    val servings: Int,

    @SerialName("prep_time")
    val prepTime: String? = null,

    @SerialName("cook_time")
    val cookTime: String? = null,

    @SerialName("total_time")
    val totalTime: String? = null,

    @SerialName("difficulty")
    val difficulty: String = "medium",

    @SerialName("cover_image")
    val coverImage: CoverImage? = null
)
