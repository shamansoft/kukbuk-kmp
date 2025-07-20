package net.shamansoft.kukbuk

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import net.shamansoft.kukbuk.auth.AuthServiceFactory
import net.shamansoft.kukbuk.auth.AuthViewModel
import net.shamansoft.kukbuk.auth.AuthenticationScreen
import net.shamansoft.kukbuk.auth.AuthenticationState
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val authRepository = remember { AuthServiceFactory.createAuthenticationRepository() }
        val authViewModel: AuthViewModel = viewModel { AuthViewModel(authRepository) }
        val authState by authViewModel.authState.collectAsState()

        when (authState) {
            is AuthenticationState.Authenticated -> {
                RecipeListScreen(
                    user = authState.user,
                    onSignOut = { authViewModel.signOut() }
                )
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