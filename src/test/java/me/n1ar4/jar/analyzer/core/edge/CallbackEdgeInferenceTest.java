/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.edge;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CallbackEdgeInferenceTest {
    private static final String PROP_REFLECTION_LOG = "jar.analyzer.reflection.log";
    private static final String PROP_REFLECTION_LOG_ENABLE = "jar.analyzer.edge.reflection.log.enable";
    private static final String REFLECTION_LOG_CONTENT =
            "Method.invoke;<me.n1ar4.cb.ReflectionTarget: void target()>;" +
                    "me.n1ar4.cb.CallbackEntry.reflectViaParams;\n";

    private String oldReflectionLog;
    private String oldReflectionLogEnable;
    private Path reflectionLogPath;

    @BeforeAll
    public void buildFixture() throws Exception {
        oldReflectionLog = System.getProperty(PROP_REFLECTION_LOG);
        oldReflectionLogEnable = System.getProperty(PROP_REFLECTION_LOG_ENABLE);
        reflectionLogPath = Files.createTempFile("jar-analyzer-reflect-", ".log");
        Files.write(reflectionLogPath, REFLECTION_LOG_CONTENT.getBytes(StandardCharsets.UTF_8));
        System.setProperty(PROP_REFLECTION_LOG_ENABLE, "true");
        System.setProperty(PROP_REFLECTION_LOG, reflectionLogPath.toAbsolutePath().toString());

        Path jar = FixtureJars.callbackTestJar();
        WorkspaceContext.updateResolveInnerJars(false);
        CoreRunner.run(jar, null, false, false, true, null, true);
        ConfigFile config = new ConfigFile();
        config.setDbPath(Neo4jProjectStore.getInstance()
                .resolveProjectHome(ActiveProjectContext.getActiveProjectKey())
                .toString());
        EngineContext.setEngine(new CoreEngine(config));
    }

    @AfterAll
    public void cleanup() {
        restoreProperty(PROP_REFLECTION_LOG, oldReflectionLog);
        restoreProperty(PROP_REFLECTION_LOG_ENABLE, oldReflectionLogEnable);
        if (reflectionLogPath != null) {
            try {
                Files.deleteIfExists(reflectionLogPath);
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    public void testCallbackEdgesExist() throws Exception {
        // Thread.start -> ThreadSubclass.run callback
        assertEdge(
                "me/n1ar4/cb/CallbackEntry", "threadStart", "()V",
                "me/n1ar4/cb/MyThread", "run", "()V",
                "callback",
                "thread_start"
        );

        // ExecutorService.submit(Runnable) -> Runnable.run callback
        assertEdge(
                "me/n1ar4/cb/CallbackEntry", "executorSubmit", "()V",
                "me/n1ar4/cb/MyRunnable", "run", "()V",
                "callback",
                "executor_runnable"
        );

        // AccessController.doPrivileged -> PrivilegedAction.run callback
        assertEdge(
                "me/n1ar4/cb/CallbackEntry", "doPrivileged", "()Ljava/lang/Object;",
                "me/n1ar4/cb/MyAction", "run", "()Ljava/lang/Object;",
                "callback",
                "doPrivileged"
        );

        // CompletableFuture.supplyAsync -> Supplier.get callback
        assertEdge(
                "me/n1ar4/cb/CallbackEntry", "completableSupply", "()V",
                "me/n1ar4/cb/MySupplier", "get", "()Ljava/lang/Object;",
                "callback",
                "completable_future_supplier"
        );

        // Proxy.newProxyInstance(...) -> InvocationHandler.invoke callback
        assertEdge(
                "me/n1ar4/cb/CallbackEntry", "dynamicProxy", "()V",
                "me/n1ar4/cb/MyInvocationHandler", "invoke",
                "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;",
                "callback",
                "dynamic_proxy"
        );

        // CGLIB Enhancer.create(...) -> MethodInterceptor.intercept callback
        assertEdge(
                "me/n1ar4/cb/CallbackEntry", "cglibProxy", "()V",
                "me/n1ar4/cb/MyCglibInterceptor", "intercept",
                "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;Lorg/springframework/cglib/proxy/MethodProxy;)Ljava/lang/Object;",
                "callback",
                "dynamic_proxy_cglib"
        );

        // Spring ProxyFactory.getProxy() -> AOP MethodInterceptor.invoke callback
        assertEdge(
                "me/n1ar4/cb/CallbackEntry", "springAopProxy", "()V",
                "me/n1ar4/cb/MySpringMethodInterceptor", "invoke",
                "(Lorg/aopalliance/intercept/MethodInvocation;)Ljava/lang/Object;",
                "callback",
                "dynamic_proxy_spring_aop"
        );

        // ByteBuddy builder usage -> interceptor method callback
        assertEdge(
                "me/n1ar4/cb/CallbackEntry", "byteBuddyProxy", "()V",
                "me/n1ar4/cb/MyByteBuddyInterceptor", "intercept",
                "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;",
                "callback",
                "dynamic_proxy_bytebuddy"
        );
    }

    @Test
    public void testReflectionEdgesExist() throws Exception {
        assertEdge(
                "me/n1ar4/cb/CallbackEntry", "reflectInvoke", "()V",
                "me/n1ar4/cb/ReflectionTarget", "target", "()V",
                "reflection",
                null
        );
    }

    @Test
    public void testReflectionLogEdgesExist() throws Exception {
        assertEdge(
                "me/n1ar4/cb/CallbackEntry", "reflectViaParams", "(Ljava/lang/String;Ljava/lang/String;)V",
                "me/n1ar4/cb/ReflectionTarget", "target", "()V",
                "reflection",
                "tamiflex_log:method.invoke"
        );
    }

    @Test
    public void testCallSiteQueryByEdgeWorks() {
        CoreEngine engine = EngineContext.getEngine();
        assertNotNull(engine);
        List<CallSiteEntity> sites = engine.getCallSitesByEdge(
                "me/n1ar4/cb/CallbackEntry", "threadStart", "()V",
                "me/n1ar4/cb/MyThread", "start", "()V");
        assertNotNull(sites);
        assertFalse(sites.isEmpty(), "callsite evidence should exist for direct Thread.start call");
    }

    @Test
    public void testDirectEdgeCarriesCallSiteKey() throws Exception {
        CoreEngine engine = EngineContext.getEngine();
        assertNotNull(engine);
        List<CallSiteEntity> sites = engine.getCallSitesByEdge(
                "me/n1ar4/cb/CallbackEntry",
                "threadStart",
                "()V",
                "me/n1ar4/cb/MyThread",
                "start",
                "()V");
        String edgeKey = null;
        if (sites != null) {
            for (CallSiteEntity site : sites) {
                if (site == null) {
                    continue;
                }
                String value = site.getCallSiteKey();
                if (value != null && !value.trim().isEmpty()) {
                    edgeKey = value;
                    break;
                }
            }
        }
        assertNotNull(edgeKey);
        assertFalse(edgeKey.trim().isEmpty(), "direct edge should contain call_site_key");

        int count = 0;
        for (CallSiteEntity item : DatabaseManager.getCallSites()) {
            if (item == null) {
                continue;
            }
            if (edgeKey.equals(item.getCallSiteKey())) {
                count++;
            }
        }
        assertTrue(count > 0, "edge call_site_key should link to at least one bytecode callsite");
    }

    private static void assertEdge(String callerClass,
                                   String callerMethod,
                                   String callerDesc,
                                   String calleeClass,
                                   String calleeMethod,
                                   String calleeDesc,
                                   String expectedType,
                                   String evidenceContains) throws Exception {
        EdgeRow row = selectEdge(callerClass, callerMethod, callerDesc,
                calleeClass, calleeMethod, calleeDesc);
        assertNotNull(row, "edge not found: " + callerClass + "." + callerMethod + " -> " + calleeClass + "." + calleeMethod);
        assertEquals(expectedType, row.type, "edge_type mismatch");
        if (evidenceContains != null) {
            assertNotNull(row.evidence);
            assertTrue(row.evidence.contains(evidenceContains),
                    "edge_evidence should contain: " + evidenceContains + " but was: " + row.evidence);
        }
    }

    private static EdgeRow selectEdge(String callerClass,
                                     String callerMethod,
                                     String callerDesc,
                                     String calleeClass,
                                     String calleeMethod,
                                     String calleeDesc) throws Exception {
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
                EdgeRow row = new EdgeRow();
                row.type = resolveEdgeType(edge);
                row.confidence = edge.getConfidence();
                row.evidence = edge.getEvidence();
                return row;
            }
        }
        return null;
    }

    private static final class EdgeRow {
        private String type;
        private String confidence;
        private String evidence;
    }

    private static void restoreProperty(String key, String oldValue) {
        if (oldValue == null) {
            System.clearProperty(key);
            return;
        }
        System.setProperty(key, oldValue);
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
