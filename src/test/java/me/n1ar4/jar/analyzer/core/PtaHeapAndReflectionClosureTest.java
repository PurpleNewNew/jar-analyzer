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
            WorkspaceContext.setResolveInnerJars(false);
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
        String sql = "SELECT COUNT(*) FROM method_call_table " +
                "WHERE caller_class_name='me/n1ar4/cb/CallbackEntry' AND caller_method_name=? AND caller_method_desc='()V' " +
                "AND callee_class_name='me/n1ar4/cb/Task' AND callee_method_name='run' AND callee_method_desc='()V' " +
                "AND edge_type='pta'";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + Const.dbFile);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, callerMethod);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static boolean hasDispatchEdge(String callerMethod, String calleeClassSimpleName) throws Exception {
        String sql = "SELECT COUNT(*) FROM method_call_table " +
                "WHERE caller_class_name='me/n1ar4/cb/CallbackEntry' AND caller_method_name=? AND caller_method_desc='()V' " +
                "AND callee_class_name=? AND callee_method_name='run' AND callee_method_desc='()V' " +
                "AND edge_type='dispatch'";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + Const.dbFile);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, callerMethod);
            stmt.setString(2, "me/n1ar4/cb/" + calleeClassSimpleName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static boolean hasReflectionEdge(String callerMethod) throws Exception {
        String sql = "SELECT COUNT(*) FROM method_call_table " +
                "WHERE caller_class_name='me/n1ar4/cb/CallbackEntry' AND caller_method_name=? " +
                "AND callee_class_name='me/n1ar4/cb/ReflectionTarget' AND callee_method_name='target' " +
                "AND callee_method_desc='()V' AND edge_type='reflection'";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + Const.dbFile);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, callerMethod);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static boolean hasMethodHandleEdge(String callerMethod) throws Exception {
        String sql = "SELECT COUNT(*) FROM method_call_table " +
                "WHERE caller_class_name='me/n1ar4/cb/CallbackEntry' AND caller_method_name=? " +
                "AND callee_class_name='me/n1ar4/cb/ReflectionTarget' AND callee_method_name='target' " +
                "AND callee_method_desc='()V' AND edge_type='method_handle'";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + Const.dbFile);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, callerMethod);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static void restoreProperty(String key, String old) {
        if (old == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, old);
        }
    }
}
