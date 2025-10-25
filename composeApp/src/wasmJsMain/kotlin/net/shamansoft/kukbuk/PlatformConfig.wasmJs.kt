package net.shamansoft.kukbuk

/**
 * WASM/Web implementation of platform configuration.
 *
 * For local development on web:
 * - Could serve recipes from a local dev server
 * - Or embed them as resources
 * - For now, returning null (web will use Google Drive)
 */
actual fun getLocalRecipesPath(): String? {
    // Web/WASM doesn't have direct file system access
    // You could implement fetching from a local dev server here
    return null
}

actual fun isDebugBuild(): Boolean {
    // In WASM, we can check the build mode
    // For now, always return false (or implement based on window.location)
    return js("typeof window !== 'undefined' && window.location.hostname === 'localhost'") as Boolean
}

actual fun getPlatformFileSystem(): okio.FileSystem {
    return okio.FileSystem.SYSTEM
}
