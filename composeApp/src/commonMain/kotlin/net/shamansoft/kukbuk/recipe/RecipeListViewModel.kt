package net.shamansoft.kukbuk.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipeListViewModel(
    private val recipeRepository: RecipeRepository
) : ViewModel() {
    
    val recipeListState: StateFlow<RecipeListState> = recipeRepository.recipeListState
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<RecipeListItem>>(emptyList())
    val searchResults: StateFlow<List<RecipeListItem>> = _searchResults.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    init {
        loadRecipes()
    }

    fun loadRecipes() {
        viewModelScope.launch {
            recipeRepository.loadRecipes()
        }
    }

    fun refreshRecipes() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                recipeRepository.refreshRecipes()
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
    
    fun getDisplayedRecipes(): List<RecipeListItem> {
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