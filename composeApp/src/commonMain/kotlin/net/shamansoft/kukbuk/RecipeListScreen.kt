package net.shamansoft.kukbuk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.shamansoft.kukbuk.auth.AuthUser
import net.shamansoft.kukbuk.recipe.RecipeListState
import net.shamansoft.kukbuk.recipe.RecipeListViewModel
import net.shamansoft.kukbuk.recipe.RecipeListItem
import net.shamansoft.kukbuk.util.RecipeImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    user: AuthUser,
    onSignOut: () -> Unit,
    viewModel: RecipeListViewModel,
    onRecipeClick: (RecipeListItem) -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val recipeListState by viewModel.recipeListState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val progressiveRecipes by viewModel.progressiveRecipes.collectAsState()
    val isLoadingProgressively by viewModel.isLoadingProgressively.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    var showSearch by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top bar with user info and actions
        TopAppBar(
            user = user,
            onSignOut = onSignOut,
            onSearchClick = { showSearch = !showSearch },
            onRefreshClick = { viewModel.refreshRecipes() },
            onNavigateToSettings = onNavigateToSettings,
            isRefreshing = isRefreshing
        )

        // Search bar
        if (showSearch) {
            Spacer(modifier = Modifier.height(8.dp))
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.searchRecipes(it) },
                onClearSearch = {
                    viewModel.clearSearch()
                    showSearch = false
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recipe list content - use progressive loading state
        when {
            // Show progressive recipes as they load
            progressiveRecipes.isNotEmpty() || isLoadingProgressively -> {
                val displayedRecipes = viewModel.getDisplayedRecipes()
                RecipeList(
                    recipes = displayedRecipes,
                    onRecipeClick = onRecipeClick,
                    isRefreshing = isRefreshing,
                    isLoadingMore = isLoadingProgressively,
                    hasMore = hasMore,
                    onRefresh = { viewModel.refreshRecipes() },
                    onLoadMore = { viewModel.loadMoreRecipes() }
                )
            }

            // Fallback to old state-based rendering
            recipeListState is RecipeListState.Loading -> {
                LoadingState()
            }

            recipeListState is RecipeListState.Success -> {
                val displayedRecipes = viewModel.getDisplayedRecipes()
                RecipeList(
                    recipes = displayedRecipes,
                    onRecipeClick = onRecipeClick,
                    isRefreshing = isRefreshing,
                    isLoadingMore = false,
                    hasMore = false,
                    onRefresh = { viewModel.refreshRecipes() },
                    onLoadMore = {}
                )
            }

            recipeListState is RecipeListState.Error -> {
                ErrorState(
                    message = (recipeListState as RecipeListState.Error).message,
                    onRetry = { viewModel.retryLoading() }
                )
            }

            recipeListState is RecipeListState.Empty -> {
                EmptyState()
            }
        }
    }
}

@Composable
private fun TopAppBar(
    user: AuthUser,
    onSignOut: () -> Unit,
    onSearchClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    isRefreshing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "My Recipes",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user.displayName ?: user.email,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSearchClick) {
                    Text("🔍")
                }

                IconButton(
                    onClick = onRefreshClick,
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text("🔄")
                    }
                }

                IconButton(onClick = onNavigateToSettings) {
                    Text("⚙️")
                }

                OutlinedButton(onClick = onSignOut) {
                    Text("Sign Out")
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search recipes...") },
        leadingIcon = { Text("🔍") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClearSearch) {
                    Text("✕")
                }
            }
        },
        singleLine = true,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeList(
    recipes: List<RecipeListItem>,
    onRecipeClick: (RecipeListItem) -> Unit,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit
) {
    if (recipes.isEmpty() && !isLoadingMore) {
        EmptySearchState()
    } else {
        val listState = rememberLazyListState()

        // Infinite scroll detection
        LaunchedEffect(listState, hasMore, isLoadingMore) {
            snapshotFlow {
                val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val totalItems = listState.layoutInfo.totalItemsCount
                // Trigger load more when we're 2 items away from the end
                lastVisibleIndex >= totalItems - 2
            }.collect { shouldLoadMore ->
                if (shouldLoadMore && hasMore && !isLoadingMore && recipes.isNotEmpty()) {
                    onLoadMore()
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = recipes,
                    key = { it.id }
                ) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        onClick = { onRecipeClick(recipe) }
                    )
                }

                // Show loading indicator at the bottom while more recipes are being loaded
                if (isLoadingMore && recipes.isNotEmpty()) {
                    item(key = "loading_more") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Loading more recipes...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Show "end of list" indicator when all recipes are loaded
                if (!hasMore && !isLoadingMore && recipes.isNotEmpty()) {
                    item(key = "end_of_list") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "All recipes loaded",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeCard(
    recipe: RecipeListItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Recipe thumbnail with image loading
            RecipeImage(
                url = recipe.imageUrl,
                contentDescription = "Recipe: ${recipe.title}",
                modifier = Modifier.size(80.dp),
                cornerRadius = 8.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = recipe.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                recipe.author?.let { author ->
                    Text(
                        text = "by $author",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                recipe.description?.let { description ->
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "Last updated: ${formatDate(recipe.lastModified)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading your recipes...",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "⚠️",
                fontSize = 64.sp
            )

            Text(
                text = "Couldn't load recipes",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Button(onClick = onRetry) {
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "📝",
                fontSize = 64.sp
            )

            Text(
                text = "No recipes found",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Start saving recipes with the Kukbuk browser extension to see them here.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your recipes will sync automatically from Google Drive.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptySearchState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "🔍",
                fontSize = 48.sp
            )

            Text(
                text = "No matching recipes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Try a different search term",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(dateString: String): String {
    // Simple date formatting - in a real app you'd use proper date formatting
    return try {
        val parts = dateString.take(10) // Take YYYY-MM-DD part
        parts.replace("-", "/")
    } catch (e: Exception) {
        dateString
    }
}
