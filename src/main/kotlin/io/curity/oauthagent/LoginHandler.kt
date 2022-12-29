package io.curity.oauthagent

import io.curity.oauthagent.controller.StartAuthorizationParameters
import io.curity.oauthagent.exception.AuthorizationResponseException
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import java.net.URLEncoder

@Service
class LoginHandler(
        private val config: OAuthAgentConfiguration,
        private val oAuthParametersProvider: OAuthParametersProvider
) {

    fun createAuthorizationRequest(parameters: StartAuthorizationParameters?): AuthorizationRequestData {

        val codeVerifier = oAuthParametersProvider.getCodeVerifier()
        val state = oAuthParametersProvider.getState()
        val encoding = "utf-8"
        var url =
                "client_id=${URLEncoder.encode(config.clientID, encoding)}" +
                "&redirect_uri=${URLEncoder.encode(config.redirectUri, encoding)}" +
                "&response_type=code" +
                "&state=${URLEncoder.encode(state, encoding)}" +
                "&code_challenge=${URLEncoder.encode(codeVerifier.hash(), encoding)}" +
                "&code_challenge_method=S256"

        if (config.scope != null)
        {
            url += "&scope=${config.scope}"
        }

        parameters?.extraParams?.forEach {
            url += "&${it.key}=${it.value}"
        }

        return AuthorizationRequestData(url, codeVerifier, state)
    }

    fun handleAuthorizationResponse(pageUrl: String?): OAuthQueryParams {

        if (pageUrl == null)
        {
            return OAuthQueryParams(null, null)
        }

        val queryParams = UriComponentsBuilder.fromUriString(pageUrl).build().queryParams

        val codeMap = queryParams["code"]
        val stateMap = queryParams["state"]
        if (codeMap != null && stateMap != null) {

            val code = codeMap.first()
            val state = stateMap.first()
            return OAuthQueryParams(code, state)
        }

        val errorMap = queryParams["error"]
        val errorDescriptionMap = queryParams["error_description"]
        if (errorMap != null && stateMap != null) {
            throw getAuthorizationResponseError(errorMap.first(), errorDescriptionMap?.first())
        }

        return OAuthQueryParams(null, null)
    }

    private fun getAuthorizationResponseError(errorCode: String, errorDescription: String?): AuthorizationResponseException {

        var errorCodeResult = errorCode
        var errorDescriptionResult = errorDescription

        if (errorCodeResult.isBlank()) {
            errorCodeResult = "authorization_response_error"
        }
        if (errorDescriptionResult.isNullOrBlank()) {
            errorDescriptionResult = "Login failed at the Authorization Server"
        }

        return AuthorizationResponseException(errorCodeResult, errorDescriptionResult)
    }
}