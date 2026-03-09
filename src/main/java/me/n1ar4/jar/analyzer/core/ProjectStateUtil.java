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
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.project.ProjectRuntimeState;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectMetadataSnapshotStore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
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

    public static ProjectRuntimeState runtimeState() {
        return ProjectRuntimeContext.getState();
    }

    public static String runtimeProjectKey() {
        return runtimeState().projectKey();
    }

    public static long runtimeBuildSeq() {
        return runtimeState().buildSeq();
    }

    public static ProjectModel runtimeProjectModel() {
        return runtimeState().projectModel();
    }

    public static String runtimeCacheKey() {
        ProjectRuntimeState state = runtimeState();
        if (state == null) {
            return "";
        }
        String raw = state.cacheKey();
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String prefix = state.projectKey();
        String digest = digest(raw);
        if (prefix == null || prefix.isBlank()) {
            return digest;
        }
        return prefix + "|" + digest;
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

    private static String digest(String raw) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes, 0, 8);
        } catch (Exception ex) {
            return Integer.toHexString(raw.hashCode());
        }
    }
}
