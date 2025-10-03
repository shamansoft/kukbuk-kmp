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

            if (user != null && tokens != null) {
                if (!isTokenExpired(tokens)) {
                    if (authService.validateToken(tokens.accessToken)) {
                        _authState.value = AuthenticationState.Authenticated(user)
                    } else {
                        tryRefreshToken(user, tokens)
                    }
                } else {
                    tryRefreshToken(user, tokens)
                }
            } else {
                _authState.value = AuthenticationState.Unauthenticated
            }
        } catch (e: Exception) {
            _authState.value = AuthenticationState.Error("Failed to initialize authentication: ${e.message}")
        }
    }

    private suspend fun tryRefreshToken(user: AuthUser, expiredTokens: AuthTokens) {
        if (expiredTokens.refreshToken != null) {
            when (val refreshResult = authService.refreshToken()) {
                is AuthResult.Success -> {
                    secureStorage.storeTokens(refreshResult.tokens)
                    _authState.value = AuthenticationState.Authenticated(user)
                }
                is AuthResult.Error -> {
                    secureStorage.clearTokens()
                    secureStorage.clearUser()
                    _authState.value = AuthenticationState.Unauthenticated
                }
                is AuthResult.Cancelled -> {
                    _authState.value = AuthenticationState.Unauthenticated
                }
            }
        } else {
            secureStorage.clearTokens()
            secureStorage.clearUser()
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
        _authState.value = AuthenticationState.Loading
        return try {
            authService.signOut()
            secureStorage.clearTokens()
            secureStorage.clearUser()
            _authState.value = AuthenticationState.Unauthenticated
            Result.success(Unit)
        } catch (e: Exception) {
            _authState.value = AuthenticationState.Error("Failed to sign out: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): AuthUser? {
        return when (val state = _authState.value) {
            is AuthenticationState.Authenticated -> state.user
            else -> null
        }
    }

    suspend fun refreshTokenIfNeeded(): Boolean {
        val user = secureStorage.getUser()
        val tokens = secureStorage.getTokens()
        
        if (user != null && tokens != null && isTokenExpired(tokens)) {
            tryRefreshToken(user, tokens)
            return when (_authState.value) {
                is AuthenticationState.Authenticated -> true
                else -> false
            }
        }
        return true
    }

    suspend fun requireValidAuthentication(): AuthUser? {
        refreshTokenIfNeeded()
        return getCurrentUser()
    }

    suspend fun getValidAccessToken(): String? {
        return secureStorage.getValidAccessToken()
    }

    fun isAuthenticated(): Boolean {
        return _authState.value is AuthenticationState.Authenticated
    }

    fun isLoading(): Boolean {
        return _authState.value is AuthenticationState.Loading
    }

    private fun isTokenExpired(tokens: AuthTokens): Boolean {
        val bufferTime = 300_000L // 5 minutes buffer
        return kotlinx.datetime.Clock.System.now().toEpochMilliseconds() >= (tokens.expiresAt - bufferTime)
    }
}