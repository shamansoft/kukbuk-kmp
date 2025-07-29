package net.shamansoft.kukbuk.auth

import android.content.Context
import androidx.activity.ComponentActivity

fun createAndroidAuthenticationRepository(context: Context, activity: ComponentActivity): AuthenticationRepository {
    val authService = AuthServiceFactory.createAuthenticationService(context, activity)
    val secureStorage = AuthServiceFactory.createSecureStorage(context)
    return AuthenticationRepository(authService, secureStorage)
}