package dev.michaelburgess.mymusic.service

import dev.michaelburgess.mymusic.domain.MusicDetails
import dev.michaelburgess.mymusic.repository.MusicRepository
import org.springframework.core.io.Resource
import org.springframework.core.io.support.ResourceRegion
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import kotlin.math.min
import java.util.concurrent.atomic.AtomicInteger

@Service
class MusicService(
    private val musicRepository: MusicRepository,
    private val storageProvider: MusicStorageProvider
) {

    fun findById(id: String) : Mono<MusicDetails> {
        return musicRepository.get(id)
    }

    fun addNewMusic(musicDetails: MusicDetails) : Mono<Void> {
        return musicRepository.save(musicDetails)
    }

    fun listAllMusic() : Flux<MusicDetails> {
        return musicRepository.getAll()
    }

    fun listMusicByCategory(categoryId: String) : Flux<MusicDetails> {
        return musicRepository.getByCategory(categoryId)
    }

    private fun getFile(id: String) : Mono<Resource> {

        return findById(id)
                .flatMap { musicDetails: MusicDetails -> storageProvider.getMusicResource(musicDetails.filename!!) }
    }

    fun getRegion(id: String, request: ServerRequest): Mono<ResourceRegion> {
        val headers = request.headers().asHttpHeaders()
        val range = if (headers.range.isNotEmpty()) headers.range[0] else null
        val sizeInt = AtomicInteger(5)
        val chunkSize: Long = getChunkSize(sizeInt.get())
        val resourceMono: Mono<Resource> = getFile(id)
        return resourceMono.map { resource: Resource ->
            val contentLength: Long = resource.contentLength()
            if (range != null) {
                val start = range.getRangeStart(contentLength)
                val end = range.getRangeEnd(contentLength)
                val resourceLength = end - start + 1
                val rangeLength: Long = min(chunkSize, resourceLength)
                ResourceRegion(resource, start, rangeLength)
            } else {
                val rangeLength: Long = min(chunkSize,contentLength)
                ResourceRegion(resource, 0, rangeLength)
            }
        }
    }

    private fun getChunkSize(size: Int): Long {
        return when (size) {
            1 -> CHUNK_SIZE_VERY_LOW
            2 -> CHUNK_SIZE_LOW
            4 -> CHUNK_SIZE_HIGH
            5 -> CHUNK_SIZE_VERY_HIGH
            else -> CHUNK_SIZE_MED
        }
    }

    companion object {
        private const val BYTE_LENGTH: Long = 1024
        private const val CHUNK_SIZE_VERY_LOW = BYTE_LENGTH * 256
        private const val CHUNK_SIZE_LOW = BYTE_LENGTH * 512
        private const val CHUNK_SIZE_MED = BYTE_LENGTH * 1024
        private const val CHUNK_SIZE_HIGH = BYTE_LENGTH * 2048
        private const val CHUNK_SIZE_VERY_HIGH = CHUNK_SIZE_HIGH * 2
    }
}