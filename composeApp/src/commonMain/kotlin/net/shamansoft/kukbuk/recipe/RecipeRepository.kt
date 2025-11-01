package net.shamansoft.kukbuk.recipe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.shamansoft.kukbuk.cache.RecipeCache
import net.shamansoft.kukbuk.util.Logger
import net.shamansoft.recipe.model.Recipe
import net.shamansoft.recipe.parser.RecipeYaml
import net.shamansoft.recipe.parser.RecipeParseException

class RecipeRepository(
    private val dataSource: RecipeDataSource,
    private val recipeCache: RecipeCache
) {

    private val _recipeListState = MutableStateFlow<RecipeListState>(RecipeListState.Loading)
    val recipeListState: StateFlow<RecipeListState> = _recipeListState.asStateFlow()

    // In-memory cache for session performance (warm cache)
    private val _recipeCache = mutableMapOf<String, Recipe>()
    private var _metadataCache: List<RecipeListItem>? = null

    // Pagination state
    private var nextPageToken: String? = null
    private var hasMorePages: Boolean = true

    companion object {
        private const val MAX_PERSISTENT_CACHE_SIZE = 100
        private const val PARALLEL_DOWNLOAD_BATCH_SIZE = 5 // Download 5 files concurrently
        private const val PAGINATION_PAGE_SIZE = 5 // Number of files to load per page
    }

    suspend fun loadRecipes(forceRefresh: Boolean = false, showCachedWhileRefreshing: Boolean = false) {
        // Optimistic UI: Show cached data immediately if available
        if (_metadataCache != null && !forceRefresh) {
            _recipeListState.value = RecipeListState.Success(_metadataCache!!)
            Logger.d("RecipeRepo", "Using cached recipe metadata (${_metadataCache!!.size} recipes)")
            return
        }

        // If showing cached data during refresh, keep it visible
        if (!showCachedWhileRefreshing || _metadataCache == null) {
            _recipeListState.value = RecipeListState.Loading
        }

        Logger.d("RecipeRepo", "Loading recipes from data source (forceRefresh=$forceRefresh, showCached=$showCachedWhileRefreshing)")

        when (val result = dataSource.listRecipeFiles()) {
            is DataSourceResult.Success -> {
                val files = result.data

                if (files.isEmpty()) {
                    _recipeListState.value = RecipeListState.Empty
                } else {
                    // Process files in parallel batches for better performance
                    val recipes = withContext(Dispatchers.IO) {
                        files.chunked(PARALLEL_DOWNLOAD_BATCH_SIZE).flatMap { batch ->
                            // Process each batch in parallel
                            coroutineScope {
                                batch.map { file ->
                                    async {
                                        try {
                                            val content = dataSource.getFileContent(file.id)
                                            if (content is DataSourceResult.Success) {
                                                try {
                                                    val recipe = RecipeYaml.parse(content.data)
                                                    RecipeListItem.fromRecipe(
                                                        recipe = recipe,
                                                        fileId = file.id,
                                                        lastModified = file.modifiedTime
                                                    )
                                                } catch (e: RecipeParseException) {
                                                    Logger.e(
                                                        "RecipeRepo",
                                                        "Failed to parse ${file.name}: ${e.message}"
                                                    )
                                                    null
                                                }
                                            } else {
                                                null
                                            }
                                        } catch (e: Exception) {
                                            // Continue processing other files
                                            Logger.e(
                                                "RecipeRepo",
                                                "Error processing file ${file.name}: ${e.message}"
                                            )
                                            null
                                        }
                                    }
                                }.mapNotNull { it.await() } // Wait for all in batch and filter nulls
                            }
                        }
                    }

                    if (recipes.isNotEmpty()) {
                        val sorted = recipes.sortedByDescending { it.lastModified }
                        _metadataCache = sorted // Cache the metadata
                        _recipeListState.value = RecipeListState.Success(sorted)
                        Logger.d("RecipeRepo", "Cached ${sorted.size} recipe metadata entries (loaded with ${files.size / PARALLEL_DOWNLOAD_BATCH_SIZE + 1} parallel batches)")
                    } else {
                        _recipeListState.value =
                            RecipeListState.Error("No valid recipes found")
                    }
                }
            }

            is DataSourceResult.Error -> {
                _recipeListState.value = RecipeListState.Error(result.message)
            }

            is DataSourceResult.Loading -> {
                // Already in loading state
            }
        }
    }

    /**
     * Load recipes progressively, emitting each recipe as it's downloaded and parsed.
     * This provides better user experience by showing recipes as they arrive instead of
     * waiting for all recipes to load.
     *
     * @return Flow of RecipeLoadEvent that emits as recipes are loaded
     */
    fun loadRecipesProgressively(): Flow<RecipeLoadEvent> = flow {
        Logger.d("RecipeRepo", "Starting progressive recipe loading")
        emit(RecipeLoadEvent.LoadingStarted)

        // First, emit cached recipes if available
        _metadataCache?.let { cached ->
            Logger.d("RecipeRepo", "Emitting ${cached.size} cached recipes progressively")
            cached.forEach { recipe ->
                emit(RecipeLoadEvent.RecipeLoaded(recipe))
            }
        }

        try {
            // Fetch file list
            when (val result = dataSource.listRecipeFiles()) {
                is DataSourceResult.Success -> {
                    val files = result.data
                    Logger.d("RecipeRepo", "Processing ${files.size} files progressively")

                    if (files.isEmpty()) {
                        emit(RecipeLoadEvent.LoadingComplete(0))
                        return@flow
                    }

                    val loadedRecipes = mutableListOf<RecipeListItem>()

                    // Process files in parallel batches, emitting each recipe as it's ready
                    files.chunked(PARALLEL_DOWNLOAD_BATCH_SIZE).forEach { batch ->
                        // Download batch in parallel
                        val batchRecipes = coroutineScope {
                            batch.map { file ->
                                async(Dispatchers.IO) {
                                    try {
                                        val content = dataSource.getFileContent(file.id)
                                        if (content is DataSourceResult.Success) {
                                            try {
                                                val recipe = RecipeYaml.parse(content.data)
                                                RecipeListItem.fromRecipe(
                                                    recipe = recipe,
                                                    fileId = file.id,
                                                    lastModified = file.modifiedTime
                                                )
                                            } catch (e: RecipeParseException) {
                                                Logger.e(
                                                    "RecipeRepo",
                                                    "Failed to parse ${file.name}: ${e.message}"
                                                )
                                                null
                                            }
                                        } else {
                                            null
                                        }
                                    } catch (e: Exception) {
                                        Logger.e(
                                            "RecipeRepo",
                                            "Error processing file ${file.name}: ${e.message}"
                                        )
                                        null
                                    }
                                }
                            }.mapNotNull { it.await() }
                        }

                        // Emit each recipe from the batch immediately
                        batchRecipes.forEach { recipe ->
                            loadedRecipes.add(recipe)
                            emit(RecipeLoadEvent.RecipeLoaded(recipe))
                            Logger.d("RecipeRepo", "Emitted recipe: ${recipe.title}")
                        }
                    }

                    // Update cache
                    if (loadedRecipes.isNotEmpty()) {
                        val sorted = loadedRecipes.sortedByDescending { it.lastModified }
                        _metadataCache = sorted
                        _recipeListState.value = RecipeListState.Success(sorted)
                        Logger.d("RecipeRepo", "Progressive loading complete: ${loadedRecipes.size} recipes")
                    }

                    emit(RecipeLoadEvent.LoadingComplete(loadedRecipes.size))
                }

                is DataSourceResult.Error -> {
                    Logger.e("RecipeRepo", "Error loading recipes: ${result.message}")
                    _recipeListState.value = RecipeListState.Error(result.message)
                    emit(RecipeLoadEvent.Error(result.message))
                }

                is DataSourceResult.Loading -> {
                    // Already emitted LoadingStarted
                }
            }
        } catch (e: Exception) {
            Logger.e("RecipeRepo", "Exception during progressive loading: ${e.message}")
            val errorMessage = e.message ?: "Unknown error"
            _recipeListState.value = RecipeListState.Error(errorMessage)
            emit(RecipeLoadEvent.Error(errorMessage))
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Load one page of recipes progressively with pagination support.
     * This is the main method for Phase 3 - loads only a small number of recipes at a time.
     *
     * @param isInitialLoad If true, resets pagination state and may emit cached recipes
     * @return Flow of RecipeLoadEvent for progressive UI updates
     */
    fun loadRecipesPageProgressively(isInitialLoad: Boolean = false): Flow<RecipeLoadEvent> = flow {
        Logger.d("RecipeRepo", "Starting paginated progressive recipe loading (initial=$isInitialLoad)")

        if (isInitialLoad) {
            // Reset pagination state
            nextPageToken = null
            hasMorePages = true

            // Emit cached recipes if available
            _metadataCache?.let { cached ->
                Logger.d("RecipeRepo", "Emitting ${cached.size} cached recipes")
                cached.forEach { recipe ->
                    emit(RecipeLoadEvent.RecipeLoaded(recipe))
                }
            }
        }

        if (!hasMorePages) {
            Logger.d("RecipeRepo", "No more pages to load")
            emit(RecipeLoadEvent.LoadingComplete(0))
            return@flow
        }

        emit(RecipeLoadEvent.LoadingStarted)

        try {
            // Fetch one page of files
            when (val result = dataSource.listRecipeFilesPaginated(
                pageSize = PAGINATION_PAGE_SIZE,
                pageToken = nextPageToken
            )) {
                is DataSourceResult.Success -> {
                    val page = result.data
                    Logger.d("RecipeRepo", "Received page with ${page.files.size} files (hasMore=${page.hasMore})")

                    // Update pagination state
                    nextPageToken = page.nextPageToken
                    hasMorePages = page.hasMore

                    if (page.files.isEmpty()) {
                        emit(RecipeLoadEvent.LoadingComplete(0))
                        return@flow
                    }

                    // Process files in parallel batch
                    val loadedRecipes = withContext(Dispatchers.IO) {
                        coroutineScope {
                            page.files.map { file ->
                                async {
                                    try {
                                        val content = dataSource.getFileContent(file.id)
                                        if (content is DataSourceResult.Success) {
                                            try {
                                                val recipe = RecipeYaml.parse(content.data)
                                                RecipeListItem.fromRecipe(
                                                    recipe = recipe,
                                                    fileId = file.id,
                                                    lastModified = file.modifiedTime
                                                )
                                            } catch (e: RecipeParseException) {
                                                Logger.e("RecipeRepo", "Failed to parse ${file.name}: ${e.message}")
                                                null
                                            }
                                        } else {
                                            null
                                        }
                                    } catch (e: Exception) {
                                        Logger.e("RecipeRepo", "Error processing file ${file.name}: ${e.message}")
                                        null
                                    }
                                }
                            }.mapNotNull { it.await() }
                        }
                    }

                    // Emit each recipe immediately
                    loadedRecipes.forEach { recipe ->
                        emit(RecipeLoadEvent.RecipeLoaded(recipe))
                        Logger.d("RecipeRepo", "Emitted recipe: ${recipe.title}")
                    }

                    emit(RecipeLoadEvent.LoadingComplete(loadedRecipes.size))
                }

                is DataSourceResult.Error -> {
                    Logger.e("RecipeRepo", "Error loading recipe page: ${result.message}")
                    _recipeListState.value = RecipeListState.Error(result.message)
                    emit(RecipeLoadEvent.Error(result.message))
                }

                is DataSourceResult.Loading -> {
                    // Already emitted LoadingStarted
                }
            }
        } catch (e: Exception) {
            Logger.e("RecipeRepo", "Exception during paginated progressive loading: ${e.message}")
            val errorMessage = e.message ?: "Unknown error"
            _recipeListState.value = RecipeListState.Error(errorMessage)
            emit(RecipeLoadEvent.Error(errorMessage))
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Check if there are more pages to load
     */
    fun hasMoreRecipes(): Boolean = hasMorePages

    /**
     * Reset pagination state (useful for refresh)
     */
    fun resetPagination() {
        nextPageToken = null
        hasMorePages = true
        Logger.d("RecipeRepo", "Pagination state reset")
    }

    suspend fun getRecipe(recipeId: String): RecipeResult<Recipe> {
        Logger.d("RecipeRepo", "getRecipe called with ID: $recipeId")

        // Check in-memory cache first (fastest)
        _recipeCache[recipeId]?.let { cachedRecipe ->
            Logger.d("RecipeRepo", "Recipe found in memory cache: ${cachedRecipe.metadata.title}")
            return RecipeResult.Success(cachedRecipe, isOffline = false)
        }

        // Try to fetch from network
        Logger.d("RecipeRepo", "Recipe not in memory cache, loading from data source")
        return when (val result = dataSource.getFileContent(recipeId)) {
            is DataSourceResult.Success -> {
                val yamlContent = result.data
                Logger.d(
                    "RecipeRepo",
                    "Loaded recipe content, parsing YAML (${yamlContent.length} chars)"
                )
                try {
                    val recipe = RecipeYaml.parse(yamlContent)
                    Logger.d("RecipeRepo", "Successfully parsed recipe: ${recipe.metadata.title}")

                    // Cache in memory for session
                    _recipeCache[recipeId] = recipe

                    // Cache persistently for offline access
                    try {
                        recipeCache.cacheRecipe(recipeId, yamlContent, recipe)
                        recipeCache.enforceCacheLimit(MAX_PERSISTENT_CACHE_SIZE)
                        Logger.d("RecipeRepo", "Cached recipe persistently for offline access")
                    } catch (e: Exception) {
                        Logger.e("RecipeRepo", "Failed to cache recipe persistently: ${e.message}")
                        // Continue even if caching fails - user still gets the recipe
                    }

                    RecipeResult.Success(recipe, isOffline = false)
                } catch (e: RecipeParseException) {
                    Logger.e("RecipeRepo", "Failed to parse recipe YAML: ${e.message}")
                    RecipeResult.Error("Failed to parse recipe data: ${e.message}")
                } catch (e: Exception) {
                    Logger.e("RecipeRepo", "Unexpected error parsing recipe: ${e.message}")
                    RecipeResult.Error("Failed to parse recipe data: ${e.message}")
                }
            }

            is DataSourceResult.Error -> {
                Logger.e("RecipeRepo", "Network error loading recipe: ${result.message}")

                // Try persistent cache as fallback
                try {
                    val cached = recipeCache.getCachedRecipe(recipeId)
                    if (cached != null) {
                        Logger.d("RecipeRepo", "Using cached recipe (offline): ${cached.recipe.metadata.title}")
                        _recipeCache[recipeId] = cached.recipe // Warm up memory cache
                        RecipeResult.Success(cached.recipe, isOffline = true)
                    } else {
                        Logger.d("RecipeRepo", "Recipe not in persistent cache")
                        RecipeResult.Error(result.message)
                    }
                } catch (e: Exception) {
                    Logger.e("RecipeRepo", "Failed to load from persistent cache: ${e.message}")
                    RecipeResult.Error(result.message)
                }
            }

            is DataSourceResult.Loading -> {
                RecipeResult.Loading
            }
        }
    }

    suspend fun refreshRecipes() {
        // Optimistic UI: Keep cached recipes visible while refreshing
        loadRecipes(forceRefresh = true, showCachedWhileRefreshing = true)
    }

    suspend fun refreshRecipe(recipeId: String): RecipeResult<Recipe> {
        Logger.d("RecipeRepo", "Force refreshing recipe: $recipeId")

        // Remove from cache to force reload
        _recipeCache.remove(recipeId)

        // Load fresh from data source
        return when (val result = dataSource.getFileContent(recipeId)) {
            is DataSourceResult.Success -> {
                Logger.d("RecipeRepo", "Loaded fresh recipe content (${result.data.length} chars)")
                try {
                    val recipe = RecipeYaml.parse(result.data)
                    Logger.d("RecipeRepo", "Successfully parsed refreshed recipe: ${recipe.metadata.title}")
                    _recipeCache[recipeId] = recipe
                    RecipeResult.Success(recipe)
                } catch (e: RecipeParseException) {
                    Logger.e("RecipeRepo", "Failed to parse refreshed recipe YAML: ${e.message}")
                    RecipeResult.Error("Failed to parse recipe data: ${e.message}")
                } catch (e: Exception) {
                    Logger.e("RecipeRepo", "Unexpected error parsing refreshed recipe: ${e.message}")
                    RecipeResult.Error("Failed to parse recipe data: ${e.message}")
                }
            }
            is DataSourceResult.Error -> {
                Logger.e("RecipeRepo", "Error refreshing recipe: ${result.message}")
                RecipeResult.Error(result.message)
            }
            is DataSourceResult.Loading -> {
                RecipeResult.Loading
            }
        }
    }

    fun searchRecipes(query: String): RecipeResult<List<RecipeListItem>> {
        return when (val currentState = _recipeListState.value) {
            is RecipeListState.Success -> {
                val filteredRecipes = currentState.recipes.filter { recipe ->
                    recipe.title.contains(query, ignoreCase = true) ||
                            recipe.author?.contains(query, ignoreCase = true) == true ||
                            recipe.description?.contains(query, ignoreCase = true) == true
                }
                RecipeResult.Success(filteredRecipes)
            }

            is RecipeListState.Error -> {
                RecipeResult.Error(currentState.message)
            }

            else -> {
                RecipeResult.Error("Recipes not loaded yet")
            }
        }
    }

    fun clearCache() {
        _recipeCache.clear()
        _metadataCache = null
        Logger.d("RecipeRepo", "Cleared in-memory caches (recipe details and metadata)")
    }

    fun getCachedRecipesCount(): Int {
        return when (val state = _recipeListState.value) {
            is RecipeListState.Success -> state.recipes.size
            else -> 0
        }
    }
}
