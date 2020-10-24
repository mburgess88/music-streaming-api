package dev.michaelburgess.mymusic

import dev.michaelburgess.mymusic.handler.MusicRouteHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.*

@Configuration
open class MusicRoutes {
    @Bean
    open fun videoEndPoint(videoRouteHandler: MusicRouteHandler): RouterFunction<ServerResponse> {
        return RouterFunctions.route().nest(RequestPredicates.path("/music"), { builder: RouterFunctions.Builder ->
            builder
                    .GET("") { videoRouteHandler.getAllMusic() }
                    .POST("") { request: ServerRequest -> videoRouteHandler.addMusic(request) }
                    .nest(RequestPredicates.path("/{id}"), { videoBuilder: RouterFunctions.Builder ->
                        videoBuilder
                                .GET("") { request: ServerRequest -> videoRouteHandler.getMusicDetails(request.pathVariable("id")) }
                                .nest(RequestPredicates.path("/stream"), { videoBuilder: RouterFunctions.Builder ->
                                    videoBuilder
                                            .GET("") { request: ServerRequest -> videoRouteHandler.getMusicFile(request) }
                                }
                                )
                    }
                    )
        }).build()
    }
}