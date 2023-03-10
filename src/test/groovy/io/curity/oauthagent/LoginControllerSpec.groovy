package io.curity.oauthagent

import org.springframework.http.HttpHeaders
import org.springframework.web.client.HttpClientErrorException
import static groovy.json.JsonOutput.toJson
import static org.springframework.http.HttpMethod.OPTIONS
import static org.springframework.http.HttpMethod.POST
import static org.springframework.http.HttpStatus.BAD_REQUEST
import static org.springframework.http.HttpStatus.OK
import static org.springframework.http.HttpStatus.UNAUTHORIZED

class LoginControllerSpec extends TokenHandlerSpecification {

    def "Sending an OPTIONS request with wrong Origin should return 401 response without CORS headers"() {
        given:
        def request = getRequestWithMaliciousOrigin(OPTIONS, getLoginStartURI())

        when:
        client.exchange(request, String.class)

        then:
        def response = thrown HttpClientErrorException
        response.statusCode == UNAUTHORIZED
        response.getResponseHeaders()['Access-Control-Allow-Origin'] == null
    }

    def "Sending OPTIONS request with a valid web origin should return a 200 response with proper CORS headers"() {
        given:
        def request = getRequestWithValidOrigin(OPTIONS, loginStartURI)

        when:
        def response = client.exchange(request, String.class)

        then:
        response.statusCode == OK
        response.headers['Access-Control-Allow-Origin'] == ["http://www.example.com"]
    }

    def "Request to end login with invalid web origin should return 401 response"() {
        given:
        def request = getRequestWithMaliciousOrigin(POST, loginEndURI)

        when:
        client.exchange(request, String.class)

        then:
        def response = thrown HttpClientErrorException
        response.statusCode == UNAUTHORIZED
    }

    def "Request to end login should return correct unauthenticated response"() {
        given:
        def request = getRequestWithValidOrigin(POST, getLoginEndURI(), toJson([ pageUrl: configuration.redirectUri ]))

        when:
        def response = client.exchange(request, String.class)

        then:
        response.statusCode == OK
        def responseBody = json.parseText(response.body)
        responseBody["isLoggedIn"] == false
        responseBody["handled"] == false
    }

    def "POST request to start login with invalid web origin should return a 401 response"() {
        given:
        def request = getRequestWithMaliciousOrigin(POST, loginStartURI)

        when:
        client.exchange(request, String.class)

        then:
        def response = thrown HttpClientErrorException
        response.statusCode == UNAUTHORIZED
    }

    def "Request to start login should return authorization request URL"() {
        given:
        def request = getRequestWithValidOrigin(POST, loginStartURI)

        when:
        def response = client.exchange(request, String.class)

        then:
        response.statusCode == OK
        def responseBody = json.parseText(response.body)
        def authorizationRequestUrl = responseBody["authorizationRequestUrl"]?.toString()
        !authorizationRequestUrl.empty
    }

    def "Posting a valid authorization response to login end should result in authenticating the user"() {
        given: // request to login/start is performed to get proper cookies
        def startLoginRequest = getRequestWithValidOrigin(POST, loginStartURI)
        def startLoginResponse = client.exchange(startLoginRequest, String.class)

        def cookies = startLoginResponse.headers.get("Set-Cookie")

        and: // Identity Server response with tokens
        stubs.idsvrRespondsWithTokens()

        and: // request to login/end
        def cookieHeaders = new HttpHeaders()
        cookieHeaders.addAll("Cookie", cookies)

        def request = getRequestWithValidOrigin(
            POST,
            loginEndURI,
            toJson([pageUrl: "${configuration.redirectUri}?$validResponsePayload" ]),
            cookieHeaders
        )

        when:
        def response = client.exchange(request, String.class)

        then:
        response.statusCode == OK
        def responseBody = json.parseText(response.body)
        responseBody["isLoggedIn"] == true
        responseBody["handled"] == true
        def responseCookies = response.headers.get("Set-Cookie")
        responseCookies.size() == 5
    }

    def "Posting a malicious authorization response to end login endpoint should return a 400 invalid_request response"() {
        given: // request to login/start is performed to get proper cookies
        def startLoginRequest = getRequestWithValidOrigin(POST, loginStartURI)
        def startLoginResponse = client.exchange(startLoginRequest, String.class)

        def cookies = startLoginResponse.headers.get("Set-Cookie")

        and: // request to login/end
        def cookieHeaders = new HttpHeaders()
        cookieHeaders.addAll("Cookie", cookies)

        def request = getRequestWithValidOrigin(
                POST,
                loginEndURI,
                toJson([pageUrl: "${configuration.redirectUri}?$maliciousResponsePayload" ]),
                cookieHeaders
        )

        when:
        client.exchange(request, String.class)

        then:
        def response = thrown HttpClientErrorException
        response.statusCode == BAD_REQUEST
        def responseBody = json.parseText(response.responseBodyAsString)
        responseBody["code"] == "invalid_request"
    }

    def "Posting to end login with session cookies should return proper 200 response"() {
        given:
        def cookiesHeader = new HttpHeaders()
        cookiesHeader.addAll("Cookie", cookiesAndCSRFForAuthenticatedUser.cookies)

        def request = getRequestWithValidOrigin(POST, getLoginEndURI(), [pageUrl: configuration.redirectUri], cookiesHeader)

        when:
        def response = client.exchange(request, String.class)

        then:
        response.statusCode == OK
        def responseBody = json.parseText(response.body)
        responseBody["isLoggedIn"] == true
        responseBody["handled"] == false
    }

    def "Ending a login with an invalid_scope error should return a 400 error to the SPA for display"() {
        given:
        def startLoginRequest = getRequestWithValidOrigin(POST, loginStartURI)
        def startLoginResponse = client.exchange(startLoginRequest, String.class)

        def cookies = startLoginResponse.headers.get("Set-Cookie")

        and:
        def cookieHeaders = new HttpHeaders()
        cookieHeaders.addAll("Cookie", cookies)

        def errorPayload = getErrorResponsePayload("invalid_scope")
        def request = getRequestWithValidOrigin(
                POST,
                loginEndURI,
                toJson([pageUrl: "${configuration.redirectUri}?$errorPayload" ]),
                cookieHeaders
        )

        when:
        client.exchange(request, String.class)

        then:
        def response = thrown HttpClientErrorException
        response.statusCode == BAD_REQUEST
        def responseBody = json.parseText(response.responseBodyAsString)
        responseBody["code"] == "invalid_scope"
    }

    def "Ending a login with a login_required error should return a 401 error to the SPA"() {
        given:
        def startLoginRequest = getRequestWithValidOrigin(POST, loginStartURI)
        def startLoginResponse = client.exchange(startLoginRequest, String.class)

        def cookies = startLoginResponse.headers.get("Set-Cookie")

        and:
        def cookieHeaders = new HttpHeaders()
        cookieHeaders.addAll("Cookie", cookies)

        def errorPayload = getErrorResponsePayload("login_required")
        def request = getRequestWithValidOrigin(
                POST,
                loginEndURI,
                toJson([pageUrl: "${configuration.redirectUri}?$errorPayload" ]),
                cookieHeaders
        )

        when:
        client.exchange(request, String.class)

        then:
        def response = thrown HttpClientErrorException
        response.statusCode == UNAUTHORIZED
        def responseBody = json.parseText(response.responseBodyAsString)
        responseBody["code"] == "login_required"
    }
}
