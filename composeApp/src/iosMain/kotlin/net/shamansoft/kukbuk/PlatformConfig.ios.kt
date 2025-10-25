package net.shamansoft.kukbuk

import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * iOS implementation of platform configuration.
 *
 * For local development:
 * - Files can be placed in the app's Documents directory
 * - Or bundled as resources in the app (requires Xcode configuration)
 */
actual fun getLocalRecipesPath(): String? {
    return if (isDebugBuild()) {
        // Use the app's Documents directory
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        )
        val documentsDirectory = paths.firstOrNull() as? String
        documentsDirectory?.let { "$it/kukbuk" }
    } else {
        null
    }
}

actual fun isDebugBuild(): Boolean {
    // In iOS, we can check if we're in DEBUG mode using compiler flags
    // This is set via Xcode build configurations
    return platform.Foundation.NSBundle.mainBundle.objectForInfoDictionaryKey("DEBUG_MODE") != null
            || isSimulator()
}

private fun isSimulator(): Boolean {
    // Alternative: detect if running on simulator
    return platform.Foundation.NSProcessInfo.processInfo.environment["SIMULATOR_DEVICE_NAME"] != null
}

actual fun getPlatformFileSystem(): okio.FileSystem {
    return okio.FileSystem.SYSTEM
}
