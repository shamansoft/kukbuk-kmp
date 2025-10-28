package net.shamansoft.recipe.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a complete recipe with all its components.
 * This is the root model class for recipe data.
 *
 * @property schemaVersion Semantic version of the schema (e.g., "1.0.0")
 * @property recipeVersion Semantic version of the recipe (e.g., "1.0.0")
 * @property metadata Recipe metadata (title, author, source, etc.)
 * @property description Markdown-formatted description
 * @property ingredients List of ingredients (required, at least one)
 * @property equipment List of required equipment
 * @property instructions List of instruction steps (required, at least one)
 * @property nutrition Nutritional information
 * @property notes Markdown-formatted notes, tips and tricks
 * @property storage Storage instructions
 */
@Serializable
data class Recipe(
    @SerialName("schema_version")
    val schemaVersion: String,

    @SerialName("recipe_version")
    val recipeVersion: String,

    @SerialName("metadata")
    val metadata: RecipeMetadata,

    @SerialName("description")
    val description: String = "",

    @SerialName("ingredients")
    val ingredients: List<Ingredient>,

    @SerialName("equipment")
    val equipment: List<String> = emptyList(),

    @SerialName("instructions")
    val instructions: List<Instruction>,

    @SerialName("nutrition")
    val nutrition: Nutrition? = null,

    @SerialName("notes")
    val notes: String = "",

    @SerialName("storage")
    val storage: Storage? = null
)
