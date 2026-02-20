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

import org.junit.jupiter.api.Tag;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("legacy-sqlite")
public class CallbackEdgeInferenceTest {
    private static final String DB_PATH = Const.dbFile;
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
        config.setDbPath(DB_PATH);
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
        String keySql = "SELECT call_site_key FROM method_call_table " +
                "WHERE caller_class_name=? AND caller_method_name=? AND caller_method_desc=? " +
                "AND callee_class_name=? AND callee_method_name=? AND callee_method_desc=? " +
                "AND edge_type='direct' LIMIT 1";
        String edgeKey = null;
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
             PreparedStatement stmt = conn.prepareStatement(keySql)) {
            stmt.setString(1, "me/n1ar4/cb/CallbackEntry");
            stmt.setString(2, "threadStart");
            stmt.setString(3, "()V");
            stmt.setString(4, "me/n1ar4/cb/MyThread");
            stmt.setString(5, "start");
            stmt.setString(6, "()V");
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    edgeKey = rs.getString(1);
                }
            }
        }
        assertNotNull(edgeKey);
        assertFalse(edgeKey.trim().isEmpty(), "direct edge should contain call_site_key");

        String checkSql = "SELECT COUNT(*) FROM bytecode_call_site_table WHERE call_site_key = ?";
        int count = 0;
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
             PreparedStatement stmt = conn.prepareStatement(checkSql)) {
            stmt.setString(1, edgeKey);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
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
        String sql = "SELECT edge_type, edge_confidence, edge_evidence " +
                "FROM method_call_table " +
                "WHERE caller_class_name=? AND caller_method_name=? AND caller_method_desc=? " +
                "AND callee_class_name=? AND callee_method_name=? AND callee_method_desc=? " +
                "LIMIT 1";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, callerClass);
            stmt.setString(2, callerMethod);
            stmt.setString(3, callerDesc);
            stmt.setString(4, calleeClass);
            stmt.setString(5, calleeMethod);
            stmt.setString(6, calleeDesc);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                EdgeRow row = new EdgeRow();
                row.type = rs.getString(1);
                row.confidence = rs.getString(2);
                row.evidence = rs.getString(3);
                return row;
            }
        }
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
}
