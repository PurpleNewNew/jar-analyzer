/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.cypher;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONWriter;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.GraphFramePayload;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ProjectGraphRequest;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.QueryFramePayload;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class QueryResultProjector {
    public static final int DEFAULT_GRAPH_NODE_BUDGET = 2000;
    public static final int DEFAULT_GRAPH_EDGE_BUDGET = 6000;

    private final int graphNodeBudget;
    private final int graphEdgeBudget;

    public QueryResultProjector() {
        this(DEFAULT_GRAPH_NODE_BUDGET, DEFAULT_GRAPH_EDGE_BUDGET);
    }

    public QueryResultProjector(int graphNodeBudget, int graphEdgeBudget) {
        this.graphNodeBudget = Math.max(32, graphNodeBudget);
        this.graphEdgeBudget = Math.max(64, graphEdgeBudget);
    }

    public QueryFramePayload toFrame(String frameId,
                                     String query,
                                     QueryResult result,
                                     long elapsedMs,
                                     GraphSnapshot snapshot) {
        QueryResult safeResult = result == null ? QueryResult.empty() : result;
        List<String> columns = safeList(safeResult.getColumns());
        List<List<Object>> rows = safeRows(safeResult.getRows());
        List<String> warnings = safeList(safeResult.getWarnings());
        GraphFramePayload graph = projectGraph(
                new ProjectGraphRequest(query, columns, rows, warnings, safeResult.isTruncated()),
                snapshot
        );
        String text = renderText(frameId, query, columns, rows, warnings, safeResult.isTruncated(), elapsedMs, graph);
        return new QueryFramePayload(
                safe(frameId),
                safe(query),
                columns,
                rows,
                warnings,
                safeResult.isTruncated(),
                Math.max(0L, elapsedMs),
                graph,
                text
        );
    }

    public GraphFramePayload projectGraph(ProjectGraphRequest request,
                                          GraphSnapshot snapshot) {
        if (request == null || snapshot == null) {
            return emptyGraph(List.of("graph_not_available"), false);
        }
        List<String> columns = safeList(request.columns());
        List<List<Object>> rows = safeRows(request.rows());
        if (columns.isEmpty() || rows.isEmpty()) {
            return emptyGraph(request.warnings(), request.truncated());
        }

        Map<String, Integer> indexes = index(columns);
        int srcIdx = first(indexes, "src_id", "source", "from_id");
        int dstIdx = first(indexes, "dst_id", "target", "to_id");
        int nodeIdsIdx = first(indexes, "node_ids");
        int edgeIdsIdx = first(indexes, "edge_ids");

        if (srcIdx >= 0 && dstIdx >= 0) {
            return projectRelationshipRows(rows, indexes, snapshot, request.warnings(), request.truncated());
        }
        if (nodeIdsIdx >= 0 || edgeIdsIdx >= 0) {
            return projectPathRows(rows, nodeIdsIdx, edgeIdsIdx, snapshot, request.warnings(), request.truncated());
        }
        return emptyGraph(request.warnings(), request.truncated());
    }

    private GraphFramePayload projectRelationshipRows(List<List<Object>> rows,
                                                      Map<String, Integer> indexes,
                                                      GraphSnapshot snapshot,
                                                      List<String> requestWarnings,
                                                      boolean requestTruncated) {
        int srcIdx = first(indexes, "src_id", "source", "from_id");
        int dstIdx = first(indexes, "dst_id", "target", "to_id");
        int edgeIdIdx = first(indexes, "edge_id");
        int relIdx = first(indexes, "rel_type", "type");
        int confidenceIdx = first(indexes, "confidence");
        int evidenceIdx = first(indexes, "evidence");

        LinkedHashMap<Long, GraphFramePayload.Node> nodes = new LinkedHashMap<>();
        LinkedHashMap<Long, GraphFramePayload.Edge> edges = new LinkedHashMap<>();
        Set<String> syntheticEdgeKeys = new HashSet<>();
        LinkedHashSet<String> warnings = mergeWarnings(requestWarnings);
        boolean budgetTruncated = false;

        for (List<Object> row : rows) {
            long srcId = parseLong(at(row, srcIdx));
            long dstId = parseLong(at(row, dstIdx));
            if (srcId <= 0L || dstId <= 0L) {
                continue;
            }
            if (nodes.size() >= graphNodeBudget && !nodes.containsKey(srcId)) {
                budgetTruncated = true;
                continue;
            }
            if (nodes.size() >= graphNodeBudget && !nodes.containsKey(dstId)) {
                budgetTruncated = true;
                continue;
            }
            putNode(nodes, snapshot, srcId);
            putNode(nodes, snapshot, dstId);

            String relType = safeText(at(row, relIdx));
            String confidence = safeText(at(row, confidenceIdx));
            String evidence = safeText(at(row, evidenceIdx));
            long edgeId = parseLong(at(row, edgeIdIdx));
            if (edgeId > 0L) {
                if (edges.size() >= graphEdgeBudget && !edges.containsKey(edgeId)) {
                    budgetTruncated = true;
                    continue;
                }
                edges.putIfAbsent(edgeId,
                        new GraphFramePayload.Edge(edgeId, srcId, dstId, relType, confidence, evidence));
            } else {
                if (edges.size() >= graphEdgeBudget) {
                    budgetTruncated = true;
                    continue;
                }
                String key = srcId + "->" + dstId + "#" + relType + "#" + confidence + "#" + evidence;
                if (!syntheticEdgeKeys.add(key)) {
                    continue;
                }
                long syntheticId = syntheticEdgeId(syntheticEdgeKeys.size());
                edges.put(syntheticId,
                        new GraphFramePayload.Edge(syntheticId, srcId, dstId, relType, confidence, evidence));
            }
        }

        if (budgetTruncated) {
            warnings.add("graph_render_truncated_by_budget(nodes=" + graphNodeBudget + ",edges=" + graphEdgeBudget + ")");
        }
        return new GraphFramePayload(
                new ArrayList<>(nodes.values()),
                new ArrayList<>(edges.values()),
                new ArrayList<>(warnings),
                requestTruncated || budgetTruncated
        );
    }

    private GraphFramePayload projectPathRows(List<List<Object>> rows,
                                              int nodeIdsIdx,
                                              int edgeIdsIdx,
                                              GraphSnapshot snapshot,
                                              List<String> requestWarnings,
                                              boolean requestTruncated) {
        LinkedHashSet<String> warnings = mergeWarnings(requestWarnings);
        List<PathRow> parsedRows = new ArrayList<>();
        Set<Long> requiredEdgeIds = new HashSet<>();

        for (List<Object> row : rows) {
            List<Long> nodeIds = nodeIdsIdx >= 0 ? parseLongList(at(row, nodeIdsIdx)) : List.of();
            List<Long> edgeIds = edgeIdsIdx >= 0 ? parseLongList(at(row, edgeIdsIdx)) : List.of();
            parsedRows.add(new PathRow(nodeIds, edgeIds));
            requiredEdgeIds.addAll(edgeIds);
        }

        Map<Long, GraphEdge> edgeById = buildEdgeIndex(snapshot, requiredEdgeIds);
        LinkedHashMap<Long, GraphFramePayload.Node> nodes = new LinkedHashMap<>();
        LinkedHashMap<Long, GraphFramePayload.Edge> edges = new LinkedHashMap<>();
        Set<String> syntheticKeys = new HashSet<>();
        boolean budgetTruncated = false;

        for (PathRow row : parsedRows) {
            List<Long> nodeIds = row.nodeIds;
            List<Long> edgeIds = row.edgeIds;
            for (Long nodeId : nodeIds) {
                if (nodeId == null || nodeId <= 0L) {
                    continue;
                }
                if (nodes.size() >= graphNodeBudget && !nodes.containsKey(nodeId)) {
                    budgetTruncated = true;
                    continue;
                }
                putNode(nodes, snapshot, nodeId);
            }

            if (!edgeIds.isEmpty()) {
                for (Long edgeId : edgeIds) {
                    if (edgeId == null || edgeId <= 0L) {
                        continue;
                    }
                    GraphEdge edge = edgeById.get(edgeId);
                    if (edge == null) {
                        warnings.add("graph_edge_missing:" + edgeId);
                        continue;
                    }
                    if (edges.size() >= graphEdgeBudget && !edges.containsKey(edgeId)) {
                        budgetTruncated = true;
                        continue;
                    }
                    putNode(nodes, snapshot, edge.getSrcId());
                    putNode(nodes, snapshot, edge.getDstId());
                    edges.putIfAbsent(edgeId, toEdge(edge));
                }
                continue;
            }

            for (int i = 0; i + 1 < nodeIds.size(); i++) {
                long srcId = safeLong(nodeIds.get(i));
                long dstId = safeLong(nodeIds.get(i + 1));
                if (srcId <= 0L || dstId <= 0L) {
                    continue;
                }
                GraphEdge edge = findEdge(snapshot, srcId, dstId);
                if (edge != null) {
                    long edgeId = edge.getEdgeId();
                    if (edges.size() >= graphEdgeBudget && !edges.containsKey(edgeId)) {
                        budgetTruncated = true;
                        continue;
                    }
                    edges.putIfAbsent(edgeId, toEdge(edge));
                    continue;
                }
                if (edges.size() >= graphEdgeBudget) {
                    budgetTruncated = true;
                    continue;
                }
                String key = srcId + "->" + dstId;
                if (!syntheticKeys.add(key)) {
                    continue;
                }
                long syntheticId = syntheticEdgeId(syntheticKeys.size());
                edges.put(syntheticId,
                        new GraphFramePayload.Edge(syntheticId, srcId, dstId, "PATH", "", "derived_path"));
            }
        }

        if (budgetTruncated) {
            warnings.add("graph_render_truncated_by_budget(nodes=" + graphNodeBudget + ",edges=" + graphEdgeBudget + ")");
        }
        return new GraphFramePayload(
                new ArrayList<>(nodes.values()),
                new ArrayList<>(edges.values()),
                new ArrayList<>(warnings),
                requestTruncated || budgetTruncated
        );
    }

    private static GraphEdge findEdge(GraphSnapshot snapshot, long srcId, long dstId) {
        for (GraphEdge edge : snapshot.getOutgoingView(srcId)) {
            if (edge != null && edge.getDstId() == dstId) {
                return edge;
            }
        }
        return null;
    }

    private static Map<Long, GraphEdge> buildEdgeIndex(GraphSnapshot snapshot, Set<Long> requiredEdgeIds) {
        if (snapshot == null || requiredEdgeIds == null || requiredEdgeIds.isEmpty()) {
            return Map.of();
        }
        Set<Long> wanted = new HashSet<>(requiredEdgeIds);
        Map<Long, GraphEdge> out = new HashMap<>();
        for (GraphNode node : snapshot.getNodesView()) {
            if (node == null) {
                continue;
            }
            for (GraphEdge edge : snapshot.getOutgoingView(node.getNodeId())) {
                if (edge == null) {
                    continue;
                }
                long edgeId = edge.getEdgeId();
                if (!wanted.contains(edgeId)) {
                    continue;
                }
                out.put(edgeId, edge);
                if (out.size() >= wanted.size()) {
                    return out;
                }
            }
        }
        return out;
    }

    private static void putNode(Map<Long, GraphFramePayload.Node> nodes,
                                GraphSnapshot snapshot,
                                long nodeId) {
        if (nodeId <= 0L || nodes.containsKey(nodeId)) {
            return;
        }
        GraphNode node = snapshot.getNode(nodeId);
        if (node == null) {
            nodes.put(nodeId, new GraphFramePayload.Node(nodeId, "node:" + nodeId, "", -1, "", "", ""));
            return;
        }
        nodes.put(nodeId,
                new GraphFramePayload.Node(
                        nodeId,
                        nodeLabel(node),
                        safe(node.getKind()),
                        node.getJarId(),
                        safe(node.getClassName()),
                        safe(node.getMethodName()),
                        safe(node.getMethodDesc())
                ));
    }

    private static GraphFramePayload.Edge toEdge(GraphEdge edge) {
        if (edge == null) {
            return new GraphFramePayload.Edge(0L, 0L, 0L, "", "", "");
        }
        return new GraphFramePayload.Edge(
                edge.getEdgeId(),
                edge.getSrcId(),
                edge.getDstId(),
                safe(edge.getRelType()),
                safe(edge.getConfidence()),
                safe(edge.getEvidence())
        );
    }

    private static String nodeLabel(GraphNode node) {
        if (node == null) {
            return "";
        }
        String className = safe(node.getClassName()).replace('/', '.');
        String methodName = safe(node.getMethodName());
        String methodDesc = safe(node.getMethodDesc());
        if (!className.isEmpty() && !methodName.isEmpty()) {
            String shortClass = shortName(className);
            String suffix = methodDesc.isEmpty() ? "" : methodDesc;
            return shortClass + "." + methodName + suffix;
        }
        if (!className.isEmpty()) {
            return className;
        }
        String kind = safe(node.getKind());
        if (!kind.isEmpty()) {
            return kind + ":" + node.getNodeId();
        }
        return "node:" + node.getNodeId();
    }

    private static String shortName(String className) {
        if (className == null || className.isBlank()) {
            return "";
        }
        int idx = className.lastIndexOf('.');
        if (idx < 0 || idx >= className.length() - 1) {
            return className;
        }
        return className.substring(idx + 1);
    }

    private static long syntheticEdgeId(int seq) {
        return -10_000_000L - Math.max(1, seq);
    }

    private static GraphFramePayload emptyGraph(List<String> warnings, boolean truncated) {
        return new GraphFramePayload(List.of(), List.of(), safeList(warnings), truncated);
    }

    private static LinkedHashSet<String> mergeWarnings(List<String> warnings) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (warnings != null) {
            for (String warning : warnings) {
                String safe = safe(warning);
                if (!safe.isEmpty()) {
                    out.add(safe);
                }
            }
        }
        return out;
    }

    private static Map<String, Integer> index(List<String> columns) {
        Map<String, Integer> out = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            String col = safe(columns.get(i)).toLowerCase(Locale.ROOT);
            if (!col.isEmpty()) {
                out.put(col, i);
            }
        }
        return out;
    }

    private static int first(Map<String, Integer> indexes, String... keys) {
        if (indexes == null || keys == null) {
            return -1;
        }
        for (String key : keys) {
            Integer idx = indexes.get(key);
            if (idx != null && idx >= 0) {
                return idx;
            }
        }
        return -1;
    }

    private static Object at(List<Object> row, int idx) {
        if (row == null || idx < 0 || idx >= row.size()) {
            return null;
        }
        return row.get(idx);
    }

    private static long parseLong(Object value) {
        if (value == null) {
            return -1L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = safeText(value);
        if (text.isEmpty()) {
            return -1L;
        }
        try {
            return Long.parseLong(text);
        } catch (Exception ignored) {
            return -1L;
        }
    }

    private static long safeLong(Long value) {
        return value == null ? -1L : value;
    }

    private static List<Long> parseLongList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            List<Long> out = new ArrayList<>(collection.size());
            for (Object item : collection) {
                long parsed = parseLong(item);
                if (parsed > 0L) {
                    out.add(parsed);
                }
            }
            return out;
        }
        if (value instanceof long[] longs) {
            List<Long> out = new ArrayList<>(longs.length);
            for (long item : longs) {
                if (item > 0L) {
                    out.add(item);
                }
            }
            return out;
        }
        if (value instanceof int[] ints) {
            List<Long> out = new ArrayList<>(ints.length);
            for (int item : ints) {
                if (item > 0) {
                    out.add((long) item);
                }
            }
            return out;
        }
        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            List<Long> out = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                Object item = Array.get(value, i);
                long parsed = parseLong(item);
                if (parsed > 0L) {
                    out.add(parsed);
                }
            }
            return out;
        }
        String text = safeText(value);
        if (text.isBlank()) {
            return List.of();
        }
        String normalized = text;
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            try {
                JSONArray arr = JSON.parseArray(normalized);
                List<Long> out = new ArrayList<>();
                for (Object item : arr) {
                    long parsed = parseLong(item);
                    if (parsed > 0L) {
                        out.add(parsed);
                    }
                }
                return out;
            } catch (Exception ignored) {
                // fallback below
            }
        }
        String[] parts = normalized.split(",");
        List<Long> out = new ArrayList<>(parts.length);
        for (String part : parts) {
            long parsed = parseLong(part.trim());
            if (parsed > 0L) {
                out.add(parsed);
            }
        }
        return out;
    }

    private static List<String> safeList(List<String> value) {
        if (value == null || value.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>(value.size());
        for (String item : value) {
            out.add(safe(item));
        }
        return out;
    }

    private static List<List<Object>> safeRows(List<List<Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<List<Object>> out = new ArrayList<>(rows.size());
        for (List<Object> row : rows) {
            if (row == null) {
                out.add(List.of());
            } else {
                out.add(List.copyOf(row));
            }
        }
        return out;
    }

    private static String renderText(String frameId,
                                     String query,
                                     List<String> columns,
                                     List<List<Object>> rows,
                                     List<String> warnings,
                                     boolean truncated,
                                     long elapsedMs,
                                     GraphFramePayload graph) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("frameId", safe(frameId));
        out.put("query", safe(query));
        out.put("elapsedMs", elapsedMs);
        out.put("truncated", truncated);
        out.put("columns", columns == null ? List.of() : columns);
        out.put("rows", rows == null ? List.of() : rows);
        out.put("warnings", warnings == null ? List.of() : warnings);
        if (graph != null) {
            out.put("graph", Map.of(
                    "nodes", graph.nodes() == null ? 0 : graph.nodes().size(),
                    "edges", graph.edges() == null ? 0 : graph.edges().size(),
                    "truncated", graph.truncated(),
                    "warnings", graph.warnings() == null ? List.of() : graph.warnings()
            ));
        }
        return JSON.toJSONString(out, JSONWriter.Feature.PrettyFormat);
    }

    private static String safeText(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record PathRow(List<Long> nodeIds, List<Long> edgeIds) {
        private PathRow {
            nodeIds = nodeIds == null ? List.of() : nodeIds;
            edgeIds = edgeIds == null ? List.of() : edgeIds;
        }
    }
}
