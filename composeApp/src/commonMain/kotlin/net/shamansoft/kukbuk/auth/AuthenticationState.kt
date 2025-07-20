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
    val refreshToken: String?,
    val expiresAt: Long
)

sealed class AuthenticationState {
    data object Loading : AuthenticationState()
    data object Unauthenticated : AuthenticationState()
    data class Authenticated(val user: AuthUser) : AuthenticationState()
    data class Error(val message: String) : AuthenticationState()
}

sealed class AuthResult {
    data class Success(val user: AuthUser, val tokens: AuthTokens) : AuthResult()
    data class Error(val message: String) : AuthResult()
    data object Cancelled : AuthResult()
}