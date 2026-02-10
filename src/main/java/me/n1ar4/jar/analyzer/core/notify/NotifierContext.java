/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.notify;

import java.util.concurrent.atomic.AtomicReference;

public final class NotifierContext {
    private static final Notifier NOOP = new Notifier() {
        @Override
        public void info(String title, String message) {
        }

        @Override
        public void warn(String title, String message) {
        }

        @Override
        public void error(String title, String message) {
        }
    };

    private static final AtomicReference<Notifier> REF = new AtomicReference<>(NOOP);

    private NotifierContext() {
    }

    public static Notifier get() {
        Notifier n = REF.get();
        return n == null ? NOOP : n;
    }

    public static void set(Notifier notifier) {
        REF.set(notifier == null ? NOOP : notifier);
    }

    public static void reset() {
        REF.set(NOOP);
    }
}

