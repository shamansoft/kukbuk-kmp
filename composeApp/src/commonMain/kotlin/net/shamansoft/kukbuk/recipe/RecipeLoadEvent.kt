package net.shamansoft.kukbuk.recipe

/**
 * Events emitted during progressive recipe loading.
 * Used to update UI as recipes are downloaded and parsed.
 */
sealed class RecipeLoadEvent {
    /**
     * A single recipe has been successfully loaded and parsed
     */
    data class RecipeLoaded(val recipe: RecipeListItem) : RecipeLoadEvent()

    /**
     * Loading has started
     */
    data object LoadingStarted : RecipeLoadEvent()

    /**
     * All recipes have been loaded successfully
     */
    data class LoadingComplete(val totalCount: Int) : RecipeLoadEvent()

    /**
     * An error occurred during loading
     */
    data class Error(val message: String) : RecipeLoadEvent()
}
