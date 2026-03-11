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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphStoreFlowSnapshotTest {
    private static final ProjectGraphStoreFacade FACADE = ProjectGraphStoreFacade.getInstance();

    @AfterEach
    void cleanup() {
        DatabaseManagerTestHook.finishBuild();
        DatabaseManager.clearAllData();
    }

    @Test
    void flowSnapshotShouldKeepOnlyMethodNodesAndFlowEdges() {
        String projectKey = "flow-lite-" + Long.toHexString(System.nanoTime());
        try {
            prepareReadyProject(projectKey, 77L);
            FACADE.write(projectKey, 30_000L, tx -> {
                tx.execute("MATCH (n) DETACH DELETE n");

                var meta = tx.createNode(Label.label("JAMeta"));
                meta.setProperty("key", "build_meta");
                meta.setProperty("build_seq", 77L);

                var clazz = tx.createNode(Label.label("JANode"), Label.label("Class"));
                clazz.setProperty("node_id", 10L);
                clazz.setProperty("kind", "class");
                clazz.setProperty("jar_id", 1);
                clazz.setProperty("class_name", "demo/FlowLite");
                clazz.setProperty("method_name", "");
                clazz.setProperty("method_desc", "");
                clazz.setProperty("call_site_key", "");
                clazz.setProperty("line_number", -1);
                clazz.setProperty("call_index", -1);
                clazz.setProperty("source_flags", 0);
                clazz.setProperty("method_semantic_flags", 0);

                var entry = methodNode(tx, 1L, "demo/FlowLite", "entry", "()V");
                var mid = methodNode(tx, 2L, "demo/FlowLite", "mid", "()V");
                var sink = methodNode(tx, 3L, "demo/FlowLite", "sink", "()V");

                var direct = entry.createRelationshipTo(mid, RelationshipType.withName("CALLS_DIRECT"));
                direct.setProperty("edge_id", 101L);
                direct.setProperty("confidence", "high");
                direct.setProperty("evidence", "unit:direct");
                direct.setProperty("call_site_key", "demo/FlowLite#entry#()V@0");
                direct.setProperty("line_number", 12);
                direct.setProperty("call_index", 0);
                direct.setProperty("edge_semantic_flags", 3);

                var alias = mid.createRelationshipTo(sink, RelationshipType.withName("ALIAS"));
                alias.setProperty("edge_id", 102L);
                alias.setProperty("confidence", "medium");
                alias.setProperty("evidence", "unit:alias");
                alias.setProperty("alias_kind", "field");
                alias.setProperty("call_site_key", "demo/FlowLite#mid#()V@1");
                alias.setProperty("line_number", 18);
                alias.setProperty("call_index", 1);
                alias.setProperty("edge_semantic_flags", 5);

                var ignored = entry.createRelationshipTo(sink, RelationshipType.withName("HAS"));
                ignored.setProperty("edge_id", 103L);
                ignored.setProperty("confidence", "high");
                ignored.setProperty("evidence", "unit:has");
                return null;
            });
            Neo4jGraphSnapshotLoader.invalidate(projectKey);

            GraphSnapshot snapshot = new GraphStore().loadFlowSnapshot(projectKey);

            assertEquals(77L, snapshot.getBuildSeq());
            assertEquals(3, snapshot.getNodeCount());
            assertEquals(3, snapshot.getNodesByKindView("method").size());
            assertTrue(snapshot.getNodesByKindView("class").isEmpty());
            assertEquals(1L, snapshot.findMethodNodeId("demo/FlowLite", "entry", "()V", 1));
            assertNull(snapshot.getNode(10L));

            List<GraphEdge> entryOutgoing = snapshot.getOutgoingView(1L);
            assertEquals(1, entryOutgoing.size());
            assertEquals("CALLS_DIRECT", entryOutgoing.get(0).getRelType());
            assertEquals("demo/FlowLite#entry#()V@0", entryOutgoing.get(0).getCallSiteKey());
            assertEquals(12, entryOutgoing.get(0).getLineNumber());
            assertEquals(0, entryOutgoing.get(0).getCallIndex());
            assertEquals(3, entryOutgoing.get(0).getSemanticFlags());

            List<GraphEdge> midOutgoing = snapshot.getOutgoingView(2L);
            assertEquals(1, midOutgoing.size());
            assertEquals("ALIAS", midOutgoing.get(0).getRelType());
            assertEquals("field", midOutgoing.get(0).getAliasKind());
            assertEquals("unit:alias", midOutgoing.get(0).getEvidence());
            assertEquals(5, midOutgoing.get(0).getSemanticFlags());

            assertTrue(snapshot.getOutgoingView(3L).isEmpty());
            assertNotNull(snapshot.getNode(1L));
            assertNotNull(snapshot.getNode(2L));
            assertNotNull(snapshot.getNode(3L));
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
