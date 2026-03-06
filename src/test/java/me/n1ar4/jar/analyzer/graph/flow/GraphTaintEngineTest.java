/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.flow;

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.dfs.DFSEdge;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphTaintEngineTest {
    @Test
    void trackShouldPreserveJarScopedSourceAndSinkSelection() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, methodNode(1L, 1, "dup/Source", "entry", "()V"));
        nodes.put(2L, methodNode(2L, 2, "dup/Source", "entry", "()V"));
        nodes.put(3L, methodNode(3L, 1, "dup/Sink", "sink", "()V"));
        nodes.put(4L, methodNode(4L, 2, "dup/Sink", "sink", "()V"));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, new GraphEdge(11L, 1L, 3L, "CALLS_DIRECT", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(12L, 2L, 4L, "CALLS_DIRECT", "high", "unit", 0));

        GraphSnapshot snapshot = GraphSnapshot.of(1L, nodes, outgoing, incoming, Map.of());
        FlowOptions options = FlowOptions.builder()
                .fromSink(false)
                .depth(4)
                .source("dup/Source", "entry", "()V")
                .sourceJarId(1)
                .sink("dup/Sink", "sink", "()V")
                .sinkJarId(1)
                .build();

        List<TaintResult> results = new GraphTaintEngine().track(snapshot, options, null).results();

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getDfsResult().getSource().getJarId());
        assertEquals(1, results.get(0).getDfsResult().getSink().getJarId());
    }

    @Test
    void analyzeDfsResultsShouldPreserveOriginalModeWhenTruncated() {
        DFSResult dfs = new DFSResult();
        dfs.setMode(DFSResult.FROM_SINK_TO_SOURCE);
        dfs.setMethodList(List.of(methodHandle("demo/Sink", "sink", "()V", 1)));
        dfs.setEdges(List.of());

        List<TaintResult> results = new GraphTaintEngine()
                .analyzeDfsResults(List.of(dfs), 1000, 10, new AtomicBoolean(true), null)
                .results();

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getDfsResult().isTruncated());
        assertEquals(DFSResult.FROM_SINK_TO_SOURCE, results.get(0).getDfsResult().getMode());
    }

    @Test
    void analyzeDfsResultsShouldNotMarkImpreciseSegmentsAsVerified() {
        MethodReference.Handle source = methodHandle("demo/Source", "entry", "()V", 1);
        MethodReference.Handle sink = methodHandle("demo/Sink", "sink", "()V", 1);
        DFSEdge edge = new DFSEdge();
        edge.setFrom(source);
        edge.setTo(sink);
        edge.setType("reflection");
        edge.setConfidence("low");
        edge.setEvidence("unit");

        DFSResult dfs = new DFSResult();
        dfs.setMode(DFSResult.FROM_SOURCE_TO_SINK);
        dfs.setMethodList(List.of(source, sink));
        dfs.setEdges(List.of(edge));

        List<TaintResult> results = new GraphTaintEngine()
                .analyzeDfsResults(List.of(dfs), 1000, 10, new AtomicBoolean(false), null)
                .results();

        assertEquals(1, results.size());
        assertFalse(results.get(0).isSuccess());
        assertTrue(results.get(0).isLowConfidence());
        assertTrue(results.get(0).getTaintText().contains("taint path not fully proven"));
    }

    private static GraphNode methodNode(long id, int jarId, String clazz, String method, String desc) {
        return new GraphNode(id, "method", jarId, clazz, method, desc, "", -1, -1);
    }

    private static MethodReference.Handle methodHandle(String clazz, String method, String desc, int jarId) {
        return new MethodReference.Handle(new ClassReference.Handle(clazz, jarId), method, desc);
    }

    private static void addEdge(Map<Long, List<GraphEdge>> outgoing,
                                Map<Long, List<GraphEdge>> incoming,
                                GraphEdge edge) {
        outgoing.computeIfAbsent(edge.getSrcId(), ignore -> new ArrayList<>()).add(edge);
        incoming.computeIfAbsent(edge.getDstId(), ignore -> new ArrayList<>()).add(edge);
    }
}
