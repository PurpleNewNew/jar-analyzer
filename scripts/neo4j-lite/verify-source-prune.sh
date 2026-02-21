#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
JAVA_SRC="${ROOT_DIR}/src/main/java/org/neo4j"
SCALA_SRC="${ROOT_DIR}/src/main/scala/org/neo4j"
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
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/commands/showcommands/*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/pipes/CommandPipe.*" -o
  -path "*/org/neo4j/cypher/internal/SchemaCommandRuntime.*" -o
  -path "*/org/neo4j/cypher/internal/procs/*" -o
  -path "*/org/neo4j/cypher/internal/procs/ActionMapper.*" -o
  -path "*/org/neo4j/cypher/internal/procs/AuthorizationAndPredicateExecutionPlan.*" -o
  -path "*/org/neo4j/cypher/internal/procs/LoggingSystemCommandExecutionPlan.*" -o
  -path "*/org/neo4j/cypher/internal/procs/NonTransactionalUpdatingSystemCommandExecutionPlan.*" -o
  -path "*/org/neo4j/cypher/internal/procs/ParameterTransformer.*" -o
  -path "*/org/neo4j/cypher/internal/procs/PredicateExecutionPlan.*" -o
  -path "*/org/neo4j/cypher/internal/procs/QualifierMapper.*" -o
  -path "*/org/neo4j/cypher/internal/procs/RowDroppingQuerySubscriber.*" -o
  -path "*/org/neo4j/cypher/internal/procs/SystemCommandExecutionPlan.*" -o
  -path "*/org/neo4j/cypher/internal/procs/SystemCommandRuntimeResult.*" -o
  -path "*/org/neo4j/cypher/internal/procs/SystemUpdateCountingQueryContext.*" -o
  -path "*/org/neo4j/cypher/internal/procs/UpdatingSystemCommandExecutionPlan.*" -o
  -path "*/org/neo4j/cypher/internal/procs/UpdatingSystemCommandRuntimeResult.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/commands/IndexOperation.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/commands/Query.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/commands/QueryString.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/commands/SortItem.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/commands/expressions/ContainerIndexExists.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/commands/expressions/IndexedInclusiveLongRange.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/commands/expressions/ShortestPathSPI.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/commands/predicates/groupInequalityPredicatesForLegacy.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/pipes/ConcurrentTransactionsLegacyPipe.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/pipes/NodeByIdSeekPipe.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/pipes/DirectedRelationshipByIdSeekPipe.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/pipes/UndirectedRelationshipByIdSeekPipe.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/pipes/IdSeekIterator.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/pipes/NodeIndexStringScanPipe.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/pipes/RelationshipIndexStringScanPipe.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/pipes/PartialTopPipe.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/pipes/TransactionCommittedCounterIterator.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/pipes/RunQueryAtPipe.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/pipes/SeekRhs.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/commands/expressions/GraphReference.*" -o
  -path "*/org/neo4j/cypher/operations/GraphFunctions.*" -o
  -path "*/org/neo4j/values/virtual/GraphReferenceValue.*" -o
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

for f in "${DBMS_DIAG_FILE}" "${HEAP_DUMP_FILE}" "${VALIDATORS_FILE}"; do
  if [[ ! -f "${f}" ]]; then
    echo "[neo4j-lite] missing required patched file: ${f}" >&2
    exit 1
  fi
done

echo "[neo4j-lite] verify-source-prune passed"
