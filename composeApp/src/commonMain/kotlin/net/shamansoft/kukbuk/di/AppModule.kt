package net.shamansoft.kukbuk.di

import net.shamansoft.kukbuk.auth.AuthServiceFactory
import net.shamansoft.kukbuk.auth.AuthViewModel
import net.shamansoft.kukbuk.auth.AuthenticationRepository
import net.shamansoft.kukbuk.auth.AuthenticationService
import net.shamansoft.kukbuk.auth.SecureStorage
import net.shamansoft.kukbuk.cache.RecipeCache
import net.shamansoft.kukbuk.cache.SqlDelightRecipeCache
import net.shamansoft.kukbuk.db.DatabaseDriverFactory
import net.shamansoft.kukbuk.db.RecipeDatabase
import net.shamansoft.kukbuk.drive.GoogleDriveService
import net.shamansoft.kukbuk.drive.HttpGoogleDriveService
import net.shamansoft.kukbuk.getLocalRecipesPath
import net.shamansoft.kukbuk.getPlatformFileSystem
import net.shamansoft.kukbuk.recipe.GoogleDriveRecipeDataSource
import net.shamansoft.kukbuk.recipe.LocalFileRecipeDataSource
import net.shamansoft.kukbuk.recipe.RecipeDataSource
import net.shamansoft.kukbuk.recipe.RecipeDetailViewModel
import net.shamansoft.kukbuk.recipe.RecipeListViewModel
import net.shamansoft.kukbuk.recipe.RecipeRepository
import net.shamansoft.kukbuk.settings.SettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Authentication module - provides authentication-related dependencies
 */
val authModule = module {
    single<AuthenticationService> { AuthServiceFactory.createAuthenticationService() }
    single<SecureStorage> { AuthServiceFactory.createSecureStorage() }
    single { AuthenticationRepository(get(), get()) }
}

/**
 * Production data source module - uses Google Drive for recipe storage
 */
val productionDataModule = module {
    single<GoogleDriveService> { HttpGoogleDriveService(get()) }
    single<RecipeDataSource> { GoogleDriveRecipeDataSource(get()) }
}

/**
 * Local development data source module - reads recipes from local file system
 */
val localDevDataModule = module {
    single<RecipeDataSource> {
        val recipesPath = getLocalRecipesPath()
            ?: throw IllegalStateException("Local recipes path not available on this platform")
        LocalFileRecipeDataSource(recipesPath, getPlatformFileSystem())
    }
}

/**
 * Database module - provides database and cache dependencies
 * Note: DatabaseDriverFactory is provided by platform-specific modules
 */
val databaseModule = module {
    single { RecipeDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single<RecipeCache> { SqlDelightRecipeCache(get()) }
}

/**
 * Recipe module - provides recipe-related dependencies
 */
val recipeModule = module {
    single { RecipeRepository(get(), get(), get()) }
}

/**
 * ViewModel module - provides all ViewModels
 */
val viewModelModule = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::RecipeListViewModel)
    viewModelOf(::SettingsViewModel)

    // RecipeDetailViewModel requires a recipeId parameter
    // We'll use factory instead of viewModel
    viewModel { (recipeId: String) ->
        RecipeDetailViewModel(recipeId, get())
    }
}

/**
 * Returns all modules for production configuration (Google Drive)
 * Note: On Android, use getAndroidProductionModules() instead
 */
internal fun getCommonProductionModules(): List<Module> = listOf(
    authModule,
    databaseModule,
    productionDataModule,
    recipeModule,
    viewModelModule
)

/**
 * Returns all modules for local development configuration (local files)
 * Note: On Android, use getAndroidLocalDevModules() instead
 */
internal fun getCommonLocalDevModules(): List<Module> = listOf(
    authModule,
    databaseModule,
    localDevDataModule,
    recipeModule,
    viewModelModule
)

/**
 * Platform-specific function to get production modules
 */
expect fun getPlatformProductionModules(): List<Module>

/**
 * Platform-specific function to get local dev modules
 */
expect fun getPlatformLocalDevModules(): List<Module>
