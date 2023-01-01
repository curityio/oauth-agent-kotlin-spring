package io.curity.oauthagent

import io.curity.oauthagent.utilities.CustomCorsProcessor
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.cors.reactive.CorsWebFilter

@SpringBootApplication
class OAuthAgentApplication {

    @Bean
    @ConditionalOnProperty(value = ["CORS_ENABLED"], havingValue = "true", matchIfMissing = true)
    fun corsWebFilter(configuration: OAuthAgentConfiguration): CorsWebFilter {

        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()
        config.allowedOrigins = configuration.trustedWebOrigins
        config.allowCredentials = true
        config.allowedMethods = listOf("POST", "GET", "OPTIONS")
        config.allowedHeaders = listOf("*")
        source.registerCorsConfiguration("/**", config)

        val corsProcessor = CustomCorsProcessor()
        return CorsWebFilter(source, corsProcessor)
    }

    @Bean
    fun oauthParametersProvider(): OAuthParametersProvider = OAuthParametersProviderImpl()
}

fun main(args: Array<String>)
{
    runApplication<OAuthAgentApplication>(*args)
}
