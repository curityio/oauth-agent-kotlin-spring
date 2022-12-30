package io.curity.oauthagent

import com.github.tomakehurst.wiremock.WireMockServer
import groovy.json.JsonSlurper
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import static groovy.json.JsonOutput.toJson
import static org.springframework.http.HttpMethod.POST
import static org.springframework.http.HttpStatus.OK
import static org.springframework.http.MediaType.APPLICATION_JSON

@SpringBootTest(classes = [OAuthAgentApplication.class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = [OAuthAgentConfiguration.class])
class TokenHandlerSpecification extends Specification {

    def requestFactory = new HttpComponentsClientHttpRequestFactory(HttpClients.createDefault())
    def client = new RestTemplate(requestFactory)
    def json = new JsonSlurper()

    @Autowired
    IdsvrStubs stubs

    @Autowired
    OAuthAgentConfiguration configuration

    @LocalServerPort
    def port

    @SpringBean
    OAuthParametersProvider oAuthParametersProvider = Stub() {
        getState() >> "someState"
        getCodeVerifier() >> "verifier"
    }

    static def baseUrl = "http://localhost"

    static def loginStartPath = "/login/start"
    static def loginStartUrl

    static def loginEndPath = "/login/end"
    static def loginEndUrl

    static def wireMockServer

    def setupSpec() {
        wireMockServer = new WireMockServer(options()
                .port(8443)
        )
        wireMockServer.start()
        configureFor("http", "localhost", 8443)
    }

    def cleanupSpec() {
        wireMockServer.stop()
    }

    protected def getCookiesAndCSRFForAuthenticatedUser() {
        def startLoginRequest = getRequestWithValidOrigin(POST, loginStartURI)
        def startLoginResponse = client.exchange(startLoginRequest, String.class)

        def cookies = startLoginResponse.headers.get("Set-Cookie")
        stubs.idsvrRespondsWithTokens()

        def cookieHeaders = new HttpHeaders()
        cookieHeaders.addAll("Cookie", cookies)

        def request = getRequestWithValidOrigin(
                POST,
                loginEndURI,
                toJson([pageUrl: "${configuration.redirectUri}?$validResponsePayload" ]),
                cookieHeaders
        )

        def response = client.exchange(request, String.class)
        assert response.statusCode == OK
        def responseBody = json.parseText(response.body)
        [cookies: response.headers.get("Set-Cookie"), csrf: responseBody["csrf"]]
    }

    protected def getLoginStartURI() {
        if (loginStartUrl == null) {
            loginStartUrl = new URI("$baseUrl:$port/${configuration.endpointsPrefix}$loginStartPath")
        }

        return loginStartUrl
    }

    protected def getLoginEndURI() {
        if (loginEndUrl == null) {
            loginEndUrl = new URI("$baseUrl:$port/${configuration.endpointsPrefix}$loginEndPath")
        }

        return loginEndUrl
    }

    protected def getRequestWithValidOrigin(HttpMethod method, URI url, body = null, HttpHeaders additionalHeaders = null) {
        def headers = additionalHeaders != null ? new HttpHeaders(additionalHeaders) : new HttpHeaders()
        headers.setOrigin(configuration.trustedWebOrigins.first())
        headers.setContentType(APPLICATION_JSON)

        new RequestEntity(body, headers, method, url)
    }

    protected def getValidResponsePayload() {
        return "code=12345&state=someState"
    }

    protected def getMaliciousResponsePayload() {
        return "code=12345&state=mismatchedState"
    }

    protected def getErrorResponsePayload(String errorCode) {
        return "error=$errorCode&state=someState"
    }

    protected static def getRequestWithMaliciousOrigin(HttpMethod method, URI url) {
        def headers = new HttpHeaders()
        headers.setOrigin("https://malicious.site")
        headers.setContentType(APPLICATION_JSON)

        new RequestEntity(headers, method, url)
    }
}
