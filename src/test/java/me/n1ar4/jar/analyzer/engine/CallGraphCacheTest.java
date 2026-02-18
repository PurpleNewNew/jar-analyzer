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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
}
