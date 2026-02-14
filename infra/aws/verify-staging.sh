#!/usr/bin/env bash
set -euo pipefail

require_env() {
  local name="$1"
  if [ -z "${!name:-}" ]; then
    echo "ERROR: ${name} is required"
    exit 1
  fi
}

trim_text() {
  printf '%s' "$1" | xargs || true
}

remote_ssh() {
  ssh -i "${STAGING_SSH_KEY}" -o StrictHostKeyChecking=no "ec2-user@${STAGING_HOST}" "$@"
}

check_ping() {
  local url="$1"
  local max_attempts="$2"
  local sleep_seconds="$3"
  local attempt
  local body

  for attempt in $(seq 1 "${max_attempts}"); do
    body="$(curl -fsS "${url}" 2>/dev/null || true)"
    if printf '%s' "${body}" | grep -Eq '"ok"[[:space:]]*:[[:space:]]*true'; then
      echo "Ping check passed on attempt ${attempt}"
      return 0
    fi
    echo "Ping check attempt ${attempt}/${max_attempts} failed; retrying in ${sleep_seconds}s"
    sleep "${sleep_seconds}"
  done

  echo "::error::Ping check failed after ${max_attempts} attempts (${url})"
  return 1
}

check_http_status() {
  local name="$1"
  local url="$2"
  local allowed_codes_pattern="$3"
  local max_attempts="$4"
  local sleep_seconds="$5"
  local attempt
  local code

  for attempt in $(seq 1 "${max_attempts}"); do
    code="$(curl -s -o /dev/null -w '%{http_code}' "${url}" || true)"
    if [[ "${code}" =~ ${allowed_codes_pattern} ]]; then
      echo "${name} check passed with HTTP ${code} on attempt ${attempt}"
      return 0
    fi
    echo "${name} check attempt ${attempt}/${max_attempts} returned HTTP ${code}; retrying in ${sleep_seconds}s"
    sleep "${sleep_seconds}"
  done

  echo "::error::${name} check failed after ${max_attempts} attempts (${url})"
  return 1
}

check_container_state() {
  local container_name="$1"
  local expected_state="$2"
  local max_attempts="$3"
  local sleep_seconds="$4"
  local attempt
  local state

  for attempt in $(seq 1 "${max_attempts}"); do
    state="$(remote_ssh "docker inspect --format '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{end}}' '${container_name}'" 2>/dev/null || true)"
    state="$(trim_text "${state}")"

    if [[ "${expected_state}" == "running-healthy" && "${state}" == "running healthy" ]]; then
      echo "${container_name} state check passed (${state}) on attempt ${attempt}"
      return 0
    fi
    if [[ "${expected_state}" == "running" && "${state}" == running* ]]; then
      echo "${container_name} state check passed (${state}) on attempt ${attempt}"
      return 0
    fi

    echo "${container_name} state attempt ${attempt}/${max_attempts} returned '${state}'; retrying in ${sleep_seconds}s"
    sleep "${sleep_seconds}"
  done

  echo "::error::${container_name} state check failed after ${max_attempts} attempts (expected=${expected_state}, last='${state}')"
  return 1
}

check_container_image_tag() {
  local container_name="$1"
  local expected_tag="$2"
  local max_attempts="$3"
  local sleep_seconds="$4"
  local attempt
  local image_ref

  for attempt in $(seq 1 "${max_attempts}"); do
    image_ref="$(remote_ssh "docker inspect --format '{{.Config.Image}}' '${container_name}'" 2>/dev/null || true)"
    image_ref="$(trim_text "${image_ref}")"

    if [[ "${image_ref}" == *":${expected_tag}" ]]; then
      echo "${container_name} image check passed (${image_ref}) on attempt ${attempt}"
      return 0
    fi

    echo "${container_name} image attempt ${attempt}/${max_attempts} returned '${image_ref}'; retrying in ${sleep_seconds}s"
    sleep "${sleep_seconds}"
  done

  echo "::error::${container_name} image tag check failed after ${max_attempts} attempts (expected=:${expected_tag}, last='${image_ref}')"
  return 1
}

main() {
  require_env STAGING_HOST
  require_env STAGING_SSH_KEY
  require_env EXPECTED_IMAGE_TAG

  local ping_url="${PING_URL:-http://${STAGING_HOST}/api/v1/ping}"
  local admin_root_url="${ADMIN_ROOT_URL:-http://${STAGING_HOST}/}"
  local admin_auth_guard_url="${ADMIN_AUTH_GUARD_URL:-http://${STAGING_HOST}/api/v1/admin/hymns}"

  echo "Waiting for services to start..."
  sleep 30

  check_ping "${ping_url}" 10 6
  check_http_status "admin-root" "${admin_root_url}" '^(200|301|302)$' 5 4
  check_http_status "admin-auth-guard" "${admin_auth_guard_url}" '^(401|403)$' 5 4
  check_container_state "eunhye-api" "running-healthy" 10 6
  check_container_state "eunhye-nginx" "running" 5 4
  check_container_image_tag "eunhye-api" "${EXPECTED_IMAGE_TAG}" 10 6
  check_container_image_tag "eunhye-nginx" "${EXPECTED_IMAGE_TAG}" 5 4
}

main "$@"
