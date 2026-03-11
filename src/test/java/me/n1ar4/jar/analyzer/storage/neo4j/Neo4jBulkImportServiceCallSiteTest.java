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

import me.n1ar4.jar.analyzer.core.CallSiteKeyUtil;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
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

class Neo4jBulkImportServiceCallSiteTest {
    private final String projectKey = "callsite-" + Long.toHexString(System.nanoTime());

    @AfterEach
    void cleanup() {
        Neo4jProjectStore.getInstance().deleteProjectStore(projectKey);
        Neo4jGraphSnapshotLoader.invalidate(projectKey);
    }

    @Test
    void shouldPersistCallSiteMetadataOnRelationshipsWhileKeepingMethodOnlyGraph() {
        MethodReference caller = methodRef("dup/Caller", "call", "()V", 1, "app.jar");
        MethodReference calleeJarOne = methodRef("dup/Shared", "target", "()V", 1, "app.jar");
        MethodReference calleeJarTwo = methodRef("dup/Shared", "target", "()V", 2, "lib.jar");
        Set<MethodReference> methods = new LinkedHashSet<>(List.of(caller, calleeJarOne, calleeJarTwo));
        List<ClassReference> classReferences = List.of(
                classRef("dup/Base", "java/lang/Object", List.of(), false, 1, "app.jar"),
                classRef("dup/Contract", "java/lang/Object", List.of(), true, 1, "app.jar"),
                classRef("dup/Caller", "dup/Base", List.of("dup/Contract"), false, 1, "app.jar"),
                classRef("dup/Shared", "dup/Base", List.of(), false, 1, "app.jar"),
                classRef("dup/Shared", "java/lang/Object", List.of(), false, 2, "lib.jar")
        );

        CallSiteEntity first = callSite(12, 0);
        CallSiteEntity second = callSite(18, 1);

        ProjectRuntimeSnapshot snapshot = new ProjectRuntimeSnapshot(
                ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION,
                1L,
                new ProjectRuntimeSnapshot.ProjectModelData(
                        ProjectModel.artifact(
                                Path.of("/tmp/jar-analyzer/callsite.jar"),
                                null,
                                List.of(Path.of("/tmp/jar-analyzer/callsite.jar")),
                                false
                        ).buildMode().name(),
                        "/tmp/jar-analyzer/callsite.jar",
                        "",
                        List.of(),
                        List.of("/tmp/jar-analyzer/callsite.jar"),
                        false
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Map.of(),
                List.of(),
                List.of(
                        callSiteData(first),
                        callSiteData(second)
                ),
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
                "oracle-harness:balanced",
                methods,
                Map.of(caller.getHandle(), Set.of(calleeJarTwo.getHandle())),
                Map.of(),
                new Neo4jBulkImportService.GraphPayloadData(
                        List.of(first, second),
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
        assertTrue(graph.getNodesByKindView("callsite").isEmpty());

        GraphNode callerNode = graph.getNodesByMethodSignatureView("dup/Caller", "call", "()V").stream()
                .findFirst()
                .orElse(null);
        GraphNode calleeNode = graph.getNodesByMethodSignatureView("dup/Shared", "target", "()V").stream()
                .filter(node -> node.getJarId() == 2)
                .findFirst()
                .orElse(null);
        assertNotNull(callerNode);
        assertNotNull(calleeNode);
        assertEquals("dup/Caller", callerNode == null ? null : callerNode.getClassName());
        assertEquals(2, calleeNode == null ? -1 : calleeNode.getJarId());
        List<me.n1ar4.jar.analyzer.graph.store.GraphEdge> edges = graph.getOutgoingView(callerNode.getNodeId()).stream()
                .filter(edge -> edge.getDstId() == calleeNode.getNodeId()
                        && "CALLS_DIRECT".equals(edge.getRelType()))
                .sorted(java.util.Comparator
                        .comparingInt(me.n1ar4.jar.analyzer.graph.store.GraphEdge::getLineNumber)
                        .thenComparingInt(me.n1ar4.jar.analyzer.graph.store.GraphEdge::getCallIndex))
                .toList();
        assertEquals(2, edges.size());
        assertEquals(List.of(first.getCallSiteKey(), second.getCallSiteKey()),
                edges.stream().map(me.n1ar4.jar.analyzer.graph.store.GraphEdge::getCallSiteKey).toList());
        assertEquals(List.of(12, 18),
                edges.stream().map(me.n1ar4.jar.analyzer.graph.store.GraphEdge::getLineNumber).toList());
        assertEquals(List.of(0, 1),
                edges.stream().map(me.n1ar4.jar.analyzer.graph.store.GraphEdge::getCallIndex).toList());

        var database = Neo4jProjectStore.getInstance().database(projectKey);
        try (var tx = database.beginTx()) {
            long classCount = ((Number) tx.execute("MATCH (c:Class) RETURN count(c) AS total")
                    .next().get("total")).longValue();
            long hasCount = ((Number) tx.execute("MATCH (:Class)-[r:HAS]->(:Method) RETURN count(r) AS total")
                    .next().get("total")).longValue();
            long extendCount = ((Number) tx.execute("MATCH (:Class)-[r:EXTEND]->(:Class) RETURN count(r) AS total")
                    .next().get("total")).longValue();
            long interfacesCount = ((Number) tx.execute("MATCH (:Class)-[r:INTERFACES]->(:Class) RETURN count(r) AS total")
                    .next().get("total")).longValue();
            assertEquals(5L, classCount);
            assertEquals(3L, hasCount);
            assertEquals(2L, extendCount);
            assertEquals(1L, interfacesCount);
            tx.commit();
        }
    }

    private static ProjectRuntimeSnapshot.CallSiteData callSiteData(CallSiteEntity site) {
        return new ProjectRuntimeSnapshot.CallSiteData(
                site.getCallerClassName(),
                site.getCallerMethodName(),
                site.getCallerMethodDesc(),
                site.getCalleeOwner(),
                site.getCalleeMethodName(),
                site.getCalleeMethodDesc(),
                site.getOpCode(),
                site.getLineNumber(),
                site.getCallIndex(),
                site.getReceiverType(),
                site.getJarId(),
                site.getCallSiteKey()
        );
    }

    private static CallSiteEntity callSite(int lineNumber, int callIndex) {
        CallSiteEntity site = new CallSiteEntity();
        site.setCallerClassName("dup/Caller");
        site.setCallerMethodName("call");
        site.setCallerMethodDesc("()V");
        site.setCalleeOwner("dup/Shared");
        site.setCalleeMethodName("target");
        site.setCalleeMethodDesc("()V");
        site.setJarId(1);
        site.setOpCode(182);
        site.setLineNumber(lineNumber);
        site.setCallIndex(callIndex);
        site.setCallSiteKey(CallSiteKeyUtil.buildCallSiteKey(site));
        return site;
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
