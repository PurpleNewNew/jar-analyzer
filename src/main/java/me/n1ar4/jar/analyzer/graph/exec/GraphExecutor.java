/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.exec;

import me.n1ar4.jar.analyzer.graph.plan.LogicalPlan;
import me.n1ar4.jar.analyzer.graph.proc.ProcedureRegistry;
import me.n1ar4.jar.analyzer.graph.query.QueryOptions;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GraphExecutor {
    private static final Pattern NODE_ID_PATTERN = Pattern.compile(
            "(?is)(?:id\\s*\\(\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*\\)|([A-Za-z_][A-Za-z0-9_]*)\\.node_id)\\s*=\\s*(?:\\$([A-Za-z0-9_]+)|([0-9]+))");
    private static final Pattern KIND_PATTERN = Pattern.compile(
            "(?is)([A-Za-z_][A-Za-z0-9_]*)\\.kind\\s*=\\s*['\"]?([A-Za-z_]+)['\"]?");
    private static final Pattern REL_TYPE_PATTERN = Pattern.compile(
            "(?is)-\\s*\\[[^\\]]*:\\s*([A-Za-z_][A-Za-z0-9_]*)[^\\]]*\\]\\s*[-]?>");
    private static final Pattern REL_TYPE_WHERE_PATTERN = Pattern.compile(
            "(?is)[A-Za-z_][A-Za-z0-9_]*\\.rel_type\\s*=\\s*['\"]?([A-Za-z_][A-Za-z0-9_]*)['\"]?");
    private static final Pattern REL_ALIAS_PATTERN = Pattern.compile(
            "(?is)\\(\\s*([A-Za-z_][A-Za-z0-9_]*)[^)]*\\)\\s*-\\s*\\[[^\\]]*\\]\\s*->\\s*\\(\\s*([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile(
            "(?is)([A-Za-z_][A-Za-z0-9_]*)\\.class_name\\s*=\\s*(?:\\$([A-Za-z0-9_]+)|'([^']*)'|\"([^\"]*)\")");
    private static final Pattern METHOD_NAME_PATTERN = Pattern.compile(
            "(?is)([A-Za-z_][A-Za-z0-9_]*)\\.method_name\\s*=\\s*(?:\\$([A-Za-z0-9_]+)|'([^']*)'|\"([^\"]*)\")");
    private static final Pattern METHOD_DESC_PATTERN = Pattern.compile(
            "(?is)([A-Za-z_][A-Za-z0-9_]*)\\.method_desc\\s*=\\s*(?:\\$([A-Za-z0-9_]+)|'([^']*)'|\"([^\"]*)\")");

    private final GraphStore store;
    private final ProcedureRegistry procedures;

    public GraphExecutor(GraphStore store, ProcedureRegistry procedures) {
        this.store = store == null ? new GraphStore() : store;
        this.procedures = procedures == null ? new ProcedureRegistry() : procedures;
    }

    public QueryResult execute(LogicalPlan plan, Map<String, Object> params, QueryOptions options) {
        if (plan == null) {
            return QueryResult.empty();
        }
        GraphSnapshot snapshot = store.loadSnapshot();
        if (plan.getKind() == LogicalPlan.Kind.PROCEDURE) {
            return procedures.execute(plan.getProcedureName(), plan.getProcedureArgs(), params, options, snapshot);
        }
        if (plan.isShortestPath()) {
            List<String> args = resolveShortestPathArgs(plan, params, options);
            return procedures.execute("ja.path.shortest", args, params, options, snapshot);
        }
        return executeMatch(plan, params, options, snapshot);
    }

    private QueryResult executeMatch(LogicalPlan plan,
                                     Map<String, Object> params,
                                     QueryOptions options,
                                     GraphSnapshot snapshot) {
        int limit = resolveLimit(plan, options);
        int skip = plan.getSkip() == null ? 0 : Math.max(0, plan.getSkip());
        String label = plan.getLabelHint();
        MatchFilters filters = resolveMatchFilters(plan.getRawQuery(), params);

        List<List<Object>> rows = new ArrayList<>();
        if (plan.isRelationshipPattern()) {
            List<GraphNode> nodes = resolveRelationshipCandidates(snapshot, label, filters);
            List<String> columns = List.of(
                    "src_id", "src_kind", "src_class", "src_method", "rel_type", "dst_id", "dst_kind", "dst_class", "dst_method", "confidence", "evidence"
            );
            for (GraphNode node : nodes) {
                if (filters.sourceNodeId != null && filters.sourceNodeId != node.getNodeId()) {
                    continue;
                }
                if (!filters.kind.isEmpty() && !filters.kind.equalsIgnoreCase(node.getKind())) {
                    continue;
                }
                if (!nodeMatchesClass(node, filters.className)
                        || !nodeMatchesMethod(node, filters.methodName)
                        || !nodeMatchesMethodDesc(node, filters.methodDesc)) {
                    continue;
                }
                for (GraphEdge edge : snapshot.getOutgoingView(node.getNodeId())) {
                    if (!filters.relType.isEmpty() && !filters.relType.equalsIgnoreCase(edge.getRelType())) {
                        continue;
                    }
                    if (filters.targetNodeId != null && filters.targetNodeId != edge.getDstId()) {
                        continue;
                    }
                    if (shouldApplyGenericNodeFilter(filters)
                            && !filters.nodeIds.contains(node.getNodeId())
                            && !filters.nodeIds.contains(edge.getDstId())) {
                        continue;
                    }
                    GraphNode dst = snapshot.getNode(edge.getDstId());
                    if (dst == null) {
                        continue;
                    }
                    rows.add(List.of(
                            node.getNodeId(),
                            node.getKind(),
                            node.getClassName(),
                            node.getMethodName(),
                            edge.getRelType(),
                            dst.getNodeId(),
                            dst.getKind(),
                            dst.getClassName(),
                            dst.getMethodName(),
                            edge.getConfidence(),
                            edge.getEvidence()
                    ));
                    if (rows.size() >= (skip + limit)) {
                        return new QueryResult(columns, slice(rows, skip, limit), List.of(), true);
                    }
                }
            }
            return new QueryResult(columns, slice(rows, skip, limit), List.of(), false);
        }

        List<GraphNode> nodes = resolveNodeCandidates(snapshot, label, filters);
        List<String> columns = List.of(
                "node_id", "kind", "jar_id", "class_name", "method_name", "method_desc", "call_site_key", "line_number", "call_index"
        );
        for (GraphNode node : nodes) {
            if (!filters.nodeIds.isEmpty() && !filters.nodeIds.contains(node.getNodeId())) {
                continue;
            }
            if (!filters.kind.isEmpty() && !filters.kind.equalsIgnoreCase(node.getKind())) {
                continue;
            }
            if (!nodeMatchesClass(node, filters.className)
                    || !nodeMatchesMethod(node, filters.methodName)
                    || !nodeMatchesMethodDesc(node, filters.methodDesc)) {
                continue;
            }
            rows.add(List.of(
                    node.getNodeId(),
                    node.getKind(),
                    node.getJarId(),
                    node.getClassName(),
                    node.getMethodName(),
                    node.getMethodDesc(),
                    node.getCallSiteKey(),
                    node.getLineNumber(),
                    node.getCallIndex()
            ));
            if (rows.size() >= (skip + limit)) {
                return new QueryResult(columns, slice(rows, skip, limit), List.of(), true);
            }
        }
        return new QueryResult(columns, slice(rows, skip, limit), List.of(), false);
    }

    private static List<GraphNode> resolveNodeCandidates(GraphSnapshot snapshot,
                                                         String label,
                                                         MatchFilters filters) {
        if (snapshot == null) {
            return List.of();
        }
        if (!filters.nodeIds.isEmpty()) {
            return resolveNodesByIds(snapshot, filters.nodeIds);
        }
        List<GraphNode> indexed = resolveIndexedCandidates(snapshot, filters);
        if (!indexed.isEmpty()) {
            return indexed;
        }
        if (!filters.kind.isEmpty()) {
            List<GraphNode> byKind = snapshot.getNodesByKindView(filters.kind);
            if (!byKind.isEmpty()) {
                return byKind;
            }
        }
        List<GraphNode> byLabel = snapshot.getNodesByLabelView(label);
        if (!byLabel.isEmpty()) {
            return byLabel;
        }
        return snapshot.getNodesByLabelView("");
    }

    private static List<GraphNode> resolveRelationshipCandidates(GraphSnapshot snapshot,
                                                                 String label,
                                                                 MatchFilters filters) {
        if (snapshot == null) {
            return List.of();
        }
        if (filters.sourceNodeId != null) {
            return resolveNodesByIds(snapshot, Set.of(filters.sourceNodeId));
        }
        if (filters.targetNodeId != null) {
            return resolveIncomingSources(snapshot, filters.targetNodeId, filters.relType);
        }
        return resolveNodeCandidates(snapshot, label, filters);
    }

    private static List<GraphNode> resolveIndexedCandidates(GraphSnapshot snapshot, MatchFilters filters) {
        if (snapshot == null || filters == null) {
            return List.of();
        }
        if (!filters.className.isEmpty() && !filters.methodName.isEmpty() && !filters.methodDesc.isEmpty()) {
            return snapshot.getNodesByMethodSignatureView(filters.className, filters.methodName, filters.methodDesc);
        }
        List<GraphNode> classCandidates = filters.className.isEmpty()
                ? List.of()
                : snapshot.getNodesByClassNameView(filters.className);
        List<GraphNode> methodCandidates = filters.methodName.isEmpty()
                ? List.of()
                : snapshot.getNodesByMethodNameView(filters.methodName);
        if (!classCandidates.isEmpty() && !methodCandidates.isEmpty()) {
            return intersectCandidates(classCandidates, methodCandidates);
        }
        if (!classCandidates.isEmpty()) {
            return classCandidates;
        }
        if (!methodCandidates.isEmpty()) {
            return methodCandidates;
        }
        return List.of();
    }

    private static List<GraphNode> intersectCandidates(List<GraphNode> left, List<GraphNode> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return List.of();
        }
        List<GraphNode> small = left.size() <= right.size() ? left : right;
        List<GraphNode> large = small == left ? right : left;
        Set<Long> idSet = new HashSet<>(small.size() * 2);
        for (GraphNode node : small) {
            if (node != null) {
                idSet.add(node.getNodeId());
            }
        }
        List<GraphNode> out = new ArrayList<>();
        for (GraphNode node : large) {
            if (node != null && idSet.contains(node.getNodeId())) {
                out.add(node);
            }
        }
        return out;
    }

    private static List<GraphNode> resolveIncomingSources(GraphSnapshot snapshot,
                                                          long targetNodeId,
                                                          String relType) {
        GraphNode target = snapshot.getNode(targetNodeId);
        if (target == null) {
            return List.of();
        }
        List<GraphNode> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (GraphEdge edge : snapshot.getIncomingView(targetNodeId)) {
            if (!relType.isEmpty() && !relType.equalsIgnoreCase(edge.getRelType())) {
                continue;
            }
            long srcId = edge.getSrcId();
            if (!seen.add(srcId)) {
                continue;
            }
            GraphNode src = snapshot.getNode(srcId);
            if (src != null) {
                out.add(src);
            }
        }
        return out;
    }

    private static List<GraphNode> resolveNodesByIds(GraphSnapshot snapshot, Set<Long> nodeIds) {
        if (snapshot == null || nodeIds == null || nodeIds.isEmpty()) {
            return List.of();
        }
        List<GraphNode> out = new ArrayList<>(nodeIds.size());
        for (Long id : nodeIds) {
            if (id == null || id <= 0L) {
                continue;
            }
            GraphNode node = snapshot.getNode(id);
            if (node != null) {
                out.add(node);
            }
        }
        return out;
    }

    private static int resolveLimit(LogicalPlan plan, QueryOptions options) {
        int optionLimit = options == null ? 500 : options.getMaxRows();
        if (plan.getLimit() == null || plan.getLimit() <= 0) {
            return optionLimit;
        }
        return Math.min(plan.getLimit(), optionLimit);
    }

    private static MatchFilters resolveMatchFilters(String rawQuery, Map<String, Object> params) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return MatchFilters.empty();
        }
        Map<String, Long> aliasNodeIds = resolveAliasNodeIds(rawQuery, params);
        Set<Long> nodeIds = new HashSet<>(aliasNodeIds.values());
        String kind = resolveWhereKind(rawQuery);
        String relType = resolveRelType(rawQuery);
        String className = normalizeClass(resolveWhereText(rawQuery, CLASS_NAME_PATTERN, params));
        String methodName = resolveWhereText(rawQuery, METHOD_NAME_PATTERN, params);
        String methodDesc = resolveWhereText(rawQuery, METHOD_DESC_PATTERN, params);
        RelationshipAliases aliases = resolveRelationshipAliases(rawQuery);
        Long sourceNodeId = aliases == null ? null : aliasNodeIds.getOrDefault(aliases.sourceAlias, null);
        Long targetNodeId = aliases == null ? null : aliasNodeIds.getOrDefault(aliases.targetAlias, null);
        return new MatchFilters(sourceNodeId, targetNodeId, nodeIds, kind, relType, className, methodName, methodDesc);
    }

    private static Map<String, Long> resolveAliasNodeIds(String rawQuery, Map<String, Object> params) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return Map.of();
        }
        Map<String, Long> out = new HashMap<>();
        Matcher matcher = NODE_ID_PATTERN.matcher(rawQuery);
        while (matcher.find()) {
            String alias = safe(matcher.group(1));
            if (alias.isEmpty()) {
                alias = safe(matcher.group(2));
            }
            Long value = resolveNodeIdToken(matcher.group(3), matcher.group(4), params);
            if (!alias.isEmpty() && value != null && value > 0L) {
                out.put(alias, value);
            }
        }
        return out;
    }

    private static String resolveWhereKind(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }
        Matcher matcher = KIND_PATTERN.matcher(rawQuery);
        if (!matcher.find()) {
            return "";
        }
        return safe(matcher.group(2));
    }

    private static String resolveRelType(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }
        Matcher patternType = REL_TYPE_PATTERN.matcher(rawQuery);
        if (patternType.find()) {
            return safe(patternType.group(1));
        }
        Matcher whereType = REL_TYPE_WHERE_PATTERN.matcher(rawQuery);
        if (whereType.find()) {
            return safe(whereType.group(1));
        }
        return "";
    }

    private static String resolveWhereText(String rawQuery, Pattern pattern, Map<String, Object> params) {
        if (rawQuery == null || rawQuery.isBlank() || pattern == null) {
            return "";
        }
        Matcher matcher = pattern.matcher(rawQuery);
        if (!matcher.find()) {
            return "";
        }
        return resolveValueToken(matcher.group(2), matcher.group(3), matcher.group(4), params);
    }

    private static String resolveValueToken(String paramKey,
                                            String singleQuoted,
                                            String doubleQuoted,
                                            Map<String, Object> params) {
        String key = safe(paramKey);
        if (!key.isEmpty()) {
            Object value = params == null ? null : params.get(key);
            return value == null ? "" : safe(String.valueOf(value));
        }
        String single = safe(singleQuoted);
        if (!single.isEmpty()) {
            return single;
        }
        return safe(doubleQuoted);
    }

    private static RelationshipAliases resolveRelationshipAliases(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        Matcher matcher = REL_ALIAS_PATTERN.matcher(rawQuery);
        if (!matcher.find()) {
            return null;
        }
        String sourceAlias = safe(matcher.group(1));
        String targetAlias = safe(matcher.group(2));
        if (sourceAlias.isEmpty() && targetAlias.isEmpty()) {
            return null;
        }
        return new RelationshipAliases(sourceAlias, targetAlias);
    }

    private static boolean shouldApplyGenericNodeFilter(MatchFilters filters) {
        return filters != null
                && !filters.nodeIds.isEmpty()
                && filters.sourceNodeId == null
                && filters.targetNodeId == null;
    }

    private static List<String> resolveShortestPathArgs(LogicalPlan plan,
                                                        Map<String, Object> params,
                                                        QueryOptions options) {
        if (plan == null) {
            return List.of("", "", "1");
        }
        long from = -1L;
        long to = -1L;
        String raw = plan.getRawQuery();
        if (raw != null && !raw.isBlank()) {
            Matcher matcher = NODE_ID_PATTERN.matcher(raw);
            while (matcher.find()) {
                Long value = resolveNodeIdToken(matcher.group(3), matcher.group(4), params);
                if (value == null || value <= 0L) {
                    continue;
                }
                if (from <= 0L) {
                    from = value;
                    continue;
                }
                if (to <= 0L && value != from) {
                    to = value;
                    break;
                }
            }
        }
        int hops = plan.getMaxHops() > 0
                ? plan.getMaxHops()
                : (options == null ? QueryOptions.defaults().getMaxHops() : options.getMaxHops());
        return List.of(
                from > 0L ? String.valueOf(from) : "",
                to > 0L ? String.valueOf(to) : "",
                String.valueOf(Math.max(1, hops))
        );
    }

    private static Long resolveNodeIdToken(String keyToken, String numberToken, Map<String, Object> params) {
        String key = safe(keyToken);
        if (!key.isEmpty()) {
            Object value = params == null ? null : params.get(key);
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value != null) {
                try {
                    return Long.parseLong(String.valueOf(value));
                } catch (Exception ignored) {
                    return null;
                }
            }
            return null;
        }
        String number = safe(numberToken);
        if (number.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(number);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<List<Object>> slice(List<List<Object>> rows, int skip, int limit) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        int from = Math.min(Math.max(0, skip), rows.size());
        int to = Math.min(rows.size(), from + Math.max(0, limit));
        if (from >= to) {
            return Collections.emptyList();
        }
        return new ArrayList<>(rows.subList(from, to));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeClass(String value) {
        return safe(value).replace('.', '/');
    }

    private static boolean nodeMatchesClass(GraphNode node, String className) {
        if (node == null) {
            return false;
        }
        if (className == null || className.isBlank()) {
            return true;
        }
        return normalizeClass(node.getClassName()).equals(className);
    }

    private static boolean nodeMatchesMethod(GraphNode node, String methodName) {
        if (node == null) {
            return false;
        }
        if (methodName == null || methodName.isBlank()) {
            return true;
        }
        return safe(node.getMethodName()).equals(methodName);
    }

    private static boolean nodeMatchesMethodDesc(GraphNode node, String methodDesc) {
        if (node == null) {
            return false;
        }
        if (methodDesc == null || methodDesc.isBlank()) {
            return true;
        }
        return safe(node.getMethodDesc()).equals(methodDesc);
    }

    private record MatchFilters(Long sourceNodeId,
                                Long targetNodeId,
                                Set<Long> nodeIds,
                                String kind,
                                String relType,
                                String className,
                                String methodName,
                                String methodDesc) {
        private static MatchFilters empty() {
            return new MatchFilters(null, null, Set.of(), "", "", "", "", "");
        }
    }

    private record RelationshipAliases(String sourceAlias, String targetAlias) {
    }
}
