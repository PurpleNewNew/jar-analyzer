/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.flow;

import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphTraversalRules;
import me.n1ar4.jar.analyzer.rules.MethodSemanticFlags;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GraphGadgetEngine {
    private static final Set<String> CONTAINER_FALLBACK_TYPES = Set.of(
            "java/util/PriorityQueue",
            "java/util/PriorityBlockingQueue",
            "java/util/TreeMap",
            "java/util/TreeSet",
            "java/util/HashMap",
            "java/util/LinkedHashMap",
            "java/util/HashSet",
            "java/util/LinkedHashSet",
            "java/util/Hashtable",
            "java/beans/beancontext/BeanContextSupport",
            "javax/management/BadAttributeValueExpException"
    );

    public Run search(GraphSnapshot snapshot, SearchRequest request, Controller controller) {
        long startNs = System.nanoTime();
        if (snapshot == null || request == null || request.fromNodeId() <= 0L || request.toNodeId() <= 0L) {
            return emptyRun(startNs, false);
        }
        Controller effectiveController = controller == null ? Controller.noop() : controller;
        Map<Long, Integer> toTarget = boundedBackwardDistance(snapshot, request, effectiveController);
        if (!toTarget.containsKey(request.fromNodeId())) {
            return emptyRun(startNs, false);
        }

        GraphNode sourceNode = snapshot.getNode(request.fromNodeId());
        if (!GraphTraversalRules.isMethodNode(sourceNode)) {
            return emptyRun(startNs, false);
        }

        int nodeVisits = 1;
        int expandedEdges = 0;
        int prunedByState = 0;
        int prunedByDistance = 0;
        boolean truncated = false;
        List<Candidate> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<Long> nodePath = new ArrayList<>();
        List<GraphEdge> edgePath = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<Frame> stack = new ArrayDeque<>();

        GadgetState initialState = GadgetState.initial(sourceNode);
        nodePath.add(request.fromNodeId());
        visited.add(request.fromNodeId());
        ExpandResult initial = expand(snapshot, request, effectiveController, request.fromNodeId(),
                initialState, 0, toTarget, visited, request.toNodeId());
        expandedEdges += initial.expandedEdges();
        prunedByState += initial.prunedByState();
        prunedByDistance += initial.prunedByDistance();
        stack.push(new Frame(request.fromNodeId(), initialState, initial.expansions()));

        while (!stack.isEmpty() && !effectiveController.shouldStop(out.size())) {
            effectiveController.checkpoint();
            Frame frame = stack.peek();
            long current = frame.nodeId();
            int hops = nodePath.size() - 1;
            Integer tail = toTarget.get(current);
            if (tail == null || hops + tail > request.maxHops()) {
                backtrack(stack, nodePath, edgePath, visited);
                continue;
            }
            if (current == request.toNodeId()) {
                String route = frame.state().route();
                if (!route.isBlank()) {
                    emitCandidate(out, seen, nodePath, edgePath, frame.state(), route, effectiveController);
                    if (out.size() >= request.maxPaths()) {
                        truncated = !stack.isEmpty();
                        break;
                    }
                }
                backtrack(stack, nodePath, edgePath, visited);
                continue;
            }
            if (hops >= request.maxHops() || frame.cursor() >= frame.expansions().size()) {
                backtrack(stack, nodePath, edgePath, visited);
                continue;
            }

            Expansion expansion = frame.expansions().get(frame.cursorAndAdvance());
            long nextNodeId = expansion.edge().getDstId();
            if (visited.contains(nextNodeId)) {
                continue;
            }
            visited.add(nextNodeId);
            nodePath.add(nextNodeId);
            edgePath.add(expansion.edge());
            nodeVisits++;
            effectiveController.onNode();

            ExpandResult next = expand(snapshot, request, effectiveController, nextNodeId, expansion.state(),
                    hops + 1, toTarget, visited, request.toNodeId());
            expandedEdges += next.expandedEdges();
            prunedByState += next.prunedByState();
            prunedByDistance += next.prunedByDistance();
            stack.push(new Frame(nextNodeId, expansion.state(), next.expansions()));
        }

        LinkedHashMap<String, Integer> routeStats = new LinkedHashMap<>();
        for (Candidate candidate : out) {
            routeStats.merge(candidate.route(), 1, Integer::sum);
        }
        return new Run(
                out,
                new Stats(nodeVisits, expandedEdges, out.size(), prunedByState, prunedByDistance, elapsedMs(startNs), routeStats),
                truncated
        );
    }

    private static ExpandResult expand(GraphSnapshot snapshot,
                                       SearchRequest request,
                                       Controller controller,
                                       long nodeId,
                                       GadgetState state,
                                       int currentHops,
                                       Map<Long, Integer> toTarget,
                                       Set<Long> visited,
                                       long targetNodeId) {
        if (currentHops >= request.maxHops()) {
            return ExpandResult.empty();
        }
        List<Expansion> out = new ArrayList<>();
        int expandedEdges = 0;
        int prunedByState = 0;
        int prunedByDistance = 0;
        for (GraphEdge edge : snapshot.getOutgoingView(nodeId)) {
            controller.onExpand();
            expandedEdges++;
            if (!GraphTraversalRules.isTraversable(snapshot,
                    edge,
                    request.blacklist(),
                    request.minEdgeConfidence(),
                    request.traversalMode().includesAlias())) {
                continue;
            }
            long nextNodeId = edge.getDstId();
            if (visited.contains(nextNodeId)) {
                continue;
            }
            Integer tail = toTarget.get(nextNodeId);
            if (tail == null || currentHops + 1 + tail > request.maxHops()) {
                prunedByDistance++;
                continue;
            }
            GraphNode nextNode = snapshot.getNode(nextNodeId);
            if (!GraphTraversalRules.isMethodNode(nextNode)) {
                prunedByState++;
                continue;
            }
            GadgetState nextState = state.advance(nextNode, edge);
            if (nextState == null || nextState.isEmpty()) {
                prunedByState++;
                continue;
            }
            out.add(new Expansion(
                    edge,
                    nextState,
                    score(edge, nextState, nextNodeId == targetNodeId, tail)
            ));
        }
        out.sort(Comparator.comparingInt(Expansion::score).thenComparingLong(expansion -> expansion.edge().getEdgeId()));
        return new ExpandResult(out, expandedEdges, prunedByState, prunedByDistance);
    }

    private static Map<Long, Integer> boundedBackwardDistance(GraphSnapshot snapshot,
                                                              SearchRequest request,
                                                              Controller controller) {
        Map<Long, Integer> dist = new HashMap<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        dist.put(request.toNodeId(), 0);
        queue.add(request.toNodeId());
        while (!queue.isEmpty() && !controller.shouldStop(0)) {
            controller.checkpoint();
            long current = queue.poll();
            int hops = dist.getOrDefault(current, 0);
            if (hops >= request.maxHops()) {
                continue;
            }
            for (GraphEdge edge : snapshot.getIncomingView(current)) {
                controller.onExpand();
                long prev = edge.getSrcId();
                if (!GraphTraversalRules.isTraversable(snapshot,
                        edge,
                        request.blacklist(),
                        request.minEdgeConfidence(),
                        request.traversalMode().includesAlias())) {
                    continue;
                }
                if (dist.containsKey(prev)) {
                    continue;
                }
                dist.put(prev, hops + 1);
                queue.add(prev);
            }
        }
        return dist;
    }

    private static void emitCandidate(List<Candidate> out,
                                      Set<String> seen,
                                      List<Long> nodePath,
                                      List<GraphEdge> edgePath,
                                      GadgetState state,
                                      String route,
                                      Controller controller) {
        if (nodePath == null || nodePath.isEmpty()) {
            return;
        }
        String key = pathKey(nodePath);
        if (!seen.add(key)) {
            return;
        }
        out.add(new Candidate(
                List.copyOf(nodePath),
                List.copyOf(edgePath),
                route,
                state.constraintNames(),
                state.lowConfidence(),
                confidence(state, route)
        ));
        controller.onPath();
    }

    private static String confidence(GadgetState state, String route) {
        if (state == null) {
            return "low";
        }
        if ("container-callback".equals(route) || "proxy-dynamic".equals(route)) {
            return state.lowConfidence() ? "medium" : "high";
        }
        if ("container-trigger".equals(route)
                || "reflection-trigger".equals(route)
                || "proxy-trigger".equals(route)) {
            return state.lowConfidence() ? "low" : "medium";
        }
        if ("reflection-callback".equals(route) || "reflection-container".equals(route)) {
            return state.lowConfidence() ? "low" : "medium";
        }
        return "low";
    }

    private static int score(GraphEdge edge, GadgetState state, boolean hitsTarget, int distanceTail) {
        int score = distanceTail * 8;
        String confidence = GraphTraversalRules.normalizeConfidence(edge == null ? null : edge.getConfidence());
        if ("high".equals(confidence)) {
            score -= 80;
        } else if ("medium".equals(confidence)) {
            score -= 10;
        } else {
            score += 80;
        }
        if (state != null) {
            if (state.hasSerializable()) {
                score -= 60;
            }
            if (state.hasContainer()) {
                score -= 40;
            }
            if (state.hasCallback()) {
                score -= 45;
            }
            if (state.hasTrigger()) {
                score -= 35;
            }
            if (state.hasProxy()) {
                score -= 30;
            }
            if (state.hasDynamic()) {
                score -= 25;
            }
            if (state.hasReflection()) {
                score -= 20;
            }
            if (state.lowConfidence()) {
                score += 60;
            }
            if (!state.route().isBlank() && hitsTarget) {
                score -= 800;
            }
        }
        if (GraphTraversalRules.isAliasEdge(edge == null ? null : edge.getRelType())) {
            score += 180;
        }
        return score;
    }

    private static void backtrack(ArrayDeque<Frame> stack,
                                  List<Long> nodePath,
                                  List<GraphEdge> edgePath,
                                  Set<Long> visited) {
        Frame finished = stack.poll();
        if (finished == null) {
            return;
        }
        if (!nodePath.isEmpty()) {
            long removed = nodePath.remove(nodePath.size() - 1);
            if (!nodePath.isEmpty()) {
                visited.remove(removed);
            }
        }
        if (!edgePath.isEmpty()) {
            edgePath.remove(edgePath.size() - 1);
        }
    }

    private static String pathKey(List<Long> nodePath) {
        StringBuilder sb = new StringBuilder();
        for (Long nodeId : nodePath) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(nodeId == null ? -1L : nodeId);
        }
        return sb.toString();
    }

    private static long elapsedMs(long startNs) {
        return Math.max(0L, (System.nanoTime() - startNs) / 1_000_000L);
    }

    private static Run emptyRun(long startNs, boolean truncated) {
        return new Run(
                List.of(),
                new Stats(0, 0, 0, 0, 0, elapsedMs(startNs), Map.of()),
                truncated
        );
    }

    public interface Controller {
        void onNode();

        void onExpand();

        void onPath();

        void checkpoint();

        boolean shouldStop(int emittedPaths);

        static Controller noop() {
            return new Controller() {
                @Override
                public void onNode() {
                }

                @Override
                public void onExpand() {
                }

                @Override
                public void onPath() {
                }

                @Override
                public void checkpoint() {
                }

                @Override
                public boolean shouldStop(int emittedPaths) {
                    return false;
                }
            };
        }
    }

    public record SearchRequest(long fromNodeId,
                                long toNodeId,
                                int maxHops,
                                int maxPaths,
                                TraversalMode traversalMode,
                                Set<String> blacklist,
                                String minEdgeConfidence) {
        public SearchRequest {
            maxHops = Math.max(1, maxHops);
            maxPaths = Math.max(1, maxPaths);
            traversalMode = traversalMode == null ? TraversalMode.CALL_ONLY : traversalMode;
            blacklist = blacklist == null ? Set.of() : Set.copyOf(blacklist);
            minEdgeConfidence = GraphTraversalRules.normalizeConfidence(minEdgeConfidence);
        }
    }

    public record Candidate(List<Long> nodeIds,
                            List<GraphEdge> edges,
                            String route,
                            List<String> constraints,
                            boolean lowConfidence,
                            String confidence) {
    }

    public record Stats(int nodeVisits,
                        int expandedEdges,
                        int emittedPaths,
                        int prunedByState,
                        int prunedByDistance,
                        long elapsedMs,
                        Map<String, Integer> routeCounts) {
        public Stats {
            routeCounts = routeCounts == null ? Map.of() : Map.copyOf(routeCounts);
        }
    }

    public record Run(List<Candidate> candidates, Stats stats, boolean truncated) {
        public Run {
            candidates = candidates == null ? List.of() : List.copyOf(candidates);
            stats = stats == null ? new Stats(0, 0, 0, 0, 0, 0L, Map.of()) : stats;
        }
    }

    private record ExpandResult(List<Expansion> expansions,
                                int expandedEdges,
                                int prunedByState,
                                int prunedByDistance) {
        private static ExpandResult empty() {
            return new ExpandResult(List.of(), 0, 0, 0);
        }
    }

    private record Expansion(GraphEdge edge, GadgetState state, int score) {
    }

    private static final class Frame {
        private final long nodeId;
        private final GadgetState state;
        private final List<Expansion> expansions;
        private int cursor;

        private Frame(long nodeId, GadgetState state, List<Expansion> expansions) {
            this.nodeId = nodeId;
            this.state = state == null ? GadgetState.empty() : state;
            this.expansions = expansions == null ? List.of() : expansions;
            this.cursor = 0;
        }

        private long nodeId() {
            return nodeId;
        }

        private GadgetState state() {
            return state;
        }

        private List<Expansion> expansions() {
            return expansions;
        }

        private int cursor() {
            return cursor;
        }

        private int cursorAndAdvance() {
            return cursor++;
        }
    }

    private record GadgetState(boolean serializable,
                               boolean deserialization,
                               boolean callback,
                               boolean trigger,
                               boolean container,
                               boolean proxy,
                               boolean dynamicCall,
                               boolean reflection,
                               boolean lowConfidence) {
        private static GadgetState empty() {
            return new GadgetState(false, false, false, false, false, false, false, false, false);
        }

        private static GadgetState initial(GraphNode node) {
            return empty().applyNode(node);
        }

        private GadgetState advance(GraphNode node, GraphEdge edge) {
            GadgetState state = applyEdge(edge);
            return state.applyNode(node);
        }

        private GadgetState applyNode(GraphNode node) {
            if (node == null) {
                return this;
            }
            int flags = Math.max(0, node.getMethodSemanticFlags());
            boolean hasSerializable = serializable
                    || hasFlag(flags, MethodSemanticFlags.SERIALIZABLE_OWNER)
                    || hasFlag(flags, MethodSemanticFlags.DESERIALIZATION_CALLBACK);
            boolean hasDeserialization = deserialization
                    || hasFlag(flags, MethodSemanticFlags.DESERIALIZATION_CALLBACK);
            boolean hasCallback = callback
                    || hasFlag(flags, MethodSemanticFlags.COMPARATOR_CALLBACK)
                    || hasFlag(flags, MethodSemanticFlags.TRANSFORMER_CALLBACK)
                    || hasFlag(flags, MethodSemanticFlags.COMPARABLE_CALLBACK);
            boolean hasTrigger = trigger
                    || hasFlag(flags, MethodSemanticFlags.TOSTRING_TRIGGER)
                    || hasFlag(flags, MethodSemanticFlags.HASHCODE_TRIGGER)
                    || hasFlag(flags, MethodSemanticFlags.EQUALS_TRIGGER);
            boolean hasContainer = container
                    || hasFlag(flags, MethodSemanticFlags.COLLECTION_CONTAINER)
                    || isContainerFallback(node.getClassName());
            boolean hasProxy = proxy
                    || hasFlag(flags, MethodSemanticFlags.INVOCATION_HANDLER);
            boolean low = lowConfidence;
            return new GadgetState(
                    hasSerializable,
                    hasDeserialization,
                    hasCallback,
                    hasTrigger,
                    hasContainer,
                    hasProxy,
                    dynamicCall,
                    reflection,
                    low
            );
        }

        private GadgetState applyEdge(GraphEdge edge) {
            if (edge == null) {
                return this;
            }
            int flags = Math.max(0, edge.getSemanticFlags());
            boolean hasCallback = callback
                    || hasEdgeFlag(flags, MethodCallMeta.EVIDENCE_CALLBACK)
                    || "CALLS_CALLBACK".equalsIgnoreCase(edge.getRelType());
            boolean hasDynamic = dynamicCall
                    || hasEdgeFlag(flags, MethodCallMeta.EVIDENCE_DISPATCH)
                    || hasEdgeFlag(flags, MethodCallMeta.EVIDENCE_OVERRIDE)
                    || hasEdgeFlag(flags, MethodCallMeta.EVIDENCE_METHOD_HANDLE)
                    || hasEdgeFlag(flags, MethodCallMeta.EVIDENCE_INDY)
                    || hasEdgeFlag(flags, MethodCallMeta.EVIDENCE_PTA)
                    || "CALLS_DISPATCH".equalsIgnoreCase(edge.getRelType())
                    || "CALLS_OVERRIDE".equalsIgnoreCase(edge.getRelType())
                    || "CALLS_METHOD_HANDLE".equalsIgnoreCase(edge.getRelType())
                    || "CALLS_INDY".equalsIgnoreCase(edge.getRelType())
                    || "CALLS_PTA".equalsIgnoreCase(edge.getRelType());
            boolean hasReflection = reflection
                    || hasEdgeFlag(flags, MethodCallMeta.EVIDENCE_REFLECTION)
                    || "CALLS_REFLECTION".equalsIgnoreCase(edge.getRelType());
            boolean low = lowConfidence
                    || "low".equalsIgnoreCase(GraphTraversalRules.normalizeConfidence(edge.getConfidence()));
            return new GadgetState(
                    serializable,
                    deserialization,
                    hasCallback,
                    trigger,
                    container,
                    proxy,
                    hasDynamic,
                    hasReflection,
                    low
            );
        }

        private String route() {
            if (!serializable) {
                return "";
            }
            if (container && callback) {
                return "container-callback";
            }
            if (proxy && dynamicCall) {
                return "proxy-dynamic";
            }
            if (container && trigger) {
                return "container-trigger";
            }
            if (reflection && callback) {
                return "reflection-callback";
            }
            if (reflection && container) {
                return "reflection-container";
            }
            if (reflection && trigger) {
                return "reflection-trigger";
            }
            if (proxy && trigger) {
                return "proxy-trigger";
            }
            if (deserialization && (callback || container || proxy || dynamicCall || reflection)) {
                return "deserialization-trigger";
            }
            return "";
        }

        private boolean isEmpty() {
            return !serializable
                    && !deserialization
                    && !callback
                    && !trigger
                    && !container
                    && !proxy
                    && !dynamicCall
                    && !reflection;
        }

        private List<String> constraintNames() {
            List<String> out = new ArrayList<>();
            addIfTrue(out, serializable, "serializable");
            addIfTrue(out, deserialization, "deserialization");
            addIfTrue(out, callback, "callback");
            addIfTrue(out, trigger, "trigger");
            addIfTrue(out, container, "container");
            addIfTrue(out, proxy, "proxy");
            addIfTrue(out, dynamicCall, "dynamic");
            addIfTrue(out, reflection, "reflection");
            return List.copyOf(out);
        }

        private boolean hasSerializable() {
            return serializable;
        }

        private boolean hasCallback() {
            return callback;
        }

        private boolean hasContainer() {
            return container;
        }

        private boolean hasTrigger() {
            return trigger;
        }

        private boolean hasProxy() {
            return proxy;
        }

        private boolean hasDynamic() {
            return dynamicCall;
        }

        private boolean hasReflection() {
            return reflection;
        }
    }

    private static void addIfTrue(List<String> out, boolean enabled, String label) {
        if (enabled) {
            out.add(label);
        }
    }

    private static boolean hasFlag(int flags, int mask) {
        return (flags & mask) != 0;
    }

    private static boolean hasEdgeFlag(int flags, int mask) {
        return (flags & mask) != 0;
    }

    private static boolean isContainerFallback(String className) {
        String normalized = safe(className);
        return !normalized.isBlank() && CONTAINER_FALLBACK_TYPES.contains(normalized);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
