/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.pta;

final class PtaSolverConfig {
    private static final String PROP_CONTEXT_DEPTH = "jar.analyzer.pta.context.depth";
    private static final String PROP_MAX_TARGETS = "jar.analyzer.pta.max.targets.per.call";
    private static final String PROP_MAX_CONTEXT_METHODS = "jar.analyzer.pta.max.context.methods";
    private static final String PROP_INCREMENTAL = "jar.analyzer.pta.incremental";

    private final int contextDepth;
    private final int maxTargetsPerCall;
    private final int maxContextMethods;
    private final boolean incremental;

    private PtaSolverConfig(int contextDepth,
                            int maxTargetsPerCall,
                            int maxContextMethods,
                            boolean incremental) {
        this.contextDepth = contextDepth;
        this.maxTargetsPerCall = maxTargetsPerCall;
        this.maxContextMethods = maxContextMethods;
        this.incremental = incremental;
    }

    static PtaSolverConfig fromSystemProperties() {
        int depth = readInt(PROP_CONTEXT_DEPTH, 1, 0, 4);
        int maxTargets = readInt(PROP_MAX_TARGETS, 256, 16, 4096);
        int maxContexts = readInt(PROP_MAX_CONTEXT_METHODS, 50000, 512, 200000);
        boolean incremental = readBoolean(PROP_INCREMENTAL, true);
        return new PtaSolverConfig(depth, maxTargets, maxContexts, incremental);
    }

    int getContextDepth() {
        return contextDepth;
    }

    int getMaxTargetsPerCall() {
        return maxTargetsPerCall;
    }

    int getMaxContextMethods() {
        return maxContextMethods;
    }

    boolean isIncremental() {
        return incremental;
    }

    private static int readInt(String key, int def, int min, int max) {
        String raw = System.getProperty(key);
        if (raw == null || raw.trim().isEmpty()) {
            return def;
        }
        try {
            int val = Integer.parseInt(raw.trim());
            if (val < min) {
                return min;
            }
            if (val > max) {
                return max;
            }
            return val;
        } catch (Exception ignored) {
            return def;
        }
    }

    private static boolean readBoolean(String key, boolean def) {
        String raw = System.getProperty(key);
        if (raw == null || raw.trim().isEmpty()) {
            return def;
        }
        return !"false".equalsIgnoreCase(raw.trim());
    }
}
