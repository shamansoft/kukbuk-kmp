package net.shamansoft.kukbuk.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthenticationRepository? = null
) : ViewModel() {
    
    // In a real implementation, inject the repository via DI
    val authState: StateFlow<AuthenticationState> = authRepository?.authState 
        ?: kotlinx.coroutines.flow.MutableStateFlow(AuthenticationState.Unauthenticated)
    
    init {
        // Initialize authentication state
        viewModelScope.launch {
            authRepository?.initialize()
        }
    }
    
    fun signInWithGoogle() {
        viewModelScope.launch {
            authRepository?.signInWithGoogle()
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            authRepository?.signOut()
        }
    }
    
    fun getCurrentUser(): AuthUser? {
        return when (val state = authState.value) {
            is AuthenticationState.Authenticated -> state.user
            else -> null
        }
    }
}