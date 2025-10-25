package net.shamansoft.kukbuk.recipe

/**
 * Abstraction for recipe data sources (Google Drive, local files, etc.)
 * This allows switching between different storage backends without changing the repository.
 */
interface RecipeDataSource {
    /**
     * Lists all available recipe files.
     * @return DataSourceResult with list of RecipeFile objects
     */
    suspend fun listRecipeFiles(): DataSourceResult<List<RecipeFile>>

    /**
     * Downloads/reads the content of a specific recipe file.
     * @param fileId Unique identifier for the file
     * @return DataSourceResult with the file content as a string
     */
    suspend fun getFileContent(fileId: String): DataSourceResult<String>
}

/**
 * Represents a recipe file from any data source.
 * @property id Unique identifier for the file
 * @property name File name (e.g., "Chocolate Cake.yaml")
 * @property modifiedTime Last modification timestamp (ISO 8601 format or empty string)
 */
data class RecipeFile(
    val id: String,
    val name: String,
    val modifiedTime: String = ""
)

/**
 * Result type for data source operations.
 * Similar to DriveResult but more generic.
 */
sealed class DataSourceResult<out T> {
    data class Success<T>(val data: T) : DataSourceResult<T>()
    data class Error(val message: String) : DataSourceResult<Nothing>()
    data object Loading : DataSourceResult<Nothing>()
}
