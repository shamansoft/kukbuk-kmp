package net.shamansoft.kukbuk.drive

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import net.shamansoft.kukbuk.auth.AuthenticationRepository

class HttpGoogleDriveService(
    private val authRepository: AuthenticationRepository,
    private val httpClient: HttpClient = createHttpClient()
) : GoogleDriveService {

    companion object {
        private const val DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
        private const val KUKBUK_FOLDER_NAME = "kukbuk"
        
        fun createHttpClient(): HttpClient {
            return HttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    })
                }
            }
        }
    }

    override suspend fun listFilesInKukbukFolder(): DriveResult<List<DriveFile>> {
        return try {
            val token = getValidAccessToken()
                ?: return DriveResult.Error("No valid access token available")

            // First, find the kukbuk folder
            val folderResult = findKukbukFolder(token)
            if (folderResult is DriveResult.Error) {
                return folderResult
            }

            val folderId = (folderResult as DriveResult.Success).data

            // List YAML files in the kukbuk folder
            val response = httpClient.get("$DRIVE_API_BASE/files") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
                parameter("q", "'$folderId' in parents and name contains '.yaml' and trashed=false")
                parameter("fields", "files(id,name,mimeType,size,modifiedTime,parents,webViewLink)")
                parameter("orderBy", "modifiedTime desc")
                parameter("pageSize", "100")
            }

            if (response.status.isSuccess()) {
                val filesResponse: DriveFilesResponse = response.body()
                DriveResult.Success(filesResponse.files)
            } else {
                DriveResult.Error("Failed to list files: ${response.status}", response.status.value)
            }
        } catch (e: Exception) {
            DriveResult.Error("Network error: ${e.message}")
        }
    }

    override suspend fun downloadFileContent(fileId: String): DriveResult<String> {
        return try {
            val token = getValidAccessToken()
                ?: return DriveResult.Error("No valid access token available")

            val response = httpClient.get("$DRIVE_API_BASE/files/$fileId") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
                parameter("alt", "media")
            }

            if (response.status.isSuccess()) {
                val content: String = response.bodyAsText()
                DriveResult.Success(content)
            } else {
                DriveResult.Error("Failed to download file: ${response.status}", response.status.value)
            }
        } catch (e: Exception) {
            DriveResult.Error("Network error: ${e.message}")
        }
    }

    override suspend fun searchFiles(query: String): DriveResult<List<DriveFile>> {
        return try {
            val token = getValidAccessToken()
                ?: return DriveResult.Error("No valid access token available")

            val response = httpClient.get("$DRIVE_API_BASE/files") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
                parameter("q", query)
                parameter("fields", "files(id,name,mimeType,size,modifiedTime,parents,webViewLink)")
            }

            if (response.status.isSuccess()) {
                val filesResponse: DriveFilesResponse = response.body()
                DriveResult.Success(filesResponse.files)
            } else {
                DriveResult.Error("Failed to search files: ${response.status}", response.status.value)
            }
        } catch (e: Exception) {
            DriveResult.Error("Network error: ${e.message}")
        }
    }

    private suspend fun findKukbukFolder(token: String): DriveResult<String> {
        return try {
            val response = httpClient.get("$DRIVE_API_BASE/files") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
                parameter("q", "name='$KUKBUK_FOLDER_NAME' and mimeType='application/vnd.google-apps.folder' and trashed=false")
                parameter("fields", "files(id,name)")
            }

            if (response.status.isSuccess()) {
                val filesResponse: DriveFilesResponse = response.body()
                val folder = filesResponse.files.firstOrNull()
                if (folder != null) {
                    DriveResult.Success(folder.id)
                } else {
                    DriveResult.Error("Kukbuk folder not found. Please ensure you have saved recipes from the browser extension.")
                }
            } else {
                DriveResult.Error("Failed to find kukbuk folder: ${response.status}", response.status.value)
            }
        } catch (e: Exception) {
            DriveResult.Error("Network error while finding folder: ${e.message}")
        }
    }

    private suspend fun getValidAccessToken(): String? {
        return try {
            authRepository.getValidAccessToken()
        } catch (e: Exception) {
            null
        }
    }
}