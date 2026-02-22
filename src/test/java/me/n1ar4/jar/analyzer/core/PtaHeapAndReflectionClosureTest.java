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

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PtaHeapAndReflectionClosureTest {
    private static final String MODE_PROP = "jar.analyzer.callgraph.mode";
    private static final String INCREMENTAL_PROP = "jar.analyzer.pta.incremental";

    @Test
    public void shouldModelHeapAndClassLoaderReflectionInPtaMode() throws Exception {
        String oldMode = System.getProperty(MODE_PROP);
        String oldIncremental = System.getProperty(INCREMENTAL_PROP);
        try {
            ContextSensitivePtaEngine.clearIncrementalCache();
            System.setProperty(MODE_PROP, "pta");
            System.setProperty(INCREMENTAL_PROP, "false");

            Path jar = FixtureJars.callbackTestJar();
            WorkspaceContext.updateResolveInnerJars(false);
            CoreRunner.run(jar, null, false, false, true, null, true);

            assertTrue(hasPtaInterfaceEdge("ptaFieldSensitiveDispatch"),
                    "field flow should produce PTA edge for Task.run callsite");
            assertTrue(hasPtaInterfaceEdge("ptaArraySensitiveDispatch"),
                    "array flow should produce PTA edge for Task.run callsite");
            assertTrue(hasPtaInterfaceEdge("ptaNativeArrayCopyDispatch"),
                    "native arraycopy flow should produce PTA edge for Task.run callsite");

            assertTrue(hasDispatchEdge("ptaFieldSensitiveDispatch", "FastTask"),
                    "field flow should retain FastTask dispatch edge");
            assertTrue(hasDispatchEdge("ptaArraySensitiveDispatch", "FastTask"),
                    "array flow should retain FastTask dispatch edge");
            assertTrue(hasDispatchEdge("ptaNativeArrayCopyDispatch", "FastTask"),
                    "native arraycopy flow should retain FastTask dispatch edge");

            assertTrue(hasReflectionEdge("reflectWithClassLoaderChain"),
                    "Class.forName(..., loader) chain should resolve reflection target");
            assertTrue(hasReflectionEdge("reflectViaLoadClassApi"),
                    "ClassLoader.loadClass + normalized method-name chain should resolve reflection target");
            assertTrue(hasReflectionEdge("reflectViaHelperFlow"),
                    "inter-procedural helper string flow should still resolve reflection target");
            assertTrue(hasMethodHandleEdge("methodHandleViaHelperFlow"),
                    "helper-based MethodHandle flow should resolve method_handle target");
        } finally {
            ContextSensitivePtaEngine.clearIncrementalCache();
            restoreProperty(MODE_PROP, oldMode);
            restoreProperty(INCREMENTAL_PROP, oldIncremental);
        }
    }

    private static boolean hasPtaInterfaceEdge(String callerMethod) throws Exception {
        return edgeExists(
                "me/n1ar4/cb/CallbackEntry",
                callerMethod,
                "()V",
                "me/n1ar4/cb/Task",
                "run",
                "()V",
                "pta",
                null
        );
    }

    private static boolean hasDispatchEdge(String callerMethod, String calleeClassSimpleName) throws Exception {
        return edgeExists(
                "me/n1ar4/cb/CallbackEntry",
                callerMethod,
                "()V",
                "me/n1ar4/cb/" + calleeClassSimpleName,
                "run",
                "()V",
                "dispatch",
                null
        );
    }

    private static boolean hasReflectionEdge(String callerMethod) throws Exception {
        return edgeExists(
                "me/n1ar4/cb/CallbackEntry",
                callerMethod,
                "()V",
                "me/n1ar4/cb/ReflectionTarget",
                "target",
                "()V",
                "reflection",
                null
        );
    }

    private static boolean hasMethodHandleEdge(String callerMethod) throws Exception {
        return edgeExists(
                "me/n1ar4/cb/CallbackEntry",
                callerMethod,
                "()V",
                "me/n1ar4/cb/ReflectionTarget",
                "target",
                "()V",
                "method_handle",
                null
        );
    }

    private static void restoreProperty(String key, String old) {
        if (old == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, old);
        }
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
        Map<Long, GraphNode> methods = new HashMap<>();
        for (GraphNode node : snapshot.getNodesByKindView("method")) {
            methods.put(node.getNodeId(), node);
        }
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
                if (!resolveEdgeType(edge).equals(edgeType)) {
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
