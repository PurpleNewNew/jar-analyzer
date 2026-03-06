/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.storage.neo4j;

import me.n1ar4.jar.analyzer.core.BuildSeqUtil;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProjectContextModelTest {
    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
        WorkspaceContext.clear();
        EngineContext.setEngine(null);
    }

    @Test
    public void activeProjectShouldBeTemporaryByDefault() {
        String active = ActiveProjectContext.getActiveProjectKey();
        assertTrue(ActiveProjectContext.isTemporaryProjectKey(active));
        assertEquals(active, ActiveProjectContext.resolveRequestedOrActive(""));
        assertEquals("", ActiveProjectContext.normalizeProjectKey(null));
    }

    @Test
    public void projectStoreShouldSplitTempAndPersistentHomes() {
        Neo4jProjectStore store = Neo4jProjectStore.getInstance();
        Path tempHome = store.resolveProjectHome(ActiveProjectContext.temporaryProjectKey());
        Path persistentHome = store.resolveProjectHome("persist-key-1");

        String sep = File.separator;
        assertTrue(tempHome.toString().contains(sep + "neo4j-temp" + sep + ActiveProjectContext.temporarySessionId()));
        assertTrue(persistentHome.toString().contains(sep + "neo4j-projects" + sep + "persist-key-1"));
    }

    @Test
    public void projectSwitchAndRemoveShouldBeBlockedWhileBuilding() {
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        ProjectRegistryEntry created = service.createProject("build-lock-test");
        try {
            service.activateTemporaryProject();
            DatabaseManager.setBuilding(true);

            assertEquals("project_build_in_progress",
                    assertThrows(IllegalStateException.class,
                            () -> service.switchActive(created.projectKey())).getMessage());
            assertEquals("project_build_in_progress",
                    assertThrows(IllegalStateException.class,
                            () -> service.remove(created.projectKey(), true)).getMessage());
        } finally {
            DatabaseManager.setBuilding(false);
            service.activateTemporaryProject();
            service.remove(created.projectKey(), true);
        }
    }

    @Test
    public void switchActiveShouldRestoreRuntimeAndEngine() {
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        ProjectRegistryEntry created = service.createProject("switch-restore-test");
        ProjectMetadataSnapshotStore metadataStore = ProjectMetadataSnapshotStore.getInstance();
        try {
            DatabaseManager.runAtomicUpdate(() -> {
                DatabaseManager.saveProjectModel(ProjectModel.artifact(
                        Path.of("/tmp/jar-analyzer/switch-restore.jar"),
                        null,
                        List.of(Path.of("/tmp/jar-analyzer/switch-restore.jar")),
                        false
                ));
                DatabaseManager.markProjectBuildReady(77L);
            });
            metadataStore.write(created.projectKey(), DatabaseManager.snapshotProjectRuntime());

            service.activateTemporaryProject();
            DatabaseManager.clearAllData();
            EngineContext.setEngine(null);

            service.switchActive(created.projectKey());

            assertEquals(created.projectKey(), ActiveProjectContext.getActiveProjectKey());
            assertEquals(77L, DatabaseManager.getProjectBuildSeq());
            assertNotNull(DatabaseManager.getProjectModel());
            assertNotNull(EngineContext.getEngine());
        } finally {
            service.activateTemporaryProject();
            service.remove(created.projectKey(), true);
        }
    }

    @Test
    public void switchActiveShouldClearRuntimeWhenNextProjectHasNoSnapshot() {
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        ProjectRegistryEntry created = service.createProject("empty-runtime-test");
        try {
            service.activateTemporaryProject();
            DatabaseManager.runAtomicUpdate(() -> {
                DatabaseManager.saveMethods(Set.of(methodRef("demo/OldController")));
                DatabaseManager.markProjectBuildReady(19L);
            });

            service.switchActive(created.projectKey());

            assertEquals(created.projectKey(), ActiveProjectContext.getActiveProjectKey());
            assertTrue(DatabaseManager.getMethodReferences().isEmpty());
            assertEquals(0L, DatabaseManager.getProjectBuildSeq());
            assertNotNull(EngineContext.getEngine());
            assertFalse(EngineContext.getEngine().isEnabled());
        } finally {
            service.activateTemporaryProject();
            service.remove(created.projectKey(), true);
        }
    }

    @Test
    public void removeActiveProjectShouldRestoreNextProjectBeforeDeletingStore() {
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        ProjectMetadataSnapshotStore metadataStore = ProjectMetadataSnapshotStore.getInstance();
        ProjectRegistryEntry first = service.createProject("remove-active-first");
        ProjectRegistryEntry second = service.createProject("remove-active-second");
        Path firstHome = Neo4jProjectStore.getInstance().resolveProjectHome(first.projectKey());
        try {
            metadataStore.write(first.projectKey(), snapshotFor("demo/FirstController", 101L));
            metadataStore.write(second.projectKey(), snapshotFor("demo/SecondController", 202L));

            service.switchActive(first.projectKey());
            assertEquals(101L, DatabaseManager.getProjectBuildSeq());

            assertTrue(service.remove(first.projectKey(), true));

            assertEquals(second.projectKey(), ActiveProjectContext.getActiveProjectKey());
            assertEquals(202L, DatabaseManager.getProjectBuildSeq());
            assertEquals(1, DatabaseManager.getMethodReferences().size());
            assertEquals("demo/SecondController",
                    DatabaseManager.getMethodReferences().get(0).getClassReference().getName());
            assertFalse(Files.exists(firstHome));
        } finally {
            service.activateTemporaryProject();
            service.remove(first.projectKey(), true);
            service.remove(second.projectKey(), true);
        }
    }

    @Test
    public void cleanupTemporaryProjectShouldClearRuntimeWhenTemporaryProjectIsActive() {
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        service.activateTemporaryProject();
        DatabaseManager.runAtomicUpdate(() -> {
            DatabaseManager.saveMethods(Set.of(methodRef("demo/TempController")));
            DatabaseManager.markProjectBuildReady(33L);
        });
        String projectKey = ActiveProjectContext.getActiveProjectKey();
        long snapshot = BuildSeqUtil.projectSnapshot(projectKey);

        service.cleanupTemporaryProject();

        assertTrue(ActiveProjectContext.isTemporaryProjectKey(ActiveProjectContext.getActiveProjectKey()));
        assertTrue(DatabaseManager.getMethodReferences().isEmpty());
        assertEquals(0L, DatabaseManager.getProjectBuildSeq());
        assertTrue(BuildSeqUtil.isProjectStale(projectKey, snapshot));
    }

    @Test
    public void failedBuildShouldKeepProjectUnreadableAfterSwitchBack() {
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        ProjectMetadataSnapshotStore metadataStore = ProjectMetadataSnapshotStore.getInstance();
        ProjectRegistryEntry created = service.createProject("failed-build-project");
        try {
            metadataStore.write(created.projectKey(), snapshotFor("demo/FailedBuildController", 303L));

            service.activateTemporaryProject();
            DatabaseManager.clearAllData();
            EngineContext.setEngine(null);
            service.switchActive(created.projectKey());
            assertEquals(303L, DatabaseManager.getProjectBuildSeq());
            assertFalse(DatabaseManager.getMethodReferences().isEmpty());

            synchronized (ActiveProjectContext.mutationLock()) {
                DatabaseManager.beginBuild(created.projectKey());
            }
            DatabaseManager.setBuilding(false);
            assertTrue(metadataStore.isUnavailable(created.projectKey()));

            service.activateTemporaryProject();
            service.switchActive(created.projectKey());

            assertEquals(created.projectKey(), ActiveProjectContext.getActiveProjectKey());
            assertTrue(DatabaseManager.getMethodReferences().isEmpty());
            assertEquals(0L, DatabaseManager.getProjectBuildSeq());
            assertFalse(DatabaseManager.isProjectReady());
            assertNotNull(EngineContext.getEngine());
            assertFalse(EngineContext.getEngine().isEnabled());
        } finally {
            DatabaseManager.setBuilding(false);
            service.activateTemporaryProject();
            service.remove(created.projectKey(), true);
        }
    }

    @Test
    public void loadShouldNotOverwriteMalformedRegistry() throws Exception {
        Path registry = Path.of(".jar-analyzer-projects.json").toAbsolutePath().normalize();
        byte[] backup = Files.exists(registry) ? Files.readAllBytes(registry) : null;
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        ProjectRegistryEntry created = service.createProject("registry-corrupt-test");
        String malformed = "{invalid-json";
        try {
            Files.writeString(registry, malformed, StandardCharsets.UTF_8);
            invokeLoad(service);

            assertEquals(malformed, Files.readString(registry, StandardCharsets.UTF_8));
            assertTrue(service.list().stream().anyMatch(entry -> entry.projectKey().equals(created.projectKey())));
        } finally {
            if (backup == null) {
                Files.deleteIfExists(registry);
            } else {
                Files.write(registry, backup);
            }
            invokeLoad(service);
            Neo4jProjectStore.getInstance().deleteProjectStore(created.projectKey());
        }
    }

    @Test
    public void projectSnapshotShouldIgnoreUnrelatedActiveProjectChanges() {
        String originalKey = ActiveProjectContext.getActiveProjectKey();
        String originalAlias = ActiveProjectContext.getActiveProjectAlias();
        String otherProjectKey = "build-snapshot-" + Long.toHexString(System.nanoTime());
        try {
            DatabaseManager.runAtomicUpdate(() -> DatabaseManager.markProjectBuildReady(11L));

            long snapshot = BuildSeqUtil.projectSnapshot(originalKey);
            ActiveProjectContext.setActiveProject(otherProjectKey, otherProjectKey);

            assertFalse(BuildSeqUtil.isProjectStale(originalKey, snapshot));
        } finally {
            ActiveProjectContext.setActiveProject(originalKey, originalAlias);
            DatabaseManager.clearAllData();
        }
    }

    @Test
    public void projectSnapshotShouldNotOpenProjectStore() throws Exception {
        String projectKey = "snapshot-read-" + Long.toHexString(System.nanoTime());
        Neo4jProjectStore store = Neo4jProjectStore.getInstance();
        ProjectMetadataSnapshotStore.getInstance().write(projectKey, snapshotFor("demo/SnapshotReadController", 88L));
        store.closeProject(projectKey);
        try {
            assertFalse(isProjectOpen(store, projectKey));
            assertEquals(88L, BuildSeqUtil.projectSnapshot(projectKey));
            assertFalse(isProjectOpen(store, projectKey));
        } finally {
            store.deleteProjectStore(projectKey);
        }
    }

    private static void invokeLoad(ProjectRegistryService service) throws Exception {
        Method load = ProjectRegistryService.class.getDeclaredMethod("load");
        load.setAccessible(true);
        load.invoke(service);
    }

    @SuppressWarnings("unchecked")
    private static boolean isProjectOpen(Neo4jProjectStore store, String projectKey) throws Exception {
        Field field = Neo4jProjectStore.class.getDeclaredField("runtimes");
        field.setAccessible(true);
        Map<String, ?> runtimes = (Map<String, ?>) field.get(store);
        return runtimes.containsKey(ActiveProjectContext.resolveRequestedOrActive(projectKey));
    }

    private static MethodReference methodRef(String className) {
        return new MethodReference(
                new ClassReference.Handle(className, 1),
                "run",
                "()V",
                false,
                Set.of(),
                1,
                10,
                "app.jar",
                1
        );
    }

    private static ProjectRuntimeSnapshot snapshotFor(String className, long buildSeq) {
        String jarPath = "/tmp/jar-analyzer/" + className.replace('/', '_') + ".jar";
        ProjectModel model = ProjectModel.artifact(
                Path.of(jarPath),
                null,
                List.of(Path.of(jarPath)),
                false
        );
        ClassReference classRef = new ClassReference(
                61,
                1,
                className,
                "java/lang/Object",
                List.of(),
                false,
                List.of(),
                Set.of(),
                "app.jar",
                1
        );
        MethodReference methodRef = methodRef(className);
        ClassFileEntity classFile = new ClassFileEntity();
        classFile.setCfId(1);
        classFile.setClassName(className);
        classFile.setPath(Path.of("/tmp/jar-analyzer/" + className + ".class"));
        classFile.setPathStr("/tmp/jar-analyzer/" + className + ".class");
        classFile.setJarName("app.jar");
        classFile.setJarId(1);
        return DatabaseManager.buildProjectRuntimeSnapshot(
                buildSeq,
                model,
                List.of(jarPath),
                new LinkedHashSet<>(List.of(classFile)),
                new LinkedHashSet<>(List.of(classRef)),
                new LinkedHashSet<>(List.of(methodRef)),
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
