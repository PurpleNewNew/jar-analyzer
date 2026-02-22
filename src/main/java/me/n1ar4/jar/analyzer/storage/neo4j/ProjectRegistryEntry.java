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

public record ProjectRegistryEntry(
        String projectKey,
        String alias,
        String inputPath,
        String runtimePath,
        boolean resolveNestedJars,
        long createdAt,
        long updatedAt
) {
    public ProjectRegistryEntry {
        projectKey = safe(projectKey);
        alias = safe(alias);
        inputPath = safe(inputPath);
        runtimePath = safe(runtimePath);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
