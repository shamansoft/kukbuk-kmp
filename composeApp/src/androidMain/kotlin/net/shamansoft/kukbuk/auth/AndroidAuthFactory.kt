package net.shamansoft.kukbuk.auth

import android.content.Context
import net.shamansoft.kukbuk.MainActivity

fun createAndroidAuthenticationRepository(context: Context, activity: MainActivity): AuthenticationRepository {
    val authService = AuthServiceFactory.createAuthenticationService(context, activity)
    val secureStorage = AuthServiceFactory.createSecureStorage(context)
    return AuthenticationRepository(authService, secureStorage)
}