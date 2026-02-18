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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GraphExecutor {
    private static final Pattern NODE_ID_PATTERN = Pattern.compile("(?is)(?:id\\s*\\(\\s*[a-z]\\s*\\)|[a-z]\\.node_id)\\s*=\\s*(?:\\$([A-Za-z0-9_]+)|([0-9]+))");
    private static final Pattern KIND_PATTERN = Pattern.compile("(?is)[a-z]\\.kind\\s*=\\s*['\"]?([A-Za-z_]+)['\"]?");

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
            return procedures.execute("ja.path.shortest", plan.getProcedureArgs(), params, options, snapshot);
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

        List<GraphNode> nodes = snapshot.getNodesByLabel(label);
        if (nodes.isEmpty()) {
            nodes = snapshot.getNodesByLabel("");
        }
        Long whereNodeId = resolveWhereNodeId(plan.getRawQuery(), params);
        String whereKind = resolveWhereKind(plan.getRawQuery());

        List<List<Object>> rows = new ArrayList<>();
        if (plan.isRelationshipPattern()) {
            List<String> columns = List.of(
                    "src_id", "src_kind", "src_class", "src_method", "rel_type", "dst_id", "dst_kind", "dst_class", "dst_method", "confidence", "evidence"
            );
            for (GraphNode node : nodes) {
                if (whereNodeId != null && whereNodeId != node.getNodeId()) {
                    continue;
                }
                if (!whereKind.isEmpty() && !whereKind.equalsIgnoreCase(node.getKind())) {
                    continue;
                }
                for (GraphEdge edge : snapshot.getOutgoing(node.getNodeId())) {
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

        List<String> columns = List.of(
                "node_id", "kind", "jar_id", "class_name", "method_name", "method_desc", "call_site_key", "line_number", "call_index"
        );
        for (GraphNode node : nodes) {
            if (whereNodeId != null && whereNodeId != node.getNodeId()) {
                continue;
            }
            if (!whereKind.isEmpty() && !whereKind.equalsIgnoreCase(node.getKind())) {
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

    private static int resolveLimit(LogicalPlan plan, QueryOptions options) {
        int optionLimit = options == null ? 500 : options.getMaxRows();
        if (plan.getLimit() == null || plan.getLimit() <= 0) {
            return optionLimit;
        }
        return Math.min(plan.getLimit(), optionLimit);
    }

    private static Long resolveWhereNodeId(String rawQuery, Map<String, Object> params) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        Matcher matcher = NODE_ID_PATTERN.matcher(rawQuery);
        if (!matcher.find()) {
            return null;
        }
        String key = matcher.group(1);
        if (key != null && !key.isBlank()) {
            Object value = params == null ? null : params.get(key.trim());
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
        String number = matcher.group(2);
        if (number == null || number.isBlank()) {
            return null;
        }
        return Long.parseLong(number);
    }

    private static String resolveWhereKind(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }
        Matcher matcher = KIND_PATTERN.matcher(rawQuery);
        if (!matcher.find()) {
            return "";
        }
        return safe(matcher.group(1));
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
}
