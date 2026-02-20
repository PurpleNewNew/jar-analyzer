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

import me.n1ar4.jar.analyzer.core.CoreRunner;
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
public class SpringFrameworkEdgeInferenceTest {
    @Test
    public void shouldInferSpringFrameworkEdges() throws Exception {
        Path jar = FixtureJars.springbootTestJar();
        WorkspaceContext.updateResolveInnerJars(false);
        CoreRunner.run(jar, null, false, false, true, null, true);

        int frameworkCount = queryCount(
                "SELECT COUNT(*) FROM method_call_table WHERE edge_type='framework'");
        assertTrue(frameworkCount > 0, "framework rule should add edges");

        int webEntryCount = queryCount(
                "SELECT COUNT(*) FROM method_call_table WHERE edge_type='framework' AND edge_evidence LIKE '%spring_web_entry%'");
        assertTrue(webEntryCount > 0, "framework rule should infer spring web entry edges");

        int mainToController = queryCount(
                "SELECT COUNT(*) FROM method_call_table WHERE edge_type='framework' " +
                        "AND caller_class_name='me/n1ar4/test/TestApplication' AND caller_method_name='main' " +
                        "AND callee_class_name='me/n1ar4/test/demos/web/DataController' AND callee_method_name='getStatus'");
        assertTrue(mainToController > 0, "main should connect to controller mapping method");
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
}
