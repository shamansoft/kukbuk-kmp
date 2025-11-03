package net.shamansoft.kukbuk

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.shamansoft.kukbuk.auth.AuthViewModel
import net.shamansoft.kukbuk.auth.AuthenticationScreen
import net.shamansoft.kukbuk.auth.AuthenticationState
import net.shamansoft.kukbuk.di.getPlatformProductionModules
import net.shamansoft.kukbuk.navigation.Screen
import net.shamansoft.kukbuk.util.Logger
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
@Preview
fun App() {
    // Initialize Koin with production modules (Google Drive)
    KoinApplication(
        application = {
            Logger.d("App", "Using PRODUCTION data source (Google Drive)")
            modules(getPlatformProductionModules())
        }
    ) {
        MaterialTheme {
            AppContent()
        }
    }
}

@Composable
fun AppContent() {
    // Inject ViewModels using Koin
    val authViewModel: AuthViewModel = koinViewModel()
    val authState by authViewModel.authState.collectAsState()

    // Navigation state management - hoisted outside authentication state
    var currentScreen by remember { mutableStateOf<Screen>(Screen.RecipeList) }

    // Keep ViewModel alive across navigation - inject via Koin
    val recipeListViewModel = koinViewModel<net.shamansoft.kukbuk.recipe.RecipeListViewModel>()

    // Offline-first: Always show main content, authentication handled inline when needed
    when (val screen = currentScreen) {
        Screen.RecipeList -> {
            RecipeListScreen(
                user = (authState as? AuthenticationState.Authenticated)?.user,
                onSignOut = { authViewModel.signOut() },
                viewModel = recipeListViewModel,
                authViewModel = authViewModel,
                onRecipeClick = { recipe ->
                    currentScreen = Screen.RecipeDetail(
                        recipeId = recipe.id,
                        recipeTitle = recipe.title
                    )
                },
                onNavigateToSettings = {
                    currentScreen = Screen.Settings
                }
            )
        }

        is Screen.RecipeDetail -> {
            // Inject RecipeDetailViewModel with parameter (recipeId)
            val detailViewModel = koinViewModel<net.shamansoft.kukbuk.recipe.RecipeDetailViewModel>(
                key = screen.recipeId // Use recipeId as key to create unique instances
            ) {
                parametersOf(screen.recipeId)
            }

            RecipeDetailScreen(
                recipeId = screen.recipeId,
                recipeTitle = screen.recipeTitle,
                onNavigateBack = { currentScreen = Screen.RecipeList },
                viewModel = detailViewModel
            )
        }

        Screen.Settings -> {
            val settingsViewModel = koinViewModel<net.shamansoft.kukbuk.settings.SettingsViewModel>()

            SettingsScreen(
                onNavigateBack = { currentScreen = Screen.RecipeList },
                viewModel = settingsViewModel
            )
        }
    }
}
