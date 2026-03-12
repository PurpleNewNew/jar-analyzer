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

import me.n1ar4.jar.analyzer.core.ProjectStateUtil;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.support.DatabaseManagerTestHook;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProjectContextModelTest {
    @AfterEach
    void cleanup() {
        DatabaseManagerTestHook.finishBuild();
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
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
    public void registerShouldSeparateProjectsByRuntimeAndNestedJarSettings() {
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        ProjectRegistryEntry base = service.register("base", "/tmp/demo/app.jar", "/tmp/jdk-21", false);
        ProjectRegistryEntry sameConfig = service.register("same", "/tmp/demo/app.jar", "/tmp/jdk-21", false);
        ProjectRegistryEntry otherRuntime = service.register("runtime", "/tmp/demo/app.jar", "/tmp/jdk-17", false);
        ProjectRegistryEntry nested = service.register("nested", "/tmp/demo/app.jar", "/tmp/jdk-21", true);
        try {
            assertEquals(base.projectKey(), sameConfig.projectKey());
            assertNotEquals(base.projectKey(), otherRuntime.projectKey());
            assertNotEquals(base.projectKey(), nested.projectKey());
        } finally {
            service.activateTemporaryProject();
            service.remove(base.projectKey(), true);
            service.remove(otherRuntime.projectKey(), true);
            service.remove(nested.projectKey(), true);
        }
    }

    @Test
    public void projectSwitchAndRemoveShouldBeBlockedWhileBuilding() {
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        ProjectRegistryEntry created = service.createProject("build-lock-test");
        try {
            service.activateTemporaryProject();
            DatabaseManager.beginBuild();

            assertEquals("project_build_in_progress",
                    assertThrows(IllegalStateException.class,
                            () -> service.switchActive(created.projectKey())).getMessage());
            assertEquals("project_build_in_progress",
                    assertThrows(IllegalStateException.class,
                            () -> service.remove(created.projectKey(), true)).getMessage());
        } finally {
            DatabaseManagerTestHook.finishBuild();
            service.activateTemporaryProject();
            service.remove(created.projectKey(), true);
        }
    }

    @Test
    public void projectSwitchShouldFailFastWhileBuildHoldsMutationLock() throws Exception {
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        ProjectRegistryEntry created = service.createProject("build-fast-fail-test");
        CountDownLatch lockHeld = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        Thread holder = Thread.ofPlatform().name("project-mutation-lock-holder").start(() -> {
            synchronized (ActiveProjectContext.mutationLock()) {
                lockHeld.countDown();
                try {
                    releaseLock.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            assertTrue(lockHeld.await(2, TimeUnit.SECONDS));
            DatabaseManager.beginBuild();

            Future<Throwable> future = executor.submit(() -> {
                try {
                    service.switchActive(created.projectKey());
                    return null;
                } catch (Throwable ex) {
                    return ex;
                }
            });

            Throwable result = future.get(500, TimeUnit.MILLISECONDS);
            assertTrue(result instanceof IllegalStateException);
            assertEquals("project_build_in_progress", result.getMessage());
        } finally {
            DatabaseManagerTestHook.finishBuild();
            releaseLock.countDown();
            executor.shutdownNow();
            holder.join(2_000L);
            service.activateTemporaryProject();
            service.remove(created.projectKey(), true);
        }
    }

    @Test
    public void switchActiveShouldRestoreRuntimeAndEngine() {
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        ProjectRegistryEntry created = service.createProject("switch-restore-test");
        try {
            ProjectRuntimeSnapshot snapshot = snapshotFor("demo/SwitchRestoreController", 77L);
            ProjectMetadataSnapshotStoreTestHook.write(created.projectKey(), snapshot);

            service.activateTemporaryProject();
            DatabaseManager.clearAllData();
            EngineContext.setEngine(null);

            service.switchActive(created.projectKey());

            assertEquals(created.projectKey(), ActiveProjectContext.getActiveProjectKey());
            assertEquals(77L, DatabaseManager.getProjectBuildSeq());
            assertNotNull(DatabaseManager.getProjectModel());
            assertEquals(created.projectKey(), ProjectRuntimeContext.projectKey());
            assertEquals(77L, ProjectRuntimeContext.buildSeq());
            assertEquals(DatabaseManager.getProjectModel(), ProjectRuntimeContext.getProjectModel());
            assertNotNull(EngineContext.getEngine());
        } finally {
            service.activateTemporaryProject();
            service.remove(created.projectKey(), true);
        }
    }

    @Test
    public void loadShouldRestorePersistedActiveProjectRuntimeAndEngine() throws Exception {
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        ProjectRegistryEntry created = service.createProject("load-restore-test");
        try {
            ProjectMetadataSnapshotStoreTestHook.write(created.projectKey(), snapshotFor("demo/LoadRestoreController", 909L));
            service.switchActive(created.projectKey());

            DatabaseManager.clearAllData();
            ProjectRuntimeContext.clear();
            EngineContext.setEngine(null);

            invokeLoad(service);

            assertEquals(created.projectKey(), ActiveProjectContext.getActiveProjectKey());
            assertEquals(909L, DatabaseManager.getProjectBuildSeq());
            assertFalse(DatabaseManager.getMethodReferences().isEmpty());
            assertEquals("demo/LoadRestoreController",
                    DatabaseManager.getMethodReferences().get(0).getClassReference().getName());
            assertEquals(created.projectKey(), ProjectRuntimeContext.projectKey());
            assertEquals(909L, ProjectRuntimeContext.buildSeq());
            assertEquals(DatabaseManager.getProjectModel(), ProjectRuntimeContext.getProjectModel());
            assertNotNull(EngineContext.getEngine());
            assertTrue(EngineContext.getEngine().isEnabled());
        } finally {
            service.activateTemporaryProject();
            service.remove(created.projectKey(), true);
        }
    }

    @Test
    public void switchActiveShouldClearRuntimeWhenNextProjectHasNoSnapshot() {
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        ProjectRegistryEntry created = service.register(
                "empty-runtime-test",
                "/tmp/demo/no-snapshot.jar",
                "/tmp/jdk-21",
                true
        );
        try {
            service.activateTemporaryProject();
            DatabaseManager.restoreProjectRuntime(snapshotFor("demo/OldController", 19L));

            service.switchActive(created.projectKey());

            assertEquals(created.projectKey(), ActiveProjectContext.getActiveProjectKey());
            assertTrue(DatabaseManager.getMethodReferences().isEmpty());
            assertEquals(0L, DatabaseManager.getProjectBuildSeq());
            assertNotNull(DatabaseManager.getProjectModel());
            assertEquals(Path.of("/tmp/demo/no-snapshot.jar"), DatabaseManager.getProjectModel().primaryInputPath());
            assertEquals(created.projectKey(), ProjectRuntimeContext.projectKey());
            assertEquals(0L, ProjectRuntimeContext.buildSeq());
            assertEquals(Path.of("/tmp/demo/no-snapshot.jar"), ProjectRuntimeContext.primaryInputPath());
            assertTrue(ProjectRuntimeContext.resolveInnerJars());
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
        ProjectRegistryEntry first = service.createProject("remove-active-first");
        ProjectRegistryEntry second = service.createProject("remove-active-second");
        Path firstHome = Neo4jProjectStore.getInstance().resolveProjectHome(first.projectKey());
        try {
            ProjectMetadataSnapshotStoreTestHook.write(first.projectKey(), snapshotFor("demo/FirstController", 101L));
            ProjectMetadataSnapshotStoreTestHook.write(second.projectKey(), snapshotFor("demo/SecondController", 202L));

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
        DatabaseManager.restoreProjectRuntime(snapshotFor("demo/TempController", 33L));
        String projectKey = ActiveProjectContext.getActiveProjectKey();
        long snapshot = ProjectStateUtil.projectBuildSnapshot(projectKey);

        service.cleanupTemporaryProject();

        assertTrue(ActiveProjectContext.isTemporaryProjectKey(ActiveProjectContext.getActiveProjectKey()));
        assertTrue(DatabaseManager.getMethodReferences().isEmpty());
        assertEquals(0L, DatabaseManager.getProjectBuildSeq());
        assertTrue(ProjectStateUtil.isProjectBuildStale(projectKey, snapshot));
    }

    @Test
    public void failedBuildShouldKeepProjectUnreadableAfterSwitchBack() {
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        ProjectMetadataSnapshotStore metadataStore = ProjectMetadataSnapshotStore.getInstance();
        ProjectRegistryEntry created = service.createProject("failed-build-project");
        try {
            ProjectMetadataSnapshotStoreTestHook.write(created.projectKey(), snapshotFor("demo/FailedBuildController", 303L));

            service.activateTemporaryProject();
            DatabaseManager.clearAllData();
            EngineContext.setEngine(null);
            service.switchActive(created.projectKey());
            assertEquals(303L, DatabaseManager.getProjectBuildSeq());
            assertFalse(DatabaseManager.getMethodReferences().isEmpty());

            synchronized (ActiveProjectContext.mutationLock()) {
                DatabaseManager.beginBuild(created.projectKey());
            }
            DatabaseManagerTestHook.finishBuild(false);
            assertTrue(metadataStore.isUnavailable(created.projectKey()));

            service.activateTemporaryProject();
            service.switchActive(created.projectKey());

            assertEquals(created.projectKey(), ActiveProjectContext.getActiveProjectKey());
            assertTrue(DatabaseManager.getMethodReferences().isEmpty());
            assertEquals(0L, DatabaseManager.getProjectBuildSeq());
            assertFalse(DatabaseManager.isProjectReady());
            assertNotNull(DatabaseManager.getProjectModel());
            assertNotNull(EngineContext.getEngine());
            assertFalse(EngineContext.getEngine().isEnabled());
        } finally {
            DatabaseManagerTestHook.finishBuild();
            service.activateTemporaryProject();
            service.remove(created.projectKey(), true);
        }
    }

    @Test
    public void failedBuildShouldKeepActiveProjectUnreadableWithoutSwitchingAway() {
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        ProjectMetadataSnapshotStore metadataStore = ProjectMetadataSnapshotStore.getInstance();
        ProjectRegistryEntry created = service.createProject("failed-build-active");
        try {
            ProjectMetadataSnapshotStoreTestHook.write(created.projectKey(), snapshotFor("demo/FailedActiveController", 404L));

            service.activateTemporaryProject();
            DatabaseManager.clearAllData();
            EngineContext.setEngine(null);
            service.switchActive(created.projectKey());
            assertEquals(404L, DatabaseManager.getProjectBuildSeq());
            assertFalse(DatabaseManager.getMethodReferences().isEmpty());

            synchronized (ActiveProjectContext.mutationLock()) {
                DatabaseManager.beginBuild(created.projectKey());
            }
            DatabaseManagerTestHook.finishBuild(false);
            assertTrue(metadataStore.isUnavailable(created.projectKey()));

            assertEquals(created.projectKey(), ActiveProjectContext.getActiveProjectKey());
            assertTrue(DatabaseManager.getMethodReferences().isEmpty());
            assertEquals(0L, DatabaseManager.getProjectBuildSeq());
            assertFalse(DatabaseManager.isProjectReady());
            assertNotNull(DatabaseManager.getProjectModel());
            assertNotNull(EngineContext.getEngine());
            assertFalse(EngineContext.getEngine().isEnabled());
        } finally {
            DatabaseManagerTestHook.finishBuild();
            service.activateTemporaryProject();
            service.remove(created.projectKey(), true);
        }
    }

    @Test
    public void switchActiveShouldFailWithoutChangingCurrentProjectWhenSnapshotIsCorrupt() throws Exception {
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        ProjectMetadataSnapshotStore metadataStore = ProjectMetadataSnapshotStore.getInstance();
        ProjectRegistryEntry created = service.createProject("corrupt-switch-target");
        try {
            service.activateTemporaryProject();
            DatabaseManager.restoreProjectRuntime(snapshotFor("demo/CurrentController", 515L));
            String originalProjectKey = ActiveProjectContext.getActiveProjectKey();

            Path snapshotFile = metadataStore.resolveSnapshotFile(created.projectKey());
            Files.createDirectories(snapshotFile.getParent());
            Files.writeString(snapshotFile, "{invalid-json", StandardCharsets.UTF_8);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.switchActive(created.projectKey()));
            assertEquals("project_runtime_snapshot_restore_failed", ex.getMessage());
            assertEquals(originalProjectKey, ActiveProjectContext.getActiveProjectKey());
            assertEquals(515L, DatabaseManager.getProjectBuildSeq());
            assertEquals(1, DatabaseManager.getMethodReferences().size());
            assertEquals("demo/CurrentController",
                    DatabaseManager.getMethodReferences().get(0).getClassReference().getName());
        } finally {
            service.activateTemporaryProject();
            service.remove(created.projectKey(), true);
        }
    }

    @Test
    public void registerShouldRollbackEntryWhenRuntimeRestoreFailsAfterPersist() throws Exception {
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        ProjectRegistryEntry existing = service.register("existing-corrupt-project", "/tmp/demo/existing-corrupt.jar", "", false);
        ProjectMetadataSnapshotStore metadataStore = ProjectMetadataSnapshotStore.getInstance();
        try {
            service.activateTemporaryProject();
            DatabaseManager.restoreProjectRuntime(snapshotFor("demo/CurrentController", 616L));
            String originalProjectKey = ActiveProjectContext.getActiveProjectKey();

            Path snapshotFile = metadataStore.resolveSnapshotFile(existing.projectKey());
            Files.createDirectories(snapshotFile.getParent());
            Files.writeString(snapshotFile, "{invalid-json", StandardCharsets.UTF_8);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.register("updated-alias", "/tmp/demo/existing-corrupt.jar", "", false));
            assertEquals("project_runtime_snapshot_restore_failed", ex.getMessage());
            assertEquals(originalProjectKey, ActiveProjectContext.getActiveProjectKey());
            assertEquals(616L, DatabaseManager.getProjectBuildSeq());
            ProjectRegistryEntry restored = service.list().stream()
                    .filter(entry -> entry.projectKey().equals(existing.projectKey()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(restored);
            assertEquals(existing.alias(), restored.alias());
        } finally {
            service.activateTemporaryProject();
            service.remove(existing.projectKey(), true);
        }
    }

    @Test
    public void removeShouldRollbackEntryWhenNextProjectRestoreFailsAfterPersist() throws Exception {
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        ProjectMetadataSnapshotStore metadataStore = ProjectMetadataSnapshotStore.getInstance();
        ProjectRegistryEntry current = service.createProject("rollback-remove-current");
        ProjectRegistryEntry broken = service.createProject("rollback-remove-broken");
        try {
            ProjectMetadataSnapshotStoreTestHook.write(current.projectKey(), snapshotFor("demo/CurrentController", 717L));
            service.switchActive(current.projectKey());
            String originalProjectKey = ActiveProjectContext.getActiveProjectKey();

            Path snapshotFile = metadataStore.resolveSnapshotFile(broken.projectKey());
            Files.createDirectories(snapshotFile.getParent());
            Files.writeString(snapshotFile, "{invalid-json", StandardCharsets.UTF_8);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.remove(current.projectKey(), false));
            assertEquals("project_runtime_snapshot_restore_failed", ex.getMessage());
            assertEquals(originalProjectKey, ActiveProjectContext.getActiveProjectKey());
            assertEquals(717L, DatabaseManager.getProjectBuildSeq());
            assertTrue(service.list().stream().anyMatch(entry -> entry.projectKey().equals(current.projectKey())));
        } finally {
            service.activateTemporaryProject();
            service.remove(current.projectKey(), true);
            service.remove(broken.projectKey(), true);
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
            ProjectRegistrySnapshot snapshot = service.snapshot();
            assertEquals("unavailable", snapshot.registryState());
            assertTrue(snapshot.registryMessage().contains(".jar-analyzer-projects.json"));
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
    public void loadShouldReportMissingRegistryFileSeparatelyFromCorruptRegistry() throws Exception {
        Path registry = Path.of(".jar-analyzer-projects.json").toAbsolutePath().normalize();
        byte[] backup = Files.exists(registry) ? Files.readAllBytes(registry) : null;
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        try {
            Files.deleteIfExists(registry);
            invokeLoad(service);

            ProjectRegistrySnapshot snapshot = service.snapshot();
            assertEquals("missing", snapshot.registryState());
            assertTrue(snapshot.registryMessage().contains(".jar-analyzer-projects.json"));
            assertTrue(snapshot.projects().isEmpty());
        } finally {
            if (backup != null) {
                Files.write(registry, backup);
            }
            invokeLoad(service);
        }
    }

    @Test
    public void projectSnapshotShouldIgnoreUnrelatedActiveProjectChanges() {
        String originalKey = ActiveProjectContext.getActiveProjectKey();
        String originalAlias = ActiveProjectContext.getActiveProjectAlias();
        String otherProjectKey = "build-snapshot-" + Long.toHexString(System.nanoTime());
        try {
            DatabaseManager.restoreProjectRuntime(snapshotFor("demo/ProjectSnapshotController", 11L));

            long snapshot = ProjectStateUtil.projectBuildSnapshot(originalKey);
            ActiveProjectContext.setActiveProject(otherProjectKey, otherProjectKey);

            assertFalse(ProjectStateUtil.isProjectBuildStale(originalKey, snapshot));
        } finally {
            ActiveProjectContext.setActiveProject(originalKey, originalAlias);
            DatabaseManager.clearAllData();
        }
    }

    @Test
    public void projectSnapshotShouldNotOpenProjectStore() throws Exception {
        String projectKey = "snapshot-read-" + Long.toHexString(System.nanoTime());
        Neo4jProjectStore store = Neo4jProjectStore.getInstance();
        ProjectMetadataSnapshotStoreTestHook.write(projectKey, snapshotFor("demo/SnapshotReadController", 88L));
        store.closeProject(projectKey);
        try {
            assertFalse(isProjectOpen(store, projectKey));
            assertEquals(88L, ProjectStateUtil.projectBuildSnapshot(projectKey));
            assertFalse(isProjectOpen(store, projectKey));
        } finally {
            store.deleteProjectStore(projectKey);
        }
    }

    @Test
    public void registerShouldNotChangeRuntimeOrEntriesWhenRegistryPersistFails() throws Exception {
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        String originalProjectKey = ActiveProjectContext.getActiveProjectKey();
        int originalSize = service.list().size();
        String failingInput = "/tmp/jar-analyzer/persist-fail-register.jar";
        String failingProjectKey = ProjectRegistryService.buildProjectKey(failingInput);
        Neo4jProjectStore.getInstance().deleteProjectStore(failingProjectKey);
        Path failingHome = Neo4jProjectStore.getInstance().resolveProjectHome(failingProjectKey);
        RegistryBackup backup = replaceRegistryWithDirectory();
        try {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.register("persist-fail-register", failingInput, "", false));
            assertEquals("project_registry_persist_failed", ex.getMessage());
            assertEquals(originalProjectKey, ActiveProjectContext.getActiveProjectKey());
            assertEquals(originalSize, service.list().size());
            assertTrue(service.list().stream().noneMatch(entry -> entry.projectKey().equals(failingProjectKey)));
            assertFalse(Files.exists(failingHome));
        } finally {
            restoreRegistryBackup(service, backup);
            Neo4jProjectStore.getInstance().deleteProjectStore(failingProjectKey);
        }
    }

    @Test
    public void createProjectShouldDeleteNewStoreWhenRegistryPersistFails() throws Exception {
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        String originalProjectKey = ActiveProjectContext.getActiveProjectKey();
        int originalSize = service.list().size();
        Set<String> beforeStores = listPersistentProjectStores();
        RegistryBackup backup = replaceRegistryWithDirectory();
        try {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.createProject("persist-fail-create"));
            assertEquals("project_registry_persist_failed", ex.getMessage());
            assertEquals(originalProjectKey, ActiveProjectContext.getActiveProjectKey());
            assertEquals(originalSize, service.list().size());
            assertEquals(beforeStores, listPersistentProjectStores());
        } finally {
            restoreRegistryBackup(service, backup);
            for (String store : listPersistentProjectStores()) {
                if (!beforeStores.contains(store)) {
                    Neo4jProjectStore.getInstance().deleteProjectStore(store);
                }
            }
        }
    }

    @Test
    public void removeShouldNotChangeRuntimeOrEntriesWhenRegistryPersistFails() throws Exception {
        ProjectRegistryService service = ProjectRegistryService.getInstance();
        ProjectRegistryEntry first = service.createProject("persist-fail-remove-first");
        ProjectRegistryEntry second = service.createProject("persist-fail-remove-second");
        try {
            service.switchActive(first.projectKey());
            RegistryBackup backup = replaceRegistryWithDirectory();
            try {
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                        () -> service.remove(first.projectKey(), false));
                assertEquals("project_registry_persist_failed", ex.getMessage());
                assertEquals(first.projectKey(), ActiveProjectContext.getActiveProjectKey());
                assertTrue(service.list().stream().anyMatch(entry -> entry.projectKey().equals(first.projectKey())));
            } finally {
                restoreRegistryBackup(service, backup);
            }
        } finally {
            service.activateTemporaryProject();
            service.remove(first.projectKey(), true);
            service.remove(second.projectKey(), true);
        }
    }

    private static void invokeLoad(ProjectRegistryService service) throws Exception {
        Method load = ProjectRegistryService.class.getDeclaredMethod("load");
        load.setAccessible(true);
        load.invoke(service);
    }

    private static RegistryBackup replaceRegistryWithDirectory() throws Exception {
        Path registry = Path.of(".jar-analyzer-projects.json").toAbsolutePath().normalize();
        Path temp = registry.resolveSibling(registry.getFileName() + ".tmp");
        byte[] backup = Files.exists(registry) ? Files.readAllBytes(registry) : null;
        Files.deleteIfExists(temp);
        Files.createDirectories(temp);
        return new RegistryBackup(registry, backup, temp);
    }

    private static void restoreRegistryBackup(ProjectRegistryService service, RegistryBackup backup) throws Exception {
        if (backup == null) {
            return;
        }
        Files.deleteIfExists(backup.tempPath());
        Files.deleteIfExists(backup.path());
        if (backup.content() != null) {
            Files.write(backup.path(), backup.content());
        }
        invokeLoad(service);
    }

    @SuppressWarnings("unchecked")
    private static boolean isProjectOpen(Neo4jProjectStore store, String projectKey) throws Exception {
        Field field = Neo4jProjectStore.class.getDeclaredField("runtimes");
        field.setAccessible(true);
        Map<String, ?> runtimes = (Map<String, ?>) field.get(store);
        return runtimes.containsKey(ActiveProjectContext.resolveRequestedOrActive(projectKey));
    }

    private static Set<String> listPersistentProjectStores() throws Exception {
        Path root = Neo4jProjectStore.getInstance().resolveProjectHome("store-root-marker").getParent();
        if (root == null || !Files.exists(root)) {
            return Set.of();
        }
        try (var stream = Files.list(root)) {
            return stream.filter(Files::isDirectory)
                    .map(path -> path.getFileName() == null ? "" : path.getFileName().toString())
                    .filter(name -> !name.isBlank())
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        }
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

    private record RegistryBackup(Path path, byte[] content, Path tempPath) {
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
