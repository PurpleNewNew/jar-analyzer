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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Helpers for build-seq scoped caches.
 * <p>
 * {@link DatabaseManager#getBuildSeq()} changes whenever a new database build starts.
 * Analysis-time caches should clear themselves when buildSeq changes to avoid
 * cross-build contamination in the same JVM.
 */
public final class BuildSeqUtil {
    private BuildSeqUtil() {
    }

    public static long snapshot() {
        return DatabaseManager.getBuildSeq();
    }

    public static boolean isStale(long snapshot) {
        return snapshot != DatabaseManager.getBuildSeq();
    }

    public static void ensureFresh(AtomicLong lastSeen,
                                   Object lock,
                                   Runnable clearAction) {
        if (lastSeen == null || lock == null || clearAction == null) {
            return;
        }
        long buildSeq = DatabaseManager.getBuildSeq();
        if (buildSeq == lastSeen.get()) {
            return;
        }
        synchronized (lock) {
            long current = DatabaseManager.getBuildSeq();
            if (current == lastSeen.get()) {
                return;
            }
            clearAction.run();
            lastSeen.set(current);
        }
    }
}

