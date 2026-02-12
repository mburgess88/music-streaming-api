package dev.michaelburgess.mymusic.repository

import dev.michaelburgess.mymusic.domain.MusicDetails
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface MusicRepository {
    fun save(musicDetails: MusicDetails): Mono<Void>
    fun get(id: String): Mono<MusicDetails>
    fun getAll(): Flux<MusicDetails>
    fun getByCategory(categoryId: String): Flux<MusicDetails>
}