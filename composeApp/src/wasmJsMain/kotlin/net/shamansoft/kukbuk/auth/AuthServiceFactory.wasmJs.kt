package net.shamansoft.kukbuk.auth

actual object AuthServiceFactory {
    actual fun createAuthenticationService(): AuthenticationService {
        return WasmAuthenticationService()
    }

    actual fun createSecureStorage(): SecureStorage {
        return WasmSecureStorage()
    }
}