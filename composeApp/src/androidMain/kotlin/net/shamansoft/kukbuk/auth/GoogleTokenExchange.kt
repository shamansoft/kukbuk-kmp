package net.shamansoft.kukbuk.auth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.shamansoft.kukbuk.util.Logger

/**
 * Exchanges a Google server auth code for OAuth 2.0 access and refresh tokens
 */
class GoogleTokenExchange(
    private val httpClient: HttpClient,
    private val clientId: String
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Exchange server auth code for OAuth tokens
     */
    suspend fun exchangeAuthCode(authCode: String): TokenExchangeResult {
        Logger.d("TokenExchange", "Exchanging server auth code for OAuth tokens")
        Logger.d("TokenExchange", "Auth code length: ${authCode.length}")

        return try {
            val response = httpClient.post("https://oauth2.googleapis.com/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    listOf(
                        "code" to authCode,
                        "client_id" to clientId,
                        "grant_type" to "authorization_code"
                    ).formUrlEncode()
                )
            }

            Logger.d("TokenExchange", "Token exchange response status: ${response.status}")

            if (response.status.isSuccess()) {
                val tokenResponse: GoogleTokenResponse = response.body()
                Logger.d("TokenExchange", "Successfully received OAuth tokens")
                Logger.d("TokenExchange", "Access token length: ${tokenResponse.access_token.length}")
                Logger.d("TokenExchange", "Refresh token present: ${tokenResponse.refresh_token != null}")
                Logger.d("TokenExchange", "Expires in: ${tokenResponse.expires_in} seconds")

                TokenExchangeResult.Success(
                    accessToken = tokenResponse.access_token,
                    refreshToken = tokenResponse.refresh_token,
                    expiresIn = tokenResponse.expires_in,
                    tokenType = tokenResponse.token_type
                )
            } else {
                val errorBody = response.bodyAsText()
                Logger.e("TokenExchange", "Token exchange failed: ${response.status}")
                Logger.e("TokenExchange", "Error body: $errorBody")
                TokenExchangeResult.Error("Token exchange failed: ${response.status}")
            }
        } catch (e: Exception) {
            Logger.e("TokenExchange", "Exception during token exchange: ${e.message}")
            e.printStackTrace()
            TokenExchangeResult.Error("Token exchange failed: ${e.message}")
        }
    }

    /**
     * Refresh an expired access token using a refresh token
     */
    suspend fun refreshAccessToken(refreshToken: String): TokenExchangeResult {
        Logger.d("TokenExchange", "Refreshing access token")

        return try {
            val response = httpClient.post("https://oauth2.googleapis.com/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    listOf(
                        "refresh_token" to refreshToken,
                        "client_id" to clientId,
                        "grant_type" to "refresh_token"
                    ).formUrlEncode()
                )
            }

            Logger.d("TokenExchange", "Token refresh response status: ${response.status}")

            if (response.status.isSuccess()) {
                val tokenResponse: GoogleTokenResponse = response.body()
                Logger.d("TokenExchange", "Successfully refreshed access token")
                Logger.d("TokenExchange", "New access token length: ${tokenResponse.access_token.length}")

                TokenExchangeResult.Success(
                    accessToken = tokenResponse.access_token,
                    refreshToken = tokenResponse.refresh_token ?: refreshToken, // Keep old refresh token if new one not provided
                    expiresIn = tokenResponse.expires_in,
                    tokenType = tokenResponse.token_type
                )
            } else {
                val errorBody = response.bodyAsText()
                Logger.e("TokenExchange", "Token refresh failed: ${response.status}")
                Logger.e("TokenExchange", "Error body: $errorBody")
                TokenExchangeResult.Error("Token refresh failed: ${response.status}")
            }
        } catch (e: Exception) {
            Logger.e("TokenExchange", "Exception during token refresh: ${e.message}")
            e.printStackTrace()
            TokenExchangeResult.Error("Token refresh failed: ${e.message}")
        }
    }
}

@Serializable
private data class GoogleTokenResponse(
    val access_token: String,
    val refresh_token: String? = null,
    val expires_in: Int,
    val token_type: String,
    val scope: String? = null,
    val id_token: String? = null
)

sealed class TokenExchangeResult {
    data class Success(
        val accessToken: String,
        val refreshToken: String?,
        val expiresIn: Int,
        val tokenType: String
    ) : TokenExchangeResult()

    data class Error(val message: String) : TokenExchangeResult()
}

// Helper function to form URL encode parameters
private fun List<Pair<String, String>>.formUrlEncode(): String {
    return joinToString("&") { (key, value) ->
        "${key.encodeURLParameter()}=${value.encodeURLParameter()}"
    }
}
