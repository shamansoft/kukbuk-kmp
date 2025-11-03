package net.shamansoft.kukbuk.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.shamansoft.kukbuk.auth.AuthenticationRepository
import net.shamansoft.kukbuk.cache.RecipeCache
import net.shamansoft.kukbuk.util.Logger

/**
 * ViewModel for Settings screen
 * Manages cache operations, logout, and settings state
 */
class SettingsViewModel(
    private val recipeCache: RecipeCache,
    private val authRepository: AuthenticationRepository
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

    /**
     * Log out the user
     * Clears all cached data and revokes authentication tokens
     */
    fun logout() {
        viewModelScope.launch {
            try {
                _settingsState.value = _settingsState.value.copy(isLoggingOut = true)
                Logger.d("SettingsVM", "Logging out user...")

                // Clear cached recipes first
                try {
                    recipeCache.clearCache()
                    Logger.d("SettingsVM", "Cache cleared during logout")
                } catch (e: Exception) {
                    Logger.e("SettingsVM", "Failed to clear cache during logout: ${e.message}")
                    // Continue with logout even if cache clear fails
                }

                // Sign out from authentication
                val result = authRepository.signOut()

                if (result.isSuccess) {
                    Logger.d("SettingsVM", "Logout successful")
                    _settingsState.value = _settingsState.value.copy(
                        isLoggingOut = false,
                        logoutSuccess = true
                    )
                } else {
                    Logger.e("SettingsVM", "Logout failed: ${result.exceptionOrNull()?.message}")
                    _settingsState.value = _settingsState.value.copy(
                        isLoggingOut = false,
                        error = "Logout failed. Please try again."
                    )
                }
            } catch (e: Exception) {
                Logger.e("SettingsVM", "Exception during logout: ${e.message}")
                _settingsState.value = _settingsState.value.copy(
                    isLoggingOut = false,
                    error = "Logout failed: ${e.message}"
                )
            }
        }
    }
}

/**
 * State for Settings screen
 */
data class SettingsState(
    val cachedRecipeCount: Int = 0,
    val isLoading: Boolean = true,
    val isClearing: Boolean = false,
    val isLoggingOut: Boolean = false,
    val showClearSuccess: Boolean = false,
    val logoutSuccess: Boolean = false,
    val error: String? = null
)
