package net.shamansoft.kukbuk.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.shamansoft.kukbuk.util.Logger

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
            Logger.e("AuthRepo", "Failed to initialize authentication: ${e.message}")
            _authState.value = AuthenticationState.Error("Failed to initialize authentication: ${e.message}")
        }
    }

    private suspend fun tryRefreshToken(user: AuthUser, expiredTokens: AuthTokens) {
        // TEMPORARY FIX: Check if refresh token is actually a valid refresh token
        // If access token == refresh token, it means we only have an ID token (not a real refresh token)
        // In this case, skip refresh and just use the token as-is (will require re-login when truly expired)
        if (expiredTokens.accessToken == expiredTokens.refreshToken) {
            Logger.i("AuthRepo", "No valid refresh token available (ID token only), keeping current session")
            _authState.value = AuthenticationState.Authenticated(user)
            return
        }

        _authState.value = AuthenticationState.Refreshing
        Logger.d("AuthRepo", "Attempting to refresh token...")

        when (val refreshResult = authService.refreshToken()) {
            is AuthResult.Success -> {
                secureStorage.storeTokens(refreshResult.tokens)
                _authState.value = AuthenticationState.Authenticated(user)
                Logger.d("AuthRepo", "Token refresh successful")
            }
            is AuthResult.Error -> {
                Logger.e("AuthRepo", "Token refresh failed: ${refreshResult.message}")
                secureStorage.clearTokens()
                secureStorage.clearUser()
                _authState.value = AuthenticationState.Unauthenticated
            }
            is AuthResult.Cancelled -> {
                Logger.i("AuthRepo", "Token refresh cancelled")
                _authState.value = AuthenticationState.Unauthenticated
            }
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
        val tokens = secureStorage.getTokens() ?: return null
        val user = secureStorage.getUser() ?: return null

        // Check if token is expired or about to expire (5 min buffer)
        if (isTokenExpired(tokens)) {
            // TEMPORARY FIX: Don't try to refresh if we only have an ID token (not a real refresh token)
            if (tokens.accessToken == tokens.refreshToken) {
                Logger.i("AuthRepo", "Token expired but no valid refresh token, returning current token")
                return tokens.accessToken // Return it anyway, will fail and trigger re-login
            }

            // Try to refresh the token
            Logger.d("AuthRepo", "Access token expired, refreshing...")
            when (val refreshResult = authService.refreshToken()) {
                is AuthResult.Success -> {
                    secureStorage.storeTokens(refreshResult.tokens)
                    return refreshResult.tokens.accessToken
                }
                is AuthResult.Error -> {
                    Logger.e("AuthRepo", "Token refresh failed: ${refreshResult.message}")
                    return null
                }
                is AuthResult.Cancelled -> return null
            }
        }

        return tokens.accessToken
    }

    suspend fun handleAuthenticationError() {
        Logger.e("AuthRepo", "Authentication error occurred - clearing credentials")
        // Don't call signOut() to avoid loops - directly clear and update state
        try {
            secureStorage.clearTokens()
            secureStorage.clearUser()
            _authState.value = AuthenticationState.Unauthenticated
        } catch (e: Exception) {
            Logger.e("AuthRepo", "Error handling authentication error: ${e.message}")
            _authState.value = AuthenticationState.Error("Authentication failed. Please sign in again.")
        }
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