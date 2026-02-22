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

public class PtaOnTheFlySemanticClosureTest {
    private static final String PROP_MODE = "jar.analyzer.callgraph.mode";
    private static final String PROP_EDGE_INFER = "jar.analyzer.edge.infer.enable";
    private static final String PROP_ONFLY = "jar.analyzer.pta.semantic.onfly.enable";
    private static final String PROP_INCREMENTAL = "jar.analyzer.pta.incremental";

    @Test
    public void shouldKeepCallbackSemanticsWhenPostEdgePipelineDisabled() throws Exception {
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

            assertCallbackEdge("threadStart", "()V",
                    "me/n1ar4/cb/MyThread", "run", "()V",
                    "thread_start");
            assertCallbackEdge("executorSubmit", "()V",
                    "me/n1ar4/cb/MyRunnable", "run", "()V",
                    "executor_runnable");
            assertCallbackEdge("doPrivileged", "()Ljava/lang/Object;",
                    "me/n1ar4/cb/MyAction", "run", "()Ljava/lang/Object;",
                    "doPrivileged");
            assertCallbackEdge("completableSupply", "()V",
                    "me/n1ar4/cb/MySupplier", "get", "()Ljava/lang/Object;",
                    "completable_future_supplier");
            assertCallbackEdge("dynamicProxy", "()V",
                    "me/n1ar4/cb/MyInvocationHandler", "invoke",
                    "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;",
                    "dynamic_proxy_jdk");
            assertCallbackEdge("cglibProxy", "()V",
                    "me/n1ar4/cb/MyCglibInterceptor", "intercept",
                    "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;Lorg/springframework/cglib/proxy/MethodProxy;)Ljava/lang/Object;",
                    "dynamic_proxy_cglib");
            assertCallbackEdge("springAopProxy", "()V",
                    "me/n1ar4/cb/MySpringMethodInterceptor", "invoke",
                    "(Lorg/aopalliance/intercept/MethodInvocation;)Ljava/lang/Object;",
                    "dynamic_proxy_spring_aop");
            assertCallbackEdge("byteBuddyProxy", "()V",
                    "me/n1ar4/cb/MyByteBuddyInterceptor", "intercept",
                    "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;",
                    "dynamic_proxy_bytebuddy");
            assertReflectionEdge("reflectViaLoadClassApi", "()V",
                    "me/n1ar4/cb/ReflectionTarget", "target", "()V",
                    "pta_reflection");
            assertReflectionEdge("reflectViaHelperFlow", "()V",
                    "me/n1ar4/cb/ReflectionTarget", "target", "()V",
                    "pta_reflection");
            assertMethodHandleEdge("methodHandleViaHelperFlow", "()V",
                    "me/n1ar4/cb/ReflectionTarget", "target", "()V",
                    "pta_method_handle");
        } finally {
            ContextSensitivePtaEngine.clearIncrementalCache();
            restoreProperty(PROP_MODE, oldMode);
            restoreProperty(PROP_EDGE_INFER, oldInfer);
            restoreProperty(PROP_ONFLY, oldOnFly);
            restoreProperty(PROP_INCREMENTAL, oldIncremental);
        }
    }

    private static void assertCallbackEdge(String callerMethod,
                                           String callerDesc,
                                           String calleeClass,
                                           String calleeMethod,
                                           String calleeDesc,
                                           String evidenceNeedle) throws Exception {
        assertTrue(edgeExists(
                        "me/n1ar4/cb/CallbackEntry",
                        callerMethod,
                        callerDesc,
                        calleeClass,
                        calleeMethod,
                        calleeDesc,
                        "callback",
                        evidenceNeedle),
                "missing callback edge: " + callerMethod + " -> " + calleeClass + "." + calleeMethod);
    }

    private static void restoreProperty(String key, String oldValue) {
        if (oldValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, oldValue);
        }
    }

    private static void assertReflectionEdge(String callerMethod,
                                             String callerDesc,
                                             String calleeClass,
                                             String calleeMethod,
                                             String calleeDesc,
                                             String evidenceNeedle) throws Exception {
        assertTrue(edgeExists(
                        "me/n1ar4/cb/CallbackEntry",
                        callerMethod,
                        callerDesc,
                        calleeClass,
                        calleeMethod,
                        calleeDesc,
                        "reflection",
                        evidenceNeedle),
                "missing reflection edge: " + callerMethod + " -> " + calleeClass + "." + calleeMethod);
    }

    private static void assertMethodHandleEdge(String callerMethod,
                                               String callerDesc,
                                               String calleeClass,
                                               String calleeMethod,
                                               String calleeDesc,
                                               String evidenceNeedle) throws Exception {
        assertTrue(edgeExists(
                        "me/n1ar4/cb/CallbackEntry",
                        callerMethod,
                        callerDesc,
                        calleeClass,
                        calleeMethod,
                        calleeDesc,
                        "method_handle",
                        evidenceNeedle),
                "missing method_handle edge: " + callerMethod + " -> " + calleeClass + "." + calleeMethod);
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
