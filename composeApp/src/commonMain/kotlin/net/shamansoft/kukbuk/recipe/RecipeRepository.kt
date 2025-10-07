package net.shamansoft.kukbuk.recipe

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.shamansoft.kukbuk.drive.GoogleDriveService
import net.shamansoft.kukbuk.drive.DriveResult

class RecipeRepository(
    private val driveService: GoogleDriveService,
    private val yamlParser: YamlRecipeParser = YamlRecipeParser()
) {
    
    private val _recipeListState = MutableStateFlow<RecipeListState>(RecipeListState.Loading)
    val recipeListState: StateFlow<RecipeListState> = _recipeListState.asStateFlow()
    
    private val _recipeCache = mutableMapOf<String, Recipe>()
    
    suspend fun loadRecipes(forceRefresh: Boolean = false) {
        if (!forceRefresh && _recipeListState.value is RecipeListState.Success) {
            return // Don't reload if we already have data and not forcing refresh
        }
        
        _recipeListState.value = RecipeListState.Loading
        
        when (val result = driveService.listFilesInKukbukFolder()) {
            is DriveResult.Success -> {
                val files = result.data
                if (files.isEmpty()) {
                    _recipeListState.value = RecipeListState.Empty
                } else {
                    val recipes = mutableListOf<RecipeMetadata>()
                    
                    // Process files in parallel for better performance
                    files.forEach { file ->
                        try {
                            val content = driveService.downloadFileContent(file.id)
                            if (content is DriveResult.Success) {
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
                            // Log error but continue processing other files
                            println("Error processing file ${file.name}: ${e.message}")
                        }
                    }
                    
                    if (recipes.isNotEmpty()) {
                        _recipeListState.value = RecipeListState.Success(recipes.sortedByDescending { it.lastModified })
                    } else {
                        _recipeListState.value = RecipeListState.Error("No valid recipes found in your Google Drive")
                    }
                }
            }
            is DriveResult.Error -> {
                _recipeListState.value = RecipeListState.Error(result.message)
            }
            is DriveResult.Loading -> {
                // Already in loading state
            }
        }
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