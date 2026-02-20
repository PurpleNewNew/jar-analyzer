/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.cypher;

import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.GraphFramePayload;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.QueryFramePayload;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class QueryResultProjectorTest {
    @Test
    void shouldProjectRelationshipMatchToGraph() {
        QueryResultProjector projector = new QueryResultProjector();
        QueryResult result = new QueryResult(
                List.of("src_id", "src_kind", "src_class", "src_method", "rel_type", "dst_id", "dst_kind", "dst_class", "dst_method", "confidence", "evidence"),
                List.of(List.of(1L, "method", "a/A", "a", "CALLS_DIRECT", 2L, "method", "b/B", "b", "high", "unit")),
                List.of(),
                false
        );

        QueryFramePayload frame = projector.toFrame("frame-1", "MATCH ...", result, 12, buildSnapshot());
        GraphFramePayload graph = frame.graph();
        Assertions.assertNotNull(graph);
        Assertions.assertEquals(2, graph.nodes().size());
        Assertions.assertEquals(1, graph.edges().size());
        Assertions.assertEquals("CALLS_DIRECT", graph.edges().get(0).relType());
    }

    @Test
    void shouldProjectPathRowsFromNodeAndEdgeIds() {
        QueryResultProjector projector = new QueryResultProjector();
        QueryResult result = new QueryResult(
                List.of("path_id", "hop", "node_ids", "edge_ids", "score", "confidence", "evidence"),
                List.of(List.of(1, 2, "1,2,3", "100,101", 0.5, "high", "ja.path.from_to")),
                List.of(),
                false
        );

        QueryFramePayload frame = projector.toFrame("frame-2", "CALL ja.path.from_to", result, 33, buildSnapshot());
        GraphFramePayload graph = frame.graph();
        Assertions.assertNotNull(graph);
        Assertions.assertEquals(3, graph.nodes().size());
        Assertions.assertEquals(2, graph.edges().size());
        Assertions.assertTrue(graph.edges().stream().anyMatch(edge -> edge.id() == 100L));
    }

    @Test
    void shouldKeepNodeOnlyResultAsTableTextByDefault() {
        QueryResultProjector projector = new QueryResultProjector();
        QueryResult result = new QueryResult(
                List.of("node_id", "kind", "class_name", "method_name"),
                List.of(
                        List.of(1L, "method", "a/A", "a"),
                        List.of(2L, "method", "b/B", "b")
                ),
                List.of(),
                false
        );

        QueryFramePayload frame = projector.toFrame("frame-3", "MATCH (n:Method) RETURN n", result, 8, buildSnapshot());
        GraphFramePayload graph = frame.graph();
        Assertions.assertNotNull(graph);
        Assertions.assertEquals(0, graph.nodes().size());
        Assertions.assertEquals(0, graph.edges().size());
    }

    private static GraphSnapshot buildSnapshot() {
        Map<Long, GraphNode> nodeMap = new HashMap<>();
        nodeMap.put(1L, new GraphNode(1L, "method", 1, "a/A", "a", "()V", "", 1, 0));
        nodeMap.put(2L, new GraphNode(2L, "method", 1, "b/B", "b", "()V", "", 1, 0));
        nodeMap.put(3L, new GraphNode(3L, "method", 1, "c/C", "c", "()V", "", 1, 0));

        GraphEdge edge100 = new GraphEdge(100L, 1L, 2L, "CALLS_DIRECT", "high", "unit", 182);
        GraphEdge edge101 = new GraphEdge(101L, 2L, 3L, "CALLS_DIRECT", "high", "unit", 182);

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        outgoing.put(1L, List.of(edge100));
        outgoing.put(2L, List.of(edge101));

        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        incoming.put(2L, List.of(edge100));
        incoming.put(3L, List.of(edge101));

        return GraphSnapshot.of(1L, nodeMap, outgoing, incoming, Map.of());
    }
}
