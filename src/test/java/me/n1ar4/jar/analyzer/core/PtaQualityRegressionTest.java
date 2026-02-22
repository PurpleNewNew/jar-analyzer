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

public class PtaQualityRegressionTest {
    private static final String PROP = "jar.analyzer.callgraph.mode";
    private static final ExpectedEdge[] EXPECTED = new ExpectedEdge[]{
            new ExpectedEdge("me/n1ar4/cb/CallbackEntry", "threadStart", "()V",
                    "me/n1ar4/cb/MyThread", "run", "()V"),
            new ExpectedEdge("me/n1ar4/cb/CallbackEntry", "executorSubmit", "()V",
                    "me/n1ar4/cb/MyRunnable", "run", "()V"),
            new ExpectedEdge("me/n1ar4/cb/CallbackEntry", "doPrivileged", "()Ljava/lang/Object;",
                    "me/n1ar4/cb/MyAction", "run", "()Ljava/lang/Object;"),
            new ExpectedEdge("me/n1ar4/cb/CallbackEntry", "completableSupply", "()V",
                    "me/n1ar4/cb/MySupplier", "get", "()Ljava/lang/Object;"),
            new ExpectedEdge("me/n1ar4/cb/CallbackEntry", "reflectInvoke", "()V",
                    "me/n1ar4/cb/ReflectionTarget", "target", "()V"),
            new ExpectedEdge("me/n1ar4/cb/CallbackEntry", "dynamicProxy", "()V",
                    "me/n1ar4/cb/MyInvocationHandler", "invoke",
                    "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;")
    };

    @Test
    public void shouldImproveRecallWithoutExplodingFalsePositives() throws Exception {
        String old = System.getProperty(PROP);
        try {
            ModeMetrics rta = buildAndMeasure("rta");
            ModeMetrics pta = buildAndMeasure("pta");

            // False-negative proxy: expected fixture edges should not regress in PTA mode.
            assertTrue(pta.expectedMatched >= rta.expectedMatched,
                    "pta recall proxy should be >= rta");

            // False-positive proxy: low-confidence and total edges should stay bounded.
            assertTrue(pta.totalEdges <= rta.totalEdges + 128,
                    "pta should not over-expand total edges aggressively");
            assertTrue(pta.lowConfidenceEdges <= rta.lowConfidenceEdges + 64,
                    "pta low-confidence growth should remain bounded");

            // PTA-specific evidence must exist if PTA mode is enabled.
            assertTrue(pta.ptaTypeEdges > 0 || pta.ptaContextEvidenceEdges > 0,
                    "pta mode should emit pta-specific edge evidence");
            assertTrue(pta.ptaContextEvidenceEdges > 0, "pta mode should emit context evidence");
            assertTrue(edgeExistsWithType("me/n1ar4/cb/CallbackEntry", "reflectViaHelperFlow", "()V",
                            "me/n1ar4/cb/ReflectionTarget", "target", "()V",
                            "reflection"),
                    "pta mode should preserve helper-based reflection closure");
            assertTrue(edgeExistsWithType("me/n1ar4/cb/CallbackEntry", "methodHandleViaHelperFlow", "()V",
                            "me/n1ar4/cb/ReflectionTarget", "target", "()V",
                            "method_handle"),
                    "pta mode should preserve helper-based MethodHandle closure");
        } finally {
            restoreProperty(old);
        }
    }

    private static ModeMetrics buildAndMeasure(String mode) throws Exception {
        ContextSensitivePtaEngine.clearIncrementalCache();
        System.setProperty(PROP, mode);
        Path jar = FixtureJars.callbackTestJar();
        WorkspaceContext.updateResolveInnerJars(false);
        CoreRunner.run(jar, null, false, false, true, null, true);

        int matched = 0;
        for (ExpectedEdge edge : EXPECTED) {
            if (edgeExists(edge)) {
                matched++;
            }
        }
        int total = countEdges(edge -> true);
        int low = countEdges(edge -> "low".equals(safe(edge.getConfidence())));
        int ptaType = countEdges(edge -> "pta".equals(resolveEdgeType(edge)));
        int ptaEvidence = countEdges(edge -> safe(edge.getEvidence()).contains("pta_ctx="));
        return new ModeMetrics(mode, matched, total, low, ptaType, ptaEvidence);
    }

    private static boolean edgeExists(ExpectedEdge edge) throws Exception {
        return edgeExistsWithType(edge.callerClass, edge.callerMethod, edge.callerDesc,
                edge.calleeClass, edge.calleeMethod, edge.calleeDesc, null);
    }

    private static boolean edgeExistsWithType(String callerClass,
                                              String callerMethod,
                                              String callerDesc,
                                              String calleeClass,
                                              String calleeMethod,
                                              String calleeDesc,
                                              String edgeType) throws Exception {
        GraphSnapshot snapshot = new GraphStore().loadSnapshot();
        Map<Long, GraphNode> methods = methodNodes(snapshot);
        for (GraphNode src : methods.values()) {
            if (!matchesMethod(src, callerClass, callerMethod, callerDesc)) {
                continue;
            }
            for (GraphEdge edge : snapshot.getOutgoingView(src.getNodeId())) {
                if (!isCallEdge(edge)) {
                    continue;
                }
                GraphNode dst = methods.get(edge.getDstId());
                if (dst == null || !matchesMethod(dst, calleeClass, calleeMethod, calleeDesc)) {
                    continue;
                }
                if (edgeType != null && !edgeType.equals(resolveEdgeType(edge))) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    private static int countEdges(Predicate<GraphEdge> predicate) {
        GraphSnapshot snapshot = new GraphStore().loadSnapshot();
        Map<Long, GraphNode> methods = methodNodes(snapshot);
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

    private static void restoreProperty(String old) {
        if (old == null) {
            System.clearProperty(PROP);
        } else {
            System.setProperty(PROP, old);
        }
    }

    private static Map<Long, GraphNode> methodNodes(GraphSnapshot snapshot) {
        Map<Long, GraphNode> out = new HashMap<>();
        for (GraphNode node : snapshot.getNodesByKindView("method")) {
            out.put(node.getNodeId(), node);
        }
        return out;
    }

    private static boolean matchesMethod(GraphNode node, String className, String methodName, String methodDesc) {
        if (node == null) {
            return false;
        }
        return normalizeClass(node.getClassName()).equals(normalizeClass(className))
                && safe(node.getMethodName()).equals(safe(methodName))
                && safe(node.getMethodDesc()).equals(safe(methodDesc));
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

    private static String normalizeClass(String value) {
        String out = safe(value).replace('\\', '/');
        if (out.endsWith(".class")) {
            out = out.substring(0, out.length() - ".class".length());
        }
        return out;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class ExpectedEdge {
        private final String callerClass;
        private final String callerMethod;
        private final String callerDesc;
        private final String calleeClass;
        private final String calleeMethod;
        private final String calleeDesc;

        private ExpectedEdge(String callerClass,
                             String callerMethod,
                             String callerDesc,
                             String calleeClass,
                             String calleeMethod,
                             String calleeDesc) {
            this.callerClass = callerClass;
            this.callerMethod = callerMethod;
            this.callerDesc = callerDesc;
            this.calleeClass = calleeClass;
            this.calleeMethod = calleeMethod;
            this.calleeDesc = calleeDesc;
        }
    }

    private static final class ModeMetrics {
        @SuppressWarnings("unused")
        private final String mode;
        private final int expectedMatched;
        private final int totalEdges;
        private final int lowConfidenceEdges;
        private final int ptaTypeEdges;
        private final int ptaContextEvidenceEdges;

        private ModeMetrics(String mode,
                            int expectedMatched,
                            int totalEdges,
                            int lowConfidenceEdges,
                            int ptaTypeEdges,
                            int ptaContextEvidenceEdges) {
            this.mode = mode;
            this.expectedMatched = expectedMatched;
            this.totalEdges = totalEdges;
            this.lowConfidenceEdges = lowConfidenceEdges;
            this.ptaTypeEdges = ptaTypeEdges;
            this.ptaContextEvidenceEdges = ptaContextEvidenceEdges;
        }
    }
}
