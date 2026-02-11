package dev.michaelburgess.mymusic.domain

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey
import java.time.Instant
import java.util.*

@DynamoDbBean
data class MusicDetails(
        @get:DynamoDbPartitionKey
        var id: String? = null,
        @get:DynamoDbSecondaryPartitionKey(indexNames = ["CategoryIndex"])
        var categoryId: String? = null,
        var name: String? = null,
        var mixedBy: String? = null,
        var dateUploaded: Instant? = null,
        var filename: String? = null,
        var coverArt: String? = null,
        var waveform: List<Double>? = null)
