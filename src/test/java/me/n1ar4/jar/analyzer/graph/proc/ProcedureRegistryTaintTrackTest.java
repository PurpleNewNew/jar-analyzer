/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.proc;

import me.n1ar4.jar.analyzer.graph.query.QueryOptions;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcedureRegistryTaintTrackTest {
    @Test
    void taintTrackShouldUseGraphFlowEngine() {
        GraphSnapshot snapshot = buildLinearSnapshot();
        QueryResult result = new ProcedureRegistry().execute(
                "ja.taint.track",
                List.of(
                        "app/Source", "entry", "(Ljava/lang/String;)V",
                        "app/Sink", "sink", "()V",
                        "6", "15000", "10"
                ),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );

        assertFalse(result.getRows().isEmpty());
        List<Object> row = result.getRows().get(0);
        assertEquals("1,2,3", row.get(2));
        assertTrue(String.valueOf(row.get(6)).contains("taint backend: graph-summary"));
    }

    @Test
    void taintTrackShouldRespectMaxPathLimit() {
        GraphSnapshot snapshot = buildTwoPathSnapshot();
        QueryResult result = new ProcedureRegistry().execute(
                "ja.taint.track",
                List.of(
                        "app/Source", "entry", "(Ljava/lang/String;)V",
                        "app/Sink", "sink", "()V",
                        "6", "15000", "1"
                ),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );

        assertFalse(result.getRows().isEmpty());
        assertTrue(result.getRows().size() <= 1);
    }

    private static GraphSnapshot buildLinearSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, methodNode(1L, "app/Source", "entry", "(Ljava/lang/String;)V"));
        nodes.put(2L, methodNode(2L, "app/Mid", "forward", "(Ljava/lang/String;)V"));
        nodes.put(3L, methodNode(3L, "app/Sink", "sink", "()V"));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, new GraphEdge(11L, 1L, 2L, "CALLS_DIRECT", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(12L, 2L, 3L, "CALLS_DIRECT", "high", "unit", 0));
        return GraphSnapshot.of(21L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphSnapshot buildTwoPathSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, methodNode(1L, "app/Source", "entry", "(Ljava/lang/String;)V"));
        nodes.put(2L, methodNode(2L, "app/MidA", "a", "()V"));
        nodes.put(3L, methodNode(3L, "app/MidB", "b", "()V"));
        nodes.put(4L, methodNode(4L, "app/Sink", "sink", "()V"));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, new GraphEdge(21L, 1L, 2L, "CALLS_DIRECT", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(22L, 2L, 4L, "CALLS_DIRECT", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(23L, 1L, 3L, "CALLS_DIRECT", "high", "unit", 0));
        addEdge(outgoing, incoming, new GraphEdge(24L, 3L, 4L, "CALLS_DIRECT", "high", "unit", 0));
        return GraphSnapshot.of(22L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphNode methodNode(long id, String clazz, String method, String desc) {
        return new GraphNode(id, "method", 1, clazz, method, desc, "", -1, -1);
    }

    private static void addEdge(Map<Long, List<GraphEdge>> outgoing,
                                Map<Long, List<GraphEdge>> incoming,
                                GraphEdge edge) {
        outgoing.computeIfAbsent(edge.getSrcId(), k -> new ArrayList<>()).add(edge);
        incoming.computeIfAbsent(edge.getDstId(), k -> new ArrayList<>()).add(edge);
    }
}
