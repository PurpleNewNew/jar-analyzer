/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.utils;

import java.util.concurrent.CancellationException;

public final class InterruptUtil {
    private InterruptUtil() {
    }

    public static boolean isInterrupted(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof InterruptedException) {
                return true;
            }
            if (cur instanceof CancellationException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    /**
     * If the given throwable represents interruption/cancellation, restore the current thread's
     * interrupted flag so upper layers can react properly.
     */
    public static void restoreInterruptIfNeeded(Throwable t) {
        if (t instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return;
        }
        if (Thread.currentThread().isInterrupted()) {
            return;
        }
        if (isInterrupted(t)) {
            Thread.currentThread().interrupt();
        }
    }
}

