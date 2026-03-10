/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.cypher;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONWriter;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.graph.model.GraphRelationType;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.GraphFramePayload;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ProjectGraphRequest;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.QueryFramePayload;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.rules.SourceRuleSupport;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
        return projectGenericRows(rows, snapshot, request.warnings(), request.truncated());
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
                        new GraphFramePayload.Edge(
                                edgeId,
                                srcId,
                                dstId,
                                relType,
                                confidence,
                                evidence,
                                edgeProperties(relType, confidence, evidence, null)
                        ));
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
                        new GraphFramePayload.Edge(
                                syntheticId,
                                srcId,
                                dstId,
                                relType,
                                confidence,
                                evidence,
                                edgeProperties(relType, confidence, evidence, null)
                        ));
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
                if (!canAddNode(nodes, nodeId)) {
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
                    if (!canAddNode(nodes, edge.getSrcId()) || !canAddNode(nodes, edge.getDstId())) {
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
                    if (!canAddNode(nodes, srcId) || !canAddNode(nodes, dstId)) {
                        budgetTruncated = true;
                        continue;
                    }
                    putNode(nodes, snapshot, srcId);
                    putNode(nodes, snapshot, dstId);
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
                        new GraphFramePayload.Edge(
                                syntheticId,
                                srcId,
                                dstId,
                                "PATH",
                                "",
                                "derived_path",
                                edgeProperties("PATH", "", "derived_path", null)
                        ));
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

    private GraphFramePayload projectGenericRows(List<List<Object>> rows,
                                                 GraphSnapshot snapshot,
                                                 List<String> requestWarnings,
                                                 boolean requestTruncated) {
        GenericGraphAccumulator accumulator = new GenericGraphAccumulator();
        for (List<Object> row : rows) {
            if (row == null) {
                continue;
            }
            for (Object cell : row) {
                collectGenericGraphItems(cell, accumulator);
            }
        }
        if (accumulator.nodeMaps.isEmpty() && accumulator.relationshipMaps.isEmpty()
                && accumulator.syntheticRelationshipMaps.isEmpty()) {
            return emptyGraph(requestWarnings, requestTruncated);
        }

        LinkedHashMap<Long, GraphFramePayload.Node> nodes = new LinkedHashMap<>();
        LinkedHashMap<Long, GraphFramePayload.Edge> edges = new LinkedHashMap<>();
        LinkedHashSet<String> warnings = mergeWarnings(requestWarnings);
        boolean budgetTruncated = false;

        for (Map.Entry<Long, Map<String, Object>> entry : accumulator.nodeMaps.entrySet()) {
            long nodeId = entry.getKey();
            if (!canAddNode(nodes, nodeId)) {
                budgetTruncated = true;
                continue;
            }
            putNode(nodes, snapshot, nodeId, entry.getValue());
        }

        for (Map<String, Object> relMap : accumulator.relationshipMaps.values()) {
            if (!projectGenericRelationship(relMap, accumulator.nodeMaps, nodes, edges, snapshot)) {
                budgetTruncated = true;
            }
        }
        for (Map<String, Object> relMap : accumulator.syntheticRelationshipMaps.values()) {
            if (!projectGenericRelationship(relMap, accumulator.nodeMaps, nodes, edges, snapshot)) {
                budgetTruncated = true;
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
        putNode(nodes, snapshot, nodeId, null);
    }

    private static void putNode(Map<Long, GraphFramePayload.Node> nodes,
                                GraphSnapshot snapshot,
                                long nodeId,
                                Map<String, Object> fallback) {
        if (nodeId <= 0L || nodes.containsKey(nodeId)) {
            return;
        }
        GraphNode node = snapshot == null ? null : snapshot.getNode(nodeId);
        if (node == null) {
            nodes.put(nodeId, fallbackNode(nodeId, fallback));
            return;
        }
        Map<String, Object> properties = mergeProperties(nodeProperties(node), propertyMap(fallback));
        List<String> labels = mergeLabels(defaultLabels(node), sanitizeDisplayLabels(stringList(fallback == null ? null : fallback.get("labels"))));
        nodes.put(nodeId,
                new GraphFramePayload.Node(
                        nodeId,
                        nodeLabel(node),
                        safe(node.getKind()),
                        node.getJarId(),
                        safe(node.getClassName()),
                        safe(node.getMethodName()),
                        safe(node.getMethodDesc()),
                        labels,
                        properties
                ));
    }

    private boolean projectGenericRelationship(Map<String, Object> relMap,
                                               Map<Long, Map<String, Object>> nodeMaps,
                                               Map<Long, GraphFramePayload.Node> nodes,
                                               Map<Long, GraphFramePayload.Edge> edges,
                                               GraphSnapshot snapshot) {
        long srcId = relationshipNodeId(relMap, "startNodeId", "start_node_id", "source", "src_id");
        long dstId = relationshipNodeId(relMap, "endNodeId", "end_node_id", "target", "dst_id");
        if (srcId <= 0L || dstId <= 0L) {
            return true;
        }
        if (!canAddNode(nodes, srcId) || !canAddNode(nodes, dstId)) {
            return false;
        }
        putNode(nodes, snapshot, srcId, nodeMaps.get(srcId));
        putNode(nodes, snapshot, dstId, nodeMaps.get(dstId));

        long edgeId = relationshipId(relMap);
        if (edgeId > 0L) {
            if (edges.size() >= graphEdgeBudget && !edges.containsKey(edgeId)) {
                return false;
            }
            edges.putIfAbsent(edgeId, genericEdge(relMap, edgeId));
            return true;
        }
        if (edges.size() >= graphEdgeBudget) {
            return false;
        }
        long syntheticId = syntheticEdgeId(edges.size() + 1);
        edges.put(syntheticId, genericEdge(relMap, syntheticId));
        return true;
    }

    private static GraphFramePayload.Edge toEdge(GraphEdge edge) {
        if (edge == null) {
            return new GraphFramePayload.Edge(0L, 0L, 0L, "", "", "", Map.of());
        }
        return new GraphFramePayload.Edge(
                edge.getEdgeId(),
                edge.getSrcId(),
                edge.getDstId(),
                safe(edge.getRelType()),
                safe(edge.getConfidence()),
                safe(edge.getEvidence()),
                edgeProperties(edge.getRelType(), edge.getConfidence(), edge.getEvidence(), edge)
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

    private static GraphFramePayload.Node fallbackNode(long nodeId, Map<String, Object> fallback) {
        Map<String, Object> properties = propertyMap(fallback);
        String kind = firstText(fallback, properties, "kind");
        int jarId = parseInt(firstValue(fallback, properties, "jar_id", "jarId"));
        String className = normalizeClassName(firstText(fallback, properties, "class_name", "className"));
        String methodName = firstText(fallback, properties, "method_name", "methodName");
        String methodDesc = firstText(fallback, properties, "method_desc", "methodDesc");
        int sourceFlags = parseInt(firstValue(fallback, properties, "source_flags", "sourceFlags"));
        return new GraphFramePayload.Node(
                nodeId,
                fallbackNodeLabel(nodeId, fallback, className, methodName, methodDesc, kind),
                kind,
                jarId,
                className,
                methodName,
                methodDesc,
                sanitizeDisplayLabels(stringList(fallback == null ? null : fallback.get("labels"))),
                mergeProperties(nodeMetadataProperties(nodeId, kind, jarId, className, methodName, methodDesc, "", -1, -1, sourceFlags), properties)
        );
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

    private static GraphFramePayload.Edge genericEdge(Map<String, Object> relMap, long edgeId) {
        Map<String, Object> properties = propertyMap(relMap);
        String relType = firstText(relMap, properties, "type", "rel_type");
        String confidence = firstText(relMap, properties, "confidence");
        String evidence = firstText(relMap, properties, "evidence");
        return new GraphFramePayload.Edge(
                edgeId,
                relationshipNodeId(relMap, "startNodeId", "start_node_id", "source", "src_id"),
                relationshipNodeId(relMap, "endNodeId", "end_node_id", "target", "dst_id"),
                relType,
                confidence,
                evidence,
                edgeProperties(relType, confidence, evidence, null, properties)
        );
    }

    private static String fallbackNodeLabel(long nodeId,
                                            Map<String, Object> fallback,
                                            String className,
                                            String methodName,
                                            String methodDesc,
                                            String kind) {
        if (!className.isEmpty() && !methodName.isEmpty()) {
            return shortName(className) + "." + methodName + methodDesc;
        }
        if (!className.isEmpty()) {
            return className;
        }
        List<String> labels = stringList(fallback == null ? null : fallback.get("labels"));
        labels = sanitizeDisplayLabels(labels);
        if (!labels.isEmpty()) {
            return String.join(":", labels) + ":" + nodeId;
        }
        if (!kind.isEmpty()) {
            return kind + ":" + nodeId;
        }
        return "node:" + nodeId;
    }

    private static List<String> defaultLabels(GraphNode node) {
        if (node == null) {
            return List.of();
        }
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add("JANode");
        String kind = safe(node.getKind());
        if (!kind.isEmpty()) {
            labels.add(displayKindLabel(kind));
        }
        return new ArrayList<>(labels);
    }

    private static Map<String, Object> nodeProperties(GraphNode node) {
        if (node == null) {
            return Map.of();
        }
        return nodeMetadataProperties(
                node.getNodeId(),
                node.getKind(),
                node.getJarId(),
                normalizeClassName(node.getClassName()),
                node.getMethodName(),
                node.getMethodDesc(),
                node.getCallSiteKey(),
                node.getLineNumber(),
                node.getCallIndex(),
                node.getSourceFlags()
        );
    }

    private static Map<String, Object> nodeMetadataProperties(long nodeId,
                                                              String kind,
                                                              int jarId,
                                                              String className,
                                                              String methodName,
                                                              String methodDesc,
                                                              String callSiteKey,
                                                              int lineNumber,
                                                              int callIndex,
                                                              int sourceFlags) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        putIfPresent(out, "node_id", nodeId > 0L ? nodeId : null);
        putIfPresent(out, "kind", kind);
        putIfPresent(out, "jar_id", jarId >= 0 ? jarId : null);
        putIfPresent(out, "class_name", normalizeClassName(className));
        putIfPresent(out, "method_name", methodName);
        putIfPresent(out, "method_desc", methodDesc);
        putIfPresent(out, "call_site_key", callSiteKey);
        putIfPresent(out, "line_number", lineNumber >= 0 ? lineNumber : null);
        putIfPresent(out, "call_index", callIndex >= 0 ? callIndex : null);
        putIfPresent(out, "source_flags", sourceFlags > 0 ? sourceFlags : null);
        Map<String, Object> semantic = nodeSemanticProperties(kind, jarId, className, methodName, methodDesc);
        out.putAll(semantic);
        return out;
    }

    private static Map<String, Object> edgeProperties(String relType,
                                                      String confidence,
                                                      String evidence,
                                                      GraphEdge edge) {
        return edgeProperties(relType, confidence, evidence, edge, Map.of());
    }

    private static Map<String, Object> edgeProperties(String relType,
                                                      String confidence,
                                                      String evidence,
                                                      GraphEdge edge,
                                                      Map<String, Object> properties) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        putIfPresent(out, "rel_type", relType);
        putIfPresent(out, "display_rel_type", GraphRelationType.relationGroup(relType));
        putIfPresent(out, "rel_subtype", GraphRelationType.relationSubtype(relType));
        putIfPresent(out, "confidence", confidence);
        putIfPresent(out, "evidence", evidence);
        if (edge != null) {
            putIfPresent(out, "op_code", edge.getOpCode() >= 0 ? edge.getOpCode() : null);
            putIfPresent(out, "call_site_key", edge.getCallSiteKey());
            putIfPresent(out, "line_number", edge.getLineNumber() >= 0 ? edge.getLineNumber() : null);
            putIfPresent(out, "call_index", edge.getCallIndex() >= 0 ? edge.getCallIndex() : null);
        }
        return mergeProperties(out, properties);
    }

    private static Map<String, Object> nodeSemanticProperties(String kind,
                                                              int jarId,
                                                              String className,
                                                              String methodName,
                                                              String methodDesc) {
        if (!"method".equalsIgnoreCase(safe(kind))) {
            return Map.of();
        }
        int effectiveSourceFlags = SourceRuleSupport.resolveCurrentSourceFlags(
                className,
                methodName,
                methodDesc,
                jarId
        );
        List<String> sourceBadges = SourceRuleSupport.describeFlags(effectiveSourceFlags);
        String sinkKind = resolveSinkKind(jarId, className, methodName, methodDesc);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        putIfPresent(out, "source_flags_effective", effectiveSourceFlags > 0 ? effectiveSourceFlags : null);
        putIfPresent(out, "source_badges", sourceBadges.isEmpty() ? null : sourceBadges);
        out.put("is_source", effectiveSourceFlags != 0);
        out.put("is_sink", !sinkKind.isBlank());
        putIfPresent(out, "sink_kind", sinkKind);
        return out;
    }

    private static String resolveSinkKind(int jarId,
                                          String className,
                                          String methodName,
                                          String methodDesc) {
        String owner = normalizeClassName(className);
        String name = safe(methodName);
        String desc = safe(methodDesc);
        if (owner.isBlank() || name.isBlank() || desc.isBlank()) {
            return "";
        }
        MethodReference.Handle handle = new MethodReference.Handle(
                new ClassReference.Handle(owner, jarId),
                name,
                desc
        );
        String sinkKind = ModelRegistry.resolveSinkKind(handle);
        return sinkKind == null ? "" : sinkKind;
    }

    private static List<String> sanitizeDisplayLabels(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String raw : labels) {
            String label = safe(raw);
            if (label.isEmpty() || isSourceSemanticLabel(label)) {
                continue;
            }
            if ("method".equalsIgnoreCase(label)) {
                out.add("Method");
                continue;
            }
            out.add(label);
        }
        return new ArrayList<>(out);
    }

    private static boolean isSourceSemanticLabel(String label) {
        String normalized = safe(label);
        return "Source".equalsIgnoreCase(normalized)
                || "SourceWeb".equalsIgnoreCase(normalized)
                || "SourceModel".equalsIgnoreCase(normalized)
                || "SourceAnno".equalsIgnoreCase(normalized)
                || "SourceRpc".equalsIgnoreCase(normalized);
    }

    private static String displayKindLabel(String kind) {
        String normalized = safe(kind);
        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.length() == 1) {
            return normalized.toUpperCase(Locale.ROOT);
        }
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }

    private static void putIfPresent(Map<String, Object> out, String key, Object value) {
        if (out == null || key == null || key.isBlank() || value == null) {
            return;
        }
        if (value instanceof String text && text.isBlank()) {
            return;
        }
        out.put(key, value);
    }

    private static Map<String, Object> mergeProperties(Map<String, Object> primary, Map<String, Object> secondary) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (primary != null) {
            for (Map.Entry<String, Object> entry : primary.entrySet()) {
                putIfPresent(out, entry.getKey(), entry.getValue());
            }
        }
        if (secondary != null) {
            for (Map.Entry<String, Object> entry : secondary.entrySet()) {
                putIfPresent(out, entry.getKey(), entry.getValue());
            }
        }
        return out;
    }

    private static List<String> mergeLabels(List<String> primary, List<String> secondary) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (primary != null) {
            out.addAll(primary);
        }
        if (secondary != null) {
            out.addAll(secondary);
        }
        return new ArrayList<>(out);
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

    private static void collectGenericGraphItems(Object value, GenericGraphAccumulator accumulator) {
        if (value == null || accumulator == null) {
            return;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> typed = normalizeMap(map);
            if (isNodeMap(typed)) {
                long nodeId = nodeIdentity(typed);
                if (nodeId > 0L) {
                    accumulator.nodeMaps.putIfAbsent(nodeId, typed);
                }
                return;
            }
            if (isRelationshipMap(typed)) {
                long edgeId = relationshipId(typed);
                if (edgeId > 0L) {
                    accumulator.relationshipMaps.putIfAbsent(edgeId, typed);
                } else {
                    accumulator.syntheticRelationshipMaps.putIfAbsent(relationshipKey(typed), typed);
                }
                return;
            }
            if (isPathMap(typed)) {
                collectGenericGraphItems(typed.get("nodes"), accumulator);
                collectGenericGraphItems(typed.get("relationships"), accumulator);
                return;
            }
            for (Object nested : typed.values()) {
                collectGenericGraphItems(nested, accumulator);
            }
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                collectGenericGraphItems(item, accumulator);
            }
            return;
        }
        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            for (int i = 0; i < len; i++) {
                collectGenericGraphItems(Array.get(value, i), accumulator);
            }
        }
    }

    private static boolean isNodeMap(Map<String, Object> map) {
        return map != null
                && map.containsKey("labels")
                && map.containsKey("properties")
                && nodeIdentity(map) > 0L;
    }

    private static boolean isRelationshipMap(Map<String, Object> map) {
        return map != null
                && map.containsKey("type")
                && map.containsKey("properties")
                && relationshipNodeId(map, "startNodeId", "start_node_id", "source", "src_id") > 0L
                && relationshipNodeId(map, "endNodeId", "end_node_id", "target", "dst_id") > 0L;
    }

    private static boolean isPathMap(Map<String, Object> map) {
        return map != null
                && map.containsKey("nodes")
                && map.containsKey("relationships");
    }

    private static long nodeIdentity(Map<String, Object> map) {
        return firstPositive(
                parseLong(map == null ? null : map.get("node_id")),
                parseLong(map == null ? null : map.get("nodeId")),
                parseLong(map == null ? null : map.get("id"))
        );
    }

    private static long relationshipId(Map<String, Object> map) {
        return firstPositive(
                parseLong(map == null ? null : map.get("edge_id")),
                parseLong(map == null ? null : map.get("edgeId")),
                parseLong(map == null ? null : map.get("id"))
        );
    }

    private static long relationshipNodeId(Map<String, Object> map, String... keys) {
        if (map == null || keys == null) {
            return -1L;
        }
        for (String key : keys) {
            long parsed = parseLong(map.get(key));
            if (parsed > 0L) {
                return parsed;
            }
        }
        return -1L;
    }

    private static long firstPositive(long... values) {
        if (values == null) {
            return -1L;
        }
        for (long value : values) {
            if (value > 0L) {
                return value;
            }
        }
        return -1L;
    }

    private static int parseInt(Object value) {
        long parsed = parseLong(value);
        return parsed <= 0L ? -1 : (int) Math.min(Integer.MAX_VALUE, parsed);
    }

    private static Map<String, Object> normalizeMap(Map<?, ?> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            out.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return out;
    }

    private static Map<String, Object> propertyMap(Map<String, Object> map) {
        if (map == null) {
            return Map.of();
        }
        Object value = map.get("properties");
        if (value instanceof Map<?, ?> raw) {
            return normalizeMap(raw);
        }
        return Map.of();
    }

    private static Object firstValue(Map<String, Object> primary,
                                     Map<String, Object> secondary,
                                     String... keys) {
        if (keys == null) {
            return null;
        }
        for (String key : keys) {
            if (primary != null && primary.containsKey(key)) {
                return primary.get(key);
            }
            if (secondary != null && secondary.containsKey(key)) {
                return secondary.get(key);
            }
        }
        return null;
    }

    private static String firstText(Map<String, Object> primary,
                                    Map<String, Object> secondary,
                                    String... keys) {
        Object value = firstValue(primary, secondary, keys);
        return safeText(value);
    }

    private static List<String> stringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            List<String> out = new ArrayList<>(collection.size());
            for (Object item : collection) {
                String text = safeText(item);
                if (!text.isEmpty()) {
                    out.add(text);
                }
            }
            return out;
        }
        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            List<String> out = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                String text = safeText(Array.get(value, i));
                if (!text.isEmpty()) {
                    out.add(text);
                }
            }
            return out;
        }
        String text = safeText(value);
        return text.isEmpty() ? List.of() : List.of(text);
    }

    private static String normalizeClassName(String className) {
        return className == null ? "" : className.replace('/', '.').trim();
    }

    private static String relationshipKey(Map<String, Object> map) {
        return relationshipNodeId(map, "startNodeId", "start_node_id", "source", "src_id")
                + "->"
                + relationshipNodeId(map, "endNodeId", "end_node_id", "target", "dst_id")
                + "#"
                + firstText(map, propertyMap(map), "type", "rel_type")
                + "#"
                + firstText(map, propertyMap(map), "confidence")
                + "#"
                + firstText(map, propertyMap(map), "evidence");
    }

    private boolean canAddNode(Map<Long, GraphFramePayload.Node> nodes, long nodeId) {
        if (nodeId <= 0L) {
            return false;
        }
        return nodes.containsKey(nodeId) || nodes.size() < graphNodeBudget;
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
                List<Object> copied = new ArrayList<>(row.size());
                copied.addAll(row);
                out.add(Collections.unmodifiableList(copied));
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

    private static final class GenericGraphAccumulator {
        private final LinkedHashMap<Long, Map<String, Object>> nodeMaps = new LinkedHashMap<>();
        private final LinkedHashMap<Long, Map<String, Object>> relationshipMaps = new LinkedHashMap<>();
        private final LinkedHashMap<String, Map<String, Object>> syntheticRelationshipMaps = new LinkedHashMap<>();
    }
}
