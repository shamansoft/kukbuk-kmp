package net.shamansoft.kukbuk.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthenticationRepository(
    private val authService: AuthenticationService,
    private val secureStorage: SecureStorage
) {
    private val _authState = MutableStateFlow<AuthenticationState>(AuthenticationState.Loading)
    val authState: StateFlow<AuthenticationState> = _authState.asStateFlow()

    suspend fun initialize() {
        _authState.value = AuthenticationState.Loading
        
        try {
            val user = secureStorage.getUser()
            val tokens = secureStorage.getTokens()
            
            if (user != null && tokens != null && !isTokenExpired(tokens)) {
                _authState.value = AuthenticationState.Authenticated(user)
            } else {
                secureStorage.clearTokens()
                secureStorage.clearUser()
                _authState.value = AuthenticationState.Unauthenticated
            }
        } catch (e: Exception) {
            _authState.value = AuthenticationState.Unauthenticated
        }
    }

    suspend fun signInWithGoogle(): AuthResult {
        _authState.value = AuthenticationState.Loading
        
        return when (val result = authService.signInWithGoogle()) {
            is AuthResult.Success -> {
                secureStorage.storeUser(result.user)
                secureStorage.storeTokens(result.tokens)
                _authState.value = AuthenticationState.Authenticated(result.user)
                result
            }
            is AuthResult.Error -> {
                _authState.value = AuthenticationState.Error(result.message)
                result
            }
            is AuthResult.Cancelled -> {
                _authState.value = AuthenticationState.Unauthenticated
                result
            }
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            authService.signOut()
            secureStorage.clearTokens()
            secureStorage.clearUser()
            _authState.value = AuthenticationState.Unauthenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): AuthUser? {
        return when (val state = _authState.value) {
            is AuthenticationState.Authenticated -> state.user
            else -> null
        }
    }

    private fun isTokenExpired(tokens: AuthTokens): Boolean {
        return System.currentTimeMillis() >= tokens.expiresAt
    }
}