/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.flow;

import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphDfsEngineTest {
    @Test
    void searchAllSourcesShouldIncludeMarkedSourceWithIncomingEdges() {
        Map<Long, GraphNode> nodes = new LinkedHashMap<>();
        nodes.put(1L, new GraphNode(1L, "method", 1, "app/Helper", "helper", "()V", "", -1, -1));
        nodes.put(2L, new GraphNode(
                2L,
                "method",
                1,
                "app/Controller",
                "entry",
                "()V",
                "",
                -1,
                -1,
                GraphNode.SOURCE_FLAG_ANY
        ));
        nodes.put(3L, new GraphNode(3L, "method", 1, "app/Sink", "sink", "()V", "", -1, -1));

        Map<Long, List<GraphEdge>> outgoing = new LinkedHashMap<>();
        Map<Long, List<GraphEdge>> incoming = new LinkedHashMap<>();
        addEdge(outgoing, incoming, new GraphEdge(11L, 1L, 2L, "CALLS_DIRECT", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(12L, 2L, 3L, "CALLS_DIRECT", "high", "unit", 0));

        GraphSnapshot snapshot = GraphSnapshot.of(1L, nodes, outgoing, incoming, Map.of());
        FlowOptions options = FlowOptions.builder()
                .fromSink(true)
                .searchAllSources(true)
                .depth(4)
                .sink("app/Sink", "sink", "()V")
                .build();

        List<DFSResult> results = new GraphDfsEngine().run(snapshot, options, null).results();

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(result ->
                result.getSource() != null
                        && result.getMethodList() != null
                        && result.getMethodList().size() == 2
                        && "app/Controller".equals(result.getSource().getClassReference().getName())
                        && "entry".equals(result.getSource().getName())));
    }

    @Test
    void preciseSearchShouldRespectSourceAndSinkJarIds() {
        Map<Long, GraphNode> nodes = new LinkedHashMap<>();
        nodes.put(1L, new GraphNode(1L, "method", 1, "dup/Source", "entry", "()V", "", -1, -1));
        nodes.put(2L, new GraphNode(2L, "method", 2, "dup/Source", "entry", "()V", "", -1, -1));
        nodes.put(3L, new GraphNode(3L, "method", 1, "dup/Sink", "sink", "()V", "", -1, -1));
        nodes.put(4L, new GraphNode(4L, "method", 2, "dup/Sink", "sink", "()V", "", -1, -1));

        Map<Long, List<GraphEdge>> outgoing = new LinkedHashMap<>();
        Map<Long, List<GraphEdge>> incoming = new LinkedHashMap<>();
        addEdge(outgoing, incoming, new GraphEdge(21L, 1L, 3L, "CALLS_DIRECT", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(22L, 2L, 4L, "CALLS_DIRECT", "high", "unit", 0));

        GraphSnapshot snapshot = GraphSnapshot.of(2L, nodes, outgoing, incoming, Map.of());
        FlowOptions options = FlowOptions.builder()
                .fromSink(false)
                .depth(4)
                .source("dup/Source", "entry", "()V")
                .sourceJarId(1)
                .sink("dup/Sink", "sink", "()V")
                .sinkJarId(1)
                .build();

        List<DFSResult> results = new GraphDfsEngine().run(snapshot, options, null).results();

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getSource().getJarId());
        assertEquals(1, results.get(0).getSink().getJarId());
    }

    private static void addEdge(Map<Long, List<GraphEdge>> outgoing,
                                Map<Long, List<GraphEdge>> incoming,
                                GraphEdge edge) {
        outgoing.computeIfAbsent(edge.getSrcId(), ignore -> new ArrayList<>()).add(edge);
        incoming.computeIfAbsent(edge.getDstId(), ignore -> new ArrayList<>()).add(edge);
    }
}
