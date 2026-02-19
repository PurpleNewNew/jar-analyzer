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

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.pta.ContextSensitivePtaEngine;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.semantic.CallResolver;
import me.n1ar4.jar.analyzer.semantic.TypeSolver;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SemanticDisambiguationRegressionTest {
    private static final String PROP_MODE = "jar.analyzer.callgraph.mode";
    private static final String PROP_EDGE_INFER = "jar.analyzer.edge.infer.enable";
    private static final String PROP_ONFLY = "jar.analyzer.pta.semantic.onfly.enable";
    private static final String PROP_INCREMENTAL = "jar.analyzer.pta.incremental";

    @Test
    public void shouldKeepProxyReflectionAndMethodHandleEvidence() throws Exception {
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

            assertTrue(edgeCountByEvidenceLike("%dynamic_proxy_jdk%") > 0,
                    "missing JDK proxy evidence");
            assertTrue(edgeCountByEvidenceLike("%dynamic_proxy_cglib%") > 0,
                    "missing CGLIB proxy evidence");
            assertTrue(edgeCountByEvidenceLike("%dynamic_proxy_bytebuddy%") > 0,
                    "missing ByteBuddy proxy evidence");
            assertTrue(edgeCountByEvidenceLike("%dynamic_proxy_spring_aop%") > 0,
                    "missing Spring AOP proxy evidence");
            assertTrue(edgeCountByEvidenceLike("%pta_reflection%") > 0,
                    "missing PTA reflection evidence");
            assertTrue(edgeCountByEvidenceLike("%pta_method_handle%") > 0,
                    "missing PTA method-handle evidence");
            assertTrue(edgeCountByType("callback") >= 8,
                    "callback edge count regressed");
            assertTrue(edgeCountByType("reflection") >= 6,
                    "reflection edge count regressed");
            assertTrue(edgeCountByType("method_handle") >= 3,
                    "method_handle edge count regressed");
        } finally {
            ContextSensitivePtaEngine.clearIncrementalCache();
            restoreProperty(PROP_MODE, oldMode);
            restoreProperty(PROP_EDGE_INFER, oldInfer);
            restoreProperty(PROP_ONFLY, oldOnFly);
            restoreProperty(PROP_INCREMENTAL, oldIncremental);
        }
    }

    @Test
    public void shouldPreferNonBridgeDescriptorAndKeepOverrideEdges() throws Exception {
        Path jar = FixtureJars.springbootTestJar();
        WorkspaceContext.updateResolveInnerJars(false);
        CoreRunner.run(jar, null, false, false, true, null, true);

        assertTrue(queryCount(
                "SELECT COUNT(*) FROM method_table WHERE (access & 64) != 0") > 0,
                "fixture should contain bridge methods");
        assertTrue(edgeCountByType("override") > 0,
                "override edges should exist");

        ConfigFile config = new ConfigFile();
        config.setDbPath(Const.dbFile);
        CoreEngine engine = new CoreEngine(config);
        TypeSolver typeSolver = new TypeSolver(engine);
        CallResolver resolver = typeSolver.getCallResolver();

        String owner = "org/springframework/boot/loader/jar/CentralDirectoryFileHeader";
        String desc = resolver.resolveMethodDescByArgCount(owner, "clone", 0);
        assertEquals("()Lorg/springframework/boot/loader/jar/CentralDirectoryFileHeader;", desc,
                "bridge-aware selection should prefer non-bridge clone descriptor");
    }

    private static int edgeCountByType(String edgeType) throws Exception {
        String sql = "SELECT COUNT(*) FROM method_call_table WHERE edge_type=?";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + Const.dbFile);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, edgeType);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static int edgeCountByEvidenceLike(String evidenceLike) throws Exception {
        String sql = "SELECT COUNT(*) FROM method_call_table WHERE edge_evidence LIKE ?";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + Const.dbFile);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, evidenceLike);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static int queryCount(String sql) throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + Const.dbFile);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void restoreProperty(String key, String oldValue) {
        if (oldValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, oldValue);
        }
    }
}
