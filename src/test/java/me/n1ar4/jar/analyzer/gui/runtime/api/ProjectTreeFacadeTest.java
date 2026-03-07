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

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.support.DatabaseManagerTestHook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectTreeFacadeTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
        EngineContext.setEngine(null);
    }

    @Test
    void snapshotShouldBeEmptyWhenEngineMissing() {
        EngineContext.setEngine(null);
        assertTrue(RuntimeFacades.projectTree().snapshot().isEmpty());
        assertTrue(RuntimeFacades.projectTree().search("abc").isEmpty());
    }

    @Test
    void supportShouldBuildSemanticRootTreeWhenProjectModelReady() {
        DatabaseManager.runAtomicUpdate(() -> {
            DatabaseManager.saveProjectModel(ProjectModel.artifact(
                    tempDir,
                    null,
                    List.of(tempDir),
                    false
            ));
            DatabaseManagerTestHook.markProjectBuildReady(9L);
        });

        ProjectTreeSupport support = new ProjectTreeSupport(ProjectTreeSupport.UiActions.noop());
        List<me.n1ar4.jar.analyzer.gui.runtime.model.TreeNodeDto> nodes = support.buildTree(
                null,
                new ProjectTreeSupport.TreeSettings(false, false, false)
        );

        assertEquals(1, nodes.size());
        assertEquals("App", nodes.get(0).label());
        assertTrue(nodes.get(0).directory());
        assertTrue(nodes.get(0).children().stream().anyMatch(node -> "Roots".equals(node.label())));
    }
}
