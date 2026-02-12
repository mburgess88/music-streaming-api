package dev.michaelburgess.mymusic.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import dev.michaelburgess.mymusic.repository.MusicRepository
import dev.michaelburgess.mymusic.repository.DynamoDbMusicRepository

@Configuration
@Profile("local")
open class LocalConfig {
    
    @Bean
    @Primary
    open fun musicRepository(): MusicRepository {
        return DynamoDbMusicRepository(
            dynamoDbEnhancedAsyncClient()
        )
    }
    
    @Bean
    open fun dynamoDbEnhancedAsyncClient(): software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient {
        val dynamoDbClient = software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient.builder()
            .endpointOverride(java.net.URI.create("http://localhost:4566"))
            .region(software.amazon.awssdk.regions.Region.US_EAST_2)
            .credentialsProvider(
                software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                    software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create("dummy", "dummy")
                )
            )
            .build()
            
        return software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient.builder()
            .dynamoDbClient(dynamoDbClient)
            .build()
    }
}