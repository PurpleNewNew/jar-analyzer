#!/usr/bin/env bash
set -euo pipefail

jar_analyzer="jar-analyzer-6.0.jar"

other_args="-Dfile.encoding=UTF-8"
java_args="-XX:+UseG1GC -Xms2g -XX:MaxGCPauseMillis=200 ${other_args}"
java_cp="lib/${jar_analyzer}"
main_class="me.n1ar4.jar.analyzer.starter.Application"

theme_name="default"
api_server_port="10032"
log_level="info"
program_args="--theme ${theme_name} --port ${api_server_port} --log-level ${log_level}"

if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "[-] JAVA_HOME NOT SET"
  exit 1
fi

echo "[*] JAVA_HOME: ${JAVA_HOME}"
echo "[*] JVM ARGS: ${java_args}"
"${JAVA_HOME}/bin/java" ${java_args} -cp "${java_cp}" ${main_class} gui ${program_args}
