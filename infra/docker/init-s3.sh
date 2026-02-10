#!/bin/bash
set -euo pipefail

BUCKET_NAME="local-bucket"
REGION="ap-northeast-2"

echo "==> Ensuring S3 bucket: ${BUCKET_NAME} (region: ${REGION})"

if awslocal s3api head-bucket --bucket "${BUCKET_NAME}" 2>/dev/null; then
  echo "==> S3 bucket '${BUCKET_NAME}' already exists, skipping."
else
  awslocal s3api create-bucket \
    --bucket "${BUCKET_NAME}" \
    --region "${REGION}" \
    --create-bucket-configuration LocationConstraint="${REGION}"
  echo "==> S3 bucket '${BUCKET_NAME}' created successfully."
fi
