/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.leak;

import java.util.function.Supplier;

/**
 * Per-thread leak scan options.
 * <p>
 * This replaces direct GUI state reads inside rule engines.
 */
public final class LeakContext {
    private static final ThreadLocal<Boolean> DETECT_BASE64 = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private LeakContext() {
    }

    public static boolean isDetectBase64Enabled() {
        Boolean v = DETECT_BASE64.get();
        return v != null && v;
    }

    public static void setDetectBase64Enabled(boolean enabled) {
        DETECT_BASE64.set(enabled);
    }

    public static void clear() {
        DETECT_BASE64.remove();
    }

    public static void runWithDetectBase64(boolean enabled, Runnable action) {
        if (action == null) {
            return;
        }
        Boolean prev = DETECT_BASE64.get();
        DETECT_BASE64.set(enabled);
        try {
            action.run();
        } finally {
            if (prev == null) {
                DETECT_BASE64.remove();
            } else {
                DETECT_BASE64.set(prev);
            }
        }
    }

    public static <T> T withDetectBase64(boolean enabled, Supplier<T> supplier) {
        if (supplier == null) {
            return null;
        }
        Boolean prev = DETECT_BASE64.get();
        DETECT_BASE64.set(enabled);
        try {
            return supplier.get();
        } finally {
            if (prev == null) {
                DETECT_BASE64.remove();
            } else {
                DETECT_BASE64.set(prev);
            }
        }
    }
}

