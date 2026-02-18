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

public final class SQLiteDriver {
    private static volatile boolean loaded;

    private SQLiteDriver() {
    }

    public static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (SQLiteDriver.class) {
            if (loaded) {
                return;
            }
            try {
                Class.forName("org.sqlite.JDBC");
                loaded = true;
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("sqlite driver not found", ex);
            }
        }
    }
}

