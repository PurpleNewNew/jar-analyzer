/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.proc;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.graph.query.QueryOptions;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.rules.MethodSemanticFlags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcedureRegistryGadgetTrackTest {
    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
    }

    @Test
    void pathGadgetShouldAcceptContainerCallbackRoute() {
        GraphSnapshot snapshot = buildContainerCallbackSnapshot();

        QueryResult result = new ProcedureRegistry().execute(
                "ja.path.gadget",
                List.of("node:1", "node:4", "6", "10"),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );

        assertEquals(1, result.getRows().size());
        List<Object> row = result.getRows().get(0);
        assertEquals("1,2,3,4", row.get(2));
        assertEquals("11,12,13", row.get(3));
        assertEquals("high", row.get(5));
        assertTrue(String.valueOf(row.get(6)).contains("route=container-callback"));
        assertTrue(String.valueOf(row.get(6)).contains("constraints=serializable,deserialization,callback,container"));
        assertTrue(result.getWarnings().contains("gadget_route_container_callback=1"));
        assertFalse(result.getWarnings().contains("path_not_found"));
    }

    @Test
    void pathGadgetShouldSupportBackwardDirection() {
        GraphSnapshot snapshot = buildContainerCallbackSnapshot();

        QueryResult result = new ProcedureRegistry().execute(
                "ja.path.gadget",
                List.of("node:1", "node:4", "6", "10", "call-only", "backward"),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );

        assertEquals(1, result.getRows().size());
        assertEquals("1,2,3,4", result.getRows().get(0).get(2));
        assertTrue(String.valueOf(result.getRows().get(0).get(6)).contains("direction=backward"));
        assertTrue(result.getWarnings().contains("gadget_search_direction=backward"));
    }

    @Test
    void gadgetTrackShouldSupportSearchAllSources() {
        GraphSnapshot snapshot = buildSearchAllSourcesSnapshot();

        QueryResult result = new ProcedureRegistry().execute(
                "ja.gadget.track",
                List.of(
                        "", "", "",
                        "app/Sink", "sink", "()V",
                        "6", "10", "true"
                ),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );

        assertEquals(1, result.getRows().size());
        List<Object> row = result.getRows().get(0);
        assertEquals("1,2,3,4", row.get(2));
        assertTrue(String.valueOf(row.get(6)).contains("ja.gadget.track"));
        assertTrue(String.valueOf(row.get(6)).contains("route=container-callback"));
        assertTrue(result.getWarnings().contains("gadget_source_candidates=2"));
    }

    @Test
    void gadgetTrackShouldEnumerateMultipleLiveSourcesInOnePass() {
        GraphSnapshot snapshot = buildMultiSourceSearchAllSourcesSnapshot();

        QueryResult result = new ProcedureRegistry().execute(
                "ja.gadget.track",
                List.of(
                        "", "", "",
                        "app/Sink", "sink", "()V",
                        "8", "10", "true"
                ),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );

        assertEquals(2, result.getRows().size());
        assertEquals(List.of("1,2,3,4", "5,6,7,4"),
                result.getRows().stream().map(row -> String.valueOf(row.get(2))).toList());
        assertTrue(result.getWarnings().contains("gadget_source_candidates=2"));
        assertTrue(result.getWarnings().contains("gadget_route_container_callback=2"));
    }

    @Test
    void gadgetTrackShouldSelectBestRoutePerSourceWhenSearchAllSourcesIsCapped() {
        GraphSnapshot snapshot = buildPrioritizedMultiRouteSearchAllSourcesSnapshot();

        QueryResult result = new ProcedureRegistry().execute(
                "ja.gadget.track",
                List.of(
                        "", "", "",
                        "app/Sink", "sink", "()V",
                        "8", "2", "true"
                ),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );

        assertEquals(2, result.getRows().size());
        assertEquals(List.of("1,4,5,8", "6,7,8"),
                result.getRows().stream().map(row -> String.valueOf(row.get(2))).toList());
        assertTrue(String.valueOf(result.getRows().get(0).get(6)).contains("route=reflection-trigger"));
        assertTrue(String.valueOf(result.getRows().get(1).get(6)).contains("route=proxy-dynamic"));
        assertFalse(result.getRows().stream().anyMatch(row -> String.valueOf(row.get(6)).contains("route=reflection-container")));
        assertTrue(result.getWarnings().contains("gadget_route_reflection_trigger=1"));
        assertTrue(result.getWarnings().contains("gadget_route_proxy_dynamic=1"));
    }

    @Test
    void gadgetTrackShouldSupportBidirectionalDirection() {
        GraphSnapshot snapshot = buildSearchAllSourcesSnapshot();

        QueryResult result = new ProcedureRegistry().execute(
                "ja.gadget.track",
                List.of(
                        "app/Source", "readObject", "(Ljava/io/ObjectInputStream;)V",
                        "app/Sink", "sink", "()V",
                        "6", "10", "false", "call-only", "bidirectional"
                ),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );

        assertEquals(1, result.getRows().size());
        assertEquals("1,2,3,4", result.getRows().get(0).get(2));
        assertTrue(String.valueOf(result.getRows().get(0).get(6)).contains("direction=bidirectional"));
        assertTrue(result.getWarnings().contains("gadget_search_direction=bidirectional"));
    }

    @Test
    void pathGadgetShouldAcceptProxyDynamicRoute() {
        GraphSnapshot snapshot = buildProxyDynamicSnapshot();

        QueryResult result = new ProcedureRegistry().execute(
                "ja.path.gadget",
                List.of("app/Source#readObject#(Ljava/io/ObjectInputStream;)V", "app/Sink#sink#()V", "6", "10"),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );

        assertEquals(1, result.getRows().size());
        List<Object> row = result.getRows().get(0);
        assertEquals("1,2,3", row.get(2));
        assertEquals("high", row.get(5));
        assertTrue(String.valueOf(row.get(6)).contains("route=proxy-dynamic"));
        assertTrue(String.valueOf(row.get(6)).contains("constraints=serializable,deserialization,proxy,dynamic"));
        assertTrue(result.getWarnings().contains("gadget_route_proxy_dynamic=1"));
    }

    @Test
    void pathGadgetShouldRejectPlainCallChainWithoutSemanticState() {
        GraphSnapshot snapshot = buildPlainSnapshot();

        QueryResult result = new ProcedureRegistry().execute(
                "ja.path.gadget",
                List.of("node:1", "node:3", "4", "10"),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );

        assertTrue(result.getRows().isEmpty());
        assertTrue(result.getWarnings().contains("path_not_found"));
        assertTrue(result.getWarnings().stream().anyMatch(v -> v.startsWith("gadget_state_cuts=")));
    }

    @Test
    void pathGadgetShouldAcceptComparableContainerRoute() {
        GraphSnapshot snapshot = buildComparableContainerSnapshot();

        QueryResult result = new ProcedureRegistry().execute(
                "ja.path.gadget",
                List.of("node:1", "node:4", "6", "10"),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );

        assertEquals(1, result.getRows().size());
        assertTrue(String.valueOf(result.getRows().get(0).get(6)).contains("route=container-callback"));
        assertTrue(String.valueOf(result.getRows().get(0).get(6)).contains("callback"));
    }

    @Test
    void pathGadgetShouldAcceptContainerTriggerRoute() {
        GraphSnapshot snapshot = buildContainerTriggerSnapshot();

        QueryResult result = new ProcedureRegistry().execute(
                "ja.path.gadget",
                List.of("node:1", "node:3", "6", "10"),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );

        assertEquals(1, result.getRows().size());
        assertEquals("medium", result.getRows().get(0).get(5));
        assertTrue(String.valueOf(result.getRows().get(0).get(6)).contains("route=container-trigger"));
        assertTrue(String.valueOf(result.getRows().get(0).get(6)).contains("constraints=serializable,deserialization,trigger,container"));
        assertTrue(result.getWarnings().contains("gadget_route_container_trigger=1"));
    }

    @Test
    void pathGadgetShouldAcceptReflectionTriggerRoute() {
        GraphSnapshot snapshot = buildReflectionTriggerSnapshot();

        QueryResult result = new ProcedureRegistry().execute(
                "ja.path.gadget",
                List.of("node:1", "node:4", "6", "10"),
                Map.of(),
                QueryOptions.defaults(),
                snapshot
        );

        assertEquals(1, result.getRows().size());
        assertEquals("medium", result.getRows().get(0).get(5));
        assertTrue(String.valueOf(result.getRows().get(0).get(6)).contains("route=reflection-trigger"));
        assertTrue(result.getWarnings().contains("gadget_route_reflection_trigger=1"));
    }

    private static GraphSnapshot buildContainerCallbackSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, methodNode(1L, "app/Source", "readObject", "(Ljava/io/ObjectInputStream;)V",
                MethodSemanticFlags.SERIALIZABLE_OWNER | MethodSemanticFlags.DESERIALIZATION_CALLBACK));
        nodes.put(2L, methodNode(2L, "java/util/PriorityQueue", "heapify", "()V",
                MethodSemanticFlags.COLLECTION_CONTAINER));
        nodes.put(3L, methodNode(3L, "app/Comparator", "compare", "(Ljava/lang/Object;Ljava/lang/Object;)I",
                MethodSemanticFlags.COMPARATOR_CALLBACK));
        nodes.put(4L, methodNode(4L, "app/Sink", "sink", "()V", 0));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, edge(11L, 1L, 2L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(12L, 2L, 3L, "CALLS_CALLBACK", MethodCallMeta.EVIDENCE_CALLBACK));
        addEdge(outgoing, incoming, edge(13L, 3L, 4L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));

        return GraphSnapshot.of(31L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphSnapshot buildSearchAllSourcesSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, methodNode(1L, "app/Source", "readObject", "(Ljava/io/ObjectInputStream;)V",
                MethodSemanticFlags.SERIALIZABLE_OWNER | MethodSemanticFlags.DESERIALIZATION_CALLBACK));
        nodes.put(2L, methodNode(2L, "java/util/PriorityQueue", "heapify", "()V",
                MethodSemanticFlags.COLLECTION_CONTAINER));
        nodes.put(3L, methodNode(3L, "app/Comparator", "compare", "(Ljava/lang/Object;Ljava/lang/Object;)I",
                MethodSemanticFlags.COMPARATOR_CALLBACK));
        nodes.put(4L, methodNode(4L, "app/Sink", "sink", "()V", 0));
        nodes.put(5L, methodNode(5L, "app/DeadSource", "readObject", "(Ljava/io/ObjectInputStream;)V",
                MethodSemanticFlags.SERIALIZABLE_OWNER | MethodSemanticFlags.DESERIALIZATION_CALLBACK));
        nodes.put(6L, methodNode(6L, "app/DeadMid", "noop", "()V", 0));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, edge(11L, 1L, 2L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(12L, 2L, 3L, "CALLS_CALLBACK", MethodCallMeta.EVIDENCE_CALLBACK));
        addEdge(outgoing, incoming, edge(13L, 3L, 4L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(21L, 5L, 6L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));

        return GraphSnapshot.of(32L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphSnapshot buildMultiSourceSearchAllSourcesSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, methodNode(1L, "app/SourceOne", "readObject", "(Ljava/io/ObjectInputStream;)V",
                MethodSemanticFlags.SERIALIZABLE_OWNER | MethodSemanticFlags.DESERIALIZATION_CALLBACK));
        nodes.put(2L, methodNode(2L, "java/util/PriorityQueue", "heapify", "()V",
                MethodSemanticFlags.COLLECTION_CONTAINER));
        nodes.put(3L, methodNode(3L, "app/ComparatorOne", "compare", "(Ljava/lang/Object;Ljava/lang/Object;)I",
                MethodSemanticFlags.COMPARATOR_CALLBACK));
        nodes.put(4L, methodNode(4L, "app/Sink", "sink", "()V", 0));
        nodes.put(5L, methodNode(5L, "app/SourceTwo", "readObject", "(Ljava/io/ObjectInputStream;)V",
                MethodSemanticFlags.SERIALIZABLE_OWNER | MethodSemanticFlags.DESERIALIZATION_CALLBACK));
        nodes.put(6L, methodNode(6L, "java/util/TreeMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                MethodSemanticFlags.COLLECTION_CONTAINER));
        nodes.put(7L, methodNode(7L, "app/ComparableTwo", "compareTo", "(Ljava/lang/Object;)I",
                MethodSemanticFlags.COMPARABLE_CALLBACK));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, edge(11L, 1L, 2L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(12L, 2L, 3L, "CALLS_CALLBACK", MethodCallMeta.EVIDENCE_CALLBACK));
        addEdge(outgoing, incoming, edge(13L, 3L, 4L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(21L, 5L, 6L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(22L, 6L, 7L, "CALLS_CALLBACK", MethodCallMeta.EVIDENCE_CALLBACK));
        addEdge(outgoing, incoming, edge(23L, 7L, 4L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));

        return GraphSnapshot.of(320L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphSnapshot buildPrioritizedMultiRouteSearchAllSourcesSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, methodNode(1L, "app/SourceOne", "readObject", "(Ljava/io/ObjectInputStream;)V",
                MethodSemanticFlags.SERIALIZABLE_OWNER | MethodSemanticFlags.DESERIALIZATION_CALLBACK));
        nodes.put(2L, methodNode(2L, "java/util/PriorityQueue", "heapify", "()V",
                MethodSemanticFlags.COLLECTION_CONTAINER));
        nodes.put(3L, methodNode(3L, "app/ReflectiveBridge", "invoke", "()V", 0));
        nodes.put(4L, methodNode(4L, "app/TriggerBean", "toString", "()Ljava/lang/String;",
                MethodSemanticFlags.TOSTRING_TRIGGER));
        nodes.put(5L, methodNode(5L, "app/ReflectiveGetter", "getOutputProperties", "()Ljava/util/Properties;", 0));
        nodes.put(6L, methodNode(6L, "app/SourceTwo", "readObject", "(Ljava/io/ObjectInputStream;)V",
                MethodSemanticFlags.SERIALIZABLE_OWNER | MethodSemanticFlags.DESERIALIZATION_CALLBACK));
        nodes.put(7L, methodNode(7L, "app/Proxy", "invoke",
                "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;",
                MethodSemanticFlags.INVOCATION_HANDLER));
        nodes.put(8L, methodNode(8L, "app/Sink", "sink", "()V", 0));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, edge(11L, 1L, 2L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(12L, 2L, 3L, "CALLS_REFLECTION", MethodCallMeta.EVIDENCE_REFLECTION));
        addEdge(outgoing, incoming, edge(13L, 3L, 8L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(14L, 1L, 4L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(15L, 4L, 5L, "CALLS_REFLECTION", MethodCallMeta.EVIDENCE_REFLECTION));
        addEdge(outgoing, incoming, edge(16L, 5L, 8L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(21L, 6L, 7L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(22L, 7L, 8L, "CALLS_METHOD_HANDLE", MethodCallMeta.EVIDENCE_METHOD_HANDLE));

        return GraphSnapshot.of(321L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphSnapshot buildProxyDynamicSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, methodNode(1L, "app/Source", "readObject", "(Ljava/io/ObjectInputStream;)V",
                MethodSemanticFlags.SERIALIZABLE_OWNER | MethodSemanticFlags.DESERIALIZATION_CALLBACK));
        nodes.put(2L, methodNode(2L, "app/Proxy", "invoke",
                "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;",
                MethodSemanticFlags.INVOCATION_HANDLER));
        nodes.put(3L, methodNode(3L, "app/Sink", "sink", "()V", 0));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, edge(31L, 1L, 2L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(32L, 2L, 3L, "CALLS_METHOD_HANDLE", MethodCallMeta.EVIDENCE_METHOD_HANDLE));

        return GraphSnapshot.of(33L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphSnapshot buildPlainSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, methodNode(1L, "app/A", "a", "()V", 0));
        nodes.put(2L, methodNode(2L, "app/B", "b", "()V", 0));
        nodes.put(3L, methodNode(3L, "app/C", "c", "()V", 0));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, edge(41L, 1L, 2L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(42L, 2L, 3L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));

        return GraphSnapshot.of(34L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphSnapshot buildComparableContainerSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, methodNode(1L, "app/Source", "readObject", "(Ljava/io/ObjectInputStream;)V",
                MethodSemanticFlags.SERIALIZABLE_OWNER | MethodSemanticFlags.DESERIALIZATION_CALLBACK));
        nodes.put(2L, methodNode(2L, "java/util/TreeMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                MethodSemanticFlags.COLLECTION_CONTAINER));
        nodes.put(3L, methodNode(3L, "app/ComparableBean", "compareTo", "(Ljava/lang/Object;)I",
                MethodSemanticFlags.COMPARABLE_CALLBACK));
        nodes.put(4L, methodNode(4L, "app/Sink", "sink", "()V", 0));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, edge(51L, 1L, 2L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(52L, 2L, 3L, "CALLS_CALLBACK", MethodCallMeta.EVIDENCE_CALLBACK));
        addEdge(outgoing, incoming, edge(53L, 3L, 4L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));

        return GraphSnapshot.of(35L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphSnapshot buildContainerTriggerSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, methodNode(1L, "javax/management/BadAttributeValueExpException", "readObject", "(Ljava/io/ObjectInputStream;)V",
                MethodSemanticFlags.SERIALIZABLE_OWNER
                        | MethodSemanticFlags.DESERIALIZATION_CALLBACK
                        | MethodSemanticFlags.COLLECTION_CONTAINER));
        nodes.put(2L, methodNode(2L, "app/ToStringBean", "toString", "()Ljava/lang/String;",
                MethodSemanticFlags.TOSTRING_TRIGGER));
        nodes.put(3L, methodNode(3L, "app/Sink", "sink", "()V", 0));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, edge(61L, 1L, 2L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(62L, 2L, 3L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));

        return GraphSnapshot.of(36L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphSnapshot buildReflectionTriggerSnapshot() {
        Map<Long, GraphNode> nodes = new HashMap<>();
        nodes.put(1L, methodNode(1L, "app/Source", "readObject", "(Ljava/io/ObjectInputStream;)V",
                MethodSemanticFlags.SERIALIZABLE_OWNER | MethodSemanticFlags.DESERIALIZATION_CALLBACK));
        nodes.put(2L, methodNode(2L, "app/ToStringBean", "toString", "()Ljava/lang/String;",
                MethodSemanticFlags.TOSTRING_TRIGGER));
        nodes.put(3L, methodNode(3L, "app/GetterBean", "getOutputProperties", "()Ljava/util/Properties;", 0));
        nodes.put(4L, methodNode(4L, "app/Sink", "sink", "()V", 0));

        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        addEdge(outgoing, incoming, edge(71L, 1L, 2L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));
        addEdge(outgoing, incoming, edge(72L, 2L, 3L, "CALLS_REFLECTION", MethodCallMeta.EVIDENCE_REFLECTION));
        addEdge(outgoing, incoming, edge(73L, 3L, 4L, "CALLS_DIRECT", MethodCallMeta.EVIDENCE_DIRECT));

        return GraphSnapshot.of(37L, nodes, outgoing, incoming, Map.of());
    }

    private static GraphNode methodNode(long id,
                                        String className,
                                        String methodName,
                                        String methodDesc,
                                        int semanticFlags) {
        return new GraphNode(id, "method", 1, className, methodName, methodDesc, "", -1, -1, 0, semanticFlags);
    }

    private static GraphEdge edge(long edgeId,
                                  long srcId,
                                  long dstId,
                                  String relType,
                                  int semanticFlags) {
        return new GraphEdge(edgeId, srcId, dstId, relType, "high", "unit", "", 0, "", -1, -1, semanticFlags);
    }

    private static void addEdge(Map<Long, List<GraphEdge>> outgoing,
                                Map<Long, List<GraphEdge>> incoming,
                                GraphEdge edge) {
        outgoing.computeIfAbsent(edge.getSrcId(), k -> new ArrayList<>()).add(edge);
        incoming.computeIfAbsent(edge.getDstId(), k -> new ArrayList<>()).add(edge);
    }
}
