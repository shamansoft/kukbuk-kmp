package net.shamansoft.kukbuk.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.shamansoft.kukbuk.util.Logger

/**
 * Authentication repository with offline-first architecture.
 *
 * Key principles:
 * - Authentication is ONLY required for syncing recipes from Google Drive
 * - Cached recipes can be viewed without authentication
 * - Token refresh is NOT automatic - only triggered when user explicitly syncs
 * - Expired tokens don't interrupt user experience - they just prevent syncing until re-auth
 *
 * Authentication flow:
 * 1. App launches → Load cached credentials (no validation)
 * 2. User views cached recipes → No authentication needed
 * 3. User pulls to refresh → Check token validity
 * 4. If token expired → Request re-authentication inline
 * 5. After auth → Retry sync operation
 */
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

            // Simply check if we have stored credentials
            // Don't try to refresh tokens - that's only needed when user explicitly syncs
            if (user != null && tokens != null) {
                _authState.value = AuthenticationState.Authenticated(user)
                Logger.d("AuthRepo", "Initialized with existing credentials")
            } else {
                _authState.value = AuthenticationState.Unauthenticated
                Logger.d("AuthRepo", "Initialized without credentials")
            }
        } catch (e: Exception) {
            Logger.e("AuthRepo", "Failed to initialize authentication: ${e.message}")
            _authState.value = AuthenticationState.Error("Failed to initialize authentication: ${e.message}")
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

    /**
     * Get access token if available and not expired.
     * Returns null if token is expired - caller should handle re-authentication.
     *
     * Note: This method does NOT attempt to refresh tokens automatically.
     * Token refresh should only happen when user explicitly initiates a sync/refresh.
     */
    suspend fun getValidAccessToken(): String? {
        val tokens = secureStorage.getTokens() ?: return null
        val user = secureStorage.getUser() ?: return null

        // Check if token is expired (with 5 minute buffer for clock skew)
        if (isTokenExpired(tokens)) {
            Logger.d("AuthRepo", "Access token expired, re-authentication required")
            return null
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