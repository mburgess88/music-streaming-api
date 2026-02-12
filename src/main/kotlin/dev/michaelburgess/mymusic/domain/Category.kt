package dev.michaelburgess.mymusic.domain

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

@DynamoDbBean
data class Category(
    @get:DynamoDbPartitionKey
    var id: String? = null,
    var name: String? = null,
    var description: String? = null,
    var coverArt: String? = null
)
