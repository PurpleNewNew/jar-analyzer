/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.flow;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.dfs.DFSEdge;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphTraversalRules;
import me.n1ar4.jar.analyzer.rules.SourceRuleSupport;
import me.n1ar4.jar.analyzer.utils.StableOrder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GraphDfsEngine {
    public DfsRun run(GraphSnapshot snapshot, FlowOptions options, AtomicBoolean cancelFlag) {
        long startNs = System.nanoTime();
        if (snapshot == null || snapshot.getNodeCount() <= 0 || snapshot.getEdgeCount() <= 0) {
            return new DfsRun(List.of(), FlowStats.empty());
        }
        FlowOptions effective = options == null ? FlowOptions.builder().build() : options;
        Budget budget = new Budget(effective, cancelFlag, startNs);
        List<DFSResult> out = new ArrayList<>();
        Set<String> seenPath = new HashSet<>();

        if (effective.isFromSink()) {
            if (effective.isSearchAllSources()) {
                runFindAllSources(snapshot, effective, budget, out, seenPath);
            } else {
                runPreciseFromSink(snapshot, effective, budget, out, seenPath);
            }
        } else {
            runPreciseFromSource(snapshot, effective, budget, out, seenPath);
        }

        FlowStats stats = buildStats(effective, budget, out.size());
        applyStats(out, stats);
        return new DfsRun(out, stats);
    }

    private void runPreciseFromSource(GraphSnapshot snapshot,
                                      FlowOptions options,
                                      Budget budget,
                                      List<DFSResult> out,
                                      Set<String> seenPath) {
        List<Long> sourceIds = resolveMethodNodeIds(snapshot,
                options.getSourceClass(),
                options.getSourceMethod(),
                options.getSourceDesc(),
                options.getSourceJarId());
        List<Long> sinkIds = resolveMethodNodeIds(snapshot,
                options.getSinkClass(),
                options.getSinkMethod(),
                options.getSinkDesc(),
                options.getSinkJarId());
        if (sourceIds.isEmpty() || sinkIds.isEmpty()) {
            return;
        }
        for (Long sourceId : sourceIds) {
            if (sourceId == null || sourceId <= 0L) {
                continue;
            }
            for (Long sinkId : sinkIds) {
                if (sinkId == null || sinkId <= 0L) {
                    continue;
                }
                if (budget.shouldStop(out.size())) {
                    return;
                }
                enumerateFromTo(snapshot, sourceId, sinkId, options, budget, out, seenPath);
            }
        }
    }

    private void runPreciseFromSink(GraphSnapshot snapshot,
                                    FlowOptions options,
                                    Budget budget,
                                    List<DFSResult> out,
                                    Set<String> seenPath) {
        List<Long> sourceIds = resolveMethodNodeIds(snapshot,
                options.getSourceClass(),
                options.getSourceMethod(),
                options.getSourceDesc(),
                options.getSourceJarId());
        List<Long> sinkIds = resolveMethodNodeIds(snapshot,
                options.getSinkClass(),
                options.getSinkMethod(),
                options.getSinkDesc(),
                options.getSinkJarId());
        if (sourceIds.isEmpty() || sinkIds.isEmpty()) {
            return;
        }
        for (Long sinkId : sinkIds) {
            if (sinkId == null || sinkId <= 0L) {
                continue;
            }
            for (Long sourceId : sourceIds) {
                if (sourceId == null || sourceId <= 0L) {
                    continue;
                }
                if (budget.shouldStop(out.size())) {
                    return;
                }
                enumerateBackwardToSource(snapshot, sinkId, sourceId, options, budget, out, seenPath);
            }
        }
    }

    private void runFindAllSources(GraphSnapshot snapshot,
                                   FlowOptions options,
                                   Budget budget,
                                   List<DFSResult> out,
                                   Set<String> seenPath) {
        List<Long> sinkIds = resolveMethodNodeIds(snapshot,
                options.getSinkClass(),
                options.getSinkMethod(),
                options.getSinkDesc(),
                options.getSinkJarId());
        if (sinkIds.isEmpty()) {
            return;
        }
        Set<Long> sourceNodeIds = resolveSourceNodeIds(snapshot, options.isOnlyFromWeb());
        for (Long sinkId : sinkIds) {
            if (sinkId == null || sinkId <= 0L) {
                continue;
            }
            if (budget.shouldStop(out.size())) {
                return;
            }
            enumerateBackwardSources(snapshot, sinkId, options, budget, out, seenPath, sourceNodeIds);
        }
    }

    private void enumerateFromTo(GraphSnapshot snapshot,
                                 long from,
                                 long to,
                                 FlowOptions options,
                                 Budget budget,
                                 List<DFSResult> out,
                                 Set<String> seenPath) {
        Map<Long, Integer> toTarget = boundedBackwardDistance(snapshot, to, options.getDepth(), options, budget, out.size());
        if (budget.shouldStop(out.size()) || !toTarget.containsKey(from)) {
            return;
        }
        List<Long> nodePath = new ArrayList<>();
        List<GraphEdge> edgePath = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<PathFrame> stack = new ArrayDeque<>();

        nodePath.add(from);
        visited.add(from);
        budget.onNode(out.size());

        List<GraphEdge> first = rankedOutgoing(snapshot, from, visited, to, 0, options.getDepth(), toTarget, options, budget, out.size());
        stack.push(new PathFrame(from, first));

        while (!stack.isEmpty() && !budget.shouldStop(out.size())) {
            PathFrame frame = stack.peek();
            long current = frame.nodeId;
            int currentHops = nodePath.size() - 1;
            Integer tail = toTarget.get(current);
            if (tail == null || currentHops + tail > options.getDepth()) {
                backtrack(stack, nodePath, edgePath, visited);
                continue;
            }

            if (current == to) {
                DFSResult result = toDfsResult(snapshot, nodePath, edgePath, options);
                if (result != null) {
                    String key = StableOrder.dfsPathKey(result);
                    if (seenPath.add(key)) {
                        out.add(result);
                        budget.onPath(out.size());
                    }
                }
                backtrack(stack, nodePath, edgePath, visited);
                continue;
            }

            if (currentHops >= options.getDepth() || frame.cursor >= frame.outgoing.size()) {
                backtrack(stack, nodePath, edgePath, visited);
                continue;
            }

            GraphEdge edge = frame.outgoing.get(frame.cursor++);
            long next = edge.getDstId();
            if (visited.contains(next)) {
                continue;
            }
            int nextHops = currentHops + 1;
            Integer nextTail = toTarget.get(next);
            if (nextTail == null || nextHops + nextTail > options.getDepth()) {
                continue;
            }
            if (budget.onEdge(out.size())) {
                return;
            }
            visited.add(next);
            nodePath.add(next);
            edgePath.add(edge);
            budget.onNode(out.size());
            List<GraphEdge> nextOutgoing = rankedOutgoing(snapshot, next, visited, to, nextHops, options.getDepth(), toTarget, options, budget, out.size());
            stack.push(new PathFrame(next, nextOutgoing));
        }
    }

    private void enumerateBackwardSources(GraphSnapshot snapshot,
                                          long sinkId,
                                          FlowOptions options,
                                          Budget budget,
                                          List<DFSResult> out,
                                          Set<String> seenPath,
                                          Set<Long> sourceNodeIds) {
        List<Long> nodePath = new ArrayList<>();
        List<GraphEdge> edgePath = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Set<Long> emittedSources = new HashSet<>();
        ArrayDeque<BackwardFrame> stack = new ArrayDeque<>();

        nodePath.add(sinkId);
        visited.add(sinkId);
        budget.onNode(out.size());
        stack.push(new BackwardFrame(sinkId, rankedIncoming(snapshot, sinkId, visited, options)));

        while (!stack.isEmpty() && !budget.shouldStop(out.size())) {
            BackwardFrame frame = stack.peek();
            long current = frame.nodeId;
            int hops = nodePath.size() - 1;

            if (!frame.sourceEvaluated) {
                frame.sourceEvaluated = true;
                boolean emit;
                if (options.isOnlyFromWeb()) {
                    emit = sourceNodeIds.contains(current) && emittedSources.add(current);
                } else {
                    emit = (sourceNodeIds.contains(current) || frame.incoming.isEmpty())
                            && emittedSources.add(current);
                }
                if (emit) {
                    DFSResult result = toSourceDfsResult(snapshot, nodePath, edgePath, options);
                    if (result != null) {
                        String key = StableOrder.dfsPathKey(result);
                        if (seenPath.add(key)) {
                            out.add(result);
                            budget.onPath(out.size());
                        }
                    }
                    if (options.isOnlyFromWeb()
                            && !sourceNodeIds.isEmpty()
                            && emittedSources.size() >= sourceNodeIds.size()) {
                        return;
                    }
                }
            }

            if (hops >= options.getDepth() || frame.cursor >= frame.incoming.size()) {
                stack.pop();
                if (!stack.isEmpty()) {
                    long removed = nodePath.remove(nodePath.size() - 1);
                    visited.remove(removed);
                    edgePath.remove(edgePath.size() - 1);
                }
                continue;
            }

            GraphEdge edge = frame.incoming.get(frame.cursor++);
            long caller = edge.getSrcId();
            if (visited.contains(caller)) {
                continue;
            }
            if (budget.onEdge(out.size())) {
                return;
            }
            visited.add(caller);
            nodePath.add(caller);
            edgePath.add(edge);
            budget.onNode(out.size());
            stack.push(new BackwardFrame(caller, rankedIncoming(snapshot, caller, visited, options)));
        }
    }

    private void enumerateBackwardToSource(GraphSnapshot snapshot,
                                           long sinkId,
                                           long sourceId,
                                           FlowOptions options,
                                           Budget budget,
                                           List<DFSResult> out,
                                           Set<String> seenPath) {
        Map<Long, Integer> fromSource = boundedForwardDistance(snapshot, sourceId, options.getDepth(), options, budget, out.size());
        if (budget.shouldStop(out.size()) || !fromSource.containsKey(sinkId)) {
            return;
        }
        List<Long> nodePath = new ArrayList<>();
        List<GraphEdge> edgePath = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<BackwardFrame> stack = new ArrayDeque<>();

        nodePath.add(sinkId);
        visited.add(sinkId);
        budget.onNode(out.size());

        List<GraphEdge> first = rankedIncoming(
                snapshot,
                sinkId,
                visited,
                sourceId,
                0,
                options.getDepth(),
                fromSource,
                options,
                budget,
                out.size()
        );
        stack.push(new BackwardFrame(sinkId, first));

        while (!stack.isEmpty() && !budget.shouldStop(out.size())) {
            BackwardFrame frame = stack.peek();
            long current = frame.nodeId;
            int currentHops = nodePath.size() - 1;
            Integer head = fromSource.get(current);
            if (head == null || currentHops + head > options.getDepth()) {
                backtrackBackward(stack, nodePath, edgePath, visited);
                continue;
            }

            if (current == sourceId) {
                DFSResult result = toSourceDfsResult(snapshot, nodePath, edgePath, options);
                if (result != null) {
                    String key = StableOrder.dfsPathKey(result);
                    if (seenPath.add(key)) {
                        out.add(result);
                        budget.onPath(out.size());
                    }
                }
                backtrackBackward(stack, nodePath, edgePath, visited);
                continue;
            }

            if (currentHops >= options.getDepth() || frame.cursor >= frame.incoming.size()) {
                backtrackBackward(stack, nodePath, edgePath, visited);
                continue;
            }

            GraphEdge edge = frame.incoming.get(frame.cursor++);
            long caller = edge.getSrcId();
            if (visited.contains(caller)) {
                continue;
            }
            int nextHops = currentHops + 1;
            Integer nextHead = fromSource.get(caller);
            if (nextHead == null || nextHops + nextHead > options.getDepth()) {
                continue;
            }
            if (budget.onEdge(out.size())) {
                return;
            }
            visited.add(caller);
            nodePath.add(caller);
            edgePath.add(edge);
            budget.onNode(out.size());
            List<GraphEdge> nextIncoming = rankedIncoming(
                    snapshot,
                    caller,
                    visited,
                    sourceId,
                    nextHops,
                    options.getDepth(),
                    fromSource,
                    options,
                    budget,
                    out.size()
            );
            stack.push(new BackwardFrame(caller, nextIncoming));
        }
    }

    private static void backtrack(ArrayDeque<PathFrame> stack,
                                  List<Long> nodePath,
                                  List<GraphEdge> edgePath,
                                  Set<Long> visited) {
        stack.pop();
        if (stack.isEmpty()) {
            return;
        }
        long removed = nodePath.remove(nodePath.size() - 1);
        visited.remove(removed);
        edgePath.remove(edgePath.size() - 1);
    }

    private static void backtrackBackward(ArrayDeque<BackwardFrame> stack,
                                          List<Long> nodePath,
                                          List<GraphEdge> edgePath,
                                          Set<Long> visited) {
        stack.pop();
        if (stack.isEmpty()) {
            return;
        }
        long removed = nodePath.remove(nodePath.size() - 1);
        visited.remove(removed);
        edgePath.remove(edgePath.size() - 1);
    }

    private Map<Long, Integer> boundedBackwardDistance(GraphSnapshot snapshot,
                                                       long target,
                                                       int maxHops,
                                                       FlowOptions options,
                                                       Budget budget,
                                                       int currentPaths) {
        Map<Long, Integer> dist = new HashMap<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        dist.put(target, 0);
        queue.add(target);
        while (!queue.isEmpty() && !budget.shouldStop(currentPaths)) {
            long node = queue.poll();
            int d = dist.get(node);
            if (d >= maxHops) {
                continue;
            }
            for (GraphEdge edge : snapshot.getIncomingView(node)) {
                if (!isTraversable(snapshot, edge, options)) {
                    continue;
                }
                if (budget.onEdge(currentPaths)) {
                    break;
                }
                long src = edge.getSrcId();
                int nd = d + 1;
                Integer old = dist.get(src);
                if (old == null || nd < old) {
                    dist.put(src, nd);
                    queue.add(src);
                }
            }
        }
        return dist;
    }

    private Map<Long, Integer> boundedForwardDistance(GraphSnapshot snapshot,
                                                      long source,
                                                      int maxHops,
                                                      FlowOptions options,
                                                      Budget budget,
                                                      int currentPaths) {
        Map<Long, Integer> dist = new HashMap<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        dist.put(source, 0);
        queue.add(source);
        while (!queue.isEmpty() && !budget.shouldStop(currentPaths)) {
            long node = queue.poll();
            int d = dist.get(node);
            if (d >= maxHops) {
                continue;
            }
            for (GraphEdge edge : snapshot.getOutgoingView(node)) {
                if (!isTraversable(snapshot, edge, options)) {
                    continue;
                }
                if (budget.onEdge(currentPaths)) {
                    break;
                }
                long dst = edge.getDstId();
                int nd = d + 1;
                Integer old = dist.get(dst);
                if (old == null || nd < old) {
                    dist.put(dst, nd);
                    queue.add(dst);
                }
            }
        }
        return dist;
    }

    private List<GraphEdge> rankedOutgoing(GraphSnapshot snapshot,
                                           long nodeId,
                                           Set<Long> visited,
                                           long target,
                                           int hops,
                                           int maxHops,
                                           Map<Long, Integer> toTarget,
                                           FlowOptions options,
                                           Budget budget,
                                           int currentPaths) {
        if (hops >= maxHops) {
            return List.of();
        }
        List<GraphEdge> out = new ArrayList<>();
        for (GraphEdge edge : snapshot.getOutgoingView(nodeId)) {
            if (!isTraversable(snapshot, edge, options)) {
                continue;
            }
            if (budget.shouldStop(currentPaths)) {
                break;
            }
            long next = edge.getDstId();
            if (visited.contains(next)) {
                continue;
            }
            Integer tail = toTarget.get(next);
            if (tail == null || hops + 1 + tail > maxHops) {
                continue;
            }
            out.add(edge);
        }
        out.sort(Comparator.comparingInt((GraphEdge e) -> scoreOutgoing(e, target, toTarget))
                .thenComparingLong(GraphEdge::getEdgeId));
        return out;
    }

    private List<GraphEdge> rankedIncoming(GraphSnapshot snapshot,
                                           long nodeId,
                                           Set<Long> visited,
                                           FlowOptions options) {
        List<GraphEdge> incoming = new ArrayList<>();
        for (GraphEdge edge : snapshot.getIncomingView(nodeId)) {
            if (!isTraversable(snapshot, edge, options)) {
                continue;
            }
            long src = edge.getSrcId();
            if (visited.contains(src)) {
                continue;
            }
            incoming.add(edge);
        }
        incoming.sort(Comparator.comparingLong(GraphEdge::getEdgeId));
        return incoming;
    }

    private List<GraphEdge> rankedIncoming(GraphSnapshot snapshot,
                                           long nodeId,
                                           Set<Long> visited,
                                           long source,
                                           int hops,
                                           int maxHops,
                                           Map<Long, Integer> fromSource,
                                           FlowOptions options,
                                           Budget budget,
                                           int currentPaths) {
        if (hops >= maxHops) {
            return List.of();
        }
        List<GraphEdge> incoming = new ArrayList<>();
        for (GraphEdge edge : snapshot.getIncomingView(nodeId)) {
            if (!isTraversable(snapshot, edge, options)) {
                continue;
            }
            if (budget.shouldStop(currentPaths)) {
                break;
            }
            long caller = edge.getSrcId();
            if (visited.contains(caller)) {
                continue;
            }
            Integer head = fromSource.get(caller);
            if (head == null || hops + 1 + head > maxHops) {
                continue;
            }
            incoming.add(edge);
        }
        incoming.sort(Comparator.comparingInt((GraphEdge e) -> scoreIncoming(e, source, fromSource))
                .thenComparingLong(GraphEdge::getEdgeId));
        return incoming;
    }

    private int scoreOutgoing(GraphEdge edge, long target, Map<Long, Integer> toTarget) {
        if (edge == null) {
            return Integer.MAX_VALUE;
        }
        int score = 0;
        if (edge.getDstId() == target) {
            score -= 1000;
        }
        String conf = GraphTraversalRules.normalizeConfidence(edge.getConfidence());
        if ("high".equals(conf)) {
            score -= 100;
        } else if ("medium".equals(conf)) {
            score -= 10;
        }
        Integer tail = toTarget.get(edge.getDstId());
        if (tail != null) {
            score += tail * 3;
        } else {
            score += 99;
        }
        return score;
    }

    private int scoreIncoming(GraphEdge edge, long source, Map<Long, Integer> fromSource) {
        if (edge == null) {
            return Integer.MAX_VALUE;
        }
        int score = 0;
        if (edge.getSrcId() == source) {
            score -= 1000;
        }
        String conf = GraphTraversalRules.normalizeConfidence(edge.getConfidence());
        if ("high".equals(conf)) {
            score -= 100;
        } else if ("medium".equals(conf)) {
            score -= 10;
        }
        Integer head = fromSource.get(edge.getSrcId());
        if (head != null) {
            score += head * 3;
        } else {
            score += 99;
        }
        return score;
    }

    private boolean isTraversable(GraphSnapshot snapshot, GraphEdge edge, FlowOptions options) {
        return GraphTraversalRules.isTraversable(
                snapshot, edge, options.getBlacklist(), options.getMinEdgeConfidence());
    }

    private DFSResult toDfsResult(GraphSnapshot snapshot,
                                  List<Long> nodePath,
                                  List<GraphEdge> edgePath,
                                  FlowOptions options) {
        if (nodePath == null || nodePath.isEmpty()) {
            return null;
        }
        List<MethodReference.Handle> handles = new ArrayList<>();
        for (Long nodeId : nodePath) {
            MethodReference.Handle handle = toHandle(snapshot.getNode(nodeId));
            if (handle == null) {
                return null;
            }
            if (!handles.isEmpty() && handles.get(handles.size() - 1).equals(handle)) {
                continue;
            }
            handles.add(handle);
        }
        if (handles.isEmpty()) {
            return null;
        }
        DFSResult result = new DFSResult();
        result.setMethodList(handles);
        result.setEdges(toDfsEdges(snapshot, edgePath));
        result.setDepth(handles.size());
        result.setSource(handles.get(0));
        result.setSink(handles.get(handles.size() - 1));
        if (options.isFromSink()) {
            result.setMode(options.isSearchAllSources() ? DFSResult.FROM_SOURCE_TO_ALL : DFSResult.FROM_SINK_TO_SOURCE);
        } else {
            result.setMode(DFSResult.FROM_SOURCE_TO_SINK);
        }
        return result;
    }

    private DFSResult toSourceDfsResult(GraphSnapshot snapshot,
                                        List<Long> backwardNodePath,
                                        List<GraphEdge> backwardEdgePath,
                                        FlowOptions options) {
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
        return toDfsResult(snapshot, nodePath, edgePath, options);
    }

    private List<DFSEdge> toDfsEdges(GraphSnapshot snapshot, List<GraphEdge> edgePath) {
        if (edgePath == null || edgePath.isEmpty()) {
            return List.of();
        }
        List<DFSEdge> edges = new ArrayList<>(edgePath.size());
        for (GraphEdge edge : edgePath) {
            GraphNode src = snapshot.getNode(edge.getSrcId());
            GraphNode dst = snapshot.getNode(edge.getDstId());
            MethodReference.Handle from = toHandle(src);
            MethodReference.Handle to = toHandle(dst);
            if (from == null || to == null) {
                continue;
            }
            DFSEdge d = new DFSEdge();
            d.setFrom(from);
            d.setTo(to);
            d.setType(relTypeToLegacyType(edge.getRelType()));
            d.setConfidence(GraphTraversalRules.normalizeConfidence(edge.getConfidence()));
            d.setEvidence(edge.getEvidence());
            d.setCallSiteKey(edge.getCallSiteKey());
            d.setLineNumber(edge.getLineNumber());
            d.setCallIndex(edge.getCallIndex());
            edges.add(d);
        }
        return edges;
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

    private MethodReference.Handle toHandle(GraphNode node) {
        if (!GraphTraversalRules.isMethodNode(node)) {
            return null;
        }
        String clazz = safe(node.getClassName()).replace('.', '/');
        String method = safe(node.getMethodName());
        String desc = safe(node.getMethodDesc());
        if (clazz.isBlank() || method.isBlank() || desc.isBlank()) {
            return null;
        }
        return new MethodReference.Handle(new ClassReference.Handle(clazz, node.getJarId()), method, desc);
    }

    private List<Long> resolveMethodNodeIds(GraphSnapshot snapshot,
                                            String className,
                                            String methodName,
                                            String methodDesc,
                                            Integer jarId) {
        String clazz = normalizeClass(className);
        String method = safe(methodName);
        String desc = safe(methodDesc);
        if (clazz.isBlank() || method.isBlank()) {
            return List.of();
        }
        Set<Long> out = new LinkedHashSet<>();
        if (!isAnyDesc(desc)) {
            for (GraphNode node : snapshot.getNodesByMethodSignatureView(clazz, method, desc)) {
                if (GraphTraversalRules.isMethodNode(node) && matchesJarId(node, jarId)) {
                    out.add(node.getNodeId());
                }
            }
        } else {
            for (GraphNode node : snapshot.getNodesByClassNameView(clazz)) {
                if (!GraphTraversalRules.isMethodNode(node)) {
                    continue;
                }
                if (!method.equals(node.getMethodName())) {
                    continue;
                }
                if (!matchesJarId(node, jarId)) {
                    continue;
                }
                out.add(node.getNodeId());
            }
        }
        return new ArrayList<>(out);
    }

    private static boolean matchesJarId(GraphNode node, Integer jarId) {
        if (jarId == null || jarId <= 0) {
            return true;
        }
        return node != null && node.getJarId() == jarId;
    }

    private Set<Long> resolveSourceNodeIds(GraphSnapshot snapshot, boolean onlyWeb) {
        Set<Long> out = new HashSet<>();
        if (snapshot == null) {
            return out;
        }
        SourceFlagResolver resolver = new SourceFlagResolver(SourceRuleSupport.snapshotCurrentRules());
        for (GraphNode node : snapshot.getNodesByKindView("method")) {
            if (!GraphTraversalRules.isMethodNode(node)) {
                continue;
            }
            int sourceFlags = resolver.resolve(node);
            if (onlyWeb) {
                if ((sourceFlags & GraphNode.SOURCE_FLAG_WEB) != 0) {
                    out.add(node.getNodeId());
                }
            } else {
                if ((sourceFlags & GraphNode.SOURCE_FLAG_ANY) != 0) {
                    out.add(node.getNodeId());
                }
            }
        }
        return out;
    }

    private static MethodReference findMethodReference(GraphNode node) {
        if (node == null || node.getClassName().isBlank() || node.getMethodName().isBlank() || node.getMethodDesc().isBlank()) {
            return null;
        }
        List<MethodReference> rows = DatabaseManager.getMethodReferencesByClass(node.getClassName());
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        MethodReference fallback = null;
        for (MethodReference row : rows) {
            if (row == null) {
                continue;
            }
            if (!node.getMethodName().equals(row.getName()) || !node.getMethodDesc().equals(row.getDesc())) {
                continue;
            }
            Integer jarId = row.getJarId();
            if (jarId != null && jarId == node.getJarId()) {
                return row;
            }
            if (fallback == null) {
                fallback = row;
            }
        }
        return fallback;
    }

    private static FlowStats buildStats(FlowOptions options, Budget budget, int pathCount) {
        FlowTruncation truncation = budget.truncation(options);
        return new FlowStats(
                budget.nodeCount,
                budget.edgeCount,
                pathCount,
                budget.elapsedMs(),
                truncation
        );
    }

    private static void applyStats(List<DFSResult> results, FlowStats stats) {
        if (results == null || results.isEmpty() || stats == null) {
            return;
        }
        FlowTruncation truncation = stats.getTruncation();
        for (DFSResult result : results) {
            if (result == null) {
                continue;
            }
            result.setTruncated(truncation.truncated());
            result.setTruncateReason(truncation.reason());
            result.setRecommend(truncation.recommend());
            result.setNodeCount(stats.getNodeCount());
            result.setEdgeCount(stats.getEdgeCount());
            result.setPathCount(stats.getPathCount());
            result.setElapsedMs(stats.getElapsedMs());
        }
    }

    private static String normalizeClass(String value) {
        return safe(value).replace('.', '/');
    }

    private static boolean isAnyDesc(String desc) {
        String value = safe(desc);
        return value.isBlank() || "*".equals(value) || "null".equalsIgnoreCase(value);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record DfsRun(List<DFSResult> results, FlowStats stats) {
        public DfsRun {
            results = results == null ? List.of() : results;
            stats = stats == null ? FlowStats.empty() : stats;
        }
    }

    private static final class PathFrame {
        private final long nodeId;
        private final List<GraphEdge> outgoing;
        private int cursor;

        private PathFrame(long nodeId, List<GraphEdge> outgoing) {
            this.nodeId = nodeId;
            this.outgoing = outgoing == null ? List.of() : outgoing;
            this.cursor = 0;
        }
    }

    private static final class BackwardFrame {
        private final long nodeId;
        private final List<GraphEdge> incoming;
        private int cursor;
        private boolean sourceEvaluated;

        private BackwardFrame(long nodeId, List<GraphEdge> incoming) {
            this.nodeId = nodeId;
            this.incoming = incoming == null ? List.of() : incoming;
            this.cursor = 0;
            this.sourceEvaluated = false;
        }
    }

    private static final class SourceFlagResolver {
        private final SourceRuleSupport.RuleSnapshot ruleSnapshot;
        private final Map<Long, Integer> resolvedFlags = new HashMap<>();

        private SourceFlagResolver(SourceRuleSupport.RuleSnapshot ruleSnapshot) {
            this.ruleSnapshot = ruleSnapshot;
        }

        private int resolve(GraphNode node) {
            if (node == null) {
                return 0;
            }
            Integer cached = resolvedFlags.get(node.getNodeId());
            if (cached != null) {
                return cached;
            }
            int flags = node.getSourceFlags();
            if (ruleSnapshot != null && !ruleSnapshot.isEmpty()) {
                flags |= SourceRuleSupport.resolveRuleFlags(findMethodReference(node), ruleSnapshot);
            }
            resolvedFlags.put(node.getNodeId(), flags);
            return flags;
        }
    }

    private static final class Budget {
        private final FlowOptions options;
        private final AtomicBoolean cancelFlag;
        private final long startNs;
        private final int pathLimit;
        private final boolean pathLimitByMaxPaths;
        private String truncateReason;
        private int nodeCount;
        private int edgeCount;

        private Budget(FlowOptions options, AtomicBoolean cancelFlag, long startNs) {
            this.options = options;
            this.cancelFlag = cancelFlag;
            this.startNs = startNs;
            this.pathLimit = options.resolvePathLimit();
            Integer maxPaths = options.getMaxPaths();
            this.pathLimitByMaxPaths = maxPaths != null && maxPaths > 0 && maxPaths < options.getMaxLimit();
        }

        private boolean onNode(int currentPaths) {
            nodeCount++;
            return shouldStop(currentPaths);
        }

        private boolean onEdge(int currentPaths) {
            edgeCount++;
            return shouldStop(currentPaths);
        }

        private boolean onPath(int currentPaths) {
            return shouldStop(currentPaths);
        }

        private boolean shouldStop(int currentPaths) {
            if (truncateReason != null && !truncateReason.isBlank()) {
                return true;
            }
            if (Thread.currentThread().isInterrupted()
                    || (cancelFlag != null && cancelFlag.get())) {
                truncateReason = "canceled";
                return true;
            }
            Integer timeoutMs = options.getTimeoutMs();
            if (timeoutMs != null && timeoutMs > 0 && elapsedMs() >= timeoutMs) {
                truncateReason = "timeout";
                return true;
            }
            Integer maxNodes = options.getMaxNodes();
            if (maxNodes != null && maxNodes > 0 && nodeCount >= maxNodes) {
                truncateReason = "maxNodes";
                return true;
            }
            Integer maxEdges = options.getMaxEdges();
            if (maxEdges != null && maxEdges > 0 && edgeCount >= maxEdges) {
                truncateReason = "maxEdges";
                return true;
            }
            if (pathLimit > 0 && currentPaths >= pathLimit) {
                truncateReason = pathLimitByMaxPaths ? "maxPaths" : "maxLimit";
                return true;
            }
            return false;
        }

        private long elapsedMs() {
            return (System.nanoTime() - startNs) / 1_000_000L;
        }

        private FlowTruncation truncation(FlowOptions options) {
            if (truncateReason == null || truncateReason.isBlank()) {
                return FlowTruncation.none();
            }
            return FlowTruncation.of(truncateReason, recommendation(truncateReason, options));
        }

        private static String recommendation(String reason, FlowOptions options) {
            if ("timeout".equals(reason)) {
                return "Try increase timeoutMs or reduce depth/maxLimit.";
            }
            if ("maxNodes".equals(reason) || "maxEdges".equals(reason)) {
                return "Try increase maxNodes/maxEdges or reduce depth.";
            }
            if ("maxPaths".equals(reason) || "maxLimit".equals(reason)) {
                return "Try increase maxPaths/maxLimit or narrow sink/source.";
            }
            if ("canceled".equals(reason)) {
                return "Flow task was canceled.";
            }
            return "Try reduce depth/maxLimit.";
        }
    }
}
