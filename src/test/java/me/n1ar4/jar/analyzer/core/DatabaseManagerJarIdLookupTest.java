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

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.JarEntity;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;
import me.n1ar4.jar.analyzer.entity.VulReportEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DatabaseManagerJarIdLookupTest {
    @BeforeEach
    @AfterEach
    public void resetDatabaseManager() {
        DatabaseManager.clearAllData();
    }

    @Test
    public void classFileLookupShouldMatchJarIdByValue() {
        ClassFileEntity row = new ClassFileEntity();
        row.setClassName("a/b/C");
        row.setJarId(200);
        row.setPath(Paths.get("a.class"));
        row.setPathStr("a.class");
        Set<ClassFileEntity> rows = new HashSet<>();
        rows.add(row);
        DatabaseManager.saveClassFiles(rows);

        assertNotNull(DatabaseManager.getClassFileByClass("a/b/C", Integer.valueOf(200)));
    }

    @Test
    public void classReferenceLookupShouldMatchJarIdByValue() {
        ClassReference ref = new ClassReference(
                "a/b/C",
                "java/lang/Object",
                List.of(),
                false,
                List.of(),
                new ArrayList<>(),
                "test.jar",
                300
        );
        DatabaseManager.saveClassInfo(Set.of(ref));

        assertNotNull(DatabaseManager.getClassReferenceByName("a/b/C", Integer.valueOf(300)));
    }

    @Test
    public void clearAllDataShouldResetJarIdCounter() {
        assertEquals(1, DatabaseManager.getJarId("/tmp/a.jar").getJid());

        DatabaseManager.clearAllData();

        assertEquals(1, DatabaseManager.getJarId("/tmp/b.jar").getJid());
    }

    @Test
    public void replaceJarsShouldRebuildSequentialIds() {
        DatabaseManager.replaceJars(List.of("/tmp/a.jar", "/tmp/b.jar", "/tmp/a.jar"));
        List<JarEntity> jars = DatabaseManager.getJarsMeta();

        assertEquals(2, jars.size());
        assertEquals(1, jars.get(0).getJid());
        assertEquals("/tmp/a.jar", jars.get(0).getJarAbsPath());
        assertEquals(2, jars.get(1).getJid());
        assertEquals("/tmp/b.jar", jars.get(1).getJarAbsPath());
    }

    @Test
    public void clearAllDataShouldResetVulIdCounter() {
        VulReportEntity first = new VulReportEntity();
        DatabaseManager.saveVulReport(first);
        assertEquals(1, first.getId());

        DatabaseManager.clearAllData();

        VulReportEntity second = new VulReportEntity();
        DatabaseManager.saveVulReport(second);
        assertEquals(1, second.getId());
    }

    @Test
    public void localVarLookupShouldFallbackToJarAgnosticKey() {
        LocalVarEntity row = new LocalVarEntity();
        row.setClassName("a/b/C");
        row.setMethodName("run");
        row.setMethodDesc("(Ljava/lang/String;)V");
        row.setJarId(300);
        row.setVarName("name");
        row.setVarDesc("Ljava/lang/String;");
        DatabaseManager.saveLocalVars(List.of(row));

        assertEquals(1, DatabaseManager.getLocalVarsByMethod("a/b/C", "run", "(Ljava/lang/String;)V").size());
    }
}
