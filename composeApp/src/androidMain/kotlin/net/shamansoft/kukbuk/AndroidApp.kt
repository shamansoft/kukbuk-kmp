package net.shamansoft.kukbuk

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import net.shamansoft.kukbuk.di.getAndroidLocalDevModules
import net.shamansoft.kukbuk.di.getAndroidProductionModules
import net.shamansoft.kukbuk.util.Logger
import org.koin.compose.KoinApplication
import org.koin.dsl.module

/**
 * Android-specific app wrapper that provides MainActivity to Koin
 */
@Composable
fun AndroidApp() {
    val context = LocalContext.current
    val activity = context as? MainActivity
        ?: throw IllegalStateException("AndroidApp must be called from MainActivity")

    // Android-specific Koin initialization with MainActivity
    KoinApplication(
        application = {
            // Provide MainActivity instance to Koin
            val mainActivityModule = module {
                single { activity }
            }

            val modules = if (DataSourceConfig.isLocalMode()) {
                Logger.d("AndroidApp", "Using LOCAL data source (local files)")
                getAndroidLocalDevModules() + mainActivityModule
            } else {
                Logger.d("AndroidApp", "Using PRODUCTION data source (Google Drive)")
                getAndroidProductionModules() + mainActivityModule
            }
            modules(modules)
        }
    ) {
        // Call the common app content
        AppContent()
    }
}
