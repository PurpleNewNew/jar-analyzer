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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Headless-safe switch for SQL logging.
 * <p>
 * GUI can toggle this flag; server/cli/tests can control it via system property.
 */
public final class SqlLogConfig {
    public static final String PROP_LOG_ALL_SQL = "jar.analyzer.sql.logAll";
    private static final AtomicBoolean ENABLED = new AtomicBoolean(Boolean.getBoolean(PROP_LOG_ALL_SQL));

    private SqlLogConfig() {
    }

    public static boolean isEnabled() {
        return ENABLED.get();
    }

    public static void setEnabled(boolean enabled) {
        ENABLED.set(enabled);
        System.setProperty(PROP_LOG_ALL_SQL, String.valueOf(enabled));
    }
}

