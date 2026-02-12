package dev.michaelburgess.mymusic.config

import dev.michaelburgess.mymusic.ResourceRegionMessageWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer

@Configuration
@EnableWebFlux
open class WebFluxConfig : WebFluxConfigurer {
    override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
        configurer.customCodecs().register(ResourceRegionMessageWriter())
    }

    @Bean
    open fun corsWebFilter(): CorsWebFilter {
        val corsConfig = CorsConfiguration()
        // Allow all origins in production/aws profile to handle CloudFront proxying smoothly
        corsConfig.allowedOrigins = listOf("*")
        corsConfig.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
        corsConfig.allowedHeaders = listOf("*")
        corsConfig.exposedHeaders = listOf("Content-Range", "Content-Length", "Accept-Ranges")
        corsConfig.allowCredentials = true
        corsConfig.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", corsConfig)

        return CorsWebFilter(source)
    }
}