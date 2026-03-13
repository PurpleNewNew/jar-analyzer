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

import me.n1ar4.jar.analyzer.core.runtime.JdkArchiveResolver;

public record ProjectRegistryEntry(
        String projectKey,
        ProjectType type,
        String alias,
        String inputPath,
        String runtimePath,
        boolean resolveNestedJars,
        String jdkModules,
        long createdAt,
        long updatedAt
) {
    public ProjectRegistryEntry {
        projectKey = safe(projectKey);
        type = type == null ? ProjectType.PERSISTENT : type;
        alias = safe(alias);
        inputPath = safe(inputPath);
        runtimePath = safe(runtimePath);
        jdkModules = normalizeJdkModules(jdkModules);
    }

    public ProjectRegistryEntry(String projectKey,
                                String alias,
                                String inputPath,
                                String runtimePath,
                                boolean resolveNestedJars,
                                long createdAt,
                                long updatedAt) {
        this(projectKey, ProjectType.PERSISTENT, alias, inputPath, runtimePath, resolveNestedJars,
                JdkArchiveResolver.DEFAULT_MODULE_POLICY, createdAt, updatedAt);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeJdkModules(String value) {
        return JdkArchiveResolver.normalizePolicy(value);
    }
}
