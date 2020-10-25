package dev.michaelburgess.mymusic.service

import dev.michaelburgess.mymusic.domain.MusicDetails
import dev.michaelburgess.mymusic.repository.MusicRepository
import org.springframework.core.io.UrlResource
import org.springframework.core.io.support.ResourceRegion
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import java.lang.Long.min
import java.util.concurrent.atomic.AtomicInteger

@Service
class MusicService(private val musicRepository: MusicRepository) {

    fun findById(id: String) : Mono<MusicDetails> {
        return musicRepository.get(id)
    }

    fun addNewMusic(musicDetails: MusicDetails) : Mono<Void> {
        return musicRepository.save(musicDetails)
    }

    fun listAllMusic() : Flux<MusicDetails> {
        return musicRepository.getAll()
    }

    private fun getFile(id: String) : Mono<UrlResource> {

        return findById(id)
                .flatMap { musicDetails: MusicDetails -> createUriResource(musicDetails)}
    }

    private fun createUriResource(musicDetails: MusicDetails): Mono<UrlResource> {
        return Mono.create { monoSink: MonoSink<UrlResource> ->
            val file = UrlResource("file://Users/michaelburgess/Personal/mymusic/" + musicDetails.filename)
            monoSink.success(file)
        }
    }

    fun getRegion(id: String, request: ServerRequest): Mono<ResourceRegion> {
        val headers = request.headers().asHttpHeaders()
        val range = if (headers.range.isNotEmpty()) headers.range[0] else null
        val sizeInt = AtomicInteger(5)
        val chunkSize: Long = getChunkSize(sizeInt.get())
        val resource: Mono<UrlResource> = getFile(id)
        return resource.map { urlResource: UrlResource ->
            val contentLength: Long = urlResource.contentLength()
            if (range != null) {
                val start = range.getRangeStart(contentLength)
                val end = range.getRangeEnd(contentLength)
                val resourceLength = end - start + 1
                val rangeLength: Long = min(chunkSize, resourceLength)
                ResourceRegion(urlResource, start, rangeLength)
            } else {
                val rangeLength: Long = min(chunkSize,contentLength)
                ResourceRegion(urlResource, 0, rangeLength)
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