package me.n1ar4.jar.analyzer.storage.neo4j;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.support.DatabaseManagerTestHook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Neo4jGraphSnapshotLoaderConcurrencyTest {
    private static final ProjectGraphStoreFacade FACADE = ProjectGraphStoreFacade.getInstance();

    @AfterEach
    void cleanup() {
        DatabaseManagerTestHook.finishBuild();
        DatabaseManager.clearAllData();
        Neo4jGraphSnapshotLoader.invalidateAll();
    }

    @Test
    void concurrentColdQueryLoadsShouldShareSameSnapshotInstance() throws Exception {
        String projectKey = "loader-concurrent-" + Long.toHexString(System.nanoTime());
        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            prepareReadyProject(projectKey, 131L);
            FACADE.write(projectKey, 30_000L, tx -> {
                tx.execute("MATCH (n) DETACH DELETE n");
                var meta = tx.createNode(Label.label("JAMeta"));
                meta.setProperty("key", "build_meta");
                meta.setProperty("build_seq", 131L);
                org.neo4j.graphdb.Node previous = null;
                for (int i = 1; i <= 160; i++) {
                    var node = methodNode(tx, i, "demo/Concurrent", "m" + i, "()V");
                    if (previous != null) {
                        var rel = previous.createRelationshipTo(node, RelationshipType.withName("CALLS_DIRECT"));
                        rel.setProperty("edge_id", 10_000L + i);
                        rel.setProperty("confidence", "high");
                        rel.setProperty("evidence", "unit:chain");
                        rel.setProperty("call_site_key", "demo/Concurrent#m" + (i - 1) + "#()V@" + i);
                        rel.setProperty("line_number", i);
                        rel.setProperty("call_index", i);
                        rel.setProperty("edge_semantic_flags", 0);
                    }
                    previous = node;
                }
                return null;
            });
            Neo4jGraphSnapshotLoader.invalidate(projectKey);

            Neo4jGraphSnapshotLoader loader = new Neo4jGraphSnapshotLoader();
            CountDownLatch ready = new CountDownLatch(6);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<GraphSnapshot>> futures = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    return loader.loadQuery(projectKey);
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            GraphSnapshot first = futures.get(0).get(30, TimeUnit.SECONDS);
            assertNotNull(first);
            assertEquals(131L, first.getBuildSeq());
            assertEquals(160, first.getNodeCount());
            for (Future<GraphSnapshot> future : futures) {
                assertSame(first, future.get(30, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
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
