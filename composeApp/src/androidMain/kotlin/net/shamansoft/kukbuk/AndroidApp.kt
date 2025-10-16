package net.shamansoft.kukbuk

import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalView
import net.shamansoft.kukbuk.auth.AuthViewModel
import net.shamansoft.kukbuk.auth.AuthenticationScreen
import net.shamansoft.kukbuk.auth.AuthenticationState
import net.shamansoft.kukbuk.auth.createAndroidAuthenticationRepository
import net.shamansoft.kukbuk.recipe.createRecipeListViewModel

private const val TAG = "Kukbuk"

@Composable
fun AndroidApp() {
    Log.d(TAG, "=== AndroidApp Composable called ===")
    MaterialTheme {
        val context = LocalContext.current
        val activity = LocalView.current.context as MainActivity

        Log.d(TAG, "Creating authentication repository")
        val authRepository = remember {
            Log.d(TAG, "Initializing AndroidAuthenticationRepository")
            createAndroidAuthenticationRepository(context, activity)
        }
        val authViewModel = remember {
            Log.d(TAG, "Creating AuthViewModel")
            AuthViewModel(authRepository)
        }
        val authState by authViewModel.authState.collectAsState()

        Log.d(TAG, "Current auth state: $authState")

        when (val currentState = authState) {
            is AuthenticationState.Authenticated -> {
                Log.d(TAG, "User authenticated: ${currentState.user.email}")
                val recipeListViewModel = remember {
                    Log.d(TAG, "Creating RecipeListViewModel")
                    createRecipeListViewModel(authRepository)
                }

                Log.d(TAG, "Rendering RecipeListScreen")
                RecipeListScreen(
                    user = currentState.user,
                    onSignOut = {
                        Log.d(TAG, "Sign out requested")
                        authViewModel.signOut()
                    },
                    viewModel = recipeListViewModel,
                    onRecipeClick = { recipe ->
                        // TODO: Navigate to recipe detail screen
                        Log.d(TAG, "Recipe clicked: ${recipe.title}")
                    }
                )
            }

            else -> {
                Log.d(TAG, "User not authenticated, showing AuthenticationScreen")
                AuthenticationScreen(
                    onAuthenticationSuccess = {
                        // Navigation handled by state observation
                        Log.d(TAG, "Authentication success callback")
                    },
                    authViewModel = authViewModel
                )
            }
        }
    }
}