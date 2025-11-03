package net.shamansoft.kukbuk.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?
)

@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String, // Required for persistent login
    val expiresAt: Long
)

/**
 * Extension to check if tokens are expired with a buffer time.
 */
fun AuthTokens.isExpired(bufferMs: Long = 300_000L): Boolean {
    val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    return now >= (expiresAt - bufferMs)
}

sealed class AuthenticationState {
    data object Loading : AuthenticationState()
    data object Unauthenticated : AuthenticationState()
    data object Refreshing : AuthenticationState() // Token refresh in progress
    data class Authenticated(val user: AuthUser) : AuthenticationState()
    data class Error(val message: String) : AuthenticationState()
}

sealed class AuthResult {
    data class Success(val user: AuthUser, val tokens: AuthTokens) : AuthResult()
    data class Error(val message: String) : AuthResult()
    data object Cancelled : AuthResult()
}