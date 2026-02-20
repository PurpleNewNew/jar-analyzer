#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
NEO4J_DIR="${NEO4J_SRC_DIR:-${ROOT_DIR}/third_party/neo4j}"
NEO4J_SEARCH_ROOT="${NEO4J_DIR}"
ALLOWLIST_FILE="${ROOT_DIR}/build/neo4lite-module-allowlist.txt"
DEST_JAVA="${ROOT_DIR}/src/main/java"
DEST_SCALA="${ROOT_DIR}/src/main/scala"
DEST_RES="${ROOT_DIR}/src/main/resources"

if [[ ! -d "${NEO4J_DIR}/community" ]]; then
  echo "[neo4j-lite] missing source tree: ${NEO4J_DIR}/community" >&2
  echo "[neo4j-lite] tip: set NEO4J_SRC_DIR=/path/to/neo4j-source for re-export" >&2
  exit 1
fi

if [[ ! -f "${ALLOWLIST_FILE}" ]]; then
  echo "[neo4j-lite] missing allowlist: ${ALLOWLIST_FILE}" >&2
  exit 1
fi

mkdir -p "${DEST_JAVA}" "${DEST_SCALA}" "${DEST_RES}"

extract_project_artifact_id() {
  local pom="$1"
  xmllint --xpath "string(/*[local-name()='project']/*[local-name()='artifactId'])" "$pom" 2>/dev/null
}

resolve_module_dir() {
  local artifact="$1"
  local best=""
  while IFS= read -r pom; do
    artifact_id="$(extract_project_artifact_id "$pom")"
    [[ -n "${artifact_id}" ]] || continue
    if [[ "${artifact_id}" != "${artifact}" ]]; then
      continue
    fi
    module_dir="$(dirname "$pom")"
    if [[ -z "${best}" || ${#module_dir} -lt ${#best} ]]; then
      best="${module_dir}"
    fi
  done < <(find "${NEO4J_SEARCH_ROOT}" -type f -name pom.xml | sort)

  if [[ -z "${best}" ]]; then
    return 1
  fi
  echo "${best}"
}

copy_with_conflict_check() {
  local src="$1"
  local dst="$2"
  if [[ -f "$dst" ]]; then
    if ! cmp -s "$src" "$dst"; then
      echo "[neo4j-lite] conflict: $dst" >&2
      echo "  from: $src" >&2
      return 1
    fi
    return 0
  fi
  mkdir -p "$(dirname "$dst")"
  cp "$src" "$dst"
  return 0
}

is_banned_rel_path() {
  local rel="$1"
  case "$rel" in
    org/neo4j/server/*|org/neo4j/bolt/*|org/neo4j/fabric/*|org/neo4j/router/*|org/neo4j/importer/*|org/neo4j/udc/*|org/neo4j/cloud/*|org/neo4j/cli/*|org/neo4j/commandline/*|org/neo4j/dbms/archive/*|org/neo4j/consistency/*|org/neo4j/dbms/diagnostics/profile/*|org/neo4j/internal/batchimport/input/parquet/*|org/neo4j/graphdb/facade/SystemDatabaseUpgrader.java|org/neo4j/graphdb/factory/module/edition/migration/*|org/neo4j/annotations/api/PublicApiDoclet.java)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

copy_tree() {
  local src_root="$1"
  local dst_root="$2"
  local kind="$3"
  [[ -d "$src_root" ]] || return 0

  while IFS= read -r -d '' file; do
    rel="${file#"${src_root}/"}"
    if is_banned_rel_path "$rel"; then
      continue
    fi
    dst="${dst_root}/${rel}"
    copy_with_conflict_check "$file" "$dst"
  done < <(find "$src_root" -type f -print0 | sort -z)

  echo "[neo4j-lite] copied ${kind}: ${src_root} -> ${dst_root}"
}

copy_generated_org_neo4j() {
  local gen_root="$1"
  [[ -d "$gen_root" ]] || return 0

  while IFS= read -r -d '' file; do
    rel="${file#*org/neo4j/}"
    if is_banned_rel_path "org/neo4j/${rel}"; then
      continue
    fi
    dst="${DEST_JAVA}/org/neo4j/${rel}"
    copy_with_conflict_check "$file" "$dst"
  done < <(find "$gen_root" -type f -name '*.java' -path '*/org/neo4j/*' -print0 | sort -z)
}

MODULES=()
while IFS= read -r line; do
  if [[ -z "${line}" || "${line}" == \#* ]]; then
    continue
  fi
  MODULES+=("${line}")
done < "$ALLOWLIST_FILE"

if [[ ${#MODULES[@]} -eq 0 ]]; then
  echo "[neo4j-lite] allowlist is empty: ${ALLOWLIST_FILE}" >&2
  exit 1
fi

echo "[neo4j-lite] module count: ${#MODULES[@]}"
for artifact in "${MODULES[@]}"; do
  if ! module_dir="$(resolve_module_dir "$artifact")"; then
    echo "[neo4j-lite] module not found for artifactId: ${artifact}" >&2
    exit 1
  fi

  copy_tree "${module_dir}/src/main/java" "${DEST_JAVA}" "java"
  copy_tree "${module_dir}/src/main/scala" "${DEST_SCALA}" "scala"
  copy_tree "${module_dir}/src/main/resources" "${DEST_RES}" "resources"

  copy_generated_org_neo4j "${module_dir}/target/generated-sources"
done

echo "[neo4j-lite] export complete"
