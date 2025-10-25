package net.shamansoft.kukbuk.di

import net.shamansoft.kukbuk.MainActivity
import net.shamansoft.kukbuk.auth.AndroidAuthenticationService
import net.shamansoft.kukbuk.auth.AndroidSecureStorage
import net.shamansoft.kukbuk.auth.AuthenticationRepository
import net.shamansoft.kukbuk.auth.AuthenticationService
import net.shamansoft.kukbuk.auth.SecureStorage
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android-specific auth module that provides Context and Activity dependencies
 * Note: Requires MainActivity to be provided in Koin context
 */
val androidAuthModule: Module = module {
    single<AuthenticationService> {
        val activity: MainActivity = get()
        AndroidAuthenticationService(activity.applicationContext, activity)
    }
    single<SecureStorage> {
        val activity: MainActivity = get()
        AndroidSecureStorage(activity.applicationContext)
    }
    single { AuthenticationRepository(get(), get()) }
}

/**
 * Returns all modules for Android production configuration (Google Drive)
 */
fun getAndroidProductionModules(): List<Module> = listOf(
    androidAuthModule,
    productionDataModule,
    recipeModule,
    viewModelModule
)

/**
 * Returns all modules for Android local development configuration (local files)
 */
fun getAndroidLocalDevModules(): List<Module> = listOf(
    androidAuthModule,
    localDevDataModule,
    recipeModule,
    viewModelModule
)

/**
 * Android actual implementation
 */
actual fun getPlatformProductionModules(): List<Module> = getAndroidProductionModules()

actual fun getPlatformLocalDevModules(): List<Module> = getAndroidLocalDevModules()
