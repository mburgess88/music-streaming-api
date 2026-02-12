package dev.michaelburgess.mymusic.repository

import dev.michaelburgess.mymusic.domain.MusicDetails
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional

@Repository
class DynamoDbMusicRepository(private val enhancedAsyncClient: DynamoDbEnhancedAsyncClient) : MusicRepository {

    private val musicDynamoDbAsyncTable: DynamoDbAsyncTable<MusicDetails> = enhancedAsyncClient.table(MusicDetails::class.java.simpleName, TableSchema.fromBean(MusicDetails::class.java))

    override fun save(musicDetails: MusicDetails): Mono<Void> {
        return Mono.fromFuture(musicDynamoDbAsyncTable.putItem(musicDetails))
    }

    override fun get(id: String): Mono<MusicDetails> {
        return Mono.fromFuture(musicDynamoDbAsyncTable.getItem(getKeyBuild(id)))
    }

    override fun getAll(): Flux<MusicDetails> {
        return Flux.from(musicDynamoDbAsyncTable.scan().items())
    }

    override fun getByCategory(categoryId: String): Flux<MusicDetails> {
        val index = musicDynamoDbAsyncTable.index("CategoryIndex")
        val queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(categoryId).build())
        return Flux.from(index.query(queryConditional)).flatMapIterable { it.items() }
    }

    private fun getKeyBuild(id: String): Key {
        return Key.builder().partitionValue(id).build()
    }
}