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
public class PtaSpringStabilityTest {
    private static final String MODE_PROP = "jar.analyzer.callgraph.mode";

    @Test
    public void shouldKeepSpringCoverageStableInPtaMode() throws Exception {
        String oldMode = System.getProperty(MODE_PROP);
        try {
            Path jar = FixtureJars.springbootTestJar();
            WorkspaceContext.updateResolveInnerJars(false);

            System.setProperty(MODE_PROP, "rta");
            CoreRunner.BuildResult rta = CoreRunner.run(jar, null, false, false, true, null, true);

            System.setProperty(MODE_PROP, "pta");
            CoreRunner.BuildResult pta = CoreRunner.run(jar, null, false, false, true, null, true);

            assertTrue(pta.getEdgeCount() >= rta.getEdgeCount(),
                    "pta mode should not regress edge coverage on spring sample");
            assertTrue(pta.getDispatchEdgesAdded() >= rta.getDispatchEdgesAdded(),
                    "pta mode should keep at least baseline dispatch coverage");
            assertTrue(pta.getPtaContextMethodsProcessed() > 0,
                    "pta mode should process context methods on spring sample");

            int framework = queryCount("SELECT COUNT(*) FROM method_call_table WHERE edge_type='framework'");
            assertTrue(framework > 0, "spring framework semantic edges should remain available");
        } finally {
            restoreProperty(oldMode);
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
            System.clearProperty(MODE_PROP);
        } else {
            System.setProperty(MODE_PROP, old);
        }
    }
}
