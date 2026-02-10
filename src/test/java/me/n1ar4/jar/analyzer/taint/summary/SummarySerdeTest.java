/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.taint.summary;

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SummarySerdeTest {
    @Test
    public void testMethodSummarySerdeRoundTrip() {
        MethodSummary summary = new MethodSummary();
        summary.setUnknown(false);
        summary.setHasSideEffect(true);
        summary.addEdge(new FlowEdge(
                FlowPort.thisPort(),
                FlowPort.ret(),
                new HashSet<>(Arrays.asList("T:java/lang/String")),
                "high"));
        MethodReference.Handle callee = new MethodReference.Handle(
                new ClassReference.Handle("a/b/C"),
                "m",
                "(I)V");
        summary.addCallFlow(new CallFlow(
                FlowPort.param(0),
                callee,
                FlowPort.param(0),
                new HashSet<>(Arrays.asList("x")),
                "low"));

        String encoded = MethodSummarySerde.toCacheValue(summary);
        assertNotNull(encoded);
        MethodSummary decoded = MethodSummarySerde.fromCacheValue(encoded);
        assertNotNull(decoded);

        assertEquals(summary.isUnknown(), decoded.isUnknown());
        assertEquals(summary.hasSideEffect(), decoded.hasSideEffect());
        assertEquals(summary.getEdges(), decoded.getEdges());
        assertEquals(summary.getCallFlows(), decoded.getCallFlows());
    }

    @Test
    public void testReachabilitySerdeRoundTrip() {
        Set<MethodReference.Handle> toSink = new HashSet<>();
        toSink.add(new MethodReference.Handle(new ClassReference.Handle("a/A"), "m", "()V"));
        Set<MethodReference.Handle> fromSource = new HashSet<>();
        fromSource.add(new MethodReference.Handle(new ClassReference.Handle("b/B"), "n", "(Ljava/lang/String;)V"));
        ReachabilityIndex index = new ReachabilityIndex(toSink, fromSource);

        String encoded = ReachabilitySerde.toCacheValue(index);
        assertNotNull(encoded);
        ReachabilityIndex decoded = ReachabilitySerde.fromCacheValue(encoded);
        assertNotNull(decoded);

        assertEquals(index.getReachableToSink(), decoded.getReachableToSink());
        assertEquals(index.getReachableFromSource(), decoded.getReachableFromSource());
    }
}

