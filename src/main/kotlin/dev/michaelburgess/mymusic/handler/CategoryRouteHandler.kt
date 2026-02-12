package dev.michaelburgess.mymusic.handler

import dev.michaelburgess.mymusic.service.CategoryService
import dev.michaelburgess.mymusic.service.MusicService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Component
class CategoryRouteHandler(
    private val categoryService: CategoryService,
    private val musicService: MusicService
) {
    fun getAllCategories(): Mono<ServerResponse> {
        return ServerResponse.ok().body(categoryService.getAllCategories(), dev.michaelburgess.mymusic.domain.Category::class.java)
    }

    fun getCategoryTracks(id: String): Mono<ServerResponse> {
        return ServerResponse.ok().body(musicService.listMusicByCategory(id), dev.michaelburgess.mymusic.domain.MusicDetails::class.java)
    }
}
