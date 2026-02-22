/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.headless;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.DecompileEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.entity.MethodCallResult;
import me.n1ar4.jar.analyzer.graph.flow.FlowOptions;
import me.n1ar4.jar.analyzer.graph.flow.GraphFlowService;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class HeadlessSmokeTest {
    @Test
    @SuppressWarnings("all")
    public void testHeadlessPipelineDoesNotThrow() {
        try {
            System.setProperty("java.awt.headless", "true");
            System.setProperty("jar.analyzer.taint.summary.enable", "false");

            Path file = FixtureJars.springbootTestJar();
            WorkspaceContext.updateResolveInnerJars(false);
            CoreRunner.run(file, null, false, true, true, null, true);

            ConfigFile config = new ConfigFile();
            config.setDbPath(Const.dbFile);
            CoreEngine engine = new CoreEngine(config);
            EngineContext.setEngine(engine);

            Edge edge = pickCalleeWithSingleCaller(engine);
            assertNotNull(edge);

            FlowOptions options = FlowOptions.builder()
                    .fromSink(true)
                    .searchAllSources(false)
                    .depth(2)
                    .timeoutMs(15_000)
                    .maxLimit(1)
                    .maxPaths(1)
                    .sink(edge.calleeClassName, edge.calleeMethodName, edge.calleeMethodDesc)
                    .source(edge.callerClassName, edge.callerMethodName, edge.callerMethodDesc)
                    .build();
            GraphFlowService flowService = new GraphFlowService();
            List<DFSResult> results = flowService.runDfs(options, null).results();
            List<TaintResult> taint = flowService.analyzeDfsResults(
                    results, 15_000, 1, new AtomicBoolean(false), null).results();
            if (taint == null) {
                throw new IllegalStateException("taint result is null");
            }

            // Decompile a concrete class file path; should not trigger any AWT/Swing in headless mode.
            String absPath = engine.getAbsPath(edge.calleeClassName);
            assertNotNull(absPath);
            Path classFile = Paths.get(absPath);
            if (Files.exists(classFile)) {
                DecompileEngine.decompile(classFile);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            fail("headless smoke test failed: " + t);
        }
    }

    private static Edge pickCalleeWithSingleCaller(CoreEngine engine) {
        if (engine == null || !engine.isEnabled()) {
            return null;
        }
        List<MethodCallResult> edges = engine.getCallEdgesByCaller(null, null, null, 0, Integer.MAX_VALUE);
        if (edges == null || edges.isEmpty()) {
            return null;
        }
        Map<String, Candidate> candidates = new HashMap<>();
        for (MethodCallResult row : edges) {
            if (row == null) {
                continue;
            }
            String calleeKey = row.getCalleeClassName() + "|" + row.getCalleeMethodName() + "|" + row.getCalleeMethodDesc();
            String callerKey = row.getCallerClassName() + "|" + row.getCallerMethodName() + "|" + row.getCallerMethodDesc();
            Candidate old = candidates.get(calleeKey);
            if (old == null) {
                candidates.put(calleeKey, new Candidate(callerKey, row, false));
                continue;
            }
            if (!old.callerKey.equals(callerKey)) {
                candidates.put(calleeKey, new Candidate(old.callerKey, old.row, true));
            }
        }
        for (Candidate candidate : candidates.values()) {
            if (candidate == null || candidate.multiCaller || candidate.row == null) {
                continue;
            }
            String absPath = engine.getAbsPath(candidate.row.getCalleeClassName());
            if (absPath == null || absPath.isBlank()) {
                continue;
            }
            Edge edge = new Edge();
            edge.calleeClassName = candidate.row.getCalleeClassName();
            edge.calleeMethodName = candidate.row.getCalleeMethodName();
            edge.calleeMethodDesc = candidate.row.getCalleeMethodDesc();
            edge.callerClassName = candidate.row.getCallerClassName();
            edge.callerMethodName = candidate.row.getCallerMethodName();
            edge.callerMethodDesc = candidate.row.getCallerMethodDesc();
            return edge;
        }
        return null;
    }

    private static final class Candidate {
        private final String callerKey;
        private final MethodCallResult row;
        private final boolean multiCaller;

        private Candidate(String callerKey, MethodCallResult row, boolean multiCaller) {
            this.callerKey = callerKey;
            this.row = row;
            this.multiCaller = multiCaller;
        }
    }

    private static final class Edge {
        private String callerClassName;
        private String callerMethodName;
        private String callerMethodDesc;
        private String calleeClassName;
        private String calleeMethodName;
        private String calleeMethodDesc;
    }
}
