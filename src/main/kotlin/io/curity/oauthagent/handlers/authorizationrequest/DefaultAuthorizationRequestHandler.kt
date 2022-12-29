package io.curity.oauthagent.handlers.authorizationrequest

import io.curity.oauthagent.OAuthAgentConfiguration
import io.curity.oauthagent.OAuthParametersProvider
import io.curity.oauthagent.controller.StartAuthorizationParameters
import io.curity.oauthagent.hash
import org.springframework.stereotype.Service
import java.net.URLEncoder

/*
 * Creates the details for a plain OpenID Connect code flow
 */
@Service
class DefaultAuthorizationRequestHandler(
        private val config: OAuthAgentConfiguration,
        private val oAuthParametersProvider: OAuthParametersProvider) : AuthorizationRequestHandler {

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun createRequest(parameters: StartAuthorizationParameters?): AuthorizationRequestData {

        val codeVerifier = oAuthParametersProvider.getCodeVerifier()
        val state = oAuthParametersProvider.getState()
        val encoding = "utf-8"
        val url =
                "client_id=${URLEncoder.encode(config.clientID, encoding)}" +
                        "&redirect_uri=${URLEncoder.encode(config.redirectUri, encoding)}" +
                        "&response_type=code" +
                        "&state=${URLEncoder.encode(state, encoding)}" +
                        "&code_challenge=${URLEncoder.encode(codeVerifier.hash(), encoding)}" +
                        "&code_challenge_method=S256"
        return AuthorizationRequestData(url, codeVerifier, state)
    }
}
