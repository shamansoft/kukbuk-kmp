package net.shamansoft.kukbuk.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")

class AndroidSecureStorage(private val context: Context) : SecureStorage {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val USER_KEY = stringPreferencesKey("auth_user")
        private val TOKENS_KEY = stringPreferencesKey("auth_tokens")
    }

    override suspend fun storeTokens(tokens: AuthTokens) {
        context.dataStore.edit { preferences ->
            preferences[TOKENS_KEY] = json.encodeToString(tokens)
        }
    }

    override suspend fun getTokens(): AuthTokens? {
        return context.dataStore.data.map { preferences ->
            preferences[TOKENS_KEY]?.let { tokenString ->
                try {
                    json.decodeFromString<AuthTokens>(tokenString)
                } catch (e: Exception) {
                    null
                }
            }
        }.first()
    }

    override suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKENS_KEY)
        }
    }

    override suspend fun storeUser(user: AuthUser) {
        context.dataStore.edit { preferences ->
            preferences[USER_KEY] = json.encodeToString(user)
        }
    }

    override suspend fun getUser(): AuthUser? {
        return context.dataStore.data.map { preferences ->
            preferences[USER_KEY]?.let { userString ->
                try {
                    json.decodeFromString<AuthUser>(userString)
                } catch (e: Exception) {
                    null
                }
            }
        }.first()
    }

    override suspend fun clearUser() {
        context.dataStore.edit { preferences ->
            preferences.remove(USER_KEY)
        }
    }
}