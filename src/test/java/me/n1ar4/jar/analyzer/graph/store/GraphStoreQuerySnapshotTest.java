/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.store;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jGraphSnapshotLoader;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectGraphStoreFacade;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectMetadataSnapshotStoreTestHook;
import me.n1ar4.support.DatabaseManagerTestHook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphStoreQuerySnapshotTest {
    private static final ProjectGraphStoreFacade FACADE = ProjectGraphStoreFacade.getInstance();

    @AfterEach
    void cleanup() {
        DatabaseManagerTestHook.finishBuild();
        DatabaseManager.clearAllData();
    }

    @Test
    void querySnapshotShouldKeepOnlyMethodNodesAndCallEdges() {
        String projectKey = "query-lite-" + Long.toHexString(System.nanoTime());
        try {
            prepareReadyProject(projectKey, 88L);
            FACADE.write(projectKey, 30_000L, tx -> {
                tx.execute("MATCH (n) DETACH DELETE n");

                var meta = tx.createNode(Label.label("JAMeta"));
                meta.setProperty("key", "build_meta");
                meta.setProperty("build_seq", 88L);

                var clazz = tx.createNode(Label.label("JANode"), Label.label("Class"));
                clazz.setProperty("node_id", 10L);
                clazz.setProperty("kind", "class");
                clazz.setProperty("jar_id", 1);
                clazz.setProperty("class_name", "demo/QueryLite");
                clazz.setProperty("method_name", "");
                clazz.setProperty("method_desc", "");
                clazz.setProperty("call_site_key", "");
                clazz.setProperty("line_number", -1);
                clazz.setProperty("call_index", -1);
                clazz.setProperty("source_flags", 0);
                clazz.setProperty("method_semantic_flags", 0);

                var entry = methodNode(tx, 1L, "demo/QueryLite", "entry", "()V");
                var mid = methodNode(tx, 2L, "demo/QueryLite", "mid", "()V");
                var sink = methodNode(tx, 3L, "demo/QueryLite", "sink", "()V");

                var direct = entry.createRelationshipTo(mid, RelationshipType.withName("CALLS_DIRECT"));
                direct.setProperty("edge_id", 201L);
                direct.setProperty("confidence", "high");
                direct.setProperty("evidence", "unit:direct");
                direct.setProperty("call_site_key", "demo/QueryLite#entry#()V@0");
                direct.setProperty("line_number", 20);
                direct.setProperty("call_index", 0);
                direct.setProperty("edge_semantic_flags", 7);

                var pta = mid.createRelationshipTo(sink, RelationshipType.withName("CALLS_PTA"));
                pta.setProperty("edge_id", 202L);
                pta.setProperty("confidence", "medium");
                pta.setProperty("evidence", "unit:pta");
                pta.setProperty("call_site_key", "demo/QueryLite#mid#()V@1");
                pta.setProperty("line_number", 24);
                pta.setProperty("call_index", 1);
                pta.setProperty("edge_semantic_flags", 9);

                var alias = mid.createRelationshipTo(sink, RelationshipType.withName("ALIAS"));
                alias.setProperty("edge_id", 203L);
                alias.setProperty("confidence", "medium");
                alias.setProperty("evidence", "unit:alias");

                var ignored = entry.createRelationshipTo(sink, RelationshipType.withName("HAS"));
                ignored.setProperty("edge_id", 204L);
                ignored.setProperty("confidence", "high");
                ignored.setProperty("evidence", "unit:has");
                return null;
            });
            Neo4jGraphSnapshotLoader.invalidate(projectKey);

            GraphSnapshot snapshot = new GraphStore().loadQuerySnapshot(projectKey);

            assertEquals(88L, snapshot.getBuildSeq());
            assertEquals(3, snapshot.getNodeCount());
            assertEquals(2, snapshot.getOutgoingView(1L).size() + snapshot.getOutgoingView(2L).size());
            assertEquals(1, snapshot.getOutgoingView(1L).size());
            assertEquals("CALLS_DIRECT", snapshot.getOutgoingView(1L).get(0).getRelType());
            assertEquals("demo/QueryLite#entry#()V@0", snapshot.getOutgoingView(1L).get(0).getCallSiteKey());
            assertEquals(20, snapshot.getOutgoingView(1L).get(0).getLineNumber());
            assertEquals(1, snapshot.getOutgoingView(2L).size());
            assertEquals("CALLS_PTA", snapshot.getOutgoingView(2L).get(0).getRelType());
            assertEquals("unit:pta", snapshot.getOutgoingView(2L).get(0).getEvidence());
            assertEquals(9, snapshot.getOutgoingView(2L).get(0).getSemanticFlags());
            assertTrue(snapshot.getOutgoingView(3L).isEmpty());
            assertNull(snapshot.getNode(10L));
            assertEquals(1L, snapshot.findMethodNodeId("demo/QueryLite", "entry", "()V", 1));
        } finally {
            FACADE.deleteProjectStore(projectKey);
            Neo4jGraphSnapshotLoader.invalidate(projectKey);
        }
    }

    @Test
    void querySnapshotShouldRequireRebuildWhenBuildSeqMismatches() {
        String projectKey = "query-mismatch-" + Long.toHexString(System.nanoTime());
        try {
            prepareReadyProject(projectKey, 99L);
            FACADE.write(projectKey, 30_000L, tx -> {
                tx.execute("MATCH (n) DETACH DELETE n");
                var meta = tx.createNode(Label.label("JAMeta"));
                meta.setProperty("key", "build_meta");
                meta.setProperty("build_seq", 98L);
                methodNode(tx, 1L, "demo/QueryMismatch", "entry", "()V");
                return null;
            });
            Neo4jGraphSnapshotLoader.invalidate(projectKey);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> new GraphStore().loadQuerySnapshot(projectKey));
            assertEquals("graph_query_snapshot_missing_rebuild", ex.getMessage());
        } finally {
            FACADE.deleteProjectStore(projectKey);
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
    }

    private static org.neo4j.graphdb.Node methodNode(org.neo4j.graphdb.Transaction tx,
                                                     long nodeId,
                                                     String className,
                                                     String methodName,
                                                     String methodDesc) {
        var node = tx.createNode(Label.label("JANode"), Label.label("Method"));
        node.setProperty("node_id", nodeId);
        node.setProperty("kind", "method");
        node.setProperty("jar_id", 1);
        node.setProperty("class_name", className);
        node.setProperty("method_name", methodName);
        node.setProperty("method_desc", methodDesc);
        node.setProperty("call_site_key", "");
        node.setProperty("line_number", -1);
        node.setProperty("call_index", -1);
        node.setProperty("source_flags", 0);
        node.setProperty("method_semantic_flags", 0);
        return node;
    }
}
