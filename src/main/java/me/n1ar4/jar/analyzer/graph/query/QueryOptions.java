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
    private final int expandBudget;
    private final int pathBudget;
    private final int timeoutCheckInterval;
    private final boolean maxMsExplicit;
    private final boolean expandBudgetExplicit;
    private final boolean pathBudgetExplicit;
    private final boolean legacyLongChainDefaults;

    public QueryOptions(int maxRows, int maxMs, int maxHops, int maxPaths) {
        this(maxRows, maxMs, maxHops, maxPaths, 0, 0, 0, false, true, false, false);
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
                false,
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
                         boolean legacyLongChainDefaults,
                         boolean maxMsExplicit,
                         boolean expandBudgetExplicit,
                         boolean pathBudgetExplicit) {
        int resolvedMaxHops = clamp(maxHops, 1, 256, legacyLongChainDefaults ? 128 : 8);
        int resolvedMaxPaths = clamp(maxPaths, 1, 5000, legacyLongChainDefaults ? 1000 : 500);
        this.maxRows = clamp(maxRows, 1, 10000, 500);
        this.maxMs = clamp(maxMs, 100, 180000, defaultMaxMs(legacyLongChainDefaults, resolvedMaxHops));
        this.maxHops = resolvedMaxHops;
        this.expandBudget = clamp(expandBudget, 1000, 20_000_000, defaultExpandBudget(legacyLongChainDefaults, resolvedMaxHops));
        this.pathBudget = clamp(pathBudget, 16, 1_000_000, defaultPathBudget(legacyLongChainDefaults, resolvedMaxHops, resolvedMaxPaths));
        this.maxPaths = Math.min(resolvedMaxPaths, this.pathBudget);
        this.timeoutCheckInterval = clamp(timeoutCheckInterval, 8, 4096, 128);
        this.maxMsExplicit = maxMsExplicit;
        this.expandBudgetExplicit = expandBudgetExplicit;
        this.pathBudgetExplicit = pathBudgetExplicit;
        this.legacyLongChainDefaults = legacyLongChainDefaults;
    }

    public static QueryOptions defaults() {
        return new QueryOptions(500, 15000, 8, 500, 0, 0, 0, false, false, false, false);
    }

    public static QueryOptions fromMap(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return defaults();
        }
        boolean legacyLongChainDefaults = resolveLegacyLongChainDefaults(raw);
        return new QueryOptions(
                toInt(raw.get("maxRows"), 0),
                toInt(raw.get("maxMs"), 0),
                toInt(raw.get("maxHops"), 0),
                toInt(raw.get("maxPaths"), 0),
                toInt(raw.get("expandBudget"), 0),
                toInt(raw.get("pathBudget"), 0),
                toInt(raw.get("timeoutCheckInterval"), 0),
                legacyLongChainDefaults,
                hasPositiveInt(raw, "maxMs"),
                hasPositiveInt(raw, "expandBudget"),
                hasPositiveInt(raw, "pathBudget")
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
                : Math.max(maxMs, defaultMaxMs(legacyLongChainDefaults, resolveBudgetMaxHops(maxHopsHint)));
        if (timeoutMsHint != null && timeoutMsHint > 0) {
            return Math.min(derived, clamp(timeoutMsHint, 100, 180000, derived));
        }
        return derived;
    }

    public int effectiveExpandBudget(int maxHopsHint) {
        if (expandBudgetExplicit) {
            return expandBudget;
        }
        return Math.max(expandBudget, defaultExpandBudget(legacyLongChainDefaults, resolveBudgetMaxHops(maxHopsHint)));
    }

    public int effectivePathBudget(int maxHopsHint, int maxPathsHint) {
        if (pathBudgetExplicit) {
            return pathBudget;
        }
        int resolvedMaxHops = resolveBudgetMaxHops(maxHopsHint);
        int resolvedMaxPaths = resolveBudgetMaxPaths(maxPathsHint);
        return Math.max(pathBudget, defaultPathBudget(legacyLongChainDefaults, resolvedMaxHops, resolvedMaxPaths));
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

    private static boolean resolveLegacyLongChainDefaults(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return false;
        }
        Object profile = raw.get("profile");
        if (profile != null) {
            String value = String.valueOf(profile).trim().toLowerCase();
            if ("long".equals(value) || "long_chain".equals(value) || "long-chain".equals(value)) {
                return true;
            }
        }
        Object longChain = raw.get("longChain");
        if (longChain instanceof Boolean b) {
            return b;
        }
        return longChain != null && "true".equalsIgnoreCase(String.valueOf(longChain).trim());
    }

    private static int defaultMaxMs(boolean boostedDefaults, int maxHops) {
        if (!boostedDefaults && maxHops <= 32) {
            return 15000;
        }
        return Math.max(20000, Math.min(60000, maxHops * 250));
    }

    private static int defaultExpandBudget(boolean boostedDefaults, int maxHops) {
        if (!boostedDefaults && maxHops <= 32) {
            return 300_000;
        }
        return Math.max(600_000, Math.min(4_000_000, maxHops * 12_000));
    }

    private static int defaultPathBudget(boolean boostedDefaults, int maxHops, int maxPaths) {
        int byHops = (!boostedDefaults && maxHops <= 32)
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

    private static boolean hasPositiveInt(Map<String, Object> raw, String key) {
        if (raw == null || key == null || key.isBlank() || !raw.containsKey(key)) {
            return false;
        }
        return toInt(raw.get(key), 0) > 0;
    }
}
