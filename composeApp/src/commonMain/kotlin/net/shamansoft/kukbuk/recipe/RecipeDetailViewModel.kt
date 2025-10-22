package net.shamansoft.kukbuk.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.shamansoft.kukbuk.util.Logger

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
     */
    data class Success(val recipe: Recipe) : RecipeDetailState()

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
                    Logger.d("RecipeDetailVM", "Recipe loaded successfully: ${result.data.title}")
                    _recipeDetailState.value = RecipeDetailState.Success(result.data)
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
                    Logger.d("RecipeDetailVM", "Recipe refreshed successfully: ${result.data.title}")
                    _recipeDetailState.value = RecipeDetailState.Success(result.data)
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
