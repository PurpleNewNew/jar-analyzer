#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAIN_CLASS="me.n1ar4.jar.analyzer.starter.Application"
JAVA_BIN="${BASE_DIR}/jre/bin/java"

find_core_jar() {
  local jar
  for jar in "${BASE_DIR}"/lib/jar-analyzer-*.jar; do
    if [[ -f "${jar}" ]]; then
      printf '%s\n' "${jar}"
      return 0
    fi
  done
  return 1
}

if [[ ! -x "${JAVA_BIN}" ]]; then
  echo "[-] bundled runtime not found: ${JAVA_BIN}"
  exit 1
fi

if ! "${JAVA_BIN}" --list-modules 2>/dev/null | grep -Eq '^jcef(@|$)'; then
  echo "[-] bundled runtime missing jcef module (JBR + JCEF required)"
  exit 1
fi

CORE_JAR="$(find_core_jar || true)"
if [[ -z "${CORE_JAR}" ]]; then
  echo "[-] core jar not found: ${BASE_DIR}/lib/jar-analyzer-*.jar"
  exit 1
fi

JVM_OPTS=(-XX:+UseZGC -Xms2g -Xmx6g -Dfile.encoding=UTF-8)
if [[ -n "${JA_JVM_OPTS:-}" ]]; then
  # shellcheck disable=SC2206
  EXTRA_JVM_OPTS=(${JA_JVM_OPTS})
else
  EXTRA_JVM_OPTS=()
fi

echo "[*] JAVA: ${JAVA_BIN}"
echo "[*] CORE JAR: ${CORE_JAR}"
echo "[*] JVM OPTS: ${JVM_OPTS[*]} ${EXTRA_JVM_OPTS[*]}"
"${JAVA_BIN}" "${JVM_OPTS[@]}" "${EXTRA_JVM_OPTS[@]}" -cp "${CORE_JAR}" "${MAIN_CLASS}" "$@"
