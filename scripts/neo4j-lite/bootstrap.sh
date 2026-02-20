#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
XSTREAM_NOISE='Security framework of XStream not explicitly initialized, using predefined black list on your own risk.'
ERR_LOG="$(mktemp -t neo4lite-inrepo-err.XXXXXX)"
trap 'rm -f "${ERR_LOG}"' EXIT

if [[ -x "${ROOT_DIR}/scripts/neo4j-lite/verify-inrepo-source.sh" ]]; then
  "${ROOT_DIR}/scripts/neo4j-lite/verify-inrepo-source.sh"
fi

if [[ -x "${ROOT_DIR}/scripts/neo4j-lite/verify-source-prune.sh" ]]; then
  "${ROOT_DIR}/scripts/neo4j-lite/verify-source-prune.sh"
fi

if ! mvn -q -DskipTests compile 2>"${ERR_LOG}"; then
  grep -Fv "${XSTREAM_NOISE}" "${ERR_LOG}" >&2 || true
  exit 1
fi
grep -Fv "${XSTREAM_NOISE}" "${ERR_LOG}" >&2 || true

if [[ -x "${ROOT_DIR}/scripts/neo4j-lite/verify-lite-jar.sh" && -f "${ROOT_DIR}/target/jar-analyzer-6.0.jar" ]]; then
  "${ROOT_DIR}/scripts/neo4j-lite/verify-lite-jar.sh"
fi

echo "[neo4j-lite] done"
