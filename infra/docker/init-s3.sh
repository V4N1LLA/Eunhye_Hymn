#!/bin/bash
set -euo pipefail

BUCKET_NAME="local-bucket"
REGION="ap-northeast-2"

echo "==> Creating S3 bucket: ${BUCKET_NAME} (region: ${REGION})"

awslocal s3api create-bucket \
  --bucket "${BUCKET_NAME}" \
  --region "${REGION}" \
  --create-bucket-configuration LocationConstraint="${REGION}"

echo "==> S3 bucket '${BUCKET_NAME}' created successfully."
