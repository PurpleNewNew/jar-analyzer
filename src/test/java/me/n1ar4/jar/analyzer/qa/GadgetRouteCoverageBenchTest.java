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

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.graph.proc.ProcedureRegistry;
import me.n1ar4.jar.analyzer.graph.query.QueryOptions;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.rules.MethodSemanticFlags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional benchmark harness for gadget route coverage and latency.
 * <p>
 * Run with:
 * mvn test -Dtest=GadgetRouteCoverageBenchTest -Dbench.gadget=true
 */
public class GadgetRouteCoverageBenchTest {
    private static final String ENABLE_PROP = "bench.gadget";
    private static final String ITER_PROP = "bench.gadget.iter";
    private static final String MIN_HIT_PROP = "bench.gadget.minHit";
    private static final String MAX_P95_PROP = "bench.gadget.maxP95Ms";

    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
    }

    @Test
    @SuppressWarnings("all")
    public void benchmarkGadgetRouteCoverage() {
        Assumptions.assumeTrue(Boolean.getBoolean(ENABLE_PROP),
                "set -D" + ENABLE_PROP + "=true to enable gadget benchmark");

        int iterations = resolveInt(ITER_PROP, 40, 1, 500);
        double minHitRate = resolveDouble(MIN_HIT_PROP, 1.0, 0.10, 1.0);
        long maxP95Ms = resolveLong(MAX_P95_PROP, 50L, 1L, 5_000L);
        BenchmarkResult bench = runBenchmark(iterations, minHitRate, maxP95Ms, true);
        assertTrue(bench.hitRatePass(),
                "gadget hit rate below threshold: actual=" + bench.hitRate() + ", expected>=" + minHitRate);
        assertTrue(bench.p95Pass(),
                "gadget p95 too high: actual=" + bench.p95Ms() + "ms, expected<=" + maxP95Ms + "ms");
    }

    static BenchmarkResult runBenchmark(int iterations,
                                        double minHitRate,
                                        long maxP95Ms,
                                        boolean writeReport) {
        ProcedureRegistry registry = new ProcedureRegistry();
        QueryOptions options = QueryOptions.defaults();
        List<Scenario> scenarios = List.of(
                pathScenario("commons-collections", buildContainerCallbackSnapshot(), List.of("node:1", "node:4", "6", "10"), "container-callback"),
                pathScenario("beanutils-comparable", buildComparableContainerSnapshot(), List.of("node:1", "node:4", "6", "10"), "container-callback"),
                pathScenario("badattribute-trigger", buildContainerTriggerSnapshot(), List.of("node:1", "node:3", "6", "10"), "container-trigger"),
                pathScenario("rome-reflection", buildReflectionTriggerSnapshot(), List.of("node:1", "node:4", "6", "10"), "reflection-trigger"),
                pathScenario("jdk-proxy", buildProxyDynamicSnapshot(), List.of("node:1", "node:3", "6", "10"), "proxy-dynamic"),
                trackScenario("search-all-sources", buildSearchAllSourcesSnapshot(),
                        List.of("", "", "", "app/Sink", "sink", "()V", "6", "10", "true"),
                        "container-callback")
        );

        List<Long> latencies = new ArrayList<>(scenarios.size() * iterations);
        List<String> misses = new ArrayList<>();
        Map<String, Integer> scenarioHits = new LinkedHashMap<>();
        int totalRuns = 0;
        int totalHits = 0;

        for (int i = 0; i < iterations; i++) {
            for (Scenario scenario : scenarios) {
                long startNs = System.nanoTime();
                QueryResult result = registry.execute(
                        scenario.procName(),
                        scenario.args(),
                        Map.of(),
                        options,
                        scenario.snapshot()
                );
                long elapsedMs = Math.max(0L, (System.nanoTime() - startNs) / 1_000_000L);
                latencies.add(elapsedMs);
                totalRuns++;

                boolean hit = matchesExpectedRoute(result, scenario.expectedRoute());
                if (hit) {
                    totalHits++;
                    scenarioHits.merge(scenario.name(), 1, Integer::sum);
                } else {
                    misses.add(scenario.name() + "@iter" + i + ":" + describeMiss(result));
                }
            }
        }

        double hitRate = totalRuns == 0 ? 0.0 : (double) totalHits / (double) totalRuns;
        long p50 = percentile(latencies, 0.50);
        long p95 = percentile(latencies, 0.95);
        long p99 = percentile(latencies, 0.99);

        List<String> report = new ArrayList<>();
        report.add("## Summary");
        report.add("- scenarios: `" + scenarios.size() + "`");
        report.add("- iterations: `" + iterations + "`");
        report.add("- runs: `" + totalRuns + "`");
        report.add("- hits: `" + totalHits + "`");
        report.add("- hitRate: `" + format(hitRate) + "`");
        report.add("- latencyP50Ms: `" + p50 + "`");
        report.add("- latencyP95Ms: `" + p95 + "`");
        report.add("- latencyP99Ms: `" + p99 + "`");
        report.add("");
        report.add("## Per Scenario");
        for (Scenario scenario : scenarios) {
            int hits = scenarioHits.getOrDefault(scenario.name(), 0);
            report.add("- `" + scenario.name() + "` route=`" + scenario.expectedRoute() + "` hits=`" + hits + "/" + iterations + "`");
        }
        report.add("");
        report.add("## Thresholds");
        report.add("- minHitRate: `" + format(minHitRate) + "`");
        report.add("- maxP95Ms: `" + maxP95Ms + "`");
        report.add("- hitRatePass: `" + (hitRate >= minHitRate) + "`");
        report.add("- p95Pass: `" + (p95 <= maxP95Ms) + "`");
        if (!misses.isEmpty()) {
            report.add("");
            report.add("## Sample Misses");
            int max = Math.min(12, misses.size());
            for (int i = 0; i < max; i++) {
                report.add("- " + misses.get(i));
            }
        }
        if (writeReport) {
            BenchReportWriter.writeMarkdown("gadget-route-coverage.md", "Gadget Route Coverage Benchmark", report);
        }

        System.out.println("[gadget-bench] runs=" + totalRuns
                + " hits=" + totalHits
                + " hitRate=" + format(hitRate)
                + " p50=" + p50 + "ms"
                + " p95=" + p95 + "ms"
                + " p99=" + p99 + "ms");
        if (!misses.isEmpty()) {
            int max = Math.min(8, misses.size());
            System.out.println("[gadget-bench] sample-misses=" + misses.subList(0, max));
        }
        return new BenchmarkResult(
                scenarios.size(),
                iterations,
                totalRuns,
                totalHits,
                hitRate,
                p50,
                p95,
                p99,
                minHitRate,
                maxP95Ms,
                Map.copyOf(scenarioHits),
                List.copyOf(misses)
        );
    }

    private static Scenario pathScenario(String name,
                                         GraphSnapshot snapshot,
                                         List<String> args,
                                         String expectedRoute) {
        return new Scenario(name, "ja.path.gadget", snapshot, List.copyOf(args), expectedRoute);
    }

    private static Scenario trackScenario(String name,
                                          GraphSnapshot snapshot,
                                          List<String> args,
                                          String expectedRoute) {
        return new Scenario(name, "ja.gadget.track", snapshot, List.copyOf(args), expectedRoute);
    }

    private static boolean matchesExpectedRoute(QueryResult result, String expectedRoute) {
        if (result == null || result.getRows() == null || result.getRows().isEmpty()) {
            return false;
        }
        String expected = safe(expectedRoute);
        for (List<Object> row : result.getRows()) {
            if (row == null || row.size() < 7) {
                continue;
            }
            String evidence = safe(String.valueOf(row.get(6)));
            if (evidence.contains("route=" + expected)) {
                return true;
            }
        }
        return false;
    }

    private static String describeMiss(QueryResult result) {
        if (result == null) {
            return "null-result";
        }
        if (result.getRows() == null || result.getRows().isEmpty()) {
            return "rows=0 warnings=" + result.getWarnings();
        }
        List<Object> row = result.getRows().get(0);
        return "unexpected-evidence=" + (row.size() >= 7 ? safe(String.valueOf(row.get(6))) : "missing");
    }

    private static long percentile(List<Long> values, double ratio) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        List<Long> copy = new ArrayList<>(values);
        copy.sort(Long::compareTo);
        int index = (int) Math.ceil(copy.size() * ratio) - 1;
        index = Math.max(0, Math.min(copy.size() - 1, index));
        return copy.get(index);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
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
            return Math.max(min, Math.min(max, value));
        } catch (Exception ignored) {
            return def;
        }
    }

    private static GraphSnapshot buildContainerCallbackSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, methodNode(1L, "app/Source", "readObject", "(Ljava/io/ObjectInputStream;)V",
                MethodSemanticFlags.SERIALIZABLE_OWNER | MethodSemanticFlags.DESERIALIZATION_CALLBACK));
        nodes.put(2L, methodNode(2L, "java/util/PriorityQueue", "heapify", "()V",
                MethodSemanticFlags.COLLECTION_CONTAINER));
        nodes.put(3L, methodNode(3L, "app/Comparator", "compare", "(Ljava/lang/Object;Ljava/lang/Object;)I",
                MethodSemanticFlags.COMPARATOR_CALLBACK));
        nodes.put(4L, methodNode(4L, "app/Sink", "sink", "()V", 0));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, edge(11L, 1L, 2L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(12L, 2L, 3L, "CALLS_CALLBACK", MethodCallMeta.EVIDENCE_CALLBACK));
        addEdge(outgoing, incoming, edge(13L, 3L, 4L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        return GraphSnapshot.of(101L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphSnapshot buildComparableContainerSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, methodNode(1L, "app/Source", "readObject", "(Ljava/io/ObjectInputStream;)V",
                MethodSemanticFlags.SERIALIZABLE_OWNER | MethodSemanticFlags.DESERIALIZATION_CALLBACK));
        nodes.put(2L, methodNode(2L, "java/util/TreeMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                MethodSemanticFlags.COLLECTION_CONTAINER));
        nodes.put(3L, methodNode(3L, "app/ComparableBean", "compareTo", "(Ljava/lang/Object;)I",
                MethodSemanticFlags.COMPARABLE_CALLBACK));
        nodes.put(4L, methodNode(4L, "app/Sink", "sink", "()V", 0));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, edge(21L, 1L, 2L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(22L, 2L, 3L, "CALLS_CALLBACK", MethodCallMeta.EVIDENCE_CALLBACK));
        addEdge(outgoing, incoming, edge(23L, 3L, 4L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        return GraphSnapshot.of(102L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphSnapshot buildContainerTriggerSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, methodNode(1L, "javax/management/BadAttributeValueExpException", "readObject", "(Ljava/io/ObjectInputStream;)V",
                MethodSemanticFlags.SERIALIZABLE_OWNER
                        | MethodSemanticFlags.DESERIALIZATION_CALLBACK
                        | MethodSemanticFlags.COLLECTION_CONTAINER));
        nodes.put(2L, methodNode(2L, "app/ToStringBean", "toString", "()Ljava/lang/String;",
                MethodSemanticFlags.TOSTRING_TRIGGER));
        nodes.put(3L, methodNode(3L, "app/Sink", "sink", "()V", 0));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, edge(31L, 1L, 2L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(32L, 2L, 3L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        return GraphSnapshot.of(103L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphSnapshot buildReflectionTriggerSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, methodNode(1L, "app/Source", "readObject", "(Ljava/io/ObjectInputStream;)V",
                MethodSemanticFlags.SERIALIZABLE_OWNER | MethodSemanticFlags.DESERIALIZATION_CALLBACK));
        nodes.put(2L, methodNode(2L, "app/ToStringBean", "toString", "()Ljava/lang/String;",
                MethodSemanticFlags.TOSTRING_TRIGGER));
        nodes.put(3L, methodNode(3L, "app/GetterBean", "getOutputProperties", "()Ljava/util/Properties;", 0));
        nodes.put(4L, methodNode(4L, "app/Sink", "sink", "()V", 0));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, edge(41L, 1L, 2L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(42L, 2L, 3L, "CALLS_REFLECTION", MethodCallMeta.EVIDENCE_REFLECTION));
        addEdge(outgoing, incoming, edge(43L, 3L, 4L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        return GraphSnapshot.of(104L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphSnapshot buildProxyDynamicSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, methodNode(1L, "app/Source", "readObject", "(Ljava/io/ObjectInputStream;)V",
                MethodSemanticFlags.SERIALIZABLE_OWNER | MethodSemanticFlags.DESERIALIZATION_CALLBACK));
        nodes.put(2L, methodNode(2L, "app/Proxy", "invoke",
                "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;",
                MethodSemanticFlags.INVOCATION_HANDLER));
        nodes.put(3L, methodNode(3L, "app/Sink", "sink", "()V", 0));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, edge(51L, 1L, 2L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(52L, 2L, 3L, "CALLS_METHOD_HANDLE", MethodCallMeta.EVIDENCE_METHOD_HANDLE));
        return GraphSnapshot.of(105L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphSnapshot buildSearchAllSourcesSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, methodNode(1L, "app/Source", "readObject", "(Ljava/io/ObjectInputStream;)V",
                MethodSemanticFlags.SERIALIZABLE_OWNER | MethodSemanticFlags.DESERIALIZATION_CALLBACK));
        nodes.put(2L, methodNode(2L, "java/util/PriorityQueue", "heapify", "()V",
                MethodSemanticFlags.COLLECTION_CONTAINER));
        nodes.put(3L, methodNode(3L, "app/Comparator", "compare", "(Ljava/lang/Object;Ljava/lang/Object;)I",
                MethodSemanticFlags.COMPARATOR_CALLBACK));
        nodes.put(4L, methodNode(4L, "app/Sink", "sink", "()V", 0));
        nodes.put(5L, methodNode(5L, "app/DeadSource", "readObject", "(Ljava/io/ObjectInputStream;)V",
                MethodSemanticFlags.SERIALIZABLE_OWNER | MethodSemanticFlags.DESERIALIZATION_CALLBACK));
        nodes.put(6L, methodNode(6L, "app/DeadMid", "noop", "()V", 0));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, edge(61L, 1L, 2L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(62L, 2L, 3L, "CALLS_CALLBACK", MethodCallMeta.EVIDENCE_CALLBACK));
        addEdge(outgoing, incoming, edge(63L, 3L, 4L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(64L, 5L, 6L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        return GraphSnapshot.of(106L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphNode methodNode(long id,
                                        String className,
                                        String methodName,
                                        String methodDesc,
                                        int semanticFlags) {
        return new GraphNode(id, "method", 1, className, methodName, methodDesc, "", -1, -1, 0, semanticFlags);
    }

    private static GraphEdge edge(long edgeId,
                                  long srcId,
                                  long dstId,
                                  String relType,
                                  int semanticFlags) {
        return new GraphEdge(edgeId, srcId, dstId, relType, "high", "bench", "", 0, "", -1, -1, semanticFlags);
    }

    private static void addEdge(Map<Long, List<GraphEdge>> outgoing,
                                Map<Long, List<GraphEdge>> incoming,
                                GraphEdge edge) {
        outgoing.computeIfAbsent(edge.getSrcId(), key -> new ArrayList<>()).add(edge);
        incoming.computeIfAbsent(edge.getDstId(), key -> new ArrayList<>()).add(edge);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record Scenario(String name,
                            String procName,
                            GraphSnapshot snapshot,
                            List<String> args,
                            String expectedRoute) {
    }

    static final class BenchmarkResult {
        private final int scenarioCount;
        private final int iterations;
        private final int totalRuns;
        private final int totalHits;
        private final double hitRate;
        private final long p50Ms;
        private final long p95Ms;
        private final long p99Ms;
        private final double minHitRate;
        private final long maxP95Ms;
        private final Map<String, Integer> scenarioHits;
        private final List<String> misses;

        private BenchmarkResult(int scenarioCount,
                                int iterations,
                                int totalRuns,
                                int totalHits,
                                double hitRate,
                                long p50Ms,
                                long p95Ms,
                                long p99Ms,
                                double minHitRate,
                                long maxP95Ms,
                                Map<String, Integer> scenarioHits,
                                List<String> misses) {
            this.scenarioCount = scenarioCount;
            this.iterations = iterations;
            this.totalRuns = totalRuns;
            this.totalHits = totalHits;
            this.hitRate = hitRate;
            this.p50Ms = p50Ms;
            this.p95Ms = p95Ms;
            this.p99Ms = p99Ms;
            this.minHitRate = minHitRate;
            this.maxP95Ms = maxP95Ms;
            this.scenarioHits = scenarioHits == null ? Map.of() : scenarioHits;
            this.misses = misses == null ? List.of() : misses;
        }

        int scenarioCount() {
            return scenarioCount;
        }

        int iterations() {
            return iterations;
        }

        int totalRuns() {
            return totalRuns;
        }

        int totalHits() {
            return totalHits;
        }

        double hitRate() {
            return hitRate;
        }

        long p50Ms() {
            return p50Ms;
        }

        long p95Ms() {
            return p95Ms;
        }

        long p99Ms() {
            return p99Ms;
        }

        double minHitRate() {
            return minHitRate;
        }

        long maxP95Ms() {
            return maxP95Ms;
        }

        Map<String, Integer> scenarioHits() {
            return scenarioHits;
        }

        List<String> misses() {
            return misses;
        }

        boolean hitRatePass() {
            return hitRate >= minHitRate;
        }

        boolean p95Pass() {
            return p95Ms <= maxP95Ms;
        }

        boolean passed() {
            return hitRatePass() && p95Pass();
        }
    }
}
