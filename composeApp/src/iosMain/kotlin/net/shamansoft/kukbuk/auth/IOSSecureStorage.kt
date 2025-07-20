package net.shamansoft.kukbuk.auth

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults
import platform.Security.*

class IOSSecureStorage : SecureStorage {
    
    private val json = Json { ignoreUnknownKeys = true }
    private val userDefaults = NSUserDefaults.standardUserDefaults
    
    companion object {
        private const val SERVICE_NAME = "KukbukAuth"
        private const val USER_ACCOUNT = "auth_user"
        private const val TOKENS_ACCOUNT = "auth_tokens"
    }
    
    override suspend fun storeTokens(tokens: AuthTokens) {
        val tokenData = json.encodeToString(tokens)
        storeInKeychain(TOKENS_ACCOUNT, tokenData)
    }
    
    override suspend fun getTokens(): AuthTokens? {
        val tokenString = getFromKeychain(TOKENS_ACCOUNT)
        return tokenString?.let {
            try {
                json.decodeFromString<AuthTokens>(it)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    override suspend fun clearTokens() {
        deleteFromKeychain(TOKENS_ACCOUNT)
    }
    
    override suspend fun storeUser(user: AuthUser) {
        val userData = json.encodeToString(user)
        storeInKeychain(USER_ACCOUNT, userData)
    }
    
    override suspend fun getUser(): AuthUser? {
        val userString = getFromKeychain(USER_ACCOUNT)
        return userString?.let {
            try {
                json.decodeFromString<AuthUser>(it)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    override suspend fun clearUser() {
        deleteFromKeychain(USER_ACCOUNT)
    }
    
    private fun storeInKeychain(account: String, value: String) {
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE_NAME,
            kSecAttrAccount to account,
            kSecValueData to value.encodeToByteArray().toNSData()
        )
        
        // Delete existing item first
        deleteFromKeychain(account)
        
        // Add new item
        SecItemAdd(query as CFDictionaryRef, null)
    }
    
    private fun getFromKeychain(account: String): String? {
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE_NAME,
            kSecAttrAccount to account,
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne
        )
        
        val result = SecItemCopyMatching(query as CFDictionaryRef, null)
        
        return if (result == errSecSuccess) {
            // TODO: Convert NSData to String
            // This requires platform-specific NSData handling
            null
        } else {
            null
        }
    }
    
    private fun deleteFromKeychain(account: String) {
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE_NAME,
            kSecAttrAccount to account
        )
        
        SecItemDelete(query as CFDictionaryRef)
    }
}

// Extension function to convert ByteArray to NSData
private fun ByteArray.toNSData(): platform.Foundation.NSData {
    // TODO: Implement proper ByteArray to NSData conversion
    // This is a placeholder - actual implementation requires platform.Foundation imports
    return platform.Foundation.NSData()
}