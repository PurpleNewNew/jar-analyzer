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

import java.util.Arrays;
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
        Assertions.assertTrue(graph.nodes().get(0).labels().contains("JANode"));
        Assertions.assertTrue(graph.edges().get(0).properties().containsKey("confidence"));
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
    void shouldProjectNodeMapsToGraph() {
        QueryResultProjector projector = new QueryResultProjector();
        QueryResult result = new QueryResult(
                List.of("n"),
                List.of(
                        List.of(Map.of(
                                "id", 901L,
                                "node_id", 1L,
                                "labels", List.of("JANode", "Method"),
                                "properties", Map.of(
                                        "kind", "method",
                                        "jar_id", 1,
                                        "class_name", "a/A",
                                        "method_name", "a",
                                        "method_desc", "()V"
                                ))),
                        List.of(Map.of(
                                "id", 902L,
                                "node_id", 2L,
                                "labels", List.of("JANode", "Method"),
                                "properties", Map.of(
                                        "kind", "method",
                                        "jar_id", 1,
                                        "class_name", "b/B",
                                        "method_name", "b",
                                        "method_desc", "()V"
                                )))
                ),
                List.of(),
                false
        );

        QueryFramePayload frame = projector.toFrame("frame-3", "MATCH (n:Method) RETURN n", result, 8, buildSnapshot());
        GraphFramePayload graph = frame.graph();
        Assertions.assertNotNull(graph);
        Assertions.assertEquals(2, graph.nodes().size());
        Assertions.assertEquals(0, graph.edges().size());
        Assertions.assertTrue(graph.nodes().stream().allMatch(node -> !node.properties().isEmpty()));
    }

    @Test
    void shouldProjectRelationshipMapsToGraph() {
        QueryResultProjector projector = new QueryResultProjector();
        QueryResult result = new QueryResult(
                List.of("r"),
                List.of(List.of(Map.of(
                        "id", 910L,
                        "edge_id", 100L,
                        "type", "CALLS_DIRECT",
                        "startNodeId", 1L,
                        "endNodeId", 2L,
                        "properties", Map.of(
                                "confidence", "high",
                                "evidence", "native-query"
                        )))),
                List.of(),
                false
        );

        QueryFramePayload frame = projector.toFrame("frame-rel-map", "MATCH ()-[r]->() RETURN r", result, 7, buildSnapshot());
        GraphFramePayload graph = frame.graph();
        Assertions.assertNotNull(graph);
        Assertions.assertEquals(2, graph.nodes().size());
        Assertions.assertEquals(1, graph.edges().size());
        Assertions.assertEquals("CALLS_DIRECT", graph.edges().get(0).relType());
        Assertions.assertEquals("native-query", graph.edges().get(0).properties().get("evidence"));
    }

    @Test
    void shouldProjectNestedPathMapsToGraph() {
        QueryResultProjector projector = new QueryResultProjector();
        Map<String, Object> path = Map.of(
                "nodes", List.of(
                        Map.of(
                                "id", 901L,
                                "node_id", 1L,
                                "labels", List.of("JANode"),
                                "properties", Map.of("kind", "method", "class_name", "a/A", "method_name", "a", "method_desc", "()V")
                        ),
                        Map.of(
                                "id", 902L,
                                "node_id", 2L,
                                "labels", List.of("JANode"),
                                "properties", Map.of("kind", "method", "class_name", "b/B", "method_name", "b", "method_desc", "()V")
                        ),
                        Map.of(
                                "id", 903L,
                                "node_id", 3L,
                                "labels", List.of("JANode"),
                                "properties", Map.of("kind", "method", "class_name", "c/C", "method_name", "c", "method_desc", "()V")
                        )
                ),
                "relationships", List.of(
                        Map.of(
                                "id", 1000L,
                                "edge_id", 100L,
                                "type", "CALLS_DIRECT",
                                "startNodeId", 1L,
                                "endNodeId", 2L,
                                "properties", Map.of("confidence", "high", "evidence", "path")
                        ),
                        Map.of(
                                "id", 1001L,
                                "edge_id", 101L,
                                "type", "CALLS_DIRECT",
                                "startNodeId", 2L,
                                "endNodeId", 3L,
                                "properties", Map.of("confidence", "high", "evidence", "path")
                        )
                ),
                "length", 2
        );
        QueryResult result = new QueryResult(
                List.of("payload"),
                List.of(List.of(Map.of("value", List.of(path)))),
                List.of(),
                false
        );

        QueryFramePayload frame = projector.toFrame("frame-path-map", "MATCH p=... RETURN {value:[p]}", result, 14, buildSnapshot());
        GraphFramePayload graph = frame.graph();
        Assertions.assertNotNull(graph);
        Assertions.assertEquals(3, graph.nodes().size());
        Assertions.assertEquals(2, graph.edges().size());
        Assertions.assertTrue(graph.edges().stream().anyMatch(edge -> edge.id() == 101L));
        Assertions.assertTrue(graph.nodes().stream().allMatch(node -> node.labels().contains("JANode")));
    }

    @Test
    void shouldHandleNullCellsWithoutFailingFrameProjection() {
        QueryResultProjector projector = new QueryResultProjector();
        QueryResult result = new QueryResult(
                List.of("src_id", "dst_id", "rel_type", "confidence", "evidence"),
                List.of(Arrays.asList(1L, 2L, null, null, null)),
                List.of(),
                false
        );

        QueryFramePayload frame = Assertions.assertDoesNotThrow(
                () -> projector.toFrame("frame-null", "MATCH ...", result, 5, buildSnapshot())
        );
        Assertions.assertNotNull(frame);
        Assertions.assertNotNull(frame.graph());
        Assertions.assertEquals(1, frame.graph().edges().size());
    }

    @Test
    void shouldRespectNodeBudgetWhenProjectingEdgeIds() {
        QueryResultProjector projector = new QueryResultProjector(32, 128);
        GraphSnapshot largeSnapshot = buildLinearSnapshot(40);
        StringBuilder edgeIdsBuilder = new StringBuilder();
        for (int i = 0; i < 39; i++) {
            if (i > 0) {
                edgeIdsBuilder.append(',');
            }
            edgeIdsBuilder.append(100 + i);
        }
        QueryResult result = new QueryResult(
                List.of("edge_ids"),
                List.of(List.of(edgeIdsBuilder.toString())),
                List.of(),
                false
        );

        QueryFramePayload frame = projector.toFrame("frame-budget", "CALL ja.path.from_to", result, 9, largeSnapshot);
        GraphFramePayload graph = frame.graph();
        Assertions.assertNotNull(graph);
        Assertions.assertTrue(graph.truncated());
        Assertions.assertEquals(32, graph.nodes().size());
        Assertions.assertTrue(graph.edges().size() > 0);
        Assertions.assertTrue(graph.edges().size() < 39);
        Assertions.assertTrue(graph.warnings().stream()
                .anyMatch(item -> item.contains("graph_render_truncated_by_budget")));
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

    private static GraphSnapshot buildLinearSnapshot(int nodeCount) {
        Map<Long, GraphNode> nodeMap = new HashMap<>();
        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        for (long nodeId = 1L; nodeId <= nodeCount; nodeId++) {
            nodeMap.put(nodeId, new GraphNode(nodeId, "method", 1, "c/C" + nodeId, "m" + nodeId, "()V", "", 1, 0));
        }
        for (long nodeId = 1L; nodeId < nodeCount; nodeId++) {
            long edgeId = 99L + nodeId;
            GraphEdge edge = new GraphEdge(edgeId, nodeId, nodeId + 1, "CALLS_DIRECT", "high", "unit", 182);
            outgoing.computeIfAbsent(nodeId, ignore -> new java.util.ArrayList<>()).add(edge);
            incoming.computeIfAbsent(nodeId + 1, ignore -> new java.util.ArrayList<>()).add(edge);
        }
        return GraphSnapshot.of(2L, nodeMap, outgoing, incoming, Map.of());
    }
}
