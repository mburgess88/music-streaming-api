package dev.michaelburgess.mymusic.routes

import dev.michaelburgess.mymusic.handler.MusicRouteHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.*

@Configuration
open class MusicRoutes {
    @Bean
    open fun musicEndPoint(musicRouteHandler: MusicRouteHandler): RouterFunction<ServerResponse> {
        return RouterFunctions.route().nest(RequestPredicates.path("/music"), { builder: RouterFunctions.Builder ->
            builder
                    .GET("") { musicRouteHandler.getAllMusic() }
                    .POST("") { request: ServerRequest -> musicRouteHandler.addMusic(request) }
                    .nest(RequestPredicates.path("/{id}"), { builder: RouterFunctions.Builder ->
                        builder
                                .GET("") { request: ServerRequest -> musicRouteHandler.getMusicDetails(request.pathVariable("id")) }
                                .nest(RequestPredicates.path("/stream"), { builder: RouterFunctions.Builder ->
                                    builder
                                            .GET("") { request: ServerRequest -> musicRouteHandler.getMusicFile(request) }
                                }
                                )
                    }
                    )
        }).build()
    }
}