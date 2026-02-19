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
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
        String sql = "SELECT COUNT(*) FROM method_call_table " +
                "WHERE caller_class_name='me/n1ar4/cb/CallbackEntry' " +
                "AND caller_method_name=? AND caller_method_desc=? " +
                "AND callee_class_name=? AND callee_method_name=? AND callee_method_desc=? " +
                "AND edge_type='callback' " +
                "AND edge_evidence LIKE ?";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + Const.dbFile);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, callerMethod);
            stmt.setString(2, callerDesc);
            stmt.setString(3, calleeClass);
            stmt.setString(4, calleeMethod);
            stmt.setString(5, calleeDesc);
            stmt.setString(6, "%" + evidenceNeedle + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next() && rs.getInt(1) > 0,
                        "missing callback edge: " + callerMethod + " -> " + calleeClass + "." + calleeMethod);
            }
        }
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
        String sql = "SELECT COUNT(*) FROM method_call_table " +
                "WHERE caller_class_name='me/n1ar4/cb/CallbackEntry' " +
                "AND caller_method_name=? AND caller_method_desc=? " +
                "AND callee_class_name=? AND callee_method_name=? AND callee_method_desc=? " +
                "AND edge_type='reflection' " +
                "AND edge_evidence LIKE ?";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + Const.dbFile);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, callerMethod);
            stmt.setString(2, callerDesc);
            stmt.setString(3, calleeClass);
            stmt.setString(4, calleeMethod);
            stmt.setString(5, calleeDesc);
            stmt.setString(6, "%" + evidenceNeedle + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next() && rs.getInt(1) > 0,
                        "missing reflection edge: " + callerMethod + " -> " + calleeClass + "." + calleeMethod);
            }
        }
    }

    private static void assertMethodHandleEdge(String callerMethod,
                                               String callerDesc,
                                               String calleeClass,
                                               String calleeMethod,
                                               String calleeDesc,
                                               String evidenceNeedle) throws Exception {
        String sql = "SELECT COUNT(*) FROM method_call_table " +
                "WHERE caller_class_name='me/n1ar4/cb/CallbackEntry' " +
                "AND caller_method_name=? AND caller_method_desc=? " +
                "AND callee_class_name=? AND callee_method_name=? AND callee_method_desc=? " +
                "AND edge_type='method_handle' " +
                "AND edge_evidence LIKE ?";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + Const.dbFile);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, callerMethod);
            stmt.setString(2, callerDesc);
            stmt.setString(3, calleeClass);
            stmt.setString(4, calleeMethod);
            stmt.setString(5, calleeDesc);
            stmt.setString(6, "%" + evidenceNeedle + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next() && rs.getInt(1) > 0,
                        "missing method_handle edge: " + callerMethod + " -> " + calleeClass + "." + calleeMethod);
            }
        }
    }
}
