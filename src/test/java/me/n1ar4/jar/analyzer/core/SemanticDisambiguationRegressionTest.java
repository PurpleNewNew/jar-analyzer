/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.pta.ContextSensitivePtaEngine;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.semantic.CallResolver;
import me.n1ar4.jar.analyzer.semantic.TypeSolver;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SemanticDisambiguationRegressionTest {
    private static final String PROP_MODE = "jar.analyzer.callgraph.mode";
    private static final String PROP_EDGE_INFER = "jar.analyzer.edge.infer.enable";
    private static final String PROP_ONFLY = "jar.analyzer.pta.semantic.onfly.enable";
    private static final String PROP_INCREMENTAL = "jar.analyzer.pta.incremental";

    @Test
    public void shouldKeepProxyReflectionAndMethodHandleEvidence() throws Exception {
        String oldMode = System.getProperty(PROP_MODE);
        String oldInfer = System.getProperty(PROP_EDGE_INFER);
        String oldOnFly = System.getProperty(PROP_ONFLY);
        String oldIncremental = System.getProperty(PROP_INCREMENTAL);
        try {
            ContextSensitivePtaEngine.clearIncrementalCache();
            System.setProperty(PROP_MODE, "pta");
            System.setProperty(PROP_EDGE_INFER, "false");
            System.setProperty(PROP_ONFLY, "true");
            System.setProperty(PROP_INCREMENTAL, "false");

            Path jar = FixtureJars.callbackTestJar();
            WorkspaceContext.updateResolveInnerJars(false);
            CoreRunner.run(jar, null, false, false, true, null, true);

            assertTrue(edgeCountByEvidenceLike("%dynamic_proxy_jdk%") > 0,
                    "missing JDK proxy evidence");
            assertTrue(edgeCountByEvidenceLike("%dynamic_proxy_cglib%") > 0,
                    "missing CGLIB proxy evidence");
            assertTrue(edgeCountByEvidenceLike("%dynamic_proxy_bytebuddy%") > 0,
                    "missing ByteBuddy proxy evidence");
            assertTrue(edgeCountByEvidenceLike("%dynamic_proxy_spring_aop%") > 0,
                    "missing Spring AOP proxy evidence");
            assertTrue(edgeCountByEvidenceLike("%pta_reflection%") > 0,
                    "missing PTA reflection evidence");
            assertTrue(edgeCountByEvidenceLike("%pta_method_handle%") > 0,
                    "missing PTA method-handle evidence");
            assertTrue(edgeCountByType("callback") >= 8,
                    "callback edge count regressed");
            assertTrue(edgeCountByType("reflection") >= 6,
                    "reflection edge count regressed");
            assertTrue(edgeCountByType("method_handle") >= 3,
                    "method_handle edge count regressed");
        } finally {
            ContextSensitivePtaEngine.clearIncrementalCache();
            restoreProperty(PROP_MODE, oldMode);
            restoreProperty(PROP_EDGE_INFER, oldInfer);
            restoreProperty(PROP_ONFLY, oldOnFly);
            restoreProperty(PROP_INCREMENTAL, oldIncremental);
        }
    }

    @Test
    public void shouldPreferNonBridgeDescriptorAndKeepOverrideEdges() throws Exception {
        Path jar = FixtureJars.springbootTestJar();
        WorkspaceContext.updateResolveInnerJars(false);
        CoreRunner.run(jar, null, false, false, true, null, true);

        long bridgeMethods = DatabaseManager.getMethodReferences().stream()
                .filter(m -> m != null && (m.getAccess() & Opcodes.ACC_BRIDGE) != 0)
                .count();
        assertTrue(bridgeMethods > 0,
                "fixture should contain bridge methods");
        assertTrue(edgeCountByType("override") > 0,
                "override edges should exist");

        ConfigFile config = new ConfigFile();
        config.setDbPath(Neo4jProjectStore.getInstance()
                .resolveProjectHome(ActiveProjectContext.getActiveProjectKey())
                .toString());
        CoreEngine engine = new CoreEngine(config);
        TypeSolver typeSolver = new TypeSolver(engine);
        CallResolver resolver = typeSolver.getCallResolver();

        String owner = "org/springframework/boot/loader/jar/CentralDirectoryFileHeader";
        String desc = resolver.resolveMethodDescByArgCount(owner, "clone", 0);
        assertEquals("()Lorg/springframework/boot/loader/jar/CentralDirectoryFileHeader;", desc,
                "bridge-aware selection should prefer non-bridge clone descriptor");
    }

    private static int edgeCountByType(String edgeType) throws Exception {
        return countEdges(edge -> edgeType.equals(resolveEdgeType(edge)));
    }

    private static int edgeCountByEvidenceLike(String evidenceLike) throws Exception {
        String needle = safe(evidenceLike).replace("%", "");
        return countEdges(edge -> safe(edge.getEvidence()).contains(needle));
    }

    private static int countEdges(Predicate<GraphEdge> predicate) {
        GraphSnapshot snapshot = new GraphStore().loadSnapshot();
        Map<Long, GraphNode> methods = new HashMap<>();
        for (GraphNode node : snapshot.getNodesByKindView("method")) {
            methods.put(node.getNodeId(), node);
        }
        int count = 0;
        for (GraphNode src : methods.values()) {
            for (GraphEdge edge : snapshot.getOutgoingView(src.getNodeId())) {
                if (!isCallEdge(edge)) {
                    continue;
                }
                if (!methods.containsKey(edge.getDstId())) {
                    continue;
                }
                if (predicate == null || predicate.test(edge)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void restoreProperty(String key, String oldValue) {
        if (oldValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, oldValue);
        }
    }

    private static boolean isCallEdge(GraphEdge edge) {
        return edge != null
                && edge.getRelType() != null
                && edge.getRelType().startsWith("CALLS_");
    }

    private static String resolveEdgeType(GraphEdge edge) {
        if (edge == null) {
            return "direct";
        }
        String rel = safe(edge.getRelType());
        return switch (rel) {
            case "CALLS_DISPATCH" -> "dispatch";
            case "CALLS_REFLECTION" -> "reflection";
            case "CALLS_CALLBACK" -> "callback";
            case "CALLS_OVERRIDE" -> "override";
            case "CALLS_FRAMEWORK" -> "framework";
            case "CALLS_METHOD_HANDLE" -> "method_handle";
            case "CALLS_PTA" -> "pta";
            default -> resolveFromEvidence(edge.getEvidence());
        };
    }

    private static String resolveFromEvidence(String evidence) {
        String ev = safe(evidence).toLowerCase();
        if (ev.contains("pta:") || ev.contains("pta_ctx=")) {
            return "pta";
        }
        if (ev.contains("method_handle")) {
            return "method_handle";
        }
        if (ev.contains("framework") || ev.contains("spring_web_entry")) {
            return "framework";
        }
        if (ev.contains("callback") || ev.contains("dynamic_proxy")) {
            return "callback";
        }
        if (ev.contains("reflection") || ev.contains("tamiflex") || ev.contains("reflect")) {
            return "reflection";
        }
        return "direct";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
