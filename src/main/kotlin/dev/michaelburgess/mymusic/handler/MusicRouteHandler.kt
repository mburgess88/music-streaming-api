package dev.michaelburgess.mymusic.handler

import dev.michaelburgess.mymusic.domain.MusicDetails
import dev.michaelburgess.mymusic.service.MusicService
import org.springframework.core.io.support.ResourceRegion
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono

@Component
class MusicRouteHandler(private val musicService: MusicService) {

    fun addMusic(request: ServerRequest): Mono<ServerResponse> {
        return request.bodyToMono(MusicDetails::class.java)
                .flatMap { newMusicDetails -> musicService.addNewMusic(newMusicDetails)}
                .then(ok().build())
    }

    fun getMusicDetails(id: String) : Mono<ServerResponse> {
        val list = musicService.findById(id)

        return ok()
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.noCache())
                .body(list, MusicDetails::class.java)
    }

    fun getAllMusic() : Mono<ServerResponse> {
        val list = musicService.listAllMusic()
        return ok()
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.noCache())
                .body(list, MusicDetails::class.java)
    }


    fun getMusicFile(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable("id")
        val resourceRegionMono = musicService.getRegion(id, request)
        return resourceRegionMono
                .flatMap { resourceRegion: ResourceRegion ->
                    ServerResponse
                            .status(HttpStatus.PARTIAL_CONTENT)
                            .contentLength(resourceRegion.count)
                            .headers { headers: HttpHeaders -> headers.setCacheControl(CacheControl.noCache()) }
                            .body(resourceRegionMono, ResourceRegion::class.java)
                }
    }
}