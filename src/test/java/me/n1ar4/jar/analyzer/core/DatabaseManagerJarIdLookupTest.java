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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
}

