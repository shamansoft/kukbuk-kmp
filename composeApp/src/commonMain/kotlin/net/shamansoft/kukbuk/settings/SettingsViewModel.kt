package net.shamansoft.kukbuk.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.shamansoft.kukbuk.cache.RecipeCache
import net.shamansoft.kukbuk.util.Logger

/**
 * ViewModel for Settings screen
 * Manages cache operations and settings state
 */
class SettingsViewModel(
    private val recipeCache: RecipeCache
) : ViewModel() {

    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

    init {
        loadCacheInfo()
    }

    /**
     * Load cache information (count, size, etc.)
     */
    private fun loadCacheInfo() {
        viewModelScope.launch {
            try {
                val count = recipeCache.getCachedRecipeCount()
                _settingsState.value = _settingsState.value.copy(
                    cachedRecipeCount = count,
                    isLoading = false
                )
                Logger.d("SettingsVM", "Loaded cache info: $count recipes cached")
            } catch (e: Exception) {
                Logger.e("SettingsVM", "Failed to load cache info: ${e.message}")
                _settingsState.value = _settingsState.value.copy(
                    isLoading = false,
                    error = "Failed to load cache information"
                )
            }
        }
    }

    /**
     * Clear all cached recipes
     */
    fun clearCache() {
        viewModelScope.launch {
            try {
                _settingsState.value = _settingsState.value.copy(isClearing = true)
                Logger.d("SettingsVM", "Clearing recipe cache...")

                recipeCache.clearCache()

                _settingsState.value = _settingsState.value.copy(
                    cachedRecipeCount = 0,
                    isClearing = false,
                    showClearSuccess = true
                )
                Logger.d("SettingsVM", "Cache cleared successfully")
            } catch (e: Exception) {
                Logger.e("SettingsVM", "Failed to clear cache: ${e.message}")
                _settingsState.value = _settingsState.value.copy(
                    isClearing = false,
                    error = "Failed to clear cache: ${e.message}"
                )
            }
        }
    }

    /**
     * Dismiss success message
     */
    fun dismissSuccessMessage() {
        _settingsState.value = _settingsState.value.copy(showClearSuccess = false)
    }

    /**
     * Dismiss error message
     */
    fun dismissError() {
        _settingsState.value = _settingsState.value.copy(error = null)
    }

    /**
     * Refresh cache information
     */
    fun refresh() {
        _settingsState.value = _settingsState.value.copy(isLoading = true)
        loadCacheInfo()
    }
}

/**
 * State for Settings screen
 */
data class SettingsState(
    val cachedRecipeCount: Int = 0,
    val isLoading: Boolean = true,
    val isClearing: Boolean = false,
    val showClearSuccess: Boolean = false,
    val error: String? = null
)
