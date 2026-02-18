/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.core.perf;

import me.n1ar4.jar.analyzer.graph.proc.ProcedureRegistry;
import me.n1ar4.jar.analyzer.graph.query.QueryOptions;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional graph benchmark harness.
 * <p>
 * Run with:
 * mvn -Dbench.graph=true test -Dtest=GraphQueryEngineBenchTest
 */
public class GraphQueryEngineBenchTest {
    private static final String ENABLE_PROP = "bench.graph";
    private static final String NODES_PROP = "bench.graph.nodes";
    private static final String FANOUT_PROP = "bench.graph.fanout";
    private static final String QUERIES_PROP = "bench.graph.queries";
    private static final String CONCURRENT_PROP = "bench.graph.concurrent";
    private static final String HOPS_PROP = "bench.graph.hops";
    private static final String MAX_MS_PROP = "bench.graph.maxMs";
    private static final String EXPAND_BUDGET_PROP = "bench.graph.expandBudget";
    private static final String PATH_BUDGET_PROP = "bench.graph.pathBudget";

    @Test
    @SuppressWarnings("all")
    public void benchLargeGraphLongChainConcurrency() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean(ENABLE_PROP),
                "set -D" + ENABLE_PROP + "=true to enable graph benchmark");

        int nodes = resolveInt(NODES_PROP, 200_000, 50_000, 1_000_000);
        int fanOut = resolveInt(FANOUT_PROP, 6, 2, 32);
        int queryCount = resolveInt(QUERIES_PROP, 128, 8, 5_000);
        int concurrency = resolveInt(CONCURRENT_PROP, 8, 1, 128);
        int hops = resolveInt(HOPS_PROP, 256, 64, 256);
        int maxMs = resolveInt(MAX_MS_PROP, 5_000, 100, 180_000);
        int expandBudget = resolveInt(EXPAND_BUDGET_PROP, 2_000_000, 1_000, 20_000_000);
        int pathBudget = resolveInt(PATH_BUDGET_PROP, 10_000, 16, 1_000_000);

        long edgeCount = (long) nodes * fanOut;
        Assumptions.assumeTrue(edgeCount >= 1_000_000L,
                "benchmark graph must contain at least 1M edges");

        GraphSnapshot snapshot = buildSyntheticSnapshot(nodes, fanOut);
        ProcedureRegistry registry = new ProcedureRegistry();
        QueryOptions options = new QueryOptions(
                10_000,
                maxMs,
                hops,
                Math.min(pathBudget, 10_000),
                QueryOptions.PROFILE_LONG_CHAIN,
                expandBudget,
                pathBudget,
                64
        );

        List<QueryTask> tasks = buildTasks(snapshot.getNodeCount(), queryCount, hops);
        for (int i = 0; i < Math.min(8, tasks.size()); i++) {
            executeTask(registry, snapshot, options, tasks.get(i));
        }

        AtomicInteger success = new AtomicInteger();
        AtomicInteger fuseTimeout = new AtomicInteger();
        AtomicInteger otherErrors = new AtomicInteger();
        List<Long> latenciesMs = Collections.synchronizedList(new ArrayList<>(tasks.size()));

        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(tasks.size());
        long benchStartNs = System.nanoTime();
        long peakRssBytes;
        try (RssSampler rssSampler = new RssSampler(50L)) {
            for (QueryTask task : tasks) {
                pool.submit(() -> {
                    try {
                        start.await();
                        long startNs = System.nanoTime();
                        try {
                            executeTask(registry, snapshot, options, task);
                            success.incrementAndGet();
                        } catch (IllegalArgumentException ex) {
                            String code = safe(ex.getMessage());
                            if (code.startsWith("cypher_query_timeout")
                                    || code.startsWith("cypher_expand_budget_exceeded")
                                    || code.startsWith("cypher_path_budget_exceeded")) {
                                fuseTimeout.incrementAndGet();
                            } else {
                                otherErrors.incrementAndGet();
                            }
                        } catch (Exception ignored) {
                            otherErrors.incrementAndGet();
                        } finally {
                            latenciesMs.add((System.nanoTime() - startNs) / 1_000_000L);
                        }
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            boolean finished = done.await(15, TimeUnit.MINUTES);
            assertTrue(finished, "graph benchmark did not finish in time");
            peakRssBytes = rssSampler.peakBytes();
        } finally {
            pool.shutdownNow();
        }
        long elapsedMs = (System.nanoTime() - benchStartNs) / 1_000_000L;

        long p50 = percentile(latenciesMs, 0.50);
        long p95 = percentile(latenciesMs, 0.95);
        long p99 = percentile(latenciesMs, 0.99);
        int total = latenciesMs.size();
        double timeoutRate = total == 0 ? 0.0 : (double) fuseTimeout.get() / total;

        System.out.println("[graph-bench] nodes=" + snapshot.getNodeCount()
                + " edges=" + snapshot.getEdgeCount()
                + " hops=" + hops
                + " concurrent=" + concurrency
                + " queries=" + total
                + " elapsedMs=" + elapsedMs);
        System.out.println("[graph-bench] latency p50=" + p50 + "ms"
                + " p95=" + p95 + "ms"
                + " p99=" + p99 + "ms");
        System.out.println("[graph-bench] success=" + success.get()
                + " fuseTimeout=" + fuseTimeout.get()
                + " timeoutRate=" + String.format("%.4f", timeoutRate)
                + " otherErrors=" + otherErrors.get());
        System.out.println("[graph-bench] peakRssMiB=" + toMiB(peakRssBytes));

        assertTrue(!latenciesMs.isEmpty(), "benchmark should produce latency samples");
        assertTrue(p95 > 0L, "p95 latency should be measurable");
    }

    private static void executeTask(ProcedureRegistry registry,
                                    GraphSnapshot snapshot,
                                    QueryOptions options,
                                    QueryTask task) {
        registry.execute(
                "ja.path.shortest",
                List.of("node:" + task.srcNodeId, "node:" + task.dstNodeId, String.valueOf(task.maxHops)),
                Map.of(),
                options,
                snapshot
        );
    }

    private static List<QueryTask> buildTasks(int totalNodes, int queryCount, int hops) {
        int normalizedNodes = Math.max(2, totalNodes - (totalNodes % 2));
        int componentSize = normalizedNodes / 2;
        int distance = Math.min(hops, Math.max(1, componentSize - 2));
        int movableRange = Math.max(1, componentSize - distance);
        List<QueryTask> tasks = new ArrayList<>(queryCount);
        for (int i = 0; i < queryCount; i++) {
            if ((i & 1) == 0) {
                long src = 1L + (i % movableRange);
                long dst = src + distance;
                tasks.add(new QueryTask(src, dst, distance));
            } else {
                long src = 1L + (i % componentSize);
                long dst = componentSize + 1L + (i % componentSize);
                tasks.add(new QueryTask(src, dst, distance));
            }
        }
        return tasks;
    }

    private static GraphSnapshot buildSyntheticSnapshot(int nodeCount, int fanOut) {
        int normalizedNodes = Math.max(2, nodeCount - (nodeCount % 2));
        int componentSize = normalizedNodes / 2;
        int edgesPerNode = Math.max(2, fanOut);
        int totalEdges = normalizedNodes * edgesPerNode;

        Map<Long, GraphNode> nodes = new HashMap<>(normalizedNodes * 2);
        IntArrayBuilder[] outgoingBuilders = new IntArrayBuilder[normalizedNodes + 1];
        IntArrayBuilder[] incomingBuilders = new IntArrayBuilder[normalizedNodes + 1];
        for (int i = 1; i <= normalizedNodes; i++) {
            nodes.put((long) i, new GraphNode(i, "method", 1, "bench/C" + i, "m", "()V", "", -1, -1));
            outgoingBuilders[i] = new IntArrayBuilder(edgesPerNode);
            incomingBuilders[i] = new IntArrayBuilder(edgesPerNode);
        }

        long[] edgeIds = new long[totalEdges];
        long[] srcIds = new long[totalEdges];
        long[] dstIds = new long[totalEdges];
        String[] relTypes = new String[totalEdges];
        String[] confidences = new String[totalEdges];
        String[] evidences = new String[totalEdges];
        int[] opCodes = new int[totalEdges];

        int edgeIndex = 0;
        for (int component = 0; component < 2; component++) {
            int base = component == 0 ? 1 : componentSize + 1;
            for (int offset = 0; offset < componentSize; offset++) {
                int src = base + offset;
                int next = base + ((offset + 1) % componentSize);
                edgeIndex = addEdge(edgeIndex, src, next,
                        "CALLS_DIRECT", "high", "bench-chain",
                        edgeIds, srcIds, dstIds, relTypes, confidences, evidences, opCodes,
                        outgoingBuilders, incomingBuilders);
                for (int extra = 1; extra < edgesPerNode; extra++) {
                    edgeIndex = addEdge(edgeIndex, src, src,
                            "NOISE_LOOP", "low", "bench-noise",
                            edgeIds, srcIds, dstIds, relTypes, confidences, evidences, opCodes,
                            outgoingBuilders, incomingBuilders);
                }
            }
        }

        Map<Long, int[]> outgoing = new HashMap<>(normalizedNodes * 2);
        Map<Long, int[]> incoming = new HashMap<>(normalizedNodes * 2);
        for (int i = 1; i <= normalizedNodes; i++) {
            outgoing.put((long) i, outgoingBuilders[i].toArray());
            incoming.put((long) i, incomingBuilders[i].toArray());
        }

        return GraphSnapshot.ofCompressed(
                1L,
                nodes,
                outgoing,
                incoming,
                edgeIds,
                srcIds,
                dstIds,
                relTypes,
                confidences,
                evidences,
                opCodes,
                Map.of()
        );
    }

    private static int addEdge(int edgeIndex,
                               int src,
                               int dst,
                               String relType,
                               String confidence,
                               String evidence,
                               long[] edgeIds,
                               long[] srcIds,
                               long[] dstIds,
                               String[] relTypes,
                               String[] confidences,
                               String[] evidences,
                               int[] opCodes,
                               IntArrayBuilder[] outgoingBuilders,
                               IntArrayBuilder[] incomingBuilders) {
        edgeIds[edgeIndex] = edgeIndex + 1L;
        srcIds[edgeIndex] = src;
        dstIds[edgeIndex] = dst;
        relTypes[edgeIndex] = relType;
        confidences[edgeIndex] = confidence;
        evidences[edgeIndex] = evidence;
        opCodes[edgeIndex] = 0;
        outgoingBuilders[src].add(edgeIndex);
        incomingBuilders[dst].add(edgeIndex);
        return edgeIndex + 1;
    }

    private static int resolveInt(String key, int def, int min, int max) {
        String raw = System.getProperty(key);
        if (raw == null || raw.trim().isEmpty()) {
            return def;
        }
        try {
            int value = Integer.parseInt(raw.trim());
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

    private static long percentile(List<Long> values, double pct) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        List<Long> copy = new ArrayList<>(values);
        Collections.sort(copy);
        int n = copy.size();
        int index = (int) Math.ceil(pct * n) - 1;
        if (index < 0) {
            index = 0;
        }
        if (index >= n) {
            index = n - 1;
        }
        return copy.get(index);
    }

    private static long toMiB(long bytes) {
        if (bytes <= 0L) {
            return 0L;
        }
        return bytes / (1024L * 1024L);
    }

    private static String safe(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private static final class QueryTask {
        private final long srcNodeId;
        private final long dstNodeId;
        private final int maxHops;

        private QueryTask(long srcNodeId, long dstNodeId, int maxHops) {
            this.srcNodeId = srcNodeId;
            this.dstNodeId = dstNodeId;
            this.maxHops = maxHops;
        }
    }

    private static final class IntArrayBuilder {
        private int[] data;
        private int size;

        private IntArrayBuilder(int initialCapacity) {
            this.data = new int[Math.max(4, initialCapacity)];
            this.size = 0;
        }

        private void add(int value) {
            if (size >= data.length) {
                int[] next = new int[data.length * 2];
                System.arraycopy(data, 0, next, 0, data.length);
                data = next;
            }
            data[size++] = value;
        }

        private int[] toArray() {
            int[] out = new int[size];
            System.arraycopy(data, 0, out, 0, size);
            return out;
        }
    }

    private static final class RssSampler implements AutoCloseable {
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final AtomicLong peakBytes = new AtomicLong(0L);
        private final OperatingSystem os;
        private final int pid;
        private final Thread sampler;

        private RssSampler(long intervalMs) {
            SystemInfo info = new SystemInfo();
            this.os = info.getOperatingSystem();
            this.pid = (int) ProcessHandle.current().pid();
            long init = readRssBytes();
            if (init > 0L) {
                peakBytes.set(init);
            }
            this.sampler = Thread.ofPlatform().daemon().name("graph-bench-rss").start(() -> {
                while (running.get()) {
                    long rss = readRssBytes();
                    if (rss > 0L) {
                        peakBytes.accumulateAndGet(rss, Math::max);
                    }
                    try {
                        Thread.sleep(Math.max(10L, intervalMs));
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
        }

        private long peakBytes() {
            long current = readRssBytes();
            if (current > 0L) {
                peakBytes.accumulateAndGet(current, Math::max);
            }
            return peakBytes.get();
        }

        private long readRssBytes() {
            try {
                OSProcess process = os.getProcess(pid);
                if (process == null) {
                    return readRssByPs();
                }
                long rss = process.getResidentSetSize();
                if (rss > 0L) {
                    return rss;
                }
                return readRssByPs();
            } catch (Exception ignored) {
                return readRssByPs();
            }
        }

        private long readRssByPs() {
            Process process = null;
            try {
                process = new ProcessBuilder("ps", "-o", "rss=", "-p", String.valueOf(pid))
                        .redirectErrorStream(true)
                        .start();
                boolean done = process.waitFor(200, TimeUnit.MILLISECONDS);
                if (!done || process.exitValue() != 0) {
                    return -1L;
                }
                byte[] out = process.getInputStream().readAllBytes();
                String text = new String(out).trim();
                if (text.isEmpty()) {
                    return -1L;
                }
                long kb = Long.parseLong(text);
                if (kb <= 0L) {
                    return -1L;
                }
                return kb * 1024L;
            } catch (Exception ignored) {
                return -1L;
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
        }

        @Override
        public void close() {
            running.set(false);
            sampler.interrupt();
            try {
                sampler.join(200L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
