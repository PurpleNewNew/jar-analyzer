/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server.handler.base;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class BaseHandlerProjectGateTest {
    private Path markerFile;

    @AfterEach
    void cleanup() throws Exception {
        if (ActiveProjectContext.isProjectMutationInProgress()) {
            ActiveProjectContext.endProjectMutation(ActiveProjectContext.getActiveProjectKey());
        }
        EngineContext.setEngine(null);
        DatabaseManager.clearAllData();
        if (markerFile != null) {
            Files.deleteIfExists(markerFile);
        }
    }

    @Test
    void requireReadyEngineShouldRejectMutationWindowEvenWhenEngineExists() throws Exception {
        String projectKey = ActiveProjectContext.getActiveProjectKey();
        Path projectHome = Neo4jProjectStore.getInstance().resolveProjectHome(projectKey);
        Files.createDirectories(projectHome);
        markerFile = projectHome.resolve("handler-ready.marker");
        Files.writeString(markerFile, "ok");

        ProjectModel model = ProjectModel.artifact(
                Path.of("/tmp/jar-analyzer/base-handler-ready.jar"),
                null,
                List.of(Path.of("/tmp/jar-analyzer/base-handler-ready.jar")),
                false
        );
        DatabaseManager.restoreProjectRuntime(new ProjectRuntimeSnapshot(
                ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION,
                7L,
                new ProjectRuntimeSnapshot.ProjectModelData(
                        model.buildMode().name(),
                        model.primaryInputPath().toString(),
                        "",
                        List.of(),
                        List.of(model.primaryInputPath().toString()),
                        false
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                java.util.Map.of(),
                java.util.Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                java.util.Set.of(),
                java.util.Set.of(),
                java.util.Set.of(),
                java.util.Set.of()
        ));

        ConfigFile config = new ConfigFile();
        config.setDbPath(projectHome.toString());
        EngineContext.setEngine(new CoreEngine(config));

        TestHandler handler = new TestHandler();
        assertNotNull(handler.resolveReadyEngine());

        ActiveProjectContext.beginProjectMutation(projectKey);
        try {
            assertNull(handler.resolveReadyEngine());
        } finally {
            ActiveProjectContext.endProjectMutation(projectKey);
        }
    }

    private static final class TestHandler extends BaseHandler {
        private CoreEngine resolveReadyEngine() {
            return requireReadyEngine();
        }
    }
}
