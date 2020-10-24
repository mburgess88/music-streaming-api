package dev.michaelburgess.mymusic.repository

import dev.michaelburgess.mymusic.domain.MusicDetails
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema

@Repository
class MusicRepository(private val enhancedAsyncClient: DynamoDbEnhancedAsyncClient) {

    private val musicDynamoDbAsyncTable: DynamoDbAsyncTable<MusicDetails> = enhancedAsyncClient.table(MusicDetails::class.java.simpleName, TableSchema.fromBean(MusicDetails::class.java))

    fun save(musicDetails: MusicDetails): Mono<Void> {
        return Mono.fromFuture(musicDynamoDbAsyncTable.putItem(musicDetails))
    }

    fun get(id: String): Mono<MusicDetails> {
        return Mono.fromFuture(musicDynamoDbAsyncTable.getItem(getKeyBuild(id)))
    }

    fun getAll(): Flux<MusicDetails> {
        return Flux.from(musicDynamoDbAsyncTable.scan().items())
    }

    private fun getKeyBuild(id: String): Key {
        return Key.builder().partitionValue(id).build()
    }

}