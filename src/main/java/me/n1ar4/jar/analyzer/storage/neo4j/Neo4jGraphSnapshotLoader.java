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
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
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

    private static final Map<String, CachedSnapshot> CACHE = new ConcurrentHashMap<>();

    public GraphSnapshot load(String projectKey) {
        String key = ActiveProjectContext.normalizeProjectKey(projectKey);
        GraphDatabaseService database = Neo4jProjectStore.getInstance().database(key);
        long buildSeq = resolveBuildSeq(database);
        CachedSnapshot cached = CACHE.get(key);
        if (cached != null && cached.buildSeq == buildSeq && cached.snapshot != null) {
            return cached.snapshot;
        }
        GraphSnapshot loaded = loadSnapshot(database, buildSeq);
        CACHE.put(key, new CachedSnapshot(buildSeq, loaded));
        return loaded;
    }

    public static void invalidate(String projectKey) {
        if (projectKey == null || projectKey.isBlank()) {
            CACHE.clear();
            return;
        }
        CACHE.remove(ActiveProjectContext.normalizeProjectKey(projectKey));
    }

    public static void invalidateAll() {
        CACHE.clear();
    }

    private static GraphSnapshot loadSnapshot(GraphDatabaseService database, long buildSeq) {
        if (database == null) {
            return GraphSnapshot.empty();
        }
        Map<Long, GraphNode> nodeMap = new LinkedHashMap<>();
        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        Map<String, List<GraphNode>> labelIndex = new HashMap<>();

        try (Transaction tx = database.beginTx();
             ResourceIterator<Node> it = tx.findNodes(NODE_LABEL)) {
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
                        toInt(node.getProperty("call_index", -1), -1)
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
                            toInt(relationship.getProperty("op_code", -1), -1)
                    );
                    outgoing.computeIfAbsent(srcId, ignore -> new ArrayList<>()).add(edge);
                    incoming.computeIfAbsent(dstId, ignore -> new ArrayList<>()).add(edge);
                }
            }
            tx.commit();
            return GraphSnapshot.of(buildSeq, nodeMap, outgoing, incoming, labelIndex);
        } catch (Exception ex) {
            logger.warn("load neo4j graph snapshot fail: {}", ex.toString());
            return GraphSnapshot.empty();
        }
    }

    private static long resolveBuildSeq(GraphDatabaseService database) {
        if (database == null) {
            return -1L;
        }
        try (Transaction tx = database.beginTx()) {
            try (ResourceIterator<Node> it = tx.findNodes(META_LABEL, "key", META_KEY)) {
                if (it.hasNext()) {
                    Node node = it.next();
                    long seq = toLong(node.getProperty("build_seq", -1L), -1L);
                    tx.commit();
                    return seq;
                }
            }
            tx.commit();
            return -1L;
        } catch (Exception ex) {
            logger.debug("resolve neo4j build seq fail: {}", ex.toString());
            return -1L;
        }
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
}
