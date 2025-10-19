package net.shamansoft.kukbuk.navigation

/**
 * Represents the different screens in the application.
 * Used for state-based navigation without external dependencies.
 */
sealed class Screen {
    /**
     * Recipe list screen - shows all user's recipes
     */
    data object RecipeList : Screen()

    /**
     * Recipe detail screen - shows full recipe information
     * @param recipeId The Google Drive file ID of the recipe
     * @param recipeTitle The recipe title (for display while loading)
     */
    data class RecipeDetail(
        val recipeId: String,
        val recipeTitle: String
    ) : Screen()
}
