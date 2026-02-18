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

    private GraphProjectionBuilder() {
    }

    public static void projectCurrentBuild(long buildSeq, boolean quickMode, String callGraphMode) {
        long start = System.nanoTime();
        SQLiteDriver.ensureLoaded();
        try (Connection conn = DriverManager.getConnection(JDBC_URL)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                clearGraphTables(conn);

                MethodNodeState methods = loadMethodNodes(conn);
                CallSiteState callSites = loadCallSiteNodes(conn);

                long edgeCount = projectMethodCallEdges(conn, methods.nodeByMethodKey);
                edgeCount += projectCallSiteEdges(conn, methods, callSites);
                edgeCount += projectNextCallsiteEdges(conn, callSites);

                saveGraphMeta(conn, buildSeq, quickMode, callGraphMode);
                saveGraphStats(conn, methods.nodeByMethodKey.size(), callSites.nodeByCallSiteKey.size(), edgeCount);

                conn.commit();
                long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
                logger.info("graph projection finish: methodNodes={} callSiteNodes={} edges={} elapsedMs={}",
                        methods.nodeByMethodKey.size(),
                        callSites.nodeByCallSiteKey.size(),
                        edgeCount,
                        elapsedMs);
            } catch (Exception ex) {
                conn.rollback();
                logger.warn("graph projection rollback: {}", ex.toString());
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (Exception ex) {
            logger.warn("graph projection fail: {}", ex.toString());
        }
    }

    private static void clearGraphTables(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM graph_meta");
            stmt.execute("DELETE FROM graph_node");
            stmt.execute("DELETE FROM graph_edge");
            stmt.execute("DELETE FROM graph_label");
            stmt.execute("DELETE FROM graph_attr");
            stmt.execute("DELETE FROM graph_stats");
        }
    }

    private static MethodNodeState loadMethodNodes(Connection conn) throws Exception {
        Map<MethodKey, Long> nodeByMethodKey = new LinkedHashMap<>();
        long nodeId = 1L;
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
                if (nodeByMethodKey.containsKey(key)) {
                    continue;
                }
                nodeByMethodKey.put(key, nodeId++);
            }
        }

        try (PreparedStatement nodeInsert = conn.prepareStatement(
                "INSERT INTO graph_node(node_id, kind, jar_id, class_name, method_name, method_desc, call_site_key, line_number, call_index, props_json) VALUES(?, ?, ?, ?, ?, ?, '', -1, -1, '{}')");
             PreparedStatement labelInsert = conn.prepareStatement(
                     "INSERT INTO graph_label(node_id, label) VALUES(?, ?)")) {
            int count = 0;
            for (Map.Entry<MethodKey, Long> entry : nodeByMethodKey.entrySet()) {
                MethodKey key = entry.getKey();
                Long id = entry.getValue();
                nodeInsert.setLong(1, id);
                nodeInsert.setString(2, "method");
                nodeInsert.setInt(3, key.jarId);
                nodeInsert.setString(4, key.className);
                nodeInsert.setString(5, key.methodName);
                nodeInsert.setString(6, key.methodDesc);
                nodeInsert.addBatch();

                labelInsert.setLong(1, id);
                labelInsert.setString(2, "Method");
                labelInsert.addBatch();
                count++;
                if (count % BATCH_SIZE == 0) {
                    nodeInsert.executeBatch();
                    labelInsert.executeBatch();
                }
            }
            nodeInsert.executeBatch();
            labelInsert.executeBatch();
        }
        return new MethodNodeState(nodeByMethodKey);
    }

    private static CallSiteState loadCallSiteNodes(Connection conn) throws Exception {
        Map<String, Long> nodeByCallSiteKey = new LinkedHashMap<>();
        List<CallSiteRow> rows = new ArrayList<>();
        long nextNodeId = findMaxNodeId(conn) + 1L;
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
                Long nodeId = nodeByCallSiteKey.get(callSiteKey);
                if (nodeId == null) {
                    nodeId = nextNodeId++;
                    nodeByCallSiteKey.put(callSiteKey, nodeId);
                }
                rows.add(new CallSiteRow(
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

        try (PreparedStatement nodeInsert = conn.prepareStatement(
                "INSERT INTO graph_node(node_id, kind, jar_id, class_name, method_name, method_desc, call_site_key, line_number, call_index, props_json) VALUES(?, 'callsite', ?, ?, ?, ?, ?, ?, ?, '{}')");
             PreparedStatement labelInsert = conn.prepareStatement(
                     "INSERT INTO graph_label(node_id, label) VALUES(?, 'CallSite')")) {
            int count = 0;
            Set<Long> insertedNodeIds = new HashSet<>();
            for (CallSiteRow row : rows) {
                if (!insertedNodeIds.add(row.nodeId)) {
                    continue;
                }
                nodeInsert.setLong(1, row.nodeId);
                nodeInsert.setInt(2, row.jarId);
                nodeInsert.setString(3, row.callerClass);
                nodeInsert.setString(4, row.callerMethod);
                nodeInsert.setString(5, row.callerDesc);
                nodeInsert.setString(6, row.callSiteKey);
                nodeInsert.setInt(7, row.lineNumber);
                nodeInsert.setInt(8, row.callIndex);
                nodeInsert.addBatch();

                labelInsert.setLong(1, row.nodeId);
                labelInsert.addBatch();
                count++;
                if (count % BATCH_SIZE == 0) {
                    nodeInsert.executeBatch();
                    labelInsert.executeBatch();
                }
            }
            nodeInsert.executeBatch();
            labelInsert.executeBatch();
        }

        return new CallSiteState(nodeByCallSiteKey, rows);
    }

    private static long projectMethodCallEdges(Connection conn,
                                               Map<MethodKey, Long> methodNodeByKey) throws Exception {
        long edgeId = findMaxEdgeId(conn) + 1L;
        long total = 0L;
        Set<String> dedup = new HashSet<>();
        try (PreparedStatement select = conn.prepareStatement(
                "SELECT caller_class_name, caller_method_name, caller_method_desc, caller_jar_id, " +
                        "callee_class_name, callee_method_name, callee_method_desc, callee_jar_id, " +
                        "edge_type, edge_confidence, edge_evidence, op_code " +
                        "FROM method_call_table " +
                        "ORDER BY caller_class_name, caller_method_name, caller_method_desc, caller_jar_id, " +
                        "callee_class_name, callee_method_name, callee_method_desc, callee_jar_id, op_code");
             ResultSet rs = select.executeQuery();
             PreparedStatement insert = conn.prepareStatement(
                     "INSERT INTO graph_edge(edge_id, src_id, dst_id, rel_type, confidence, evidence, op_code, weight, props_json) VALUES(?, ?, ?, ?, ?, ?, ?, 1.0, '{}')")) {
            int batch = 0;
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
                Long src = findMethodNode(methodNodeByKey, caller);
                Long dst = findMethodNode(methodNodeByKey, callee);
                if (src == null || dst == null) {
                    continue;
                }
                String relType = GraphRelationType.fromEdgeType(rs.getString(9)).name();
                String confidence = safe(rs.getString(10));
                String evidence = safe(rs.getString(11));
                int opCode = rs.getInt(12);
                String edgeKey = src + "|" + dst + "|" + relType + "|" + opCode + "|" + confidence + "|" + evidence;
                if (!dedup.add(edgeKey)) {
                    continue;
                }
                insert.setLong(1, edgeId++);
                insert.setLong(2, src);
                insert.setLong(3, dst);
                insert.setString(4, relType);
                insert.setString(5, confidence.isEmpty() ? "low" : confidence);
                insert.setString(6, evidence);
                insert.setInt(7, opCode);
                insert.addBatch();
                total++;
                batch++;
                if (batch % BATCH_SIZE == 0) {
                    insert.executeBatch();
                }
            }
            insert.executeBatch();
        }
        return total;
    }

    private static long projectCallSiteEdges(Connection conn,
                                             MethodNodeState methods,
                                             CallSiteState callSites) throws Exception {
        long edgeId = findMaxEdgeId(conn) + 1L;
        long total = 0L;
        Set<String> dedup = new HashSet<>();
        try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO graph_edge(edge_id, src_id, dst_id, rel_type, confidence, evidence, op_code, weight, props_json) VALUES(?, ?, ?, ?, ?, ?, ?, 1.0, '{}')")) {
            int batch = 0;
            for (CallSiteRow row : callSites.callSiteRows) {
                Long methodNode = findMethodNode(methods.nodeByMethodKey,
                        new MethodKey(row.callerClass, row.callerMethod, row.callerDesc, row.jarId));
                if (methodNode == null) {
                    methodNode = findMethodNode(methods.nodeByMethodKey,
                            new MethodKey(row.callerClass, row.callerMethod, row.callerDesc, -1));
                }
                if (methodNode != null) {
                    String containsKey = methodNode + "|" + row.nodeId + "|" + GraphRelationType.CONTAINS_CALLSITE;
                    if (dedup.add(containsKey)) {
                        insert.setLong(1, edgeId++);
                        insert.setLong(2, methodNode);
                        insert.setLong(3, row.nodeId);
                        insert.setString(4, GraphRelationType.CONTAINS_CALLSITE.name());
                        insert.setString(5, "high");
                        insert.setString(6, "callsite");
                        insert.setInt(7, row.opCode);
                        insert.addBatch();
                        total++;
                        batch++;
                    }
                }

                Long calleeNode = findMethodNode(methods.nodeByMethodKey,
                        new MethodKey(row.calleeClass, row.calleeMethod, row.calleeDesc, -1));
                if (calleeNode != null) {
                    String calleeKey = row.nodeId + "|" + calleeNode + "|" + GraphRelationType.CALLSITE_TO_CALLEE;
                    if (dedup.add(calleeKey)) {
                        insert.setLong(1, edgeId++);
                        insert.setLong(2, row.nodeId);
                        insert.setLong(3, calleeNode);
                        insert.setString(4, GraphRelationType.CALLSITE_TO_CALLEE.name());
                        insert.setString(5, "medium");
                        insert.setString(6, "bytecode");
                        insert.setInt(7, row.opCode);
                        insert.addBatch();
                        total++;
                        batch++;
                    }
                }
                if (batch > 0 && batch % BATCH_SIZE == 0) {
                    insert.executeBatch();
                }
            }
            insert.executeBatch();
        }
        return total;
    }

    private static long projectNextCallsiteEdges(Connection conn,
                                                 CallSiteState callSites) throws Exception {
        long edgeId = findMaxEdgeId(conn) + 1L;
        long total = 0L;
        Map<String, List<CallSiteRow>> byMethod = new LinkedHashMap<>();
        for (CallSiteRow row : callSites.callSiteRows) {
            String key = row.callerClass + "#" + row.callerMethod + "#" + row.callerDesc + "#" + row.jarId;
            byMethod.computeIfAbsent(key, ignore -> new ArrayList<>()).add(row);
        }
        try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO graph_edge(edge_id, src_id, dst_id, rel_type, confidence, evidence, op_code, weight, props_json) VALUES(?, ?, ?, ?, 'high', 'order', -1, 1.0, '{}')")) {
            int batch = 0;
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
                    insert.setLong(1, edgeId++);
                    insert.setLong(2, cur.nodeId);
                    insert.setLong(3, next.nodeId);
                    insert.setString(4, GraphRelationType.NEXT_CALLSITE.name());
                    insert.addBatch();
                    total++;
                    batch++;
                    if (batch % BATCH_SIZE == 0) {
                        insert.executeBatch();
                    }
                }
            }
            insert.executeBatch();
        }
        return total;
    }

    private static void saveGraphMeta(Connection conn,
                                      long buildSeq,
                                      boolean quickMode,
                                      String callGraphMode) throws Exception {
        try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO graph_meta(schema_version, build_seq, built_at, options_json) VALUES(?, ?, strftime('%s','now'), ?)") ) {
            insert.setString(1, GRAPH_SCHEMA_VERSION);
            insert.setLong(2, buildSeq);
            insert.setString(3, "{\"quickMode\":" + quickMode + ",\"callGraphMode\":\"" + safe(callGraphMode) + "\"}");
            insert.executeUpdate();
        }
    }

    private static void saveGraphStats(Connection conn,
                                       int methodNodeCount,
                                       int callSiteNodeCount,
                                       long edgeCount) throws Exception {
        try (PreparedStatement insert = conn.prepareStatement(
                "INSERT OR REPLACE INTO graph_stats(metric, metric_value, updated_at) VALUES(?, ?, strftime('%s','now'))")) {
            insertMetric(insert, "method_nodes", String.valueOf(methodNodeCount));
            insertMetric(insert, "callsite_nodes", String.valueOf(callSiteNodeCount));
            insertMetric(insert, "total_nodes", String.valueOf(methodNodeCount + callSiteNodeCount));
            insertMetric(insert, "total_edges", String.valueOf(edgeCount));
            insertMetric(insert, "schema_version", GRAPH_SCHEMA_VERSION);
            insert.executeBatch();
        }
    }

    private static void insertMetric(PreparedStatement insert, String metric, String value) throws Exception {
        insert.setString(1, metric);
        insert.setString(2, value);
        insert.addBatch();
    }

    private static long findMaxNodeId(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(node_id), 0) FROM graph_node")) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0L;
    }

    private static long findMaxEdgeId(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(edge_id), 0) FROM graph_edge")) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0L;
    }

    private static Long findMethodNode(Map<MethodKey, Long> methodNodeByKey, MethodKey key) {
        Long node = methodNodeByKey.get(key);
        if (node != null) {
            return node;
        }
        for (Map.Entry<MethodKey, Long> entry : methodNodeByKey.entrySet()) {
            MethodKey mk = entry.getKey();
            if (!mk.className.equals(key.className)) {
                continue;
            }
            if (!mk.methodName.equals(key.methodName)) {
                continue;
            }
            if (!mk.methodDesc.equals(key.methodDesc)) {
                continue;
            }
            return entry.getValue();
        }
        return null;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static int normalizeInt(int value) {
        return value < 0 ? Integer.MAX_VALUE : value;
    }

    private static final class MethodNodeState {
        private final Map<MethodKey, Long> nodeByMethodKey;

        private MethodNodeState(Map<MethodKey, Long> nodeByMethodKey) {
            this.nodeByMethodKey = nodeByMethodKey;
        }
    }

    private static final class CallSiteState {
        private final Map<String, Long> nodeByCallSiteKey;
        private final List<CallSiteRow> callSiteRows;

        private CallSiteState(Map<String, Long> nodeByCallSiteKey, List<CallSiteRow> callSiteRows) {
            this.nodeByCallSiteKey = nodeByCallSiteKey;
            this.callSiteRows = callSiteRows;
        }
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
