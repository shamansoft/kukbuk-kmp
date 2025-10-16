package net.shamansoft.kukbuk.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.shamansoft.kukbuk.MainActivity
import net.shamansoft.kukbuk.R
import net.shamansoft.kukbuk.util.Logger
import kotlin.coroutines.resume

class AndroidAuthenticationService(
    private val context: Context,
    private val activity: MainActivity
) : AuthenticationService {

    private val _authenticationState =
        MutableStateFlow<AuthenticationState>(AuthenticationState.Loading)
    override val authenticationState: StateFlow<AuthenticationState> =
        _authenticationState.asStateFlow()

    private val googleSignInClient: GoogleSignInClient
    private val signInLauncher: ActivityResultLauncher<Intent>
    private var pendingSignInContinuation: ((Result<GoogleSignInAccount>) -> Unit)? = null

    init {
        // Configure Google Sign-In with OAuth scopes
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(context.getString(R.string.google_web_client_id))
            .requestServerAuthCode(context.getString(R.string.google_web_client_id))
            // Request OAuth scopes for Google Drive API
            .requestScopes(
                Scope("https://www.googleapis.com/auth/drive.readonly"),
                Scope("https://www.googleapis.com/auth/drive.file")
            )
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)

        // Get the launcher from MainActivity (already registered)
        signInLauncher = activity.getSignInLauncher()

        // Set the callback for sign-in results
        activity.signInResultCallback = { data ->
            handleSignInActivityResult(data)
        }
    }

    override suspend fun signInWithGoogle(): AuthResult = suspendCancellableCoroutine { continuation ->
        try {
            // Sign out first to ensure we get fresh tokens with proper scopes
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                pendingSignInContinuation = { result ->
                    result.fold(
                        onSuccess = { account ->
                            CoroutineScope(Dispatchers.IO).launch {
                                val authResult = convertAccountToAuthResult(account)
                                continuation.resume(authResult)
                            }
                        },
                        onFailure = { error ->
                            Logger.e("AndroidAuth", "Sign-in failed: ${error.message}")
                            continuation.resume(AuthResult.Error(error.message ?: "Sign in failed"))
                        }
                    )
                }

                signInLauncher?.launch(signInIntent)
            }
        } catch (e: Exception) {
            Logger.e("AndroidAuth", "Exception in signInWithGoogle: ${e.message}")
            continuation.resume(AuthResult.Error("Sign in failed: ${e.message}"))
        }
    }

    private fun handleSignInActivityResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)

            pendingSignInContinuation?.invoke(Result.success(account))
            pendingSignInContinuation = null

        } catch (e: ApiException) {
            when (e.statusCode) {
                12501 -> { // USER_CANCELED
                    pendingSignInContinuation?.invoke(Result.failure(Exception("Sign in cancelled")))
                }
                else -> {
                    Logger.e("AndroidAuth", "Sign-in API error: ${e.statusCode} - ${e.message}")
                    pendingSignInContinuation?.invoke(Result.failure(e))
                }
            }
            pendingSignInContinuation = null
        } catch (e: Exception) {
            Logger.e("AndroidAuth", "Exception processing sign-in: ${e.message}")
            pendingSignInContinuation?.invoke(Result.failure(e))
            pendingSignInContinuation = null
        }
    }

    private suspend fun convertAccountToAuthResult(account: GoogleSignInAccount): AuthResult {
        val user = AuthUser(
            id = account.id ?: account.email ?: "",
            email = account.email ?: "",
            displayName = account.displayName,
            photoUrl = account.photoUrl?.toString()
        )

        return try {
            if (account.account == null) {
                Logger.e("AndroidAuth", "No Android Account available")
                return AuthResult.Error("No account available for token request")
            }

            // Scope format for GoogleAuthUtil: "oauth2:scope1 scope2"
            val scope = "oauth2:https://www.googleapis.com/auth/drive.readonly https://www.googleapis.com/auth/drive.file"

            // Get OAuth access token on IO dispatcher (network operation)
            val accessToken = withContext(Dispatchers.IO) {
                try {
                    GoogleAuthUtil.getToken(context, account.account!!, scope)
                } catch (e: Exception) {
                    Logger.e("AndroidAuth", "Failed to get OAuth token: ${e.message}")
                    throw e
                }
            }

            val tokens = AuthTokens(
                accessToken = accessToken,
                refreshToken = account.serverAuthCode,
                expiresAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() + 3600_000 // 1 hour
            )

            AuthResult.Success(user, tokens)

        } catch (e: Exception) {
            Logger.e("AndroidAuth", "Failed to create auth result: ${e.message}")
            AuthResult.Error("Failed to get OAuth access token: ${e.message}")
        }
    }

    companion object {
        private const val REQUEST_CODE_OAUTH = 9001
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            googleSignInClient.signOut().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e("AndroidAuth", "Sign out failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): AuthUser? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account?.let {
            AuthUser(
                id = it.id ?: it.email ?: "",
                email = it.email ?: "",
                displayName = it.displayName,
                photoUrl = it.photoUrl?.toString()
            )
        }
    }

    override suspend fun isUserSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && !account.isExpired
    }

    override suspend fun getValidAccessToken(): String? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null || account.isExpired) {
            return null
        }
        return account.idToken
    }

    override suspend fun refreshToken(): AuthResult {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            return AuthResult.Error("No account to refresh")
        }

        // Token refresh requires server auth code exchange implementation
        return AuthResult.Error("Token refresh not yet implemented - please sign in again")
    }

    override suspend fun validateToken(token: String): Boolean {
        return try {
            token.isNotBlank() && token.startsWith("eyJ") // Basic JWT format check
        } catch (e: Exception) {
            false
        }
    }
}

// Extension function to convert Tasks to coroutines
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { exception ->
            continuation.cancel(exception)
        }
    }
