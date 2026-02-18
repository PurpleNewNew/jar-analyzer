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
    public static final String PROFILE_DEFAULT = "default";
    public static final String PROFILE_LONG_CHAIN = "long-chain";

    private final String profile;
    private final int maxRows;
    private final int maxMs;
    private final int maxHops;
    private final int maxPaths;
    private final int expandBudget;
    private final int pathBudget;
    private final int timeoutCheckInterval;

    public QueryOptions(int maxRows, int maxMs, int maxHops, int maxPaths) {
        this(maxRows, maxMs, maxHops, maxPaths, PROFILE_DEFAULT, 0, 0, 0);
    }

    public QueryOptions(int maxRows,
                        int maxMs,
                        int maxHops,
                        int maxPaths,
                        String profile,
                        int expandBudget,
                        int pathBudget,
                        int timeoutCheckInterval) {
        this.profile = normalizeProfile(profile);
        boolean longChain = isLongChainProfile(this.profile);
        this.maxRows = clamp(maxRows, 1, 10000, 500);
        this.maxMs = clamp(maxMs, 100, 180000, longChain ? 20000 : 15000);
        this.maxHops = clamp(maxHops, 1, longChain ? 256 : 64, longChain ? 128 : 8);
        this.expandBudget = clamp(expandBudget, 1000, 20_000_000, longChain ? 150_000 : 300_000);
        this.pathBudget = clamp(pathBudget, 16, 1_000_000, longChain ? 2000 : 5000);
        int clampedMaxPaths = clamp(maxPaths, 1, 5000, 500);
        this.maxPaths = Math.min(clampedMaxPaths, this.pathBudget);
        this.timeoutCheckInterval = clamp(timeoutCheckInterval, 8, 4096, longChain ? 64 : 128);
    }

    public static QueryOptions defaults() {
        return new QueryOptions(500, 15000, 8, 500, PROFILE_DEFAULT, 0, 0, 0);
    }

    public static QueryOptions fromMap(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return defaults();
        }
        String profile = resolveProfile(raw);
        return new QueryOptions(
                toInt(raw.get("maxRows"), 0),
                toInt(raw.get("maxMs"), 0),
                toInt(raw.get("maxHops"), 0),
                toInt(raw.get("maxPaths"), 0),
                profile,
                toInt(raw.get("expandBudget"), 0),
                toInt(raw.get("pathBudget"), 0),
                toInt(raw.get("timeoutCheckInterval"), 0)
        );
    }

    public String getProfile() {
        return profile;
    }

    public boolean isLongChainProfile() {
        return isLongChainProfile(profile);
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

    private static String resolveProfile(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return PROFILE_DEFAULT;
        }
        Object profile = raw.get("profile");
        String fromProfile = normalizeProfile(profile == null ? null : String.valueOf(profile));
        if (!PROFILE_DEFAULT.equals(fromProfile)) {
            return fromProfile;
        }
        Object longChain = raw.get("longChain");
        if (longChain instanceof Boolean b && b) {
            return PROFILE_LONG_CHAIN;
        }
        if (longChain != null && "true".equalsIgnoreCase(String.valueOf(longChain).trim())) {
            return PROFILE_LONG_CHAIN;
        }
        return PROFILE_DEFAULT;
    }

    private static boolean isLongChainProfile(String profile) {
        return PROFILE_LONG_CHAIN.equals(normalizeProfile(profile));
    }

    private static String normalizeProfile(String raw) {
        if (raw == null) {
            return PROFILE_DEFAULT;
        }
        String value = raw.trim().toLowerCase();
        if (value.isEmpty()) {
            return PROFILE_DEFAULT;
        }
        if ("long".equals(value) || "long_chain".equals(value) || "long-chain".equals(value)) {
            return PROFILE_LONG_CHAIN;
        }
        return PROFILE_DEFAULT;
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
