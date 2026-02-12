package dev.michaelburgess.mymusic.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.core.io.AbstractResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import java.io.InputStream

@Component
@Profile("aws")
class S3MusicStorageProvider(
    private val s3Client: S3Client,
    @Value("\${aws.s3.bucket}") private val bucket: String
) : MusicStorageProvider {

    override fun getMusicResource(filename: String): Mono<Resource> {
        return Mono.fromCallable {
            S3Resource(s3Client, bucket, filename)
        }
    }
}

class S3Resource(
    private val s3Client: S3Client,
    private val bucket: String,
    private val key: String
) : AbstractResource() {

    override fun getDescription(): String = "S3 resource [bucket='$bucket', key='$key']"

    override fun getInputStream(): InputStream {
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build()
        return s3Client.getObject(getObjectRequest)
    }

    override fun contentLength(): Long {
        val headObjectRequest = HeadObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build()
        return s3Client.headObject(headObjectRequest).contentLength()
    }

    override fun exists(): Boolean {
        return try {
            val headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()
            s3Client.headObject(headObjectRequest)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getFilename(): String = key
}
