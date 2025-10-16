package net.shamansoft.kukbuk.auth

import android.content.Context
import androidx.activity.ComponentActivity
import net.shamansoft.kukbuk.MainActivity

actual object AuthServiceFactory {
    actual fun createAuthenticationService(): AuthenticationService {
        throw IllegalStateException("Use createAuthenticationService(context, activity) on Android")
    }

    actual fun createSecureStorage(): SecureStorage {
        throw IllegalStateException("Use createSecureStorage(context) on Android")
    }

    fun createAuthenticationService(context: Context, activity: MainActivity): AuthenticationService {
        return AndroidAuthenticationService(context, activity)
    }

    fun createSecureStorage(context: Context): SecureStorage {
        return AndroidSecureStorage(context)
    }
}