package dev.michaelburgess.mymusic.repository

import dev.michaelburgess.mymusic.domain.Category
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema

@Repository
class DynamoDbCategoryRepository(private val enhancedAsyncClient: DynamoDbEnhancedAsyncClient) : CategoryRepository {

    private val categoryTable: DynamoDbAsyncTable<Category> = enhancedAsyncClient.table("Category", TableSchema.fromBean(Category::class.java))

    override fun save(category: Category): Mono<Void> {
        return Mono.fromFuture(categoryTable.putItem(category))
    }

    override fun get(id: String): Mono<Category> {
        return Mono.fromFuture(categoryTable.getItem(Key.builder().partitionValue(id).build()))
    }

    override fun getAll(): Flux<Category> {
        return Flux.from(categoryTable.scan().items())
    }
}
