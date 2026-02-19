/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.concurrent;

import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.graph.flow.FlowOptions;
import me.n1ar4.jar.analyzer.graph.flow.GraphFlowService;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BuildTwiceIsolationTest {
    private static final String DB_PATH = Const.dbFile;

    @Test
    @SuppressWarnings("all")
    public void testBuildTwiceAndConcurrentDfsTaintDoesNotMix() throws Exception {
        Path jar = FixtureJars.springbootTestJar();

        WorkspaceContext.setResolveInnerJars(false);

        long before = DatabaseManager.getBuildSeq();

        CoreRunner.BuildResult first = CoreRunner.run(jar, null, false, false, true, null, true);
        long buildSeq1 = first.getBuildSeq();
        assertTrue(buildSeq1 > before, "buildSeq should advance after build-1");
        DbMetrics m1 = readDbMetrics();

        MethodRow sink1 = pickDeterministicMethod();
        assertNotNull(sink1);
        runConcurrentDfsTaint(sink1, 6);

        CoreRunner.BuildResult second = CoreRunner.run(jar, null, false, false, true, null, true);
        long buildSeq2 = second.getBuildSeq();
        assertTrue(buildSeq2 > buildSeq1, "buildSeq should advance after build-2");
        DbMetrics m2 = readDbMetrics();

        // DB should be reproducible for the same fixture input.
        assertEquals(m1.methodCount, m2.methodCount, "method_table count should be stable across rebuilds");
        assertEquals(m1.edgeCount, m2.edgeCount, "method_call_table count should be stable across rebuilds");

        MethodRow sink2 = pickDeterministicMethod();
        assertNotNull(sink2);
        runConcurrentDfsTaint(sink2, 6);
    }

    private static void runConcurrentDfsTaint(MethodRow sink, int tasks) throws Exception {
        int threads = Math.max(2, Math.min(tasks, Runtime.getRuntime().availableProcessors()));
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (int i = 0; i < tasks; i++) {
                futures.add(pool.submit(new DfsTaintTask(sink)));
            }
            for (Future<Void> f : futures) {
                f.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private static final class DfsTaintTask implements Callable<Void> {
        private final MethodRow sink;

        private DfsTaintTask(MethodRow sink) {
            this.sink = sink;
        }

        @Override
        public Void call() {
            AtomicBoolean cancel = new AtomicBoolean(false);
            FlowOptions options = FlowOptions.builder()
                    .fromSink(true)
                    .searchAllSources(true)
                    .depth(6)
                    .maxLimit(3)
                    .maxPaths(3)
                    .maxNodes(800)
                    .maxEdges(4000)
                    .timeoutMs(15_000)
                    .sink(sink.className, sink.methodName, sink.methodDesc)
                    .build();
            GraphFlowService flowService = new GraphFlowService();
            List<DFSResult> results = flowService.runDfs(options, cancel).results();
            List<TaintResult> taint = flowService
                    .analyzeDfsResults(results, 15_000, 3, cancel, null)
                    .results();
            if (taint == null) {
                throw new IllegalStateException("taint result is null");
            }
            return null;
        }
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

    private static DbMetrics readDbMetrics() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH)) {
            long methodCount = count(conn, "SELECT COUNT(*) FROM method_table");
            long edgeCount = count(conn, "SELECT COUNT(*) FROM method_call_table");
            return new DbMetrics(methodCount, edgeCount);
        }
    }

    private static long count(Connection conn, String sql) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (!rs.next()) {
                return 0L;
            }
            return rs.getLong(1);
        }
    }

    private static final class DbMetrics {
        private final long methodCount;
        private final long edgeCount;

        private DbMetrics(long methodCount, long edgeCount) {
            this.methodCount = methodCount;
            this.edgeCount = edgeCount;
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
}
