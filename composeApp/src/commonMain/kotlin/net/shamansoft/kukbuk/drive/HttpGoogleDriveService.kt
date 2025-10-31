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

import net.shamansoft.kukbuk.util.Logger
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

    // Cache the folder ID to avoid repeated lookups (saves ~459ms per load)
    private var cachedFolderId: String? = null

    override suspend fun listFilesInKukbukFolder(): DriveResult<List<DriveFile>> {
        Logger.d("DriveService", "listFilesInKukbukFolder() called")
        return try {
            Logger.d("DriveService", "Getting access token...")
            val token = getValidAccessToken()
            if (token == null) {
                Logger.d("DriveService", "No valid access token available")
                return DriveResult.Error("No valid access token available")
            }
            Logger.d("DriveService", "Access token obtained: ${token.take(20)}...")

            // Get kukbuk folder ID (from cache if available)
            val folderId = cachedFolderId ?: run {
                Logger.d("DriveService", "Finding kukbuk folder (not cached)...")
                val folderResult = findKukbukFolder(token)
                if (folderResult is DriveResult.Error) {
                    Logger.d("DriveService", "Failed to find kukbuk folder: ${folderResult.message}")
                    return folderResult
                }
                val id = (folderResult as DriveResult.Success).data
                cachedFolderId = id // Cache for future use
                Logger.d("DriveService", "Found and cached kukbuk folder with id: $id")
                id
            }
            if (cachedFolderId != null) {
                Logger.d("DriveService", "Using cached kukbuk folder id: $folderId")
            }

            // List YAML files in the kukbuk folder
            val query = "'$folderId' in parents and name contains '.yaml' and trashed=false"
            Logger.d("DriveService", "Listing files with query: $query")

            val response = httpClient.get("$DRIVE_API_BASE/files") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
                parameter("q", query)
                parameter("fields", "files(id,name,mimeType,size,modifiedTime,parents,webViewLink)")
                parameter("orderBy", "modifiedTime desc")
                parameter("pageSize", "100")
            }

            Logger.d("DriveService", "Response status: ${response.status}")

            if (response.status.isSuccess()) {
                val filesResponse: DriveFilesResponse = response.body()
                Logger.d("DriveService", "Successfully listed ${filesResponse.files.size} files")
                DriveResult.Success(filesResponse.files)
            } else {
                val errorBody = response.bodyAsText()
                Logger.d("DriveService", "Failed to list files: ${response.status}, body: $errorBody")

                if (response.status.value == 401) {
                    Logger.e("DriveService", "Authentication error (401) - clearing auth state")
                    authRepository.handleAuthenticationError()
                    DriveResult.Error("Authentication expired. Please sign in again.", response.status.value)
                } else {
                    DriveResult.Error("Failed to list files: ${response.status}", response.status.value)
                }
            }
        } catch (e: Exception) {
            Logger.d("DriveService", "Exception in listFilesInKukbukFolder: ${e.message}")
            e.printStackTrace()
            DriveResult.Error("Network error: ${e.message}")
        }
    }

    override suspend fun listFilesInKukbukFolderPaginated(
        pageSize: Int,
        pageToken: String?
    ): DriveResult<DriveFilesPage> {
        Logger.d("DriveService", "listFilesInKukbukFolderPaginated() called (pageSize=$pageSize, pageToken=${pageToken?.take(10)}...)")
        return try {
            val token = getValidAccessToken()
            if (token == null) {
                Logger.d("DriveService", "No valid access token available")
                return DriveResult.Error("No valid access token available")
            }

            // Get kukbuk folder ID (from cache if available)
            val folderId = cachedFolderId ?: run {
                Logger.d("DriveService", "Finding kukbuk folder (not cached)...")
                val folderResult = findKukbukFolder(token)
                if (folderResult is DriveResult.Error) {
                    Logger.d("DriveService", "Failed to find kukbuk folder: ${folderResult.message}")
                    return folderResult
                }
                val id = (folderResult as DriveResult.Success).data
                cachedFolderId = id
                Logger.d("DriveService", "Found and cached kukbuk folder with id: $id")
                id
            }

            // List YAML files in the kukbuk folder with pagination
            val query = "'$folderId' in parents and name contains '.yaml' and trashed=false"
            Logger.d("DriveService", "Listing files (paginated) with query: $query")

            val response = httpClient.get("$DRIVE_API_BASE/files") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
                parameter("q", query)
                parameter("fields", "files(id,name,mimeType,size,modifiedTime,parents,webViewLink),nextPageToken")
                parameter("orderBy", "modifiedTime desc")
                parameter("pageSize", pageSize.toString())
                pageToken?.let { parameter("pageToken", it) }
            }

            Logger.d("DriveService", "Response status: ${response.status}")

            if (response.status.isSuccess()) {
                val filesResponse: DriveFilesResponse = response.body()
                val page = DriveFilesPage.from(filesResponse)
                Logger.d("DriveService", "Successfully listed ${page.files.size} files (hasMore=${page.hasMore})")
                DriveResult.Success(page)
            } else {
                val errorBody = response.bodyAsText()
                Logger.d("DriveService", "Failed to list files: ${response.status}, body: $errorBody")

                if (response.status.value == 401) {
                    Logger.e("DriveService", "Authentication error (401) - clearing auth state")
                    authRepository.handleAuthenticationError()
                    DriveResult.Error("Authentication expired. Please sign in again.", response.status.value)
                } else {
                    DriveResult.Error("Failed to list files: ${response.status}", response.status.value)
                }
            }
        } catch (e: Exception) {
            Logger.d("DriveService", "Exception in listFilesInKukbukFolderPaginated: ${e.message}")
            e.printStackTrace()
            DriveResult.Error("Network error: ${e.message}")
        }
    }

    override suspend fun downloadFileContent(fileId: String): DriveResult<String> {
        Logger.d("DriveService", "downloadFileContent() called for fileId: $fileId")
        return try {
            val token = getValidAccessToken()
            if (token == null) {
                Logger.d("DriveService", "No valid access token for download")
                return DriveResult.Error("No valid access token available")
            }

            Logger.d("DriveService", "Downloading file content...")
            val response = httpClient.get("$DRIVE_API_BASE/files/$fileId") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
                parameter("alt", "media")
            }

            Logger.d("DriveService", "Download response status: ${response.status}")

            if (response.status.isSuccess()) {
                val content: String = response.bodyAsText()
                Logger.d("DriveService", "Successfully downloaded file, content length: ${content.length}")
                DriveResult.Success(content)
            } else {
                val errorBody = response.bodyAsText()
                Logger.d("DriveService", "Failed to download file: ${response.status}, body: $errorBody")

                if (response.status.value == 401) {
                    Logger.e("DriveService", "Authentication error (401) - clearing auth state")
                    authRepository.handleAuthenticationError()
                    DriveResult.Error("Authentication expired. Please sign in again.", response.status.value)
                } else {
                    DriveResult.Error("Failed to download file: ${response.status}", response.status.value)
                }
            }
        } catch (e: Exception) {
            Logger.d("DriveService", "Exception in downloadFileContent: ${e.message}")
            e.printStackTrace()
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
        Logger.d("DriveService", "findKukbukFolder() called")
        return try {
            val query = "name='$KUKBUK_FOLDER_NAME' and mimeType='application/vnd.google-apps.folder' and trashed=false"
            Logger.d("DriveService", "Searching for folder with query: $query")

            val response = httpClient.get("$DRIVE_API_BASE/files") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
                parameter("q", query)
                parameter("fields", "files(id,name)")
            }

            Logger.d("DriveService", "Find folder response status: ${response.status}")

            if (response.status.isSuccess()) {
                val filesResponse: DriveFilesResponse = response.body()
                Logger.d("DriveService", "Found ${filesResponse.files.size} matching folders")
                val folder = filesResponse.files.firstOrNull()
                if (folder != null) {
                    Logger.d("DriveService", "Kukbuk folder found: ${folder.name} (id=${folder.id})")
                    DriveResult.Success(folder.id)
                } else {
                    Logger.d("DriveService", "Kukbuk folder not found")
                    DriveResult.Error("Kukbuk folder not found. Please ensure you have saved recipes from the browser extension.")
                }
            } else {
                val errorBody = response.bodyAsText()
                Logger.d("DriveService", "Failed to find kukbuk folder: ${response.status}, body: $errorBody")

                // Handle 401 Unauthorized - token expired or invalid
                if (response.status.value == 401) {
                    Logger.e("DriveService", "Authentication error (401) - clearing auth state")
                    authRepository.handleAuthenticationError()
                    DriveResult.Error("Authentication expired. Please sign in again.", response.status.value)
                } else {
                    DriveResult.Error("Failed to find kukbuk folder: ${response.status}", response.status.value)
                }
            }
        } catch (e: Exception) {
            Logger.d("DriveService", "Exception in findKukbukFolder: ${e.message}")
            e.printStackTrace()
            DriveResult.Error("Network error while finding folder: ${e.message}")
        }
    }

    private suspend fun getValidAccessToken(): String? {
        Logger.d("DriveService", "getValidAccessToken() called")
        return try {
            val token = authRepository.getValidAccessToken()
            if (token != null) {
                Logger.d("DriveService", "Access token retrieved successfully")
            } else {
                Logger.d("DriveService", "No access token available from authRepository")
            }
            token
        } catch (e: Exception) {
            Logger.d("DriveService", "Exception getting access token: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
