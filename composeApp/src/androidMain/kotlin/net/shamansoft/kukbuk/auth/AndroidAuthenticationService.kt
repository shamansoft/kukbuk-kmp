package net.shamansoft.kukbuk.auth

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.shamansoft.kukbuk.R

class AndroidAuthenticationService(
    private val context: Context,
    private val activity: ComponentActivity
) : AuthenticationService {

    private val _authenticationState =
        MutableStateFlow<AuthenticationState>(AuthenticationState.Loading)
    override val authenticationState: StateFlow<AuthenticationState> =
        _authenticationState.asStateFlow()

    private val credentialManager = CredentialManager.create(context)

    override suspend fun signInWithGoogle(): AuthResult {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.google_web_client_id))
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activity,
            )

            handleSignInResult(result)
        } catch (e: GetCredentialException) {
            when {
                e.message?.contains("canceled", ignoreCase = true) == true -> {
                    AuthResult.Cancelled
                }
                else -> {
                    AuthResult.Error("Sign in failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            AuthResult.Error("Sign in failed: ${e.message}")
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse): AuthResult {
        return try {
            val credential = result.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            
            val user = AuthUser(
                id = googleIdTokenCredential.id,
                email = googleIdTokenCredential.id, // Google ID is typically the email
                displayName = googleIdTokenCredential.displayName,
                photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
            )

            val tokens = AuthTokens(
                accessToken = googleIdTokenCredential.idToken,
                refreshToken = null,
                expiresAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() + 3600_000 // 1 hour
            )

            AuthResult.Success(user, tokens)
        } catch (e: GoogleIdTokenParsingException) {
            AuthResult.Error("Failed to parse Google ID token: ${e.message}")
        } catch (e: Exception) {
            AuthResult.Error("Failed to process sign-in result: ${e.message}")
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            // With Credential Manager, sign out is typically handled by clearing stored tokens
            // The actual credential clearing is managed by the repository layer
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): AuthUser? {
        // With Credential Manager, user info is retrieved from stored tokens
        // This would typically be handled by the repository layer
        return null
    }

    override suspend fun isUserSignedIn(): Boolean {
        // With Credential Manager, sign-in status is determined by valid stored tokens
        // This would typically be handled by the repository layer
        return false
    }

    override suspend fun getValidAccessToken(): String? {
        // With Credential Manager, tokens are managed by the repository layer
        return null
    }

    override suspend fun refreshToken(): AuthResult {
        // Google ID tokens cannot be refreshed - user needs to re-authenticate
        // This is a limitation of the Google Identity Services library
        return AuthResult.Error("Token refresh not supported - please sign in again")
    }

    override suspend fun validateToken(token: String): Boolean {
        // For Google ID tokens, we can do basic validation
        // In a real implementation, you might want to verify with Google's tokeninfo endpoint
        return try {
            token.isNotBlank() && token.startsWith("eyJ") // Basic JWT format check
        } catch (e: Exception) {
            false
        }
    }
}