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
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Label;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProjectContextModelTest {
    @AfterEach
    void cleanup() {
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
            writeBuildMeta(originalKey, 11L);
            writeBuildMeta(otherProjectKey, 22L);

            long snapshot = BuildSeqUtil.projectSnapshot(originalKey);
            ActiveProjectContext.setActiveProject(otherProjectKey, otherProjectKey);

            assertFalse(BuildSeqUtil.isProjectStale(originalKey, snapshot));
        } finally {
            ActiveProjectContext.setActiveProject(originalKey, originalAlias);
            Neo4jProjectStore.getInstance().deleteProjectStore(otherProjectKey);
        }
    }

    private static void invokeLoad(ProjectRegistryService service) throws Exception {
        Method load = ProjectRegistryService.class.getDeclaredMethod("load");
        load.setAccessible(true);
        load.invoke(service);
    }

    private static void writeBuildMeta(String projectKey, long buildSeq) {
        var database = Neo4jProjectStore.getInstance().database(projectKey);
        try (var tx = database.beginTx()) {
            tx.execute("MATCH (m:JAMeta {key:'build_meta'}) DETACH DELETE m");
            var meta = tx.createNode(Label.label("JAMeta"));
            meta.setProperty("key", "build_meta");
            meta.setProperty("build_seq", buildSeq);
            tx.commit();
        }
    }
}
