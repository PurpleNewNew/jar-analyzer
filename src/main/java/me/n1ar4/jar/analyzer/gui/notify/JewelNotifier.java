/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.notify;

import me.n1ar4.jar.analyzer.core.notify.Notifier;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

public final class JewelNotifier implements Notifier {
    private static final Logger logger = LogManager.getLogger();

    @Override
    public void info(String title, String message) {
        logger.info(format(title, message));
    }

    @Override
    public void warn(String title, String message) {
        logger.warn(format(title, message));
    }

    @Override
    public void error(String title, String message) {
        logger.error(format(title, message));
    }

    private static String format(String title, String message) {
        String safeTitle = title == null ? "Jar Analyzer" : title.trim();
        String safeMessage = message == null ? "" : message.trim();
        if (safeTitle.isEmpty()) {
            return safeMessage;
        }
        if (safeMessage.isEmpty()) {
            return safeTitle;
        }
        return safeTitle + ": " + safeMessage;
    }
}
