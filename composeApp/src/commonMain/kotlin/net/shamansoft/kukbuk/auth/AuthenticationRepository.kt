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
        Logger.d("AuthRepo", "initialize() called")
        _authState.value = AuthenticationState.Loading

        try {
            val user = secureStorage.getUser()
            val tokens = secureStorage.getTokens()

            Logger.d("AuthRepo", "User from storage: ${user?.email ?: "null"}")
            Logger.d("AuthRepo", "Tokens from storage: ${if (tokens != null) "present" else "null"}")

            if (user != null && tokens != null) {
                Logger.d("AuthRepo", "Token expired: ${isTokenExpired(tokens)}")
                if (!isTokenExpired(tokens)) {
                    Logger.d("AuthRepo", "Validating token...")
                    if (authService.validateToken(tokens.accessToken)) {
                        Logger.d("AuthRepo", "Token valid, setting authenticated state")
                        _authState.value = AuthenticationState.Authenticated(user)
                    } else {
                        Logger.d("AuthRepo", "Token invalid, trying refresh")
                        tryRefreshToken(user, tokens)
                    }
                } else {
                    Logger.d("AuthRepo", "Token expired, trying refresh")
                    tryRefreshToken(user, tokens)
                }
            } else {
                Logger.d("AuthRepo", "No user or tokens, setting unauthenticated")
                _authState.value = AuthenticationState.Unauthenticated
            }
        } catch (e: Exception) {
            Logger.e("AuthRepo", "Exception during initialize: ${e.message}")
            e.printStackTrace()
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
        Logger.d("AuthRepo", "getValidAccessToken() called")

        val tokens = secureStorage.getTokens()
        if (tokens == null) {
            Logger.d("AuthRepo", "No tokens found in storage")
            return null
        }

        Logger.d("AuthRepo", "Found tokens, checking expiration...")
        Logger.d("AuthRepo", "Token expires at: ${tokens.expiresAt}, current time: ${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}")

        // Check if token is expired or about to expire (5 min buffer)
        if (isTokenExpired(tokens)) {
            Logger.d("AuthRepo", "Token is expired, attempting refresh...")
            val user = secureStorage.getUser()
            if (user != null && tokens.refreshToken != null) {
                // Try to refresh the token
                when (val refreshResult = authService.refreshToken()) {
                    is AuthResult.Success -> {
                        Logger.d("AuthRepo", "Token refreshed successfully")
                        secureStorage.storeTokens(refreshResult.tokens)
                        return refreshResult.tokens.accessToken
                    }
                    is AuthResult.Error -> {
                        Logger.e("AuthRepo", "Token refresh failed: ${refreshResult.message}")
                        return null
                    }
                    is AuthResult.Cancelled -> {
                        Logger.d("AuthRepo", "Token refresh cancelled")
                        return null
                    }
                }
            } else {
                Logger.d("AuthRepo", "Cannot refresh token - no user or refresh token")
                return null
            }
        }

        Logger.d("AuthRepo", "Returning valid access token (length=${tokens.accessToken.length})")
        return tokens.accessToken
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