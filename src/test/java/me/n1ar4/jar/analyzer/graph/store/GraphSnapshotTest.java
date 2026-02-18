/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.store;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GraphSnapshotTest {
    @Test
    void findMethodNodeIdShouldPreferScopedThenLooseMatch() {
        GraphNode node1 = new GraphNode(1L, "method", 1, "a/B", "foo", "()V", "", -1, -1);
        GraphNode node2 = new GraphNode(2L, "method", 2, "a/B", "foo", "()V", "", -1, -1);
        GraphNode node3 = new GraphNode(3L, "callsite", 1, "a/B", "foo", "()V", "cs-1", 10, 0);
        GraphSnapshot snapshot = GraphSnapshot.of(
                7L,
                Map.of(1L, node1, 2L, node2, 3L, node3),
                Map.of(),
                Map.of(),
                Map.of()
        );

        assertEquals(1L, snapshot.findMethodNodeId("a/B", "foo", "()V", 1));
        assertEquals(2L, snapshot.findMethodNodeId("a.B", "foo", "()V", 2));
        assertEquals(1L, snapshot.findMethodNodeId("a/B", "foo", "()V", null));
        assertEquals(-1L, snapshot.findMethodNodeId("a/B", "bar", "()V", 1));
        assertEquals(Set.of(1L, 2L), snapshot.getNodesByKindView("method")
                .stream().map(GraphNode::getNodeId).collect(Collectors.toSet()));
        assertEquals(Set.of(3L), snapshot.getNodesByKindView("callsite")
                .stream().map(GraphNode::getNodeId).collect(Collectors.toSet()));
    }
}
