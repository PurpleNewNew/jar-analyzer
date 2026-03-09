/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectMetadataSnapshotStore;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Helpers for active-project runtime state and persisted project build snapshots.
 */
public final class ProjectStateUtil {
    private ProjectStateUtil() {
    }

    public static long runtimeSnapshot() {
        return ProjectRuntimeContext.stateVersion();
    }

    public static long projectBuildSnapshot(String projectKey) {
        return DatabaseManager.getProjectBuildSeq(projectKey);
    }

    public static boolean isRuntimeStale(String projectKey, long snapshot) {
        String normalized = ActiveProjectContext.normalizeProjectKey(projectKey);
        if (normalized.isBlank()) {
            return true;
        }
        if (ActiveProjectContext.isProjectMutationInProgress(normalized)
                || ProjectMetadataSnapshotStore.getInstance().isUnavailable(normalized)) {
            return true;
        }
        String activeProjectKey = ActiveProjectContext.normalizeProjectKey(
                ActiveProjectContext.getPublishedActiveProjectKey());
        if (!normalized.equals(activeProjectKey)) {
            return true;
        }
        return snapshot != runtimeSnapshot();
    }

    public static boolean isProjectBuildStale(String projectKey, long snapshot) {
        if (snapshot <= 0L) {
            return false;
        }
        if (DatabaseManager.isBuilding(projectKey)
                || ProjectMetadataSnapshotStore.getInstance().isUnavailable(projectKey)) {
            return true;
        }
        long current = projectBuildSnapshot(projectKey);
        if (current <= 0L) {
            return true;
        }
        return snapshot != current;
    }

    public static void ensureFresh(AtomicLong lastSeen,
                                   Object lock,
                                   Runnable clearAction) {
        if (lastSeen == null || lock == null || clearAction == null) {
            return;
        }
        long snapshot = runtimeSnapshot();
        if (snapshot == lastSeen.get()) {
            return;
        }
        synchronized (lock) {
            long current = runtimeSnapshot();
            if (current == lastSeen.get()) {
                return;
            }
            clearAction.run();
            lastSeen.set(current);
        }
    }
}
