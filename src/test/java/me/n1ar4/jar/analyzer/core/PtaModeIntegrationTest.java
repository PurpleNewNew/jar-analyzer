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

public class PtaModeIntegrationTest {
    private static final String PROP = "jar.analyzer.callgraph.mode";

    @Test
    public void shouldGeneratePtaEdges() throws Exception {
        String old = System.getProperty(PROP);
        try {
            ContextSensitivePtaEngine.clearIncrementalCache();
            System.setProperty(PROP, "pta");
            Path jar = FixtureJars.callbackTestJar();
            WorkspaceContext.updateResolveInnerJars(false);
            CoreRunner.run(jar, null, false, false, true, null, true);

            int ptaCount = queryCount("SELECT COUNT(*) FROM method_call_table WHERE edge_type='pta'");
            assertTrue(ptaCount > 0, "pta mode should generate pta edges");

            int ctxEvidence = queryCount("SELECT COUNT(*) FROM method_call_table WHERE edge_evidence LIKE '%pta_ctx=%'");
            assertTrue(ctxEvidence > 0, "pta mode should preserve context evidence");
        } finally {
            ContextSensitivePtaEngine.clearIncrementalCache();
            restoreProperty(old);
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
}
