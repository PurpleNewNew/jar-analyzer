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

import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jGraphSnapshotLoader;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Helpers for build-seq scoped caches.
 * <p>
 * {@link DatabaseManager#getBuildSeq()} changes whenever a new database build starts.
 * Analysis-time caches should clear themselves when buildSeq changes to avoid
 * cross-build contamination in the same JVM.
 */
public final class BuildSeqUtil {
    private static final Neo4jGraphSnapshotLoader GRAPH_SNAPSHOT_LOADER = new Neo4jGraphSnapshotLoader();

    private BuildSeqUtil() {
    }

    public static long snapshot() {
        return compose(DatabaseManager.getBuildSeq(), ActiveProjectContext.currentEpoch());
    }

    public static boolean isStale(long snapshot) {
        return snapshot != compose(DatabaseManager.getBuildSeq(), ActiveProjectContext.currentEpoch());
    }

    public static long projectSnapshot(String projectKey) {
        return GRAPH_SNAPSHOT_LOADER.currentBuildSeq(projectKey);
    }

    public static boolean isProjectStale(String projectKey, long snapshot) {
        if (snapshot <= 0L) {
            return false;
        }
        long current = projectSnapshot(projectKey);
        if (current <= 0L) {
            return false;
        }
        return snapshot != current;
    }

    public static void ensureFresh(AtomicLong lastSeen,
                                   Object lock,
                                   Runnable clearAction) {
        if (lastSeen == null || lock == null || clearAction == null) {
            return;
        }
        long buildSeq = snapshot();
        if (buildSeq == lastSeen.get()) {
            return;
        }
        synchronized (lock) {
            long current = snapshot();
            if (current == lastSeen.get()) {
                return;
            }
            clearAction.run();
            lastSeen.set(current);
        }
    }

    private static long compose(long buildSeq, long projectEpoch) {
        long buildPart = buildSeq & 0xFFFF_FFFFL;
        long projectPart = projectEpoch & 0xFFFF_FFFFL;
        return (projectPart << 32) | buildPart;
    }
}
