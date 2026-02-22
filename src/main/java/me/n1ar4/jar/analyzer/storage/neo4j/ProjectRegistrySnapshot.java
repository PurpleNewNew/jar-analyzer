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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record ProjectRegistrySnapshot(
        List<ProjectRegistryEntry> projects,
        String activeProjectKey,
        String activeProjectAlias
) {
    public ProjectRegistrySnapshot {
        projects = projects == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(projects));
        activeProjectKey = safe(activeProjectKey);
        activeProjectAlias = safe(activeProjectAlias);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
