#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
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

if [[ -z "${JAR}" || ! -f "${JAR}" ]]; then
  echo "[neo4j-lite] jar not found under ${TARGET_DIR}" >&2
  exit 1
fi

TMP_LIST="$(mktemp)"
trap 'rm -f "${TMP_LIST}"' EXIT

jar tf "${JAR}" > "${TMP_LIST}"

BANNED_PACKAGE_REGEX='^org/neo4j/(server|bolt|router|fabric|importer|udc|cloud|dbms/routing|dbms/admissioncontrol|logging/event|graphalgo|csv|batchimport|internal/batchimport|consistency|dbms/diagnostics/jmx|kernel/impl/traversal|kernel/api/impl/fulltext|cypher/internal/profiling|cypher/internal/parser/javacc|kernel/api/impl/schema/trigram|kernel/api/impl/index/backup|cypher/internal/runtime/interpreted|cypher/internal/procs)/'
BANNED_CLASS_REGEX='^org/neo4j/cypher/internal/SchemaCommandRuntime(\$.*)?\.class$|^org/neo4j/cypher/internal/ir/converters/QuantifiedPathPatternConverters(\$.*)?\.class$|^org/neo4j/cypher/internal/AdministrationShowCommandUtils(\$.*)?\.class$|^org/neo4j/cypher/internal/compiler/AdministrationCommandPlanBuilder(\$.*)?\.class$|^org/neo4j/cypher/internal/compiler/SchemaCommandPlanBuilder(\$.*)?\.class$|^org/neo4j/cypher/internal/compiler/UnsupportedSystemCommand(\$.*)?\.class$|^org/neo4j/cypher/internal/logical/plans/AdministrationCommandLogicalPlan(\$.*)?\.class$|^org/neo4j/cypher/internal/logical/plans/SystemProcedureCall(\$.*)?\.class$|^org/neo4j/cypher/internal/logical/plans/PrivilegeCommandScope(\$.*)?\.class$|^org/neo4j/cypher/internal/ast/(ShowConstraintTypes|ShowFunctionTypes|ShowIndexTypes|ShowExecutableBy)(\$.*)?\.class$|^org/neo4j/cypher/internal/parser/v5/ast/factory/(DdlBuilder|DdlCreateBuilder|DdlPrivilegeBuilder|DdlShowBuilder)(\$.*)?\.class$|^org/neo4j/cypher/internal/parser/common/ast/factory/ShowCommandFilterTypes(\$.*)?\.class$|^org/neo4j/cypher/internal/rewriting/rewriters/(rewriteShowQuery|expandShowWhere)(\$.*)?\.class$|^org/neo4j/cypher/internal/optionsmap/(CreateFulltextIndexOptionsConverter|CreateVectorIndexOptionsConverter)(\$.*)?\.class$|^org/neo4j/cypher/internal/ir/RunQueryAtProjection(\$.*)?\.class$|^org/neo4j/cypher/internal/logical/plans/RunQueryAt(\$.*)?\.class$|^org/neo4j/kernel/api/impl/fulltext/(FulltextAdapter|DefaultFulltextAdapter)(\$.*)?\.class$|^org/neo4j/procedure/builtin/(FulltextProcedures|VectorIndexProcedures|JmxQueryProcedure)(\$.*)?\.class$|^org/neo4j/kernel/impl/index/schema/(FulltextIndexProviderFactory|VectorIndexProviderFactory)(\$.*)?\.class$|^org/neo4j/kernel/impl/storemigration/SchemaStore44MigrationUtil(\$.*)?\.class$|^org/neo4j/storageengine/migration/MigrationProgressMonitor(\$.*)?\.class$|^org/neo4j/cypher/operations/GraphFunctions(\$.*)?\.class$|^org/neo4j/values/virtual/GraphReferenceValue(\$.*)?\.class$'

if rg -n "${BANNED_PACKAGE_REGEX}" "${TMP_LIST}" >/dev/null \
  || rg -n "${BANNED_CLASS_REGEX}" "${TMP_LIST}" >/dev/null; then
  echo "[neo4j-lite] banned packages found in in-repo artifact jar:" >&2
  rg -n "${BANNED_PACKAGE_REGEX}|${BANNED_CLASS_REGEX}" "${TMP_LIST}" | head -n 120 >&2
  exit 1
fi

if rg -n '^org/neo4j/kernel/api/impl/schema/vector/.*\.class$' "${TMP_LIST}" | \
  rg -v 'VectorSimilarity(\$.*)?\.class$|VectorSimilarityFunctions(\$.*)?\.class$' >/dev/null; then
  echo "[neo4j-lite] unexpected vector classes remain (only VectorSimilarity* are allowed)" >&2
  rg -n '^org/neo4j/kernel/api/impl/schema/vector/.*\.class$' "${TMP_LIST}" | \
    rg -v 'VectorSimilarity(\$.*)?\.class$|VectorSimilarityFunctions(\$.*)?\.class$' | head -n 120 >&2
  exit 1
fi

if ! rg -n '^org/neo4j/' "${TMP_LIST}" >/dev/null; then
  echo "[neo4j-lite] no org/neo4j namespace found in in-repo artifact jar: ${JAR}" >&2
  exit 1
fi

echo "[neo4j-lite] verify-lite-jar passed"
