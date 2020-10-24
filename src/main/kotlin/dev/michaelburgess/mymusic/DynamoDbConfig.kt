package dev.michaelburgess.mymusic

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import java.net.URI

@Configuration
open class DynamoDbConfig(@Value("\${aws.dynamodb.endpoint}") private val dynamoDbEndPointUrl: String) {

    @Bean
    open fun dynamoDbAsyncClient() : DynamoDbAsyncClient {
        return DynamoDbAsyncClient.builder()
                .credentialsProvider(ProfileCredentialsProvider.create("default"))
                .endpointOverride(URI.create(dynamoDbEndPointUrl))
                .build()
    }

    @Bean
    open fun dynamoDbEnhancedAsyncClient() : DynamoDbEnhancedAsyncClient {
        return DynamoDbEnhancedAsyncClient.builder()
                .dynamoDbClient(dynamoDbAsyncClient())
                .build()
    }
}
