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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GraphStore {
    private static final Logger logger = LogManager.getLogger();
    private static final String JDBC_URL = "jdbc:sqlite:" + Const.dbFile;

    public GraphSnapshot loadSnapshot() {
        SQLiteDriver.ensureLoaded();
        Map<Long, GraphNode> nodeMap = new HashMap<>();
        Map<Long, List<GraphEdge>> outgoing = new HashMap<>();
        Map<Long, List<GraphEdge>> incoming = new HashMap<>();
        Map<String, List<GraphNode>> labelIndex = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement()) {
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
                    GraphEdge edge = new GraphEdge(
                            rs.getLong(1),
                            rs.getLong(2),
                            rs.getLong(3),
                            rs.getString(4),
                            rs.getString(5),
                            rs.getString(6),
                            rs.getInt(7)
                    );
                    outgoing.computeIfAbsent(edge.getSrcId(), key -> new ArrayList<>()).add(edge);
                    incoming.computeIfAbsent(edge.getDstId(), key -> new ArrayList<>()).add(edge);
                }
            }
        } catch (Exception ex) {
            logger.warn("load graph snapshot fail: {}", ex.toString());
        }
        return new GraphSnapshot(nodeMap, outgoing, incoming, labelIndex);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
