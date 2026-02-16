/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.headless;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.dfs.DFSEngine;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.dfs.DfsOutputs;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.DecompileEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.taint.TaintAnalyzer;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class HeadlessSmokeTest {
    private static final String DB_PATH = Const.dbFile;

    @Test
    @SuppressWarnings("all")
    public void testHeadlessPipelineDoesNotThrow() {
        try {
            System.setProperty("java.awt.headless", "true");
            System.setProperty("jar.analyzer.taint.summary.enable", "false");

            Path file = FixtureJars.springbootTestJar();
            WorkspaceContext.setResolveInnerJars(false);
            CoreRunner.run(file, null, false, true, true, null, true);

            ConfigFile config = new ConfigFile();
            config.setDbPath(DB_PATH);
            CoreEngine engine = new CoreEngine(config);
            EngineContext.setEngine(engine);

            Edge edge = pickCalleeWithSingleCaller();
            assertNotNull(edge);

            DFSEngine dfs = new DFSEngine(DfsOutputs.noop(), true, false, 2);
            dfs.setTimeoutMs(15_000);
            dfs.setMaxLimit(1);
            dfs.setSink(edge.calleeClassName, edge.calleeMethodName, edge.calleeMethodDesc);
            dfs.setSource(edge.callerClassName, edge.callerMethodName, edge.callerMethodDesc);
            dfs.doAnalyze();
            List<DFSResult> results = dfs.getResults();

            List<TaintResult> taint = TaintAnalyzer.analyze(results, 15_000, 1, new AtomicBoolean(false));
            if (taint == null) {
                throw new IllegalStateException("taint result is null");
            }

            // Decompile a concrete class file path; should not trigger any AWT/Swing in headless mode.
            String absPath = engine.getAbsPath(edge.calleeClassName);
            assertNotNull(absPath);
            Path classFile = Paths.get(absPath);
            if (Files.exists(classFile)) {
                DecompileEngine.decompile(classFile);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            fail("headless smoke test failed: " + t);
        }
    }

    private static Edge pickCalleeWithSingleCaller() {
        String sql = "SELECT\n" +
                "    mc.callee_class_name,\n" +
                "    mc.callee_method_name,\n" +
                "    mc.callee_method_desc,\n" +
                "    MIN(mc.caller_class_name) AS caller_class_name,\n" +
                "    MIN(mc.caller_method_name) AS caller_method_name,\n" +
                "    MIN(mc.caller_method_desc) AS caller_method_desc\n" +
                "FROM method_call_table mc\n" +
                "JOIN class_file_table cf ON cf.class_name = mc.callee_class_name || '.class'\n" +
                "GROUP BY mc.callee_class_name, mc.callee_method_name, mc.callee_method_desc\n" +
                "HAVING COUNT(DISTINCT mc.caller_class_name || '|' || mc.caller_method_name || '|' || mc.caller_method_desc) = 1\n" +
                "ORDER BY MIN(mc.callee_jar_id) ASC, mc.callee_class_name ASC, mc.callee_method_name ASC, mc.callee_method_desc ASC\n" +
                "LIMIT 1";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            Edge edge = new Edge();
            edge.calleeClassName = rs.getString(1);
            edge.calleeMethodName = rs.getString(2);
            edge.calleeMethodDesc = rs.getString(3);
            edge.callerClassName = rs.getString(4);
            edge.callerMethodName = rs.getString(5);
            edge.callerMethodDesc = rs.getString(6);
            return edge;
        } catch (Exception e) {
            return null;
        }
    }

    private static final class Edge {
        private String callerClassName;
        private String callerMethodName;
        private String callerMethodDesc;
        private String calleeClassName;
        private String calleeMethodName;
        private String calleeMethodDesc;
    }
}
