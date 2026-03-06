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

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProjectContextModelTest {
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
}
