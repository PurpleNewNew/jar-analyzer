#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
JAVA_SRC="${ROOT_DIR}/src/main/java/org/neo4j"
SCALA_SRC="${ROOT_DIR}/src/main/scala/org/neo4j"
FULLTEXT_SRC_DIR="${JAVA_SRC}/kernel/api/impl/fulltext"
VECTOR_SRC_DIR="${JAVA_SRC}/kernel/api/impl/schema/vector"
KNN_SERVICE_FILE="${ROOT_DIR}/src/main/resources/META-INF/services/org.apache.lucene.codecs.KnnVectorsFormat"
ANALYZER_SERVICE_FILE="${ROOT_DIR}/src/main/resources/META-INF/services/org.neo4j.graphdb.schema.AnalyzerProvider"
FACTORY_FILE="${JAVA_SRC}/graphdb/facade/DatabaseManagementServiceFactory.java"
ABSTRACT_EDITION_FILE="${JAVA_SRC}/graphdb/factory/module/edition/AbstractEditionModule.java"
COMMUNITY_EDITION_FILE="${JAVA_SRC}/graphdb/factory/module/edition/CommunityEditionModule.java"
DBMS_DIAG_FILE="${JAVA_SRC}/kernel/diagnostics/providers/DbmsDiagnosticsManager.java"
HEAP_DUMP_FILE="${JAVA_SRC}/internal/diagnostics/HeapDumpDiagnostics.java"
VALIDATORS_FILE="${JAVA_SRC}/kernel/impl/util/Validators.java"

for d in "${JAVA_SRC}" "${SCALA_SRC}"; do
  if [[ ! -d "${d}" ]]; then
    echo "[neo4j-lite] missing source tree: ${d}" >&2
    exit 1
  fi
done

banned_find_expr=(
  -path "*/org/neo4j/server/*" -o
  -path "*/org/neo4j/bolt/*" -o
  -path "*/org/neo4j/fabric/*" -o
  -path "*/org/neo4j/router/*" -o
  -path "*/org/neo4j/importer/*" -o
  -path "*/org/neo4j/udc/*" -o
  -path "*/org/neo4j/cloud/*" -o
  -path "*/org/neo4j/cli/*" -o
  -path "*/org/neo4j/commandline/*" -o
  -path "*/org/neo4j/dbms/routing/*" -o
  -path "*/org/neo4j/dbms/admissioncontrol/*" -o
  -path "*/org/neo4j/dbms/archive/*" -o
  -path "*/org/neo4j/dbms/diagnostics/profile/*" -o
  -path "*/org/neo4j/graphdb/factory/module/edition/migration/*" -o
  -path "*/org/neo4j/logging/event/*" -o
  -path "*/org/neo4j/graphalgo/*" -o
  -path "*/org/neo4j/csv/*" -o
  -path "*/org/neo4j/batchimport/*" -o
  -path "*/org/neo4j/internal/batchimport/*" -o
  -path "*/org/neo4j/consistency/*" -o
  -path "*/org/neo4j/dbms/diagnostics/jmx/*" -o
  -path "*/org/neo4j/cypher/internal/profiling/*" -o
  -path "*/org/neo4j/cypher/internal/parser/javacc/*" -o
  -path "*/org/neo4j/cypher/internal/ir/converters/QuantifiedPathPatternConverters.*" -o
  -path "*/org/neo4j/cypher/internal/AdministrationShowCommandUtils.*" -o
  -path "*/org/neo4j/cypher/internal/compiler/AdministrationCommandPlanBuilder.*" -o
  -path "*/org/neo4j/cypher/internal/compiler/SchemaCommandPlanBuilder.*" -o
  -path "*/org/neo4j/cypher/internal/logical/plans/AdministrationCommandLogicalPlan.*" -o
  -path "*/org/neo4j/cypher/internal/logical/plans/SystemProcedureCall.*" -o
  -path "*/org/neo4j/cypher/internal/logical/plans/PrivilegeCommandScope.*" -o
  -path "*/org/neo4j/cypher/internal/ast/ShowConstraintTypes.*" -o
  -path "*/org/neo4j/cypher/internal/ast/ShowFunctionTypes.*" -o
  -path "*/org/neo4j/cypher/internal/ast/ShowIndexTypes.*" -o
  -path "*/org/neo4j/cypher/internal/ast/ShowExecutableBy.*" -o
  -path "*/org/neo4j/cypher/internal/parser/v5/ast/factory/DdlBuilder.*" -o
  -path "*/org/neo4j/cypher/internal/parser/v5/ast/factory/DdlCreateBuilder.*" -o
  -path "*/org/neo4j/cypher/internal/parser/v5/ast/factory/DdlPrivilegeBuilder.*" -o
  -path "*/org/neo4j/cypher/internal/parser/v5/ast/factory/DdlShowBuilder.*" -o
  -path "*/org/neo4j/cypher/internal/parser/common/ast/factory/ShowCommandFilterTypes.*" -o
  -path "*/org/neo4j/cypher/internal/rewriting/rewriters/rewriteShowQuery.*" -o
  -path "*/org/neo4j/cypher/internal/rewriting/rewriters/expandShowWhere.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/*" -o
  -path "*/org/neo4j/cypher/internal/SchemaCommandRuntime.*" -o
  -path "*/org/neo4j/cypher/internal/procs/*" -o
  -path "*/org/neo4j/cypher/operations/GraphFunctions.*" -o
  -path "*/org/neo4j/values/virtual/GraphReferenceValue.*" -o
  -path "*/org/neo4j/kernel/api/impl/fulltext/FulltextAdapter.*" -o
  -path "*/org/neo4j/kernel/api/impl/fulltext/DefaultFulltextAdapter.*" -o
  -path "*/org/neo4j/procedure/builtin/FulltextProcedures.*" -o
  -path "*/org/neo4j/procedure/builtin/VectorIndexProcedures.*" -o
  -path "*/org/neo4j/procedure/builtin/JmxQueryProcedure.*" -o
  -path "*/org/neo4j/kernel/impl/index/schema/FulltextIndexProviderFactory.*" -o
  -path "*/org/neo4j/kernel/impl/index/schema/VectorIndexProviderFactory.*" -o
  -path "*/org/neo4j/cypher/internal/optionsmap/CreateFulltextIndexOptionsConverter.*" -o
  -path "*/org/neo4j/cypher/internal/optionsmap/CreateVectorIndexOptionsConverter.*" -o
  -path "*/org/neo4j/kernel/impl/storemigration/SchemaStore44MigrationUtil.*" -o
  -path "*/org/neo4j/storageengine/migration/MigrationProgressMonitor.*" -o
  -path "*/org/neo4j/kernel/api/impl/schema/trigram/*" -o
  -path "*/org/neo4j/kernel/api/impl/index/backup/*" -o
  -path "*/org/neo4j/kernel/impl/traversal/*"
)

if find "${JAVA_SRC}" "${SCALA_SRC}" -type f \( "${banned_find_expr[@]}" \) | rg . >/dev/null; then
  echo "[neo4j-lite] banned source namespace remains in src/main/{java,scala}" >&2
  find "${JAVA_SRC}" "${SCALA_SRC}" -type f \( "${banned_find_expr[@]}" \) | head -n 120 >&2
  exit 1
fi

if [[ -d "${FULLTEXT_SRC_DIR}" ]]; then
  echo "[neo4j-lite] fulltext source package should be fully removed: ${FULLTEXT_SRC_DIR}" >&2
  find "${FULLTEXT_SRC_DIR}" -type f | head -n 120 >&2
  exit 1
fi

if [[ -d "${VECTOR_SRC_DIR}" ]]; then
  if find "${VECTOR_SRC_DIR}" -type f -name '*.java' \
    ! -name 'VectorSimilarity.java' \
    ! -name 'VectorSimilarityFunctions.java' | rg . >/dev/null; then
    echo "[neo4j-lite] unexpected vector source files remain (only VectorSimilarity* are allowed)" >&2
    find "${VECTOR_SRC_DIR}" -type f -name '*.java' \
      ! -name 'VectorSimilarity.java' \
      ! -name 'VectorSimilarityFunctions.java' | head -n 120 >&2
    exit 1
  fi
fi

if [[ -f "${KNN_SERVICE_FILE}" ]]; then
  echo "[neo4j-lite] vector codec service loader file must be removed: ${KNN_SERVICE_FILE}" >&2
  exit 1
fi

if [[ -f "${ANALYZER_SERVICE_FILE}" ]]; then
  echo "[neo4j-lite] fulltext analyzer service loader file must be removed: ${ANALYZER_SERVICE_FILE}" >&2
  exit 1
fi

if rg -n 'case class RunQueryAtProjection\b|case class RunQueryAt\b' "${SCALA_SRC}" >/dev/null; then
  echo "[neo4j-lite] RunQueryAt types reintroduced in source tree" >&2
  rg -n 'case class RunQueryAtProjection\b|case class RunQueryAt\b' "${SCALA_SRC}" >&2
  exit 1
fi

if rg -n 'org\.neo4j\.bolt|BoltServer|Netty4LoggerFactory|TransactionManagerImpl|CommunityNeoWebServer|CommunityQueryRouterBootstrap|org\.neo4j\.server\.security' \
  "${FACTORY_FILE}" "${ABSTRACT_EDITION_FILE}" "${COMMUNITY_EDITION_FILE}" >/dev/null; then
  echo "[neo4j-lite] banned runtime hooks remain in embedded source path" >&2
  rg -n 'org\.neo4j\.bolt|BoltServer|Netty4LoggerFactory|TransactionManagerImpl|CommunityNeoWebServer|CommunityQueryRouterBootstrap|org\.neo4j\.server\.security' \
    "${FACTORY_FILE}" "${ABSTRACT_EDITION_FILE}" "${COMMUNITY_EDITION_FILE}" >&2
  exit 1
fi

if rg -n 'getFulltextProvider|getVectorIndexProvider' \
  "${JAVA_SRC}/kernel/impl/api/index/IndexProviderMap.java" \
  "${JAVA_SRC}/kernel/impl/api/index/IndexingProvidersService.java" >/dev/null; then
  echo "[neo4j-lite] fulltext/vector provider API leaked back into index interfaces" >&2
  rg -n 'getFulltextProvider|getVectorIndexProvider' \
    "${JAVA_SRC}/kernel/impl/api/index/IndexProviderMap.java" \
    "${JAVA_SRC}/kernel/impl/api/index/IndexingProvidersService.java" >&2
  exit 1
fi

for f in "${DBMS_DIAG_FILE}" "${HEAP_DUMP_FILE}" "${VALIDATORS_FILE}"; do
  if [[ ! -f "${f}" ]]; then
    echo "[neo4j-lite] missing required patched file: ${f}" >&2
    exit 1
  fi
done

echo "[neo4j-lite] verify-source-prune passed"
