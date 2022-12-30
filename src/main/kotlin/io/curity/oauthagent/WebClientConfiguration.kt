package io.curity.oauthagent

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
class WebClientConfiguration
{
    @Bean
    fun webClient(): WebClient?
    {
        val httpClient = HttpClient.create()
        return WebClient.builder().clientConnector(ReactorClientHttpConnector(httpClient)).build()
    }
}
