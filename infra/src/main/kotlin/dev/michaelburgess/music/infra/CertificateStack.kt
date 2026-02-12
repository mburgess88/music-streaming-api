package dev.michaelburgess.music.infra

import software.amazon.awscdk.CfnOutput
import software.amazon.awscdk.RemovalPolicy
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.certificatemanager.Certificate
import software.amazon.awscdk.services.certificatemanager.CertificateValidation
import software.constructs.Construct

class CertificateStack(scope: Construct, id: String, props: StackProps?) : Stack(scope, id, props) {

    init {
        val domainName = "mixes.michaelburgess.dev"

        val certificate = Certificate.Builder.create(this, "MusicCertificate")
            .domainName(domainName)
            .validation(CertificateValidation.fromDns()) // Requires manual DNS record addition in Porkbun
            .build()

        CfnOutput.Builder.create(this, "CertificateArn")
            .value(certificate.certificateArn)
            .build()
            
        // This output helps the user know what to add to Porkbun if not automated via Route53
        // Note: CDK will show these in the terminal during deployment
    }
}
