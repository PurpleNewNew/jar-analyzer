package me.n1ar4.jar.analyzer.storage.neo4j;

import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Label;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Neo4jBulkImportServiceBuildMetaTest {
    private final String projectKey = "meta-" + Long.toHexString(System.nanoTime());

    @AfterEach
    void cleanup() {
        Neo4jProjectStore.getInstance().deleteProjectStore(projectKey);
        Neo4jGraphSnapshotLoader.invalidate(projectKey);
    }

    @Test
    void shouldImportBuildMetaWithCsvPayload() {
        ClassReference classRef = new ClassReference(
                61,
                0x0021,
                "demo/MetaController",
                "java/lang/Object",
                List.of(),
                false,
                List.of(),
                Set.of(),
                "meta.jar",
                1
        );
        MethodReference methodRef = new MethodReference(
                new ClassReference.Handle("demo/MetaController", 1),
                "run",
                "()V",
                false,
                Set.of(),
                1,
                10,
                "meta.jar",
                1
        );
        Set<MethodReference> methods = new LinkedHashSet<>(List.of(methodRef));
        List<ClassReference> classes = List.of(classRef);
        ProjectRuntimeSnapshot snapshot = new ProjectRuntimeSnapshot(
                ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION,
                7L,
                new ProjectRuntimeSnapshot.ProjectModelData(
                        ProjectModel.artifact(
                                Path.of("/tmp/jar-analyzer/meta.jar"),
                                null,
                                List.of(Path.of("/tmp/jar-analyzer/meta.jar")),
                                false
                        ).buildMode().name(),
                        "/tmp/jar-analyzer/meta.jar",
                        "",
                        List.of(),
                        List.of("/tmp/jar-analyzer/meta.jar"),
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
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );

        new Neo4jBulkImportService().replaceFromAnalysis(
                projectKey,
                7L,
                false,
                "bytecode:balanced-v1",
                methods,
                Map.of(),
                Map.of(),
                new Neo4jBulkImportService.GraphPayloadData(
                        List.of(),
                        classes,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of()
                ),
                () -> snapshot,
                Map.of(
                        "call_graph_engine", "bytecode-mainline+pta-refine",
                        "analysis_profile", "balanced",
                        "target_jar_count", 1,
                        "library_jar_count", 0,
                        "sdk_entry_count", 0,
                        "explicit_entry_method_count", 0
                ),
                null
        );

        var database = Neo4jProjectStore.getInstance().database(projectKey);
        try (var tx = database.beginTx();
             var it = tx.findNodes(Label.label("JAMeta"), "key", "build_meta")) {
            assertTrue(it.hasNext());
            var meta = it.next();
            assertEquals(7L, ((Number) meta.getProperty("build_seq")).longValue());
            assertEquals(false, meta.getProperty("quick_mode"));
            assertEquals("bytecode:balanced-v1", meta.getProperty("call_graph_mode"));
            assertEquals("bytecode-mainline+pta-refine", meta.getProperty("call_graph_engine"));
            assertEquals("balanced", meta.getProperty("analysis_profile"));
            assertEquals(2, ((Number) meta.getProperty("node_count")).intValue());
            assertEquals(1L, ((Number) meta.getProperty("edge_count")).longValue());
            assertEquals(1, ((Number) meta.getProperty("target_jar_count")).intValue());
            tx.commit();
        }
    }
}
