package net.shamansoft.kukbuk.recipe

import kotlinx.serialization.Serializable

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
    val ingredients: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
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