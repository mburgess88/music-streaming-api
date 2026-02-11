package dev.michaelburgess.mymusic.routes

import dev.michaelburgess.mymusic.handler.MusicRouteHandler
import dev.michaelburgess.mymusic.handler.CategoryRouteHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.*

@Configuration
open class MusicRoutes {
    @Bean
    open fun musicEndPoint(musicRouteHandler: MusicRouteHandler, categoryRouteHandler: CategoryRouteHandler): RouterFunction<ServerResponse> {
        return RouterFunctions.route()
            .nest(RequestPredicates.path("/api"), { apiBuilder ->
                apiBuilder
                    .nest(RequestPredicates.path("/music"), { builder ->
                        builder
                                .GET("") { musicRouteHandler.getAllMusic() }
                                .POST("") { request: ServerRequest -> musicRouteHandler.addMusic(request) }
                                .GET("/{id}") { request: ServerRequest -> musicRouteHandler.getMusicDetails(request.pathVariable("id")) }
                                .GET("/{id}/stream") { request: ServerRequest -> musicRouteHandler.getMusicFile(request) }
                    })
                    .nest(RequestPredicates.path("/categories"), { builder ->
                        builder
                            .GET("") { categoryRouteHandler.getAllCategories() }
                            .GET("/{id}/tracks") { request: ServerRequest -> categoryRouteHandler.getCategoryTracks(request.pathVariable("id")) }
                    })
            })
            .build()
    }
}