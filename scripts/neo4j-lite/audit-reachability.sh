#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
OUT_DIR="${ROOT_DIR}/build/neo4lite-audit"
KEEP_FILE="${ROOT_DIR}/build/neo4lite-keep-classes.txt"
CLASSLOAD_LOG="${ROOT_DIR}/target/neo4j-classload-full.log"
LOADED="${OUT_DIR}/loaded-classes.txt"
ALL_CLASSES="${OUT_DIR}/all-classes.txt"
NOT_LOADED="${OUT_DIR}/not-loaded-classes.txt"
REPORT="${OUT_DIR}/reachability-report.md"

mkdir -p "${OUT_DIR}" "${ROOT_DIR}/target"

if [[ ! -f "${KEEP_FILE}" ]]; then
  cat > "${KEEP_FILE}" <<'KEEP'
# One FQCN per line. Lines starting with # are ignored.
# Keep classes here when pruning by runtime reachability would delete required-but-not-covered classes.
KEEP
fi

# Execute full test suite with class-load tracing for org.neo4j reachability audit.
mvn -q test -DargLine="-Xlog:class+load=info:file=${CLASSLOAD_LOG}"

rg -o "org\\.neo4j\\.[A-Za-z0-9_.$]+" "${CLASSLOAD_LOG}" | sort -u > "${LOADED}" || true

{
  find "${ROOT_DIR}/src/main/java/org/neo4j" -type f -name '*.java'
  find "${ROOT_DIR}/src/main/scala/org/neo4j" -type f -name '*.scala'
} | sed -E 's#^.+/src/main/(java|scala)/##; s#/#.#g; s#\.java$##; s#\.scala$##' | sort -u > "${ALL_CLASSES}"

TMP_KEEP="$(mktemp -t neo4lite-keep.XXXXXX)"
trap 'rm -f "${TMP_KEEP}"' EXIT
rg -v '^\s*#|^\s*$' "${KEEP_FILE}" | sed 's/\r$//' | sort -u > "${TMP_KEEP}" || true

# Treat keep-classes as covered to avoid false-positive prune candidates.
cat "${LOADED}" "${TMP_KEEP}" | sort -u > "${OUT_DIR}/loaded-plus-keep.txt"
comm -23 "${ALL_CLASSES}" "${OUT_DIR}/loaded-plus-keep.txt" > "${NOT_LOADED}" || true

loaded_count="$(wc -l < "${LOADED}" | tr -d ' ')"
all_count="$(wc -l < "${ALL_CLASSES}" | tr -d ' ')"
not_loaded_count="$(wc -l < "${NOT_LOADED}" | tr -d ' ')"

{
  echo "# Neo4lite Reachability Audit"
  echo
  echo "- generated_at: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  echo "- loaded_classes: ${loaded_count}"
  echo "- all_inrepo_classes: ${all_count}"
  echo "- prune_candidates: ${not_loaded_count}"
  echo
  echo "## Loaded By Top Package"
  awk -F. 'NF>=3 {print $3}' "${LOADED}" | sort | uniq -c | sort -nr | sed 's/^/- /'
  echo
  echo "## Not Loaded By Top Package"
  awk -F. 'NF>=3 {print $3}' "${NOT_LOADED}" | sort | uniq -c | sort -nr | sed 's/^/- /'
} > "${REPORT}"

echo "[neo4j-lite] audit report: ${REPORT}"
echo "[neo4j-lite] loaded classes: ${LOADED}"
echo "[neo4j-lite] not loaded classes: ${NOT_LOADED}"
