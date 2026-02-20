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

import org.junit.jupiter.api.Tag;

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

@Tag("legacy-sqlite")
public class PtaOnTheFlySpringClosureTest {
    private static final String PROP_MODE = "jar.analyzer.callgraph.mode";
    private static final String PROP_EDGE_INFER = "jar.analyzer.edge.infer.enable";
    private static final String PROP_ONFLY = "jar.analyzer.pta.semantic.onfly.enable";
    private static final String PROP_INCREMENTAL = "jar.analyzer.pta.incremental";

    @Test
    public void shouldKeepSpringFrameworkSemanticsWhenPostEdgePipelineDisabled() throws Exception {
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

            Path jar = FixtureJars.springbootTestJar();
            WorkspaceContext.updateResolveInnerJars(false);
            CoreRunner.run(jar, null, false, false, true, null, true);

            int framework = queryCount("SELECT COUNT(*) FROM method_call_table WHERE edge_type='framework'");
            assertTrue(framework > 0, "framework semantic edges should be preserved by pta on-the-fly plugin");

            int webEntry = queryCount("SELECT COUNT(*) FROM method_call_table WHERE edge_type='framework' " +
                    "AND edge_evidence LIKE '%spring_web_entry%'");
            assertTrue(webEntry > 0, "spring web-entry semantics should remain available");

            int mainToController = queryCount("SELECT COUNT(*) FROM method_call_table WHERE edge_type='framework' " +
                    "AND caller_class_name='me/n1ar4/test/TestApplication' AND caller_method_name='main' " +
                    "AND callee_class_name='me/n1ar4/test/demos/web/DataController' AND callee_method_name='getStatus'");
            assertTrue(mainToController > 0, "main should connect to controller mapping under on-the-fly mode");
        } finally {
            ContextSensitivePtaEngine.clearIncrementalCache();
            restoreProperty(PROP_MODE, oldMode);
            restoreProperty(PROP_EDGE_INFER, oldInfer);
            restoreProperty(PROP_ONFLY, oldOnFly);
            restoreProperty(PROP_INCREMENTAL, oldIncremental);
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

    private static void restoreProperty(String key, String oldValue) {
        if (oldValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, oldValue);
        }
    }
}
