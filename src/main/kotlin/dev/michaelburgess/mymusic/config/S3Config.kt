package dev.michaelburgess.mymusic.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI

@Configuration
@Profile("aws")
open class S3Config {

    @Value("\${aws.region:us-east-2}")
    private lateinit var region: String

    @Value("\${aws.s3.endpoint:}")
    private var endpoint: String? = null

    @Bean
    open fun s3AsyncClient(): S3AsyncClient {
        val builder = S3AsyncClient.builder()
            .region(Region.of(region))
        
        if (!endpoint.isNullOrEmpty()) {
            builder.endpointOverride(URI.create(endpoint!!))
            builder.credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("dummy", "dummy")
                )
            )
        }
        
        return builder.build()
    }

    @Bean
    open fun s3Client(): S3Client {
        val builder = S3Client.builder()
            .region(Region.of(region))
        
        if (!endpoint.isNullOrEmpty()) {
            builder.endpointOverride(URI.create(endpoint!!))
            builder.credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("dummy", "dummy")
                )
            )
        }
        
        return builder.build()
    }
}
