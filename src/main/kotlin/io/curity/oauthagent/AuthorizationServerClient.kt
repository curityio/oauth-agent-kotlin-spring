package io.curity.oauthagent

import com.fasterxml.jackson.annotation.JsonProperty
import io.curity.oauthagent.exception.*
import io.curity.oauthagent.utilities.Grant
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange

@Service
class AuthorizationServerClient(
    private val client: WebClient,
    private val config: OAuthAgentConfiguration,

)
{
    suspend fun redeemCodeForTokens(code: String, codeVerifier: String): TokenResponse
    {
        try
        {
            val body = "client_id=${config.clientID}&client_secret=${config.clientSecret}&grant_type=authorization_code&redirect_uri=${config.redirectUri}&code=${code}&code_verifier=${codeVerifier}"
            return client.post()
                .uri(config.tokenEndpoint)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(body)
                .awaitExchange { response -> handleAuthorizationServerResponse(response, Grant.AuthorizationCode) }

        } catch (exception: WebClientRequestException)
        {
            throw AuthorizationServerException("Connectivity problem during an Authorization Code Grant", exception)
        }
    }

    suspend fun getUserInfo(accessToken: String): Map<String, Any>
    {
        try
        {
            return client.post()
                .uri(config.userInfoEndpoint)
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .awaitExchange { response -> handleAuthorizationServerResponse(response, Grant.UserInfo) }

        } catch (exception: WebClientRequestException)
        {
            throw AuthorizationServerException("Connectivity problem during a User Info request", exception)
        }
    }

    suspend fun refreshAccessToken(refreshToken: String): TokenResponse
    {
        try
        {
            return client.post()
                .uri(config.tokenEndpoint)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue("client_id=${config.clientID}&client_secret=${config.clientSecret}&grant_type=refresh_token&refresh_token=$refreshToken")
                .awaitExchange { response -> handleAuthorizationServerResponse(response, Grant.RefreshToken) }

        } catch (exception: WebClientRequestException)
        {
            throw AuthorizationServerException("Connectivity problem during a Refresh Token Grant", exception)
        }
    }

    private suspend inline fun <reified T : Any> handleAuthorizationServerResponse(
            response: ClientResponse,
            grant: Grant
    ): T
    {
        if (response.statusCode().is5xxServerError)
        {
            val text = response.awaitBody<String>()
            throw AuthorizationServerException("Server error response in $grant: $text")
        }

        if (response.statusCode().is4xxClientError)
        {
            val text = response.awaitBody<String>()
            throw AuthorizationClientException.create(grant, response.statusCode(), text)
        }

        return response.awaitBody()
    }
}

class TokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("refresh_token") val refreshToken: String?,
    @JsonProperty("id_token") val idToken: String?
)
