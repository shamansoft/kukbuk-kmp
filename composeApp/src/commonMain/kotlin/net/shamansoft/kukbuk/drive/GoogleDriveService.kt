package net.shamansoft.kukbuk.drive

import kotlinx.serialization.Serializable

interface GoogleDriveService {
    suspend fun listFilesInKukbukFolder(): DriveResult<List<DriveFile>>

    /**
     * List files in kukbuk folder with pagination support
     * @param pageSize Number of files to return per page (default 10)
     * @param pageToken Token for the next page (null for first page)
     * @return Paginated result with files and pagination metadata
     */
    suspend fun listFilesInKukbukFolderPaginated(
        pageSize: Int = 10,
        pageToken: String? = null
    ): DriveResult<DriveFilesPage>

    suspend fun downloadFileContent(fileId: String): DriveResult<String>
    suspend fun searchFiles(query: String): DriveResult<List<DriveFile>>
}

@Serializable
data class DriveFile(
    val id: String,
    val name: String,
    val mimeType: String? = null,
    val size: Long? = null,
    val modifiedTime: String? = null,
    val parents: List<String> = emptyList(),
    val webViewLink: String? = null
)

@Serializable
data class DriveFilesResponse(
    val files: List<DriveFile>,
    val nextPageToken: String? = null
)

/**
 * Paginated response for file listing with metadata about pagination state
 */
data class DriveFilesPage(
    val files: List<DriveFile>,
    val nextPageToken: String?,
    val hasMore: Boolean
) {
    companion object {
        fun from(response: DriveFilesResponse): DriveFilesPage {
            return DriveFilesPage(
                files = response.files,
                nextPageToken = response.nextPageToken,
                hasMore = response.nextPageToken != null
            )
        }
    }
}

sealed class DriveResult<out T> {
    data class Success<T>(val data: T) : DriveResult<T>()
    data class Error(val message: String, val code: Int? = null) : DriveResult<Nothing>()
    data object Loading : DriveResult<Nothing>()
}

class DriveServiceException(
    message: String,
    val code: Int? = null,
    cause: Throwable? = null
) : Exception(message, cause)