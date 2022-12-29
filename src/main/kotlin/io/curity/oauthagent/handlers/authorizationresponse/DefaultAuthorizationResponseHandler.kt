package io.curity.oauthagent.handlers.authorizationresponse

import io.curity.oauthagent.controller.OAuthQueryParams
import io.curity.oauthagent.exception.AuthorizationResponseException
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder

/*
 * Handles a  code flow authorization response with response_mode=query, to receive the code and state
 */
@Service
class DefaultAuthorizationResponseHandler(
): AuthorizationResponseHandler {

    override suspend fun handleResponse(pageUrl: String?): OAuthQueryParams {

        if (pageUrl == null)
        {
            return OAuthQueryParams(null, null)
        }

        val queryParams = UriComponentsBuilder.fromUriString(pageUrl).build().queryParams

        val codeMap = queryParams["code"]
        val stateMap = queryParams["state"]
        if (codeMap != null && stateMap != null) {
            return OAuthQueryParams(codeMap.first(), stateMap.first())
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