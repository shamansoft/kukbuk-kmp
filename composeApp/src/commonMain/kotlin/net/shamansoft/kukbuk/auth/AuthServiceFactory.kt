package net.shamansoft.kukbuk.auth

// Platform-specific factory for creating authentication services
expect object AuthServiceFactory {
    fun createAuthenticationService(): AuthenticationService
    fun createSecureStorage(): SecureStorage
}

fun createAuthenticationRepository(): AuthenticationRepository {
    val authService = AuthServiceFactory.createAuthenticationService()
    val secureStorage = AuthServiceFactory.createSecureStorage()
    return AuthenticationRepository(authService, secureStorage)
}