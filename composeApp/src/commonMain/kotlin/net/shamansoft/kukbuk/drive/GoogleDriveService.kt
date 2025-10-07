package net.shamansoft.kukbuk.drive

import kotlinx.serialization.Serializable

interface GoogleDriveService {
    suspend fun listFilesInKukbukFolder(): DriveResult<List<DriveFile>>
    suspend fun downloadFileContent(fileId: String): DriveResult<String>
    suspend fun searchFiles(query: String): DriveResult<List<DriveFile>>
}

@Serializable
data class DriveFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long? = null,
    val modifiedTime: String,
    val parents: List<String> = emptyList(),
    val webViewLink: String? = null
)

@Serializable
data class DriveFilesResponse(
    val files: List<DriveFile>,
    val nextPageToken: String? = null
)

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