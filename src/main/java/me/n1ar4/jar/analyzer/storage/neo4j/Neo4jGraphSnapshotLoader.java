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
import me.n1ar4.jar.analyzer.graph.store.GraphTraversalRules;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Neo4jGraphSnapshotLoader {
    private static final Logger logger = LogManager.getLogger();
    private static final Label NODE_LABEL = Label.label("JANode");
    private static final Label META_LABEL = Label.label("JAMeta");
    private static final String META_KEY = "build_meta";
    private static final String METHOD_KIND = "method";
    private static final ProjectGraphStoreFacade PROJECT_STORE = ProjectGraphStoreFacade.getInstance();
    private static final long BUILD_SEQ_TIMEOUT_MS = 30_000L;
    private static final long SNAPSHOT_LOAD_TIMEOUT_MS = 120_000L;

    private static final Map<String, CachedSnapshot> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CachedSnapshot> FLOW_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CachedSnapshot> QUERY_CACHE = new ConcurrentHashMap<>();

    public GraphSnapshot load(String projectKey) {
        String key = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        long buildSeq = PROJECT_STORE.read(key, BUILD_SEQ_TIMEOUT_MS, Neo4jGraphSnapshotLoader::resolveBuildSeq);
        CachedSnapshot cached = CACHE.get(key);
        if (cached != null && cached.buildSeq == buildSeq && cached.snapshot != null) {
            return cached.snapshot;
        }
        LoadedSnapshot loaded = PROJECT_STORE.read(key, SNAPSHOT_LOAD_TIMEOUT_MS, Neo4jGraphSnapshotLoader::loadSnapshot);
        CACHE.put(key, new CachedSnapshot(loaded.buildSeq, loaded.snapshot));
        return loaded.snapshot;
    }

    public GraphSnapshot loadFlow(String projectKey) {
        String key = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        long buildSeq = PROJECT_STORE.read(key, BUILD_SEQ_TIMEOUT_MS, Neo4jGraphSnapshotLoader::resolveBuildSeq);
        CachedSnapshot cached = FLOW_CACHE.get(key);
        if (cached != null && cached.buildSeq == buildSeq && cached.snapshot != null) {
            return cached.snapshot;
        }
        LoadedSnapshot loaded = PROJECT_STORE.read(key, SNAPSHOT_LOAD_TIMEOUT_MS, Neo4jGraphSnapshotLoader::loadFlowSnapshot);
        FLOW_CACHE.put(key, new CachedSnapshot(loaded.buildSeq, loaded.snapshot));
        return loaded.snapshot;
    }

    public GraphSnapshot loadQuery(String projectKey) {
        String key = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        long buildSeq = PROJECT_STORE.read(key, BUILD_SEQ_TIMEOUT_MS, Neo4jGraphSnapshotLoader::resolveBuildSeq);
        CachedSnapshot cached = QUERY_CACHE.get(key);
        if (cached != null && cached.buildSeq == buildSeq && cached.snapshot != null) {
            return cached.snapshot;
        }
        LoadedSnapshot loaded = PROJECT_STORE.read(key, SNAPSHOT_LOAD_TIMEOUT_MS, Neo4jGraphSnapshotLoader::loadQuerySnapshot);
        QUERY_CACHE.put(key, new CachedSnapshot(loaded.buildSeq, loaded.snapshot));
        return loaded.snapshot;
    }

    public static void invalidate(String projectKey) {
        String normalized = ActiveProjectContext.normalizeProjectKey(projectKey);
        if (normalized.isBlank()) {
            CACHE.clear();
            FLOW_CACHE.clear();
            QUERY_CACHE.clear();
            return;
        }
        CACHE.remove(normalized);
        FLOW_CACHE.remove(normalized);
        QUERY_CACHE.remove(normalized);
    }

    public static void invalidateAll() {
        CACHE.clear();
        FLOW_CACHE.clear();
        QUERY_CACHE.clear();
    }

    private static LoadedSnapshot loadSnapshot(Transaction tx) {
        if (tx == null) {
            return new LoadedSnapshot(-1L, GraphSnapshot.empty());
        }
        Map<Long, GraphNode> nodeMap = new LinkedHashMap<>();
        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        Map<String, List<GraphNode>> labelIndex = new HashMap<>();

        try (ResourceIterator<Node> it = tx.findNodes(NODE_LABEL)) {
            long buildSeq = resolveBuildSeq(tx);
            while (it.hasNext()) {
                Node node = it.next();
                long nodeId = toLong(node.getProperty("node_id", -1L), -1L);
                if (nodeId <= 0L) {
                    continue;
                }
                GraphNode graphNode = new GraphNode(
                        nodeId,
                        toString(node.getProperty("kind", "")),
                        toInt(node.getProperty("jar_id", -1), -1),
                        toString(node.getProperty("class_name", "")),
                        toString(node.getProperty("method_name", "")),
                        toString(node.getProperty("method_desc", "")),
                        toString(node.getProperty("call_site_key", "")),
                        toInt(node.getProperty("line_number", -1), -1),
                        toInt(node.getProperty("call_index", -1), -1),
                        toInt(node.getProperty("source_flags", 0), 0),
                        toInt(node.getProperty("method_semantic_flags", 0), 0)
                );
                nodeMap.put(nodeId, graphNode);
                for (Label label : node.getLabels()) {
                    String labelName = safe(label.name());
                    if (labelName.isBlank() || "JANode".equalsIgnoreCase(labelName)) {
                        continue;
                    }
                    labelIndex.computeIfAbsent(labelName.toLowerCase(), k -> new ArrayList<>()).add(graphNode);
                }
            }
            for (Node node : tx.getAllNodes()) {
                if (!node.hasLabel(NODE_LABEL)) {
                    continue;
                }
                long srcId = toLong(node.getProperty("node_id", -1L), -1L);
                if (srcId <= 0L) {
                    continue;
                }
                for (Relationship relationship : node.getRelationships(Direction.OUTGOING)) {
                    Node endNode = relationship.getEndNode();
                    if (!endNode.hasLabel(NODE_LABEL)) {
                        continue;
                    }
                    long dstId = toLong(endNode.getProperty("node_id", -1L), -1L);
                    if (dstId <= 0L) {
                        continue;
                    }
                    GraphEdge edge = new GraphEdge(
                            toLong(relationship.getProperty("edge_id", relationship.getId()), relationship.getId()),
                            srcId,
                            dstId,
                            relationship.getType().name(),
                            toString(relationship.getProperty("confidence", "low")),
                            toString(relationship.getProperty("evidence", "")),
                            toString(relationship.getProperty("alias_kind", "")),
                            toInt(relationship.getProperty("op_code", -1), -1),
                            toString(relationship.getProperty("call_site_key", "")),
                            toInt(relationship.getProperty("line_number", -1), -1),
                            toInt(relationship.getProperty("call_index", -1), -1),
                            toInt(relationship.getProperty("edge_semantic_flags", 0), 0)
                    );
                    outgoing.computeIfAbsent(srcId, ignore -> new ArrayList<>()).add(edge);
                    incoming.computeIfAbsent(dstId, ignore -> new ArrayList<>()).add(edge);
                }
            }
            return new LoadedSnapshot(buildSeq, GraphSnapshot.of(buildSeq, nodeMap, outgoing, incoming, labelIndex));
        } catch (Exception ex) {
            logger.warn("load neo4j graph snapshot fail: {}", ex.toString());
            return new LoadedSnapshot(-1L, GraphSnapshot.empty());
        }
    }

    private static LoadedSnapshot loadFlowSnapshot(Transaction tx) {
        if (tx == null) {
            return new LoadedSnapshot(-1L, GraphSnapshot.empty());
        }
        Map<Long, GraphNode> nodeMap = new LinkedHashMap<>();
        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        Map<String, List<GraphNode>> labelIndex = new HashMap<>();

        try (ResourceIterator<Node> it = tx.findNodes(NODE_LABEL, "kind", METHOD_KIND)) {
            long buildSeq = resolveBuildSeq(tx);
            while (it.hasNext()) {
                Node node = it.next();
                long nodeId = toLong(node.getProperty("node_id", -1L), -1L);
                if (nodeId <= 0L) {
                    continue;
                }
                GraphNode graphNode = new GraphNode(
                        nodeId,
                        METHOD_KIND,
                        toInt(node.getProperty("jar_id", -1), -1),
                        toString(node.getProperty("class_name", "")),
                        toString(node.getProperty("method_name", "")),
                        toString(node.getProperty("method_desc", "")),
                        "",
                        -1,
                        -1,
                        toInt(node.getProperty("source_flags", 0), 0),
                        toInt(node.getProperty("method_semantic_flags", 0), 0)
                );
                nodeMap.put(nodeId, graphNode);
                labelIndex.computeIfAbsent("method", ignore -> new ArrayList<>()).add(graphNode);
            }
            for (Node node : tx.getAllNodes()) {
                if (!METHOD_KIND.equalsIgnoreCase(toString(node.getProperty("kind", "")))) {
                    continue;
                }
                long srcId = toLong(node.getProperty("node_id", -1L), -1L);
                if (srcId <= 0L || !nodeMap.containsKey(srcId)) {
                    continue;
                }
                for (Relationship relationship : node.getRelationships(Direction.OUTGOING)) {
                    String relType = relationship.getType().name();
                    if (!GraphTraversalRules.isFlowEdge(relType, true)) {
                        continue;
                    }
                    Node endNode = relationship.getEndNode();
                    long dstId = toLong(endNode.getProperty("node_id", -1L), -1L);
                    if (dstId <= 0L || !nodeMap.containsKey(dstId)) {
                        continue;
                    }
                    GraphEdge edge = new GraphEdge(
                            toLong(relationship.getProperty("edge_id", relationship.getId()), relationship.getId()),
                            srcId,
                            dstId,
                            relType,
                            toString(relationship.getProperty("confidence", "low")),
                            toString(relationship.getProperty("evidence", "")),
                            toString(relationship.getProperty("alias_kind", "")),
                            toInt(relationship.getProperty("op_code", -1), -1),
                            toString(relationship.getProperty("call_site_key", "")),
                            toInt(relationship.getProperty("line_number", -1), -1),
                            toInt(relationship.getProperty("call_index", -1), -1),
                            toInt(relationship.getProperty("edge_semantic_flags", 0), 0)
                    );
                    outgoing.computeIfAbsent(srcId, ignore -> new ArrayList<>()).add(edge);
                    incoming.computeIfAbsent(dstId, ignore -> new ArrayList<>()).add(edge);
                }
            }
            return new LoadedSnapshot(buildSeq, GraphSnapshot.of(buildSeq, nodeMap, outgoing, incoming, labelIndex));
        } catch (Exception ex) {
            logger.warn("load neo4j flow snapshot fail: {}", ex.toString());
            return new LoadedSnapshot(-1L, GraphSnapshot.empty());
        }
    }

    private static LoadedSnapshot loadQuerySnapshot(Transaction tx) {
        if (tx == null) {
            return new LoadedSnapshot(-1L, GraphSnapshot.empty());
        }
        Map<Long, GraphNode> nodeMap = new LinkedHashMap<>();
        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        Map<String, List<GraphNode>> labelIndex = new HashMap<>();

        try (ResourceIterator<Node> it = tx.findNodes(NODE_LABEL, "kind", METHOD_KIND)) {
            long buildSeq = resolveBuildSeq(tx);
            while (it.hasNext()) {
                Node node = it.next();
                long nodeId = toLong(node.getProperty("node_id", -1L), -1L);
                if (nodeId <= 0L) {
                    continue;
                }
                GraphNode graphNode = new GraphNode(
                        nodeId,
                        METHOD_KIND,
                        toInt(node.getProperty("jar_id", -1), -1),
                        toString(node.getProperty("class_name", "")),
                        toString(node.getProperty("method_name", "")),
                        toString(node.getProperty("method_desc", "")),
                        "",
                        -1,
                        -1,
                        toInt(node.getProperty("source_flags", 0), 0),
                        toInt(node.getProperty("method_semantic_flags", 0), 0)
                );
                nodeMap.put(nodeId, graphNode);
                labelIndex.computeIfAbsent(METHOD_KIND, ignore -> new ArrayList<>()).add(graphNode);
            }
            for (Node node : tx.getAllNodes()) {
                if (!METHOD_KIND.equalsIgnoreCase(toString(node.getProperty("kind", "")))) {
                    continue;
                }
                long srcId = toLong(node.getProperty("node_id", -1L), -1L);
                if (srcId <= 0L || !nodeMap.containsKey(srcId)) {
                    continue;
                }
                for (Relationship relationship : node.getRelationships(Direction.OUTGOING)) {
                    String relType = relationship.getType().name();
                    if (!GraphTraversalRules.isCallEdge(relType)) {
                        continue;
                    }
                    Node endNode = relationship.getEndNode();
                    long dstId = toLong(endNode.getProperty("node_id", -1L), -1L);
                    if (dstId <= 0L || !nodeMap.containsKey(dstId)) {
                        continue;
                    }
                    GraphEdge edge = new GraphEdge(
                            toLong(relationship.getProperty("edge_id", relationship.getId()), relationship.getId()),
                            srcId,
                            dstId,
                            relType,
                            toString(relationship.getProperty("confidence", "low")),
                            toString(relationship.getProperty("evidence", "")),
                            toString(relationship.getProperty("alias_kind", "")),
                            toInt(relationship.getProperty("op_code", -1), -1),
                            toString(relationship.getProperty("call_site_key", "")),
                            toInt(relationship.getProperty("line_number", -1), -1),
                            toInt(relationship.getProperty("call_index", -1), -1),
                            toInt(relationship.getProperty("edge_semantic_flags", 0), 0)
                    );
                    outgoing.computeIfAbsent(srcId, ignore -> new ArrayList<>()).add(edge);
                    incoming.computeIfAbsent(dstId, ignore -> new ArrayList<>()).add(edge);
                }
            }
            return new LoadedSnapshot(buildSeq, GraphSnapshot.of(buildSeq, nodeMap, outgoing, incoming, labelIndex));
        } catch (Exception ex) {
            logger.warn("load neo4j query snapshot fail: {}", ex.toString());
            return new LoadedSnapshot(-1L, GraphSnapshot.empty());
        }
    }

    private static long resolveBuildSeq(Transaction tx) {
        if (tx == null) {
            return -1L;
        }
        try (ResourceIterator<Node> it = tx.findNodes(META_LABEL, "key", META_KEY)) {
            if (it.hasNext()) {
                Node node = it.next();
                return toLong(node.getProperty("build_seq", -1L), -1L);
            }
        }
        return -1L;
    }

    private static String toString(Object value) {
        return value == null ? "" : String.valueOf(value);
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
        } catch (Exception ex) {
            return def;
        }
    }

    private static long toLong(Object value, long def) {
        if (value == null) {
            return def;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ex) {
            return def;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record CachedSnapshot(long buildSeq, GraphSnapshot snapshot) {
    }

    private record LoadedSnapshot(long buildSeq, GraphSnapshot snapshot) {
    }
}
