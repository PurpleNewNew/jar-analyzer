/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.storage.neo4j;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public final class ActiveProjectContext {
    private static final String TEMP_PROJECT_PREFIX = "temp-";
    private static final String TEMP_ALIAS = "temporary";
    private static final String TEMP_SESSION_ID = resolveSessionId();
    private static final String TEMP_PROJECT_KEY = TEMP_PROJECT_PREFIX + TEMP_SESSION_ID;
    private static final Object PROJECT_MUTATION_LOCK = new Object();
    private static final AtomicLong PROJECT_EPOCH = new AtomicLong(1L);
    private static final AtomicInteger PROJECT_MUTATION_DEPTH = new AtomicInteger(0);
    private static final ThreadLocal<String> PROJECT_OVERRIDE = new ThreadLocal<>();

    private static volatile String activeProjectKey = TEMP_PROJECT_KEY;
    private static volatile String activeProjectAlias = TEMP_ALIAS;

    private ActiveProjectContext() {
    }

    public static String getActiveProjectKey() {
        String override = PROJECT_OVERRIDE.get();
        if (override != null && !override.isBlank()) {
            return override;
        }
        String key = activeProjectKey;
        if (key == null || key.isBlank()) {
            return TEMP_PROJECT_KEY;
        }
        return key;
    }

    public static String getActiveProjectAlias() {
        String alias = activeProjectAlias;
        return alias == null ? "" : alias;
    }

    public static synchronized void setActiveProject(String projectKey, String alias) {
        String normalized = resolveRequestedOrTemporary(projectKey);
        String normalizedAlias = resolveAlias(normalized, alias);
        boolean changed = !normalized.equals(activeProjectKey);
        activeProjectKey = normalized;
        activeProjectAlias = normalizedAlias;
        if (changed) {
            PROJECT_EPOCH.incrementAndGet();
        }
    }

    public static long currentEpoch() {
        return PROJECT_EPOCH.get();
    }

    public static void bumpProjectEpoch() {
        PROJECT_EPOCH.incrementAndGet();
    }

    public static Object mutationLock() {
        return PROJECT_MUTATION_LOCK;
    }

    public static void beginProjectMutation() {
        PROJECT_MUTATION_DEPTH.incrementAndGet();
    }

    public static void endProjectMutation() {
        PROJECT_MUTATION_DEPTH.updateAndGet(current -> Math.max(0, current - 1));
    }

    public static boolean isProjectMutationInProgress() {
        return PROJECT_MUTATION_DEPTH.get() > 0;
    }

    public static String normalizeProjectKey(String projectKey) {
        if (projectKey == null || projectKey.isBlank()) {
            return "";
        }
        return projectKey.trim();
    }

    public static <T> T withProject(String projectKey, Supplier<T> supplier) {
        if (supplier == null) {
            return null;
        }
        String previous = PROJECT_OVERRIDE.get();
        String normalized = resolveRequestedOrActive(projectKey);
        PROJECT_OVERRIDE.set(normalized);
        try {
            return supplier.get();
        } finally {
            if (previous == null) {
                PROJECT_OVERRIDE.remove();
            } else {
                PROJECT_OVERRIDE.set(previous);
            }
        }
    }

    public static void withProject(String projectKey, Runnable runnable) {
        withProject(projectKey, () -> {
            if (runnable != null) {
                runnable.run();
            }
            return null;
        });
    }

    public static String resolveRequestedOrActive(String requestedProjectKey) {
        String normalized = normalizeProjectKey(requestedProjectKey);
        if (normalized.isBlank()) {
            return getActiveProjectKey();
        }
        return normalized;
    }

    public static String temporaryProjectKey() {
        return TEMP_PROJECT_KEY;
    }

    public static String temporaryProjectAlias() {
        return TEMP_ALIAS;
    }

    public static String temporarySessionId() {
        return TEMP_SESSION_ID;
    }

    public static boolean isTemporaryProjectKey(String projectKey) {
        String normalized = normalizeProjectKey(projectKey);
        return !normalized.isBlank() && normalized.startsWith(TEMP_PROJECT_PREFIX);
    }

    public static String extractTemporarySessionId(String projectKey) {
        String normalized = normalizeProjectKey(projectKey);
        if (!isTemporaryProjectKey(normalized)) {
            return "";
        }
        String suffix = normalized.substring(TEMP_PROJECT_PREFIX.length());
        return suffix.isBlank() ? TEMP_SESSION_ID : suffix;
    }

    private static String resolveRequestedOrTemporary(String projectKey) {
        String normalized = normalizeProjectKey(projectKey);
        return normalized.isBlank() ? TEMP_PROJECT_KEY : normalized;
    }

    private static String resolveAlias(String projectKey, String alias) {
        String normalizedAlias = alias == null ? "" : alias.trim();
        if (!normalizedAlias.isBlank()) {
            return normalizedAlias;
        }
        if (isTemporaryProjectKey(projectKey)) {
            return TEMP_ALIAS;
        }
        return projectKey;
    }

    private static String resolveSessionId() {
        String value = System.getProperty("jar.analyzer.temp.session");
        if (value != null) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        String random = UUID.randomUUID().toString().replace("-", "").toLowerCase(Locale.ROOT);
        return random.length() <= 12 ? random : random.substring(0, 12);
    }
}
