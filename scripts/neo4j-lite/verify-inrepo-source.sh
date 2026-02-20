#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
POM_FILE="${ROOT_DIR}/pom.xml"
JAVA_SRC="${ROOT_DIR}/src/main/java/org/neo4j"
SCALA_SRC="${ROOT_DIR}/src/main/scala/org/neo4j"

if [[ ! -d "${JAVA_SRC}" ]]; then
  echo "[neo4j-lite] missing in-repo neo4j java sources: ${JAVA_SRC}" >&2
  exit 1
fi

if [[ ! -d "${SCALA_SRC}" ]]; then
  echo "[neo4j-lite] missing in-repo neo4j scala sources: ${SCALA_SRC}" >&2
  exit 1
fi

if rg -n '<groupId>me\.n1ar4\.neo4lite</groupId>|<artifactId>neo4lite-embedded</artifactId>' "${POM_FILE}" >/dev/null; then
  echo "[neo4j-lite] neo4lite artifact dependency still present in pom.xml" >&2
  rg -n '<groupId>me\.n1ar4\.neo4lite</groupId>|<artifactId>neo4lite-embedded</artifactId>' "${POM_FILE}" >&2
  exit 1
fi

TMP_TREE="$(mktemp -t neo4j-inrepo-deptree.XXXXXX)"
trap 'rm -f "${TMP_TREE}"' EXIT
mvn -q -DskipTests dependency:tree > "${TMP_TREE}"

if rg -n 'me\.n1ar4\.neo4lite:|org\.neo4j:|org\.neo4j\.app:' "${TMP_TREE}" >/dev/null; then
  echo "[neo4j-lite] banned artifact dependency detected in dependency:tree" >&2
  rg -n 'me\.n1ar4\.neo4lite:|org\.neo4j:|org\.neo4j\.app:' "${TMP_TREE}" >&2
  exit 1
fi

echo "[neo4j-lite] verify-inrepo-source passed"
