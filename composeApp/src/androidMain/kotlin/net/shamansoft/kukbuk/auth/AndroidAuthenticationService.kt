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
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
    private val tokenExchange: GoogleTokenExchange

    init {
        Logger.d("AndroidAuth", "Initializing with OAuth scopes for Drive API")

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

        // Initialize HTTP client for token exchange
        val httpClient = HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
        }

        tokenExchange = GoogleTokenExchange(
            httpClient = httpClient,
            clientId = context.getString(R.string.google_web_client_id)
        )

        // Get the launcher from MainActivity (already registered)
        signInLauncher = activity.getSignInLauncher()

        // Set the callback for sign-in results
        activity.signInResultCallback = { data ->
            Logger.d("AndroidAuth", "Sign-in activity result received")
            handleSignInActivityResult(data)
        }

        Logger.d("AndroidAuth", "Google Sign-In client configured with Drive API scopes")
    }

    override suspend fun signInWithGoogle(): AuthResult = suspendCancellableCoroutine { continuation ->
        Logger.d("AndroidAuth", "signInWithGoogle() called")

        try {
            // Sign out first to ensure we get fresh tokens with proper scopes
            googleSignInClient.signOut().addOnCompleteListener {
                Logger.d("AndroidAuth", "Previous session cleared, starting new sign-in")

                val signInIntent = googleSignInClient.signInIntent
                pendingSignInContinuation = { result ->
                    result.fold(
                        onSuccess = { account ->
                            Logger.d("AndroidAuth", "Converting account to AuthResult")
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

                Logger.d("AndroidAuth", "Launching sign-in intent")
                signInLauncher?.launch(signInIntent)
            }
        } catch (e: Exception) {
            Logger.e("AndroidAuth", "Exception in signInWithGoogle: ${e.message}")
            e.printStackTrace()
            continuation.resume(AuthResult.Error("Sign in failed: ${e.message}"))
        }
    }

    private fun handleSignInActivityResult(data: Intent?) {
        Logger.d("AndroidAuth", "Processing sign-in result")

        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)

            Logger.d("AndroidAuth", "Successfully signed in: ${account.email}")
            Logger.d("AndroidAuth", "Account has server auth code: ${account.serverAuthCode != null}")
            Logger.d("AndroidAuth", "Account scopes: ${account.grantedScopes.joinToString(", ") { it.scopeUri }}")

            pendingSignInContinuation?.invoke(Result.success(account))
            pendingSignInContinuation = null

        } catch (e: ApiException) {
            Logger.e("AndroidAuth", "ApiException in sign-in: ${e.statusCode} - ${e.message}")
            e.printStackTrace()

            when (e.statusCode) {
                12501 -> { // USER_CANCELED
                    Logger.d("AndroidAuth", "User cancelled sign-in")
                    pendingSignInContinuation?.invoke(Result.failure(Exception("Sign in cancelled")))
                }
                else -> {
                    pendingSignInContinuation?.invoke(Result.failure(e))
                }
            }
            pendingSignInContinuation = null
        } catch (e: Exception) {
            Logger.e("AndroidAuth", "Exception processing sign-in result: ${e.message}")
            e.printStackTrace()
            pendingSignInContinuation?.invoke(Result.failure(e))
            pendingSignInContinuation = null
        }
    }

    private suspend fun convertAccountToAuthResult(account: GoogleSignInAccount): AuthResult {
        Logger.d("AndroidAuth", "Converting GoogleSignInAccount to AuthResult")
        Logger.d("AndroidAuth", "Email: ${account.email}")
        Logger.d("AndroidAuth", "Display Name: ${account.displayName}")
        Logger.d("AndroidAuth", "ID Token: ${if (account.idToken != null) "present" else "null"}")
        Logger.d("AndroidAuth", "Server Auth Code: ${if (account.serverAuthCode != null) "present (${account.serverAuthCode?.length} chars)" else "null"}")

        val user = AuthUser(
            id = account.id ?: account.email ?: "",
            email = account.email ?: "",
            displayName = account.displayName,
            photoUrl = account.photoUrl?.toString()
        )

        // Get OAuth access token using GoogleAuthUtil
        Logger.d("AndroidAuth", "Requesting OAuth access token for Drive API scopes using GoogleAuthUtil...")

        return try {
            if (account.account == null) {
                Logger.e("AndroidAuth", "No Android Account available from GoogleSignInAccount")
                return AuthResult.Error("No account available for token request")
            }

            // Scope format for GoogleAuthUtil: "oauth2:scope1 scope2"
            val scope = "oauth2:https://www.googleapis.com/auth/drive.readonly https://www.googleapis.com/auth/drive.file"
            Logger.d("AndroidAuth", "Requesting token with scope: $scope")

            // Get OAuth access token on IO dispatcher (network operation)
            val accessToken = kotlinx.coroutines.withContext(Dispatchers.IO) {
                try {
                    val token = GoogleAuthUtil.getToken(context, account.account!!, scope)
                    Logger.d("AndroidAuth", "Successfully obtained OAuth access token (length: ${token.length})")
                    token
                } catch (e: Exception) {
                    Logger.e("AndroidAuth", "GoogleAuthUtil.getToken failed: ${e.message}")
                    e.printStackTrace()
                    throw e
                }
            }

            val tokens = AuthTokens(
                accessToken = accessToken,
                refreshToken = account.serverAuthCode, // Keep server auth code for potential refresh
                expiresAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() + 3600_000 // 1 hour
            )

            Logger.d("AndroidAuth", "Created auth tokens with OAuth access token")
            AuthResult.Success(user, tokens)

        } catch (e: Exception) {
            Logger.e("AndroidAuth", "Exception getting OAuth token: ${e.message}")
            e.printStackTrace()
            AuthResult.Error("Failed to get OAuth access token: ${e.message}")
        }
    }

    companion object {
        private const val REQUEST_CODE_OAUTH = 9001
    }

    override suspend fun signOut(): Result<Unit> {
        Logger.d("AndroidAuth", "signOut() called")
        return try {
            googleSignInClient.signOut().await()
            Logger.d("AndroidAuth", "Successfully signed out")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e("AndroidAuth", "Sign out failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): AuthUser? {
        Logger.d("AndroidAuth", "getCurrentUser() called")
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account?.let {
            Logger.d("AndroidAuth", "Found signed-in user: ${it.email}")
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
        val isSignedIn = account != null && !account.isExpired
        Logger.d("AndroidAuth", "isUserSignedIn: $isSignedIn")
        return isSignedIn
    }

    override suspend fun getValidAccessToken(): String? {
        Logger.d("AndroidAuth", "getValidAccessToken() called")
        val account = GoogleSignIn.getLastSignedInAccount(context)

        if (account == null) {
            Logger.d("AndroidAuth", "No signed-in account found")
            return null
        }

        if (account.isExpired) {
            Logger.d("AndroidAuth", "Account is expired")
            return null
        }

        val token = account.idToken
        Logger.d("AndroidAuth", "Returning access token (length: ${token?.length ?: 0})")
        return token
    }

    override suspend fun refreshToken(): AuthResult {
        Logger.d("AndroidAuth", "refreshToken() called")

        // Get the current account to access stored data
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            Logger.d("AndroidAuth", "No signed-in account found for refresh")
            return AuthResult.Error("No account to refresh")
        }

        // The refresh token should be stored in secure storage
        // For now, this is a placeholder - actual refresh token needs to come from storage
        Logger.d("AndroidAuth", "Note: Refresh token should be retrieved from secure storage")
        Logger.d("AndroidAuth", "Refresh functionality requires server auth code exchange implementation")

        return AuthResult.Error("Token refresh not yet implemented - please sign in again")
    }

    override suspend fun validateToken(token: String): Boolean {
        val isValid = try {
            token.isNotBlank() && token.startsWith("eyJ") // Basic JWT format check
        } catch (e: Exception) {
            false
        }
        Logger.d("AndroidAuth", "validateToken: $isValid")
        return isValid
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
