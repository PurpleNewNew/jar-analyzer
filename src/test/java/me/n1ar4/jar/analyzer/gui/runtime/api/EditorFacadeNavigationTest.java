/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.gui.runtime.model.EditorDocumentDto;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorFacadeNavigationTest {
    @Test
    void openMethodShouldKeepNavigationContextWhenEngineMissing() {
        EngineContext.setEngine(null);
        RuntimeFacades.editor().openMethod("a.b.C", "run", "()V", 1);
        EditorDocumentDto doc = RuntimeFacades.editor().current();
        assertEquals("a.b.C", doc.className());
        assertEquals("run", doc.methodName());
        assertEquals("()V", doc.methodDesc());
        assertTrue(doc.statusText().contains("method opened"));
    }

    @Test
    void openClassShouldFallbackWhenDbPathIsStale() throws Exception {
        Path jar = FixtureJars.springbootTestJar();
        CoreRunner.run(jar, null, false, true, true, null, true);

        ConfigFile config = new ConfigFile();
        config.setDbPath(Const.dbFile);
        CoreEngine engine = new CoreEngine(config);
        EngineContext.setEngine(engine);
        try {
            ClassKey key = pickAnyClassWithJarId();
            assertNotNull(key);
            breakClassPath(key);

            RuntimeFacades.editor().openClass(stripClassSuffix(key.className), key.jarId);
            EditorDocumentDto doc = RuntimeFacades.editor().current();
            assertNotNull(doc);
            assertTrue(doc.content() != null && !doc.content().isBlank());
            assertTrue(!doc.statusText().contains("not found"));
            assertTrue(!doc.statusText().contains("failed"));
        } finally {
            EngineContext.setEngine(null);
        }
    }

    private static ClassKey pickAnyClassWithJarId() throws Exception {
        String sql = "SELECT class_name, jar_id FROM class_file_table " +
                "WHERE jar_id > 0 AND class_name NOT LIKE 'module-info%' " +
                "ORDER BY cf_id ASC LIMIT 1";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + Const.dbFile);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            String className = rs.getString(1);
            int jarId = rs.getInt(2);
            return new ClassKey(className, jarId);
        }
    }

    private static void breakClassPath(ClassKey key) throws Exception {
        String badPath = "C:/__jar_analyzer_stale__/missing.class";
        String sql = "UPDATE class_file_table SET path_str = ? WHERE class_name = ? AND jar_id = ?";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + Const.dbFile);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, badPath);
            stmt.setString(2, key.className);
            stmt.setInt(3, key.jarId);
            stmt.executeUpdate();
        }
    }

    private static String stripClassSuffix(String className) {
        if (className == null) {
            return "";
        }
        String value = className.trim();
        if (value.endsWith(".class")) {
            return value.substring(0, value.length() - 6);
        }
        return value;
    }

    private static final class ClassKey {
        private final String className;
        private final int jarId;

        private ClassKey(String className, int jarId) {
            this.className = className;
            this.jarId = jarId;
        }
    }
}
