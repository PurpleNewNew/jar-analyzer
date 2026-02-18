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

import java.util.Map;

public final class QueryOptions {
    private final int maxRows;
    private final int maxMs;
    private final int maxHops;
    private final int maxPaths;

    public QueryOptions(int maxRows, int maxMs, int maxHops, int maxPaths) {
        this.maxRows = clamp(maxRows, 1, 10000, 500);
        this.maxMs = clamp(maxMs, 100, 120000, 15000);
        this.maxHops = clamp(maxHops, 1, 32, 8);
        this.maxPaths = clamp(maxPaths, 1, 5000, 500);
    }

    public static QueryOptions defaults() {
        return new QueryOptions(500, 15000, 8, 500);
    }

    public static QueryOptions fromMap(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return defaults();
        }
        return new QueryOptions(
                toInt(raw.get("maxRows"), 500),
                toInt(raw.get("maxMs"), 15000),
                toInt(raw.get("maxHops"), 8),
                toInt(raw.get("maxPaths"), 500)
        );
    }

    public int getMaxRows() {
        return maxRows;
    }

    public int getMaxMs() {
        return maxMs;
    }

    public int getMaxHops() {
        return maxHops;
    }

    public int getMaxPaths() {
        return maxPaths;
    }

    private static int clamp(int value, int min, int max, int def) {
        if (value <= 0) {
            return def;
        }
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
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
        } catch (Exception ignored) {
            return def;
        }
    }
}
