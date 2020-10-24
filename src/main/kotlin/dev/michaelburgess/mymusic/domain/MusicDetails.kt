package dev.michaelburgess.mymusic.domain

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import java.time.Instant
import java.util.*

@DynamoDbBean
data class MusicDetails(
        @get:DynamoDbPartitionKey
        var id: String? = UUID.randomUUID().toString(),
        var name: String? = null,
        var mixedBy: String? = null,
        var dateUploaded: Instant? = Instant.now(),
        var filename: String? = null)
