package net.shamansoft.kukbuk.recipe

import net.shamansoft.recipe.model.Recipe

/**
 * Simplified recipe model for list display.
 * Extracted from the full Recipe model for efficient list rendering.
 */
data class RecipeListItem(
    val id: String,
    val title: String,
    val author: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val categories: List<String> = emptyList(),
    val lastModified: String = ""
) {
    companion object {
        /**
         * Creates a RecipeListItem from a full Recipe model.
         * Used when parsing individual recipe files for metadata extraction.
         */
        fun fromRecipe(recipe: Recipe, fileId: String, lastModified: String): RecipeListItem {
            return RecipeListItem(
                id = fileId,
                title = recipe.metadata.title,
                author = recipe.metadata.author,
                description = recipe.description.takeIf { it.isNotBlank() },
                imageUrl = recipe.metadata.coverImage?.path,
                categories = recipe.metadata.category,
                lastModified = lastModified
            )
        }
    }
}

sealed class RecipeListState {
    data object Loading : RecipeListState()
    data class Success(val recipes: List<RecipeListItem>) : RecipeListState()
    data class Error(val message: String) : RecipeListState()
    data object Empty : RecipeListState()
}

sealed class RecipeResult<out T> {
    data class Success<T>(val data: T) : RecipeResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : RecipeResult<Nothing>()
    data object Loading : RecipeResult<Nothing>()
}
