#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

bash "${ROOT_DIR}/scripts/verify-legacy-sqlite-tests.sh"
bash "${ROOT_DIR}/scripts/verify-neo4j-prune-temp-tests.sh"
