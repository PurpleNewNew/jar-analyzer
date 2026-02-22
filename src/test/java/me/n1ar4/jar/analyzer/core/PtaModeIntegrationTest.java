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

import me.n1ar4.jar.analyzer.core.pta.ContextSensitivePtaEngine;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PtaModeIntegrationTest {
    private static final String PROP = "jar.analyzer.callgraph.mode";

    @Test
    public void shouldGeneratePtaEdges() throws Exception {
        String old = System.getProperty(PROP);
        try {
            ContextSensitivePtaEngine.clearIncrementalCache();
            System.setProperty(PROP, "pta");
            Path jar = FixtureJars.callbackTestJar();
            WorkspaceContext.updateResolveInnerJars(false);
            CoreRunner.run(jar, null, false, false, true, null, true);

            int ptaCount = countEdges(edge -> "pta".equals(resolveEdgeType(edge)));
            assertTrue(ptaCount > 0, "pta mode should generate pta edges");

            int ctxEvidence = countEdges(edge -> safe(edge.getEvidence()).contains("pta_ctx="));
            assertTrue(ctxEvidence > 0, "pta mode should preserve context evidence");
        } finally {
            ContextSensitivePtaEngine.clearIncrementalCache();
            restoreProperty(old);
        }
    }

    private static int countEdges(Predicate<GraphEdge> predicate) {
        GraphSnapshot snapshot = new GraphStore().loadSnapshot();
        Map<Long, GraphNode> methods = new HashMap<>();
        for (GraphNode node : snapshot.getNodesByKindView("method")) {
            methods.put(node.getNodeId(), node);
        }
        int count = 0;
        for (GraphNode node : methods.values()) {
            for (GraphEdge edge : snapshot.getOutgoingView(node.getNodeId())) {
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

    private static void restoreProperty(String old) {
        if (old == null) {
            System.clearProperty(PROP);
        } else {
            System.setProperty(PROP, old);
        }
    }
}
