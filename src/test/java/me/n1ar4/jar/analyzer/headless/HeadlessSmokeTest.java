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
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.graph.flow.model.FlowPath;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.DecompileDispatcher;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.engine.model.CallEdgeView;
import me.n1ar4.jar.analyzer.graph.flow.FlowOptions;
import me.n1ar4.jar.analyzer.graph.flow.GraphFlowService;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
            ProjectRuntimeContext.updateResolveInnerJars(false);
            CoreRunner.run(file, null, false, null);

            ConfigFile config = new ConfigFile();
            config.setDbPath(Neo4jProjectStore.getInstance()
                    .resolveProjectHome(ActiveProjectContext.getActiveProjectKey())
                    .toString());
            CoreEngine engine = new CoreEngine(config);
            EngineContext.setEngine(engine);

            Edge edge = pickAnyEdge(engine);
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
            List<FlowPath> results = flowService.runDfs(options, null).results();
            List<TaintResult> taint = flowService.analyzeDfsResults(
                    results, 15_000, 1, new AtomicBoolean(false), null).results();
            if (taint == null) {
                throw new IllegalStateException("taint result is null");
            }

            // Decompile a concrete app class path; should not trigger any AWT/Swing in headless mode.
            String absPath = engine.getAbsPath("me/n1ar4/test/TestApplication");
            if (absPath == null || absPath.isBlank()) {
                absPath = engine.getAbsPath(edge.calleeClassName);
            }
            if (absPath == null || absPath.isBlank()) {
                absPath = firstAvailableClassPath();
            }
            if (absPath != null && !absPath.isBlank()) {
                Path classFile = Paths.get(absPath);
                if (Files.exists(classFile)) {
                    DecompileDispatcher.decompile(classFile);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
            fail("headless smoke test failed: " + t);
        }
    }

    private static Edge pickAnyEdge(CoreEngine engine) {
        if (engine == null || !engine.isEnabled()) {
            return null;
        }
        List<CallEdgeView> edges = engine.getCallEdgesByCaller(null, null, null, 0, Integer.MAX_VALUE);
        if (edges == null || edges.isEmpty()) {
            return null;
        }
        for (CallEdgeView row : edges) {
            if (row == null) {
                continue;
            }
            Edge edge = new Edge();
            edge.calleeClassName = row.getCalleeClassName();
            edge.calleeMethodName = row.getCalleeMethodName();
            edge.calleeMethodDesc = row.getCalleeMethodDesc();
            edge.callerClassName = row.getCallerClassName();
            edge.callerMethodName = row.getCallerMethodName();
            edge.callerMethodDesc = row.getCallerMethodDesc();
            return edge;
        }
        return null;
    }

    private static final class Edge {
        private String callerClassName;
        private String callerMethodName;
        private String callerMethodDesc;
        private String calleeClassName;
        private String calleeMethodName;
        private String calleeMethodDesc;
    }

    private static String firstAvailableClassPath() {
        for (ClassFileEntity row : DatabaseManager.getClassFiles()) {
            if (row == null) {
                continue;
            }
            String path = row.getPathStr();
            if (path != null && !path.isBlank()) {
                return path;
            }
            Path p = row.getPath();
            if (p != null) {
                return p.toAbsolutePath().normalize().toString();
            }
        }
        return null;
    }
}
