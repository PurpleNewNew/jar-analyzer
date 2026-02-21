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

if rg -n '^org/neo4j/(server|bolt|router|fabric|importer|udc|cloud|dbms/routing|dbms/admissioncontrol|logging/event|graphalgo|csv|batchimport|internal/batchimport|consistency|dbms/diagnostics/jmx|kernel/impl/traversal|cypher/internal/profiling|cypher/internal/parser/javacc|kernel/api/impl/schema/trigram|kernel/api/impl/index/backup|cypher/internal/runtime/interpreted/commands/showcommands)/' "${TMP_LIST}" >/dev/null \
  || rg -n '^org/neo4j/cypher/internal/ir/converters/QuantifiedPathPatternConverters(\$.*)?\.class$|^org/neo4j/cypher/internal/AdministrationShowCommandUtils(\$.*)?\.class$|^org/neo4j/cypher/internal/compiler/AdministrationCommandPlanBuilder(\$.*)?\.class$|^org/neo4j/cypher/internal/compiler/UnsupportedSystemCommand(\$.*)?\.class$|^org/neo4j/cypher/internal/logical/plans/AdministrationCommandLogicalPlan(\$.*)?\.class$|^org/neo4j/cypher/internal/parser/v5/ast/factory/DdlShowBuilder(\$.*)?\.class$|^org/neo4j/cypher/internal/parser/common/ast/factory/ShowCommandFilterTypes(\$.*)?\.class$|^org/neo4j/cypher/internal/rewriting/rewriters/(rewriteShowQuery|expandShowWhere)(\$.*)?\.class$|^org/neo4j/cypher/internal/runtime/interpreted/pipes/CommandPipe(\$.*)?\.class$|^org/neo4j/cypher/internal/procs/(ActionMapper|AuthorizationAndPredicateExecutionPlan|LoggingSystemCommandExecutionPlan|NonTransactionalUpdatingSystemCommandExecutionPlan|ParameterTransformer|PredicateExecutionPlan|QualifierMapper|RowDroppingQuerySubscriber|SystemCommandExecutionPlan|SystemCommandRuntimeResult|SystemUpdateCountingQueryContext|UpdatingSystemCommandExecutionPlan|UpdatingSystemCommandRuntimeResult)(\$.*)?\.class$' "${TMP_LIST}" >/dev/null; then
  echo "[neo4j-lite] banned packages found in in-repo artifact jar:" >&2
  rg -n '^org/neo4j/(server|bolt|router|fabric|importer|udc|cloud|dbms/routing|dbms/admissioncontrol|logging/event|graphalgo|csv|batchimport|internal/batchimport|consistency|dbms/diagnostics/jmx|kernel/impl/traversal|cypher/internal/profiling|cypher/internal/parser/javacc|kernel/api/impl/schema/trigram|kernel/api/impl/index/backup|cypher/internal/runtime/interpreted/commands/showcommands)/|^org/neo4j/cypher/internal/ir/converters/QuantifiedPathPatternConverters(\$.*)?\.class$|^org/neo4j/cypher/internal/AdministrationShowCommandUtils(\$.*)?\.class$|^org/neo4j/cypher/internal/compiler/AdministrationCommandPlanBuilder(\$.*)?\.class$|^org/neo4j/cypher/internal/compiler/UnsupportedSystemCommand(\$.*)?\.class$|^org/neo4j/cypher/internal/logical/plans/AdministrationCommandLogicalPlan(\$.*)?\.class$|^org/neo4j/cypher/internal/parser/v5/ast/factory/DdlShowBuilder(\$.*)?\.class$|^org/neo4j/cypher/internal/parser/common/ast/factory/ShowCommandFilterTypes(\$.*)?\.class$|^org/neo4j/cypher/internal/rewriting/rewriters/(rewriteShowQuery|expandShowWhere)(\$.*)?\.class$|^org/neo4j/cypher/internal/runtime/interpreted/pipes/CommandPipe(\$.*)?\.class$|^org/neo4j/cypher/internal/procs/(ActionMapper|AuthorizationAndPredicateExecutionPlan|LoggingSystemCommandExecutionPlan|NonTransactionalUpdatingSystemCommandExecutionPlan|ParameterTransformer|PredicateExecutionPlan|QualifierMapper|RowDroppingQuerySubscriber|SystemCommandExecutionPlan|SystemCommandRuntimeResult|SystemUpdateCountingQueryContext|UpdatingSystemCommandExecutionPlan|UpdatingSystemCommandRuntimeResult)(\$.*)?\.class$' "${TMP_LIST}" | head -n 120 >&2
  exit 1
fi

if ! rg -n '^org/neo4j/' "${TMP_LIST}" >/dev/null; then
  echo "[neo4j-lite] no org/neo4j namespace found in in-repo artifact jar: ${JAR}" >&2
  exit 1
fi

echo "[neo4j-lite] verify-lite-jar passed"
