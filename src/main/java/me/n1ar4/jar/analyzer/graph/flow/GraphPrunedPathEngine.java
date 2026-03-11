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
import me.n1ar4.jar.analyzer.graph.store.GraphTraversalRules;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.rules.PruningPolicy;
import me.n1ar4.jar.analyzer.utils.StableOrder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GraphPrunedPathEngine {
    private final GraphTaintSemantics semantics;

    public GraphPrunedPathEngine() {
        this(new GraphTaintSemantics());
    }

    GraphPrunedPathEngine(GraphTaintSemantics semantics) {
        this.semantics = semantics == null ? new GraphTaintSemantics() : semantics;
    }

    public Run search(GraphSnapshot snapshot, SearchRequest request, Controller controller) {
        return search(snapshot, request, controller, null);
    }

    public Run search(GraphSnapshot snapshot,
                      SearchRequest request,
                      Controller controller,
                      DistanceCache distanceCache) {
        long startNs = System.nanoTime();
        if (snapshot == null || request == null || request.fromNodeId() <= 0L || request.toNodeId() <= 0L) {
            return emptyRun(startNs);
        }
        semantics.refreshRuleContext();
        Controller effectiveController = controller == null ? Controller.noop() : controller;
        Map<Long, Integer> toTarget = loadBackwardDistance(
                snapshot,
                request.toNodeId(),
                request.maxHops(),
                request.traversalMode(),
                request.blacklist(),
                request.minEdgeConfidence(),
                effectiveController,
                distanceCache
        );
        if (!toTarget.containsKey(request.fromNodeId())) {
            return emptyRun(startNs);
        }

        GraphNode sourceNode = snapshot.getNode(request.fromNodeId());
        GraphNode sinkNode = snapshot.getNode(request.toNodeId());
        MethodReference.Handle sourceHandle = semantics.toHandle(sourceNode);
        MethodReference.Handle sinkHandle = semantics.toHandle(sinkNode);
        if (sourceHandle == null || sinkHandle == null) {
            return emptyRun(startNs);
        }
        String sinkKind = semantics.resolveSinkKind(sinkHandle);
        ModelRegistry.SinkDescriptor sinkDescriptor = ModelRegistry.resolveSinkDescriptor(sinkHandle);
        GraphTaintSemantics.PortState initialState = semantics.initialState(sourceHandle);
        PruningPolicy.Resolved policy = semantics.resolvePolicy(sinkKind, sinkDescriptor, false, false);
        return runForward(snapshot, request, effectiveController, toTarget, sinkKind, initialState, policy, startNs);
    }

    public Run searchBackward(GraphSnapshot snapshot, ReverseSearchRequest request, Controller controller) {
        return searchBackward(snapshot, request, controller, null);
    }

    public Run searchBackward(GraphSnapshot snapshot,
                              ReverseSearchRequest request,
                              Controller controller,
                              DistanceCache distanceCache) {
        long startNs = System.nanoTime();
        if (snapshot == null || request == null || request.sinkNodeId() <= 0L) {
            return emptyRun(startNs);
        }
        semantics.refreshRuleContext();
        Controller effectiveController = controller == null ? Controller.noop() : controller;

        GraphNode sinkNode = snapshot.getNode(request.sinkNodeId());
        MethodReference.Handle sinkHandle = semantics.toHandle(sinkNode);
        if (sinkHandle == null) {
            return emptyRun(startNs);
        }
        String sinkKind = semantics.resolveSinkKind(sinkHandle);
        ModelRegistry.SinkDescriptor sinkDescriptor = ModelRegistry.resolveSinkDescriptor(sinkHandle);
        GraphTaintSemantics.PortState initialState = semantics.initialState(sinkHandle);
        PruningPolicy.Resolved policy = semantics.resolvePolicy(sinkKind, sinkDescriptor, true, request.isSearchAllSources());

        if (request.isSearchAllSources()) {
            return runBackwardAllSources(snapshot, request, effectiveController, sinkKind, initialState, policy, startNs);
        }
        if (request.sourceNodeId() <= 0L) {
            return emptyRun(startNs);
        }
        Map<Long, Integer> fromSource = loadForwardDistance(
                snapshot,
                request.sourceNodeId(),
                request.maxHops(),
                request.traversalMode(),
                request.blacklist(),
                request.minEdgeConfidence(),
                effectiveController,
                distanceCache
        );
        if (!fromSource.containsKey(request.sinkNodeId())) {
            return emptyRun(startNs);
        }
        return runBackwardToSource(snapshot, request, effectiveController, fromSource, sinkKind, initialState, policy, startNs);
    }

    private Run runForward(GraphSnapshot snapshot,
                           SearchRequest request,
                           Controller controller,
                           Map<Long, Integer> toTarget,
                           String sinkKind,
                           GraphTaintSemantics.PortState initialState,
                           PruningPolicy.Resolved policy,
                           long startNs) {
        int nodeVisits = 1;
        int expandedEdges = 0;
        int prunedByState = 0;
        int prunedByDistance = 0;
        List<Candidate> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<Long> nodePath = new ArrayList<>();
        List<GraphEdge> edgePath = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<Frame> stack = new ArrayDeque<>();

        nodePath.add(request.fromNodeId());
        visited.add(request.fromNodeId());
        ExpandResult initial = expandForward(snapshot, request, controller, request.fromNodeId(), initialState,
                0, toTarget, visited, sinkKind, policy, request.toNodeId());
        expandedEdges += initial.expandedEdges();
        prunedByState += initial.prunedByState();
        prunedByDistance += initial.prunedByDistance();
        stack.push(new Frame(request.fromNodeId(), initialState, initial.expansions(), false, true, false));

        while (!stack.isEmpty() && out.size() < request.maxPaths() && !controller.shouldStop(out.size())) {
            controller.checkpoint();
            Frame frame = stack.peek();
            long current = frame.nodeId;
            int hops = nodePath.size() - 1;
            Integer tail = toTarget.get(current);
            if (tail == null || hops + tail > request.maxHops()) {
                backtrack(stack, nodePath, edgePath, visited);
                continue;
            }
            if (current == request.toNodeId()) {
                emitForwardCandidate(snapshot, out, seen, nodePath, edgePath, frame.lowConfidence, frame.proven, controller);
                backtrack(stack, nodePath, edgePath, visited);
                continue;
            }
            if (hops >= request.maxHops() || frame.cursor >= frame.expansions.size()) {
                backtrack(stack, nodePath, edgePath, visited);
                continue;
            }

            Expansion expansion = frame.expansions.get(frame.cursor++);
            long nextNodeId = expansion.edge.getDstId();
            if (visited.contains(nextNodeId)) {
                continue;
            }
            visited.add(nextNodeId);
            nodePath.add(nextNodeId);
            edgePath.add(expansion.edge);
            nodeVisits++;
            controller.onNode();

            ExpandResult next = expandForward(snapshot, request, controller, nextNodeId, expansion.state,
                    hops + 1, toTarget, visited, sinkKind, policy, request.toNodeId());
            expandedEdges += next.expandedEdges();
            prunedByState += next.prunedByState();
            prunedByDistance += next.prunedByDistance();
            stack.push(new Frame(
                    nextNodeId,
                    expansion.state,
                    next.expansions(),
                    frame.lowConfidence || expansion.lowConfidence,
                    frame.proven && expansion.proven,
                    false
            ));
        }

        return new Run(out, new Stats(
                nodeVisits,
                expandedEdges,
                out.size(),
                prunedByState,
                prunedByDistance,
                elapsedMs(startNs)
        ));
    }

    private Run runBackwardToSource(GraphSnapshot snapshot,
                                    ReverseSearchRequest request,
                                    Controller controller,
                                    Map<Long, Integer> fromSource,
                                    String sinkKind,
                                    GraphTaintSemantics.PortState initialState,
                                    PruningPolicy.Resolved policy,
                                    long startNs) {
        int nodeVisits = 1;
        int expandedEdges = 0;
        int prunedByState = 0;
        int prunedByDistance = 0;
        List<Candidate> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<Long> backwardNodePath = new ArrayList<>();
        List<GraphEdge> backwardEdgePath = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<Frame> stack = new ArrayDeque<>();

        backwardNodePath.add(request.sinkNodeId());
        visited.add(request.sinkNodeId());
        ExpandResult initial = expandBackward(snapshot, request, controller, request.sinkNodeId(), initialState,
                0, fromSource, visited, sinkKind, policy, request.sourceNodeId());
        expandedEdges += initial.expandedEdges();
        prunedByState += initial.prunedByState();
        prunedByDistance += initial.prunedByDistance();
        stack.push(new Frame(request.sinkNodeId(), initialState, initial.expansions(), false, true, false));

        while (!stack.isEmpty() && out.size() < request.maxPaths() && !controller.shouldStop(out.size())) {
            controller.checkpoint();
            Frame frame = stack.peek();
            long current = frame.nodeId;
            int hops = backwardNodePath.size() - 1;
            Integer head = fromSource.get(current);
            if (head == null || hops + head > request.maxHops()) {
                backtrack(stack, backwardNodePath, backwardEdgePath, visited);
                continue;
            }
            if (current == request.sourceNodeId()) {
                emitBackwardCandidate(snapshot, out, seen, backwardNodePath, backwardEdgePath,
                        frame.lowConfidence, frame.proven, controller, DFSResult.FROM_SINK_TO_SOURCE);
                backtrack(stack, backwardNodePath, backwardEdgePath, visited);
                continue;
            }
            if (hops >= request.maxHops() || frame.cursor >= frame.expansions.size()) {
                backtrack(stack, backwardNodePath, backwardEdgePath, visited);
                continue;
            }

            Expansion expansion = frame.expansions.get(frame.cursor++);
            long nextNodeId = expansion.edge.getSrcId();
            if (visited.contains(nextNodeId)) {
                continue;
            }
            visited.add(nextNodeId);
            backwardNodePath.add(nextNodeId);
            backwardEdgePath.add(expansion.edge);
            nodeVisits++;
            controller.onNode();

            ExpandResult next = expandBackward(snapshot, request, controller, nextNodeId, expansion.state,
                    hops + 1, fromSource, visited, sinkKind, policy, request.sourceNodeId());
            expandedEdges += next.expandedEdges();
            prunedByState += next.prunedByState();
            prunedByDistance += next.prunedByDistance();
            stack.push(new Frame(
                    nextNodeId,
                    expansion.state,
                    next.expansions(),
                    frame.lowConfidence || expansion.lowConfidence,
                    frame.proven && expansion.proven,
                    false
            ));
        }

        return new Run(out, new Stats(
                nodeVisits,
                expandedEdges,
                out.size(),
                prunedByState,
                prunedByDistance,
                elapsedMs(startNs)
        ));
    }

    private Run runBackwardAllSources(GraphSnapshot snapshot,
                                      ReverseSearchRequest request,
                                      Controller controller,
                                      String sinkKind,
                                      GraphTaintSemantics.PortState initialState,
                                      PruningPolicy.Resolved policy,
                                      long startNs) {
        int nodeVisits = 1;
        int expandedEdges = 0;
        int prunedByState = 0;
        int prunedByDistance = 0;
        List<Candidate> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Set<Long> emittedSources = new HashSet<>();
        List<Long> backwardNodePath = new ArrayList<>();
        List<GraphEdge> backwardEdgePath = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<Frame> stack = new ArrayDeque<>();

        backwardNodePath.add(request.sinkNodeId());
        visited.add(request.sinkNodeId());
        ExpandResult initial = expandBackward(snapshot, request, controller, request.sinkNodeId(), initialState,
                0, null, visited, sinkKind, policy, -1L);
        expandedEdges += initial.expandedEdges();
        prunedByState += initial.prunedByState();
        prunedByDistance += initial.prunedByDistance();
        stack.push(new Frame(request.sinkNodeId(), initialState, initial.expansions(), false, true, false));

        while (!stack.isEmpty() && out.size() < request.maxPaths() && !controller.shouldStop(out.size())) {
            controller.checkpoint();
            Frame frame = stack.peek();
            long current = frame.nodeId;
            int hops = backwardNodePath.size() - 1;

            if (!frame.targetEvaluated) {
                frame.targetEvaluated = true;
                boolean isSource = request.sourceNodeIds().contains(current);
                boolean emit = request.onlyMarkedSources()
                        ? isSource && emittedSources.add(current)
                        : (isSource || frame.expansions.isEmpty()) && emittedSources.add(current);
                if (emit) {
                    emitBackwardCandidate(snapshot, out, seen, backwardNodePath, backwardEdgePath,
                            frame.lowConfidence, frame.proven, controller, DFSResult.FROM_SOURCE_TO_ALL);
                    if (out.size() >= request.maxPaths()) {
                        break;
                    }
                    if (request.onlyMarkedSources()
                            && !request.sourceNodeIds().isEmpty()
                            && emittedSources.size() >= request.sourceNodeIds().size()) {
                        break;
                    }
                }
            }

            if (hops >= request.maxHops() || frame.cursor >= frame.expansions.size()) {
                backtrack(stack, backwardNodePath, backwardEdgePath, visited);
                continue;
            }

            Expansion expansion = frame.expansions.get(frame.cursor++);
            long nextNodeId = expansion.edge.getSrcId();
            if (visited.contains(nextNodeId)) {
                continue;
            }
            visited.add(nextNodeId);
            backwardNodePath.add(nextNodeId);
            backwardEdgePath.add(expansion.edge);
            nodeVisits++;
            controller.onNode();

            ExpandResult next = expandBackward(snapshot, request, controller, nextNodeId, expansion.state,
                    hops + 1, null, visited, sinkKind, policy, -1L);
            expandedEdges += next.expandedEdges();
            prunedByState += next.prunedByState();
            prunedByDistance += next.prunedByDistance();
            stack.push(new Frame(
                    nextNodeId,
                    expansion.state,
                    next.expansions(),
                    frame.lowConfidence || expansion.lowConfidence,
                    frame.proven && expansion.proven,
                    false
            ));
        }

        return new Run(out, new Stats(
                nodeVisits,
                expandedEdges,
                out.size(),
                prunedByState,
                prunedByDistance,
                elapsedMs(startNs)
        ));
    }

    private ExpandResult expandForward(GraphSnapshot snapshot,
                                       SearchRequest request,
                                       Controller controller,
                                       long nodeId,
                                       GraphTaintSemantics.PortState state,
                                       int currentHops,
                                       Map<Long, Integer> toTarget,
                                       Set<Long> visited,
                                       String sinkKind,
                                       PruningPolicy.Resolved policy,
                                       long targetNodeId) {
        if (currentHops >= request.maxHops()) {
            return ExpandResult.empty();
        }
        MethodReference.Handle currentHandle = semantics.toHandle(snapshot.getNode(nodeId));
        if (currentHandle == null) {
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
            MethodReference.Handle nextHandle = semantics.toHandle(snapshot.getNode(nextNodeId));
            GraphTaintSemantics.Transition transition =
                    semantics.transition(currentHandle, nextHandle, state, edge, sinkKind, policy);
            if (transition == null || transition.state() == null || transition.state().isEmpty()) {
                prunedByState++;
                continue;
            }
            out.add(new Expansion(
                    edge,
                    transition.state(),
                    transition.lowConfidence(),
                    transition.proven(),
                    score(edge, nextNodeId == targetNodeId, tail, transition)
            ));
        }
        out.sort(Comparator.comparingInt((Expansion expansion) -> expansion.score)
                .thenComparingLong(expansion -> expansion.edge.getEdgeId()));
        return new ExpandResult(out, expandedEdges, prunedByState, prunedByDistance);
    }

    private ExpandResult expandBackward(GraphSnapshot snapshot,
                                        ReverseSearchRequest request,
                                        Controller controller,
                                        long nodeId,
                                        GraphTaintSemantics.PortState state,
                                        int currentHops,
                                        Map<Long, Integer> fromSource,
                                        Set<Long> visited,
                                        String sinkKind,
                                        PruningPolicy.Resolved policy,
                                        long sourceNodeId) {
        if (currentHops >= request.maxHops()) {
            return ExpandResult.empty();
        }
        MethodReference.Handle currentHandle = semantics.toHandle(snapshot.getNode(nodeId));
        if (currentHandle == null) {
            return ExpandResult.empty();
        }
        List<Expansion> out = new ArrayList<>();
        int expandedEdges = 0;
        int prunedByState = 0;
        int prunedByDistance = 0;
        for (GraphEdge edge : snapshot.getIncomingView(nodeId)) {
            controller.onExpand();
            expandedEdges++;
            if (!GraphTraversalRules.isTraversable(snapshot,
                    edge,
                    request.blacklist(),
                    request.minEdgeConfidence(),
                    request.traversalMode().includesAlias())) {
                continue;
            }
            long nextNodeId = edge.getSrcId();
            if (visited.contains(nextNodeId)) {
                continue;
            }
            Integer head = fromSource == null ? null : fromSource.get(nextNodeId);
            if (fromSource != null && (head == null || currentHops + 1 + head > request.maxHops())) {
                prunedByDistance++;
                continue;
            }
            MethodReference.Handle nextHandle = semantics.toHandle(snapshot.getNode(nextNodeId));
            GraphTaintSemantics.Transition transition =
                    semantics.reverseTransition(nextHandle, currentHandle, state, edge, sinkKind, policy);
            if (transition == null || transition.state() == null || transition.state().isEmpty()) {
                prunedByState++;
                continue;
            }
            out.add(new Expansion(
                    edge,
                    transition.state(),
                    transition.lowConfidence(),
                    transition.proven(),
                    score(edge, nextNodeId == sourceNodeId, head == null ? 0 : head, transition)
            ));
        }
        out.sort(Comparator.comparingInt((Expansion expansion) -> expansion.score)
                .thenComparingLong(expansion -> expansion.edge.getEdgeId()));
        return new ExpandResult(out, expandedEdges, prunedByState, prunedByDistance);
    }

    private Map<Long, Integer> loadBackwardDistance(GraphSnapshot snapshot,
                                                    long targetNodeId,
                                                    int maxHops,
                                                    TraversalMode traversalMode,
                                                    Set<String> blacklist,
                                                    String minEdgeConfidence,
                                                    Controller controller,
                                                    DistanceCache distanceCache) {
        DistanceKey key = DistanceKey.backward(snapshot, targetNodeId, maxHops, traversalMode, blacklist, minEdgeConfidence);
        Map<Long, Integer> cached = distanceCache == null ? null : distanceCache.get(key);
        if (cached != null) {
            return cached;
        }
        Map<Long, Integer> dist = boundedBackwardDistance(
                snapshot,
                targetNodeId,
                maxHops,
                traversalMode,
                blacklist,
                minEdgeConfidence,
                controller
        );
        if (distanceCache != null) {
            distanceCache.put(key, dist);
        }
        return dist;
    }

    private Map<Long, Integer> loadForwardDistance(GraphSnapshot snapshot,
                                                   long sourceNodeId,
                                                   int maxHops,
                                                   TraversalMode traversalMode,
                                                   Set<String> blacklist,
                                                   String minEdgeConfidence,
                                                   Controller controller,
                                                   DistanceCache distanceCache) {
        DistanceKey key = DistanceKey.forward(snapshot, sourceNodeId, maxHops, traversalMode, blacklist, minEdgeConfidence);
        Map<Long, Integer> cached = distanceCache == null ? null : distanceCache.get(key);
        if (cached != null) {
            return cached;
        }
        Map<Long, Integer> dist = boundedForwardDistance(
                snapshot,
                sourceNodeId,
                maxHops,
                traversalMode,
                blacklist,
                minEdgeConfidence,
                controller
        );
        if (distanceCache != null) {
            distanceCache.put(key, dist);
        }
        return dist;
    }

    private Map<Long, Integer> boundedBackwardDistance(GraphSnapshot snapshot,
                                                       long targetNodeId,
                                                       int maxHops,
                                                       TraversalMode traversalMode,
                                                       Set<String> blacklist,
                                                       String minEdgeConfidence,
                                                       Controller controller) {
        Map<Long, Integer> dist = new HashMap<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        dist.put(targetNodeId, 0);
        queue.add(targetNodeId);
        while (!queue.isEmpty() && !controller.shouldStop(0)) {
            controller.checkpoint();
            long current = queue.poll();
            int hops = dist.getOrDefault(current, 0);
            if (hops >= maxHops) {
                continue;
            }
            for (GraphEdge edge : snapshot.getIncomingView(current)) {
                controller.onExpand();
                long prev = edge.getSrcId();
                if (!GraphTraversalRules.isTraversable(snapshot,
                        edge,
                        blacklist,
                        minEdgeConfidence,
                        traversalMode != null && traversalMode.includesAlias())) {
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

    private Map<Long, Integer> boundedForwardDistance(GraphSnapshot snapshot,
                                                      long sourceNodeId,
                                                      int maxHops,
                                                      TraversalMode traversalMode,
                                                      Set<String> blacklist,
                                                      String minEdgeConfidence,
                                                      Controller controller) {
        Map<Long, Integer> dist = new HashMap<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        dist.put(sourceNodeId, 0);
        queue.add(sourceNodeId);
        while (!queue.isEmpty() && !controller.shouldStop(0)) {
            controller.checkpoint();
            long current = queue.poll();
            int hops = dist.getOrDefault(current, 0);
            if (hops >= maxHops) {
                continue;
            }
            for (GraphEdge edge : snapshot.getOutgoingView(current)) {
                controller.onExpand();
                long next = edge.getDstId();
                if (!GraphTraversalRules.isTraversable(snapshot,
                        edge,
                        blacklist,
                        minEdgeConfidence,
                        traversalMode != null && traversalMode.includesAlias())) {
                    continue;
                }
                if (dist.containsKey(next)) {
                    continue;
                }
                dist.put(next, hops + 1);
                queue.add(next);
            }
        }
        return dist;
    }

    private static int score(GraphEdge edge,
                             boolean hitsTarget,
                             int distanceTail,
                             GraphTaintSemantics.Transition transition) {
        int score = distanceTail * 8;
        if (GraphTraversalRules.isAliasEdge(edge == null ? null : edge.getRelType())) {
            score += 220;
        }
        String confidence = GraphTraversalRules.normalizeConfidence(edge == null ? null : edge.getConfidence());
        if ("high".equals(confidence)) {
            score -= 100;
        } else if ("medium".equals(confidence)) {
            score -= 20;
        } else {
            score += 120;
        }
        if (hitsTarget) {
            score -= 1000;
        }
        if (transition != null && transition.proven()) {
            score -= 30;
        }
        if (transition != null && transition.lowConfidence()) {
            score += 40;
        }
        if (transition != null && transition.state() != null && transition.state().uncertain()) {
            score += 50;
        }
        return score;
    }

    private static void emitForwardCandidate(GraphSnapshot snapshot,
                                             List<Candidate> out,
                                             Set<String> seen,
                                             List<Long> nodePath,
                                             List<GraphEdge> edgePath,
                                             boolean lowConfidence,
                                             boolean proven,
                                             Controller controller) {
        DFSResult dfs = toDfsResult(snapshot, nodePath, edgePath, DFSResult.FROM_SOURCE_TO_SINK);
        if (dfs == null) {
            return;
        }
        String key = StableOrder.dfsPathKey(dfs);
        if (!seen.add(key)) {
            return;
        }
        out.add(new Candidate(List.copyOf(nodePath), List.copyOf(edgePath), dfs, lowConfidence, proven));
        controller.onPath();
    }

    private static void emitBackwardCandidate(GraphSnapshot snapshot,
                                              List<Candidate> out,
                                              Set<String> seen,
                                              List<Long> backwardNodePath,
                                              List<GraphEdge> backwardEdgePath,
                                              boolean lowConfidence,
                                              boolean proven,
                                              Controller controller,
                                              int mode) {
        Candidate candidate = toBackwardCandidate(snapshot, backwardNodePath, backwardEdgePath, lowConfidence, proven, mode);
        if (candidate == null || candidate.dfsResult() == null) {
            return;
        }
        String key = StableOrder.dfsPathKey(candidate.dfsResult());
        if (!seen.add(key)) {
            return;
        }
        out.add(candidate);
        controller.onPath();
    }

    private static Candidate toBackwardCandidate(GraphSnapshot snapshot,
                                                 List<Long> backwardNodePath,
                                                 List<GraphEdge> backwardEdgePath,
                                                 boolean lowConfidence,
                                                 boolean proven,
                                                 int mode) {
        if (backwardNodePath == null || backwardNodePath.isEmpty()) {
            return null;
        }
        List<Long> nodePath = new ArrayList<>(backwardNodePath.size());
        for (int i = backwardNodePath.size() - 1; i >= 0; i--) {
            nodePath.add(backwardNodePath.get(i));
        }
        List<GraphEdge> edgePath = new ArrayList<>(backwardEdgePath.size());
        for (int i = backwardEdgePath.size() - 1; i >= 0; i--) {
            edgePath.add(backwardEdgePath.get(i));
        }
        DFSResult dfs = toDfsResult(snapshot, nodePath, edgePath, mode);
        if (dfs == null) {
            return null;
        }
        return new Candidate(nodePath, edgePath, dfs, lowConfidence, proven);
    }

    private static DFSResult toDfsResult(GraphSnapshot snapshot,
                                         List<Long> nodePath,
                                         List<GraphEdge> edgePath,
                                         int mode) {
        if (snapshot == null || nodePath == null || nodePath.isEmpty()) {
            return null;
        }
        List<MethodReference.Handle> methods = new ArrayList<>();
        for (Long nodeId : nodePath) {
            GraphNode node = snapshot.getNode(nodeId);
            if (!GraphTraversalRules.isMethodNode(node)) {
                return null;
            }
            String className = node.getClassName();
            String methodName = node.getMethodName();
            String methodDesc = node.getMethodDesc();
            if (className == null || className.isBlank()
                    || methodName == null || methodName.isBlank()
                    || methodDesc == null || methodDesc.isBlank()) {
                return null;
            }
            MethodReference.Handle handle = new MethodReference.Handle(
                    new ClassReference.Handle(className, node.getJarId()),
                    methodName,
                    methodDesc
            );
            if (!methods.isEmpty() && methods.get(methods.size() - 1).equals(handle)) {
                continue;
            }
            methods.add(handle);
        }
        if (methods.isEmpty()) {
            return null;
        }
        List<DFSEdge> dfsEdges = new ArrayList<>();
        for (GraphEdge edge : edgePath) {
            GraphNode src = snapshot.getNode(edge.getSrcId());
            GraphNode dst = snapshot.getNode(edge.getDstId());
            if (!GraphTraversalRules.isMethodNode(src) || !GraphTraversalRules.isMethodNode(dst)) {
                continue;
            }
            DFSEdge dfsEdge = new DFSEdge();
            dfsEdge.setFrom(new MethodReference.Handle(
                    new ClassReference.Handle(src.getClassName(), src.getJarId()),
                    src.getMethodName(),
                    src.getMethodDesc()
            ));
            dfsEdge.setTo(new MethodReference.Handle(
                    new ClassReference.Handle(dst.getClassName(), dst.getJarId()),
                    dst.getMethodName(),
                    dst.getMethodDesc()
            ));
            dfsEdge.setType(relTypeToLegacyType(edge.getRelType()));
            dfsEdge.setConfidence(GraphTraversalRules.normalizeConfidence(edge.getConfidence()));
            dfsEdge.setEvidence(edge.getEvidence());
            dfsEdge.setCallSiteKey(edge.getCallSiteKey());
            dfsEdge.setLineNumber(edge.getLineNumber());
            dfsEdge.setCallIndex(edge.getCallIndex());
            dfsEdges.add(dfsEdge);
        }
        DFSResult result = new DFSResult();
        result.setMethodList(methods);
        result.setEdges(dfsEdges);
        result.setDepth(methods.size());
        result.setSource(methods.get(0));
        result.setSink(methods.get(methods.size() - 1));
        result.setMode(mode);
        return result;
    }

    private static String relTypeToLegacyType(String relType) {
        if (relType == null) {
            return "unknown";
        }
        return switch (relType) {
            case "CALLS_DISPATCH" -> "dispatch";
            case "CALLS_REFLECTION" -> "reflection";
            case "CALLS_CALLBACK" -> "callback";
            case "CALLS_OVERRIDE" -> "override";
            case "CALLS_INDY" -> "invoke_dynamic";
            case "CALLS_METHOD_HANDLE" -> "method_handle";
            case "CALLS_FRAMEWORK" -> "framework";
            case "CALLS_PTA" -> "pta";
            case "CALLS_DIRECT" -> "direct";
            default -> relType.toLowerCase();
        };
    }

    private static void backtrack(ArrayDeque<Frame> stack,
                                  List<Long> nodePath,
                                  List<GraphEdge> edgePath,
                                  Set<Long> visited) {
        Frame finished = stack.pop();
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

    private static Run emptyRun(long startNs) {
        return new Run(List.of(), new Stats(0, 0, 0, 0, 0, elapsedMs(startNs)));
    }

    private static long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
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

    public static final class DistanceCache {
        private final Map<DistanceKey, Map<Long, Integer>> distances = new HashMap<>();

        private Map<Long, Integer> get(DistanceKey key) {
            return key == null ? null : distances.get(key);
        }

        private void put(DistanceKey key, Map<Long, Integer> dist) {
            if (key == null || dist == null || dist.isEmpty()) {
                return;
            }
            distances.put(key, Map.copyOf(dist));
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
            blacklist = blacklist == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(blacklist));
            minEdgeConfidence = minEdgeConfidence == null || minEdgeConfidence.isBlank() ? "low" : minEdgeConfidence;
        }
    }

    public record ReverseSearchRequest(long sinkNodeId,
                                       long sourceNodeId,
                                       Set<Long> sourceNodeIds,
                                       boolean searchAllSources,
                                       boolean onlyMarkedSources,
                                       int maxHops,
                                       int maxPaths,
                                       TraversalMode traversalMode,
                                       Set<String> blacklist,
                                       String minEdgeConfidence) {
        public ReverseSearchRequest {
            maxHops = Math.max(1, maxHops);
            maxPaths = Math.max(1, maxPaths);
            traversalMode = traversalMode == null ? TraversalMode.CALL_ONLY : traversalMode;
            sourceNodeIds = sourceNodeIds == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(sourceNodeIds));
            blacklist = blacklist == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(blacklist));
            minEdgeConfidence = minEdgeConfidence == null || minEdgeConfidence.isBlank() ? "low" : minEdgeConfidence;
        }

        public boolean isSearchAllSources() {
            return searchAllSources;
        }
    }

    public record Candidate(List<Long> nodeIds,
                            List<GraphEdge> edges,
                            DFSResult dfsResult,
                            boolean lowConfidence,
                            boolean proven) {
        public Candidate {
            nodeIds = nodeIds == null ? List.of() : List.copyOf(nodeIds);
            edges = edges == null ? List.of() : List.copyOf(edges);
        }
    }

    public record Stats(int nodeVisits,
                        int expandedEdges,
                        int emittedPaths,
                        int prunedByState,
                        int prunedByDistance,
                        long elapsedMs) {
    }

    public record Run(List<Candidate> candidates, Stats stats) {
        public Run {
            candidates = candidates == null ? List.of() : List.copyOf(candidates);
            stats = stats == null ? new Stats(0, 0, 0, 0, 0, 0L) : stats;
        }
    }

    private record DistanceKey(long buildSeq,
                               int nodeCount,
                               int edgeCount,
                               boolean backward,
                               long anchorNodeId,
                               int maxHops,
                               TraversalMode traversalMode,
                               Set<String> blacklist,
                               String minEdgeConfidence) {
        private static DistanceKey backward(GraphSnapshot snapshot,
                                            long targetNodeId,
                                            int maxHops,
                                            TraversalMode traversalMode,
                                            Set<String> blacklist,
                                            String minEdgeConfidence) {
            return new DistanceKey(
                    snapshot == null ? -1L : snapshot.getBuildSeq(),
                    snapshot == null ? 0 : snapshot.getNodeCount(),
                    snapshot == null ? 0 : snapshot.getEdgeCount(),
                    true,
                    targetNodeId,
                    maxHops,
                    traversalMode == null ? TraversalMode.CALL_ONLY : traversalMode,
                    blacklist == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(blacklist)),
                    minEdgeConfidence == null || minEdgeConfidence.isBlank() ? "low" : minEdgeConfidence
            );
        }

        private static DistanceKey forward(GraphSnapshot snapshot,
                                           long sourceNodeId,
                                           int maxHops,
                                           TraversalMode traversalMode,
                                           Set<String> blacklist,
                                           String minEdgeConfidence) {
            return new DistanceKey(
                    snapshot == null ? -1L : snapshot.getBuildSeq(),
                    snapshot == null ? 0 : snapshot.getNodeCount(),
                    snapshot == null ? 0 : snapshot.getEdgeCount(),
                    false,
                    sourceNodeId,
                    maxHops,
                    traversalMode == null ? TraversalMode.CALL_ONLY : traversalMode,
                    blacklist == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(blacklist)),
                    minEdgeConfidence == null || minEdgeConfidence.isBlank() ? "low" : minEdgeConfidence
            );
        }
    }

    private static final class Frame {
        private final long nodeId;
        private final GraphTaintSemantics.PortState state;
        private final List<Expansion> expansions;
        private final boolean lowConfidence;
        private final boolean proven;
        private int cursor;
        private boolean targetEvaluated;

        private Frame(long nodeId,
                      GraphTaintSemantics.PortState state,
                      List<Expansion> expansions,
                      boolean lowConfidence,
                      boolean proven,
                      boolean targetEvaluated) {
            this.nodeId = nodeId;
            this.state = state;
            this.expansions = expansions == null ? List.of() : expansions;
            this.lowConfidence = lowConfidence;
            this.proven = proven;
            this.cursor = 0;
            this.targetEvaluated = targetEvaluated;
        }
    }

    private record Expansion(GraphEdge edge,
                             GraphTaintSemantics.PortState state,
                             boolean lowConfidence,
                             boolean proven,
                             int score) {
    }

    private record ExpandResult(List<Expansion> expansions,
                                int expandedEdges,
                                int prunedByState,
                                int prunedByDistance) {
        private static ExpandResult empty() {
            return new ExpandResult(List.of(), 0, 0, 0);
        }
    }
}
