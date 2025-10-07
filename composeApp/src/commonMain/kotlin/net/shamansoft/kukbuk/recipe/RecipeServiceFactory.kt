package net.shamansoft.kukbuk.recipe

import net.shamansoft.kukbuk.auth.AuthenticationRepository
import net.shamansoft.kukbuk.drive.HttpGoogleDriveService

fun createRecipeListViewModel(authRepository: AuthenticationRepository): RecipeListViewModel {
    val driveService = HttpGoogleDriveService(authRepository)
    val recipeRepository = RecipeRepository(driveService)
    return RecipeListViewModel(recipeRepository)
}