package net.shamansoft.kukbuk.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.shamansoft.kukbuk.util.Logger

class RecipeListViewModel(
    private val recipeRepository: RecipeRepository
) : ViewModel() {
    
    val recipeListState: StateFlow<RecipeListState> = recipeRepository.recipeListState
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<RecipeMetadata>>(emptyList())
    val searchResults: StateFlow<List<RecipeMetadata>> = _searchResults.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    init {
        Logger.d("RecipeListVM", "Initializing...")
        loadRecipes()
    }

    fun loadRecipes() {
        Logger.d("RecipeListVM", "loadRecipes() called")
        viewModelScope.launch {
            Logger.d("RecipeListVM", "Launching coroutine to load recipes")
            recipeRepository.loadRecipes()
            Logger.d("RecipeListVM", "loadRecipes() completed, state: ${recipeListState.value}")
        }
    }

    fun refreshRecipes() {
        Logger.d("RecipeListVM", "refreshRecipes() called")
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                Logger.d("RecipeListVM", "Calling repository.refreshRecipes()")
                recipeRepository.refreshRecipes()
                Logger.d("RecipeListVM", "refreshRecipes() completed, state: ${recipeListState.value}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    fun searchRecipes(query: String) {
        _searchQuery.value = query
        
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        
        viewModelScope.launch {
            _isSearching.value = true
            try {
                when (val result = recipeRepository.searchRecipes(query)) {
                    is RecipeResult.Success -> {
                        _searchResults.value = result.data
                    }
                    is RecipeResult.Error -> {
                        _searchResults.value = emptyList()
                    }
                    is RecipeResult.Loading -> {
                        // Keep current results while searching
                    }
                }
            } finally {
                _isSearching.value = false
            }
        }
    }
    
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }
    
    fun getDisplayedRecipes(): List<RecipeMetadata> {
        return if (_searchQuery.value.isNotBlank()) {
            _searchResults.value
        } else {
            when (val state = recipeListState.value) {
                is RecipeListState.Success -> state.recipes
                else -> emptyList()
            }
        }
    }
    
    fun getRecipesCount(): Int {
        return recipeRepository.getCachedRecipesCount()
    }
    
    fun retryLoading() {
        loadRecipes()
    }
}