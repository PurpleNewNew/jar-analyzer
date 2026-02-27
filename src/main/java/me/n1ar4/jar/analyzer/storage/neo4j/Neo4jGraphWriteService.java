/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.storage.neo4j;

import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Neo4jGraphWriteService {
    private static final Logger logger = LogManager.getLogger();
    private static final Label NODE_LABEL = Label.label("JANode");
    private static final Label META_LABEL = Label.label("JAMeta");

    public void replaceFromSnapshot(String projectKey,
                                    GraphSnapshot snapshot,
                                    long buildSeq,
                                    boolean quickMode,
                                    String callGraphMode) {
        String normalized = ActiveProjectContext.normalizeProjectKey(projectKey);
        GraphDatabaseService database = Neo4jProjectStore.getInstance().database(normalized);
        if (database == null) {
            throw new IllegalStateException("neo4j_database_unavailable");
        }
        GraphSnapshot safeSnapshot = snapshot == null ? GraphSnapshot.empty() : snapshot;
        try (Transaction tx = database.beginTx()) {
            tx.execute("MATCH (n:JANode) DETACH DELETE n");
            tx.execute("MATCH (m:JAMeta {key:'build_meta'}) DETACH DELETE m");

            Map<Long, Node> nodeRefs = new HashMap<>();
            for (GraphNode node : safeSnapshot.getNodesView()) {
                if (node == null) {
                    continue;
                }
                Label kindLabel = Label.label(kindLabel(node.getKind()));
                Node neo = tx.createNode(NODE_LABEL, kindLabel);
                applySourceLabels(neo, node);
                neo.setProperty("node_id", node.getNodeId());
                neo.setProperty("kind", safe(node.getKind()));
                neo.setProperty("jar_id", node.getJarId());
                neo.setProperty("class_name", safe(node.getClassName()));
                neo.setProperty("method_name", safe(node.getMethodName()));
                neo.setProperty("method_desc", safe(node.getMethodDesc()));
                neo.setProperty("call_site_key", safe(node.getCallSiteKey()));
                neo.setProperty("line_number", node.getLineNumber());
                neo.setProperty("call_index", node.getCallIndex());
                neo.setProperty("source_flags", node.getSourceFlags());
                nodeRefs.put(node.getNodeId(), neo);
            }

            long edgeCount = 0L;
            for (GraphNode node : safeSnapshot.getNodesView()) {
                if (node == null) {
                    continue;
                }
                Node src = nodeRefs.get(node.getNodeId());
                if (src == null) {
                    continue;
                }
                for (GraphEdge edge : safeSnapshot.getOutgoingView(node.getNodeId())) {
                    Node dst = nodeRefs.get(edge.getDstId());
                    if (dst == null) {
                        continue;
                    }
                    String relType = sanitizeRelationshipType(edge.getRelType());
                    var rel = src.createRelationshipTo(dst, RelationshipType.withName(relType));
                    rel.setProperty("edge_id", edge.getEdgeId());
                    rel.setProperty("rel_type", safe(edge.getRelType()));
                    rel.setProperty("confidence", safe(edge.getConfidence()));
                    rel.setProperty("evidence", safe(edge.getEvidence()));
                    rel.setProperty("op_code", edge.getOpCode());
                    edgeCount++;
                }
            }

            Node meta = tx.createNode(META_LABEL);
            meta.setProperty("key", "build_meta");
            meta.setProperty("build_seq", buildSeq);
            meta.setProperty("quick_mode", quickMode);
            meta.setProperty("call_graph_mode", safe(callGraphMode));
            meta.setProperty("node_count", safeSnapshot.getNodeCount());
            meta.setProperty("edge_count", edgeCount);
            meta.setProperty("updated_at", System.currentTimeMillis());
            tx.commit();
            Neo4jGraphSnapshotLoader.invalidate(normalized);
            logger.info("neo4j graph replace finish: key={} buildSeq={} nodes={} edges={}",
                    normalized, buildSeq, safeSnapshot.getNodeCount(), edgeCount);
        } catch (Exception ex) {
            logger.warn("neo4j graph replace fail: key={} err={}", normalized, ex.toString());
            throw ex;
        }
    }

    private static String kindLabel(String kind) {
        String normalized = safe(kind).trim();
        if (normalized.isBlank()) {
            return "Unknown";
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if ("method".equals(lower)) {
            return "Method";
        }
        if ("callsite".equals(lower)) {
            return "CallSite";
        }
        StringBuilder out = new StringBuilder();
        boolean upper = true;
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                out.append(upper ? Character.toUpperCase(ch) : Character.toLowerCase(ch));
                upper = false;
            } else {
                upper = true;
            }
        }
        if (out.isEmpty()) {
            return "Unknown";
        }
        return out.toString();
    }

    private static String sanitizeRelationshipType(String type) {
        String value = safe(type).trim();
        if (value.isBlank()) {
            return "RELATED";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == '_') {
                out.append(Character.toUpperCase(ch));
            } else {
                out.append('_');
            }
        }
        String normalized = out.toString();
        if (normalized.isBlank()) {
            return "RELATED";
        }
        return normalized;
    }

    private static void applySourceLabels(Node neo, GraphNode node) {
        if (neo == null || node == null || !"method".equalsIgnoreCase(node.getKind())) {
            return;
        }
        int flags = node.getSourceFlags();
        if ((flags & GraphNode.SOURCE_FLAG_ANY) != 0) {
            neo.addLabel(Label.label("Source"));
        }
        if ((flags & GraphNode.SOURCE_FLAG_WEB) != 0) {
            neo.addLabel(Label.label("SourceWeb"));
        }
        if ((flags & GraphNode.SOURCE_FLAG_MODEL) != 0) {
            neo.addLabel(Label.label("SourceModel"));
        }
        if ((flags & GraphNode.SOURCE_FLAG_ANNOTATION) != 0) {
            neo.addLabel(Label.label("SourceAnno"));
        }
        if ((flags & GraphNode.SOURCE_FLAG_RPC) != 0) {
            neo.addLabel(Label.label("SourceRpc"));
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
