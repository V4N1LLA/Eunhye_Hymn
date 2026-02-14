#!/bin/bash
set -euo pipefail

# ── Configuration ────────────────────────────────────────────
APP_DIR="/home/ec2-user/app"
COMPOSE_FILE="${APP_DIR}/docker-compose.prod.yml"
COMPOSE_AWSLOGS_FILE="${APP_DIR}/docker-compose.prod.awslogs.yml"
COMPOSE_ARGS_BASE=(-f "${COMPOSE_FILE}")

# Check required env
if [ -z "${ECR_REGISTRY:-}" ]; then
  echo "ERROR: ECR_REGISTRY is not set"
  exit 1
fi

if [ -z "${AWS_REGION:-}" ]; then
  AWS_REGION="ap-northeast-2"
fi

ENABLE_AWSLOGS="${ENABLE_AWSLOGS:-false}"

to_lower() {
  echo "$1" | tr '[:upper:]' '[:lower:]'
}

is_true() {
  case "$(to_lower "$1")" in
    true|1|yes|y|on) return 0 ;;
    *) return 1 ;;
  esac
}

has_awslogs_driver() {
  docker info --format '{{range .Plugins.Log}}{{println .}}{{end}}' 2>/dev/null | grep -qx 'awslogs'
}

USE_AWSLOGS=false
COMPOSE_ARGS=("${COMPOSE_ARGS_BASE[@]}")
if is_true "${ENABLE_AWSLOGS}"; then
  if [ -f "${COMPOSE_AWSLOGS_FILE}" ]; then
    if has_awslogs_driver; then
      USE_AWSLOGS=true
      COMPOSE_ARGS+=(-f "${COMPOSE_AWSLOGS_FILE}")
    else
      echo "WARNING: awslogs driver is not available on this host; falling back to default json-file logging."
      ENABLE_AWSLOGS=false
    fi
  else
    echo "WARNING: ${COMPOSE_AWSLOGS_FILE} not found; falling back to default json-file logging."
    ENABLE_AWSLOGS=false
  fi
fi

export ECR_REGISTRY AWS_REGION ENABLE_AWSLOGS

echo "=== Eunhye Hymn Deploy ==="
echo "ECR Registry: ${ECR_REGISTRY}"
echo "AWS Region:   ${AWS_REGION}"
echo "AWS Logs:     ${ENABLE_AWSLOGS} (effective=${USE_AWSLOGS})"

# ── 1. ECR Login ─────────────────────────────────────────────
echo "[1/4] Logging in to ECR..."
aws ecr get-login-password --region "${AWS_REGION}" | \
  docker login --username AWS --password-stdin "${ECR_REGISTRY}"

# ── 2. Pull latest images ───────────────────────────────────
echo "[2/4] Pulling latest images..."
docker compose "${COMPOSE_ARGS[@]}" pull

# ── 3. Restart containers ───────────────────────────────────
echo "[3/4] Restarting containers..."
set +e
docker compose "${COMPOSE_ARGS[@]}" up -d --remove-orphans
UP_EXIT=$?
set -e

if [ "${UP_EXIT}" -ne 0 ]; then
  if [ "${USE_AWSLOGS}" = "true" ]; then
    echo "WARNING: compose up failed with awslogs enabled. Retrying with default logging."
    ENABLE_AWSLOGS=false
    export ENABLE_AWSLOGS
    COMPOSE_ARGS=("${COMPOSE_ARGS_BASE[@]}")
    docker compose "${COMPOSE_ARGS[@]}" up -d --remove-orphans
  else
    echo "ERROR: compose up failed."
    exit "${UP_EXIT}"
  fi
fi

# ── 4. Cleanup old images ───────────────────────────────────
echo "[4/4] Cleaning up unused images..."
docker image prune -f

echo "=== Deploy complete ==="
docker compose "${COMPOSE_ARGS[@]}" ps
