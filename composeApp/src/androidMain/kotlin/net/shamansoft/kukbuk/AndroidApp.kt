package net.shamansoft.kukbuk

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import net.shamansoft.kukbuk.auth.AuthViewModel
import net.shamansoft.kukbuk.auth.AuthenticationScreen
import net.shamansoft.kukbuk.auth.AuthenticationState
import net.shamansoft.kukbuk.auth.createAndroidAuthenticationRepository
import net.shamansoft.kukbuk.navigation.Screen
import net.shamansoft.kukbuk.recipe.createRecipeDetailViewModel
import net.shamansoft.kukbuk.recipe.createRecipeListViewModel

@Composable
fun AndroidApp() {
    MaterialTheme {
        val context = LocalContext.current
        val activity = LocalView.current.context as MainActivity

        val authRepository = remember {
            createAndroidAuthenticationRepository(context, activity)
        }
        val authViewModel = remember {
            AuthViewModel(authRepository)
        }
        val authState by authViewModel.authState.collectAsState()

        // Navigation state management
        var currentScreen by remember { mutableStateOf<Screen>(Screen.RecipeList) }

        when (val currentState = authState) {
            is AuthenticationState.Authenticated -> {
                when (val screen = currentScreen) {
                    Screen.RecipeList -> {
                        val recipeListViewModel = remember {
                            createRecipeListViewModel(authRepository)
                        }

                        RecipeListScreen(
                            user = currentState.user,
                            onSignOut = { authViewModel.signOut() },
                            viewModel = recipeListViewModel,
                            onRecipeClick = { recipe ->
                                currentScreen = Screen.RecipeDetail(
                                    recipeId = recipe.driveFileId,
                                    recipeTitle = recipe.title
                                )
                            }
                        )
                    }

                    is Screen.RecipeDetail -> {
                        val detailViewModel = remember(screen.recipeId) {
                            createRecipeDetailViewModel(screen.recipeId, authRepository)
                        }

                        RecipeDetailScreen(
                            recipeId = screen.recipeId,
                            recipeTitle = screen.recipeTitle,
                            onNavigateBack = { currentScreen = Screen.RecipeList },
                            viewModel = detailViewModel
                        )
                    }
                }
            }

            else -> {
                AuthenticationScreen(
                    onAuthenticationSuccess = {
                        // Navigation handled by state observation
                    },
                    authViewModel = authViewModel
                )
            }
        }
    }
}