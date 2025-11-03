package net.shamansoft.kukbuk.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.shamansoft.kukbuk.util.Logger

/**
 * Android implementation of SecureStorage using EncryptedSharedPreferences.
 *
 * Uses AES256-GCM encryption via Android Keystore for secure token storage.
 * This provides encryption at rest for sensitive authentication data.
 */
class AndroidSecureStorage(private val context: Context) : SecureStorage {

    private val json = Json { ignoreUnknownKeys = true }

    private val encryptedPreferences: SharedPreferences by lazy {
        try {
            // Create or retrieve master key from Android Keystore
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            // Create encrypted shared preferences
            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Logger.e("AndroidSecureStorage", "Failed to create encrypted preferences: ${e.message}")
            throw SecurityException("Failed to initialize secure storage", e)
        }
    }

    companion object {
        private const val ENCRYPTED_PREFS_FILE_NAME = "kukbuk_secure_auth"
        private const val KEY_AUTH_USER = "auth_user"
        private const val KEY_AUTH_TOKENS = "auth_tokens"
    }

    override suspend fun storeTokens(tokens: AuthTokens) = withContext(Dispatchers.IO) {
        try {
            val tokenString = json.encodeToString(tokens)
            encryptedPreferences.edit()
                .putString(KEY_AUTH_TOKENS, tokenString)
                .apply()
            Logger.d("AndroidSecureStorage", "Tokens stored securely")
        } catch (e: Exception) {
            Logger.e("AndroidSecureStorage", "Failed to store tokens: ${e.message}")
            throw e
        }
    }

    override suspend fun getTokens(): AuthTokens? = withContext(Dispatchers.IO) {
        try {
            val tokenString = encryptedPreferences.getString(KEY_AUTH_TOKENS, null)
            tokenString?.let {
                try {
                    json.decodeFromString<AuthTokens>(it)
                } catch (e: Exception) {
                    Logger.e("AndroidSecureStorage", "Failed to deserialize tokens: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Logger.e("AndroidSecureStorage", "Failed to retrieve tokens: ${e.message}")
            null
        }
    }

    override suspend fun clearTokens() = withContext(Dispatchers.IO) {
        try {
            encryptedPreferences.edit()
                .remove(KEY_AUTH_TOKENS)
                .apply()
            Logger.d("AndroidSecureStorage", "Tokens cleared")
        } catch (e: Exception) {
            Logger.e("AndroidSecureStorage", "Failed to clear tokens: ${e.message}")
        }
    }

    override suspend fun storeUser(user: AuthUser) = withContext(Dispatchers.IO) {
        try {
            val userString = json.encodeToString(user)
            encryptedPreferences.edit()
                .putString(KEY_AUTH_USER, userString)
                .apply()
            Logger.d("AndroidSecureStorage", "User info stored securely")
        } catch (e: Exception) {
            Logger.e("AndroidSecureStorage", "Failed to store user: ${e.message}")
            throw e
        }
    }

    override suspend fun getUser(): AuthUser? = withContext(Dispatchers.IO) {
        try {
            val userString = encryptedPreferences.getString(KEY_AUTH_USER, null)
            userString?.let {
                try {
                    json.decodeFromString<AuthUser>(it)
                } catch (e: Exception) {
                    Logger.e("AndroidSecureStorage", "Failed to deserialize user: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Logger.e("AndroidSecureStorage", "Failed to retrieve user: ${e.message}")
            null
        }
    }

    override suspend fun clearUser() = withContext(Dispatchers.IO) {
        try {
            encryptedPreferences.edit()
                .remove(KEY_AUTH_USER)
                .apply()
            Logger.d("AndroidSecureStorage", "User info cleared")
        } catch (e: Exception) {
            Logger.e("AndroidSecureStorage", "Failed to clear user: ${e.message}")
        }
    }

    override suspend fun getValidAccessToken(): String? {
        return getTokens()?.accessToken
    }

    /**
     * Clear all secure data (for logout or account removal).
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        try {
            encryptedPreferences.edit()
                .clear()
                .apply()
            Logger.d("AndroidSecureStorage", "All secure data cleared")
        } catch (e: Exception) {
            Logger.e("AndroidSecureStorage", "Failed to clear all data: ${e.message}")
        }
    }
}
