package dev.michaelburgess.music.infra

import software.amazon.awscdk.App
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.Environment

fun main() {
    val app = App()

    val account = System.getenv("CDK_DEFAULT_ACCOUNT")
    
    val envEast2 = Environment.builder()
        .region("us-east-2")
        .account(account)
        .build()

    val envEast1 = Environment.builder()
        .region("us-east-1")
        .account(account)
        .build()

    // 1. Certificate Stack (Must be in us-east-1 for CloudFront)
    val certStack = CertificateStack(app, "MusicCertificateStack", StackProps.builder()
        .env(envEast1)
        .build())

    // 2. Main Infrastructure Stack
    val musicStack = MusicStack(app, "MusicStreamingStack", StackProps.builder()
        .env(envEast2)
        .build())

    app.synth()
}
