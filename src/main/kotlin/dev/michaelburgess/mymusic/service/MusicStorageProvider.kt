package dev.michaelburgess.mymusic.service

import org.springframework.core.io.Resource
import reactor.core.publisher.Mono

interface MusicStorageProvider {
    fun getMusicResource(filename: String): Mono<Resource>
}
