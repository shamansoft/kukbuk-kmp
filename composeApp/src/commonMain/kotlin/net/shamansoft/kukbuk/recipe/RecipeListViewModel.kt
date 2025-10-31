package net.shamansoft.kukbuk.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class RecipeListViewModel(
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    val recipeListState: StateFlow<RecipeListState> = recipeRepository.recipeListState

    // Progressive loading state - recipes accumulated as they're loaded
    private val _progressiveRecipes = MutableStateFlow<List<RecipeListItem>>(emptyList())
    val progressiveRecipes: StateFlow<List<RecipeListItem>> = _progressiveRecipes.asStateFlow()

    private val _isLoadingProgressively = MutableStateFlow(false)
    val isLoadingProgressively: StateFlow<Boolean> = _isLoadingProgressively.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<RecipeListItem>>(emptyList())
    val searchResults: StateFlow<List<RecipeListItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var progressiveLoadingJob: Job? = null

    init {
        loadRecipesProgressively()
    }

    fun loadRecipes() {
        viewModelScope.launch {
            recipeRepository.loadRecipes()
        }
    }

    /**
     * Load recipes progressively, showing them as they're downloaded
     */
    fun loadRecipesProgressively() {
        progressiveLoadingJob?.cancel()
        progressiveLoadingJob = viewModelScope.launch {
            _isLoadingProgressively.value = true
            _progressiveRecipes.value = emptyList()

            recipeRepository.loadRecipesProgressively()
                .onEach { event ->
                    when (event) {
                        is RecipeLoadEvent.LoadingStarted -> {
                            _isLoadingProgressively.value = true
                        }
                        is RecipeLoadEvent.RecipeLoaded -> {
                            // Add recipe to the list and sort by modification time
                            val currentList = _progressiveRecipes.value.toMutableList()
                            currentList.add(event.recipe)
                            _progressiveRecipes.value = currentList.sortedByDescending { it.lastModified }
                        }
                        is RecipeLoadEvent.LoadingComplete -> {
                            _isLoadingProgressively.value = false
                        }
                        is RecipeLoadEvent.Error -> {
                            _isLoadingProgressively.value = false
                            // Error is already logged in repository
                        }
                    }
                }
                .catch { e ->
                    _isLoadingProgressively.value = false
                    // Error handling
                }
                .collect { }
        }
    }

    fun refreshRecipes() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _isLoadingProgressively.value = true
            _progressiveRecipes.value = emptyList()

            try {
                recipeRepository.loadRecipesProgressively()
                    .onEach { event ->
                        when (event) {
                            is RecipeLoadEvent.RecipeLoaded -> {
                                val currentList = _progressiveRecipes.value.toMutableList()
                                currentList.add(event.recipe)
                                _progressiveRecipes.value = currentList.sortedByDescending { it.lastModified }
                            }
                            is RecipeLoadEvent.LoadingComplete -> {
                                _isLoadingProgressively.value = false
                                _isRefreshing.value = false
                            }
                            is RecipeLoadEvent.Error -> {
                                _isLoadingProgressively.value = false
                                _isRefreshing.value = false
                            }
                            else -> {}
                        }
                    }
                    .catch { e ->
                        _isLoadingProgressively.value = false
                        _isRefreshing.value = false
                    }
                    .collect { }
            } finally {
                _isRefreshing.value = false
                _isLoadingProgressively.value = false
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
            // Use progressive recipes if available, otherwise fall back to repository state
            if (_progressiveRecipes.value.isNotEmpty()) {
                _progressiveRecipes.value
            } else {
                when (val state = recipeListState.value) {
                    is RecipeListState.Success -> state.recipes
                    else -> emptyList()
                }
            }
        }
    }

    fun getRecipesCount(): Int {
        return if (_progressiveRecipes.value.isNotEmpty()) {
            _progressiveRecipes.value.size
        } else {
            recipeRepository.getCachedRecipesCount()
        }
    }

    fun retryLoading() {
        loadRecipesProgressively()
    }
}