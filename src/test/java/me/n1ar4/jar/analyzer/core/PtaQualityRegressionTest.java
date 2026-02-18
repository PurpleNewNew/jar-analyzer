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
        WorkspaceContext.setResolveInnerJars(false);
        CoreRunner.run(jar, null, false, false, true, null, true);

        int matched = 0;
        for (ExpectedEdge edge : EXPECTED) {
            if (edgeExists(edge)) {
                matched++;
            }
        }
        int total = queryCount("SELECT COUNT(*) FROM method_call_table");
        int low = queryCount("SELECT COUNT(*) FROM method_call_table WHERE edge_confidence='low'");
        int ptaType = queryCount("SELECT COUNT(*) FROM method_call_table WHERE edge_type='pta'");
        int ptaEvidence = queryCount("SELECT COUNT(*) FROM method_call_table WHERE edge_evidence LIKE '%pta_ctx=%'");
        return new ModeMetrics(mode, matched, total, low, ptaType, ptaEvidence);
    }

    private static boolean edgeExists(ExpectedEdge edge) throws Exception {
        String sql = "SELECT COUNT(*) FROM method_call_table " +
                "WHERE caller_class_name=? AND caller_method_name=? AND caller_method_desc=? " +
                "AND callee_class_name=? AND callee_method_name=? AND callee_method_desc=?";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + Const.dbFile);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, edge.callerClass);
            stmt.setString(2, edge.callerMethod);
            stmt.setString(3, edge.callerDesc);
            stmt.setString(4, edge.calleeClass);
            stmt.setString(5, edge.calleeMethod);
            stmt.setString(6, edge.calleeDesc);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static boolean edgeExistsWithType(String callerClass,
                                              String callerMethod,
                                              String callerDesc,
                                              String calleeClass,
                                              String calleeMethod,
                                              String calleeDesc,
                                              String edgeType) throws Exception {
        String sql = "SELECT COUNT(*) FROM method_call_table " +
                "WHERE caller_class_name=? AND caller_method_name=? AND caller_method_desc=? " +
                "AND callee_class_name=? AND callee_method_name=? AND callee_method_desc=? " +
                "AND edge_type=?";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + Const.dbFile);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, callerClass);
            stmt.setString(2, callerMethod);
            stmt.setString(3, callerDesc);
            stmt.setString(4, calleeClass);
            stmt.setString(5, calleeMethod);
            stmt.setString(6, calleeDesc);
            stmt.setString(7, edgeType);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static int queryCount(String sql) throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + Const.dbFile);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private static void restoreProperty(String old) {
        if (old == null) {
            System.clearProperty(PROP);
        } else {
            System.setProperty(PROP, old);
        }
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
