package net.shamansoft.kukbuk.recipe

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import net.shamansoft.kukbuk.util.Logger
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * Local file system implementation of RecipeDataSource.
 * Reads YAML recipe files from a local directory using Okio for cross-platform file access.
 *
 * @param recipesPath Absolute path to the directory containing recipe YAML files
 * @param fileSystem Okio FileSystem instance
 */
class LocalFileRecipeDataSource(
    private val recipesPath: String,
    private val fileSystem: FileSystem
) : RecipeDataSource {

    override suspend fun listRecipeFiles(): DataSourceResult<List<RecipeFile>> {
        return withContext(Dispatchers.Default) {
            try {
                val path = recipesPath.toPath()

                if (!fileSystem.exists(path)) {
                    Logger.e("LocalFileDataSource", "Directory does not exist: $recipesPath")
                    return@withContext DataSourceResult.Error("Local recipes directory not found: $recipesPath")
                }

                val files = fileSystem.list(path)
                    .filter { it.name.endsWith(".yaml") || it.name.endsWith(".yml") }
                    .map { filePath ->
                        val metadata = fileSystem.metadata(filePath)
                        RecipeFile(
                            id = filePath.toString(), // Use full path as ID
                            name = filePath.name,
                            modifiedTime = metadata.lastModifiedAtMillis?.let { millis ->
                                // Convert to ISO 8601 format similar to Google Drive
                                kotlinx.datetime.Instant.fromEpochMilliseconds(millis).toString()
                            } ?: ""
                        )
                    }
                    .sortedByDescending { it.modifiedTime }

                Logger.d("LocalFileDataSource", "Found ${files.size} recipe files in $recipesPath")
                DataSourceResult.Success(files)
            } catch (e: Exception) {
                val errorMsg = "Error listing local recipe files: ${e.message}"
                Logger.e("LocalFileDataSource", errorMsg)
                DataSourceResult.Error(errorMsg)
            }
        }
    }

    override suspend fun getFileContent(fileId: String): DataSourceResult<String> {
        return withContext(Dispatchers.Default) {
            try {
                val path = fileId.toPath()

                if (!fileSystem.exists(path)) {
                    Logger.e("LocalFileDataSource", "File does not exist: $fileId")
                    return@withContext DataSourceResult.Error("Recipe file not found: $fileId")
                }

                val content = fileSystem.read(path) {
                    readUtf8()
                }

                Logger.d("LocalFileDataSource", "Read ${content.length} chars from ${path.name}")
                DataSourceResult.Success(content)
            } catch (e: Exception) {
                val errorMsg = "Error reading recipe file: ${e.message}"
                Logger.e("LocalFileDataSource", errorMsg)
                DataSourceResult.Error(errorMsg)
            }
        }
    }
}
