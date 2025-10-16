package net.shamansoft.kukbuk.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import net.shamansoft.kukbuk.util.Logger
class AuthViewModel(
    private val authRepository: AuthenticationRepository? = null
) : ViewModel() {

    // In a real implementation, inject the repository via DI
    val authState: StateFlow<AuthenticationState> = authRepository?.authState
        ?: kotlinx.coroutines.flow.MutableStateFlow(AuthenticationState.Unauthenticated)

    private val _isLoading = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        Logger.d("AuthVM", "Initializing...")
        // Initialize authentication state
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Logger.d("AuthVM", "Calling authRepository.initialize()")
                authRepository?.initialize()
                Logger.d("AuthVM", "Initialized, auth state: ${authState.value}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                when (val result = authRepository?.signInWithGoogle()) {
                    is AuthResult.Error -> {
                        _errorMessage.value = result.message
                    }
                    is AuthResult.Cancelled -> {
                        _errorMessage.value = "Sign in was cancelled"
                    }
                    else -> {
                        _errorMessage.value = null
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Sign in failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = authRepository?.signOut()
                if (result?.isFailure == true) {
                    _errorMessage.value = "Sign out failed: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Sign out failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getCurrentUser(): AuthUser? {
        return when (val state = authState.value) {
            is AuthenticationState.Authenticated -> state.user
            else -> null
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun refreshAuthenticationIfNeeded() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                authRepository?.refreshTokenIfNeeded()
            } catch (e: Exception) {
                _errorMessage.value = "Authentication refresh failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun isAuthenticated(): Boolean {
        return authRepository?.isAuthenticated() ?: false
    }

    fun requireValidAuthentication(): AuthUser? {
        viewModelScope.launch {
            authRepository?.requireValidAuthentication()
        }
        return getCurrentUser()
    }
}
