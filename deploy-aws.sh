#!/bin/bash
set -e

echo "🚀 Starting AWS Deployment..."

# 0. Get AWS Details
echo "🔍 Fetching AWS Account Details..."
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGION=$(aws configure get region)
REGION=${REGION:-us-east-2}
ECR_REPO_NAME="music-streaming-api"
ECR_URI="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${ECR_REPO_NAME}"

# Export for CDK
export CDK_DEFAULT_ACCOUNT=$ACCOUNT_ID
export CDK_DEFAULT_REGION=$REGION

# Generate unique image tag based on timestamp
IMAGE_TAG=$(date +%Y%m%d%H%M%S)
echo "🏷️  Generated Image Tag: $IMAGE_TAG"

# 1. Ensure ECR Repository exists BEFORE CDK deployment
echo "🏗️ Ensuring ECR Repository '$ECR_REPO_NAME' exists..."
aws ecr create-repository --repository-name $ECR_REPO_NAME --region $REGION 2>/dev/null || echo "Repo already exists"

# 2. Build Backend
echo "📦 Building Backend JAR..."
#./mvnw clean package -DskipTests

# 3. Build UI
echo "📦 Building UI Assets (Production)..."
cd ../music-streaming-ui
rm -rf build/
# Force production mode to use relative paths for CloudFront proxy
REACT_APP_API_BASE="" npm run build
cd ../music-streaming-api

## 4. Docker Build & Push
#echo "🐳 Building Docker Image (linux/arm64)..."
#docker build --platform linux/arm64 -t $ECR_REPO_NAME .
#
#echo "🔐 Authenticating with ECR..."
#aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin ${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com
#
#echo "📤 Tagging and Pushing Image to ECR ($IMAGE_TAG and latest)..."
#docker tag ${ECR_REPO_NAME}:latest ${ECR_URI}:${IMAGE_TAG}
#docker tag ${ECR_REPO_NAME}:latest ${ECR_URI}:latest
#docker push ${ECR_URI}:${IMAGE_TAG}
#docker push ${ECR_URI}:latest
#
## 5. Deploy Certificate Infrastructure (Step 1 - Global us-east-1)
#echo "☁️ Deploying Certificate Stack in us-east-1..."
#cd infra
#cdk deploy MusicCertificateStack --require-approval never
#
## Fetch the Certificate ARN
#CERT_ARN=$(aws cloudformation describe-stacks --stack-name MusicCertificateStack --region us-east-1 --query "Stacks[0].Outputs[?OutputKey=='CertificateArn'].OutputValue" --output text)
#
#if [ "$CERT_ARN" == "None" ] || [ -z "$CERT_ARN" ]; then
#    echo "⚠️  Certificate not yet issued or stack failed. Proceeding without custom domain..."
#    PASS_CERT_ARN=""
#else
#    echo "📜 Certificate Issued: $CERT_ARN"
#    PASS_CERT_ARN="-c certificateArn=$CERT_ARN"
#fi
#
## 6. Deploy Main Infrastructure with CDK (passing the unique image tag and cert ARN)
#echo "☁️ Deploying Main Infrastructure with CDK..."
#cdk deploy MusicStreamingStack -c imageTag=$IMAGE_TAG $PASS_CERT_ARN --require-approval never
#cd ..
#
## 7. Service Stabilization
#echo "🔄 Checking ECS Service status..."
#ACTUAL_CLUSTER_NAME=$(aws ecs list-clusters --query "clusterArns[?contains(@, 'MusicCluster')]" --output text | awk -F'/' '{print $NF}')
#
#if [ ! -z "$ACTUAL_CLUSTER_NAME" ]; then
#    ACTUAL_SERVICE_NAME=$(aws ecs list-services --cluster $ACTUAL_CLUSTER_NAME --query "serviceArns[?contains(@, 'MusicService')]" --output text | awk -F'/' '{print $NF}')
#
#    if [ ! -z "$ACTUAL_SERVICE_NAME" ]; then
#        echo "⏳ Waiting for service to stabilize..."
#        aws ecs wait services-stable --cluster $ACTUAL_CLUSTER_NAME --services $ACTUAL_SERVICE_NAME
#        echo "✨ Service is now healthy and stable!"
#    fi
#fi
#
## 8. Fetch Stack Outputs
#echo "🔍 Fetching Deployment Details..."
STACK_NAME="MusicStreamingStack"
UI_BUCKET=$(aws cloudformation describe-stacks --stack-name $STACK_NAME --query "Stacks[0].Outputs[?OutputKey=='UiBucketName'].OutputValue" --output text)
#MUSIC_BUCKET=$(aws cloudformation describe-stacks --stack-name $STACK_NAME --query "Stacks[0].Outputs[?OutputKey=='MusicBucketName'].OutputValue" --output text)
CF_URL=$(aws cloudformation describe-stacks --stack-name $STACK_NAME --query "Stacks[0].Outputs[?OutputKey=='CloudFrontUrl'].OutputValue" --output text)
DIST_ID=$(aws cloudformation describe-stacks --stack-name $STACK_NAME --query "Stacks[0].Outputs[?OutputKey=='CloudFrontDistributionId'].OutputValue" --output text)
#
## 9. Sync S3 Files
#echo "📤 Uploading Music files to S3..."
#aws s3 sync /Users/michaelburgess/Personal/mymusic/ s3://$MUSIC_BUCKET/

echo "📤 Uploading UI assets to S3..."
aws s3 sync ../music-streaming-ui/build/ s3://$UI_BUCKET/ --delete --cache-control "max-age=0, no-cache, no-store, must-revalidate"

# 10. Invalidate CloudFront Cache
echo "🧹 Invalidating CloudFront Cache..."
aws cloudfront create-invalidation --distribution-id $DIST_ID --paths "/*" > /dev/null
#
## 11. Seed Categories in Production
#echo "🌱 Seeding Categories in AWS..."
#aws dynamodb batch-write-item \
#    --request-items '{
#        "Category": [
#            {
#                "PutRequest": {
#                    "Item": {
#                        "id": {"S": "michaels-mixes"},
#                        "name": {"S": "Michael'\''s Mixes"},
#                        "description": {"S": "My personal collection of curated mixes"},
#                        "coverArt": {"S": "https://picsum.photos/seed/cat1/300/300"}
#                    }
#                }
#            },
#            {
#                "PutRequest": {
#                    "Item": {
#                        "id": {"S": "house-techno"},
#                        "name": {"S": "House & Techno"},
#                        "description": {"S": "Deeper vibes and driving beats"},
#                        "coverArt": {"S": "https://picsum.photos/seed/cat2/300/300"}
#                    }
#                }
#            }
#        ]
#    }' \
#    --region $REGION

echo "✅ Deployment Complete!"
echo "🌐 App URL: $CF_URL"
echo "📦 Deployed Image Tag: $IMAGE_TAG"
