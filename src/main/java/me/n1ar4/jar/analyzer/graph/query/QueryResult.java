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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class QueryResult {
    private final List<String> columns;
    private final List<List<Object>> rows;
    private final List<String> warnings;
    private final boolean truncated;

    public QueryResult(List<String> columns,
                       List<List<Object>> rows,
                       List<String> warnings,
                       boolean truncated) {
        this.columns = columns == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(columns));
        this.rows = rows == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(rows));
        this.warnings = warnings == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(warnings));
        this.truncated = truncated;
    }

    public static QueryResult empty() {
        return new QueryResult(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false);
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<List<Object>> getRows() {
        return rows;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public boolean isTruncated() {
        return truncated;
    }
}
