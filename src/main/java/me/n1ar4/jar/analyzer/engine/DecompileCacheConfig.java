/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.engine;

/**
 * Decompile cache capacity resolver.
 *
 * Priority:
 * 1) System property: jar.analyzer.decompile.cache.size
 * 2) Environment variable: JAR_ANALYZER_DECOMPILE_CACHE_SIZE
 * 3) Default value
 */
public final class DecompileCacheConfig {
    public static final int DEFAULT_CAPACITY = 2000;
    private static final int MIN_CAPACITY = 10;

    private DecompileCacheConfig() {
    }

    public static int resolveCapacity() {
        Integer fromProp = parseInt(System.getProperty("jar.analyzer.decompile.cache.size"));
        if (fromProp != null) {
            return normalize(fromProp, DEFAULT_CAPACITY);
        }
        Integer fromEnv = parseInt(System.getenv("JAR_ANALYZER_DECOMPILE_CACHE_SIZE"));
        if (fromEnv != null) {
            return normalize(fromEnv, DEFAULT_CAPACITY);
        }
        return DEFAULT_CAPACITY;
    }

    public static Integer parseOptional(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        Integer parsed = parseInt(trimmed);
        if (parsed == null) {
            return null;
        }
        return normalize(parsed, DEFAULT_CAPACITY);
    }

    public static int normalize(int value, int fallback) {
        if (value < MIN_CAPACITY) {
            return fallback;
        }
        return value;
    }

    private static Integer parseInt(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
