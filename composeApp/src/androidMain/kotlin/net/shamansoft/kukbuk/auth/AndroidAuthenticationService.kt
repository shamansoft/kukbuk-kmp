package net.shamansoft.kukbuk.auth

import android.content.Context
import android.accounts.Account
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.shamansoft.kukbuk.MainActivity
import net.shamansoft.kukbuk.R
import net.shamansoft.kukbuk.util.Logger

/**
 * Android implementation of AuthenticationService using the modern Credential Manager API.
 *
 * This implementation:
 * - Uses Credential Manager for Google Sign-In
 * - Uses GoogleAuthUtil to get OAuth access tokens for Google Drive API
 * - Stores tokens securely in EncryptedSharedPreferences
 *
 * Note: This implementation does not support true token refresh. Users will need to
 * re-authenticate when tokens expire (typically after 1 hour).
 */
class AndroidAuthenticationService(
    private val context: Context,
    private val activity: MainActivity
) : AuthenticationService {

    private val _authenticationState =
        MutableStateFlow<AuthenticationState>(AuthenticationState.Loading)
    override val authenticationState: StateFlow<AuthenticationState> =
        _authenticationState.asStateFlow()

    private val credentialManager = CredentialManager.create(context)

    override suspend fun signInWithGoogle(): AuthResult = withContext(Dispatchers.Main) {
        try {
            val serverClientId = context.getString(R.string.google_web_client_id)

            // Build Google ID option with server auth code for refresh tokens
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Allow account picker
                .setServerClientId(serverClientId)
                .setNonce(null) // Optional nonce for security
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Request credentials using Credential Manager
            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )

            handleSignInResult(result, serverClientId)

        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            Logger.e("AndroidAuth", "Sign-in cancelled by user")
            AuthResult.Cancelled
        } catch (e: androidx.credentials.exceptions.NoCredentialException) {
            Logger.e("AndroidAuth", "No credentials available: ${e.message}")
            AuthResult.Error("No Google account found on device")
        } catch (e: Exception) {
            Logger.e("AndroidAuth", "Sign-in failed: ${e.message}")
            AuthResult.Error("Sign in failed: ${e.message}")
        }
    }

    private suspend fun handleSignInResult(
        result: GetCredentialResponse,
        serverClientId: String
    ): AuthResult {
        return try {
            when (val credential = result.credential) {
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)

                        // Extract user information
                        val user = AuthUser(
                            id = googleIdTokenCredential.id,
                            email = googleIdTokenCredential.id, // Email is the ID
                            displayName = googleIdTokenCredential.displayName,
                            photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                        )

                        // Get actual OAuth access token for Google Drive API
                        // The ID token is for identity, but we need an access token for API calls
                        Logger.d("AndroidAuth", "Getting OAuth access token for Google Drive...")

                        return try {
                            val accessToken = withContext(Dispatchers.IO) {
                                getOAuthAccessToken(googleIdTokenCredential.id)
                            }

                            val tokens = AuthTokens(
                                accessToken = accessToken,
                                refreshToken = accessToken, // Use same token (no real refresh capability)
                                expiresAt = kotlinx.datetime.Clock.System.now()
                                    .toEpochMilliseconds() + 3600_000
                            )

                            AuthResult.Success(user, tokens)
                        } catch (e: Exception) {
                            Logger.e("AndroidAuth", "Failed to get access token: ${e.message}")
                            AuthResult.Error("Failed to get access token: ${e.message}")
                        }
                    } else {
                        Logger.e("AndroidAuth", "Unexpected credential type: ${credential.type}")
                        AuthResult.Error("Unexpected credential type")
                    }
                }
                else -> {
                    Logger.e("AndroidAuth", "Unexpected credential class: ${credential::class}")
                    AuthResult.Error("Unexpected credential format")
                }
            }
        } catch (e: GoogleIdTokenParsingException) {
            Logger.e("AndroidAuth", "Invalid Google ID token: ${e.message}")
            AuthResult.Error("Invalid Google credential")
        } catch (e: Exception) {
            Logger.e("AndroidAuth", "Failed to process sign-in result: ${e.message}")
            AuthResult.Error("Failed to process sign-in: ${e.message}")
        }
    }

    /**
     * Token refresh is not supported in this implementation.
     * Returns error to trigger re-authentication when token expires.
     */
    override suspend fun refreshToken(): AuthResult {
        Logger.i("AndroidAuth", "Token refresh not supported - require re-authentication")
        return AuthResult.Error("Token refresh not supported. Please sign in again.")
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            // Clear credentials from Credential Manager
            credentialManager.clearCredentialState(
                androidx.credentials.ClearCredentialStateRequest()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e("AndroidAuth", "Sign out failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): AuthUser? {
        // With Credential Manager, we rely on stored user info
        val secureStorage = AndroidSecureStorage(context)
        return secureStorage.getUser()
    }

    override suspend fun isUserSignedIn(): Boolean {
        val secureStorage = AndroidSecureStorage(context)
        return secureStorage.getUser() != null && secureStorage.getTokens() != null
    }

    override suspend fun getValidAccessToken(): String? {
        val secureStorage = AndroidSecureStorage(context)
        val tokens = secureStorage.getTokens() ?: return null

        // Check if token is expired (with 5-minute buffer)
        val bufferTime = 300_000L // 5 minutes
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

        return if (now < tokens.expiresAt - bufferTime) {
            tokens.accessToken
        } else {
            null // Token expired, needs refresh
        }
    }

    override suspend fun validateToken(token: String): Boolean {
        return try {
            // Basic validation: check if it's a valid format
            token.isNotBlank() && token.length > 32
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get OAuth access token using GoogleAuthUtil.
     * This gets a proper access token that works with Google Drive API.
     */
    private fun getOAuthAccessToken(email: String): String {
        val account = Account(email, "com.google")
        val scope = "oauth2:https://www.googleapis.com/auth/drive.readonly https://www.googleapis.com/auth/drive.file"

        return try {
            GoogleAuthUtil.getToken(context, account, scope)
        } catch (e: Exception) {
            Logger.e("AndroidAuth", "GoogleAuthUtil.getToken failed: ${e.message}")
            throw e
        }
    }
}
