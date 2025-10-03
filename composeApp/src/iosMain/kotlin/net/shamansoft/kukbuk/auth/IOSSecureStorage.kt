package net.shamansoft.kukbuk.auth

import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

class IOSSecureStorage : SecureStorage {

    private val json = Json { ignoreUnknownKeys = true }
    private val userDefaults = NSUserDefaults.standardUserDefaults

    companion object {
        private const val USER_KEY = "auth_user"
        private const val TOKENS_KEY = "auth_tokens"
    }

    override suspend fun storeTokens(tokens: AuthTokens) {
        val tokenData = json.encodeToString(tokens)
        userDefaults.setObject(tokenData, TOKENS_KEY)
        userDefaults.synchronize()
    }

    override suspend fun getTokens(): AuthTokens? {
        val tokenString = userDefaults.stringForKey(TOKENS_KEY)
        return tokenString?.let {
            try {
                json.decodeFromString<AuthTokens>(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun clearTokens() {
        userDefaults.removeObjectForKey(TOKENS_KEY)
        userDefaults.synchronize()
    }

    override suspend fun storeUser(user: AuthUser) {
        val userData = json.encodeToString(user)
        userDefaults.setObject(userData, USER_KEY)
        userDefaults.synchronize()
    }

    override suspend fun getUser(): AuthUser? {
        val userString = userDefaults.stringForKey(USER_KEY)
        return userString?.let {
            try {
                json.decodeFromString<AuthUser>(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun clearUser() {
        userDefaults.removeObjectForKey(USER_KEY)
        userDefaults.synchronize()
    }

    override suspend fun getValidAccessToken(): String? {
        return getTokens()?.accessToken
    }
}