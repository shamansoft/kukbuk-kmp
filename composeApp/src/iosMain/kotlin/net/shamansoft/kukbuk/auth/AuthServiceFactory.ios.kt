package net.shamansoft.kukbuk.auth

actual object AuthServiceFactory {
    actual fun createAuthenticationService(): AuthenticationService {
        return IOSAuthenticationService()
    }
    
    actual fun createSecureStorage(): SecureStorage {
        return IOSSecureStorage()
    }
}