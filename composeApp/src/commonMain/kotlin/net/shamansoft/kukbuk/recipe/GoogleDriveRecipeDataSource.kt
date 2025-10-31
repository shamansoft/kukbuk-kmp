package net.shamansoft.kukbuk.recipe

import net.shamansoft.kukbuk.drive.DriveResult
import net.shamansoft.kukbuk.drive.GoogleDriveService

/**
 * Adapter that wraps GoogleDriveService to implement the RecipeDataSource interface.
 * This allows the RecipeRepository to work with the abstraction instead of being
 * tightly coupled to Google Drive.
 */
class GoogleDriveRecipeDataSource(
    private val driveService: GoogleDriveService
) : RecipeDataSource {

    override suspend fun listRecipeFiles(): DataSourceResult<List<RecipeFile>> {
        return when (val result = driveService.listFilesInKukbukFolder()) {
            is DriveResult.Success -> {
                val recipeFiles = result.data.map { driveFile ->
                    RecipeFile(
                        id = driveFile.id,
                        name = driveFile.name,
                        modifiedTime = driveFile.modifiedTime ?: ""
                    )
                }
                DataSourceResult.Success(recipeFiles)
            }
            is DriveResult.Error -> {
                DataSourceResult.Error(result.message)
            }
            is DriveResult.Loading -> {
                DataSourceResult.Loading
            }
        }
    }

    override suspend fun listRecipeFilesPaginated(
        pageSize: Int,
        pageToken: String?
    ): DataSourceResult<RecipeFilesPage> {
        return when (val result = driveService.listFilesInKukbukFolderPaginated(pageSize, pageToken)) {
            is DriveResult.Success -> {
                val recipeFiles = result.data.files.map { driveFile ->
                    RecipeFile(
                        id = driveFile.id,
                        name = driveFile.name,
                        modifiedTime = driveFile.modifiedTime ?: ""
                    )
                }
                DataSourceResult.Success(
                    RecipeFilesPage(
                        files = recipeFiles,
                        nextPageToken = result.data.nextPageToken,
                        hasMore = result.data.hasMore
                    )
                )
            }
            is DriveResult.Error -> {
                DataSourceResult.Error(result.message)
            }
            is DriveResult.Loading -> {
                DataSourceResult.Loading
            }
        }
    }

    override suspend fun getFileContent(fileId: String): DataSourceResult<String> {
        return when (val result = driveService.downloadFileContent(fileId)) {
            is DriveResult.Success -> {
                DataSourceResult.Success(result.data)
            }
            is DriveResult.Error -> {
                DataSourceResult.Error(result.message)
            }
            is DriveResult.Loading -> {
                DataSourceResult.Loading
            }
        }
    }
}
