package net.shamansoft.kukbuk.auth

import android.content.Context
import androidx.activity.ComponentActivity

actual object AuthServiceFactory {
    private var context: Context? = null
    private var activity: ComponentActivity? = null

    fun initialize(context: Context, activity: ComponentActivity) {
        this.context = context
        this.activity = activity
    }

    actual fun createAuthenticationService(): AuthenticationService {
        requireNotNull(context) { "AuthServiceFactory not initialized. Call initialize() first." }
        requireNotNull(activity) { "AuthServiceFactory not initialized. Call initialize() first." }

        return AndroidAuthenticationService(context!!, activity!!)
    }

    actual fun createSecureStorage(): SecureStorage {
        requireNotNull(context) { "AuthServiceFactory not initialized. Call initialize() first." }

        return AndroidSecureStorage(context!!)
    }
}