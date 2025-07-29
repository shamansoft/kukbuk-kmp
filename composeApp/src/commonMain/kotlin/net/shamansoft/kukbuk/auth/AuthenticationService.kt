package net.shamansoft.kukbuk.auth

import kotlinx.coroutines.flow.StateFlow

interface AuthenticationService {
    val authenticationState: StateFlow<AuthenticationState>

    suspend fun signInWithGoogle(): AuthResult
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUser(): AuthUser?
    suspend fun isUserSignedIn(): Boolean
    suspend fun getValidAccessToken(): String?
    suspend fun refreshToken(): AuthResult
    suspend fun validateToken(token: String): Boolean
}

interface SecureStorage {
    suspend fun storeTokens(tokens: AuthTokens)
    suspend fun getTokens(): AuthTokens?
    suspend fun clearTokens()
    suspend fun storeUser(user: AuthUser)
    suspend fun getUser(): AuthUser?
    suspend fun clearUser()
}