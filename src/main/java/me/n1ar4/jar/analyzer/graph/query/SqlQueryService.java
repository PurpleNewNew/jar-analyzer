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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;

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
        Map<String, Object> safeParams = params == null ? Collections.emptyMap() : params;
        ParsedSql parsedSql = parseNamedParams(sql);
        if (!safeParams.isEmpty() && parsedSql.paramOrder().isEmpty()) {
            throw new IllegalArgumentException("sql_param_placeholder_missing");
        }

        List<String> columns = new ArrayList<>();
        List<List<Object>> rows = new ArrayList<>();
        boolean truncated = false;
        int limit = options == null ? QueryOptions.defaults().getMaxRows() : options.getMaxRows();
        int maxMs = options == null ? QueryOptions.defaults().getMaxMs() : options.getMaxMs();

        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             PreparedStatement stmt = conn.prepareStatement(parsedSql.sql())) {
            stmt.setQueryTimeout(Math.max(1, maxMs / 1000));
            bindNamedParams(stmt, parsedSql.paramOrder(), safeParams);
            try (ResultSet rs = stmt.executeQuery()) {
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

    private static void bindNamedParams(PreparedStatement stmt, List<String> paramOrder, Map<String, Object> params)
            throws Exception {
        if (paramOrder == null || paramOrder.isEmpty()) {
            return;
        }
        for (int i = 0; i < paramOrder.size(); i++) {
            String name = paramOrder.get(i);
            if (!params.containsKey(name)) {
                throw new IllegalArgumentException("sql_param_missing:" + name);
            }
            stmt.setObject(i + 1, params.get(name));
        }
    }

    private static ParsedSql parseNamedParams(String sql) {
        if (sql == null || sql.isEmpty()) {
            return new ParsedSql("", List.of());
        }
        List<String> names = new ArrayList<>();
        StringBuilder out = new StringBuilder(sql.length());
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            char next = (i + 1) < sql.length() ? sql.charAt(i + 1) : '\0';

            if (inLineComment) {
                out.append(ch);
                if (ch == '\n' || ch == '\r') {
                    inLineComment = false;
                }
                continue;
            }
            if (inBlockComment) {
                out.append(ch);
                if (ch == '*' && next == '/') {
                    out.append(next);
                    i++;
                    inBlockComment = false;
                }
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote) {
                if (ch == '-' && next == '-') {
                    out.append(ch).append(next);
                    i++;
                    inLineComment = true;
                    continue;
                }
                if (ch == '/' && next == '*') {
                    out.append(ch).append(next);
                    i++;
                    inBlockComment = true;
                    continue;
                }
            }
            if (ch == '\'' && !inDoubleQuote) {
                out.append(ch);
                if (inSingleQuote && next == '\'') {
                    out.append(next);
                    i++;
                } else {
                    inSingleQuote = !inSingleQuote;
                }
                continue;
            }
            if (ch == '"' && !inSingleQuote) {
                out.append(ch);
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote && ch == ':' && isParamStart(next)) {
                int j = i + 1;
                while (j < sql.length() && isParamPart(sql.charAt(j))) {
                    j++;
                }
                String name = sql.substring(i + 1, j);
                names.add(name);
                out.append('?');
                i = j - 1;
                continue;
            }
            out.append(ch);
        }
        return new ParsedSql(out.toString(), names);
    }

    private static boolean isParamStart(char ch) {
        return (ch >= 'a' && ch <= 'z')
                || (ch >= 'A' && ch <= 'Z')
                || ch == '_';
    }

    private static boolean isParamPart(char ch) {
        return isParamStart(ch) || (ch >= '0' && ch <= '9');
    }

    private record ParsedSql(String sql, List<String> paramOrder) {
    }
}
