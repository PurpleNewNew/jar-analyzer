/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.swing;

import javax.swing.SwingUtilities;
import java.util.Arrays;

public final class SwingUiApplyGuard {
    private SwingUiApplyGuard() {
    }

    public static boolean ensureEdt(String tag, Runnable rerunOnEdt) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(rerunOnEdt);
            return false;
        }
        assert SwingUtilities.isEventDispatchThread() : safe(tag) + " must run on EDT";
        return true;
    }

    public static int fingerprint(Object... values) {
        return Arrays.deepHashCode(values);
    }

    public static final class Throttle {
        private final long minIntervalNanos;
        private long lastApplyNanos;
        private int lastFingerprint = Integer.MIN_VALUE;

        public Throttle() {
            this(40L);
        }

        public Throttle(long minIntervalMs) {
            this.minIntervalNanos = Math.max(0L, minIntervalMs) * 1_000_000L;
        }

        public boolean allow(int fingerprint) {
            long now = System.nanoTime();
            if (fingerprint == lastFingerprint && now - lastApplyNanos < minIntervalNanos) {
                return false;
            }
            lastFingerprint = fingerprint;
            lastApplyNanos = now;
            return true;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

