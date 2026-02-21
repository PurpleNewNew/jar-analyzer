#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
INTERPRETED_RUNTIME_FILE="${ROOT_DIR}/src/main/scala/org/neo4j/cypher/internal/InterpretedRuntime.scala"
FACTORY_FILE="${ROOT_DIR}/src/main/scala/org/neo4j/cypher/internal/CommunityRuntimeFactory.scala"
RUNTIME_NAME_FILE="${ROOT_DIR}/src/main/scala/org/neo4j/cypher/internal/RuntimeName.scala"
OPTIONS_FILE="${ROOT_DIR}/src/main/scala/org/neo4j/cypher/internal/options/CypherQueryOptions.scala"
PLANNER_FILE="${ROOT_DIR}/src/main/scala/org/neo4j/cypher/internal/planning/CypherPlanner.scala"
CONFIG_FILE="${ROOT_DIR}/src/main/scala/org/neo4j/cypher/internal/config/CypherConfiguration.scala"
RUNTIME_FILE="${ROOT_DIR}/src/main/scala/org/neo4j/cypher/internal/CypherRuntime.scala"

if [[ -f "${INTERPRETED_RUNTIME_FILE}" ]]; then
  echo "[neo4j-lite] InterpretedRuntime.scala must not exist in single-runtime mode" >&2
  exit 1
fi

if rg -n "InterpretedRuntime|UnknownRuntime" "${FACTORY_FILE}" >/dev/null; then
  echo "[neo4j-lite] CommunityRuntimeFactory still contains interpreted/unknown runtime fallback path" >&2
  rg -n "InterpretedRuntime|UnknownRuntime" "${FACTORY_FILE}" >&2
  exit 1
fi

if rg -n "case class UnknownRuntime\\b|UnknownRuntime\\(" "${RUNTIME_FILE}" >/dev/null; then
  echo "[neo4j-lite] CypherRuntime still contains UnknownRuntime fallback type/path" >&2
  rg -n "case class UnknownRuntime\\b|UnknownRuntime\\(" "${RUNTIME_FILE}" >&2
  exit 1
fi

if rg -n "InterpretedRuntimeName" "${RUNTIME_NAME_FILE}" >/dev/null; then
  echo "[neo4j-lite] RuntimeName still exposes InterpretedRuntimeName" >&2
  rg -n "InterpretedRuntimeName" "${RUNTIME_NAME_FILE}" >&2
  exit 1
fi

if rg -n "PipelinedRuntimeName|ParallelRuntimeName|SchemaRuntimeName" "${RUNTIME_NAME_FILE}" >/dev/null; then
  echo "[neo4j-lite] RuntimeName still exposes non-slotted runtime names" >&2
  rg -n "PipelinedRuntimeName|ParallelRuntimeName|SchemaRuntimeName" "${RUNTIME_NAME_FILE}" >&2
  exit 1
fi

if ! rg -n "def values: Set\\[CypherRuntimeOption\\] = Set\\(slotted\\)" "${OPTIONS_FILE}" >/dev/null; then
  echo "[neo4j-lite] CypherRuntimeOption.values must be slotted-only" >&2
  exit 1
fi

if rg -n "def values: Set\\[CypherRuntimeOption\\].*(interpreted|legacy|parallel|pipelined)" "${OPTIONS_FILE}" >/dev/null; then
  echo "[neo4j-lite] CypherRuntimeOption.values still accepts unsupported runtimes" >&2
  rg -n "def values: Set\\[CypherRuntimeOption\\].*(interpreted|legacy|parallel|pipelined)" "${OPTIONS_FILE}" >&2
  exit 1
fi

if rg -n "case object (legacy|interpreted|parallel|pipelined) extends CypherRuntimeOption" "${OPTIONS_FILE}" >/dev/null; then
  echo "[neo4j-lite] CypherRuntimeOption still defines unsupported runtime case objects" >&2
  rg -n "case object (legacy|interpreted|parallel|pipelined) extends CypherRuntimeOption" "${OPTIONS_FILE}" >&2
  exit 1
fi

if rg -n "CypherRuntimeOption\\.(parallel|pipelined|legacy|interpreted)" "${PLANNER_FILE}" "${CONFIG_FILE}" >/dev/null; then
  echo "[neo4j-lite] planner/config still references unsupported runtime options" >&2
  rg -n "CypherRuntimeOption\\.(parallel|pipelined|legacy|interpreted)" "${PLANNER_FILE}" "${CONFIG_FILE}" >&2
  exit 1
fi

if ! rg -n "SUPPORTED_RUNTIME_OPTIONS\\(runtime\\)" "${OPTIONS_FILE}" >/dev/null; then
  echo "[neo4j-lite] options-layer runtime guard is missing" >&2
  exit 1
fi

echo "[neo4j-lite] verify-single-runtime passed"
