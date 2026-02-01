#!/usr/bin/env bash
set -euo pipefail

jar_analyzer="jar-analyzer-6.0.jar"
base_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
jre_bin="${base_dir}/jre/bin/java"

other_args="-Dfile.encoding=UTF-8"
java_args="-XX:+UseG1GC -Xms2g -XX:MaxGCPauseMillis=200 ${other_args}"
java_cp="lib/${jar_analyzer}:lib/tools.jar"
main_class="me.n1ar4.jar.analyzer.starter.Application"

theme_name="default"
api_server_port="10032"
log_level="info"
program_args="--theme ${theme_name} --port ${api_server_port} --log-level ${log_level}"

echo "[*] RUN ${jar_analyzer} ON JAVA 8"
echo "[*] JVM ARGS: ${java_args}"
"${jre_bin}" ${java_args} -cp "${java_cp}" ${main_class} gui ${program_args}
