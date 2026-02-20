#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "${ROOT_DIR}"

expected_files=(
  "src/test/java/me/n1ar4/core/perf/JarAnalyzerBenchTest.java"
  "src/test/java/me/n1ar4/core/spring/SpringCoreTest.java"
  "src/test/java/me/n1ar4/core/spring/SpringCustomTest.java"
  "src/test/java/me/n1ar4/jar/analyzer/concurrent/BuildTwiceIsolationTest.java"
  "src/test/java/me/n1ar4/jar/analyzer/concurrent/ConcurrentDfsTaintTest.java"
  "src/test/java/me/n1ar4/jar/analyzer/core/PtaAdaptiveSensitivityBudgetTest.java"
  "src/test/java/me/n1ar4/jar/analyzer/core/PtaHeapAndReflectionClosureTest.java"
  "src/test/java/me/n1ar4/jar/analyzer/core/PtaModeIntegrationTest.java"
  "src/test/java/me/n1ar4/jar/analyzer/core/PtaOnTheFlySemanticClosureTest.java"
  "src/test/java/me/n1ar4/jar/analyzer/core/PtaOnTheFlySpringClosureTest.java"
  "src/test/java/me/n1ar4/jar/analyzer/core/PtaQualityRegressionTest.java"
  "src/test/java/me/n1ar4/jar/analyzer/core/PtaSpringStabilityTest.java"
  "src/test/java/me/n1ar4/jar/analyzer/core/SemanticDisambiguationRegressionTest.java"
  "src/test/java/me/n1ar4/jar/analyzer/core/edge/CallbackEdgeInferenceTest.java"
  "src/test/java/me/n1ar4/jar/analyzer/core/edge/SpringFrameworkEdgeInferenceTest.java"
  "src/test/java/me/n1ar4/jar/analyzer/determinism/DeterministicDfsOutputTest.java"
  "src/test/java/me/n1ar4/jar/analyzer/determinism/ExporterDeterminismTest.java"
  "src/test/java/me/n1ar4/jar/analyzer/gui/swing/cypher/CypherScriptServiceTest.java"
  "src/test/java/me/n1ar4/jar/analyzer/headless/HeadlessSmokeTest.java"
  "src/test/java/me/n1ar4/jar/analyzer/qa/GlobalSearchIncrementalBenchTest.java"
  "src/test/java/me/n1ar4/jar/analyzer/qa/NavigationQualityBenchTest.java"
  "src/test/java/me/n1ar4/jar/analyzer/server/handler/JobStressCancelTest.java"
)

actual_files=()
while IFS= read -r file; do
  [[ -z "${file}" ]] && continue
  actual_files+=("${file}")
done < <(rg -l '@Tag\("legacy-sqlite"\)' src/test/java | sort)

expected_sorted="$(printf '%s\n' "${expected_files[@]}" | sort)"
actual_sorted="$(printf '%s\n' "${actual_files[@]}" | sort)"

if [[ "${expected_sorted}" != "${actual_sorted}" ]]; then
  echo "[verify-legacy-sqlite-tests] mismatch in legacy-sqlite tag whitelist" >&2
  diff -u <(printf '%s\n' "${expected_files[@]}" | sort) <(printf '%s\n' "${actual_files[@]}" | sort) || true
  exit 1
fi

echo "[verify-legacy-sqlite-tests] OK (${#expected_files[@]} files)"
