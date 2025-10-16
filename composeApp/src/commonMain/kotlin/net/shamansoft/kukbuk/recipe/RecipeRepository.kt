package net.shamansoft.kukbuk.recipe

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.shamansoft.kukbuk.drive.GoogleDriveService
import net.shamansoft.kukbuk.drive.DriveResult

import net.shamansoft.kukbuk.util.Logger
class RecipeRepository(
    private val driveService: GoogleDriveService,
    private val yamlParser: YamlRecipeParser = YamlRecipeParser()
) {
    
    private val _recipeListState = MutableStateFlow<RecipeListState>(RecipeListState.Loading)
    val recipeListState: StateFlow<RecipeListState> = _recipeListState.asStateFlow()
    
    private val _recipeCache = mutableMapOf<String, Recipe>()
    
    suspend fun loadRecipes(forceRefresh: Boolean = false) {
        Logger.d("RecipeRepo", "loadRecipes() called, forceRefresh=$forceRefresh, currentState=${_recipeListState.value}")

        if (!forceRefresh && _recipeListState.value is RecipeListState.Success) {
            Logger.d("RecipeRepo", "Skipping load - already have data")
            return // Don't reload if we already have data and not forcing refresh
        }

        Logger.d("RecipeRepo", "Setting state to Loading")
        _recipeListState.value = RecipeListState.Loading

        Logger.d("RecipeRepo", "Calling driveService.listFilesInKukbukFolder()")
        when (val result = driveService.listFilesInKukbukFolder()) {
            is DriveResult.Success -> {
                val files = result.data
                Logger.d("RecipeRepo", "Successfully retrieved ${files.size} files from Drive")

                if (files.isEmpty()) {
                    Logger.d("RecipeRepo", "No files found, setting state to Empty")
                    _recipeListState.value = RecipeListState.Empty
                } else {
                    val recipes = mutableListOf<RecipeMetadata>()

                    // Process files in parallel for better performance
                    files.forEachIndexed { index, file ->
                        Logger.d("RecipeRepo", "Processing file ${index + 1}/${files.size}: ${file.name} (id=${file.id})")
                        try {
                            val content = driveService.downloadFileContent(file.id)
                            if (content is DriveResult.Success) {
                                Logger.d("RecipeRepo", "Downloaded content for ${file.name}, length=${content.data.length}")
                                val metadata = yamlParser.parseRecipeMetadata(
                                    yamlContent = content.data,
                                    fileId = file.id,
                                    lastModified = file.modifiedTime ?: "",
                                    fileName = file.name
                                )
                                if (metadata != null) {
                                    Logger.d("RecipeRepo", "Successfully parsed metadata: ${metadata.title}")
                                    recipes.add(metadata)
                                } else {
                                    Logger.d("RecipeRepo", "Failed to parse metadata for ${file.name}")
                                }
                            } else if (content is DriveResult.Error) {
                                Logger.d("RecipeRepo", "Failed to download ${file.name}: ${content.message}")
                            }
                        } catch (e: Exception) {
                            // Log error but continue processing other files
                            Logger.d("RecipeRepo", "Error processing file ${file.name}: ${e.message}")
                            e.printStackTrace()
                        }
                    }

                    Logger.d("RecipeRepo", "Total recipes parsed: ${recipes.size}")

                    if (recipes.isNotEmpty()) {
                        val sorted = recipes.sortedByDescending { it.lastModified }
                        Logger.d("RecipeRepo", "Setting state to Success with ${sorted.size} recipes")
                        _recipeListState.value = RecipeListState.Success(sorted)
                    } else {
                        Logger.d("RecipeRepo", "No valid recipes found, setting state to Error")
                        _recipeListState.value = RecipeListState.Error("No valid recipes found in your Google Drive")
                    }
                }
            }
            is DriveResult.Error -> {
                Logger.d("RecipeRepo", "Drive error: ${result.message}")
                _recipeListState.value = RecipeListState.Error(result.message)
            }
            is DriveResult.Loading -> {
                Logger.d("RecipeRepo", "Drive still loading (unexpected)")
                // Already in loading state
            }
        }

        Logger.d("RecipeRepo", "loadRecipes() finished, final state: ${_recipeListState.value}")
    }
    
    suspend fun getRecipe(recipeId: String): RecipeResult<Recipe> {
        // Check cache first
        _recipeCache[recipeId]?.let { cachedRecipe ->
            return RecipeResult.Success(cachedRecipe)
        }
        
        return when (val result = driveService.downloadFileContent(recipeId)) {
            is DriveResult.Success -> {
                val recipe = yamlParser.parseRecipeYaml(
                    yamlContent = result.data,
                    fileId = recipeId,
                    lastModified = "" // We'll get this from the file metadata
                )
                if (recipe != null) {
                    _recipeCache[recipeId] = recipe
                    RecipeResult.Success(recipe)
                } else {
                    RecipeResult.Error("Failed to parse recipe data")
                }
            }
            is DriveResult.Error -> {
                RecipeResult.Error(result.message)
            }
            is DriveResult.Loading -> {
                RecipeResult.Loading
            }
        }
    }
    
    suspend fun refreshRecipes() {
        loadRecipes(forceRefresh = true)
    }
    
    suspend fun searchRecipes(query: String): RecipeResult<List<RecipeMetadata>> {
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
