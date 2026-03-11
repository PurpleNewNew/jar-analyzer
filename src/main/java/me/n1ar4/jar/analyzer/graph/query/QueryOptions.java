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
import java.util.Set;

public final class QueryOptions {
    private static final Set<String> PUBLIC_OPTION_KEYS = Set.of("maxRows");

    private final int maxRows;
    private final int maxMs;
    private final int maxHops;
    private final int maxPaths;
    private final int expandBudget;
    private final int pathBudget;
    private final int timeoutCheckInterval;
    private final boolean maxMsExplicit;
    private final boolean expandBudgetExplicit;
    private final boolean pathBudgetExplicit;

    public QueryOptions(int maxRows, int maxMs, int maxHops, int maxPaths) {
        this(maxRows, maxMs, maxHops, maxPaths, 0, 0, 0, true, false, false);
    }

    public QueryOptions(int maxRows,
                        int maxMs,
                        int maxHops,
                        int maxPaths,
                        int expandBudget,
                        int pathBudget,
                        int timeoutCheckInterval) {
        this(maxRows,
                maxMs,
                maxHops,
                maxPaths,
                expandBudget,
                pathBudget,
                timeoutCheckInterval,
                maxMs > 0,
                expandBudget > 0,
                pathBudget > 0);
    }

    private QueryOptions(int maxRows,
                         int maxMs,
                         int maxHops,
                         int maxPaths,
                         int expandBudget,
                         int pathBudget,
                         int timeoutCheckInterval,
                         boolean maxMsExplicit,
                         boolean expandBudgetExplicit,
                         boolean pathBudgetExplicit) {
        int resolvedMaxHops = clamp(maxHops, 1, 256, 8);
        int resolvedMaxPaths = clamp(maxPaths, 1, 5000, 500);
        this.maxRows = clamp(maxRows, 1, 10000, 500);
        this.maxMs = clamp(maxMs, 100, 180000, defaultMaxMs(resolvedMaxHops));
        this.maxHops = resolvedMaxHops;
        this.expandBudget = clamp(expandBudget, 1000, 20_000_000, defaultExpandBudget(resolvedMaxHops));
        this.pathBudget = clamp(pathBudget, 16, 1_000_000, defaultPathBudget(resolvedMaxHops, resolvedMaxPaths));
        this.maxPaths = Math.min(resolvedMaxPaths, this.pathBudget);
        this.timeoutCheckInterval = clamp(timeoutCheckInterval, 8, 4096, 128);
        this.maxMsExplicit = maxMsExplicit;
        this.expandBudgetExplicit = expandBudgetExplicit;
        this.pathBudgetExplicit = pathBudgetExplicit;
    }

    public static QueryOptions defaults() {
        return new QueryOptions(500, 15000, 8, 500, 0, 0, 0, false, false, false);
    }

    public static QueryOptions fromMap(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return defaults();
        }
        validatePublicOptions(raw);
        return new QueryOptions(
                toInt(raw.get("maxRows"), 0),
                defaults().getMaxMs(),
                defaults().getMaxHops(),
                defaults().getMaxPaths(),
                0,
                0,
                0,
                false,
                false,
                false
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

    public int getExpandBudget() {
        return expandBudget;
    }

    public int getPathBudget() {
        return pathBudget;
    }

    public int getTimeoutCheckInterval() {
        return timeoutCheckInterval;
    }

    public int effectiveBudgetMaxMs(int maxHopsHint, Integer timeoutMsHint) {
        int derived = maxMsExplicit
                ? maxMs
                : Math.max(maxMs, defaultMaxMs(resolveBudgetMaxHops(maxHopsHint)));
        if (timeoutMsHint != null && timeoutMsHint > 0) {
            return Math.min(derived, clamp(timeoutMsHint, 100, 180000, derived));
        }
        return derived;
    }

    public int effectiveExpandBudget(int maxHopsHint) {
        if (expandBudgetExplicit) {
            return expandBudget;
        }
        return Math.max(expandBudget, defaultExpandBudget(resolveBudgetMaxHops(maxHopsHint)));
    }

    public int effectivePathBudget(int maxHopsHint, int maxPathsHint) {
        if (pathBudgetExplicit) {
            return pathBudget;
        }
        int resolvedMaxHops = resolveBudgetMaxHops(maxHopsHint);
        int resolvedMaxPaths = resolveBudgetMaxPaths(maxPathsHint);
        return Math.max(pathBudget, defaultPathBudget(resolvedMaxHops, resolvedMaxPaths));
    }

    private int resolveBudgetMaxHops(int maxHopsHint) {
        return Math.max(maxHops, clamp(maxHopsHint, 1, 256, maxHops));
    }

    private int resolveBudgetMaxPaths(int maxPathsHint) {
        return Math.max(maxPaths, clamp(maxPathsHint, 1, 5000, maxPaths));
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

    private static int defaultMaxMs(int maxHops) {
        if (maxHops <= 32) {
            return 15000;
        }
        return Math.max(20000, Math.min(60000, maxHops * 250));
    }

    private static int defaultExpandBudget(int maxHops) {
        if (maxHops <= 32) {
            return 300_000;
        }
        return Math.max(600_000, Math.min(4_000_000, maxHops * 12_000));
    }

    private static int defaultPathBudget(int maxHops, int maxPaths) {
        int byHops = (maxHops <= 32)
                ? 5000
                : Math.max(8000, Math.min(100_000, maxHops * 96));
        int byPaths = Math.max(5000, Math.min(100_000, maxPaths * 8));
        return Math.max(byHops, byPaths);
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

    private static void validatePublicOptions(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        for (String key : raw.keySet()) {
            if (key == null || key.isBlank()) {
                continue;
            }
            if (!PUBLIC_OPTION_KEYS.contains(key)) {
                throw new IllegalArgumentException("invalid_request: unsupported query option: " + key);
            }
        }
    }
}
