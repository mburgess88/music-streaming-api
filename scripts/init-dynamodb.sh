#!/bin/bash
set -e

# DynamoDB initialization script for LocalStack with UUID-based IDs and Real Waveforms
echo "Starting DynamoDB initialization with Real Waveforms..."

# Configuration
AWS_REGION="us-east-2"
DYNAMODB_ENDPOINT="http://127.0.0.1:4566"
S3_ENDPOINT="http://127.0.0.1:4566"
TABLE_NAME="MusicDetails"
BUCKET_NAME="music-ui-static-site"
MUSIC_STORAGE_BUCKET="music-streaming-storage"
UI_PROJECT_PATH="/tmp/ui-source"
MUSIC_SOURCE_PATH="/tmp/music-source"

# Waveform Data (calculated from actual MP3s)
TEST_TRACK_WAVEFORM='[{"N":"0.25"},{"N":"0.17"},{"N":"0.16"},{"N":"0.13"},{"N":"0.12"},{"N":"0.15"},{"N":"0.25"},{"N":"0.23"},{"N":"0.23"},{"N":"0.25"},{"N":"0.20"},{"N":"0.31"},{"N":"0.26"},{"N":"0.27"},{"N":"0.27"},{"N":"0.23"},{"N":"0.20"},{"N":"0.30"},{"N":"0.16"},{"N":"0.10"},{"N":"0.55"},{"N":"0.62"},{"N":"0.62"},{"N":"0.62"},{"N":"0.59"},{"N":"0.53"},{"N":"0.52"},{"N":"0.46"},{"N":"0.47"},{"N":"0.54"},{"N":"0.43"},{"N":"0.44"},{"N":"0.45"},{"N":"0.46"},{"N":"0.46"},{"N":"0.50"},{"N":"0.55"},{"N":"0.76"},{"N":"0.96"},{"N":"0.93"},{"N":"0.94"},{"N":"0.91"},{"N":"0.88"},{"N":"0.93"},{"N":"0.97"},{"N":"0.94"},{"N":"0.95"},{"N":"0.96"},{"N":"0.79"},{"N":"0.96"},{"N":"0.94"},{"N":"0.92"},{"N":"0.90"},{"N":"0.98"},{"N":"0.71"},{"N":"1.00"},{"N":"0.90"},{"N":"0.92"},{"N":"0.92"},{"N":"0.97"},{"N":"0.53"},{"N":"0.68"},{"N":"0.67"},{"N":"0.63"},{"N":"0.65"},{"N":"0.55"},{"N":"0.62"},{"N":"0.58"},{"N":"0.55"},{"N":"0.65"},{"N":"0.68"},{"N":"0.54"},{"N":"0.92"},{"N":"0.97"},{"N":"0.94"},{"N":"0.94"},{"N":"0.97"},{"N":"0.82"},{"N":"0.97"},{"N":"0.97"},{"N":"0.95"},{"N":"0.95"},{"N":"1.00"},{"N":"0.83"},{"N":"0.96"},{"N":"0.92"},{"N":"0.95"},{"N":"0.92"},{"N":"0.87"},{"N":"0.78"},{"N":"0.93"},{"N":"0.93"},{"N":"0.94"},{"N":"0.92"},{"N":"0.74"},{"N":"0.64"},{"N":"0.56"},{"N":"0.61"},{"N":"0.54"},{"N":"0.43"}]'
HOUSE_TECHNO_WAVEFORM='[{"N":"0.64"},{"N":"0.67"},{"N":"0.69"},{"N":"0.61"},{"N":"0.23"},{"N":"0.31"},{"N":"0.68"},{"N":"0.71"},{"N":"0.71"},{"N":"0.58"},{"N":"0.72"},{"N":"0.49"},{"N":"0.64"},{"N":"0.80"},{"N":"0.71"},{"N":"0.59"},{"N":"0.84"},{"N":"0.94"},{"N":"0.78"},{"N":"0.90"},{"N":"0.88"},{"N":"0.84"},{"N":"0.57"},{"N":"0.91"},{"N":"1.00"},{"N":"0.35"},{"N":"0.85"},{"N":"0.98"},{"N":"0.81"},{"N":"0.64"},{"N":"0.70"},{"N":"0.55"},{"N":"0.62"},{"N":"0.66"},{"N":"0.65"},{"N":"0.58"},{"N":"0.59"},{"N":"0.85"},{"N":"0.56"},{"N":"0.75"},{"N":"0.85"},{"N":"0.71"},{"N":"0.67"},{"N":"0.54"},{"N":"0.63"},{"N":"0.91"},{"N":"0.75"},{"N":"0.65"},{"N":"0.76"},{"N":"0.47"},{"N":"0.68"},{"N":"0.76"},{"N":"0.46"},{"N":"0.59"},{"N":"1.00"},{"N":"0.70"},{"N":"0.72"},{"N":"0.99"},{"N":"0.88"},{"N":"0.60"},{"N":"0.54"},{"N":"0.86"},{"N":"0.78"},{"N":"0.73"},{"N":"0.67"},{"N":"0.43"},{"N":"0.72"},{"N":"0.81"},{"N":"0.89"},{"N":"0.56"},{"N":"0.66"},{"N":"0.72"},{"N":"0.93"},{"N":"0.77"},{"N":"0.68"},{"N":"0.91"},{"N":"0.67"},{"N":"0.92"},{"N":"0.96"},{"N":"0.94"},{"N":"0.36"},{"N":"0.72"},{"N":"0.87"},{"N":"0.68"},{"N":"0.71"},{"N":"0.28"},{"N":"0.63"},{"N":"0.70"},{"N":"0.85"},{"N":"0.77"},{"N":"0.64"},{"N":"0.51"},{"N":"0.81"},{"N":"0.87"},{"N":"0.88"},{"N":"0.72"},{"N":"0.60"},{"N":"0.85"},{"N":"0.71"},{"N":"0.82"}]'

# Fallback for placeholders
FALLBACK_WAVEFORM='[{"N":"0.70"},{"N":"0.78"},{"N":"0.84"},{"N":"0.88"},{"N":"0.88"},{"N":"0.86"},{"N":"0.80"},{"N":"0.72"},{"N":"0.63"},{"N":"0.54"},{"N":"0.46"},{"N":"0.40"},{"N":"0.37"},{"N":"0.38"},{"N":"0.41"},{"N":"0.46"},{"N":"0.52"},{"N":"0.57"},{"N":"0.61"},{"N":"0.63"},{"N":"0.61"},{"N":"0.57"},{"N":"0.50"},{"N":"0.41"},{"N":"0.32"},{"N":"0.23"},{"N":"0.16"},{"N":"0.12"},{"N":"0.12"},{"N":"0.14"},{"N":"0.19"},{"N":"0.27"},{"N":"0.35"},{"N":"0.42"},{"N":"0.48"},{"N":"0.51"},{"N":"0.51"},{"N":"0.49"},{"N":"0.44"},{"N":"0.38"},{"N":"0.31"},{"N":"0.26"},{"N":"0.22"},{"N":"0.22"},{"N":"0.25"},{"N":"0.31"},{"N":"0.39"},{"N":"0.49"},{"N":"0.59"},{"N":"0.67"},{"N":"0.74"},{"N":"0.78"},{"N":"0.78"},{"N":"0.75"},{"N":"0.70"},{"N":"0.64"},{"N":"0.58"},{"N":"0.52"},{"N":"0.49"},{"N":"0.49"},{"N":"0.51"},{"N":"0.56"},                     {"N":"0.63"},{"N":"0.71"},{"N":"0.79"},{"N":"0.85"},{"N":"0.88"},{"N":"0.88"},{"N":"0.85"},{"N":"0.79"},{"N":"0.70"},{"N":"0.61"},{"N":"0.52"},{"N":"0.45"},{"N":"0.39"},{"N":"0.37"},{"N":"0.38"},{"N":"0.42"},{"N":"0.47"},{"N":"0.53"},{"N":"0.58"},{"N":"0.62"},{"N":"0.63"},{"N":"0.61"},{"N":"0.56"},{"N":"0.49"},{"N":"0.40"},{"N":"0.30"},{"N":"0.22"},{"N":"0.15"},{"N":"0.12"},{"N":"0.12"},{"N":"0.15"},{"N":"0.21"},{"N":"0.28"},{"N":"0.36"},{"N":"0.43"},{"N":"0.49"},{"N":"0.51"},{"N":"0.51"}]'

# Configure AWS CLI
export AWS_ACCESS_KEY_ID=dummy
export AWS_SECRET_ACCESS_KEY=dummy
export AWS_DEFAULT_REGION=$AWS_REGION

# Wait for DynamoDB
echo "Waiting for DynamoDB to be ready..."
max_attempts=30
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if curl -s --connect-timeout 2 $DYNAMODB_ENDPOINT >/dev/null 2>&1; then
        echo "DynamoDB endpoint is accessible!"
        break
    fi
    attempt=$((attempt + 1))
    echo "Attempt $attempt/$max_attempts: Waiting for DynamoDB..."
    sleep 2
done

# Delete and Recreate Tables
echo "Recreating tables..."
aws dynamodb delete-table --table-name $TABLE_NAME --endpoint-url $DYNAMODB_ENDPOINT 2>/dev/null || true
aws dynamodb delete-table --table-name "Category" --endpoint-url $DYNAMODB_ENDPOINT 2>/dev/null || true
sleep 5

aws dynamodb create-table \
    --table-name $TABLE_NAME \
    --attribute-definitions \
        AttributeName=id,AttributeType=S \
        AttributeName=categoryId,AttributeType=S \
    --key-schema AttributeName=id,KeyType=HASH \
    --global-secondary-indexes \
        "[{\"IndexName\": \"CategoryIndex\", \"KeySchema\": [{\"AttributeName\": \"categoryId\", \"KeyType\": \"HASH\"}], \"Projection\": {\"ProjectionType\": \"ALL\"}}]" \
    --billing-mode PAY_PER_REQUEST \
    --endpoint-url $DYNAMODB_ENDPOINT

aws dynamodb create-table \
    --table-name "Category" \
    --attribute-definitions AttributeName=id,AttributeType=S \
    --key-schema AttributeName=id,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --endpoint-url $DYNAMODB_ENDPOINT

aws dynamodb wait table-exists --table-name $TABLE_NAME --endpoint-url $DYNAMODB_ENDPOINT
aws dynamodb wait table-exists --table-name "Category" --endpoint-url $DYNAMODB_ENDPOINT

# Seed Categories
echo "Seeding Categories..."
aws dynamodb batch-write-item \
    --request-items '{
        "Category": [
            {
                "PutRequest": {
                    "Item": {
                        "id": {"S": "michaels-mixes"},
                        "name": {"S": "Michael'\''s Mixes"},
                        "description": {"S": "My personal collection of curated mixes"},
                        "coverArt": {"S": "https://picsum.photos/seed/cat1/300/300"}
                    }
                }
            },
            {
                "PutRequest": {
                    "Item": {
                        "id": {"S": "house-techno"},
                        "name": {"S": "House & Techno"},
                        "description": {"S": "Deeper vibes and driving beats"},
                        "coverArt": {"S": "https://picsum.photos/seed/cat2/300/300"}
                    }
                }
            }
        ]
    }' \
    --endpoint-url $DYNAMODB_ENDPOINT

# Seed Data with Real Waveforms
echo "Seeding tracks..."
aws dynamodb batch-write-item \
    --request-items '{
        "MusicDetails": [
            {
                "PutRequest": {
                    "Item": {
                        "id": {"S": "f47ac10b-58cc-4372-a567-0e02b2c3d479"},
                        "categoryId": {"S": "michaels-mixes"},
                        "name": {"S": "Summer Vibes"},
                        "mixedBy": {"S": "DJ Michael"},
                        "filename": {"S": "summer_vibes.mp3"},
                        "coverArt": {"S": "https://picsum.photos/seed/music1/60/60"},
                        "dateUploaded": {"S": "2026-01-25T00:00:00Z"},
                        "waveform": {"L": '"$FALLBACK_WAVEFORM"'}
                    }
                }
            },
            {
                "PutRequest": {
                    "Item": {
                        "id": {"S": "c9a3b7d1-2d4e-4f56-b8e3-9a4c1d2c5f6e"},
                        "categoryId": {"S": "michaels-mixes"},
                        "name": {"S": "Night Drive"},
                        "mixedBy": {"S": "Michael Burgess"},
                        "filename": {"S": "night_drive.mp3"},
                        "coverArt": {"S": "https://picsum.photos/seed/music2/60/60"},
                        "dateUploaded": {"S": "2026-01-24T23:00:00Z"},
                        "waveform": {"L": '"$FALLBACK_WAVEFORM"'}
                    }
                }
            },
            {
                "PutRequest": {
                    "Item": {
                        "id": {"S": "a1b2c3d4-5e6f-7a8b-9c0d-1e2f3a4b5c6d"},
                        "categoryId": {"S": "michaels-mixes"},
                        "name": {"S": "Morning Coffee"},
                        "mixedBy": {"S": "DJ Michael"},
                        "filename": {"S": "morning_coffee.mp3"},
                        "coverArt": {"S": "https://picsum.photos/seed/music3/60/60"},
                        "dateUploaded": {"S": "2026-01-24T22:00:00Z"},
                        "waveform": {"L": '"$FALLBACK_WAVEFORM"'}
                    }
                }
            },
            {
                "PutRequest": {
                    "Item": {
                        "id": {"S": "7d8e9f0a-1b2c-3d4e-5f6a-7b8c9d0e1f2a"},
                        "categoryId": {"S": "michaels-mixes"},
                        "name": {"S": "Test Track"},
                        "mixedBy": {"S": "Test Artist"},
                        "filename": {"S": "test.mp3"},
                        "coverArt": {"S": "https://picsum.photos/seed/music4/60/60"},
                        "dateUploaded": {"S": "2026-01-24T20:00:00Z"},
                        "waveform": {"L": '"$TEST_TRACK_WAVEFORM"'}
                    }
                }
            },
            {
                "PutRequest": {
                    "Item": {
                        "id": {"S": "b56e9a82-3d4c-4f56-b8e3-9a4c1d2c5f6e"},
                        "categoryId": {"S": "house-techno"},
                        "name": {"S": "Melodic House & Techno Mix"},
                        "mixedBy": {"S": "DJ Michael"},
                        "filename": {"S": "melodic-house-techno-mix.mp3"},
                        "coverArt": {"S": "https://picsum.photos/seed/mix1/60/60"},
                        "dateUploaded": {"S": "2026-02-08T18:00:00Z"},
                        "waveform": {"L": '"$HOUSE_TECHNO_WAVEFORM"'}
                    }
                }
            }
        ]
    }' \
    --endpoint-url $DYNAMODB_ENDPOINT

echo "Music storage S3 setup starting..."
aws s3api create-bucket --bucket $MUSIC_STORAGE_BUCKET --endpoint-url $S3_ENDPOINT --region $AWS_REGION 2>/dev/null || echo "Bucket $MUSIC_STORAGE_BUCKET already exists"

if [ -d "$MUSIC_SOURCE_PATH" ]; then
    echo "Syncing music files from $MUSIC_SOURCE_PATH to s3://$MUSIC_STORAGE_BUCKET..."
    aws s3 sync "$MUSIC_SOURCE_PATH" "s3://$MUSIC_STORAGE_BUCKET" --endpoint-url $S3_ENDPOINT
    echo "✅ Music sync completed!"
else
    echo "⚠️  Music source path $MUSIC_SOURCE_PATH not found"
fi

echo "S3 UI setup starting..."
# (Rest of the S3 setup remains the same)
aws s3api create-bucket --bucket $BUCKET_NAME --endpoint-url $S3_ENDPOINT --region $AWS_REGION 2>/dev/null || echo "Bucket exists"
aws s3 website s3://$BUCKET_NAME --index-document index.html --error-document index.html --endpoint-url $S3_ENDPOINT 2>/dev/null

cat > /tmp/bucket-policy.json << EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "PublicReadGetObject",
            "Effect": "Allow",
            "Principal": "*",
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::$BUCKET_NAME/*"
        }
    ]
}
EOF
aws s3api put-bucket-policy --bucket $BUCKET_NAME --policy file:///tmp/bucket-policy.json --endpoint-url $S3_ENDPOINT 2>/dev/null

echo "Building React UI..."
if [ -d "$UI_PROJECT_PATH" ]; then
    cd "$UI_PROJECT_PATH"
    [ ! -d "node_modules" ] && npm install
    npm run build
    aws s3 sync build/ "s3://$BUCKET_NAME" --delete --endpoint-url $S3_ENDPOINT
    echo "✅ S3 static website setup completed!"
fi
echo "🎉 Initialization completed!"
