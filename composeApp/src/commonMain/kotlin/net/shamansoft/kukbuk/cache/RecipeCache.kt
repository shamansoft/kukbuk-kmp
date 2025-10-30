package net.shamansoft.kukbuk.cache

import kotlinx.coroutines.flow.Flow
import net.shamansoft.recipe.model.Recipe

/**
 * Manages local caching of recipes for offline access.
 *
 * Implementations should:
 * - Cache recipes as they are viewed
 * - Enforce size limits (default: 100 recipes)
 * - Provide efficient queries for recent recipes
 * - Handle cache cleanup and expiration
 */
interface RecipeCache {
    /**
     * Cache a recipe for offline access.
     * Updates lastViewed timestamp automatically.
     *
     * @param recipeId Unique identifier for the recipe
     * @param recipeYaml Raw YAML string (from Google Drive)
     * @param recipe Parsed recipe data (for extracting metadata like title/author)
     */
    suspend fun cacheRecipe(recipeId: String, recipeYaml: String, recipe: Recipe)

    /**
     * Get a cached recipe by ID.
     *
     * @param recipeId Unique identifier for the recipe
     * @return Cached recipe data with metadata, or null if not cached
     */
    suspend fun getCachedRecipe(recipeId: String): CachedRecipeData?

    /**
     * Get the N most recently viewed recipes.
     *
     * @param limit Maximum number of recipes to return (default: 100)
     * @return List of cached recipes ordered by lastViewed (newest first)
     */
    suspend fun getMostRecentRecipes(limit: Int = 100): List<CachedRecipeData>

    /**
     * Check if a recipe exists in the cache.
     *
     * @param recipeId Unique identifier for the recipe
     * @return true if recipe is cached, false otherwise
     */
    suspend fun isRecipeCached(recipeId: String): Boolean

    /**
     * Update the lastViewed timestamp for a recipe.
     * Used when user views an already-cached recipe.
     *
     * @param recipeId Unique identifier for the recipe
     */
    suspend fun markRecipeAsViewed(recipeId: String)

    /**
     * Clear all cached recipes.
     * Use with caution - this removes all offline access.
     */
    suspend fun clearCache()

    /**
     * Get count of cached recipes.
     *
     * @return Total number of recipes in cache
     */
    suspend fun getCachedRecipeCount(): Int

    /**
     * Enforce cache size limit by removing oldest entries.
     * Should be called after caching new recipes.
     *
     * @param maxCount Maximum number of recipes to keep in cache (default: 100)
     */
    suspend fun enforceCacheLimit(maxCount: Int = 100)

    /**
     * Delete recipes that haven't been synced in a while.
     * Useful for cache maintenance to remove stale data.
     *
     * @param maxAgeMillis Maximum age in milliseconds (recipes older than this are deleted)
     */
    suspend fun deleteExpiredRecipes(maxAgeMillis: Long)

    /**
     * Observe cache size changes.
     * Useful for UI indicators showing cache status.
     *
     * @return Flow emitting cache size whenever it changes
     */
    fun observeCacheSize(): Flow<Int>

    /**
     * Get all cached recipe IDs.
     * Useful for debugging or clearing specific entries.
     *
     * @return List of all cached recipe IDs
     */
    suspend fun getAllCachedRecipeIds(): List<String>
}

/**
 * Represents a cached recipe with metadata.
 *
 * @property recipe Complete recipe data
 * @property lastViewed Timestamp when recipe was last viewed (milliseconds since epoch)
 * @property lastSynced Timestamp when recipe was last synced from server (milliseconds since epoch)
 * @property isFavorite Whether user has marked this recipe as favorite (future feature)
 */
data class CachedRecipeData(
    val recipe: Recipe,
    val lastViewed: Long,
    val lastSynced: Long,
    val isFavorite: Boolean = false
)
