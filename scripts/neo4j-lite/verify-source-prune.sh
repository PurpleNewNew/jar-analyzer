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
  -path "*/org/neo4j/kernel/impl/traversal/*"
)

if find "${JAVA_SRC}" "${SCALA_SRC}" -type f \( "${banned_find_expr[@]}" \) | rg . >/dev/null; then
  echo "[neo4j-lite] banned source namespace remains in src/main/{java,scala}" >&2
  find "${JAVA_SRC}" "${SCALA_SRC}" -type f \( "${banned_find_expr[@]}" \) | head -n 120 >&2
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
