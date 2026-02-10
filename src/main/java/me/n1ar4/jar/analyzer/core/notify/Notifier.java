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

/**
 * Headless-friendly notification abstraction.
 * <p>
 * GUI may implement this with Swing dialogs; server/cli can use no-op or logging.
 */
public interface Notifier {
    void info(String title, String message);

    void warn(String title, String message);

    void error(String title, String message);
}

