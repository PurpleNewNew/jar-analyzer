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

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public final class ActiveProjectContext {
    private static final String DEFAULT_PROJECT_KEY = "default";
    private static final AtomicLong PROJECT_EPOCH = new AtomicLong(1L);
    private static final ThreadLocal<String> PROJECT_OVERRIDE = new ThreadLocal<>();

    private static volatile String activeProjectKey = DEFAULT_PROJECT_KEY;
    private static volatile String activeProjectAlias = "default";

    private ActiveProjectContext() {
    }

    public static String getActiveProjectKey() {
        String override = PROJECT_OVERRIDE.get();
        if (override != null && !override.isBlank()) {
            return override;
        }
        String key = activeProjectKey;
        if (key == null || key.isBlank()) {
            return DEFAULT_PROJECT_KEY;
        }
        return key;
    }

    public static String getActiveProjectAlias() {
        String alias = activeProjectAlias;
        return alias == null ? "" : alias;
    }

    public static void setActiveProject(String projectKey, String alias) {
        String normalized = normalizeProjectKey(projectKey);
        String normalizedAlias = alias == null || alias.isBlank() ? normalized : alias.trim();
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

    public static String normalizeProjectKey(String projectKey) {
        if (projectKey == null || projectKey.isBlank()) {
            return DEFAULT_PROJECT_KEY;
        }
        return projectKey.trim();
    }

    public static <T> T withProject(String projectKey, Supplier<T> supplier) {
        if (supplier == null) {
            return null;
        }
        String previous = PROJECT_OVERRIDE.get();
        String normalized = normalizeProjectKey(projectKey);
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
        if (requestedProjectKey == null || requestedProjectKey.isBlank()) {
            return getActiveProjectKey();
        }
        return normalizeProjectKey(requestedProjectKey);
    }
}
