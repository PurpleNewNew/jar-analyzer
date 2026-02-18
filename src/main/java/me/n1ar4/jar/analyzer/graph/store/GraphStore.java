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

import me.n1ar4.jar.analyzer.core.SQLiteDriver;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class GraphStore {
    private static final Logger logger = LogManager.getLogger();
    private static final String JDBC_URL = "jdbc:sqlite:" + Const.dbFile;
    private static final long UNKNOWN_BUILD_SEQ = -1L;
    private static final AtomicReference<CachedSnapshot> SNAPSHOT_CACHE = new AtomicReference<>();

    public GraphSnapshot loadSnapshot() {
        SQLiteDriver.ensureLoaded();
        CachedSnapshot cached = SNAPSHOT_CACHE.get();
        try (Connection conn = DriverManager.getConnection(JDBC_URL)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                long buildSeq = resolveBuildSeq(conn);
                if (cached != null && cached.buildSeq == buildSeq && cached.snapshot != null) {
                    conn.commit();
                    return cached.snapshot;
                }
                GraphSnapshot fresh = loadSnapshot(conn, buildSeq);
                conn.commit();
                SNAPSHOT_CACHE.set(new CachedSnapshot(buildSeq, fresh));
                return fresh;
            } catch (Exception ex) {
                try {
                    conn.rollback();
                } catch (Exception ignored) {
                    logger.debug("rollback graph snapshot read tx fail: {}", ignored.toString());
                }
                throw ex;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (Exception ex) {
            logger.warn("load graph snapshot fail: {}", ex.toString());
            CachedSnapshot fallback = SNAPSHOT_CACHE.get();
            if (fallback != null && fallback.snapshot != null) {
                return fallback.snapshot;
            }
            return GraphSnapshot.empty();
        }
    }

    public static void invalidateCache() {
        SNAPSHOT_CACHE.set(null);
    }

    private GraphSnapshot loadSnapshot(Connection conn, long buildSeq) throws Exception {
        Map<Long, GraphNode> nodeMap = new HashMap<>();
        Map<String, List<GraphNode>> labelIndex = new HashMap<>();

        EdgeColumnsBuilder edges = new EdgeColumnsBuilder(16_384);
        Map<Long, IntArrayBuilder> outgoing = new HashMap<>();
        Map<Long, IntArrayBuilder> incoming = new HashMap<>();

        try (Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT node_id, kind, jar_id, class_name, method_name, method_desc, call_site_key, line_number, call_index FROM graph_node ORDER BY node_id")) {
                while (rs.next()) {
                    long nodeId = rs.getLong(1);
                    GraphNode node = new GraphNode(
                            nodeId,
                            rs.getString(2),
                            rs.getInt(3),
                            rs.getString(4),
                            rs.getString(5),
                            rs.getString(6),
                            rs.getString(7),
                            rs.getInt(8),
                            rs.getInt(9)
                    );
                    nodeMap.put(nodeId, node);
                }
            }
            try (ResultSet rs = stmt.executeQuery("SELECT node_id, label FROM graph_label ORDER BY label, node_id")) {
                while (rs.next()) {
                    long nodeId = rs.getLong(1);
                    String label = safe(rs.getString(2)).toLowerCase();
                    GraphNode node = nodeMap.get(nodeId);
                    if (node == null) {
                        continue;
                    }
                    labelIndex.computeIfAbsent(label, key -> new ArrayList<>()).add(node);
                }
            }
            try (ResultSet rs = stmt.executeQuery("SELECT edge_id, src_id, dst_id, rel_type, confidence, evidence, op_code FROM graph_edge ORDER BY edge_id")) {
                while (rs.next()) {
                    long edgeId = rs.getLong(1);
                    long src = rs.getLong(2);
                    long dst = rs.getLong(3);
                    String relType = rs.getString(4);
                    String confidence = rs.getString(5);
                    String evidence = rs.getString(6);
                    int opCode = rs.getInt(7);

                    int edgeIdx = edges.add(edgeId, src, dst, relType, confidence, evidence, opCode);
                    outgoing.computeIfAbsent(src, key -> new IntArrayBuilder()).add(edgeIdx);
                    incoming.computeIfAbsent(dst, key -> new IntArrayBuilder()).add(edgeIdx);
                }
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

    private static long resolveBuildSeq(Connection conn) {
        if (conn == null) {
            return UNKNOWN_BUILD_SEQ;
        }
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(build_seq), -1) FROM graph_meta")) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception ex) {
            logger.debug("resolve graph build seq fail: {}", ex.toString());
        }
        return UNKNOWN_BUILD_SEQ;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
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
