package net.shamansoft.kukbuk.auth

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import net.shamansoft.kukbuk.R
import kotlin.coroutines.resume

class AndroidAuthenticationService(
    private val context: Context,
    private val activity: ComponentActivity
) : AuthenticationService {

    private val _authenticationState =
        MutableStateFlow<AuthenticationState>(AuthenticationState.Loading)
    override val authenticationState: StateFlow<AuthenticationState> =
        _authenticationState.asStateFlow()

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken(context.getString(R.string.google_web_client_id))
            .requestScopes(
                Scope("https://www.googleapis.com/auth/drive.file"),
                Scope("https://www.googleapis.com/auth/drive.appdata")
            )
            .build()

        GoogleSignIn.getClient(context, gso)
    }

    override suspend fun signInWithGoogle(): AuthResult =
        suspendCancellableCoroutine { continuation ->
            val signInIntent = googleSignInClient.signInIntent

            val launcher = activity.activityResultRegistry.register(
                "google_sign_in",
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    val account = task.getResult(ApiException::class.java)

                    val user = AuthUser(
                        id = account.id ?: "",
                        email = account.email ?: "",
                        displayName = account.displayName,
                        photoUrl = account.photoUrl?.toString()
                    )

                    val tokens = AuthTokens(
                        accessToken = account.idToken ?: "",
                        refreshToken = null,
                        expiresAt = kotlinx.datetime.Clock.System.now()
                            .toEpochMilliseconds() + 3600_000 // 1 hour
                    )

                    continuation.resume(AuthResult.Success(user, tokens))
                } catch (e: ApiException) {
                    when (e.statusCode) {
                        12501 -> continuation.resume(AuthResult.Cancelled) // User cancelled
                        else -> continuation.resume(AuthResult.Error("Sign in failed: ${e.message}"))
                    }
                } catch (e: Exception) {
                    continuation.resume(AuthResult.Error("Sign in failed: ${e.message}"))
                }
            }

            launcher.launch(signInIntent)

            continuation.invokeOnCancellation {
                launcher.unregister()
            }
        }

    override suspend fun signOut(): Result<Unit> {
        return try {
            googleSignInClient.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): AuthUser? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account?.let {
            AuthUser(
                id = it.id ?: "",
                email = it.email ?: "",
                displayName = it.displayName,
                photoUrl = it.photoUrl?.toString()
            )
        }
    }

    override suspend fun isUserSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }

    override suspend fun getValidAccessToken(): String? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account?.idToken
    }
}