#!/bin/bash
set -euo pipefail

# ── Configuration ────────────────────────────────────────────
APP_DIR="/home/ec2-user/app"
COMPOSE_FILE="${APP_DIR}/docker-compose.prod.yml"

# Check required env
if [ -z "${ECR_REGISTRY:-}" ]; then
  echo "ERROR: ECR_REGISTRY is not set"
  exit 1
fi

if [ -z "${AWS_REGION:-}" ]; then
  AWS_REGION="ap-northeast-2"
fi

echo "=== Eunhye Hymn Deploy ==="
echo "ECR Registry: ${ECR_REGISTRY}"
echo "AWS Region:   ${AWS_REGION}"

# ── 1. ECR Login ─────────────────────────────────────────────
echo "[1/4] Logging in to ECR..."
aws ecr get-login-password --region "${AWS_REGION}" | \
  docker login --username AWS --password-stdin "${ECR_REGISTRY}"

# ── 2. Pull latest images ───────────────────────────────────
echo "[2/4] Pulling latest images..."
docker compose -f "${COMPOSE_FILE}" pull

# ── 3. Restart containers ───────────────────────────────────
echo "[3/4] Restarting containers..."
docker compose -f "${COMPOSE_FILE}" up -d --remove-orphans

# ── 4. Cleanup old images ───────────────────────────────────
echo "[4/4] Cleaning up unused images..."
docker image prune -f

echo "=== Deploy complete ==="
docker compose -f "${COMPOSE_FILE}" ps
