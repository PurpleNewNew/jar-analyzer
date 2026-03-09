/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.storage.neo4j.procedure;

import me.n1ar4.jar.analyzer.graph.query.QueryOptions;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;

import java.util.function.Supplier;

public final class NativeJaQueryContext {
    private static final ThreadLocal<QueryOptions> OPTIONS = new ThreadLocal<>();
    private static final ThreadLocal<String> PROJECT_KEY = new ThreadLocal<>();

    private NativeJaQueryContext() {
    }

    public static <T> T with(QueryOptions options, String projectKey, Supplier<T> supplier) {
        if (supplier == null) {
            return null;
        }
        QueryOptions previousOptions = OPTIONS.get();
        String previousProjectKey = PROJECT_KEY.get();
        OPTIONS.set(options == null ? QueryOptions.defaults() : options);
        PROJECT_KEY.set(ActiveProjectContext.resolveRequestedOrActive(projectKey));
        try {
            return supplier.get();
        } finally {
            if (previousOptions == null) {
                OPTIONS.remove();
            } else {
                OPTIONS.set(previousOptions);
            }
            if (previousProjectKey == null || previousProjectKey.isBlank()) {
                PROJECT_KEY.remove();
            } else {
                PROJECT_KEY.set(previousProjectKey);
            }
        }
    }

    public static void with(QueryOptions options, String projectKey, Runnable runnable) {
        with(options, projectKey, () -> {
            if (runnable != null) {
                runnable.run();
            }
            return null;
        });
    }

    public static QueryOptions currentOptions() {
        QueryOptions options = OPTIONS.get();
        return options == null ? QueryOptions.defaults() : options;
    }

    public static String currentProjectKey() {
        String projectKey = PROJECT_KEY.get();
        if (projectKey == null || projectKey.isBlank()) {
            return ActiveProjectContext.getActiveProjectKey();
        }
        return projectKey;
    }
}
