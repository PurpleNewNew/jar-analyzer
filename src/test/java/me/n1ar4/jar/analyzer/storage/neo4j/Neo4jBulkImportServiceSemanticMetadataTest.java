package me.n1ar4.jar.analyzer.storage.neo4j;

import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.rules.MethodSemanticFlags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Neo4jBulkImportServiceSemanticMetadataTest {
    private final String projectKey = "semantic-" + Long.toHexString(System.nanoTime());

    @AfterEach
    void cleanup() {
        Neo4jProjectStore.getInstance().deleteProjectStore(projectKey);
        Neo4jGraphSnapshotLoader.invalidate(projectKey);
    }

    @Test
    void shouldPersistMethodAndEdgeSemanticFlags() {
        MethodReference entry = methodRef(
                "demo/web/UserController",
                "index",
                "()Ljava/lang/String;",
                MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.SPRING_ENDPOINT
        );
        MethodReference mapper = methodRef(
                "demo/mapper/UserMapper",
                "findUsers",
                "()Ljava/util/List;",
                MethodSemanticFlags.MYBATIS_DYNAMIC_SQL
        );
        Set<MethodReference> methods = new LinkedHashSet<>(List.of(entry, mapper));
        List<ClassReference> classReferences = List.of(
                classRef("demo/web/UserController"),
                classRef("demo/mapper/UserMapper")
        );

        MethodCallKey edgeKey = MethodCallKey.of(entry.getHandle(), mapper.getHandle());
        MethodCallMeta edgeMeta = new MethodCallMeta(MethodCallMeta.TYPE_FRAMEWORK, MethodCallMeta.CONF_HIGH);
        edgeMeta.addEvidence(MethodCallMeta.TYPE_CALLBACK, MethodCallMeta.CONF_MEDIUM, "framework_callback");

        ProjectRuntimeSnapshot snapshot = new ProjectRuntimeSnapshot(
                ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION,
                1L,
                new ProjectRuntimeSnapshot.ProjectModelData(
                        ProjectModel.artifact(
                                Path.of("/tmp/jar-analyzer/semantic.jar"),
                                null,
                                List.of(Path.of("/tmp/jar-analyzer/semantic.jar")),
                                false
                        ).buildMode().name(),
                        "/tmp/jar-analyzer/semantic.jar",
                        "",
                        List.of(),
                        List.of("/tmp/jar-analyzer/semantic.jar"),
                        false
                ),
                List.of(),
                List.of(),
                classReferenceData(classReferences),
                methodReferenceData(methods),
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
                false,
                "taie:balanced",
                methods,
                Map.of(entry.getHandle(), Set.of(mapper.getHandle())),
                Map.of(edgeKey, edgeMeta),
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
                Map.of()
        );

        GraphSnapshot graph = new Neo4jGraphSnapshotLoader().load(projectKey);
        var entryNode = graph.getNodesByMethodSignatureView("demo/web/UserController", "index", "()Ljava/lang/String;")
                .stream()
                .findFirst()
                .orElse(null);
        var mapperNode = graph.getNodesByMethodSignatureView("demo/mapper/UserMapper", "findUsers", "()Ljava/util/List;")
                .stream()
                .findFirst()
                .orElse(null);
        assertNotNull(entryNode);
        assertNotNull(mapperNode);
        assertTrue((entryNode.getMethodSemanticFlags() & MethodSemanticFlags.SPRING_ENDPOINT) != 0);
        assertTrue((mapperNode.getMethodSemanticFlags() & MethodSemanticFlags.MYBATIS_DYNAMIC_SQL) != 0);

        GraphEdge edge = graph.getOutgoingView(entryNode.getNodeId()).stream()
                .filter(item -> item.getDstId() == mapperNode.getNodeId())
                .findFirst()
                .orElse(null);
        assertNotNull(edge);
        assertTrue((edge.getSemanticFlags() & MethodCallMeta.EVIDENCE_FRAMEWORK) != 0);
        assertTrue((edge.getSemanticFlags() & MethodCallMeta.EVIDENCE_CALLBACK) != 0);

        var database = Neo4jProjectStore.getInstance().database(projectKey);
        try (var tx = database.beginTx()) {
            var nodeRow = tx.execute("""
                    MATCH (m:Method {class_name:'demo/web/UserController', method_name:'index'})
                    RETURN m.method_semantic_flags AS flags
                    """).next();
            var edgeRow = tx.execute("""
                    MATCH (:Method {class_name:'demo/web/UserController', method_name:'index'})-[r]->(:Method {class_name:'demo/mapper/UserMapper', method_name:'findUsers'})
                    RETURN r.edge_semantic_flags AS flags
                    """).next();
            assertEquals(entry.getSemanticFlags(), ((Number) nodeRow.get("flags")).intValue());
            assertEquals(edgeMeta.getEvidenceBits(), ((Number) edgeRow.get("flags")).intValue());
            tx.commit();
        }
    }

    private static MethodReference methodRef(String className, String methodName, String methodDesc, int semanticFlags) {
        return new MethodReference(
                new ClassReference.Handle(className, 1),
                methodName,
                methodDesc,
                false,
                Set.of(),
                1,
                10,
                "app.jar",
                1,
                semanticFlags
        );
    }

    private static ClassReference classRef(String className) {
        return new ClassReference(
                61,
                0x0021,
                className,
                "java/lang/Object",
                List.of(),
                false,
                List.of(),
                Set.of(),
                "app.jar",
                1
        );
    }

    private static List<ProjectRuntimeSnapshot.ClassReferenceData> classReferenceData(List<ClassReference> refs) {
        return refs.stream()
                .map(ref -> new ProjectRuntimeSnapshot.ClassReferenceData(
                        ref.getVersion(),
                        ref.getAccess(),
                        ref.getName(),
                        ref.getSuperClass(),
                        ref.getInterfaces(),
                        ref.isInterface(),
                        List.of(),
                        List.of(),
                        ref.getJarName(),
                        ref.getJarId()
                ))
                .toList();
    }

    private static List<ProjectRuntimeSnapshot.MethodReferenceData> methodReferenceData(Set<MethodReference> refs) {
        return refs.stream()
                .map(ref -> new ProjectRuntimeSnapshot.MethodReferenceData(
                        new ProjectRuntimeSnapshot.ClassHandleData(ref.getClassReference().getName(), ref.getJarId()),
                        List.of(),
                        ref.getName(),
                        ref.getDesc(),
                        ref.getAccess(),
                        ref.isStatic(),
                        ref.getLineNumber(),
                        ref.getJarName(),
                        ref.getJarId(),
                        ref.getSemanticFlags()
                ))
                .toList();
    }
}
