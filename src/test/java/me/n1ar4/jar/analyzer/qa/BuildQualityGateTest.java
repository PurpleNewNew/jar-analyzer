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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional quality gate that combines accuracy and conservative
 * performance thresholds for the frozen build baseline.
 */
public class BuildQualityGateTest {
    private static final String ENABLE_PROP = "bench.build_quality_gate";

    private static final String GADGET_ITER_PROP = "bench.build_quality_gate.gadget.iter";
    private static final String GADGET_MIN_HIT_PROP = "bench.build_quality_gate.gadget.minHit";
    private static final String GADGET_MAX_P95_PROP = "bench.build_quality_gate.gadget.maxP95Ms";

    private static final String NAV_ITER_PROP = "bench.build_quality_gate.nav.iter";
    private static final String NAV_MIN_HIT_PROP = "bench.build_quality_gate.nav.minHit";
    private static final String NAV_MAX_P95_PROP = "bench.build_quality_gate.nav.maxP95Ms";

    private static final String WEB_ITER_PROP = "bench.build_quality_gate.web.iter";
    private static final String WEB_SCENARIOS_PROP = "bench.build_quality_gate.web.scenarios";
    private static final String WEB_MAX_BUILD_P95_PROP = "bench.build_quality_gate.web.maxBuildP95Ms";
    private static final String WEB_MAX_PEAK_HEAP_MIB_PROP = "bench.build_quality_gate.web.maxPeakHeapMiB";

    private static final String DEFAULT_WEB_SCENARIOS = "framework-stack,ssm-war,ssm-project-mode,springboot-fatjar";

    @Test
    @SuppressWarnings("all")
    public void benchmarkBuildQualityGate() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean(ENABLE_PROP),
                "set -D" + ENABLE_PROP + "=true to enable build quality gate");

        int gadgetIterations = resolveInt(GADGET_ITER_PROP, 3, 1, 200);
        double gadgetMinHitRate = resolveDouble(GADGET_MIN_HIT_PROP, 1.0, 0.10, 1.0);
        long gadgetMaxP95Ms = resolveLong(GADGET_MAX_P95_PROP, 200L, 1L, 5_000L);

        int navIterations = resolveInt(NAV_ITER_PROP, 10, 1, 200);
        double navMinHitRate = resolveDouble(NAV_MIN_HIT_PROP, 0.95, 0.10, 1.0);
        long navMaxP95Ms = resolveLong(NAV_MAX_P95_PROP, 300L, 1L, 5_000L);

        int webIterations = resolveInt(WEB_ITER_PROP, 1, 1, 10);
        Set<String> webScenarios = resolveWebScenarios(System.getProperty(WEB_SCENARIOS_PROP));
        long webMaxBuildP95Ms = resolveLong(WEB_MAX_BUILD_P95_PROP, 18_000L, 1L, 120_000L);
        long webMaxPeakHeapMiB = resolveLong(WEB_MAX_PEAK_HEAP_MIB_PROP, 1_600L, 64L, 16_384L);

        GadgetRouteCoverageBenchTest.BenchmarkResult gadget =
                GadgetRouteCoverageBenchTest.runBenchmark(gadgetIterations, gadgetMinHitRate, gadgetMaxP95Ms, false);
        NavigationQualityBenchTest.BenchmarkResult navigation =
                NavigationQualityBenchTest.runBenchmark(navIterations, navMinHitRate, navMaxP95Ms, false);
        BuildBaselineBenchTest.BaselineResult web =
                BuildBaselineBenchTest.runBaseline(webIterations, webScenarios, false);

        long webPeakHeapMiB = toMiB(web.maxPeakHeapUsedBytes());
        boolean webBuildP95Pass = web.buildWallP95Ms() <= webMaxBuildP95Ms;
        boolean webPeakHeapPass = webPeakHeapMiB <= webMaxPeakHeapMiB;

        writeGateReport(
                gadget,
                navigation,
                web,
                webScenarios,
                webBuildP95Pass,
                webMaxBuildP95Ms,
                webPeakHeapPass,
                webPeakHeapMiB,
                webMaxPeakHeapMiB
        );

        assertTrue(gadget.hitRatePass(),
                "gadget hit rate below threshold: actual=" + format(gadget.hitRate())
                        + ", expected>=" + format(gadget.minHitRate()));
        assertTrue(gadget.p95Pass(),
                "gadget p95 too high: actual=" + gadget.p95Ms()
                        + "ms, expected<=" + gadget.maxP95Ms() + "ms");
        assertTrue(navigation.hitRatePass(),
                "navigation hit rate below threshold: actual=" + format(navigation.hitRate())
                        + ", expected>=" + format(navigation.minHitRate()));
        assertTrue(navigation.p95Pass(),
                "navigation p95 too high: actual=" + navigation.p95Ms()
                        + "ms, expected<=" + navigation.maxP95Ms() + "ms");
        assertTrue(web.passed(), "real web baseline failures: " + web.failures());
        assertTrue(webBuildP95Pass,
                "real web build p95 too high: actual=" + web.buildWallP95Ms()
                        + "ms, expected<=" + webMaxBuildP95Ms + "ms");
        assertTrue(webPeakHeapPass,
                "real web peak heap too high: actual=" + webPeakHeapMiB
                        + "MiB, expected<=" + webMaxPeakHeapMiB + "MiB");
    }

    private static void writeGateReport(GadgetRouteCoverageBenchTest.BenchmarkResult gadget,
                                        NavigationQualityBenchTest.BenchmarkResult navigation,
                                        BuildBaselineBenchTest.BaselineResult web,
                                        Set<String> webScenarios,
                                        boolean webBuildP95Pass,
                                        long webMaxBuildP95Ms,
                                        boolean webPeakHeapPass,
                                        long webPeakHeapMiB,
                                        long webMaxPeakHeapMiB) {
        List<String> lines = new ArrayList<>();
        lines.add("## Summary");
        lines.add("- gate: `" + ENABLE_PROP + "`");
        lines.add("- passed: `" + (gadget.passed() && navigation.passed() && web.passed() && webBuildP95Pass && webPeakHeapPass) + "`");
        lines.add("");
        lines.add("## Gadget");
        lines.add("- scenarios: `" + gadget.scenarioCount() + "`");
        lines.add("- iterations: `" + gadget.iterations() + "`");
        lines.add("- hitRate: `" + format(gadget.hitRate()) + "`");
        lines.add("- latencyP95Ms: `" + gadget.p95Ms() + "`");
        lines.add("- minHitRate: `" + format(gadget.minHitRate()) + "`");
        lines.add("- maxP95Ms: `" + gadget.maxP95Ms() + "`");
        lines.add("- hitRatePass: `" + gadget.hitRatePass() + "`");
        lines.add("- p95Pass: `" + gadget.p95Pass() + "`");
        if (!gadget.misses().isEmpty()) {
            lines.add("- sampleMiss: `" + gadget.misses().get(0) + "`");
        }
        lines.add("");
        lines.add("## Navigation");
        lines.add("- scenarios: `" + navigation.scenarioCount() + "`");
        lines.add("- iterations: `" + navigation.iterations() + "`");
        lines.add("- hitRate: `" + format(navigation.hitRate()) + "`");
        lines.add("- emptyResults: `" + navigation.emptyResults() + "`");
        lines.add("- avgCandidates: `" + format(navigation.avgCandidates()) + "`");
        lines.add("- latencyP95Ms: `" + navigation.p95Ms() + "`");
        lines.add("- minHitRate: `" + format(navigation.minHitRate()) + "`");
        lines.add("- maxP95Ms: `" + navigation.maxP95Ms() + "`");
        lines.add("- hitRatePass: `" + navigation.hitRatePass() + "`");
        lines.add("- p95Pass: `" + navigation.p95Pass() + "`");
        if (!navigation.misses().isEmpty()) {
            lines.add("- sampleMiss: `" + navigation.misses().get(0) + "`");
        }
        lines.add("");
        lines.add("## Real Web");
        lines.add("- scenarios: `" + String.join(",", webScenarios) + "`");
        lines.add("- scenarioCount: `" + web.scenarioCount() + "`");
        lines.add("- iterations: `" + web.iterations() + "`");
        lines.add("- passedRuns: `" + web.passedCount() + "`");
        lines.add("- failedRuns: `" + web.failedCount() + "`");
        lines.add("- buildWallP95Ms: `" + web.buildWallP95Ms() + "`");
        lines.add("- maxBuildP95Ms: `" + webMaxBuildP95Ms + "`");
        lines.add("- buildP95Pass: `" + webBuildP95Pass + "`");
        lines.add("- peakHeapMiB: `" + webPeakHeapMiB + "`");
        lines.add("- maxPeakHeapMiB: `" + webMaxPeakHeapMiB + "`");
        lines.add("- peakHeapPass: `" + webPeakHeapPass + "`");
        lines.add("- regressionPass: `" + web.passed() + "`");
        if (!web.failures().isEmpty()) {
            lines.add("");
            lines.add("## Web Failures");
            for (String failure : web.failures()) {
                lines.add("- " + failure);
            }
        }
        BenchReportWriter.writeMarkdown("build-quality-gate.md", "Build Quality Gate", lines);
    }

    private static Set<String> resolveWebScenarios(String raw) {
        Set<String> selected = BuildBaselineBenchTest.resolveScenarioSelection(raw);
        if (!selected.isEmpty()) {
            return selected;
        }
        Set<String> defaults = new LinkedHashSet<>();
        defaults.addAll(BuildBaselineBenchTest.resolveScenarioSelection(DEFAULT_WEB_SCENARIOS));
        return defaults;
    }

    private static int resolveInt(String prop, int def, int min, int max) {
        String raw = System.getProperty(prop);
        if (raw == null || raw.isBlank()) {
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
        if (raw == null || raw.isBlank()) {
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
        if (raw == null || raw.isBlank()) {
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

    private static long toMiB(long bytes) {
        if (bytes <= 0L) {
            return 0L;
        }
        return bytes / (1024L * 1024L);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
