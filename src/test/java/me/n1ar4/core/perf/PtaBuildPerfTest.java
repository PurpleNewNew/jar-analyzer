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

import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PtaBuildPerfTest {
    private static final String ENABLE_PROP = "bench.pta";
    private static final String ITER_PROP = "bench.pta.iter";
    private static final String MODE_PROP = "jar.analyzer.callgraph.mode";

    @Test
    public void benchPtaBuildStability() {
        Assumptions.assumeTrue(Boolean.getBoolean(ENABLE_PROP),
                "set -D" + ENABLE_PROP + "=true to enable pta benchmark");
        int iters = resolveInt(ITER_PROP, 3, 2, 10);
        String oldMode = System.getProperty(MODE_PROP);
        try {
            System.setProperty(MODE_PROP, "pta");
            WorkspaceContext.setResolveInnerJars(false);
            Path callbackJar = FixtureJars.callbackTestJar();
            Path springJar = FixtureJars.springbootTestJar();
            // Warm-up.
            runOnce(callbackJar);
            runOnce(springJar);

            List<Long> callbackMs = new ArrayList<>();
            List<Long> springMs = new ArrayList<>();
            List<Long> callbackEdges = new ArrayList<>();
            List<Long> springEdges = new ArrayList<>();
            for (int i = 0; i < iters; i++) {
                RunMetrics cb = runOnce(callbackJar);
                callbackMs.add(cb.durationMs);
                callbackEdges.add(cb.getEdgeCount());

                RunMetrics sp = runOnce(springJar);
                springMs.add(sp.durationMs);
                springEdges.add(sp.getEdgeCount());
            }

            long cbMax = max(callbackMs);
            long cbMin = min(callbackMs);
            long spMax = max(springMs);
            long spMin = min(springMs);
            long cbEdgeMax = max(callbackEdges);
            long cbEdgeMin = min(callbackEdges);
            long spEdgeMax = max(springEdges);
            long spEdgeMin = min(springEdges);

            // Runtime stability: worst case should stay within 2.2x of best case in same JVM run.
            assertTrue(cbMax <= Math.max(1L, (long) Math.ceil(cbMin * 2.2)),
                    "callback fixture pta runtime jitter is too high");
            assertTrue(spMax <= Math.max(1L, (long) Math.ceil(spMin * 2.2)),
                    "spring fixture pta runtime jitter is too high");

            // Graph stability: edge count should be deterministic run-to-run.
            assertTrue(cbEdgeMax == cbEdgeMin, "callback fixture edge count should stay stable");
            assertTrue(spEdgeMax == spEdgeMin, "spring fixture edge count should stay stable");
        } finally {
            if (oldMode == null) {
                System.clearProperty(MODE_PROP);
            } else {
                System.setProperty(MODE_PROP, oldMode);
            }
        }
    }

    private static RunMetrics runOnce(Path jar) {
        long start = System.nanoTime();
        CoreRunner.BuildResult result = CoreRunner.run(jar, null, false, false, true, null, true);
        long durationMs = (System.nanoTime() - start) / 1_000_000L;
        return new RunMetrics(durationMs, result.getEdgeCount());
    }

    private static int resolveInt(String key, int def, int min, int max) {
        String raw = System.getProperty(key);
        if (raw == null || raw.trim().isEmpty()) {
            return def;
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
            return def;
        }
    }

    private static long max(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        return Collections.max(values);
    }

    private static long min(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        return Collections.min(values);
    }

    private static final class RunMetrics {
        private final long durationMs;
        private final long edgeCount;

        private RunMetrics(long durationMs, long edgeCount) {
            this.durationMs = durationMs;
            this.edgeCount = edgeCount;
        }

        private long getEdgeCount() {
            return edgeCount;
        }
    }
}
