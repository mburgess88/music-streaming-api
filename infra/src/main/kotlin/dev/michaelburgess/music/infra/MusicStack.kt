package dev.michaelburgess.music.infra

import software.amazon.awscdk.CfnOutput
import software.amazon.awscdk.Fn
import software.amazon.awscdk.RemovalPolicy
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.autoscaling.*
import software.amazon.awscdk.services.budgets.CfnBudget
import software.amazon.awscdk.services.certificatemanager.Certificate
import software.amazon.awscdk.services.cloudfront.*
import software.amazon.awscdk.services.cloudfront.origins.S3Origin
import software.amazon.awscdk.services.cloudfront.origins.HttpOrigin
import software.amazon.awscdk.services.cloudfront.origins.HttpOriginProps
import software.amazon.awscdk.services.dynamodb.Attribute
import software.amazon.awscdk.services.dynamodb.AttributeType
import software.amazon.awscdk.services.dynamodb.BillingMode
import software.amazon.awscdk.services.dynamodb.Table
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps
import software.amazon.awscdk.services.dynamodb.ProjectionType
import software.amazon.awscdk.services.ec2.*
import software.amazon.awscdk.services.ecr.Repository
import software.amazon.awscdk.services.ecs.*
import software.amazon.awscdk.services.ecs.HealthCheck
import software.amazon.awscdk.services.iam.*
import software.amazon.awscdk.services.s3.Bucket
import software.amazon.awscdk.services.s3.BucketEncryption
import software.constructs.Construct

class MusicStack(scope: Construct, id: String, props: StackProps?) : Stack(scope, id, props) {

    init {
        val domainName = "mixes.michaelburgess.dev"
        val certArn = node.tryGetContext("certificateArn") as? String
        
        // 0. Billing: Zero-Cost Budget Alert
        CfnBudget.Builder.create(this, "ZeroCostBudget")

            .budget(CfnBudget.BudgetDataProperty.builder()
                .budgetLimit(CfnBudget.SpendProperty.builder()
                    .amount(0.01)
                    .unit("USD")
                    .build())
                .budgetType("COST")
                .timeUnit("MONTHLY")
                .build())
            .notificationsWithSubscribers(listOf(
                CfnBudget.NotificationWithSubscribersProperty.builder()
                    .notification(CfnBudget.NotificationProperty.builder()
                        .comparisonOperator("GREATER_THAN")
                        .notificationType("ACTUAL")
                        .threshold(100.0)
                        .build())
                    .subscribers(listOf(
                        CfnBudget.SubscriberProperty.builder()
                            .address("michael@michaelburgess.dev")
                            .subscriptionType("EMAIL")
                            .build()
                    ))
                    .build(),
                CfnBudget.NotificationWithSubscribersProperty.builder()
                    .notification(CfnBudget.NotificationProperty.builder()
                        .comparisonOperator("GREATER_THAN")
                        .notificationType("FORECASTED")
                        .threshold(100.0)
                        .build())
                    .subscribers(listOf(
                        CfnBudget.SubscriberProperty.builder()
                            .address("michael@michaelburgess.dev")
                            .subscriptionType("EMAIL")
                            .build()
                    ))
                    .build()
            ))
            .build()

        // 1. Storage: DynamoDB
        val musicTable = Table.Builder.create(this, "MusicDetailsTable")
            .tableName("MusicDetails")
            .partitionKey(Attribute.builder().name("id").type(AttributeType.STRING).build())
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .removalPolicy(RemovalPolicy.DESTROY)
            .build()

        musicTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
            .indexName("CategoryIndex")
            .partitionKey(Attribute.builder().name("categoryId").type(AttributeType.STRING).build())
            .projectionType(ProjectionType.ALL)
            .build())

        val categoryTable = Table.Builder.create(this, "CategoryTable")
            .tableName("Category")
            .partitionKey(Attribute.builder().name("id").type(AttributeType.STRING).build())
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .removalPolicy(RemovalPolicy.DESTROY)
            .build()

        // 2. Storage: S3 for Music
        val musicBucket = Bucket.Builder.create(this, "MusicStorageBucket")
            .bucketName("music-streaming-storage")
            .encryption(BucketEncryption.S3_MANAGED)
            .removalPolicy(RemovalPolicy.DESTROY)
            .autoDeleteObjects(true)
            .build()

        // 3. Storage: S3 for UI Hosting
        val uiBucket = Bucket.Builder.create(this, "UiHostingBucket")
            .removalPolicy(RemovalPolicy.DESTROY)
            .autoDeleteObjects(true)
            .build()
        
        // 4. ECR Repository (Imported because it's created in the deploy script)
        val ecrRepo = Repository.fromRepositoryName(this, "MusicAppRepo", "music-streaming-api")

        // 5. Networking: VPC
        val vpc = Vpc.Builder.create(this, "MusicVpc")
            .maxAzs(2)
            .subnetConfiguration(listOf(
                SubnetConfiguration.builder()
                    .name("Public")
                    .subnetType(SubnetType.PUBLIC)
                    .build()
            ))
            .build()

        // Elastic IP for the static backend endpoint
        val eip = CfnEIP.Builder.create(this, "MusicEip").build()

        // 6. Compute: ECS Cluster
        val cluster = Cluster.Builder.create(this, "MusicCluster")
            .vpc(vpc)
            .build()

        val securityGroup = SecurityGroup.Builder.create(this, "MusicEc2Sg")
            .vpc(vpc)
            .allowAllOutbound(true)
            .build()
        
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(8080), "Allow Spring Boot access")
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(22), "Allow SSH")

        // Role for EC2 Instance
        val instanceRole = Role.Builder.create(this, "MusicInstanceRole")
            .assumedBy(ServicePrincipal("ec2.amazonaws.com"))
            .managedPolicies(listOf(
                ManagedPolicy.fromAwsManagedPolicyName("service-role/AmazonEC2ContainerServiceforEC2Role")
            ))
            .build()

        instanceRole.addToPrincipalPolicy(PolicyStatement.Builder.create()
            .actions(listOf("ec2:AssociateAddress"))
            .resources(listOf("*"))
            .build())

        // Create UserData explicitly
        val userData = UserData.forLinux()
        userData.addCommands(
            "exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1",
            "echo ECS_CLUSTER=${cluster.clusterName} >> /etc/ecs/ecs.config",
            "TOKEN=$(curl -X PUT \"http://169.254.169.254/latest/api/token\" -H \"X-aws-ec2-metadata-token-ttl-seconds: 21600\")",
            "INSTANCE_ID=$(curl -H \"X-aws-metadata-token: \$TOKEN\" http://169.254.169.254/latest/meta-data/instance-id)",
            "echo \"Associating EIP ${eip.attrAllocationId} with instance \$INSTANCE_ID...\"",
            "for i in {1..5}; do",
            "  aws ec2 associate-address --instance-id \$INSTANCE_ID --allocation-id ${eip.attrAllocationId} --region ${this.region} && break || sleep 10",
            "done"
        )

        // Launch Template
        val launchTemplate = LaunchTemplate.Builder.create(this, "MusicLaunchTemplate")
            .instanceType(InstanceType.of(InstanceClass.T4G, InstanceSize.SMALL))
            .machineImage(EcsOptimizedImage.amazonLinux2(AmiHardwareType.ARM))
            .securityGroup(securityGroup)
            .role(instanceRole)
            .userData(userData)
            .build()

        // ASG using Launch Template
        val asg = AutoScalingGroup.Builder.create(this, "MusicAsg")
            .vpc(vpc)
            .launchTemplate(launchTemplate)
            .minCapacity(1)
            .maxCapacity(1)
            .desiredCapacity(1)
            .build()

        val capacityProvider = AsgCapacityProvider.Builder.create(this, "AsgCapacityProvider")
            .autoScalingGroup(asg)
            .enableManagedTerminationProtection(false)
            .build()
        
        cluster.addAsgCapacityProvider(capacityProvider)

        // Nightly Sleep Schedule
        asg.scaleOnSchedule("NightlyStop", ScheduledActionProps.builder()
            .autoScalingGroup(asg)
            .schedule(Schedule.cron(CronOptions.builder().hour("22").minute("0").build()))
            .minCapacity(0)
            .maxCapacity(0)
            .desiredCapacity(0)
            .build())

        asg.scaleOnSchedule("MorningStart", ScheduledActionProps.builder()
            .autoScalingGroup(asg)
            .schedule(Schedule.cron(CronOptions.builder().hour("8").minute("0").build()))
            .minCapacity(1)
            .maxCapacity(1)
            .desiredCapacity(1)
            .build())

        // 7. ECS Task Definition
        val taskDefinition = Ec2TaskDefinition.Builder.create(this, "MusicTaskDef")
            .build()
        
        // Use the image tag passed from the deploy script, default to 'latest'
        val imageTag = node.tryGetContext("imageTag") as? String ?: "latest"

        val container = taskDefinition.addContainer("MusicContainer", ContainerDefinitionOptions.builder()
            .image(ContainerImage.fromEcrRepository(ecrRepo, imageTag))
            .memoryLimitMiB(512) 
            .cpu(512)
            .healthCheck(HealthCheck.builder()
                .command(listOf("CMD-SHELL", "wget -q -O - http://localhost:8080/api/music || exit 1"))
                .interval(software.amazon.awscdk.Duration.seconds(30))
                .timeout(software.amazon.awscdk.Duration.seconds(5))
                .retries(3)
                .startPeriod(software.amazon.awscdk.Duration.seconds(60))
                .build())
            .logging(LogDriver.awsLogs(AwsLogDriverProps.builder().streamPrefix("MusicApp").build()))
            .environment(mapOf(
                "SPRING_PROFILES_ACTIVE" to "aws",
                "AWS_REGION" to this.region,
                "AWS_S3_BUCKET" to musicBucket.bucketName,
                "DYNAMODB_TABLE_NAME" to musicTable.tableName,
                "AWS_DYNAMODB_ENDPOINT" to "",
                "AWS_S3_ENDPOINT" to ""
            ))
            .build())

        container.addPortMappings(PortMapping.builder()
            .containerPort(8080)
            .hostPort(8080)
            .build())

        musicBucket.grantRead(taskDefinition.taskRole)
        musicTable.grantReadWriteData(taskDefinition.taskRole)
        categoryTable.grantReadWriteData(taskDefinition.taskRole)
        ecrRepo.grantPull(taskDefinition.executionRole!!)

        val service = Ec2Service.Builder.create(this, "MusicService")
            .cluster(cluster)
            .taskDefinition(taskDefinition)
            .desiredCount(1)
            .minHealthyPercent(0)
            .maxHealthyPercent(100)
            .capacityProviderStrategies(listOf(
                CapacityProviderStrategy.builder()
                    .capacityProvider(capacityProvider.capacityProviderName)
                    .weight(1)
                    .build()
            ))
            .build()

        // Disable Availability Zone Rebalancing using an escape hatch
        // This is required when maximumPercent <= 100 on a single-instance cluster.
        val cfnService = service.node.defaultChild as CfnService
        cfnService.addPropertyOverride("AvailabilityZoneRebalancing", "DISABLED")

        // 8. CloudFront Distribution
        val dashedIp = Fn.join("-", Fn.split(".", eip.attrPublicIp))
        val backendDns = "ec2-$dashedIp.${this.region}.compute.amazonaws.com"

        val distributionBuilder = Distribution.Builder.create(this, "MusicDistribution")
            .defaultRootObject("index.html")
            .errorResponses(listOf(
                ErrorResponse.builder()
                    .httpStatus(403)
                    .responseHttpStatus(200)
                    .responsePagePath("/index.html")
                    .build(),
                ErrorResponse.builder()
                    .httpStatus(404)
                    .responseHttpStatus(200)
                    .responsePagePath("/index.html")
                    .build()
            ))
            .defaultBehavior(BehaviorOptions.builder()
                .origin(S3Origin(uiBucket))
                .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                .build())
            .additionalBehaviors(mapOf(
                "/api/*" to BehaviorOptions.builder()
                    .origin(HttpOrigin(backendDns, HttpOriginProps.builder()
                        .httpPort(8080)
                        .protocolPolicy(OriginProtocolPolicy.HTTP_ONLY)
                        .build()))
                    .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                    .allowedMethods(AllowedMethods.ALLOW_ALL)
                    .cachePolicy(CachePolicy.CACHING_DISABLED)
                    .originRequestPolicy(OriginRequestPolicy.ALL_VIEWER)
                    .build()
            ))

        if (certArn != null && certArn.isNotEmpty()) {
            val certificate = Certificate.fromCertificateArn(this, "CloudFrontCertificate", certArn)
            distributionBuilder
                .domainNames(listOf(domainName))
                .certificate(certificate)
        }

        val distribution = distributionBuilder.build()

        CfnOutput.Builder.create(this, "UiBucketName").value(uiBucket.bucketName).build()
        CfnOutput.Builder.create(this, "MusicBucketName").value(musicBucket.bucketName).build()
        CfnOutput.Builder.create(this, "CloudFrontUrl").value("https://${distribution.distributionDomainName}").build()
        CfnOutput.Builder.create(this, "CloudFrontDistributionId").value(distribution.distributionId).build()
        CfnOutput.Builder.create(this, "Ec2PublicIp").value(eip.attrPublicIp).build()
    }
}
