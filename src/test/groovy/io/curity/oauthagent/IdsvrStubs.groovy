package io.curity.oauthagent

import org.jose4j.jwk.RsaJsonWebKey
import org.jose4j.jws.AlgorithmIdentifiers
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.JwtClaims
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.containing
import static com.github.tomakehurst.wiremock.client.WireMock.post
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static groovy.json.JsonOutput.toJson

@Service
class IdsvrStubs {

    @Autowired
    RsaJsonWebKey rsaJsonWebKey

    @Autowired
    private OAuthAgentConfiguration configuration

    def idsvrRespondsWithTokens() {
        stubFor(post(getTokenEndpointPath())
                .withRequestBody(containing("code=12345"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(toJson([access_token: "access", refresh_token: "refresh", "id_token": IDToken]))
                ))
    }

    def idsvrRespondsWithRefreshedTokens() {
        stubFor(post(getTokenEndpointPath())
            .withRequestBody(containing("refresh_token=refresh"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(toJson([access_token: "access_new", refresh_token: "refresh_new", "id_token": IDToken]))
            ))
    }

    def idsvrRespondsToRefreshWithInvalidGrantWhenRefreshTokenIsExpired() {
        stubFor(post(getTokenEndpointPath())
            .withRequestBody(containing("refresh_token=refresh"))
            .willReturn(aResponse()
                    .withStatus(400)
                    .withBody("{error: invalid_grant}")
            ))
    }

    def idsvrRespondsToRefreshWithInvalidClientWhenClientSecretIsMisconfigured() {
        stubFor(post(getTokenEndpointPath())
                .withRequestBody(containing("refresh_token=refresh"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("{error: invalid_client}")
                ))
    }

    def getTokenEndpointPath() {
        (new URI(configuration.tokenEndpoint)).path
    }

    def getUserInfoEndpointPath() {
        (new URI(configuration.userInfoEndpoint)).path
    }

    def idsvrRespondsWithUserInfo() {
        stubFor(post(getUserInfoEndpointPath())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(toJson([sub: "user@example.com", username: "user"]))
                ))
    }

    def idsvrRespondsWith401DuringUserInfoRequest() {
        stubFor(post(getUserInfoEndpointPath())
                .willReturn(aResponse()
                        .withStatus(401)
                        .withBody("{error: invalid_token}")
                ))
    }

    private def getIDToken() {
        def claims = new JwtClaims()
        claims.setIssuer(configuration.issuer)
        claims.setAudience(configuration.clientID)
        claims.setExpirationTimeMinutesInTheFuture(10)
        claims.setGeneratedJwtId()
        claims.setIssuedAtToNow()
        claims.setClaim("sub","user@example.com")
        claims.setClaim("auth_time", 1626259937)

        def jws = new JsonWebSignature()
        jws.setPayload(claims.toJson())
        jws.setKey(rsaJsonWebKey.getPrivateKey())
        jws.setKeyIdHeaderValue(rsaJsonWebKey.getKeyId())
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256)

        jws.getCompactSerialization()
    }
}
