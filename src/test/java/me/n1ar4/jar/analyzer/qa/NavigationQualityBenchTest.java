/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.qa;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.gui.runtime.api.RuntimeFacades;
import me.n1ar4.jar.analyzer.gui.runtime.model.EditorDeclarationResultDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.EditorDeclarationTargetDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.EditorDocumentDto;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional benchmark harness for editor semantic navigation quality.
 * <p>
 * Run with:
 * mvn -Pbench-nav test -Dtest=NavigationQualityBenchTest
 */
public class NavigationQualityBenchTest {
    private static final String ENABLE_PROP = "bench.nav";
    private static final String ITER_PROP = "bench.nav.iter";
    private static final String MIN_HIT_PROP = "bench.nav.minHit";
    private static final String MAX_P95_PROP = "bench.nav.maxP95Ms";

    @Test
    @SuppressWarnings("all")
    public void benchmarkEditorNavigationQuality() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean(ENABLE_PROP),
                "set -D" + ENABLE_PROP + "=true to enable navigation benchmark");

        int iterations = resolveInt(ITER_PROP, 30, 1, 500);
        double minHitRate = resolveDouble(MIN_HIT_PROP, 0.95, 0.10, 1.00);
        long maxP95Ms = resolveLong(MAX_P95_PROP, 150L, 10L, 5_000L);

        CoreEngine engine = buildCallbackFixture();
        try {
            List<Scenario> scenarios = List.of(
                    new Scenario("declaration-dynamicProxy", "me/n1ar4/cb/CallbackEntry", "dynamicProxy", "()V",
                            NavigationAction.DECLARATION,
                            List.of(new ExpectedTarget("me/n1ar4/cb/CallbackEntry", "dynamicProxy", "()V"))),
                    new Scenario("usages-dynamicProxy", "me/n1ar4/cb/CallbackEntry", "dynamicProxy", "()V",
                            NavigationAction.USAGES,
                            List.of(new ExpectedTarget("me/n1ar4/cb/CallbackEntry", "dynamicProxy", "()V"))),
                    new Scenario("impl-task-run", "me/n1ar4/cb/Task", "run", "()V",
                            NavigationAction.IMPLEMENTATION,
                            List.of(
                                    new ExpectedTarget("me/n1ar4/cb/FastTask", "run", "()V"),
                                    new ExpectedTarget("me/n1ar4/cb/SlowTask", "run", "()V"))),
                    new Scenario("hierarchy-task-run", "me/n1ar4/cb/Task", "run", "()V",
                            NavigationAction.HIERARCHY,
                            List.of(
                                    new ExpectedTarget("me/n1ar4/cb/Task", "run", "()V"),
                                    new ExpectedTarget("me/n1ar4/cb/FastTask", "run", "()V"),
                                    new ExpectedTarget("me/n1ar4/cb/SlowTask", "run", "()V")))
            );

            int totalRuns = 0;
            int totalHits = 0;
            int emptyResult = 0;
            long totalCandidates = 0L;
            List<Long> latencies = new ArrayList<>(scenarios.size() * iterations);
            List<String> misses = new ArrayList<>();

            for (int i = 0; i < iterations; i++) {
                for (Scenario scenario : scenarios) {
                    RunResult result = runScenario(scenario);
                    totalRuns++;
                    if (result.hit()) {
                        totalHits++;
                    } else {
                        misses.add(scenario.name + "@" + i + ":" + result.status());
                    }
                    if (result.result() == null || !result.result().hasTargets()) {
                        emptyResult++;
                    }
                    totalCandidates += result.result() == null ? 0 : result.result().targets().size();
                    latencies.add(result.elapsedMs());
                }
            }

            double hitRate = totalRuns == 0 ? 0.0 : ((double) totalHits / (double) totalRuns);
            double avgCandidates = totalRuns == 0 ? 0.0 : ((double) totalCandidates / (double) totalRuns);
            long p50 = percentile(latencies, 0.50);
            long p95 = percentile(latencies, 0.95);
            long p99 = percentile(latencies, 0.99);
            boolean hitPass = hitRate >= minHitRate;
            boolean p95Pass = p95 <= maxP95Ms;

            List<String> reportLines = new ArrayList<>();
            reportLines.add("## Summary");
            reportLines.add("- runs: `" + totalRuns + "`");
            reportLines.add("- hits: `" + totalHits + "`");
            reportLines.add("- hitRate: `" + String.format(Locale.ROOT, "%.4f", hitRate) + "`");
            reportLines.add("- emptyResults: `" + emptyResult + "`");
            reportLines.add("- avgCandidates: `" + String.format(Locale.ROOT, "%.2f", avgCandidates) + "`");
            reportLines.add("- latencyP50Ms: `" + p50 + "`");
            reportLines.add("- latencyP95Ms: `" + p95 + "`");
            reportLines.add("- latencyP99Ms: `" + p99 + "`");
            reportLines.add("");
            reportLines.add("## Thresholds");
            reportLines.add("- minHitRate: `" + String.format(Locale.ROOT, "%.4f", minHitRate) + "`");
            reportLines.add("- maxP95Ms: `" + maxP95Ms + "`");
            reportLines.add("- hitRatePass: `" + hitPass + "`");
            reportLines.add("- p95Pass: `" + p95Pass + "`");
            if (!misses.isEmpty()) {
                reportLines.add("");
                reportLines.add("## Sample Misses");
                int max = Math.min(12, misses.size());
                for (int i = 0; i < max; i++) {
                    reportLines.add("- " + misses.get(i));
                }
            }
            BenchReportWriter.writeMarkdown("navigation-quality.md", "Navigation Quality Benchmark", reportLines);

            System.out.println("[nav-bench] runs=" + totalRuns
                    + " hits=" + totalHits
                    + " hitRate=" + String.format(Locale.ROOT, "%.4f", hitRate)
                    + " empty=" + emptyResult
                    + " avgCandidates=" + String.format(Locale.ROOT, "%.2f", avgCandidates));
            System.out.println("[nav-bench] latency p50=" + p50 + "ms"
                    + " p95=" + p95 + "ms"
                    + " p99=" + p99 + "ms");
            if (!misses.isEmpty()) {
                int max = Math.min(12, misses.size());
                System.out.println("[nav-bench] sample-misses=" + misses.subList(0, max));
            }

            assertTrue(hitRate >= minHitRate,
                    "navigation hit rate below threshold: actual=" + hitRate + ", expected=" + minHitRate);
            assertTrue(p95 <= maxP95Ms,
                    "navigation p95 too high: actual=" + p95 + "ms, expected<=" + maxP95Ms + "ms");
        } finally {
            EngineContext.setEngine(null);
        }
    }

    private static RunResult runScenario(Scenario scenario) {
        RuntimeFacades.editor().openMethod(
                scenario.className(),
                scenario.methodName(),
                scenario.methodDesc(),
                null
        );
        EditorDocumentDto doc = RuntimeFacades.editor().current();
        String content = doc == null ? "" : safe(doc.content());
        int offset = findMethodTokenOffset(content, scenario.methodName());
        long start = System.nanoTime();
        EditorDeclarationResultDto result = switch (scenario.action()) {
            case DECLARATION -> RuntimeFacades.editor().resolveDeclaration(offset);
            case USAGES -> RuntimeFacades.editor().resolveUsages(offset);
            case IMPLEMENTATION -> RuntimeFacades.editor().resolveImplementations(offset);
            case HIERARCHY -> RuntimeFacades.editor().resolveTypeHierarchy(offset);
        };
        long elapsed = (System.nanoTime() - start) / 1_000_000L;

        boolean hit = containsExpectedTargets(result, scenario.expected());
        String status = result == null ? "null-result" : safe(result.statusText());
        return new RunResult(hit, elapsed, result, status);
    }

    private static boolean containsExpectedTargets(EditorDeclarationResultDto result,
                                                   List<ExpectedTarget> expected) {
        if (expected == null || expected.isEmpty()) {
            return result != null && result.hasTargets();
        }
        if (result == null || !result.hasTargets()) {
            return false;
        }
        List<EditorDeclarationTargetDto> targets = result.targets();
        for (ExpectedTarget e : expected) {
            boolean found = false;
            for (EditorDeclarationTargetDto target : targets) {
                if (target == null) {
                    continue;
                }
                String cls = normalizeClass(target.className());
                String m = safe(target.methodName());
                String d = safe(target.methodDesc());
                if (safe(e.className()).equals(cls)
                        && safe(e.methodName()).equals(m)
                        && safe(e.methodDesc()).equals(d)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static int findMethodTokenOffset(String content, String methodName) {
        String text = safe(content);
        String token = safe(methodName);
        if (text.isBlank() || token.isBlank()) {
            return 0;
        }
        int idx = text.indexOf(token + "(");
        if (idx >= 0) {
            return idx + Math.max(0, token.length() / 2);
        }
        idx = text.indexOf(token);
        return idx >= 0 ? idx + Math.max(0, token.length() / 2) : 0;
    }

    private static CoreEngine buildCallbackFixture() throws Exception {
        Path jar = FixtureJars.callbackTestJar();
        WorkspaceContext.updateResolveInnerJars(false);
        CoreRunner.run(jar, null, false, false, true, null, true);
        ConfigFile config = new ConfigFile();
        config.setDbPath(Const.dbFile);
        CoreEngine engine = new CoreEngine(config);
        EngineContext.setEngine(engine);
        return engine;
    }

    private static int resolveInt(String prop, int def, int min, int max) {
        String raw = System.getProperty(prop);
        if (raw == null || raw.trim().isEmpty()) {
            return def;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return Math.max(min, Math.min(max, value));
        } catch (Exception ignored) {
            return def;
        }
    }

    private static long resolveLong(String prop, long def, long min, long max) {
        String raw = System.getProperty(prop);
        if (raw == null || raw.trim().isEmpty()) {
            return def;
        }
        try {
            long value = Long.parseLong(raw.trim());
            return Math.max(min, Math.min(max, value));
        } catch (Exception ignored) {
            return def;
        }
    }

    private static double resolveDouble(String prop, double def, double min, double max) {
        String raw = System.getProperty(prop);
        if (raw == null || raw.trim().isEmpty()) {
            return def;
        }
        try {
            double value = Double.parseDouble(raw.trim());
            if (value < min) {
                return min;
            }
            if (value > max) {
                return max;
            }
            return value;
        } catch (Exception ignored) {
            return def;
        }
    }

    private static long percentile(List<Long> values, double p) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int index = (int) Math.ceil(sorted.size() * p) - 1;
        if (index < 0) {
            index = 0;
        }
        if (index >= sorted.size()) {
            index = sorted.size() - 1;
        }
        return sorted.get(index);
    }

    private static String normalizeClass(String className) {
        String value = safe(className).trim();
        if (value.isBlank()) {
            return "";
        }
        return value.replace('.', '/');
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record Scenario(
            String name,
            String className,
            String methodName,
            String methodDesc,
            NavigationAction action,
            List<ExpectedTarget> expected
    ) {
    }

    private record ExpectedTarget(String className, String methodName, String methodDesc) {
    }

    private record RunResult(boolean hit, long elapsedMs, EditorDeclarationResultDto result, String status) {
    }

    private enum NavigationAction {
        DECLARATION,
        USAGES,
        IMPLEMENTATION,
        HIERARCHY
    }
}
