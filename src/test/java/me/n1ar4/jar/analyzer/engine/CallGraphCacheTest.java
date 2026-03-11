/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.engine;

import me.n1ar4.jar.analyzer.entity.MethodCallResult;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CallGraphCacheTest {
    @Test
    public void shouldKeepDistinctCallersAcrossDifferentJarIds() {
        MethodCallResult edge1 = edge(
                "a/Caller", "run", "()V", 11,
                "a/Sink", "sink", "()V", 1
        );
        MethodCallResult edge2 = edge(
                "a/Caller", "run", "()V", 22,
                "a/Sink", "sink", "()V", 1
        );

        CallGraphCache cache = CallGraphCache.build(Arrays.asList(edge1, edge2));
        ArrayList<MethodResult> callers = cache.getCallers("a/Sink", "sink", "()V");

        assertEquals(2, callers.size(), "caller variants from different jars should be preserved");
        Set<Integer> callerJarIds = new HashSet<>();
        for (MethodResult caller : callers) {
            callerJarIds.add(caller.getJarId());
        }
        assertTrue(callerJarIds.contains(11));
        assertTrue(callerJarIds.contains(22));

        MethodResult scopedCallee = new MethodResult();
        scopedCallee.setClassName("a/Sink");
        scopedCallee.setMethodName("sink");
        scopedCallee.setMethodDesc("()V");
        scopedCallee.setJarId(1);
        ArrayList<MethodResult> scopedCallers = cache.getCallers(scopedCallee);
        assertEquals(2, scopedCallers.size());
    }

    @Test
    public void shouldKeepDistinctCalleesAcrossDifferentJarIdsAndCountScopedEdges() {
        MethodCallResult edge1 = edge(
                "a/Entry", "go", "()V", 9,
                "a/Target", "hit", "()V", 101
        );
        MethodCallResult edge2 = edge(
                "a/Entry", "go", "()V", 9,
                "a/Target", "hit", "()V", 202
        );

        CallGraphCache cache = CallGraphCache.build(Arrays.asList(edge1, edge2));
        ArrayList<MethodResult> callees = cache.getCallees("a/Entry", "go", "()V");

        assertEquals(2, callees.size(), "callee variants from different jars should be preserved");
        Set<Integer> calleeJarIds = new HashSet<>();
        for (MethodResult callee : callees) {
            calleeJarIds.add(callee.getJarId());
        }
        assertTrue(calleeJarIds.contains(101));
        assertTrue(calleeJarIds.contains(202));

        MethodResult scopedCaller = new MethodResult();
        scopedCaller.setClassName("a/Entry");
        scopedCaller.setMethodName("go");
        scopedCaller.setMethodDesc("()V");
        scopedCaller.setJarId(9);
        ArrayList<MethodResult> scopedCallees = cache.getCallees(scopedCaller);
        assertEquals(2, scopedCallees.size());

        assertEquals(2, cache.getEdgeCount(), "edge count should use jar-scoped edge identity");
    }

    @Test
    public void shouldIndexCallEdgeRowsWithoutScanningWholeGraphAgain() {
        MethodCallResult edge1 = edge(
                "a/Entry", "go", "()V", 9,
                "a/Target", "hit", "()V", 101
        );
        MethodCallResult edge2 = edge(
                "a/Entry", "go", "()V", 9,
                "a/Target", "miss", "()V", 202
        );

        CallGraphCache cache = CallGraphCache.build(Arrays.asList(edge1, edge2));

        ArrayList<MethodCallResult> callerEdges = cache.getCallEdgesByCaller("a/Entry", "go", "()V", 9);
        ArrayList<MethodCallResult> calleeEdges = cache.getCallEdgesByCallee("a/Target", "hit", "()V", 101);
        ArrayList<MethodCallResult> wildcardEdges = cache.getCallEdgesByCaller("a/Entry", "go", "*", null);

        assertEquals(2, callerEdges.size());
        assertEquals(1, calleeEdges.size());
        assertEquals(2, wildcardEdges.size());
        assertEquals("hit", calleeEdges.get(0).getCalleeMethodName());
    }

    @Test
    public void shouldBuildDirectlyFromGraphSnapshot() {
        GraphSnapshot snapshot = GraphSnapshot.of(
                7L,
                Map.of(
                        1L, methodNode(1L, "a/Entry", "go", "()V", 9),
                        2L, methodNode(2L, "a/Target", "hit", "()V", 101),
                        3L, methodNode(3L, "a/Target", "miss", "()V", 202)
                ),
                Map.of(
                        1L, List.of(
                                edge(11L, 1L, 2L, "CALLS_DIRECT", "high", "direct"),
                                edge(12L, 1L, 3L, "CALLS_PTA", "medium", "pta:ctx")
                        )
                ),
                Map.of(
                        2L, List.of(edge(11L, 1L, 2L, "CALLS_DIRECT", "high", "direct")),
                        3L, List.of(edge(12L, 1L, 3L, "CALLS_PTA", "medium", "pta:ctx"))
                ),
                Map.of()
        );

        CallGraphCache cache = CallGraphCache.build(snapshot, Map.of(
                9, "caller-9.jar",
                101, "callee-101.jar",
                202, "callee-202.jar"
        ));

        assertEquals(2, cache.getEdgeCount());
        assertEquals(3, cache.getMethodCount());
        assertEquals(2, cache.getCallees("a/Entry", "go", "()V", 9).size());
        assertEquals("pta", cache.getCallEdgesByCallee("a/Target", "miss", "()V", 202).get(0).getEdgeType());
    }

    private static MethodCallResult edge(String callerClass,
                                         String callerMethod,
                                         String callerDesc,
                                         int callerJarId,
                                         String calleeClass,
                                         String calleeMethod,
                                         String calleeDesc,
                                         int calleeJarId) {
        MethodCallResult edge = new MethodCallResult();
        edge.setCallerClassName(callerClass);
        edge.setCallerMethodName(callerMethod);
        edge.setCallerMethodDesc(callerDesc);
        edge.setCallerJarId(callerJarId);
        edge.setCallerJarName("caller-" + callerJarId + ".jar");
        edge.setCalleeClassName(calleeClass);
        edge.setCalleeMethodName(calleeMethod);
        edge.setCalleeMethodDesc(calleeDesc);
        edge.setCalleeJarId(calleeJarId);
        edge.setCalleeJarName("callee-" + calleeJarId + ".jar");
        edge.setEdgeType("direct");
        edge.setEdgeConfidence("high");
        edge.setEdgeEvidence("direct:high");
        return edge;
    }

    private static GraphNode methodNode(long id,
                                        String className,
                                        String methodName,
                                        String methodDesc,
                                        int jarId) {
        return new GraphNode(id, "method", jarId, className, methodName, methodDesc, "", -1, -1, 0, 0);
    }

    private static GraphEdge edge(long edgeId,
                                  long srcId,
                                  long dstId,
                                  String relType,
                                  String confidence,
                                  String evidence) {
        return new GraphEdge(edgeId, srcId, dstId, relType, confidence, evidence, "", -1, "", -1, -1, 0);
    }
}
