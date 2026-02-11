package dev.michaelburgess.mymusic.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import java.net.URI

@Configuration
open class DynamoDbConfig(
    @Value("\${aws.dynamodb.endpoint:}") private val dynamoDbEndPointUrl: String,
    @Value("\${aws.region:us-east-2}") private val awsRegion: String
) {

    @Bean
    open fun dynamoDbAsyncClient() : DynamoDbAsyncClient {
        val builder = DynamoDbAsyncClient.builder()
                .region(Region.of(awsRegion))
        
        if (dynamoDbEndPointUrl.isNotEmpty()) {
            builder.endpointOverride(URI.create(dynamoDbEndPointUrl))
            builder.credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("dummy", "dummy")
                )
            )
        }
        
        return builder.build()
    }

    @Bean
    open fun dynamoDbEnhancedAsyncClient() : DynamoDbEnhancedAsyncClient {
        return DynamoDbEnhancedAsyncClient.builder()
                .dynamoDbClient(dynamoDbAsyncClient())
                .build()
    }
}
