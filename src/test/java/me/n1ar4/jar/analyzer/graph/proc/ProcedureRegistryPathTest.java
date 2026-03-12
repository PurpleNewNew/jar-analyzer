/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.proc;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.graph.query.QueryOptions;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.support.PrunedFlowFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcedureRegistryPathTest {
    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
    }

    @Test
    void shortestShouldUseBidirectionalBoundedSearch() {
        GraphSnapshot snapshot = buildSimpleSnapshot();
        ProcedureRegistry registry = new ProcedureRegistry();

        QueryResult result = registry.execute(
                "ja.path.shortest",
                List.of("node:1", "node:3", "4"),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );

        assertFalse(result.getRows().isEmpty());
        List<Object> row = result.getRows().get(0);
        assertEquals("1,2,3", row.get(2));
        assertEquals("11,12", row.get(3));
    }

    @Test
    void shortestShouldResolveMethodSignatureReference() {
        GraphSnapshot snapshot = buildSimpleSnapshot();
        ProcedureRegistry registry = new ProcedureRegistry();

        QueryResult result = registry.execute(
                "ja.path.shortest",
                List.of("app.Entry#run#()V", "app/Sink#sink#()V", "3"),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );

        assertFalse(result.getRows().isEmpty());
        assertEquals("1,2,3", result.getRows().get(0).get(2));
        assertTrue(result.getWarnings().contains("path_search_direction=bidirectional"));
    }

    @Test
    void shortestShouldSupportExplicitBackwardDirection() {
        GraphSnapshot snapshot = buildSimpleSnapshot();
        ProcedureRegistry registry = new ProcedureRegistry();

        QueryResult result = registry.execute(
                "ja.path.shortest",
                List.of("node:1", "node:3", "4", "call-only", "backward"),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );

        assertEquals(1, result.getRows().size());
        assertEquals("1,2,3", result.getRows().get(0).get(2));
        assertTrue(String.valueOf(result.getRows().get(0).get(6)).contains("direction=backward"));
        assertTrue(result.getWarnings().contains("path_search_direction=backward"));
    }

    @Test
    void fromToPrunedShouldSupportBidirectionalDirection() {
        GraphSnapshot snapshot = buildSimpleSnapshot();
        ProcedureRegistry registry = new ProcedureRegistry();

        QueryResult result = registry.execute(
                "ja.path.from_to_pruned",
                List.of("node:1", "node:3", "4", "8", "call-only", "bidirectional"),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );

        assertEquals(2, result.getRows().size());
        assertTrue(String.valueOf(result.getRows().get(0).get(6)).contains("direction=bidirectional"));
        assertTrue(result.getWarnings().contains("path_search_direction=bidirectional"));
    }

    @Test
    void fromToShouldFailWhenPathBudgetExceeded() {
        GraphSnapshot snapshot = buildPathExplosionSnapshot();
        ProcedureRegistry registry = new ProcedureRegistry();
        QueryOptions options = new QueryOptions(500, 15000, 8, 5000, 0, 16, 0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> registry.execute(
                "ja.path.from_to",
                List.of("node:1", "node:14", "8", "200"),
                Map.of(),
                options,
                snapshot
        ));
        assertEquals("cypher_path_budget_exceeded", ex.getMessage());
    }

    @Test
    void fromToPrunedShouldKeepSingleSemanticPathUnderPathExplosion() {
        PrunedFlowFixture.FixtureData fixture = PrunedFlowFixture.install();
        ProcedureRegistry registry = new ProcedureRegistry();
        QueryOptions options = new QueryOptions(500, 15000, 8, 5000, 0, 16, 0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> registry.execute(
                "ja.path.from_to",
                List.of("node:" + fixture.sourceNodeId(), "node:" + fixture.sinkNodeId(), "8", "200"),
                Map.of(),
                options,
                fixture.snapshot()
        ));
        assertEquals("cypher_path_budget_exceeded", ex.getMessage());

        QueryResult result = registry.execute(
                "ja.path.from_to_pruned",
                List.of("node:" + fixture.sourceNodeId(), "node:" + fixture.sinkNodeId(), "8", "200"),
                Map.of(),
                options,
                fixture.snapshot()
        );

        assertEquals(1, result.getRows().size());
        assertEquals("1,2,6,10,14", result.getRows().get(0).get(2));
        assertTrue(result.getWarnings().contains("pruned_state_cuts=9"));
    }

    @Test
    void shortestShouldFailWhenExpandBudgetExceeded() {
        GraphSnapshot snapshot = buildTwoComponentDenseSnapshot();
        ProcedureRegistry registry = new ProcedureRegistry();
        QueryOptions options = new QueryOptions(500, 30000, 64, 500, 1000, 0, 0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> registry.execute(
                "ja.path.shortest",
                List.of("node:1", "node:61", "64"),
                Map.of(),
                options,
                snapshot
        ));
        assertEquals("cypher_expand_budget_exceeded", ex.getMessage());
    }

    @Test
    void pathProceduresShouldIgnoreNonCallAndNonMethodEdges() {
        GraphSnapshot snapshot = buildNonCallBridgeSnapshot();
        ProcedureRegistry registry = new ProcedureRegistry();

        QueryResult shortest = registry.execute(
                "ja.path.shortest",
                List.of("node:1", "node:3", "4"),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );
        assertTrue(shortest.getRows().isEmpty());
        assertTrue(shortest.getWarnings().contains("path_not_found"));

        QueryResult fromTo = registry.execute(
                "ja.path.from_to",
                List.of("node:1", "node:3", "4", "16"),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );
        assertTrue(fromTo.getRows().isEmpty());
        assertTrue(fromTo.getWarnings().contains("path_not_found"));
    }

    @Test
    void pathProceduresShouldOptionallyTraverseAliasEdges() {
        GraphSnapshot snapshot = buildAliasBridgeSnapshot();
        ProcedureRegistry registry = new ProcedureRegistry();

        QueryResult callOnly = registry.execute(
                "ja.path.shortest",
                List.of("node:1", "node:4", "4"),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );
        assertTrue(callOnly.getRows().isEmpty());
        assertTrue(callOnly.getWarnings().contains("path_not_found"));

        QueryResult aliasEnabled = registry.execute(
                "ja.path.shortest",
                List.of("node:1", "node:4", "4", "call+alias"),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );
        assertEquals(1, aliasEnabled.getRows().size());
        assertEquals("1,2,3,4", aliasEnabled.getRows().get(0).get(2));
        assertTrue(String.valueOf(aliasEnabled.getRows().get(0).get(6)).contains("call+alias"));
    }

    private static GraphSnapshot buildSimpleSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, new GraphNode(1L, "method", 1, "app/Entry", "run", "()V", "", -1, -1));
        nodes.put(2L, new GraphNode(2L, "method", 1, "app/Mid", "mid", "()V", "", -1, -1));
        nodes.put(3L, new GraphNode(3L, "method", 1, "app/Sink", "sink", "()V", "", -1, -1));
        nodes.put(4L, new GraphNode(4L, "method", 1, "app/AltA", "a", "()V", "", -1, -1));
        nodes.put(5L, new GraphNode(5L, "method", 1, "app/AltB", "b", "()V", "", -1, -1));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, new GraphEdge(11L, 1L, 2L, "CALLS_DIRECT", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(12L, 2L, 3L, "CALLS_DIRECT", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(13L, 1L, 4L, "CALLS_DIRECT", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(14L, 4L, 5L, "CALLS_DIRECT", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(15L, 5L, 3L, "CALLS_DIRECT", "high", "unit", 0));

        return GraphSnapshot.of(9L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphSnapshot buildPathExplosionSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();

        long nextNodeId = 1L;
        long nextEdgeId = 1000L;
        List<List<Long>> layers = new ArrayList<>();

        layers.add(List.of(nextNodeId));
        nodes.put(nextNodeId, methodNode(nextNodeId, "bench/Layer0", "m0"));

        for (int layer = 1; layer <= 4; layer++) {
            List<Long> ids = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                nextNodeId++;
                ids.add(nextNodeId);
                nodes.put(nextNodeId, methodNode(nextNodeId, "bench/Layer" + layer, "m" + i));
            }
            layers.add(ids);
        }

        nextNodeId++;
        long sinkId = nextNodeId;
        nodes.put(sinkId, methodNode(sinkId, "bench/Sink", "sink"));
        layers.add(List.of(sinkId));

        for (int i = 0; i + 1 < layers.size(); i++) {
            List<Long> srcs = layers.get(i);
            List<Long> dsts = layers.get(i + 1);
            for (Long src : srcs) {
                for (Long dst : dsts) {
                    addEdge(outgoing, incoming,
                            new GraphEdge(nextEdgeId++, src, dst, "CALLS_DIRECT", "high", "unit", 0));
                }
            }
        }

        return GraphSnapshot.of(12L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphSnapshot buildTwoComponentDenseSnapshot() {
        final int componentSize = 60;
        final int fanOut = 20;
        Map<Long, GraphNode> nodes = new HashMap<>();
        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();

        for (int i = 1; i <= componentSize * 2; i++) {
            nodes.put((long) i, methodNode(i, "bench/C" + i, "m"));
        }

        long edgeId = 2000L;
        edgeId = connectDenseComponent(1, componentSize, fanOut, edgeId, outgoing, incoming);
        connectDenseComponent(componentSize + 1, componentSize * 2, fanOut, edgeId, outgoing, incoming);
        return GraphSnapshot.of(13L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphSnapshot buildNonCallBridgeSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, new GraphNode(1L, "method", 1, "app/Entry", "run", "()V", "", -1, -1));
        nodes.put(2L, new GraphNode(2L, "callsite", 1, "app/Entry", "run", "()V", "cs-1", 10, 0));
        nodes.put(3L, new GraphNode(3L, "method", 1, "app/Sink", "sink", "()V", "", -1, -1));
        nodes.put(4L, new GraphNode(4L, "method", 1, "app/Isolated", "noop", "()V", "", -1, -1));
        nodes.put(5L, new GraphNode(5L, "method", 1, "app/Isolated", "noop2", "()V", "", -1, -1));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, new GraphEdge(31L, 1L, 2L, "CONTAINS_CALLSITE", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(32L, 2L, 3L, "CALLSITE_TO_CALLEE", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(33L, 4L, 5L, "CALLS_DIRECT", "high", "unit", 0));

        return GraphSnapshot.of(14L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphSnapshot buildAliasBridgeSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, new GraphNode(1L, "method", 1, "app/Source", "entry", "(Ljava/lang/String;)V", "", -1, -1));
        nodes.put(2L, new GraphNode(2L, "method", 1, "app/MidA", "wrap", "(Ljava/lang/String;)V", "", -1, -1));
        nodes.put(3L, new GraphNode(3L, "method", 1, "app/MidB", "bind", "(Ljava/lang/String;)V", "", -1, -1));
        nodes.put(4L, new GraphNode(4L, "method", 1, "app/Sink", "sink", "()V", "", -1, -1));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, new GraphEdge(41L, 1L, 2L, "CALLS_DIRECT", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(42L, 2L, 3L, "ALIAS", "high", "summary_flow:arg_to_arg", "arg_to_arg", 0, "", -1, -1));
        addEdge(outgoing, incoming, new GraphEdge(43L, 3L, 4L, "CALLS_DIRECT", "high", "unit", 0));
        return GraphSnapshot.of(15L, nodes, outgoing, incoming, Map.of());
    }

    private static long connectDenseComponent(int start,
                                              int end,
                                              int fanOut,
                                              long edgeId,
                                              Map<Long, List<GraphEdge>> outgoing,
                                              Map<Long, List<GraphEdge>> incoming) {
        int size = end - start + 1;
        for (int offset = 0; offset < size; offset++) {
            long src = start + offset;
            for (int step = 1; step <= fanOut; step++) {
                long dst = start + ((offset + step) % size);
                addEdge(outgoing, incoming,
                        new GraphEdge(edgeId++, src, dst, "CALLS_DIRECT", "high", "bench", 0));
            }
        }
        return edgeId;
    }

    private static GraphNode methodNode(long id, String clazz, String method) {
        return new GraphNode(id, "method", 1, clazz, method, "()V", "", -1, -1);
    }

    private static void addEdge(Map<Long, List<GraphEdge>> outgoing,
                                Map<Long, List<GraphEdge>> incoming,
                                GraphEdge edge) {
        outgoing.computeIfAbsent(edge.getSrcId(), k -> new ArrayList<>()).add(edge);
        incoming.computeIfAbsent(edge.getDstId(), k -> new ArrayList<>()).add(edge);
    }
}
