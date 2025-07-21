package net.shamansoft.kukbuk.auth

import kotlinx.serialization.json.Json

class WasmSecureStorage : SecureStorage {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val USER_KEY = "auth_user"
        private const val TOKENS_KEY = "auth_tokens"
    }

    override suspend fun storeTokens(tokens: AuthTokens) {
        val tokenData = json.encodeToString(tokens)
        // TODO: Use secure browser storage like IndexedDB with encryption
        // For now using localStorage as placeholder
        localStorage.setItem(TOKENS_KEY, tokenData)
    }

    override suspend fun getTokens(): AuthTokens? {
        val tokenString = localStorage.getItem(TOKENS_KEY)
        return tokenString?.let {
            try {
                json.decodeFromString<AuthTokens>(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun clearTokens() {
        localStorage.removeItem(TOKENS_KEY)
    }

    override suspend fun storeUser(user: AuthUser) {
        val userData = json.encodeToString(user)
        localStorage.setItem(USER_KEY, userData)
    }

    override suspend fun getUser(): AuthUser? {
        val userString = localStorage.getItem(USER_KEY)
        return userString?.let {
            try {
                json.decodeFromString<AuthUser>(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun clearUser() {
        localStorage.removeItem(USER_KEY)
    }
}

// Simple localStorage wrapper for WASM
external object localStorage {
    fun setItem(key: String, value: String)
    fun getItem(key: String): String?
    fun removeItem(key: String)
}