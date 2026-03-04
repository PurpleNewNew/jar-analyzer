/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.store;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphTraversalRulesTest {
    @Test
    void shouldTreatOnlyCallsBetweenMethodsAsTraversableBaseRule() {
        GraphSnapshot snapshot = buildSnapshot();
        GraphEdge callEdge = new GraphEdge(1L, 1L, 2L, "CALLS_DIRECT", "high", "test", 0);
        GraphEdge callsiteEdge = new GraphEdge(2L, 1L, 3L, "CONTAINS_CALLSITE", "high", "test", 0);
        GraphEdge nonMethodTarget = new GraphEdge(3L, 1L, 3L, "CALLS_DIRECT", "high", "test", 0);

        assertTrue(GraphTraversalRules.isMethodCallEdge(snapshot, callEdge));
        assertFalse(GraphTraversalRules.isMethodCallEdge(snapshot, callsiteEdge));
        assertFalse(GraphTraversalRules.isMethodCallEdge(snapshot, nonMethodTarget));
    }

    @Test
    void shouldApplyBlacklistAndConfidenceOnTopOfBaseRule() {
        GraphSnapshot snapshot = buildSnapshot();
        GraphEdge edge = new GraphEdge(4L, 1L, 2L, "CALLS_DISPATCH", "medium", "test", 0);

        assertTrue(GraphTraversalRules.isTraversable(snapshot, edge, Set.of(), "low"));
        assertFalse(GraphTraversalRules.isTraversable(snapshot, edge, Set.of("app/A"), "low"));
        assertFalse(GraphTraversalRules.isTraversable(snapshot, edge, Set.of(), "high"));
    }

    @Test
    void shouldNormalizeConfidenceConsistently() {
        assertEquals("low", GraphTraversalRules.normalizeConfidence(null));
        assertEquals("low", GraphTraversalRules.normalizeConfidence("unknown"));
        assertEquals("high", GraphTraversalRules.normalizeConfidence("HIGH"));
        assertTrue(GraphTraversalRules.meetsMinConfidence("medium", "low"));
        assertFalse(GraphTraversalRules.meetsMinConfidence("low", "medium"));
    }

    private static GraphSnapshot buildSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, new GraphNode(1L, "method", 1, "app/A", "a", "()V", "", -1, -1));
        nodes.put(2L, new GraphNode(2L, "method", 1, "app/B", "b", "()V", "", -1, -1));
        nodes.put(3L, new GraphNode(3L, "callsite", 1, "app/A", "a", "()V", "cs-1", 1, 0));
        return GraphSnapshot.of(1L, nodes, Map.of(), Map.of(), Map.of());
    }
}
