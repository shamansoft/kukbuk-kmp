package net.shamansoft.kukbuk.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.shamansoft.kukbuk.util.Logger
import net.shamansoft.recipe.model.Recipe

/**
 * Represents the state of the recipe detail screen
 */
sealed class RecipeDetailState {
    /**
     * Loading recipe data
     */
    data object Loading : RecipeDetailState()

    /**
     * Recipe loaded successfully
     * @param recipe The recipe data
     * @param isOffline True if loaded from cache (offline mode)
     */
    data class Success(
        val recipe: Recipe,
        val isOffline: Boolean = false
    ) : RecipeDetailState()

    /**
     * Error loading recipe
     */
    data class Error(val message: String) : RecipeDetailState()
}

/**
 * ViewModel for managing recipe detail screen state
 */
class RecipeDetailViewModel(
    private val recipeId: String,
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _recipeDetailState = MutableStateFlow<RecipeDetailState>(RecipeDetailState.Loading)
    val recipeDetailState: StateFlow<RecipeDetailState> = _recipeDetailState.asStateFlow()

    init {
        loadRecipe()
    }

    /**
     * Load the recipe from the repository
     */
    fun loadRecipe() {
        viewModelScope.launch {
            _recipeDetailState.value = RecipeDetailState.Loading
            Logger.d("RecipeDetailVM", "Loading recipe: $recipeId")

            when (val result = recipeRepository.getRecipe(recipeId)) {
                is RecipeResult.Success -> {
                    val offlineMsg = if (result.isOffline) " (offline)" else ""
                    Logger.d("RecipeDetailVM", "Recipe loaded successfully: ${result.data.metadata.title}$offlineMsg")
                    _recipeDetailState.value = RecipeDetailState.Success(
                        recipe = result.data,
                        isOffline = result.isOffline
                    )
                }
                is RecipeResult.Error -> {
                    Logger.e("RecipeDetailVM", "Error loading recipe: ${result.message}")
                    _recipeDetailState.value = RecipeDetailState.Error(
                        result.message
                    )
                }
                is RecipeResult.Loading -> {
                    // Already in loading state
                }
            }
        }
    }

    /**
     * Retry loading the recipe after an error
     */
    fun retry() {
        Logger.d("RecipeDetailVM", "Retrying recipe load")
        loadRecipe()
    }

    /**
     * Force refresh the recipe from Google Drive
     */
    fun refresh() {
        viewModelScope.launch {
            _recipeDetailState.value = RecipeDetailState.Loading
            Logger.d("RecipeDetailVM", "Force refreshing recipe: $recipeId")

            when (val result = recipeRepository.refreshRecipe(recipeId)) {
                is RecipeResult.Success -> {
                    val offlineMsg = if (result.isOffline) " (offline)" else ""
                    Logger.d("RecipeDetailVM", "Recipe refreshed successfully: ${result.data.metadata.title}$offlineMsg")
                    _recipeDetailState.value = RecipeDetailState.Success(
                        recipe = result.data,
                        isOffline = result.isOffline
                    )
                }
                is RecipeResult.Error -> {
                    Logger.e("RecipeDetailVM", "Error refreshing recipe: ${result.message}")
                    _recipeDetailState.value = RecipeDetailState.Error(result.message)
                }
                is RecipeResult.Loading -> {
                    // Already in loading state
                }
            }
        }
    }
}
