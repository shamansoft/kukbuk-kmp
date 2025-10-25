package net.shamansoft.kukbuk.recipe

import kotlinx.serialization.Serializable

/**
 * Represents a substitution for an ingredient
 */
@Serializable
data class Substitution(
    val item: String,
    val amount: Double? = null,
    val unit: String? = null,
    val notes: String? = null,
    val ratio: Double = 1.0
)

/**
 * Represents a structured ingredient with detailed information
 */
@Serializable
data class Ingredient(
    val item: String,
    val amount: Double? = null,
    val unit: String? = null,
    val notes: String? = null,
    val optional: Boolean = false,
    val substitutions: List<Substitution> = emptyList(),
    val component: String? = null // e.g., "dough", "filling", "sauce"
) {
    /**
     * Returns a display string for the ingredient
     * Format: "amount unit item (notes)"
     */
    fun toDisplayString(): String {
        val parts = mutableListOf<String>()

        if (amount != null && unit != null) {
            val formattedAmount = if (amount % 1.0 == 0.0) {
                amount.toInt().toString()
            } else {
                amount.toString()
            }
            parts.add("$formattedAmount $unit")
        } else if (amount != null) {
            parts.add(amount.toString())
        } else if (unit != null) {
            parts.add(unit)
        }

        parts.add(item)

        if (notes != null) {
            parts.add("($notes)")
        }

        if (optional) {
            parts.add("(optional)")
        }

        return parts.joinToString(" ")
    }
}

/**
 * Represents media (image or video) associated with a recipe step
 */
@Serializable
data class Media(
    val type: String, // "image" or "video"
    val path: String,
    val alt: String? = null,
    val thumbnail: String? = null,
    val duration: Int? = null // for videos, in seconds
)

/**
 * Represents a structured instruction step
 */
@Serializable
data class Instruction(
    val step: Int,
    val description: String,
    val time: String? = null, // e.g., "5 min"
    val temperature: String? = null, // e.g., "375°F"
    val media: List<Media> = emptyList()
)

/**
 * Represents nutritional information per serving
 */
@Serializable
data class NutritionInfo(
    val servingSize: String? = null,
    val calories: Int? = null,
    val protein: String? = null,
    val carbs: String? = null,
    val fat: String? = null,
    val fiber: String? = null,
    val sugar: String? = null,
    val sodium: String? = null
)

/**
 * Represents storage instructions
 */
@Serializable
data class StorageInfo(
    val refrigerator: String? = null, // e.g., "3-5 days"
    val freezer: String? = null, // e.g., "up to 3 months"
    val roomTemperature: String? = null // e.g., "2 hours maximum"
)

/**
 * Represents cover image metadata
 */
@Serializable
data class CoverImage(
    val path: String,
    val alt: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

/**
 * Main Recipe data class with full schema support
 */
@Serializable
data class Recipe(
    val id: String,
    val title: String,
    val author: String? = null,
    val description: String? = null,
    val prepTime: String? = null,
    val cookTime: String? = null,
    val totalTime: String? = null,
    val servings: String? = null,
    val difficulty: String? = null,
    val cuisine: String? = null,
    val tags: List<String> = emptyList(),

    // Structured ingredients and instructions
    val ingredients: List<Ingredient> = emptyList(),
    val instructions: List<Instruction> = emptyList(),

    // Additional metadata
    val dateCreated: String? = null,
    val categories: List<String> = emptyList(),
    val language: String? = null,
    val coverImage: CoverImage? = null,

    // Additional sections
    val equipment: List<String> = emptyList(),
    val nutrition: NutritionInfo? = null,
    val storage: StorageInfo? = null,

    // Version tracking
    val schemaVersion: String? = null,
    val recipeVersion: String? = null,

    // Other fields
    val notes: String? = null,
    val imageUrl: String? = null,
    val sourceUrl: String? = null,
    val driveFileId: String? = null,
    val lastModified: String? = null
)

@Serializable
data class RecipeMetadata(
    val id: String,
    val title: String,
    val author: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val categories: List<String> = emptyList(),
    val dateCreated: String? = null,
    val driveFileId: String,
    val lastModified: String
)

sealed class RecipeListState {
    data object Loading : RecipeListState()
    data class Success(val recipes: List<RecipeMetadata>) : RecipeListState()
    data class Error(val message: String) : RecipeListState()
    data object Empty : RecipeListState()
}

sealed class RecipeResult<out T> {
    data class Success<T>(val data: T) : RecipeResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : RecipeResult<Nothing>()
    data object Loading : RecipeResult<Nothing>()
}