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

import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;

import java.nio.file.Path;

public final class ProjectMetadataSnapshotStoreTestHook {
    private ProjectMetadataSnapshotStoreTestHook() {
    }

    public static void write(String projectKey, ProjectRuntimeSnapshot snapshot) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        Path home = Neo4jProjectStore.getInstance().resolveProjectHome(normalized);
        ProjectMetadataSnapshotStore.getInstance().writeToHome(home, home, snapshot);
    }
}
