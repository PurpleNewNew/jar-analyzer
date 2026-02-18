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
import me.n1ar4.jar.analyzer.dfs.DFSEngine;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.dfs.DfsOutputs;
import me.n1ar4.jar.analyzer.graph.query.QueryOptions;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.taint.TaintAnalyzer;
import me.n1ar4.jar.analyzer.taint.TaintResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ProcedureRegistry {
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
        String name = safe(procName).toLowerCase();
        if ("ja.path.shortest".equals(name)) {
            return shortest(argExprs, params, options, snapshot);
        }
        if ("ja.path.from_to".equals(name)) {
            return fromTo(argExprs, params, options, snapshot);
        }
        if ("ja.taint.track".equals(name)) {
            return taintTrack(argExprs, params, options);
        }
        throw new IllegalArgumentException("cypher_feature_not_supported");
    }

    private QueryResult shortest(List<String> argExprs,
                                 Map<String, Object> params,
                                 QueryOptions options,
                                 GraphSnapshot snapshot) {
        long from = resolveNodeRef(resolveArg(argExprs, params, 0), snapshot);
        long to = resolveNodeRef(resolveArg(argExprs, params, 1), snapshot);
        int maxHops = toInt(resolveArg(argExprs, params, 2), options.getMaxHops());
        Path path = bfsShortest(snapshot, from, to, maxHops);
        if (path == null) {
            return new QueryResult(DEFAULT_COLUMNS, List.of(), List.of("path_not_found"), false);
        }
        return new QueryResult(DEFAULT_COLUMNS, List.of(toRow(1, path, "high", "ja.path.shortest")), List.of(), false);
    }

    private QueryResult fromTo(List<String> argExprs,
                               Map<String, Object> params,
                               QueryOptions options,
                               GraphSnapshot snapshot) {
        long from = resolveNodeRef(resolveArg(argExprs, params, 0), snapshot);
        long to = resolveNodeRef(resolveArg(argExprs, params, 1), snapshot);
        int maxHops = toInt(resolveArg(argExprs, params, 2), options.getMaxHops());
        int maxPaths = toInt(resolveArg(argExprs, params, 3), options.getMaxPaths());

        List<Path> paths = new ArrayList<>();
        ArrayDeque<PathState> stack = new ArrayDeque<>();
        Set<Long> firstVisited = new HashSet<>();
        firstVisited.add(from);
        stack.push(new PathState(List.of(from), List.of(), firstVisited));

        while (!stack.isEmpty() && paths.size() < maxPaths) {
            PathState state = stack.pop();
            if (state.nodeIds.get(state.nodeIds.size() - 1) == to) {
                paths.add(new Path(state.nodeIds, state.edgeIds));
                continue;
            }
            if (state.nodeIds.size() - 1 >= maxHops) {
                continue;
            }
            long current = state.nodeIds.get(state.nodeIds.size() - 1);
            for (GraphEdge edge : snapshot.getOutgoing(current)) {
                long next = edge.getDstId();
                if (state.visited.contains(next)) {
                    continue;
                }
                List<Long> nodeIds = new ArrayList<>(state.nodeIds);
                nodeIds.add(next);
                List<Long> edgeIds = new ArrayList<>(state.edgeIds);
                edgeIds.add(edge.getEdgeId());
                Set<Long> visited = new HashSet<>(state.visited);
                visited.add(next);
                stack.push(new PathState(nodeIds, edgeIds, visited));
            }
        }

        List<List<Object>> rows = new ArrayList<>();
        int pathId = 1;
        for (Path path : paths) {
            rows.add(toRow(pathId++, path, "medium", "ja.path.from_to"));
        }
        return new QueryResult(DEFAULT_COLUMNS, rows, List.of(), false);
    }

    private QueryResult taintTrack(List<String> argExprs,
                                   Map<String, Object> params,
                                   QueryOptions options) {
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

        DFSEngine dfsEngine = new DFSEngine(DfsOutputs.noop(), true, false, depth);
        dfsEngine.setSink(sinkClass, sinkMethod, sinkDesc);
        dfsEngine.setSource(sourceClass, sourceMethod, sourceDesc);
        if (timeoutMs != null && timeoutMs > 0) {
            dfsEngine.setTimeoutMs(timeoutMs);
        }
        if (maxPaths != null && maxPaths > 0) {
            dfsEngine.setMaxPaths(maxPaths);
        }
        dfsEngine.doAnalyze();
        List<DFSResult> dfsResults = dfsEngine.getResults();
        List<TaintResult> taintResults = TaintAnalyzer.analyze(dfsResults, timeoutMs, maxPaths);

        List<List<Object>> rows = new ArrayList<>();
        int pathId = 1;
        for (TaintResult taintResult : taintResults) {
            DFSResult dfs = taintResult.getDfsResult();
            List<Long> nodeIds = new ArrayList<>();
            if (dfs != null && dfs.getMethodList() != null) {
                for (MethodReference.Handle handle : dfs.getMethodList()) {
                    nodeIds.add((long) handle.hashCode());
                }
            }
            List<Long> edgeIds = List.of();
            Path path = new Path(nodeIds, edgeIds);
            rows.add(toRow(pathId++, path,
                    taintResult.isLowConfidence() ? "low" : "high",
                    safe(taintResult.getTaintText())));
            if (rows.size() >= options.getMaxRows()) {
                break;
            }
        }
        return new QueryResult(DEFAULT_COLUMNS, rows, List.of(), taintResults.size() > rows.size());
    }

    private static Path bfsShortest(GraphSnapshot snapshot, long from, long to, int maxHops) {
        if (from <= 0 || to <= 0) {
            return null;
        }
        if (from == to) {
            return new Path(List.of(from), List.of());
        }
        ArrayDeque<PathState> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        visited.add(from);
        queue.add(new PathState(List.of(from), List.of(), visited));
        while (!queue.isEmpty()) {
            PathState state = queue.poll();
            if (state.nodeIds.size() - 1 >= maxHops) {
                continue;
            }
            long current = state.nodeIds.get(state.nodeIds.size() - 1);
            for (GraphEdge edge : snapshot.getOutgoing(current)) {
                long next = edge.getDstId();
                if (state.visited.contains(next)) {
                    continue;
                }
                List<Long> nodeIds = new ArrayList<>(state.nodeIds);
                nodeIds.add(next);
                List<Long> edgeIds = new ArrayList<>(state.edgeIds);
                edgeIds.add(edge.getEdgeId());
                Set<Long> newVisited = new HashSet<>(state.visited);
                newVisited.add(next);
                PathState nextState = new PathState(nodeIds, edgeIds, newVisited);
                if (next == to) {
                    return new Path(nodeIds, edgeIds);
                }
                queue.add(nextState);
            }
        }
        return null;
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
            String[] parts = raw.split("#", 3);
            if (parts.length == 3) {
                String clazz = normalizeClass(parts[0]);
                String method = parts[1].trim();
                String desc = parts[2].trim();
                for (GraphNode node : snapshot.getNodesByLabel("method")) {
                    if (!"method".equalsIgnoreCase(node.getKind())) {
                        continue;
                    }
                    if (!clazz.equals(node.getClassName())) {
                        continue;
                    }
                    if (!method.equals(node.getMethodName())) {
                        continue;
                    }
                    if (!desc.equals(node.getMethodDesc())) {
                        continue;
                    }
                    return node.getNodeId();
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

    private static final class PathState {
        private final List<Long> nodeIds;
        private final List<Long> edgeIds;
        private final Set<Long> visited;

        private PathState(List<Long> nodeIds, List<Long> edgeIds, Set<Long> visited) {
            this.nodeIds = nodeIds;
            this.edgeIds = edgeIds;
            this.visited = visited;
        }
    }

    private static final class Path {
        private final List<Long> nodeIds;
        private final List<Long> edgeIds;

        private Path(List<Long> nodeIds, List<Long> edgeIds) {
            this.nodeIds = nodeIds;
            this.edgeIds = edgeIds;
        }
    }
}
