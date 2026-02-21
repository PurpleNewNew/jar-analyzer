#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
COMMON_DIR="${ROOT_DIR}/src/main/java/org/neo4j/cypher/internal/parser/common"
OUT_DIR="${ROOT_DIR}/build/neo4lite-audit"
REPORT_FILE="${OUT_DIR}/parser-common-reference-report.tsv"
UNREF_FILE="${OUT_DIR}/parser-common-unreferenced.txt"
DELETED_FILE="${OUT_DIR}/parser-common-deleted.txt"

DELETE_MODE="false"
if [[ "${1:-}" == "--delete" ]]; then
  DELETE_MODE="true"
fi

if [[ ! -d "${COMMON_DIR}" ]]; then
  echo "[neo4j-lite] parser-common source directory missing: ${COMMON_DIR}" >&2
  exit 1
fi

mkdir -p "${OUT_DIR}"
printf "type\treference_count\tsource\n" > "${REPORT_FILE}"
: > "${UNREF_FILE}"
: > "${DELETED_FILE}"

SEARCH_PATHS=()
for d in "${ROOT_DIR}/src/main/java" "${ROOT_DIR}/src/main/scala" "${ROOT_DIR}/src/test/java"; do
  if [[ -d "${d}" ]]; then
    SEARCH_PATHS+=("${d}")
  fi
done

while IFS= read -r file; do
  rel="${file#${ROOT_DIR}/}"
  type_name="$(basename "${file}" .java)"
  all_refs="$(rg -n "\\b${type_name}\\b" "${SEARCH_PATHS[@]}" 2>/dev/null || true)"
  refs_without_self="$(printf '%s\n' "${all_refs}" | grep -vF "${rel}:" || true)"
  ref_count="$(printf '%s\n' "${refs_without_self}" | sed '/^$/d' | wc -l | tr -d ' ')"

  printf "%s\t%s\t%s\n" "${type_name}" "${ref_count}" "${rel}" >> "${REPORT_FILE}"
  if [[ "${ref_count}" == "0" ]]; then
    printf "%s\n" "${rel}" >> "${UNREF_FILE}"
  fi
done < <(find "${COMMON_DIR}" -type f -name '*.java' | sort)

if [[ "${DELETE_MODE}" == "true" ]]; then
  while IFS= read -r rel; do
    [[ -z "${rel}" ]] && continue
    rm -f "${ROOT_DIR}/${rel}"
    printf "%s\n" "${rel}" >> "${DELETED_FILE}"
  done < "${UNREF_FILE}"
fi

echo "[neo4j-lite] parser-common audit report: ${REPORT_FILE}"
echo "[neo4j-lite] parser-common unreferenced: $(wc -l < "${UNREF_FILE}" | tr -d ' ')"
if [[ "${DELETE_MODE}" == "true" ]]; then
  echo "[neo4j-lite] parser-common deleted: $(wc -l < "${DELETED_FILE}" | tr -d ' ')"
fi
