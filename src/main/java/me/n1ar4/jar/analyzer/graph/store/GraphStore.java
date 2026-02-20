/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.store;

import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jStore;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.neo4j.graphdb.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class GraphStore {
    private static final Logger logger = LogManager.getLogger();
    private static final long UNKNOWN_BUILD_SEQ = -1L;
    private static final long SNAPSHOT_TIMEOUT_MS = 120_000L;
    private static final AtomicReference<CachedSnapshot> SNAPSHOT_CACHE = new AtomicReference<>();

    private final Neo4jStore neo4jStore;

    public GraphStore() {
        this(Neo4jStore.getInstance());
    }

    public GraphStore(Neo4jStore neo4jStore) {
        this.neo4jStore = neo4jStore == null ? Neo4jStore.getInstance() : neo4jStore;
    }

    public GraphSnapshot loadSnapshot() {
        CachedSnapshot cached = SNAPSHOT_CACHE.get();
        try {
            long buildSeq = resolveBuildSeq();
            if (cached != null && cached.buildSeq == buildSeq && cached.snapshot != null) {
                return cached.snapshot;
            }
            GraphSnapshot fresh = loadSnapshot(buildSeq);
            SNAPSHOT_CACHE.set(new CachedSnapshot(buildSeq, fresh));
            return fresh;
        } catch (Exception ex) {
            throw new IllegalStateException("load graph snapshot from neo4j fail: " + ex, ex);
        }
    }

    public static void invalidateCache() {
        SNAPSHOT_CACHE.set(null);
    }

    private long resolveBuildSeq() {
        try {
            Long buildSeq = neo4jStore.read(30_000L, tx -> {
                try (Result rs = tx.execute(
                        "MATCH (m:BuildMeta {name:'graph_projection'}) " +
                                "RETURN coalesce(m.buildSeq, -1) AS buildSeq LIMIT 1")) {
                    if (!rs.hasNext()) {
                        return UNKNOWN_BUILD_SEQ;
                    }
                    Map<String, Object> row = rs.next();
                    return asLong(row.get("buildSeq"), UNKNOWN_BUILD_SEQ);
                }
            });
            return buildSeq == null ? UNKNOWN_BUILD_SEQ : buildSeq;
        } catch (Exception ex) {
            logger.debug("resolve neo4j build seq fail: {}", ex.toString());
            return UNKNOWN_BUILD_SEQ;
        }
    }

    private GraphSnapshot loadSnapshot(long buildSeq) {
        return neo4jStore.read(SNAPSHOT_TIMEOUT_MS, tx -> {
            Map<Long, GraphNode> nodeMap = new HashMap<>();
            Map<String, List<GraphNode>> labelIndex = new HashMap<>();

            EdgeColumnsBuilder edges = new EdgeColumnsBuilder(16_384);
            Map<Long, IntArrayBuilder> outgoing = new HashMap<>();
            Map<Long, IntArrayBuilder> incoming = new HashMap<>();

            try (Result rs = tx.execute(
                    "MATCH (n:GraphNode) " +
                            "RETURN n.nodeId AS nodeId, " +
                            "coalesce(n.kind, '') AS kind, " +
                            "coalesce(n.jarId, -1) AS jarId, " +
                            "coalesce(n.className, '') AS className, " +
                            "coalesce(n.methodName, '') AS methodName, " +
                            "coalesce(n.methodDesc, '') AS methodDesc, " +
                            "coalesce(n.callSiteKey, '') AS callSiteKey, " +
                            "coalesce(n.lineNumber, -1) AS lineNumber, " +
                            "coalesce(n.callIndex, -1) AS callIndex, " +
                            "labels(n) AS labels " +
                            "ORDER BY nodeId")) {
                while (rs.hasNext()) {
                    Map<String, Object> row = rs.next();
                    long nodeId = asLong(row.get("nodeId"), -1L);
                    if (nodeId <= 0L) {
                        continue;
                    }
                    GraphNode node = new GraphNode(
                            nodeId,
                            safe(row.get("kind")),
                            asInt(row.get("jarId"), -1),
                            safe(row.get("className")),
                            safe(row.get("methodName")),
                            safe(row.get("methodDesc")),
                            safe(row.get("callSiteKey")),
                            asInt(row.get("lineNumber"), -1),
                            asInt(row.get("callIndex"), -1)
                    );
                    nodeMap.put(nodeId, node);

                    Object labels = row.get("labels");
                    if (labels instanceof Iterable<?> iterable) {
                        for (Object label : iterable) {
                            String normalized = safe(label).toLowerCase();
                            if (normalized.isEmpty()) {
                                continue;
                            }
                            labelIndex.computeIfAbsent(normalized, key -> new ArrayList<>()).add(node);
                        }
                    }
                }
            }

            try (Result rs = tx.execute(
                    "MATCH (s:GraphNode)-[r:GRAPH_EDGE]->(d:GraphNode) " +
                            "RETURN coalesce(r.edgeId, id(r)) AS edgeId, " +
                            "s.nodeId AS srcId, d.nodeId AS dstId, " +
                            "coalesce(r.relType, type(r)) AS relType, " +
                            "coalesce(r.confidence, '') AS confidence, " +
                            "coalesce(r.evidence, '') AS evidence, " +
                            "coalesce(r.opCode, -1) AS opCode " +
                            "ORDER BY edgeId")) {
                while (rs.hasNext()) {
                    Map<String, Object> row = rs.next();
                    long edgeId = asLong(row.get("edgeId"), -1L);
                    long src = asLong(row.get("srcId"), -1L);
                    long dst = asLong(row.get("dstId"), -1L);
                    if (src <= 0L || dst <= 0L) {
                        continue;
                    }
                    int edgeIdx = edges.add(
                            edgeId,
                            src,
                            dst,
                            safe(row.get("relType")),
                            safe(row.get("confidence")),
                            safe(row.get("evidence")),
                            asInt(row.get("opCode"), -1)
                    );
                    outgoing.computeIfAbsent(src, key -> new IntArrayBuilder()).add(edgeIdx);
                    incoming.computeIfAbsent(dst, key -> new IntArrayBuilder()).add(edgeIdx);
                }
            }

            return GraphSnapshot.ofCompressed(
                    buildSeq,
                    nodeMap,
                    freezeAdjacency(outgoing),
                    freezeAdjacency(incoming),
                    edges.edgeIds(),
                    edges.srcIds(),
                    edges.dstIds(),
                    edges.relTypes(),
                    edges.confidences(),
                    edges.evidences(),
                    edges.opCodes(),
                    labelIndex
            );
        });
    }

    private static Map<Long, int[]> freezeAdjacency(Map<Long, IntArrayBuilder> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, int[]> out = new HashMap<>(source.size() * 2);
        for (Map.Entry<Long, IntArrayBuilder> entry : source.entrySet()) {
            out.put(entry.getKey(), entry.getValue().toArray());
        }
        return out;
    }

    private static long asLong(Object value, long def) {
        if (value == null) {
            return def;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return def;
        }
    }

    private static int asInt(Object value, int def) {
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

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record CachedSnapshot(long buildSeq, GraphSnapshot snapshot) {
    }

    private static final class IntArrayBuilder {
        private int[] data = new int[16];
        private int size = 0;

        private void add(int value) {
            if (size >= data.length) {
                int[] next = new int[data.length * 2];
                System.arraycopy(data, 0, next, 0, data.length);
                data = next;
            }
            data[size++] = value;
        }

        private int[] toArray() {
            if (size == 0) {
                return new int[0];
            }
            int[] out = new int[size];
            System.arraycopy(data, 0, out, 0, size);
            return out;
        }
    }

    private static final class EdgeColumnsBuilder {
        private long[] edgeIds;
        private long[] srcIds;
        private long[] dstIds;
        private String[] relTypes;
        private String[] confidences;
        private String[] evidences;
        private int[] opCodes;
        private int size;

        private EdgeColumnsBuilder(int initCapacity) {
            int cap = Math.max(16, initCapacity);
            this.edgeIds = new long[cap];
            this.srcIds = new long[cap];
            this.dstIds = new long[cap];
            this.relTypes = new String[cap];
            this.confidences = new String[cap];
            this.evidences = new String[cap];
            this.opCodes = new int[cap];
            this.size = 0;
        }

        private int add(long edgeId,
                        long src,
                        long dst,
                        String relType,
                        String confidence,
                        String evidence,
                        int opCode) {
            if (size >= edgeIds.length) {
                grow();
            }
            int idx = size++;
            edgeIds[idx] = edgeId;
            srcIds[idx] = src;
            dstIds[idx] = dst;
            relTypes[idx] = safe(relType);
            confidences[idx] = safe(confidence);
            evidences[idx] = safe(evidence);
            opCodes[idx] = opCode;
            return idx;
        }

        private long[] edgeIds() {
            return trim(edgeIds);
        }

        private long[] srcIds() {
            return trim(srcIds);
        }

        private long[] dstIds() {
            return trim(dstIds);
        }

        private String[] relTypes() {
            return trim(relTypes);
        }

        private String[] confidences() {
            return trim(confidences);
        }

        private String[] evidences() {
            return trim(evidences);
        }

        private int[] opCodes() {
            return trim(opCodes);
        }

        private void grow() {
            int next = edgeIds.length * 2;
            edgeIds = copy(edgeIds, next);
            srcIds = copy(srcIds, next);
            dstIds = copy(dstIds, next);
            relTypes = copy(relTypes, next);
            confidences = copy(confidences, next);
            evidences = copy(evidences, next);
            opCodes = copy(opCodes, next);
        }

        private long[] trim(long[] source) {
            if (size == 0) {
                return new long[0];
            }
            long[] out = new long[size];
            System.arraycopy(source, 0, out, 0, size);
            return out;
        }

        private int[] trim(int[] source) {
            if (size == 0) {
                return new int[0];
            }
            int[] out = new int[size];
            System.arraycopy(source, 0, out, 0, size);
            return out;
        }

        private String[] trim(String[] source) {
            if (size == 0) {
                return new String[0];
            }
            String[] out = new String[size];
            System.arraycopy(source, 0, out, 0, size);
            return out;
        }

        private static long[] copy(long[] source, int newSize) {
            long[] out = new long[newSize];
            System.arraycopy(source, 0, out, 0, source.length);
            return out;
        }

        private static int[] copy(int[] source, int newSize) {
            int[] out = new int[newSize];
            System.arraycopy(source, 0, out, 0, source.length);
            return out;
        }

        private static String[] copy(String[] source, int newSize) {
            String[] out = new String[newSize];
            System.arraycopy(source, 0, out, 0, source.length);
            return out;
        }
    }
}
