package net.shamansoft.kukbuk.recipe

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.shamansoft.kukbuk.util.Logger

class RecipeRepository(
    private val dataSource: RecipeDataSource,
    private val yamlParser: YamlRecipeParser = YamlRecipeParser()
) {

    private val _recipeListState = MutableStateFlow<RecipeListState>(RecipeListState.Loading)
    val recipeListState: StateFlow<RecipeListState> = _recipeListState.asStateFlow()

    private val _recipeCache = mutableMapOf<String, Recipe>()
    private var _metadataCache: List<RecipeMetadata>? = null

    suspend fun loadRecipes(forceRefresh: Boolean = false) {
        // If we have cached data and not forcing refresh, use cache
        if (!forceRefresh && _metadataCache != null) {
            _recipeListState.value = RecipeListState.Success(_metadataCache!!)
            Logger.d("RecipeRepo", "Using cached recipe metadata (${_metadataCache!!.size} recipes)")
            return
        }

        Logger.d("RecipeRepo", "Loading recipes from data source (forceRefresh=$forceRefresh)")
        _recipeListState.value = RecipeListState.Loading

        when (val result = dataSource.listRecipeFiles()) {
            is DataSourceResult.Success -> {
                val files = result.data

                if (files.isEmpty()) {
                    _recipeListState.value = RecipeListState.Empty
                } else {
                    val recipes = mutableListOf<RecipeMetadata>()

                    files.forEach { file ->
                        try {
                            val content = dataSource.getFileContent(file.id)
                            if (content is DataSourceResult.Success) {
                                val metadata = yamlParser.parseRecipeMetadata(
                                    yamlContent = content.data,
                                    fileId = file.id,
                                    lastModified = file.modifiedTime,
                                    fileName = file.name
                                )
                                if (metadata != null) {
                                    recipes.add(metadata)
                                }
                            }
                        } catch (e: Exception) {
                            // Continue processing other files
                            Logger.e(
                                "RecipeRepo",
                                "Error processing file ${file.name}: ${e.message}"
                            )
                        }
                    }

                    if (recipes.isNotEmpty()) {
                        val sorted = recipes.sortedByDescending { it.lastModified }
                        _metadataCache = sorted // Cache the metadata
                        _recipeListState.value = RecipeListState.Success(sorted)
                        Logger.d("RecipeRepo", "Cached ${sorted.size} recipe metadata entries")
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

    suspend fun getRecipe(recipeId: String): RecipeResult<Recipe> {
        Logger.d("RecipeRepo", "getRecipe called with ID: $recipeId")

        // Check cache first
        _recipeCache[recipeId]?.let { cachedRecipe ->
            Logger.d("RecipeRepo", "Recipe found in cache: ${cachedRecipe.title}")
            return RecipeResult.Success(cachedRecipe)
        }

        Logger.d("RecipeRepo", "Recipe not in cache, loading from data source")
        return when (val result = dataSource.getFileContent(recipeId)) {
            is DataSourceResult.Success -> {
                Logger.d(
                    "RecipeRepo",
                    "Loaded recipe content, parsing YAML (${result.data.length} chars)"
                )
                val recipe = yamlParser.parseRecipeYaml(
                    yamlContent = result.data,
                    fileId = recipeId,
                    lastModified = "" // We'll get this from the file metadata
                )
                if (recipe != null) {
                    Logger.d("RecipeRepo", "Successfully parsed recipe: ${recipe.title}")
                    _recipeCache[recipeId] = recipe
                    RecipeResult.Success(recipe)
                } else {
                    Logger.e("RecipeRepo", "Failed to parse recipe YAML")
                    RecipeResult.Error("Failed to parse recipe data")
                }
            }

            is DataSourceResult.Error -> {
                Logger.e("RecipeRepo", "Error loading recipe: ${result.message}")
                RecipeResult.Error(result.message)
            }

            is DataSourceResult.Loading -> {
                RecipeResult.Loading
            }
        }
    }

    suspend fun refreshRecipes() {
        loadRecipes(forceRefresh = true)
    }

    suspend fun refreshRecipe(recipeId: String): RecipeResult<Recipe> {
        Logger.d("RecipeRepo", "Force refreshing recipe: $recipeId")

        // Remove from cache to force reload
        _recipeCache.remove(recipeId)

        // Load fresh from data source
        return when (val result = dataSource.getFileContent(recipeId)) {
            is DataSourceResult.Success -> {
                Logger.d("RecipeRepo", "Loaded fresh recipe content (${result.data.length} chars)")
                val recipe = yamlParser.parseRecipeYaml(
                    yamlContent = result.data,
                    fileId = recipeId,
                    lastModified = ""
                )
                if (recipe != null) {
                    Logger.d("RecipeRepo", "Successfully parsed refreshed recipe: ${recipe.title}")
                    _recipeCache[recipeId] = recipe
                    RecipeResult.Success(recipe)
                } else {
                    Logger.e("RecipeRepo", "Failed to parse refreshed recipe YAML")
                    RecipeResult.Error("Failed to parse recipe data")
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

    fun searchRecipes(query: String): RecipeResult<List<RecipeMetadata>> {
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
    }

    fun getCachedRecipesCount(): Int {
        return when (val state = _recipeListState.value) {
            is RecipeListState.Success -> state.recipes.size
            else -> 0
        }
    }
}
