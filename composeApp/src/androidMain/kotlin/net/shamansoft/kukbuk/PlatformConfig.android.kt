package net.shamansoft.kukbuk

import android.content.Context
import net.shamansoft.kukbuk.BuildConfig

// Internal storage context - set by MainActivity
private var appContext: Context? = null

fun initPlatformConfig(context: Context) {
    appContext = context
}

actual fun getLocalRecipesPath(): String? {
    return if (isDebugBuild()) {
        // Use app's internal files directory
        // Files go here: /data/data/net.shamansoft.kukbuk/files/recipes/
        val context = appContext ?: return null
        "${context.filesDir.absolutePath}/recipes"
    } else {
        null
    }
}

actual fun isDebugBuild(): Boolean {
    return BuildConfig.DEBUG
}

actual fun getPlatformFileSystem(): okio.FileSystem {
    return okio.FileSystem.SYSTEM
}
