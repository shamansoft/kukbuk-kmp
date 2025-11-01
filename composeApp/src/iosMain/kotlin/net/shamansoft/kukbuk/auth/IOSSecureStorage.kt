package net.shamansoft.kukbuk.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.*
import platform.CoreFoundation.*

/**
 * iOS implementation of SecureStorage using Keychain Services.
 *
 * Uses iOS Keychain for encrypted storage of sensitive authentication data.
 * Data is encrypted and protected by the iOS secure enclave.
 */
@OptIn(ExperimentalForeignApi::class)
class IOSSecureStorage : SecureStorage {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val SERVICE_NAME = "net.shamansoft.kukbuk.auth"
        private const val KEY_AUTH_USER = "auth_user"
        private const val KEY_AUTH_TOKENS = "auth_tokens"
    }

    override suspend fun storeTokens(tokens: AuthTokens) = withContext(Dispatchers.IO) {
        try {
            val tokenString = json.encodeToString(tokens)
            saveToKeychain(KEY_AUTH_TOKENS, tokenString)
            println("IOSSecureStorage: Tokens stored securely in Keychain")
        } catch (e: Exception) {
            println("IOSSecureStorage: Failed to store tokens: ${e.message}")
            throw e
        }
    }

    override suspend fun getTokens(): AuthTokens? = withContext(Dispatchers.IO) {
        try {
            val tokenString = readFromKeychain(KEY_AUTH_TOKENS)
            tokenString?.let {
                try {
                    json.decodeFromString<AuthTokens>(it)
                } catch (e: Exception) {
                    println("IOSSecureStorage: Failed to deserialize tokens: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("IOSSecureStorage: Failed to retrieve tokens: ${e.message}")
            null
        }
    }

    override suspend fun clearTokens() = withContext(Dispatchers.IO) {
        try {
            deleteFromKeychain(KEY_AUTH_TOKENS)
            println("IOSSecureStorage: Tokens cleared from Keychain")
        } catch (e: Exception) {
            println("IOSSecureStorage: Failed to clear tokens: ${e.message}")
        }
    }

    override suspend fun storeUser(user: AuthUser) = withContext(Dispatchers.IO) {
        try {
            val userString = json.encodeToString(user)
            saveToKeychain(KEY_AUTH_USER, userString)
            println("IOSSecureStorage: User info stored securely in Keychain")
        } catch (e: Exception) {
            println("IOSSecureStorage: Failed to store user: ${e.message}")
            throw e
        }
    }

    override suspend fun getUser(): AuthUser? = withContext(Dispatchers.IO) {
        try {
            val userString = readFromKeychain(KEY_AUTH_USER)
            userString?.let {
                try {
                    json.decodeFromString<AuthUser>(it)
                } catch (e: Exception) {
                    println("IOSSecureStorage: Failed to deserialize user: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("IOSSecureStorage: Failed to retrieve user: ${e.message}")
            null
        }
    }

    override suspend fun clearUser() = withContext(Dispatchers.IO) {
        try {
            deleteFromKeychain(KEY_AUTH_USER)
            println("IOSSecureStorage: User info cleared from Keychain")
        } catch (e: Exception) {
            println("IOSSecureStorage: Failed to clear user: ${e.message}")
        }
    }

    override suspend fun getValidAccessToken(): String? {
        return getTokens()?.accessToken
    }

    /**
     * Save a string value to iOS Keychain.
     */
    private fun saveToKeychain(key: String, value: String) {
        memScoped {
            // Convert string to NSData
            val nsString = NSString.create(string = value)
            val data = nsString.dataUsingEncoding(NSUTF8StringEncoding)
                ?: throw IllegalStateException("Failed to encode data")

            // First, try to update existing item
            val updated = updateKeychainItem(key, data)

            // If update failed (item doesn't exist), add new item
            if (!updated) {
                addKeychainItem(key, data)
            }
        }
    }

    /**
     * Read a string value from iOS Keychain.
     */
    private fun readFromKeychain(key: String): String? {
        return memScoped {
            val query = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(SERVICE_NAME))
            CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(key))
            CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
            CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)

            CFRelease(query)

            if (status == errSecSuccess) {
                val data = CFBridgingRelease(result.value) as? NSData
                data?.let {
                    NSString.create(data = it, encoding = NSUTF8StringEncoding) as? String
                }
            } else {
                null
            }
        }
    }

    /**
     * Delete an item from iOS Keychain.
     */
    private fun deleteFromKeychain(key: String) {
        memScoped {
            val query = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(SERVICE_NAME))
            CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(key))

            SecItemDelete(query)
            CFRelease(query)
        }
    }

    /**
     * Add a new item to Keychain.
     */
    private fun addKeychainItem(key: String, data: NSData) {
        memScoped {
            val query = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(SERVICE_NAME))
            CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(key))
            CFDictionaryAddValue(query, kSecValueData, CFBridgingRetain(data))
            CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlock)

            val status = SecItemAdd(query, null)
            CFRelease(query)

            if (status != errSecSuccess) {
                throw SecurityException("Failed to add item to Keychain: $status")
            }
        }
    }

    /**
     * Update an existing item in Keychain.
     * Returns true if successful, false if item doesn't exist.
     */
    private fun updateKeychainItem(key: String, data: NSData): Boolean {
        memScoped {
            val query = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(SERVICE_NAME))
            CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(key))

            val attributes = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionaryAddValue(attributes, kSecValueData, CFBridgingRetain(data))

            val status = SecItemUpdate(query, attributes)

            CFRelease(query)
            CFRelease(attributes)

            return status == errSecSuccess
        }
    }

    /**
     * Clear all secure data (for logout or account removal).
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        try {
            clearTokens()
            clearUser()
            println("IOSSecureStorage: All secure data cleared from Keychain")
        } catch (e: Exception) {
            println("IOSSecureStorage: Failed to clear all data: ${e.message}")
        }
    }
}
