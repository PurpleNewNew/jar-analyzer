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

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.dfs.DFSEngine;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.dfs.DfsOutputs;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.taint.TaintAnalyzer;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class ConcurrentDfsTaintTest {
    private static final String DB_PATH = Const.dbFile;

    @Test
    @SuppressWarnings("all")
    public void testConcurrentDfsAndTaintDoesNotCrash() {
        try {
            System.out.println("[concurrent] begin");
            Path file = FixtureJars.springbootTestJar();
            WorkspaceContext.setResolveInnerJars(false);
            System.out.println("[concurrent] build db start");
            // Use quick mode here to reduce DB write surface in tests; DFS/Taint should still work.
            CoreRunner.run(file, null, false, true, true, null, true);
            System.out.println("[concurrent] build db done");

            ConfigFile config = new ConfigFile();
            config.setDbPath(DB_PATH);
            CoreEngine engine = new CoreEngine(config);
            EngineContext.setEngine(engine);
            System.out.println("[concurrent] engine ready");

            MethodRow sink = pickAnyMethod();
            assertNotNull(sink);
            System.out.println("[concurrent] picked sink");

            int threads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
            int tasks = Math.min(8, threads * 2);
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            try {
                List<Future<Void>> futures = new ArrayList<>();
                for (int i = 0; i < tasks; i++) {
                    futures.add(pool.submit(new DfsTaintTask(sink)));
                }
                System.out.println("[concurrent] tasks submitted: " + tasks);
                for (Future<Void> f : futures) {
                    f.get(60, TimeUnit.SECONDS);
                }
                System.out.println("[concurrent] tasks done");
            } finally {
                pool.shutdownNow();
                pool.awaitTermination(10, TimeUnit.SECONDS);
            }
            System.out.println("[concurrent] end");
        } catch (Throwable t) {
            t.printStackTrace();
            fail("concurrent dfs/taint failed: " + t);
        }
    }

    private static final class DfsTaintTask implements Callable<Void> {
        private final MethodRow sink;

        private DfsTaintTask(MethodRow sink) {
            this.sink = sink;
        }

        @Override
        public Void call() {
            DFSEngine dfs = new DFSEngine(DfsOutputs.noop(), true, true, 6);
            dfs.setMaxLimit(3);
            dfs.setMaxPaths(3);
            dfs.setMaxNodes(800);
            dfs.setMaxEdges(4000);
            dfs.setTimeoutMs(15_000);
            dfs.setSink(sink.className, sink.methodName, sink.methodDesc);
            dfs.doAnalyze();
            List<DFSResult> results = dfs.getResults();
            AtomicBoolean cancel = new AtomicBoolean(false);
            List<TaintResult> taint = TaintAnalyzer.analyze(results, 15_000, 3, cancel);
            // Just ensure it runs; the fixture graph may produce 0 results on some sinks.
            if (taint == null) {
                throw new IllegalStateException("taint result is null");
            }
            return null;
        }
    }

    private static MethodRow pickAnyMethod() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH)) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT class_name, method_name, method_desc FROM method_table LIMIT 1");
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new MethodRow(
                        rs.getString(1),
                        rs.getString(2),
                        rs.getString(3));
            } finally {
                stmt.close();
            }
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
}
