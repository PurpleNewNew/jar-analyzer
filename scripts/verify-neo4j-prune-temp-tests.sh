#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LIST_FILE="${ROOT_DIR}/build/neo4lite-temp-skip-tests.txt"

if [[ ! -f "${LIST_FILE}" ]]; then
  echo "[verify-neo4j-prune-temp-tests] missing list file: ${LIST_FILE}" >&2
  exit 1
fi

ACTUAL_LIST="$(mktemp -t neo4j-prune-actual.XXXXXX)"
EXPECTED_LIST="$(mktemp -t neo4j-prune-expected.XXXXXX)"
trap 'rm -f "${ACTUAL_LIST}" "${EXPECTED_LIST}"' EXIT

rg -l '@Tag\("neo4j-prune-temp"\)' "${ROOT_DIR}/src/test/java" | sed "s#^${ROOT_DIR}/##" | sort > "${ACTUAL_LIST}" || true

today="$(date +%F)"

while IFS= read -r raw_line; do
  line="${raw_line#${raw_line%%[![:space:]]*}}"
  line="${line%${line##*[![:space:]]}}"
  [[ -z "${line}" || "${line}" = \#* ]] && continue

  IFS='|' read -r file reason expiry rest <<< "${line}"
  if [[ -n "${rest:-}" || -z "${file}" || -z "${reason}" || -z "${expiry}" ]]; then
    echo "[verify-neo4j-prune-temp-tests] invalid entry (expected file|reason|YYYY-MM-DD): ${line}" >&2
    exit 1
  fi

  if [[ ! "${expiry}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
    echo "[verify-neo4j-prune-temp-tests] invalid expiry date: ${line}" >&2
    exit 1
  fi

  if [[ "${expiry}" < "${today}" ]]; then
    echo "[verify-neo4j-prune-temp-tests] expired entry (${expiry}): ${file}" >&2
    exit 1
  fi

  echo "${file}" >> "${EXPECTED_LIST}"
done < "${LIST_FILE}"

sort -u -o "${EXPECTED_LIST}" "${EXPECTED_LIST}"

if ! diff -u "${EXPECTED_LIST}" "${ACTUAL_LIST}" >/dev/null; then
  echo "[verify-neo4j-prune-temp-tests] mismatch in neo4j-prune-temp whitelist" >&2
  diff -u "${EXPECTED_LIST}" "${ACTUAL_LIST}" >&2 || true
  exit 1
fi

count="$(wc -l < "${ACTUAL_LIST}" | tr -d ' ')"
echo "[verify-neo4j-prune-temp-tests] OK (${count} files)"
