package net.shamansoft.kukbuk.di

import net.shamansoft.kukbuk.db.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS-specific database module that provides DatabaseDriverFactory
 */
val iosDatabaseModule: Module = module {
    single { DatabaseDriverFactory() }
}

/**
 * Returns all modules for iOS production configuration (Google Drive)
 */
fun getIosProductionModules(): List<Module> = listOf(
    authModule,
    iosDatabaseModule,
    databaseModule,
    productionDataModule,
    recipeModule,
    viewModelModule
)

/**
 * Returns all modules for iOS local development configuration (local files)
 */
fun getIosLocalDevModules(): List<Module> = listOf(
    authModule,
    iosDatabaseModule,
    databaseModule,
    localDevDataModule,
    recipeModule,
    viewModelModule
)

/**
 * iOS actual implementation
 */
actual fun getPlatformProductionModules(): List<Module> = getIosProductionModules()

actual fun getPlatformLocalDevModules(): List<Module> = getIosLocalDevModules()
