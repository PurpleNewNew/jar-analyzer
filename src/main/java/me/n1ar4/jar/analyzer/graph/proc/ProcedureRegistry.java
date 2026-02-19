/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.proc;

import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.graph.flow.FlowOptions;
import me.n1ar4.jar.analyzer.graph.flow.GraphTaintEngine;
import me.n1ar4.jar.analyzer.graph.query.QueryOptions;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.taint.TaintResult;

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

public final class ProcedureRegistry {
    private static final int MAX_FAN_OUT = 96;
    private static final List<String> DEFAULT_COLUMNS = List.of(
            "path_id",
            "hop",
            "node_ids",
            "edge_ids",
            "score",
            "confidence",
            "evidence"
    );

    public QueryResult execute(String procName,
                               List<String> argExprs,
                               Map<String, Object> params,
                               QueryOptions options,
                               GraphSnapshot snapshot) {
        QueryOptions effectiveOptions = options == null ? QueryOptions.defaults() : options;
        QueryBudget budget = QueryBudget.of(effectiveOptions);
        String name = safe(procName).toLowerCase();
        if ("ja.path.shortest".equals(name)) {
            return shortest(argExprs, params, effectiveOptions, snapshot, budget);
        }
        if ("ja.path.from_to".equals(name)) {
            return fromTo(argExprs, params, effectiveOptions, snapshot, budget);
        }
        if ("ja.taint.track".equals(name)) {
            return taintTrack(argExprs, params, effectiveOptions, snapshot, budget);
        }
        throw new IllegalArgumentException("cypher_feature_not_supported");
    }

    private QueryResult shortest(List<String> argExprs,
                                 Map<String, Object> params,
                                 QueryOptions options,
                                 GraphSnapshot snapshot,
                                 QueryBudget budget) {
        long from = resolveNodeRef(resolveArg(argExprs, params, 0), snapshot);
        long to = resolveNodeRef(resolveArg(argExprs, params, 1), snapshot);
        int maxHops = toInt(resolveArg(argExprs, params, 2), options.getMaxHops());
        Path path = bidirectionalShortest(snapshot, from, to, maxHops, budget);
        ensureWithinBudget(budget);
        if (path == null) {
            return new QueryResult(DEFAULT_COLUMNS, List.of(), List.of("path_not_found"), false);
        }
        budget.onPath();
        return new QueryResult(DEFAULT_COLUMNS, List.of(toRow(1, path, "high", "ja.path.shortest")), List.of(), false);
    }

    private QueryResult fromTo(List<String> argExprs,
                               Map<String, Object> params,
                               QueryOptions options,
                               GraphSnapshot snapshot,
                               QueryBudget budget) {
        long from = resolveNodeRef(resolveArg(argExprs, params, 0), snapshot);
        long to = resolveNodeRef(resolveArg(argExprs, params, 1), snapshot);
        int maxHops = toInt(resolveArg(argExprs, params, 2), options.getMaxHops());
        int maxPaths = toInt(resolveArg(argExprs, params, 3), options.getMaxPaths());

        Map<Long, Integer> toTarget = boundedBackwardDistance(snapshot, to, maxHops, budget);
        if (!toTarget.containsKey(from)) {
            return new QueryResult(DEFAULT_COLUMNS, List.of(), List.of("path_not_found"), false);
        }

        List<Path> paths = new ArrayList<>();
        List<Long> nodePath = new ArrayList<>();
        List<Long> edgePath = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<DfsFrame> stack = new ArrayDeque<>();

        nodePath.add(from);
        visited.add(from);
        List<GraphEdge> firstOutgoing = rankedOutgoing(snapshot, from, visited, to, 0, maxHops, toTarget, maxPaths, budget);
        stack.push(new DfsFrame(from, firstOutgoing));

        while (!stack.isEmpty() && paths.size() < maxPaths) {
            checkBudget(budget);
            DfsFrame frame = stack.peek();
            long current = frame.nodeId;
            int currentHops = nodePath.size() - 1;

            Integer tail = toTarget.get(current);
            if (tail == null || currentHops + tail > maxHops) {
                backtrackFrame(stack, nodePath, edgePath, visited);
                continue;
            }

            if (current == to) {
                budget.onPath();
                paths.add(new Path(new ArrayList<>(nodePath), new ArrayList<>(edgePath)));
                backtrackFrame(stack, nodePath, edgePath, visited);
                continue;
            }

            if (currentHops >= maxHops || frame.cursor >= frame.outgoing.size()) {
                backtrackFrame(stack, nodePath, edgePath, visited);
                continue;
            }

            GraphEdge edge = frame.outgoing.get(frame.cursor++);
            long next = edge.getDstId();
            if (visited.contains(next)) {
                continue;
            }
            int nextHops = currentHops + 1;
            Integer nextTail = toTarget.get(next);
            if (nextTail == null || nextHops + nextTail > maxHops) {
                continue;
            }

            visited.add(next);
            nodePath.add(next);
            edgePath.add(edge.getEdgeId());
            List<GraphEdge> nextOutgoing = rankedOutgoing(snapshot, next, visited, to, nextHops, maxHops, toTarget, maxPaths, budget);
            stack.push(new DfsFrame(next, nextOutgoing));
        }

        List<List<Object>> rows = new ArrayList<>();
        int pathId = 1;
        for (Path path : paths) {
            rows.add(toRow(pathId++, path, "medium", "ja.path.from_to"));
        }
        boolean truncated = paths.size() >= maxPaths && !stack.isEmpty();
        return new QueryResult(DEFAULT_COLUMNS, rows, List.of(), truncated);
    }

    private QueryResult taintTrack(List<String> argExprs,
                                   Map<String, Object> params,
                                   QueryOptions options,
                                   GraphSnapshot snapshot,
                                   QueryBudget budget) {
        String sourceClass = normalizeClass(toString(resolveArg(argExprs, params, 0)));
        String sourceMethod = toString(resolveArg(argExprs, params, 1));
        String sourceDesc = normalizeDesc(toString(resolveArg(argExprs, params, 2)));
        String sinkClass = normalizeClass(toString(resolveArg(argExprs, params, 3)));
        String sinkMethod = toString(resolveArg(argExprs, params, 4));
        String sinkDesc = normalizeDesc(toString(resolveArg(argExprs, params, 5)));
        int depth = toInt(resolveArg(argExprs, params, 6), options.getMaxHops());
        Integer timeoutMs = toNullableInt(resolveArg(argExprs, params, 7));
        Integer maxPaths = toNullableInt(resolveArg(argExprs, params, 8));

        if (sourceClass.isEmpty() || sourceMethod.isEmpty() || sourceDesc.isEmpty()
                || sinkClass.isEmpty() || sinkMethod.isEmpty() || sinkDesc.isEmpty()) {
            throw new IllegalArgumentException("missing_param");
        }

        int effectiveTimeoutMs = Math.min(options.getMaxMs(), budget.remainingMsOrDefault(options.getMaxMs()));
        if (timeoutMs != null && timeoutMs > 0) {
            effectiveTimeoutMs = Math.min(effectiveTimeoutMs, timeoutMs);
        }
        Integer effectiveMaxPaths = null;
        if (maxPaths != null && maxPaths > 0) {
            effectiveMaxPaths = Math.min(maxPaths, options.getMaxPaths());
        }

        FlowOptions flowOptions = FlowOptions.builder()
                .fromSink(false)
                .searchAllSources(false)
                .depth(depth)
                .timeoutMs(effectiveTimeoutMs)
                .maxLimit(effectiveMaxPaths)
                .maxPaths(effectiveMaxPaths)
                .source(sourceClass, sourceMethod, sourceDesc)
                .sink(sinkClass, sinkMethod, sinkDesc)
                .build();
        GraphTaintEngine.TaintRun taintRun = new GraphTaintEngine().track(snapshot, flowOptions, null);
        ensureWithinBudget(budget);

        List<TaintResult> taintResults = taintRun.results();

        List<List<Object>> rows = new ArrayList<>();
        Set<String> warnings = new LinkedHashSet<>();
        int unresolvedNodes = 0;
        int pathId = 1;
        for (TaintResult taintResult : taintResults) {
            checkBudget(budget);
            budget.onPath();
            DFSResult dfs = taintResult.getDfsResult();
            Path mapped = mapTaintPathToGraph(dfs, snapshot, budget);
            unresolvedNodes += mapped.unresolvedCount;
            if (mapped.nodeIds.isEmpty()) {
                continue;
            }
            rows.add(toRow(pathId++, mapped,
                    taintResult.isLowConfidence() ? "low" : "high",
                    safe(taintResult.getTaintText())));
            if (rows.size() >= options.getMaxRows()) {
                break;
            }
        }
        if (unresolvedNodes > 0) {
            warnings.add("taint_unresolved_method_nodes=" + unresolvedNodes);
        }
        if (taintRun.stats().getTruncation().truncated()) {
            warnings.add("taint_truncated_reason=" + safe(taintRun.stats().getTruncation().reason()));
        }
        boolean truncated = taintResults.size() > rows.size()
                || taintRun.stats().getTruncation().truncated();
        return new QueryResult(DEFAULT_COLUMNS, rows, new ArrayList<>(warnings), truncated);
    }

    private static Path bidirectionalShortest(GraphSnapshot snapshot,
                                              long from,
                                              long to,
                                              int maxHops,
                                              QueryBudget budget) {
        if (from <= 0 || to <= 0) {
            return null;
        }
        if (from == to) {
            return new Path(List.of(from), List.of());
        }
        int bound = Math.max(1, maxHops);
        Map<Long, Integer> fromBound = boundedForwardDistance(snapshot, from, bound, budget);
        if (!fromBound.containsKey(to)) {
            return null;
        }
        Map<Long, Integer> toBound = boundedBackwardDistance(snapshot, to, bound, budget);
        if (!toBound.containsKey(from)) {
            return null;
        }

        ArrayDeque<Long> frontQ = new ArrayDeque<>();
        ArrayDeque<Long> backQ = new ArrayDeque<>();
        Map<Long, Integer> frontDist = new HashMap<>();
        Map<Long, Integer> backDist = new HashMap<>();
        Map<Long, PrevStep> frontPrev = new HashMap<>();
        Map<Long, NextStep> backNext = new HashMap<>();

        frontQ.add(from);
        backQ.add(to);
        frontDist.put(from, 0);
        backDist.put(to, 0);
        frontPrev.put(from, new PrevStep(-1L, -1L));
        backNext.put(to, new NextStep(-1L, -1L));

        int bestDistance = Integer.MAX_VALUE;
        long meetNode = -1L;

        while (!frontQ.isEmpty() && !backQ.isEmpty()) {
            checkBudget(budget);
            MeetCandidate candidate;
            if (frontQ.size() <= backQ.size()) {
                candidate = expandForwardLayer(snapshot, frontQ, frontDist, backDist, frontPrev, toBound, bound, budget);
            } else {
                candidate = expandBackwardLayer(snapshot, backQ, backDist, frontDist, backNext, fromBound, bound, budget);
            }
            if (candidate.distance < bestDistance) {
                bestDistance = candidate.distance;
                meetNode = candidate.meetNode;
            }
            if (meetNode <= 0L || bestDistance > bound) {
                continue;
            }
            int frontDepth = frontQ.isEmpty() ? Integer.MAX_VALUE : frontDist.get(frontQ.peek());
            int backDepth = backQ.isEmpty() ? Integer.MAX_VALUE : backDist.get(backQ.peek());
            if (frontDepth == Integer.MAX_VALUE || backDepth == Integer.MAX_VALUE || frontDepth + backDepth >= bestDistance) {
                break;
            }
        }

        if (meetNode <= 0L || bestDistance > bound) {
            return null;
        }
        return rebuildPath(meetNode, frontPrev, backNext);
    }

    private static MeetCandidate expandForwardLayer(GraphSnapshot snapshot,
                                                    ArrayDeque<Long> queue,
                                                    Map<Long, Integer> frontDist,
                                                    Map<Long, Integer> backDist,
                                                    Map<Long, PrevStep> frontPrev,
                                                    Map<Long, Integer> toTargetBound,
                                                    int maxHops,
                                                    QueryBudget budget) {
        int size = queue.size();
        MeetCandidate best = MeetCandidate.none();
        for (int i = 0; i < size; i++) {
            checkBudget(budget);
            long current = queue.poll();
            Integer currentDist = frontDist.get(current);
            if (currentDist == null || currentDist >= maxHops) {
                continue;
            }
            Integer tail = toTargetBound.get(current);
            if (tail == null || currentDist + tail > maxHops) {
                continue;
            }
            for (GraphEdge edge : snapshot.getOutgoingView(current)) {
                budget.onExpand();
                long next = edge.getDstId();
                if (!isTraversable(edge, current, next)) {
                    continue;
                }
                int nextDist = currentDist + 1;
                if (nextDist > maxHops) {
                    continue;
                }
                Integer nextTail = toTargetBound.get(next);
                if (nextTail == null || nextDist + nextTail > maxHops) {
                    continue;
                }
                Integer existed = frontDist.get(next);
                if (existed != null && existed <= nextDist) {
                    continue;
                }
                frontDist.put(next, nextDist);
                frontPrev.put(next, new PrevStep(current, edge.getEdgeId()));
                queue.add(next);
                Integer back = backDist.get(next);
                if (back != null) {
                    int total = nextDist + back;
                    if (total < best.distance) {
                        best = new MeetCandidate(next, total);
                    }
                }
            }
        }
        return best;
    }

    private static MeetCandidate expandBackwardLayer(GraphSnapshot snapshot,
                                                     ArrayDeque<Long> queue,
                                                     Map<Long, Integer> backDist,
                                                     Map<Long, Integer> frontDist,
                                                     Map<Long, NextStep> backNext,
                                                     Map<Long, Integer> fromSourceBound,
                                                     int maxHops,
                                                     QueryBudget budget) {
        int size = queue.size();
        MeetCandidate best = MeetCandidate.none();
        for (int i = 0; i < size; i++) {
            checkBudget(budget);
            long current = queue.poll();
            Integer currentDist = backDist.get(current);
            if (currentDist == null || currentDist >= maxHops) {
                continue;
            }
            Integer tail = fromSourceBound.get(current);
            if (tail == null || currentDist + tail > maxHops) {
                continue;
            }
            for (GraphEdge edge : snapshot.getIncomingView(current)) {
                budget.onExpand();
                long prev = edge.getSrcId();
                if (!isTraversable(edge, prev, current)) {
                    continue;
                }
                int nextDist = currentDist + 1;
                if (nextDist > maxHops) {
                    continue;
                }
                Integer nextTail = fromSourceBound.get(prev);
                if (nextTail == null || nextDist + nextTail > maxHops) {
                    continue;
                }
                Integer existed = backDist.get(prev);
                if (existed != null && existed <= nextDist) {
                    continue;
                }
                backDist.put(prev, nextDist);
                backNext.put(prev, new NextStep(current, edge.getEdgeId()));
                queue.add(prev);
                Integer front = frontDist.get(prev);
                if (front != null) {
                    int total = nextDist + front;
                    if (total < best.distance) {
                        best = new MeetCandidate(prev, total);
                    }
                }
            }
        }
        return best;
    }

    private static Path rebuildPath(long meetNode,
                                    Map<Long, PrevStep> frontPrev,
                                    Map<Long, NextStep> backNext) {
        List<Long> prefixNodes = new ArrayList<>();
        List<Long> prefixEdges = new ArrayList<>();
        long cursor = meetNode;
        while (cursor > 0L) {
            prefixNodes.add(cursor);
            PrevStep step = frontPrev.get(cursor);
            if (step == null || step.prevNode <= 0L) {
                break;
            }
            prefixEdges.add(step.viaEdgeId);
            cursor = step.prevNode;
        }
        Collections.reverse(prefixNodes);
        Collections.reverse(prefixEdges);

        List<Long> nodeIds = new ArrayList<>(prefixNodes);
        List<Long> edgeIds = new ArrayList<>(prefixEdges);
        long nextCursor = meetNode;
        while (true) {
            NextStep step = backNext.get(nextCursor);
            if (step == null || step.nextNode <= 0L) {
                break;
            }
            edgeIds.add(step.viaEdgeId);
            nodeIds.add(step.nextNode);
            nextCursor = step.nextNode;
        }
        return new Path(nodeIds, edgeIds);
    }

    private static Map<Long, Integer> boundedForwardDistance(GraphSnapshot snapshot,
                                                             long start,
                                                             int maxHops,
                                                             QueryBudget budget) {
        if (start <= 0L || maxHops <= 0) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> dist = new HashMap<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        dist.put(start, 0);
        queue.add(start);
        while (!queue.isEmpty()) {
            checkBudget(budget);
            long current = queue.poll();
            int hops = dist.get(current);
            if (hops >= maxHops) {
                continue;
            }
            for (GraphEdge edge : snapshot.getOutgoingView(current)) {
                budget.onExpand();
                long next = edge.getDstId();
                if (!isTraversable(edge, current, next) || dist.containsKey(next)) {
                    continue;
                }
                dist.put(next, hops + 1);
                queue.add(next);
            }
        }
        return dist;
    }

    private static Map<Long, Integer> boundedBackwardDistance(GraphSnapshot snapshot,
                                                              long target,
                                                              int maxHops,
                                                              QueryBudget budget) {
        if (target <= 0L || maxHops <= 0) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> dist = new HashMap<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        dist.put(target, 0);
        queue.add(target);
        while (!queue.isEmpty()) {
            checkBudget(budget);
            long current = queue.poll();
            int hops = dist.get(current);
            if (hops >= maxHops) {
                continue;
            }
            for (GraphEdge edge : snapshot.getIncomingView(current)) {
                budget.onExpand();
                long prev = edge.getSrcId();
                if (!isTraversable(edge, prev, current) || dist.containsKey(prev)) {
                    continue;
                }
                dist.put(prev, hops + 1);
                queue.add(prev);
            }
        }
        return dist;
    }

    private static List<GraphEdge> rankedOutgoing(GraphSnapshot snapshot,
                                                  long current,
                                                  Set<Long> visited,
                                                  long target,
                                                  int currentHops,
                                                  int maxHops,
                                                  Map<Long, Integer> toTarget,
                                                  int maxPaths,
                                                  QueryBudget budget) {
        List<ScoredEdge> scored = new ArrayList<>();
        for (GraphEdge edge : snapshot.getOutgoingView(current)) {
            budget.onExpand();
            long next = edge.getDstId();
            if (!isTraversable(edge, current, next) || visited.contains(next)) {
                continue;
            }
            int nextHops = currentHops + 1;
            if (nextHops > maxHops) {
                continue;
            }
            Integer tail = toTarget.get(next);
            if (tail == null || nextHops + tail > maxHops) {
                continue;
            }
            int score = confidenceScore(edge.getConfidence()) * 1000
                    + relationScore(edge.getRelType()) * 10
                    - tail;
            if (next == target) {
                score += 10_000;
            }
            scored.add(new ScoredEdge(edge, score));
        }
        if (scored.isEmpty()) {
            return List.of();
        }
        scored.sort(Comparator.comparingInt(ScoredEdge::score).reversed());
        int dynamicCap = Math.max(8, Math.min(MAX_FAN_OUT, Math.max(8, maxPaths / 2)));
        int limit = Math.min(dynamicCap, scored.size());
        List<GraphEdge> out = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            out.add(scored.get(i).edge);
        }
        return out;
    }

    private static void backtrackFrame(ArrayDeque<DfsFrame> stack,
                                       List<Long> nodePath,
                                       List<Long> edgePath,
                                       Set<Long> visited) {
        stack.pop();
        if (stack.isEmpty()) {
            return;
        }
        if (!nodePath.isEmpty()) {
            long removedNode = nodePath.remove(nodePath.size() - 1);
            visited.remove(removedNode);
        }
        if (!edgePath.isEmpty()) {
            edgePath.remove(edgePath.size() - 1);
        }
    }

    private static Path mapTaintPathToGraph(DFSResult dfs, GraphSnapshot snapshot, QueryBudget budget) {
        if (dfs == null || dfs.getMethodList() == null || dfs.getMethodList().isEmpty()) {
            return new Path(List.of(), List.of(), 0);
        }
        List<Long> nodeIds = new ArrayList<>();
        int unresolved = 0;
        for (MethodReference.Handle handle : dfs.getMethodList()) {
            long nodeId = resolveMethodNodeId(snapshot, handle);
            if (nodeId <= 0L) {
                unresolved++;
                continue;
            }
            if (!nodeIds.isEmpty() && nodeIds.get(nodeIds.size() - 1) == nodeId) {
                continue;
            }
            nodeIds.add(nodeId);
        }
        if (nodeIds.isEmpty()) {
            return new Path(List.of(), List.of(), unresolved);
        }
        List<Long> edgeIds = resolveEdgeChain(snapshot, nodeIds, budget);
        return new Path(nodeIds, edgeIds, unresolved);
    }

    private static List<Long> resolveEdgeChain(GraphSnapshot snapshot, List<Long> nodeIds, QueryBudget budget) {
        if (nodeIds == null || nodeIds.size() < 2) {
            return List.of();
        }
        List<Long> edgeIds = new ArrayList<>(Math.max(0, nodeIds.size() - 1));
        for (int i = 0; i + 1 < nodeIds.size(); i++) {
            long src = nodeIds.get(i);
            long dst = nodeIds.get(i + 1);
            GraphEdge best = null;
            int bestScore = Integer.MIN_VALUE;
            for (GraphEdge edge : snapshot.getOutgoingView(src)) {
                budget.onExpand();
                if (edge.getDstId() != dst) {
                    continue;
                }
                int score = confidenceScore(edge.getConfidence()) * 1000 + relationScore(edge.getRelType());
                if (score > bestScore) {
                    best = edge;
                    bestScore = score;
                }
            }
            if (best != null) {
                edgeIds.add(best.getEdgeId());
            }
        }
        return edgeIds;
    }

    private static long resolveMethodNodeId(GraphSnapshot snapshot, MethodReference.Handle handle) {
        if (snapshot == null || handle == null || handle.getClassReference() == null) {
            return -1L;
        }
        return snapshot.findMethodNodeId(
                handle.getClassReference().getName(),
                handle.getName(),
                handle.getDesc(),
                handle.getJarId()
        );
    }

    private static boolean isTraversable(GraphEdge edge, long src, long dst) {
        if (edge == null) {
            return false;
        }
        if (src <= 0L || dst <= 0L) {
            return false;
        }
        return src != dst;
    }

    private static int confidenceScore(String confidence) {
        String v = safe(confidence).toLowerCase();
        return switch (v) {
            case "high" -> 3;
            case "medium" -> 2;
            case "low" -> 1;
            default -> 0;
        };
    }

    private static int relationScore(String relation) {
        String rel = safe(relation).toUpperCase();
        return switch (rel) {
            case "CALLS_DIRECT" -> 9;
            case "CALLS_DISPATCH" -> 8;
            case "CALLS_REFLECTION" -> 7;
            case "CALLS_CALLBACK" -> 6;
            case "CALLS_OVERRIDE" -> 5;
            case "CALLSITE_TO_CALLEE" -> 4;
            case "CONTAINS_CALLSITE" -> 3;
            case "NEXT_CALLSITE" -> 2;
            default -> 1;
        };
    }

    private static List<Object> toRow(int pathId, Path path, String confidence, String evidence) {
        int hop = Math.max(0, path.nodeIds.size() - 1);
        double score = hop <= 0 ? 1.0 : (1.0 / hop);
        return List.of(
                pathId,
                hop,
                joinLongs(path.nodeIds),
                joinLongs(path.edgeIds),
                score,
                confidence,
                evidence
        );
    }

    private static String joinLongs(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Long value : values) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(value == null ? -1 : value);
        }
        return sb.toString();
    }

    private static Object resolveArg(List<String> argExprs, Map<String, Object> params, int index) {
        if (argExprs == null || index < 0 || index >= argExprs.size()) {
            return null;
        }
        String raw = safe(argExprs.get(index));
        if (raw.startsWith("$")) {
            String key = raw.substring(1).trim();
            if (params == null) {
                return null;
            }
            return params.get(key);
        }
        if ((raw.startsWith("\"") && raw.endsWith("\"")) || (raw.startsWith("'") && raw.endsWith("'"))) {
            return raw.substring(1, raw.length() - 1);
        }
        if ("true".equalsIgnoreCase(raw) || "false".equalsIgnoreCase(raw)) {
            return Boolean.parseBoolean(raw);
        }
        try {
            return Long.parseLong(raw);
        } catch (Exception ignored) {
            return raw;
        }
    }

    private static long resolveNodeRef(Object value, GraphSnapshot snapshot) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String raw = toString(value);
        if (raw.isEmpty()) {
            return -1L;
        }
        if (raw.startsWith("node:")) {
            try {
                return Long.parseLong(raw.substring("node:".length()));
            } catch (Exception ignored) {
                return -1L;
            }
        }
        if (raw.contains("#")) {
            String[] parts = raw.split("#", 4);
            if (parts.length >= 3) {
                String clazz = normalizeClass(parts[0]);
                String method = parts[1].trim();
                String desc = parts[2].trim();
                Integer jarId = null;
                if (parts.length == 4) {
                    jarId = toNullableInt(parts[3]);
                }
                long nodeId = snapshot.findMethodNodeId(clazz, method, desc, jarId);
                if (nodeId > 0L) {
                    return nodeId;
                }
            }
        }
        return -1L;
    }

    private static int toInt(Object value, int def) {
        if (value == null) {
            return def;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return def;
        }
    }

    private static Integer toNullableInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String toString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String normalizeClass(String value) {
        return safe(value).replace('.', '/');
    }

    private static String normalizeDesc(String value) {
        String v = safe(value);
        if (v.equalsIgnoreCase("null")) {
            return "*";
        }
        return v;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static void checkBudget(QueryBudget budget) {
        if (budget != null) {
            budget.checkpoint();
        }
    }

    private static void ensureWithinBudget(QueryBudget budget) {
        if (budget != null) {
            budget.ensureWithinDeadline();
        }
    }

    private static void throwTimeout() {
        throw new IllegalArgumentException("cypher_query_timeout");
    }

    private static void throwExpandBudgetExceeded() {
        throw new IllegalArgumentException("cypher_expand_budget_exceeded");
    }

    private static void throwPathBudgetExceeded() {
        throw new IllegalArgumentException("cypher_path_budget_exceeded");
    }

    private static final class DfsFrame {
        private final long nodeId;
        private final List<GraphEdge> outgoing;
        private int cursor;

        private DfsFrame(long nodeId, List<GraphEdge> outgoing) {
            this.nodeId = nodeId;
            this.outgoing = outgoing == null ? List.of() : outgoing;
            this.cursor = 0;
        }
    }

    private static final class QueryBudget {
        private static final int DEFAULT_INTERVAL = 128;
        private final long deadlineNs;
        private final long expandBudget;
        private final long pathBudget;
        private final int checkInterval;
        private long expansions;
        private long paths;
        private int ticks;

        private QueryBudget(long deadlineNs, long expandBudget, long pathBudget, int checkInterval) {
            this.deadlineNs = deadlineNs;
            this.expandBudget = expandBudget;
            this.pathBudget = pathBudget;
            this.checkInterval = Math.max(8, checkInterval);
            this.expansions = 0L;
            this.paths = 0L;
            this.ticks = 0;
        }

        private static QueryBudget of(QueryOptions options) {
            QueryOptions effective = options == null ? QueryOptions.defaults() : options;
            long deadlineNs = resolveDeadlineNs(effective.getMaxMs());
            return new QueryBudget(
                    deadlineNs,
                    Math.max(1L, effective.getExpandBudget()),
                    Math.max(1L, effective.getPathBudget()),
                    Math.max(DEFAULT_INTERVAL, effective.getTimeoutCheckInterval())
            );
        }

        private static long resolveDeadlineNs(int maxMs) {
            if (maxMs <= 0) {
                return Long.MAX_VALUE;
            }
            long now = System.nanoTime();
            long budgetNs = Math.max(1L, (long) maxMs) * 1_000_000L;
            long end = now + budgetNs;
            if (end < 0L) {
                end = Long.MAX_VALUE;
            }
            return end;
        }

        private void onExpand() {
            expansions++;
            if (expansions > expandBudget) {
                throwExpandBudgetExceeded();
            }
            if (checkInterval <= 1 || (expansions % checkInterval) == 0) {
                ensureWithinDeadline();
            }
        }

        private void onPath() {
            paths++;
            if (paths > pathBudget) {
                throwPathBudgetExceeded();
            }
            if (checkInterval <= 1 || (paths % checkInterval) == 0) {
                ensureWithinDeadline();
            }
        }

        private void checkpoint() {
            ticks++;
            if ((ticks & 0xFF) != 0) {
                return;
            }
            ensureWithinDeadline();
        }

        private int remainingMsOrDefault(int fallbackMs) {
            if (deadlineNs == Long.MAX_VALUE) {
                return fallbackMs;
            }
            long now = System.nanoTime();
            long remainNs = deadlineNs - now;
            if (remainNs <= 0L) {
                throwTimeout();
            }
            long remainMs = Math.max(1L, remainNs / 1_000_000L);
            if (remainMs > Integer.MAX_VALUE) {
                return fallbackMs;
            }
            return (int) Math.min(Math.max(1, fallbackMs), remainMs);
        }

        private void ensureWithinDeadline() {
            if (System.nanoTime() > deadlineNs) {
                throwTimeout();
            }
        }
    }

    private static final class Path {
        private final List<Long> nodeIds;
        private final List<Long> edgeIds;
        private final int unresolvedCount;

        private Path(List<Long> nodeIds, List<Long> edgeIds) {
            this(nodeIds, edgeIds, 0);
        }

        private Path(List<Long> nodeIds, List<Long> edgeIds, int unresolvedCount) {
            this.nodeIds = nodeIds;
            this.edgeIds = edgeIds;
            this.unresolvedCount = Math.max(0, unresolvedCount);
        }
    }

    private record PrevStep(long prevNode, long viaEdgeId) {
    }

    private record NextStep(long nextNode, long viaEdgeId) {
    }

    private record MeetCandidate(long meetNode, int distance) {
        private static MeetCandidate none() {
            return new MeetCandidate(-1L, Integer.MAX_VALUE);
        }
    }

    private record ScoredEdge(GraphEdge edge, int score) {
    }
}
