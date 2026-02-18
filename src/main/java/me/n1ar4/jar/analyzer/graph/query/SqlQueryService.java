/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.query;

import me.n1ar4.jar.analyzer.core.SQLiteDriver;
import me.n1ar4.jar.analyzer.starter.Const;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SqlQueryService implements QueryService {
    private static final String JDBC_URL = "jdbc:sqlite:" + Const.dbFile;

    @Override
    public QueryResult execute(String query, Map<String, Object> params, QueryOptions options) throws Exception {
        SQLiteDriver.ensureLoaded();
        String sql = query == null ? "" : query.trim();
        if (sql.isEmpty()) {
            return QueryResult.empty();
        }
        String head = sql.split("\\s+", 2)[0].toLowerCase();
        if (!("select".equals(head) || "with".equals(head) || "explain".equals(head) || "pragma".equals(head))) {
            throw new IllegalArgumentException("sql_read_only");
        }

        List<String> columns = new ArrayList<>();
        List<List<Object>> rows = new ArrayList<>();
        boolean truncated = false;
        int limit = options == null ? QueryOptions.defaults().getMaxRows() : options.getMaxRows();
        int maxMs = options == null ? QueryOptions.defaults().getMaxMs() : options.getMaxMs();

        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(Math.max(1, maxMs / 1000));
            try (ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(meta.getColumnLabel(i));
                }
                while (rs.next()) {
                    List<Object> row = new ArrayList<>(columnCount);
                    for (int i = 1; i <= columnCount; i++) {
                        row.add(rs.getObject(i));
                    }
                    rows.add(row);
                    if (rows.size() >= limit) {
                        truncated = rs.next();
                        break;
                    }
                }
            }
        }

        return new QueryResult(columns, rows, List.of(), truncated);
    }
}
