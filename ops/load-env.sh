#!/usr/bin/env bash
# Spring Boot does not read .env files. Load them into your shell, then run mvn.
#
# From the repository root:
#   source ops/load-env.sh
#   cd services/order-service && mvn spring-boot:run
#
# Or one line:
#   source ops/load-env.sh && cd services/order-service && mvn spring-boot:run

# Resolve ops/ directory when this file is sourced (bash or zsh)
if [[ -n "${BASH_SOURCE[0]:-}" ]]; then
  _LOAD_ENV_HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
elif [[ -n "${ZSH_VERSION:-}" ]]; then
  _LOAD_ENV_HERE="$(cd "$(dirname "${(%):-%x}")" && pwd)"
else
  _LOAD_ENV_HERE="$(cd "$(dirname "$0")" && pwd)"
fi
_LOAD_ENV_ROOT="$(cd "$_LOAD_ENV_HERE/.." && pwd)"
unset _LOAD_ENV_HERE

if [[ -f "$_LOAD_ENV_ROOT/.env" ]]; then
  set -a
  # shellcheck source=/dev/null
  source "$_LOAD_ENV_ROOT/.env"
  set +a
  echo "Loaded environment variables from $_LOAD_ENV_ROOT/.env"
else
  echo "No .env at $_LOAD_ENV_ROOT — set DB_URL, DB_USERNAME, DB_PASSWORD, SPRING_PROFILES_ACTIVE yourself." >&2
fi
unset _LOAD_ENV_ROOT
