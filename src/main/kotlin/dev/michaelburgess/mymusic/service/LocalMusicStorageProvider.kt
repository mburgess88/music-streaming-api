package dev.michaelburgess.mymusic.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.io.File

@Component
@Profile("!aws")
class LocalMusicStorageProvider(
    @Value("\${music.local.path:/Users/michaelburgess/Personal/mymusic/}") private val localPath: String
) : MusicStorageProvider {
    
    override fun getMusicResource(filename: String): Mono<Resource> {
        return Mono.fromCallable {
            val path = if (localPath.endsWith("/")) localPath else "$localPath/"
            UrlResource("file:$path$filename")
        }
    }
}
