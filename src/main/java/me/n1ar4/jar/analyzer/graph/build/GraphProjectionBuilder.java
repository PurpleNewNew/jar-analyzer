/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.build;

import me.n1ar4.jar.analyzer.core.SQLiteDriver;
import me.n1ar4.jar.analyzer.graph.model.GraphRelationType;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GraphProjectionBuilder {
    private static final Logger logger = LogManager.getLogger();
    private static final String GRAPH_SCHEMA_VERSION = "v1";
    private static final String JDBC_URL = "jdbc:sqlite:" + Const.dbFile;
    private static final int BATCH_SIZE = 1000;
    private static final String TEMP_EDGE_TABLE = "tmp_graph_edge_desired";

    private GraphProjectionBuilder() {
    }

    public static void projectCurrentBuild(long buildSeq, boolean quickMode, String callGraphMode) {
        long start = System.nanoTime();
        SQLiteDriver.ensureLoaded();
        try (Connection conn = DriverManager.getConnection(JDBC_URL)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                long projectionSeq = resolveProjectionSeq(conn, buildSeq);
                ExistingGraphState existing = loadExistingState(conn);
                ProjectionPlan plan = buildProjectionPlan(conn, existing);

                NodeSyncStats nodeStats = upsertNodesAndLabels(conn, plan, existing, projectionSeq);
                EdgeSyncStats edgeStats = upsertAndPruneEdges(conn, plan, projectionSeq);
                long deletedNodeCount = pruneStaleNodes(conn, projectionSeq);

                saveGraphMeta(conn, buildSeq, quickMode, callGraphMode);
                saveGraphStats(conn,
                        plan.methodNodeByKey.size(),
                        plan.callSiteNodeByCallSiteKey.size(),
                        edgeStats.totalEdges,
                        nodeStats,
                        edgeStats,
                        deletedNodeCount);

                conn.commit();
                GraphStore.invalidateCache();
                long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
                logger.info("graph projection finish: methodNodes={} callSiteNodes={} edges={} nodeIns={} nodeUpd={} nodeDel={} edgeIns={} edgeUpd={} edgeDel={} elapsedMs={}",
                        plan.methodNodeByKey.size(),
                        plan.callSiteNodeByCallSiteKey.size(),
                        edgeStats.totalEdges,
                        nodeStats.inserted,
                        nodeStats.updated,
                        deletedNodeCount,
                        edgeStats.inserted,
                        edgeStats.updated,
                        edgeStats.deleted,
                        elapsedMs);
            } catch (Exception ex) {
                conn.rollback();
                GraphStore.invalidateCache();
                logger.warn("graph projection rollback: {}", ex.toString());
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (Exception ex) {
            logger.warn("graph projection fail: {}", ex.toString());
        }
    }

    private static ExistingGraphState loadExistingState(Connection conn) throws Exception {
        Map<MethodKey, Long> methodNodeByKey = new HashMap<>();
        Map<String, Long> callSiteNodeByKey = new HashMap<>();
        long maxNodeId = 0L;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT node_id, kind, jar_id, class_name, method_name, method_desc, call_site_key FROM graph_node");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long nodeId = rs.getLong(1);
                String kind = safe(rs.getString(2)).toLowerCase();
                int jarId = rs.getInt(3);
                String className = safe(rs.getString(4));
                String methodName = safe(rs.getString(5));
                String methodDesc = safe(rs.getString(6));
                String callSiteKey = safe(rs.getString(7));
                maxNodeId = Math.max(maxNodeId, nodeId);
                if ("method".equals(kind)) {
                    methodNodeByKey.putIfAbsent(new MethodKey(className, methodName, methodDesc, jarId), nodeId);
                } else if ("callsite".equals(kind) && !callSiteKey.isEmpty()) {
                    callSiteNodeByKey.putIfAbsent(callSiteKey, nodeId);
                }
            }
        }

        return new ExistingGraphState(methodNodeByKey, callSiteNodeByKey, maxNodeId);
    }

    private static ProjectionPlan buildProjectionPlan(Connection conn, ExistingGraphState existing) throws Exception {
        long nextNodeId = existing.maxNodeId + 1L;

        Map<MethodKey, Long> methodNodeByKey = new LinkedHashMap<>();
        Map<MethodLooseKey, Long> methodNodeByLooseKey = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT class_name, method_name, method_desc, jar_id FROM method_table ORDER BY class_name, method_name, method_desc, jar_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                MethodKey key = new MethodKey(
                        safe(rs.getString(1)),
                        safe(rs.getString(2)),
                        safe(rs.getString(3)),
                        rs.getInt(4)
                );
                if (methodNodeByKey.containsKey(key)) {
                    continue;
                }
                Long nodeId = existing.methodNodeByKey.get(key);
                if (nodeId == null) {
                    nodeId = nextNodeId++;
                }
                methodNodeByKey.put(key, nodeId);
                methodNodeByLooseKey.putIfAbsent(new MethodLooseKey(key.className, key.methodName, key.methodDesc), nodeId);
            }
        }

        Map<String, Long> callSiteNodeByCallSiteKey = new LinkedHashMap<>();
        List<CallSiteRow> callSiteRows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT cs_id, caller_class_name, caller_method_name, caller_method_desc, callee_owner, callee_method_name, callee_method_desc, op_code, line_number, call_index, jar_id, call_site_key " +
                        "FROM bytecode_call_site_table " +
                        "ORDER BY caller_class_name, caller_method_name, caller_method_desc, call_index, line_number, cs_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int csId = rs.getInt(1);
                String callerClass = safe(rs.getString(2));
                String callerMethod = safe(rs.getString(3));
                String callerDesc = safe(rs.getString(4));
                String calleeClass = safe(rs.getString(5));
                String calleeMethod = safe(rs.getString(6));
                String calleeDesc = safe(rs.getString(7));
                int opCode = rs.getInt(8);
                int lineNumber = rs.getInt(9);
                int callIndex = rs.getInt(10);
                int jarId = rs.getInt(11);
                String callSiteKey = safe(rs.getString(12));
                if (callSiteKey.isEmpty()) {
                    callSiteKey = callerClass + "#" + callerMethod + "#" + callerDesc + "#" + callIndex + "#" + lineNumber + "#" + csId;
                }
                Long nodeId = callSiteNodeByCallSiteKey.get(callSiteKey);
                if (nodeId == null) {
                    nodeId = existing.callSiteNodeByKey.get(callSiteKey);
                    if (nodeId == null) {
                        nodeId = nextNodeId++;
                    }
                    callSiteNodeByCallSiteKey.put(callSiteKey, nodeId);
                }
                callSiteRows.add(new CallSiteRow(
                        csId,
                        callSiteKey,
                        nodeId,
                        callerClass,
                        callerMethod,
                        callerDesc,
                        calleeClass,
                        calleeMethod,
                        calleeDesc,
                        opCode,
                        lineNumber,
                        callIndex,
                        jarId
                ));
            }
        }

        MethodNodeState methods = new MethodNodeState(methodNodeByKey, methodNodeByLooseKey);
        CallSiteState callSites = new CallSiteState(callSiteNodeByCallSiteKey, callSiteRows);
        return new ProjectionPlan(methods, callSites, methodNodeByKey, callSiteNodeByCallSiteKey);
    }

    private static void collectMethodCallEdges(Connection conn,
                                               MethodNodeState methods,
                                               EdgeSink sink) throws Exception {
        try (PreparedStatement select = conn.prepareStatement(
                "SELECT caller_class_name, caller_method_name, caller_method_desc, caller_jar_id, " +
                        "callee_class_name, callee_method_name, callee_method_desc, callee_jar_id, " +
                        "edge_type, edge_confidence, edge_evidence, op_code " +
                        "FROM method_call_table " +
                        "ORDER BY caller_class_name, caller_method_name, caller_method_desc, caller_jar_id, " +
                        "callee_class_name, callee_method_name, callee_method_desc, callee_jar_id, op_code");
             ResultSet rs = select.executeQuery()) {
            while (rs.next()) {
                MethodKey caller = new MethodKey(
                        safe(rs.getString(1)),
                        safe(rs.getString(2)),
                        safe(rs.getString(3)),
                        rs.getInt(4)
                );
                MethodKey callee = new MethodKey(
                        safe(rs.getString(5)),
                        safe(rs.getString(6)),
                        safe(rs.getString(7)),
                        rs.getInt(8)
                );
                Long src = resolveMethodNode(methods, caller);
                Long dst = resolveMethodNode(methods, callee);
                if (src == null || dst == null) {
                    continue;
                }
                String relType = GraphRelationType.fromEdgeType(rs.getString(9)).name();
                String confidence = safe(rs.getString(10));
                if (confidence.isEmpty()) {
                    confidence = "low";
                }
                String evidence = safe(rs.getString(11));
                int opCode = rs.getInt(12);
                sink.accept(src, dst, relType, confidence, evidence, opCode);
            }
        }
    }

    private static void collectCallSiteEdges(MethodNodeState methods,
                                             CallSiteState callSites,
                                             EdgeSink sink) throws Exception {
        for (CallSiteRow row : callSites.callSiteRows) {
            Long methodNode = resolveMethodNode(methods,
                    new MethodKey(row.callerClass, row.callerMethod, row.callerDesc, row.jarId));
            if (methodNode != null) {
                sink.accept(
                        methodNode,
                        row.nodeId,
                        GraphRelationType.CONTAINS_CALLSITE.name(),
                        "high",
                        "callsite",
                        row.opCode
                );
            }

            Long calleeNode = resolveMethodNode(methods,
                    new MethodKey(row.calleeClass, row.calleeMethod, row.calleeDesc, -1));
            if (calleeNode != null) {
                sink.accept(
                        row.nodeId,
                        calleeNode,
                        GraphRelationType.CALLSITE_TO_CALLEE.name(),
                        "medium",
                        "bytecode",
                        row.opCode
                );
            }
        }
    }

    private static void collectNextCallSiteEdges(CallSiteState callSites,
                                                 EdgeSink sink) throws Exception {
        Map<String, List<CallSiteRow>> byMethod = new LinkedHashMap<>();
        for (CallSiteRow row : callSites.callSiteRows) {
            String key = row.callerClass + "#" + row.callerMethod + "#" + row.callerDesc + "#" + row.jarId;
            byMethod.computeIfAbsent(key, ignore -> new ArrayList<>()).add(row);
        }
        for (List<CallSiteRow> rows : byMethod.values()) {
            rows.sort((a, b) -> {
                int ai = normalizeInt(a.callIndex);
                int bi = normalizeInt(b.callIndex);
                if (ai != bi) {
                    return Integer.compare(ai, bi);
                }
                int al = normalizeInt(a.lineNumber);
                int bl = normalizeInt(b.lineNumber);
                if (al != bl) {
                    return Integer.compare(al, bl);
                }
                return Integer.compare(a.csId, b.csId);
            });
            for (int i = 0; i + 1 < rows.size(); i++) {
                CallSiteRow cur = rows.get(i);
                CallSiteRow next = rows.get(i + 1);
                if (cur.nodeId == next.nodeId) {
                    continue;
                }
                sink.accept(
                        cur.nodeId,
                        next.nodeId,
                        GraphRelationType.NEXT_CALLSITE.name(),
                        "high",
                        "order",
                        -1
                );
            }
        }
    }

    private static Long resolveMethodNode(MethodNodeState methods, MethodKey key) {
        if (methods == null || key == null) {
            return null;
        }
        Long exact = methods.nodeByMethodKey.get(key);
        if (exact != null) {
            return exact;
        }
        return methods.nodeByLooseKey.get(new MethodLooseKey(key.className, key.methodName, key.methodDesc));
    }

    private static NodeSyncStats upsertNodesAndLabels(Connection conn,
                                                      ProjectionPlan plan,
                                                      ExistingGraphState existing,
                                                      long projectionSeq) throws Exception {
        NodeSyncStats stats = new NodeSyncStats();
        String nodeSql = "INSERT INTO graph_node(node_id, kind, jar_id, class_name, method_name, method_desc, call_site_key, line_number, call_index, props_json, last_seen_build_seq) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(node_id) DO UPDATE SET " +
                "kind=excluded.kind, jar_id=excluded.jar_id, class_name=excluded.class_name, method_name=excluded.method_name, " +
                "method_desc=excluded.method_desc, call_site_key=excluded.call_site_key, line_number=excluded.line_number, " +
                "call_index=excluded.call_index, props_json=excluded.props_json, last_seen_build_seq=excluded.last_seen_build_seq";
        String labelSql = "INSERT INTO graph_label(node_id, label) VALUES(?, ?) ON CONFLICT(node_id, label) DO NOTHING";

        try (PreparedStatement nodeInsert = conn.prepareStatement(nodeSql);
             PreparedStatement labelInsert = conn.prepareStatement(labelSql)) {
            int batch = 0;
            for (Map.Entry<MethodKey, Long> entry : plan.methodNodeByKey.entrySet()) {
                MethodKey key = entry.getKey();
                long nodeId = entry.getValue();
                if (existing.methodNodeByKey.containsKey(key)) {
                    stats.updated++;
                } else {
                    stats.inserted++;
                }
                bindNode(nodeInsert, nodeId, "method", key.jarId,
                        key.className, key.methodName, key.methodDesc, "", -1, -1, projectionSeq);
                nodeInsert.addBatch();
                bindLabel(labelInsert, nodeId, "Method");
                labelInsert.addBatch();
                batch++;
                if (batch % BATCH_SIZE == 0) {
                    nodeInsert.executeBatch();
                    labelInsert.executeBatch();
                }
            }
            Set<Long> seenCallSiteNodes = new HashSet<>();
            for (CallSiteRow row : plan.callSites.callSiteRows) {
                long nodeId = row.nodeId;
                if (!seenCallSiteNodes.add(nodeId)) {
                    continue;
                }
                if (existing.callSiteNodeByKey.containsKey(row.callSiteKey)) {
                    stats.updated++;
                } else {
                    stats.inserted++;
                }
                bindNode(nodeInsert, nodeId, "callsite", row.jarId,
                        row.callerClass, row.callerMethod, row.callerDesc,
                        row.callSiteKey, row.lineNumber, row.callIndex, projectionSeq);
                nodeInsert.addBatch();
                bindLabel(labelInsert, nodeId, "CallSite");
                labelInsert.addBatch();
                batch++;
                if (batch % BATCH_SIZE == 0) {
                    nodeInsert.executeBatch();
                    labelInsert.executeBatch();
                }
            }
            nodeInsert.executeBatch();
            labelInsert.executeBatch();
        }
        return stats;
    }

    private static EdgeSyncStats upsertAndPruneEdges(Connection conn,
                                                     ProjectionPlan plan,
                                                     long projectionSeq) throws Exception {
        createTempEdgeTable(conn);
        try {
            writeDesiredEdgesToTemp(conn, plan);

            EdgeSyncStats stats = new EdgeSyncStats();
            stats.updated = markExistingEdgesSeen(conn, projectionSeq);
            stats.inserted = insertMissingEdges(conn, projectionSeq);
            stats.deleted = pruneStaleEdges(conn, projectionSeq);
            stats.totalEdges = countLong(conn, "SELECT COUNT(1) FROM graph_edge");
            return stats;
        } finally {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS " + TEMP_EDGE_TABLE);
            }
        }
    }

    private static void createTempEdgeTable(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TEMP TABLE IF NOT EXISTS " + TEMP_EDGE_TABLE + " (" +
                    "src_id INTEGER NOT NULL," +
                    "dst_id INTEGER NOT NULL," +
                    "rel_type TEXT NOT NULL," +
                    "confidence TEXT NOT NULL," +
                    "evidence TEXT NOT NULL," +
                    "op_code INTEGER NOT NULL," +
                    "PRIMARY KEY(src_id, dst_id, rel_type, confidence, evidence, op_code)" +
                    ") WITHOUT ROWID");
            stmt.execute("DELETE FROM " + TEMP_EDGE_TABLE);
        }
    }

    private static void writeDesiredEdgesToTemp(Connection conn,
                                                ProjectionPlan plan) throws Exception {
        try (TempEdgeWriter writer = new TempEdgeWriter(conn)) {
            collectMethodCallEdges(conn, plan.methods, writer::accept);
            collectCallSiteEdges(plan.methods, plan.callSites, writer::accept);
            collectNextCallSiteEdges(plan.callSites, writer::accept);
        }
    }

    private static long markExistingEdgesSeen(Connection conn, long projectionSeq) throws Exception {
        String sql = "UPDATE graph_edge " +
                "SET last_seen_build_seq=?, weight=1.0, props_json='{}' " +
                "WHERE EXISTS (" +
                "SELECT 1 FROM " + TEMP_EDGE_TABLE + " d " +
                "WHERE d.src_id=graph_edge.src_id AND d.dst_id=graph_edge.dst_id " +
                "AND d.rel_type=graph_edge.rel_type AND d.confidence=graph_edge.confidence " +
                "AND d.evidence=graph_edge.evidence AND d.op_code=graph_edge.op_code)";
        try (PreparedStatement update = conn.prepareStatement(sql)) {
            update.setLong(1, projectionSeq);
            return update.executeUpdate();
        }
    }

    private static long insertMissingEdges(Connection conn, long projectionSeq) throws Exception {
        String sql = "INSERT INTO graph_edge(src_id, dst_id, rel_type, confidence, evidence, op_code, weight, props_json, last_seen_build_seq) " +
                "SELECT d.src_id, d.dst_id, d.rel_type, d.confidence, d.evidence, d.op_code, 1.0, '{}', ? " +
                "FROM " + TEMP_EDGE_TABLE + " d " +
                "WHERE NOT EXISTS (" +
                "SELECT 1 FROM graph_edge ge " +
                "WHERE ge.src_id=d.src_id AND ge.dst_id=d.dst_id " +
                "AND ge.rel_type=d.rel_type AND ge.confidence=d.confidence " +
                "AND ge.evidence=d.evidence AND ge.op_code=d.op_code)";
        try (PreparedStatement insert = conn.prepareStatement(sql)) {
            insert.setLong(1, projectionSeq);
            return insert.executeUpdate();
        }
    }

    private static long pruneStaleEdges(Connection conn, long projectionSeq) throws Exception {
        long staleCount = countLong(conn,
                "SELECT COUNT(1) FROM graph_edge WHERE COALESCE(last_seen_build_seq, -1) <> ?",
                projectionSeq);
        if (staleCount <= 0L) {
            return 0L;
        }
        try (PreparedStatement deleteEdgeAttr = conn.prepareStatement(
                "DELETE FROM graph_attr WHERE owner_type='edge' AND owner_id IN (" +
                        "SELECT edge_id FROM graph_edge WHERE COALESCE(last_seen_build_seq, -1) <> ?)");
             PreparedStatement deleteEdge = conn.prepareStatement(
                     "DELETE FROM graph_edge WHERE COALESCE(last_seen_build_seq, -1) <> ?")) {
            deleteEdgeAttr.setLong(1, projectionSeq);
            deleteEdgeAttr.executeUpdate();
            deleteEdge.setLong(1, projectionSeq);
            deleteEdge.executeUpdate();
        }
        return staleCount;
    }

    private static long pruneStaleNodes(Connection conn, long projectionSeq) throws Exception {
        long staleCount = countLong(conn,
                "SELECT COUNT(1) FROM graph_node WHERE COALESCE(last_seen_build_seq, -1) <> ?",
                projectionSeq);
        if (staleCount <= 0L) {
            return 0L;
        }
        try (PreparedStatement deleteLabel = conn.prepareStatement(
                "DELETE FROM graph_label WHERE node_id IN (" +
                        "SELECT node_id FROM graph_node WHERE COALESCE(last_seen_build_seq, -1) <> ?)");
             PreparedStatement deleteAttr = conn.prepareStatement(
                     "DELETE FROM graph_attr WHERE owner_type='node' AND owner_id IN (" +
                             "SELECT node_id FROM graph_node WHERE COALESCE(last_seen_build_seq, -1) <> ?)");
             PreparedStatement deleteNode = conn.prepareStatement(
                     "DELETE FROM graph_node WHERE COALESCE(last_seen_build_seq, -1) <> ?")) {
            deleteLabel.setLong(1, projectionSeq);
            deleteLabel.executeUpdate();
            deleteAttr.setLong(1, projectionSeq);
            deleteAttr.executeUpdate();
            deleteNode.setLong(1, projectionSeq);
            deleteNode.executeUpdate();
        }
        return staleCount;
    }

    private static void bindNode(PreparedStatement ps,
                                 long nodeId,
                                 String kind,
                                 int jarId,
                                 String className,
                                 String methodName,
                                 String methodDesc,
                                 String callSiteKey,
                                 int lineNumber,
                                 int callIndex,
                                 long projectionSeq) throws Exception {
        ps.setLong(1, nodeId);
        ps.setString(2, safe(kind));
        ps.setInt(3, jarId);
        ps.setString(4, safe(className));
        ps.setString(5, safe(methodName));
        ps.setString(6, safe(methodDesc));
        ps.setString(7, safe(callSiteKey));
        ps.setInt(8, lineNumber);
        ps.setInt(9, callIndex);
        ps.setString(10, "{}");
        ps.setLong(11, projectionSeq);
    }

    private static void bindLabel(PreparedStatement ps, long nodeId, String label) throws Exception {
        ps.setLong(1, nodeId);
        ps.setString(2, label);
    }

    private static void saveGraphMeta(Connection conn,
                                      long buildSeq,
                                      boolean quickMode,
                                      String callGraphMode) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM graph_meta");
        }
        try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO graph_meta(schema_version, build_seq, built_at, options_json) VALUES(?, ?, strftime('%s','now'), ?)") ) {
            insert.setString(1, GRAPH_SCHEMA_VERSION);
            insert.setLong(2, buildSeq);
            insert.setString(3,
                    "{\"quickMode\":" + quickMode
                            + ",\"callGraphMode\":\"" + safe(callGraphMode)
                            + "\",\"projectionMode\":\"streaming_delta\"}");
            insert.executeUpdate();
        }
    }

    private static void saveGraphStats(Connection conn,
                                       int methodNodeCount,
                                       int callSiteNodeCount,
                                       long edgeCount,
                                       NodeSyncStats nodeStats,
                                       EdgeSyncStats edgeStats,
                                       long deletedNodeCount) throws Exception {
        try (PreparedStatement insert = conn.prepareStatement(
                "INSERT OR REPLACE INTO graph_stats(metric, metric_value, updated_at) VALUES(?, ?, strftime('%s','now'))")) {
            insertMetric(insert, "method_nodes", String.valueOf(methodNodeCount));
            insertMetric(insert, "callsite_nodes", String.valueOf(callSiteNodeCount));
            insertMetric(insert, "total_nodes", String.valueOf((long) methodNodeCount + callSiteNodeCount));
            insertMetric(insert, "total_edges", String.valueOf(edgeCount));
            insertMetric(insert, "schema_version", GRAPH_SCHEMA_VERSION);
            insertMetric(insert, "node_upsert_inserted", String.valueOf(nodeStats.inserted));
            insertMetric(insert, "node_upsert_updated", String.valueOf(nodeStats.updated));
            insertMetric(insert, "node_deleted", String.valueOf(deletedNodeCount));
            insertMetric(insert, "edge_upsert_inserted", String.valueOf(edgeStats.inserted));
            insertMetric(insert, "edge_upsert_updated", String.valueOf(edgeStats.updated));
            insertMetric(insert, "edge_deleted", String.valueOf(edgeStats.deleted));
            insert.executeBatch();
        }
    }

    private static void insertMetric(PreparedStatement insert, String metric, String value) throws Exception {
        insert.setString(1, metric);
        insert.setString(2, value);
        insert.addBatch();
    }

    private static long resolveProjectionSeq(Connection conn, long buildSeq) {
        long marker = Math.max(1L, buildSeq);
        marker = Math.max(marker, safeMax(conn, "SELECT COALESCE(MAX(build_seq), 0) FROM graph_meta") + 1L);
        marker = Math.max(marker, safeMax(conn, "SELECT COALESCE(MAX(last_seen_build_seq), 0) FROM graph_node") + 1L);
        marker = Math.max(marker, safeMax(conn, "SELECT COALESCE(MAX(last_seen_build_seq), 0) FROM graph_edge") + 1L);
        return marker;
    }

    private static long safeMax(Connection conn, String sql) {
        try {
            return countLong(conn, sql);
        } catch (Exception ex) {
            return 0L;
        }
    }

    private static long countLong(Connection conn, String sql) throws Exception {
        return countLong(conn, sql, null);
    }

    private static long countLong(Connection conn, String sql, Long arg) throws Exception {
        if (arg == null) {
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            return 0L;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, arg);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0L;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static int normalizeInt(int value) {
        return value < 0 ? Integer.MAX_VALUE : value;
    }

    private interface EdgeSink {
        void accept(long src, long dst, String relType, String confidence, String evidence, int opCode) throws Exception;
    }

    private static final class TempEdgeWriter implements AutoCloseable {
        private final PreparedStatement insert;
        private int batch;

        private TempEdgeWriter(Connection conn) throws Exception {
            this.insert = conn.prepareStatement(
                    "INSERT OR IGNORE INTO " + TEMP_EDGE_TABLE + "(src_id, dst_id, rel_type, confidence, evidence, op_code) VALUES(?, ?, ?, ?, ?, ?)");
            this.batch = 0;
        }

        private void accept(long src,
                            long dst,
                            String relType,
                            String confidence,
                            String evidence,
                            int opCode) throws Exception {
            insert.setLong(1, src);
            insert.setLong(2, dst);
            insert.setString(3, safe(relType));
            String conf = safe(confidence);
            if (conf.isEmpty()) {
                conf = "low";
            }
            insert.setString(4, conf);
            insert.setString(5, safe(evidence));
            insert.setInt(6, opCode);
            insert.addBatch();
            batch++;
            if (batch % BATCH_SIZE == 0) {
                insert.executeBatch();
            }
        }

        @Override
        public void close() throws Exception {
            insert.executeBatch();
            insert.close();
        }
    }

    private static final class ExistingGraphState {
        private final Map<MethodKey, Long> methodNodeByKey;
        private final Map<String, Long> callSiteNodeByKey;
        private final long maxNodeId;

        private ExistingGraphState(Map<MethodKey, Long> methodNodeByKey,
                                   Map<String, Long> callSiteNodeByKey,
                                   long maxNodeId) {
            this.methodNodeByKey = methodNodeByKey;
            this.callSiteNodeByKey = callSiteNodeByKey;
            this.maxNodeId = maxNodeId;
        }
    }

    private static final class ProjectionPlan {
        private final MethodNodeState methods;
        private final CallSiteState callSites;
        private final Map<MethodKey, Long> methodNodeByKey;
        private final Map<String, Long> callSiteNodeByCallSiteKey;

        private ProjectionPlan(MethodNodeState methods,
                               CallSiteState callSites,
                               Map<MethodKey, Long> methodNodeByKey,
                               Map<String, Long> callSiteNodeByCallSiteKey) {
            this.methods = methods;
            this.callSites = callSites;
            this.methodNodeByKey = methodNodeByKey;
            this.callSiteNodeByCallSiteKey = callSiteNodeByCallSiteKey;
        }
    }

    private static final class MethodNodeState {
        private final Map<MethodKey, Long> nodeByMethodKey;
        private final Map<MethodLooseKey, Long> nodeByLooseKey;

        private MethodNodeState(Map<MethodKey, Long> nodeByMethodKey,
                                Map<MethodLooseKey, Long> nodeByLooseKey) {
            this.nodeByMethodKey = nodeByMethodKey;
            this.nodeByLooseKey = nodeByLooseKey;
        }
    }

    private static final class CallSiteState {
        private final Map<String, Long> nodeByCallSiteKey;
        private final List<CallSiteRow> callSiteRows;

        private CallSiteState(Map<String, Long> nodeByCallSiteKey,
                              List<CallSiteRow> callSiteRows) {
            this.nodeByCallSiteKey = nodeByCallSiteKey;
            this.callSiteRows = callSiteRows;
        }
    }

    private static final class NodeSyncStats {
        private long inserted;
        private long updated;
    }

    private static final class EdgeSyncStats {
        private long inserted;
        private long updated;
        private long deleted;
        private long totalEdges;
    }

    private static final class MethodKey {
        private final String className;
        private final String methodName;
        private final String methodDesc;
        private final int jarId;

        private MethodKey(String className, String methodName, String methodDesc, int jarId) {
            this.className = safe(className);
            this.methodName = safe(methodName);
            this.methodDesc = safe(methodDesc);
            this.jarId = jarId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MethodKey other)) {
                return false;
            }
            return jarId == other.jarId
                    && className.equals(other.className)
                    && methodName.equals(other.methodName)
                    && methodDesc.equals(other.methodDesc);
        }

        @Override
        public int hashCode() {
            int result = className.hashCode();
            result = 31 * result + methodName.hashCode();
            result = 31 * result + methodDesc.hashCode();
            result = 31 * result + jarId;
            return result;
        }
    }

    private static final class MethodLooseKey {
        private final String className;
        private final String methodName;
        private final String methodDesc;

        private MethodLooseKey(String className, String methodName, String methodDesc) {
            this.className = safe(className);
            this.methodName = safe(methodName);
            this.methodDesc = safe(methodDesc);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MethodLooseKey other)) {
                return false;
            }
            return className.equals(other.className)
                    && methodName.equals(other.methodName)
                    && methodDesc.equals(other.methodDesc);
        }

        @Override
        public int hashCode() {
            int result = className.hashCode();
            result = 31 * result + methodName.hashCode();
            result = 31 * result + methodDesc.hashCode();
            return result;
        }
    }

    private static final class CallSiteRow {
        private final int csId;
        private final String callSiteKey;
        private final long nodeId;
        private final String callerClass;
        private final String callerMethod;
        private final String callerDesc;
        private final String calleeClass;
        private final String calleeMethod;
        private final String calleeDesc;
        private final int opCode;
        private final int lineNumber;
        private final int callIndex;
        private final int jarId;

        private CallSiteRow(int csId,
                            String callSiteKey,
                            long nodeId,
                            String callerClass,
                            String callerMethod,
                            String callerDesc,
                            String calleeClass,
                            String calleeMethod,
                            String calleeDesc,
                            int opCode,
                            int lineNumber,
                            int callIndex,
                            int jarId) {
            this.csId = csId;
            this.callSiteKey = safe(callSiteKey);
            this.nodeId = nodeId;
            this.callerClass = safe(callerClass);
            this.callerMethod = safe(callerMethod);
            this.callerDesc = safe(callerDesc);
            this.calleeClass = safe(calleeClass);
            this.calleeMethod = safe(calleeMethod);
            this.calleeDesc = safe(calleeDesc);
            this.opCode = opCode;
            this.lineNumber = lineNumber;
            this.callIndex = callIndex;
            this.jarId = jarId;
        }
    }
}
