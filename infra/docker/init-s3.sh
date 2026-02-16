#!/bin/bash
# ==============================================================
# init-s3.sh — Create S3 bucket in LocalStack (idempotent)
# ==============================================================
set -euo pipefail

BUCKET_NAME="${S3_BUCKET:-local-bucket}"
ENDPOINT="${AWS_ENDPOINT_URL:-http://localstack:4566}"

echo "Waiting for LocalStack S3 to be ready..."
for i in $(seq 1 30); do
  if aws --endpoint-url="$ENDPOINT" s3api head-bucket --bucket "$BUCKET_NAME" 2>/dev/null; then
    echo "Bucket '$BUCKET_NAME' already exists. Nothing to do."
    exit 0
  fi

  # Try to create the bucket
  if aws --endpoint-url="$ENDPOINT" s3 mb "s3://$BUCKET_NAME" 2>/dev/null; then
    echo "Bucket '$BUCKET_NAME' created successfully."
    exit 0
  fi

  echo "LocalStack not ready yet (attempt $i/30). Retrying in 2s..."
  sleep 2
done

echo "ERROR: Could not create bucket '$BUCKET_NAME' after 30 attempts."
exit 1
