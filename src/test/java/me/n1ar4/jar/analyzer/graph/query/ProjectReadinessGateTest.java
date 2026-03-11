/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.query;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jGraphSnapshotLoader;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectMetadataSnapshotStoreTestHook;
import me.n1ar4.support.DatabaseManagerTestHook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Label;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectReadinessGateTest {
    @AfterEach
    void cleanup() {
        DatabaseManagerTestHook.finishBuild();
        DatabaseManager.clearAllData();
    }

    @Test
    void graphStoreShouldRejectReadsWhileBuilding() {
        DatabaseManager.beginBuild();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new GraphStore().loadSnapshot());
        assertEquals("project_build_in_progress", ex.getMessage());
    }

    @Test
    void queryServiceShouldRejectNativeReadsWhileBuilding() {
        DatabaseManager.beginBuild();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new Neo4jQueryService().execute("MATCH (n) RETURN n LIMIT 1", Map.of(), QueryOptions.defaults()));
        assertEquals("project_build_in_progress", ex.getMessage());
    }

    @Test
    void graphStoreShouldRejectReadsWhenProjectRuntimeIsMissing() {
        DatabaseManager.clearAllData();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new GraphStore().loadSnapshot());
        assertEquals("project_model_missing_rebuild", ex.getMessage());
    }

    @Test
    void graphStoreShouldNotReviveActiveTemporaryProjectFromDiskAfterClearAllData() {
        String tempProjectKey = ActiveProjectContext.temporaryProjectKey();
        try {
            prepareReadyProject(tempProjectKey, 55L);
            DatabaseManager.clearAllData();

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> new GraphStore().loadSnapshot());
            assertEquals("project_model_missing_rebuild", ex.getMessage());
        } finally {
            Neo4jProjectStore.getInstance().deleteProjectStore(tempProjectKey);
            Neo4jGraphSnapshotLoader.invalidate(tempProjectKey);
        }
    }

    @Test
    void graphStoreShouldAllowReadyNonActiveProjectWhenActiveRuntimeIsMissing() {
        String projectKey = "readiness-" + Long.toHexString(System.nanoTime());
        try {
            prepareReadyProject(projectKey, 33L);
            DatabaseManager.clearAllData();

            assertNotNull(ActiveProjectContext.withProject(projectKey, () -> new GraphStore().loadSnapshot()));
        } finally {
            Neo4jProjectStore.getInstance().deleteProjectStore(projectKey);
            Neo4jGraphSnapshotLoader.invalidate(projectKey);
        }
    }

    @Test
    void queryServiceShouldAllowExplicitReadyProjectWhileAnotherProjectIsBuilding() throws Exception {
        String projectKey = "readiness-query-" + Long.toHexString(System.nanoTime());
        try {
            prepareReadyProject(projectKey, 44L);
            DatabaseManager.clearAllData();
            DatabaseManager.beginBuild();

            QueryResult result = new Neo4jQueryService()
                    .execute("MATCH (n:JANode) RETURN n LIMIT 1", Map.of(), QueryOptions.defaults(), projectKey);
            assertNotNull(result);
        } finally {
            DatabaseManagerTestHook.finishBuild();
            Neo4jProjectStore.getInstance().deleteProjectStore(projectKey);
            Neo4jGraphSnapshotLoader.invalidate(projectKey);
        }
    }

    private static void prepareReadyProject(String projectKey, long buildSeq) {
        ProjectModel model = ProjectModel.artifact(
                Path.of("/tmp/jar-analyzer/" + projectKey + ".jar"),
                null,
                List.of(Path.of("/tmp/jar-analyzer/" + projectKey + ".jar")),
                false
        );
        ProjectRuntimeSnapshot snapshot = new ProjectRuntimeSnapshot(
                ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION,
                buildSeq,
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
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                java.util.Set.of(),
                java.util.Set.of(),
                java.util.Set.of(),
                java.util.Set.of()
        );
        ProjectMetadataSnapshotStoreTestHook.write(projectKey, snapshot);
        var database = Neo4jProjectStore.getInstance().database(projectKey);
        try (var tx = database.beginTx()) {
            tx.execute("MATCH (m:JAMeta {key:'build_meta'}) DETACH DELETE m");
            var meta = tx.createNode(Label.label("JAMeta"));
            meta.setProperty("key", "build_meta");
            meta.setProperty("build_seq", buildSeq);
            var node = tx.createNode(Label.label("JANode"));
            node.setProperty("node_id", 1L);
            node.setProperty("kind", "method");
            node.setProperty("jar_id", 1);
            node.setProperty("class_name", "demo/Ready");
            node.setProperty("method_name", "run");
            node.setProperty("method_desc", "()V");
            node.setProperty("call_site_key", "");
            node.setProperty("line_number", -1);
            node.setProperty("call_index", -1);
            node.setProperty("source_flags", 0);
            tx.commit();
        }
        Neo4jGraphSnapshotLoader.invalidate(projectKey);
    }
}
