package net.shamansoft.kukbuk.cache

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import net.shamansoft.kukbuk.db.RecipeDatabase
import net.shamansoft.kukbuk.util.Logger
import net.shamansoft.recipe.model.Recipe
import net.shamansoft.recipe.parser.RecipeYaml

/**
 * SQLDelight-based implementation of RecipeCache.
 *
 * Stores recipes as YAML strings (original format from Google Drive) in a local SQLite database.
 * Denormalizes key fields (title, author, imageUrl) for efficient querying without parsing.
 * Provides efficient querying, caching, and cleanup operations.
 *
 * Thread-safe: All operations use Dispatchers.IO for background execution.
 */
class SqlDelightRecipeCache(
    private val database: RecipeDatabase
) : RecipeCache {

    private val queries = database.cachedRecipeQueries

    override suspend fun cacheRecipe(recipeId: String, recipeYaml: String, recipe: Recipe) = withContext(Dispatchers.IO) {
        try {
            val now = Clock.System.now().toEpochMilliseconds()

            queries.insertOrReplaceRecipe(
                id = recipeId,
                recipeYaml = recipeYaml,
                title = recipe.metadata.title,
                author = recipe.metadata.author,
                imageUrl = recipe.metadata.coverImage?.path,
                lastViewed = now,
                lastSynced = now,
                isFavorite = 0
            )

            Logger.d("RecipeCache", "Cached recipe: ${recipe.metadata.title} (ID: $recipeId)")
        } catch (e: Exception) {
            Logger.e("RecipeCache", "Failed to cache recipe $recipeId: ${e.message}")
            throw e
        }
    }

    override suspend fun getCachedRecipe(recipeId: String): CachedRecipeData? = withContext(Dispatchers.IO) {
        try {
            queries.getCachedRecipe(recipeId)
                .executeAsOneOrNull()
                ?.toCachedRecipeData()
                ?.also {
                    Logger.d("RecipeCache", "Retrieved cached recipe: ${it.recipe.metadata.title}")
                }
        } catch (e: Exception) {
            Logger.e("RecipeCache", "Failed to get cached recipe $recipeId: ${e.message}")
            null
        }
    }

    override suspend fun getMostRecentRecipes(limit: Int): List<CachedRecipeData> = withContext(Dispatchers.IO) {
        try {
            queries.getMostRecentRecipes(limit.toLong())
                .executeAsList()
                .map { it.toCachedRecipeData() }
                .also {
                    Logger.d("RecipeCache", "Retrieved ${it.size} most recent cached recipes")
                }
        } catch (e: Exception) {
            Logger.e("RecipeCache", "Failed to get recent recipes: ${e.message}")
            emptyList()
        }
    }

    override suspend fun isRecipeCached(recipeId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            queries.recipeExistsInCache(recipeId).executeAsOne()
        } catch (e: Exception) {
            Logger.e("RecipeCache", "Failed to check if recipe cached $recipeId: ${e.message}")
            false
        }
    }

    override suspend fun markRecipeAsViewed(recipeId: String) = withContext(Dispatchers.IO) {
        try {
            queries.updateLastViewed(
                lastViewed = Clock.System.now().toEpochMilliseconds(),
                id = recipeId
            )
            Logger.d("RecipeCache", "Updated lastViewed for recipe: $recipeId")
        } catch (e: Exception) {
            Logger.e("RecipeCache", "Failed to update lastViewed for $recipeId: ${e.message}")
        }
    }

    override suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            queries.clearAllCache()
            Logger.d("RecipeCache", "Cleared all cached recipes")
        } catch (e: Exception) {
            Logger.e("RecipeCache", "Failed to clear cache: ${e.message}")
            throw e
        }
    }

    override suspend fun getCachedRecipeCount(): Int = withContext(Dispatchers.IO) {
        try {
            queries.getCachedRecipeCount().executeAsOne().toInt()
        } catch (e: Exception) {
            Logger.e("RecipeCache", "Failed to get cache count: ${e.message}")
            0
        }
    }

    override suspend fun enforceCacheLimit(maxCount: Int) = withContext(Dispatchers.IO) {
        try {
            val currentCount = getCachedRecipeCount()
            if (currentCount > maxCount) {
                val deleteCount = currentCount - maxCount
                queries.deleteOldestRecipes(deleteCount.toLong())
                Logger.d("RecipeCache", "Enforced cache limit: deleted $deleteCount oldest recipes (was $currentCount, now $maxCount)")
            }
        } catch (e: Exception) {
            Logger.e("RecipeCache", "Failed to enforce cache limit: ${e.message}")
        }
    }

    override suspend fun deleteExpiredRecipes(maxAgeMillis: Long) = withContext(Dispatchers.IO) {
        try {
            val expirationTime = Clock.System.now().toEpochMilliseconds() - maxAgeMillis
            val expiredRecipes = queries.getExpiredRecipes(expirationTime).executeAsList()
            queries.deleteExpiredRecipes(expirationTime)
            Logger.d("RecipeCache", "Deleted ${expiredRecipes.size} expired recipes older than ${maxAgeMillis}ms")
        } catch (e: Exception) {
            Logger.e("RecipeCache", "Failed to delete expired recipes: ${e.message}")
        }
    }

    override fun observeCacheSize(): Flow<Int> {
        return queries.getCachedRecipeCount()
            .asFlow()
            .mapToOne(Dispatchers.IO)
            .map { it.toInt() }
    }

    override suspend fun getAllCachedRecipeIds(): List<String> = withContext(Dispatchers.IO) {
        try {
            queries.getAllCachedRecipes()
                .executeAsList()
                .map { it.id }
        } catch (e: Exception) {
            Logger.e("RecipeCache", "Failed to get all cached recipe IDs: ${e.message}")
            emptyList()
        }
    }

    /**
     * Convert database entity to CachedRecipeData.
     * Parses YAML and creates metadata wrapper.
     */
    private fun net.shamansoft.kukbuk.db.CachedRecipe.toCachedRecipeData(): CachedRecipeData {
        val recipe = RecipeYaml.parse(recipeYaml)
        return CachedRecipeData(
            recipe = recipe,
            lastViewed = lastViewed,
            lastSynced = lastSynced,
            isFavorite = isFavorite != 0L
        )
    }
}
