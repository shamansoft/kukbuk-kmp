package net.shamansoft.kukbuk

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import net.shamansoft.kukbuk.auth.AuthViewModel
import net.shamansoft.kukbuk.auth.AuthenticationScreen
import net.shamansoft.kukbuk.auth.AuthenticationState
import net.shamansoft.kukbuk.auth.createAuthenticationRepository
import net.shamansoft.kukbuk.recipe.createRecipeListViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

import net.shamansoft.kukbuk.util.Logger
@Composable
@Preview
fun App() {
    MaterialTheme {
        val authRepository = remember { createAuthenticationRepository() }
        val authViewModel = remember { AuthViewModel(authRepository) }
        val authState by authViewModel.authState.collectAsState()

        when (val currentState = authState) {
            is AuthenticationState.Authenticated -> {
                Logger.d("App", "User authenticated: ${currentState.user.email}")
                val recipeListViewModel = remember {
                    Logger.d("App", "Creating RecipeListViewModel")
                    createRecipeListViewModel(authRepository)
                }

                Logger.d("App", "Rendering RecipeListScreen")
                RecipeListScreen(
                    user = currentState.user,
                    onSignOut = { authViewModel.signOut() },
                    viewModel = recipeListViewModel,
                    onRecipeClick = { recipe ->
                        // TODO: Navigate to recipe detail screen
                        println("Recipe clicked: ${recipe.title}")
                    }
                )
            }

            else -> {
                Logger.d("App", "User not authenticated, showing AuthenticationScreen, state: $currentState")
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
