package net.shamansoft.kukbuk.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WasmAuthenticationService : AuthenticationService {

    private val _authenticationState =
        MutableStateFlow<AuthenticationState>(AuthenticationState.Loading)
    override val authenticationState: StateFlow<AuthenticationState> =
        _authenticationState.asStateFlow()

    override suspend fun signInWithGoogle(): AuthResult {
        // TODO: Implement WASM Google Sign-In
        // This would require:
        // 1. Using JavaScript Google Sign-In SDK
        // 2. Implementing browser-based OAuth flow
        // 3. Handling redirect or popup flows

        return AuthResult.Error("WASM Google Sign-In not yet implemented. Please use Android or iOS.")
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): AuthUser? {
        return null
    }

    override suspend fun isUserSignedIn(): Boolean {
        return false
    }

    override suspend fun getValidAccessToken(): String? {
        return null
    }
}