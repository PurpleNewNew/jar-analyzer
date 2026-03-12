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

import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.rules.SinkRuleRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Neo4jBulkImportServiceAliasEdgeTest {
    private static final String MODEL_PROP = "jar.analyzer.rules.model.path";
    private static final String SOURCE_PROP = "jar.analyzer.rules.source.path";
    private static final String SINK_PROP = "jar.analyzer.rules.sink.path";

    @TempDir
    Path tempDir;

    private final String projectKey = "alias-" + Long.toHexString(System.nanoTime());

    @AfterEach
    void cleanup() {
        Neo4jProjectStore.getInstance().deleteProjectStore(projectKey);
        Neo4jGraphSnapshotLoader.invalidate(projectKey);
    }

    @Test
    void shouldPersistAliasEdgesDerivedFromAdditionalFlowRules() throws Exception {
        Path model = tempDir.resolve("model.json");
        Path source = tempDir.resolve("source.json");
        Path sink = tempDir.resolve("sink.json");
        String oldModelProp = System.getProperty(MODEL_PROP);
        String oldSourceProp = System.getProperty(SOURCE_PROP);
        String oldSinkProp = System.getProperty(SINK_PROP);
        try {
            Files.writeString(model, """
                    {
                      "summaryModel": [],
                      "additionalTaintSteps": [
                        {
                          "className": "demo/Wrapper",
                          "methodName": "wrap",
                          "methodDesc": "(Ljava/lang/String;)Ljava/lang/String;",
                          "flows": [
                            {
                              "from": "arg0",
                              "to": "return"
                            }
                          ]
                        }
                      ],
                      "sanitizerModel": []
                    }
                    """, StandardCharsets.UTF_8);
            Files.writeString(source, "{\"sourceAnnotations\":[],\"sourceModel\":[]}", StandardCharsets.UTF_8);
            Files.writeString(sink, "{\"name\":\"alias-test\",\"levels\":{}}", StandardCharsets.UTF_8);

            System.setProperty(MODEL_PROP, model.toString());
            System.setProperty(SOURCE_PROP, source.toString());
            System.setProperty(SINK_PROP, sink.toString());
            SinkRuleRegistry.reload();
            ModelRegistry.reload();

            MethodReference caller = methodRef("app/Entry", "call", "()V", 1, "app.jar");
            MethodReference wrapper = methodRef("demo/Wrapper", "wrap", "(Ljava/lang/String;)Ljava/lang/String;", 1, "app.jar");
            Set<MethodReference> methods = new LinkedHashSet<>(List.of(caller, wrapper));
            List<ClassReference> classReferences = List.of(
                    classRef("app/Entry", "java/lang/Object", List.of(), false, 1, "app.jar"),
                    classRef("demo/Wrapper", "java/lang/Object", List.of(), false, 1, "app.jar")
            );

            ProjectRuntimeSnapshot snapshot = new ProjectRuntimeSnapshot(
                    ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION,
                    1L,
                    new ProjectRuntimeSnapshot.ProjectModelData(
                            ProjectModel.artifact(
                                    tempDir.resolve("alias.jar"),
                                    null,
                                    List.of(tempDir.resolve("alias.jar")),
                                    false
                            ).buildMode().name(),
                            tempDir.resolve("alias.jar").toString(),
                            "",
                            List.of(),
                            List.of(tempDir.resolve("alias.jar").toString()),
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
                    1L,
                    "oracle-harness:balanced",
                    methods,
                    Map.of(caller.getHandle(), Set.of(wrapper.getHandle())),
                    Map.of(),
                    new Neo4jBulkImportService.GraphPayloadData(
                            List.of(),
                            classReferences,
                            List.of(),
                            List.of(),
                            List.of(),
                            List.of(),
                            List.of(),
                            Map.of()
                    ),
                    () -> snapshot,
                    Map.of(),
                    null
            );

            var database = Neo4jProjectStore.getInstance().database(projectKey);
            try (var tx = database.beginTx()) {
                Map<String, Object> row = tx.execute("""
                        MATCH (:Method {class_name:'app/Entry'})-[r:ALIAS]->(:Method {class_name:'demo/Wrapper'})
                        RETURN count(r) AS total,
                               collect(r.alias_kind) AS aliasKinds,
                               collect(r.confidence) AS confidences,
                               collect(r.evidence) AS evidences
                        """).next();
                assertEquals(1L, ((Number) row.get("total")).longValue());
                assertEquals(List.of("container_wrapper"), row.get("aliasKinds"));
                assertEquals(List.of("medium"), row.get("confidences"));
                assertEquals(List.of("additional_flow:container_wrapper"), row.get("evidences"));
                tx.commit();
            }

            GraphSnapshot graph = new Neo4jGraphSnapshotLoader().load(projectKey);
            GraphNode callerNode = graph.getNodesByMethodSignatureView("app/Entry", "call", "()V").get(0);
            List<GraphEdge> aliasEdges = graph.getOutgoingView(callerNode.getNodeId()).stream()
                    .filter(edge -> "ALIAS".equals(edge.getRelType()))
                    .toList();
            assertEquals(1, aliasEdges.size());
            assertEquals("container_wrapper", aliasEdges.get(0).getAliasKind());
            assertFalse(aliasEdges.get(0).getEvidence().isBlank());
        } finally {
            restore(MODEL_PROP, oldModelProp);
            restore(SOURCE_PROP, oldSourceProp);
            restore(SINK_PROP, oldSinkProp);
            SinkRuleRegistry.reload();
            ModelRegistry.reload();
        }
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
            return;
        }
        System.setProperty(key, value);
    }

    private static MethodReference methodRef(String className,
                                             String methodName,
                                             String methodDesc,
                                             int jarId,
                                             String jarName) {
        return new MethodReference(
                new ClassReference.Handle(className, jarId),
                methodName,
                methodDesc,
                false,
                Set.of(),
                1,
                10,
                jarName,
                jarId
        );
    }

    private static ClassReference classRef(String className,
                                           String superClass,
                                           List<String> interfaces,
                                           boolean isInterface,
                                           int jarId,
                                           String jarName) {
        return new ClassReference(
                61,
                isInterface ? 0x0201 : 0x0021,
                className,
                superClass,
                interfaces,
                isInterface,
                List.of(),
                Set.of(),
                jarName,
                jarId
        );
    }
}
