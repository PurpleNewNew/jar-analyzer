#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
SRC_MAIN_JAVA="${ROOT_DIR}/src/main/java"
SRC_MAIN_SCALA="${ROOT_DIR}/src/main/scala"
SRC_TEST="${ROOT_DIR}/src/test"
INTERPRETED_DIR="${ROOT_DIR}/src/main/scala/org/neo4j/cypher/internal/runtime/interpreted"
SLOTTED_RUNTIME_FILE="${ROOT_DIR}/src/main/scala/org/neo4j/cypher/internal/SlottedRuntime.scala"

if [[ -d "${INTERPRETED_DIR}" ]]; then
  echo "[neo4j-lite] interpreted runtime directory must not exist: ${INTERPRETED_DIR}" >&2
  exit 1
fi

if rg -n "runtime\\.interpreted" "${SRC_MAIN_JAVA}" "${SRC_MAIN_SCALA}" "${SRC_TEST}" >/dev/null; then
  echo "[neo4j-lite] runtime.interpreted references found in source/test trees" >&2
  rg -n "runtime\\.interpreted" "${SRC_MAIN_JAVA}" "${SRC_MAIN_SCALA}" "${SRC_TEST}" | head -n 120 >&2
  exit 1
fi

if rg -n "InterpretedPipeMapper" "${SLOTTED_RUNTIME_FILE}" >/dev/null; then
  echo "[neo4j-lite] SlottedRuntime still references InterpretedPipeMapper" >&2
  rg -n "InterpretedPipeMapper" "${SLOTTED_RUNTIME_FILE}" >&2
  exit 1
fi

TARGET_DIR="${ROOT_DIR}/target"
JAR=""

if [[ -d "${TARGET_DIR}" ]]; then
  while IFS= read -r file; do
    JAR="${file}"
    break
  done < <(find "${TARGET_DIR}" -maxdepth 1 -type f -name '*-jar-with-dependencies.jar' | sort)
fi

if [[ -z "${JAR}" && -d "${TARGET_DIR}" ]]; then
  while IFS= read -r file; do
    JAR="${file}"
    break
  done < <(find "${TARGET_DIR}" -maxdepth 1 -type f -name 'jar-analyzer-*.jar' ! -name '*-sources.jar' | sort)
fi

if [[ -n "${JAR}" && -f "${JAR}" ]]; then
  latest_src_mtime="$(
    find "${SRC_MAIN_JAVA}/org/neo4j" "${SRC_MAIN_SCALA}/org/neo4j" -type f -exec stat -f '%m' {} + \
      | awk 'BEGIN{max=0} {if($1>max) max=$1} END{print max}'
  )"
  jar_mtime="$(stat -f '%m' "${JAR}")"

  if [[ "${jar_mtime}" -lt "${latest_src_mtime}" ]]; then
    echo "[neo4j-lite] skip jar namespace check (stale jar before package): ${JAR}"
  elif jar tf "${JAR}" | rg -n '^org/neo4j/cypher/internal/runtime/interpreted/' >/dev/null; then
    echo "[neo4j-lite] packaged jar still contains interpreted runtime namespace: ${JAR}" >&2
    jar tf "${JAR}" | rg -n '^org/neo4j/cypher/internal/runtime/interpreted/' | head -n 120 >&2
    exit 1
  fi
fi

echo "[neo4j-lite] verify-no-interpreted passed"
