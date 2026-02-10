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

import me.n1ar4.jar.analyzer.core.AnalyzeEnv;
import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.dfs.DFSEngine;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.dfs.DfsOutputs;
import me.n1ar4.jar.analyzer.taint.TaintAnalyzer;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optional benchmark harness.
 * <p>
 * Run with: mvn -Dbench=true -Dbench.iter=10 test -Dtest=JarAnalyzerBenchTest
 */
public class JarAnalyzerBenchTest {
    private static final String DB_PATH = "jar-analyzer.db";
    private static final String BENCH_PROP = "bench";
    private static final String BENCH_JAR_PROP = "bench.jar";
    private static final String BENCH_ITER_PROP = "bench.iter";
    private static final String BENCH_DEPTH_PROP = "bench.depth";

    @Test
    @SuppressWarnings("all")
    public void benchBuildDfsTaint() throws Exception {
        String enabled = System.getProperty(BENCH_PROP);
        Assumptions.assumeTrue(enabled != null && !enabled.trim().isEmpty(),
                "set -D" + BENCH_PROP + "=true to enable benchmark");

        Path jar = resolveJar();
        Assumptions.assumeTrue(jar != null && Files.exists(jar), "bench jar not found");

        AnalyzeEnv.isCli = true;
        AnalyzeEnv.jarsInJar = false;

        CoreRunner.BuildResult build = CoreRunner.run(jar, null, false);
        System.out.println("[bench] jar=" + jar.toAbsolutePath());
        System.out.println("[bench] buildSeq=" + build.getBuildSeq()
                + " classFiles=" + build.getClassFileCount()
                + " classes=" + build.getClassCount()
                + " methods=" + build.getMethodCount()
                + " edges=" + build.getEdgeCount()
                + " dbSize=" + build.getDbSizeLabel());

        MethodRow sink = pickDeterministicMethod();
        Assumptions.assumeTrue(sink != null, "sink method not found");

        int depth = resolveInt(BENCH_DEPTH_PROP, 6, 1, 30);
        int iters = resolveInt(BENCH_ITER_PROP, 10, 1, 200);

        // Warm-up to populate caches.
        runOnce(sink, depth);

        List<Long> dfsMs = new ArrayList<>();
        List<Long> taintMs = new ArrayList<>();
        int totalChains = 0;
        for (int i = 0; i < iters; i++) {
            RunResult r = runOnce(sink, depth);
            dfsMs.add(r.dfsMs);
            if (r.taintMs >= 0) {
                taintMs.add(r.taintMs);
            }
            totalChains += r.chainCount;
        }
        System.out.println("[bench] dfs iters=" + dfsMs.size()
                + " p50=" + p(dfsMs, 0.50) + "ms"
                + " p95=" + p(dfsMs, 0.95) + "ms"
                + " chainsAvg=" + (dfsMs.isEmpty() ? 0 : (totalChains / dfsMs.size())));
        if (!taintMs.isEmpty()) {
            System.out.println("[bench] taint iters=" + taintMs.size()
                    + " p50=" + p(taintMs, 0.50) + "ms"
                    + " p95=" + p(taintMs, 0.95) + "ms");
        } else {
            System.out.println("[bench] taint skipped (no chains)");
        }
    }

    private static RunResult runOnce(MethodRow sink, int depth) {
        DFSEngine dfs = new DFSEngine(DfsOutputs.noop(), true, true, depth);
        dfs.setMaxLimit(5);
        dfs.setMaxPaths(10);
        dfs.setMaxNodes(10_000);
        dfs.setMaxEdges(100_000);
        dfs.setTimeoutMs(30_000);
        dfs.setSink(sink.className, sink.methodName, sink.methodDesc);

        long dfsStart = System.nanoTime();
        dfs.doAnalyze();
        long dfsMs = (System.nanoTime() - dfsStart) / 1_000_000L;

        List<DFSResult> results = dfs.getResults();
        if (results == null || results.isEmpty()) {
            return new RunResult(dfsMs, -1L, 0);
        }

        long taintStart = System.nanoTime();
        List<TaintResult> taint = TaintAnalyzer.analyze(results, 30_000, 5, new AtomicBoolean(false));
        long taintMs = (System.nanoTime() - taintStart) / 1_000_000L;
        if (taint == null) {
            taintMs = -1L;
        }
        return new RunResult(dfsMs, taintMs, results.size());
    }

    private static Path resolveJar() {
        String raw = System.getProperty(BENCH_JAR_PROP);
        if (raw != null && !raw.trim().isEmpty()) {
            return Paths.get(raw.trim());
        }
        return FixtureJars.springbootTestJar();
    }

    private static int resolveInt(String prop, int defaultValue, int min, int max) {
        String raw = System.getProperty(prop);
        if (raw == null || raw.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int v = Integer.parseInt(raw.trim());
            if (v < min) {
                return min;
            }
            if (v > max) {
                return max;
            }
            return v;
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static long p(List<Long> values, double pct) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        List<Long> copy = new ArrayList<>(values);
        Collections.sort(copy);
        int n = copy.size();
        int idx = (int) Math.ceil(pct * n) - 1;
        if (idx < 0) {
            idx = 0;
        }
        if (idx >= n) {
            idx = n - 1;
        }
        return copy.get(idx);
    }

    private static MethodRow pickDeterministicMethod() {
        String sql = "SELECT class_name, method_name, method_desc FROM method_table " +
                "ORDER BY jar_id ASC, class_name ASC, method_name ASC, method_desc ASC LIMIT 1";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            return new MethodRow(rs.getString(1), rs.getString(2), rs.getString(3));
        } catch (Exception e) {
            return null;
        }
    }

    private static final class MethodRow {
        private final String className;
        private final String methodName;
        private final String methodDesc;

        private MethodRow(String className, String methodName, String methodDesc) {
            this.className = className;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
        }
    }

    private static final class RunResult {
        private final long dfsMs;
        private final long taintMs;
        private final int chainCount;

        private RunResult(long dfsMs, long taintMs, int chainCount) {
            this.dfsMs = dfsMs;
            this.taintMs = taintMs;
            this.chainCount = chainCount;
        }
    }
}

