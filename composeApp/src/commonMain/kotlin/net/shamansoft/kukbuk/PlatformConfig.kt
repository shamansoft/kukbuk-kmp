package net.shamansoft.kukbuk

import okio.FileSystem

/**
 * Returns the local directory path where recipe YAML files are stored.
 * This is used for local development and testing.
 *
 * @return Path to the local recipes directory, or null if not available on this platform
 */
expect fun getLocalRecipesPath(): String?

/**
 * Returns true if the app is running in debug/development mode.
 * This can be used to enable debug features like local file loading.
 */
expect fun isDebugBuild(): Boolean

/**
 * Returns the platform's file system instance.
 * This is needed because FileSystem.SYSTEM is expect/actual in Okio.
 */
expect fun getPlatformFileSystem(): FileSystem
