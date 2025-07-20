package net.shamansoft.kukbuk.auth

// Platform-specific factory for creating authentication services
expect object AuthServiceFactory {
    fun createAuthenticationService(): AuthenticationService
    fun createSecureStorage(): SecureStorage
    
    fun createAuthenticationRepository(): AuthenticationRepository {
        val authService = createAuthenticationService()
        val secureStorage = createSecureStorage()
        return AuthenticationRepository(authService, secureStorage)
    }
}