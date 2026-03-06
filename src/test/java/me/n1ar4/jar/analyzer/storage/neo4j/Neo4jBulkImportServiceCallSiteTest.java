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
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
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

class Neo4jBulkImportServiceCallSiteTest {
    private final String projectKey = "callsite-" + Long.toHexString(System.nanoTime());

    @AfterEach
    void cleanup() {
        Neo4jProjectStore.getInstance().deleteProjectStore(projectKey);
        Neo4jGraphSnapshotLoader.invalidate(projectKey);
    }

    @Test
    void shouldPreferPreciseCallGraphTargetWhenCallSiteCalleeIsDuplicatedAcrossJars() {
        MethodReference caller = methodRef("dup/Caller", "call", "()V", 1, "app.jar");
        MethodReference calleeJarOne = methodRef("dup/Shared", "target", "()V", 1, "app.jar");
        MethodReference calleeJarTwo = methodRef("dup/Shared", "target", "()V", 2, "lib.jar");
        Set<MethodReference> methods = new LinkedHashSet<>(List.of(caller, calleeJarOne, calleeJarTwo));

        CallSiteEntity site = new CallSiteEntity();
        site.setCallerClassName("dup/Caller");
        site.setCallerMethodName("call");
        site.setCallerMethodDesc("()V");
        site.setCalleeOwner("dup/Shared");
        site.setCalleeMethodName("target");
        site.setCalleeMethodDesc("()V");
        site.setJarId(1);
        site.setOpCode(182);
        site.setLineNumber(12);
        site.setCallIndex(0);
        site.setCallSiteKey(CallSiteKeyUtil.buildCallSiteKey(site));

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
                Map.of(caller.getHandle(), Set.of(calleeJarTwo.getHandle())),
                Map.of(),
                List.of(site),
                snapshot,
                Map.of()
        );

        GraphSnapshot graph = new Neo4jGraphSnapshotLoader().load(projectKey);
        GraphNode callSiteNode = graph.getNodesByKindView("callsite").stream()
                .filter(node -> site.getCallSiteKey().equals(node.getCallSiteKey()))
                .findFirst()
                .orElse(null);
        assertNotNull(callSiteNode);

        GraphEdge calleeEdge = graph.getOutgoingView(callSiteNode.getNodeId()).stream()
                .filter(edge -> "CALLSITE_TO_CALLEE".equals(edge.getRelType()))
                .findFirst()
                .orElse(null);
        assertNotNull(calleeEdge);

        GraphNode calleeNode = graph.getNode(calleeEdge.getDstId());
        assertNotNull(calleeNode);
        assertEquals("dup/Shared", calleeNode.getClassName());
        assertEquals("target", calleeNode.getMethodName());
        assertEquals(2, calleeNode.getJarId());
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
}
