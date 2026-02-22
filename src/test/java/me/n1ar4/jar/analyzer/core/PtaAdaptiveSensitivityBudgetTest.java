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

public class PtaAdaptiveSensitivityBudgetTest {
    private static final String PROP_MODE = "jar.analyzer.callgraph.mode";
    private static final String PROP_INCREMENTAL = "jar.analyzer.pta.incremental";
    private static final String PROP_ADAPTIVE = "jar.analyzer.pta.adaptive.enable";
    private static final String PROP_MAX_CTX_METHOD = "jar.analyzer.pta.adaptive.max.contexts.per.method";
    private static final String PROP_MAX_CTX_SITE = "jar.analyzer.pta.adaptive.max.contexts.per.site";
    private static final String PROP_HUGE_DISPATCH = "jar.analyzer.pta.adaptive.huge.dispatch.threshold";
    private static final String PROP_OBJ_WIDEN = "jar.analyzer.pta.adaptive.object.widen.context.threshold";

    @Test
    public void shouldKeepCorePrecisionUnderTightAdaptiveBudgets() throws Exception {
        Map<String, String> old = snapshot(
                PROP_MODE,
                PROP_INCREMENTAL,
                PROP_ADAPTIVE,
                PROP_MAX_CTX_METHOD,
                PROP_MAX_CTX_SITE,
                PROP_HUGE_DISPATCH,
                PROP_OBJ_WIDEN
        );
        try {
            ContextSensitivePtaEngine.clearIncrementalCache();
            System.setProperty(PROP_MODE, "pta");
            System.setProperty(PROP_INCREMENTAL, "false");
            System.setProperty(PROP_ADAPTIVE, "true");
            System.setProperty(PROP_MAX_CTX_METHOD, "2");
            System.setProperty(PROP_MAX_CTX_SITE, "1");
            System.setProperty(PROP_HUGE_DISPATCH, "2");
            System.setProperty(PROP_OBJ_WIDEN, "1");

            Path jar = FixtureJars.callbackTestJar();
            WorkspaceContext.updateResolveInnerJars(false);
            CoreRunner.run(jar, null, false, false, true, null, true);

            assertTrue(countEdges(edge -> "pta".equals(resolveEdgeType(edge))) > 0,
                    "pta edges should still exist under tight adaptive budget");
            assertTrue(countEdges(edge -> safe(edge.getEvidence()).contains("pta_ctx=")) > 0,
                    "pta context evidence should still exist under adaptive widening");

            assertTrue(edgeExists(
                            "me/n1ar4/cb/CallbackEntry",
                            "ptaFieldSensitiveDispatch",
                            "()V",
                            "me/n1ar4/cb/FastTask",
                            "run",
                            "()V",
                            null,
                            null),
                    "tight adaptive budget should not drop core field-sensitive dispatch edge");

            assertTrue(edgeExists(
                            "me/n1ar4/cb/CallbackEntry",
                            "reflectViaHelperFlow",
                            "()V",
                            "me/n1ar4/cb/ReflectionTarget",
                            "target",
                            "()V",
                            "reflection",
                            null),
                    "tight adaptive budget should not drop helper reflection closure");

            assertTrue(edgeExists(
                            "me/n1ar4/cb/CallbackEntry",
                            "methodHandleViaHelperFlow",
                            "()V",
                            "me/n1ar4/cb/ReflectionTarget",
                            "target",
                            "()V",
                            "method_handle",
                            null),
                    "tight adaptive budget should not drop helper MethodHandle closure");
        } finally {
            ContextSensitivePtaEngine.clearIncrementalCache();
            restore(old);
        }
    }

    private static Map<String, String> snapshot(String... keys) {
        HashMap<String, String> out = new HashMap<>();
        if (keys == null) {
            return out;
        }
        for (String key : keys) {
            out.put(key, System.getProperty(key));
        }
        return out;
    }

    private static void restore(Map<String, String> old) {
        if (old == null || old.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : old.entrySet()) {
            if (entry.getValue() == null) {
                System.clearProperty(entry.getKey());
            } else {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        }
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

    private static boolean edgeExists(String callerClass,
                                      String callerMethod,
                                      String callerDesc,
                                      String calleeClass,
                                      String calleeMethod,
                                      String calleeDesc,
                                      String edgeType,
                                      String evidenceNeedle) {
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
                if (evidenceNeedle != null && !safe(edge.getEvidence()).contains(evidenceNeedle)) {
                    continue;
                }
                return true;
            }
        }
        return false;
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
}
