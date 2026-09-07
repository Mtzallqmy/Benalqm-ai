#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
env_file="${MOATAZ_ENV_FILE:-${repo_root}/.env.moataz}"

if [[ ! -f "${env_file}" ]]; then
  echo "Missing ${env_file}. Copy .env.moataz.example to .env.moataz and configure it." >&2
  exit 2
fi

cd "${repo_root}/docker"
docker compose \
  --env-file "${env_file}" \
  -f docker-compose.yaml \
  -f docker-compose.moataz.yaml \
  "$@"
