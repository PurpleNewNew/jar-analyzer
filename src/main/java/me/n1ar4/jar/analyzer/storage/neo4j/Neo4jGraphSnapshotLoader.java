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

import me.n1ar4.jar.analyzer.core.DatabaseManager;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Predicate;

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
    private static final Map<String, FutureTask<CachedSnapshot>> CACHE_LOADS = new ConcurrentHashMap<>();
    private static final Map<String, FutureTask<CachedSnapshot>> FLOW_CACHE_LOADS = new ConcurrentHashMap<>();
    private static final Map<String, FutureTask<CachedSnapshot>> QUERY_CACHE_LOADS = new ConcurrentHashMap<>();

    public GraphSnapshot load(String projectKey) {
        return loadSnapshotCached(projectKey, CACHE, CACHE_LOADS, Neo4jGraphSnapshotLoader::loadSnapshot);
    }

    public GraphSnapshot loadFlow(String projectKey) {
        return loadSnapshotCached(projectKey, FLOW_CACHE, FLOW_CACHE_LOADS, Neo4jGraphSnapshotLoader::loadFlowSnapshot);
    }

    public GraphSnapshot loadQuery(String projectKey) {
        return loadSnapshotCached(projectKey, QUERY_CACHE, QUERY_CACHE_LOADS, Neo4jGraphSnapshotLoader::loadQuerySnapshot);
    }

    public static void invalidate(String projectKey) {
        String normalized = ActiveProjectContext.normalizeProjectKey(projectKey);
        if (normalized.isBlank()) {
            CACHE.clear();
            FLOW_CACHE.clear();
            QUERY_CACHE.clear();
            CACHE_LOADS.clear();
            FLOW_CACHE_LOADS.clear();
            QUERY_CACHE_LOADS.clear();
            return;
        }
        CACHE.remove(normalized);
        FLOW_CACHE.remove(normalized);
        QUERY_CACHE.remove(normalized);
        CACHE_LOADS.remove(normalized);
        FLOW_CACHE_LOADS.remove(normalized);
        QUERY_CACHE_LOADS.remove(normalized);
    }

    public static void invalidateAll() {
        CACHE.clear();
        FLOW_CACHE.clear();
        QUERY_CACHE.clear();
        CACHE_LOADS.clear();
        FLOW_CACHE_LOADS.clear();
        QUERY_CACHE_LOADS.clear();
    }

    private GraphSnapshot loadSnapshotCached(String projectKey,
                                             Map<String, CachedSnapshot> cache,
                                             Map<String, FutureTask<CachedSnapshot>> loads,
                                             java.util.function.Function<Transaction, LoadedSnapshot> loader) {
        String key = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        long expectedBuildSeq = resolveExpectedBuildSeq(key);
        CachedSnapshot cached = cache.get(key);
        if (isUsable(cached, expectedBuildSeq)) {
            return cached.snapshot;
        }
        while (true) {
            FutureTask<CachedSnapshot> task = new FutureTask<>(() -> {
                LoadedSnapshot loaded = PROJECT_STORE.read(key, SNAPSHOT_LOAD_TIMEOUT_MS, loader);
                return new CachedSnapshot(loaded.buildSeq, loaded.snapshot);
            });
            FutureTask<CachedSnapshot> active = loads.putIfAbsent(key, task);
            if (active == null) {
                try {
                    task.run();
                    CachedSnapshot loaded = task.get();
                    long currentBuildSeq = resolveExpectedBuildSeq(key);
                    if (isUsable(loaded, currentBuildSeq)) {
                        cache.put(key, loaded);
                        return loaded.snapshot;
                    }
                    CachedSnapshot refreshed = cache.get(key);
                    if (isUsable(refreshed, currentBuildSeq)) {
                        return refreshed.snapshot;
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("graph_snapshot_load_interrupted", ex);
                } catch (ExecutionException ex) {
                    throw rethrow(ex.getCause());
                } finally {
                    loads.remove(key, task);
                }
            }
            try {
                CachedSnapshot loaded = active.get();
                long currentBuildSeq = resolveExpectedBuildSeq(key);
                if (isUsable(loaded, currentBuildSeq)) {
                    cache.put(key, loaded);
                    return loaded.snapshot;
                }
                CachedSnapshot refreshed = cache.get(key);
                if (isUsable(refreshed, currentBuildSeq)) {
                    return refreshed.snapshot;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("graph_snapshot_load_interrupted", ex);
            } catch (ExecutionException ex) {
                throw rethrow(ex.getCause());
            }
        }
    }

    private static long resolveExpectedBuildSeq(String projectKey) {
        String key = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        if (key.isBlank()) {
            return -1L;
        }
        long buildSeq = DatabaseManager.getProjectBuildSeq(key);
        if (buildSeq > 0L) {
            return buildSeq;
        }
        return PROJECT_STORE.read(key, BUILD_SEQ_TIMEOUT_MS, Neo4jGraphSnapshotLoader::resolveBuildSeq);
    }

    private static boolean isUsable(CachedSnapshot cached, long buildSeq) {
        return cached != null
                && cached.snapshot != null
                && buildSeq > 0L
                && cached.buildSeq == buildSeq;
    }

    private static IllegalStateException rethrow(Throwable cause) {
        if (cause instanceof IllegalStateException state) {
            return state;
        }
        if (cause instanceof RuntimeException runtime) {
            return new IllegalStateException(runtime.getMessage(), runtime);
        }
        return new IllegalStateException("graph_snapshot_load_failed", cause);
    }

    private static LoadedSnapshot loadSnapshot(Transaction tx) {
        if (tx == null) {
            return new LoadedSnapshot(-1L, GraphSnapshot.empty());
        }
        try (ResourceIterator<Node> it = tx.findNodes(NODE_LABEL)) {
            long buildSeq = resolveBuildSeq(tx);
            CompressedGraphBuilder builder = new CompressedGraphBuilder(buildSeq);
            List<NodeRef> sourceNodes = new ArrayList<>();
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
                builder.addNode(graphNode);
                sourceNodes.add(new NodeRef(node, nodeId));
                for (Label label : node.getLabels()) {
                    String labelName = safe(label.name());
                    if (labelName.isBlank() || "JANode".equalsIgnoreCase(labelName)) {
                        continue;
                    }
                    builder.addLabel(labelName.toLowerCase(), graphNode);
                }
            }
            for (NodeRef sourceNode : sourceNodes) {
                appendOutgoingEdges(builder, sourceNode.node(), sourceNode.nodeId(), relType -> true, true);
            }
            return builder.build();
        } catch (Exception ex) {
            logger.warn("load neo4j graph snapshot fail: {}", ex.toString());
            return new LoadedSnapshot(-1L, GraphSnapshot.empty());
        }
    }

    private static LoadedSnapshot loadFlowSnapshot(Transaction tx) {
        return loadMethodSnapshot(tx, relType -> GraphTraversalRules.isFlowEdge(relType, true), "flow");
    }

    private static LoadedSnapshot loadQuerySnapshot(Transaction tx) {
        return loadMethodSnapshot(tx, GraphTraversalRules::isCallEdge, "query");
    }

    private static LoadedSnapshot loadMethodSnapshot(Transaction tx,
                                                    Predicate<String> relFilter,
                                                    String mode) {
        if (tx == null) {
            return new LoadedSnapshot(-1L, GraphSnapshot.empty());
        }
        try (ResourceIterator<Node> it = tx.findNodes(NODE_LABEL, "kind", METHOD_KIND)) {
            long buildSeq = resolveBuildSeq(tx);
            CompressedGraphBuilder builder = new CompressedGraphBuilder(buildSeq);
            List<NodeRef> sourceNodes = new ArrayList<>();
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
                builder.addNode(graphNode);
                builder.addLabel(METHOD_KIND, graphNode);
                sourceNodes.add(new NodeRef(node, nodeId));
            }
            for (NodeRef sourceNode : sourceNodes) {
                appendOutgoingEdges(builder, sourceNode.node(), sourceNode.nodeId(), relFilter, false);
            }
            return builder.build();
        } catch (Exception ex) {
            logger.warn("load neo4j {} snapshot fail: {}", mode, ex.toString());
            return new LoadedSnapshot(-1L, GraphSnapshot.empty());
        }
    }

    private static void appendOutgoingEdges(CompressedGraphBuilder builder,
                                            Node sourceNode,
                                            long srcId,
                                            Predicate<String> relFilter,
                                            boolean requireNodeLabel) {
        if (builder == null || sourceNode == null || srcId <= 0L) {
            return;
        }
        for (Relationship relationship : sourceNode.getRelationships(Direction.OUTGOING)) {
            String relType = relationship.getType().name();
            if (relFilter != null && !relFilter.test(relType)) {
                continue;
            }
            Node endNode = relationship.getEndNode();
            if (requireNodeLabel && !endNode.hasLabel(NODE_LABEL)) {
                continue;
            }
            long dstId = toLong(endNode.getProperty("node_id", -1L), -1L);
            if (dstId <= 0L || !builder.hasNode(dstId)) {
                continue;
            }
            builder.addEdge(
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

    private record NodeRef(Node node, long nodeId) {
    }

    private record CachedSnapshot(long buildSeq, GraphSnapshot snapshot) {
    }

    private record LoadedSnapshot(long buildSeq, GraphSnapshot snapshot) {
    }

    private static final class CompressedGraphBuilder {
        private final long buildSeq;
        private final Map<Long, GraphNode> nodeMap;
        private final Map<String, List<GraphNode>> labelIndex;
        private final Map<Long, IntArrayBuilder> outgoingAdj;
        private final Map<Long, IntArrayBuilder> incomingAdj;
        private long[] edgeIds;
        private long[] srcIds;
        private long[] dstIds;
        private String[] relTypes;
        private String[] confidences;
        private String[] evidences;
        private String[] aliasKinds;
        private int[] opCodes;
        private int[] semanticFlags;
        private String[] callSiteKeys;
        private int[] lineNumbers;
        private int[] callIndexes;
        private int edgeCount;

        private CompressedGraphBuilder(long buildSeq) {
            this.buildSeq = buildSeq;
            this.nodeMap = new LinkedHashMap<>();
            this.labelIndex = new HashMap<>();
            this.outgoingAdj = new HashMap<>();
            this.incomingAdj = new HashMap<>();
            this.edgeIds = new long[64];
            this.srcIds = new long[64];
            this.dstIds = new long[64];
            this.relTypes = new String[64];
            this.confidences = new String[64];
            this.evidences = new String[64];
            this.aliasKinds = new String[64];
            this.opCodes = new int[64];
            this.semanticFlags = new int[64];
            this.callSiteKeys = new String[64];
            this.lineNumbers = new int[64];
            this.callIndexes = new int[64];
            this.edgeCount = 0;
        }

        private void addNode(GraphNode node) {
            if (node == null) {
                return;
            }
            nodeMap.put(node.getNodeId(), node);
        }

        private boolean hasNode(long nodeId) {
            return nodeMap.containsKey(nodeId);
        }

        private void addLabel(String label, GraphNode node) {
            if (node == null) {
                return;
            }
            String key = safe(label).toLowerCase();
            if (key.isBlank()) {
                return;
            }
            labelIndex.computeIfAbsent(key, ignore -> new ArrayList<>()).add(node);
        }

        private void addEdge(long edgeId,
                             long srcId,
                             long dstId,
                             String relType,
                             String confidence,
                             String evidence,
                             String aliasKind,
                             int opCode,
                             String callSiteKey,
                             int lineNumber,
                             int callIndex,
                             int edgeSemanticFlags) {
            ensureEdgeCapacity(edgeCount + 1);
            int index = edgeCount++;
            edgeIds[index] = edgeId;
            srcIds[index] = srcId;
            dstIds[index] = dstId;
            relTypes[index] = safe(relType);
            confidences[index] = safe(confidence);
            evidences[index] = safe(evidence);
            aliasKinds[index] = safe(aliasKind);
            opCodes[index] = opCode;
            semanticFlags[index] = Math.max(0, edgeSemanticFlags);
            callSiteKeys[index] = safe(callSiteKey);
            lineNumbers[index] = lineNumber;
            callIndexes[index] = callIndex;
            outgoingAdj.computeIfAbsent(srcId, ignore -> new IntArrayBuilder()).add(index);
            incomingAdj.computeIfAbsent(dstId, ignore -> new IntArrayBuilder()).add(index);
        }

        private LoadedSnapshot build() {
            return new LoadedSnapshot(
                    buildSeq,
                    GraphSnapshot.ofCompressed(
                            buildSeq,
                            nodeMap,
                            freezeAdjacency(outgoingAdj),
                            freezeAdjacency(incomingAdj),
                            Arrays.copyOf(edgeIds, edgeCount),
                            Arrays.copyOf(srcIds, edgeCount),
                            Arrays.copyOf(dstIds, edgeCount),
                            Arrays.copyOf(relTypes, edgeCount),
                            Arrays.copyOf(confidences, edgeCount),
                            Arrays.copyOf(evidences, edgeCount),
                            Arrays.copyOf(aliasKinds, edgeCount),
                            Arrays.copyOf(opCodes, edgeCount),
                            Arrays.copyOf(semanticFlags, edgeCount),
                            Arrays.copyOf(callSiteKeys, edgeCount),
                            Arrays.copyOf(lineNumbers, edgeCount),
                            Arrays.copyOf(callIndexes, edgeCount),
                            labelIndex
                    )
            );
        }

        private void ensureEdgeCapacity(int size) {
            if (size <= edgeIds.length) {
                return;
            }
            int next = Math.max(64, edgeIds.length * 2);
            while (next < size) {
                next *= 2;
            }
            edgeIds = Arrays.copyOf(edgeIds, next);
            srcIds = Arrays.copyOf(srcIds, next);
            dstIds = Arrays.copyOf(dstIds, next);
            relTypes = Arrays.copyOf(relTypes, next);
            confidences = Arrays.copyOf(confidences, next);
            evidences = Arrays.copyOf(evidences, next);
            aliasKinds = Arrays.copyOf(aliasKinds, next);
            opCodes = Arrays.copyOf(opCodes, next);
            semanticFlags = Arrays.copyOf(semanticFlags, next);
            callSiteKeys = Arrays.copyOf(callSiteKeys, next);
            lineNumbers = Arrays.copyOf(lineNumbers, next);
            callIndexes = Arrays.copyOf(callIndexes, next);
        }

        private static Map<Long, int[]> freezeAdjacency(Map<Long, IntArrayBuilder> source) {
            if (source == null || source.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<Long, int[]> out = new HashMap<>(source.size() * 2);
            for (Map.Entry<Long, IntArrayBuilder> entry : source.entrySet()) {
                out.put(entry.getKey(), entry.getValue() == null ? new int[0] : entry.getValue().toArray());
            }
            return out;
        }
    }

    private static final class IntArrayBuilder {
        private int[] values = new int[8];
        private int size;

        private void add(int value) {
            if (size >= values.length) {
                values = Arrays.copyOf(values, values.length * 2);
            }
            values[size++] = value;
        }

        private int[] toArray() {
            return Arrays.copyOf(values, size);
        }
    }
}
