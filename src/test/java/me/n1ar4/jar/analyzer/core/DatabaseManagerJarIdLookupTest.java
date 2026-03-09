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
import me.n1ar4.jar.analyzer.engine.ClassLookupService;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.JarEntity;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;
import me.n1ar4.jar.analyzer.entity.VulReportEntity;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.ClassIndex;
import me.n1ar4.jar.analyzer.utils.DeferredFileWriter;
import me.n1ar4.jar.analyzer.utils.JarUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseManagerJarIdLookupTest {
    @BeforeEach
    @AfterEach
    public void resetDatabaseManager() throws Exception {
        DeferredFileWriter.awaitAndStop();
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
    }

    @Test
    public void classFileLookupShouldMatchJarIdByValue() {
        ClassFileEntity row = new ClassFileEntity();
        row.setClassName("a/b/C");
        row.setJarId(200);
        row.setPath(Paths.get("a.class"));
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

    @Test
    public void snapshotShouldPersistClassPathFromSinglePathSource() throws Exception {
        Path classPath = Files.createTempFile("jar-analyzer-snapshot-", ".class")
                .toAbsolutePath()
                .normalize();
        ClassFileEntity row = classFileWithPath("demo/PathSnapshot", classPath, 7);

        ProjectRuntimeSnapshot snapshot = DatabaseManager.buildProjectRuntimeSnapshot(
                9L,
                null,
                List.of(),
                Set.of(row),
                Set.of(),
                Set.of(),
                java.util.Map.of(),
                java.util.Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        DatabaseManager.restoreProjectRuntime(snapshot);

        ClassFileEntity restored = DatabaseManager.getClassFileByClass("demo/PathSnapshot", 7);
        assertNotNull(restored);
        assertEquals(classPath, restored.getPath());
        assertEquals(classPath.toString(), restored.getPathStr());
    }

    @Test
    public void classIndexShouldResolveClassFileFromSinglePathSource() throws Exception {
        Path classPath = Files.createTempFile("jar-analyzer-index-", ".class")
                .toAbsolutePath()
                .normalize();
        ClassFileEntity row = classFileWithPath("demo/PathIndex", classPath, 11);

        DatabaseManager.saveClassFiles(Set.of(row));
        ClassIndex.refresh();

        assertEquals(classPath, ClassIndex.resolveClassFile("demo/PathIndex", 11));
    }

    @Test
    public void rawClassResolveShouldUseWorkspaceCopyPath() throws Exception {
        Path sourceClass = Path.of(DatabaseManagerJarIdLookupTest.class
                .getResource("DatabaseManagerJarIdLookupTest.class")
                .toURI())
                .toAbsolutePath()
                .normalize();
        ProjectRuntimeContext.setProjectModel(ProjectModel.artifact(
                Path.of("/tmp/unrelated-root.jar"),
                null,
                List.of(),
                false
        ));

        JarUtil.ResolveResult result = JarUtil.resolveNormalJarFile(sourceClass.toString(), 13, false);
        DeferredFileWriter.awaitAndStop();

        assertEquals(1, result.getClassFiles().size());
        ClassFileEntity resolved = result.getClassFiles().iterator().next();
        assertNotNull(resolved.getPath());
        assertEquals(resolved.getPath().toString(), resolved.getPathStr());
        assertNotEquals(sourceClass, resolved.getPath());
        assertTrue(resolved.getPath().startsWith(Path.of(Const.tempDir).toAbsolutePath().normalize()));
        assertTrue(Files.exists(resolved.getPath()));
    }

    @Test
    public void classIndexShouldInvalidateAcrossProjectRuntimeSwitchAtSameBuildSeq() throws Exception {
        Path firstClass = writeDummyClassFile("jar-analyzer-index-first");
        ProjectRuntimeContext.restoreProjectRuntime(
                "project-a",
                0L,
                ProjectModel.artifact(firstClass, null, List.of(firstClass), false)
        );
        DatabaseManager.saveClassFiles(Set.of(classFileWithPath("demo/Shared", firstClass, 21)));
        ClassIndex.refresh();
        assertEquals(firstClass, ClassIndex.resolveClassFile("demo/Shared", 21));

        DatabaseManager.clearAllData();
        Path secondClass = writeDummyClassFile("jar-analyzer-index-second");
        ProjectRuntimeContext.restoreProjectRuntime(
                "project-b",
                0L,
                ProjectModel.artifact(secondClass, null, List.of(secondClass), false)
        );
        DatabaseManager.saveClassFiles(Set.of(classFileWithPath("demo/Shared", secondClass, 21)));

        assertEquals(secondClass, ClassIndex.resolveClassFile("demo/Shared", 21));
    }

    @Test
    public void classLookupShouldInvalidatePositiveCacheAcrossProjectRuntimeSwitchAtSameBuildSeq() throws Exception {
        Path firstClass = writeDummyClassFile("jar-analyzer-lookup-first");
        ProjectRuntimeContext.restoreProjectRuntime(
                "lookup-project-a",
                0L,
                ProjectModel.artifact(firstClass, null, List.of(firstClass), false)
        );
        DatabaseManager.saveClassFiles(Set.of(classFileWithPath("demo/LookupShared", firstClass, 31)));

        ClassLookupService.LookupResult first = ClassLookupService.findClass("demo/LookupShared", 31);
        assertNotNull(first);
        assertEquals(firstClass.toString(), first.getExternalPath());

        DatabaseManager.clearAllData();
        Path secondClass = writeDummyClassFile("jar-analyzer-lookup-second");
        ProjectRuntimeContext.restoreProjectRuntime(
                "lookup-project-b",
                0L,
                ProjectModel.artifact(secondClass, null, List.of(secondClass), false)
        );
        DatabaseManager.saveClassFiles(Set.of(classFileWithPath("demo/LookupShared", secondClass, 31)));

        ClassLookupService.LookupResult second = ClassLookupService.findClass("demo/LookupShared", 31);
        assertNotNull(second);
        assertEquals(secondClass.toString(), second.getExternalPath());
        assertNotEquals(first.getExternalPath(), second.getExternalPath());
    }

    private static ClassFileEntity classFileWithPath(String className, Path classPath, int jarId) {
        ClassFileEntity entity = new ClassFileEntity();
        entity.setClassName(className);
        entity.setJarId(jarId);
        entity.setPath(classPath);
        return entity;
    }

    private static Path writeDummyClassFile(String prefix) throws Exception {
        Path file = Files.createTempFile(prefix, ".class").toAbsolutePath().normalize();
        Files.write(file, new byte[]{1, 2, 3, 4});
        return file;
    }
}
