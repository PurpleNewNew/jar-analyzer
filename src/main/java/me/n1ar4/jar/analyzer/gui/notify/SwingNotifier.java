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
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

public final class SwingNotifier implements Notifier {
    private static final Logger logger = LogManager.getLogger();

    @Override
    public void info(String title, String message) {
        logger.info(format(title, message));
        showDialog(JOptionPane.INFORMATION_MESSAGE, title, message);
    }

    @Override
    public void warn(String title, String message) {
        logger.warn(format(title, message));
        showDialog(JOptionPane.WARNING_MESSAGE, title, message);
    }

    @Override
    public void error(String title, String message) {
        logger.error(format(title, message));
        showDialog(JOptionPane.ERROR_MESSAGE, title, message);
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

    private static void showDialog(int type, String title, String message) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        String safeTitle = title == null || title.trim().isEmpty() ? "Jar Analyzer" : title.trim();
        String safeMessage = message == null ? "" : message;
        Runnable task = () -> JOptionPane.showMessageDialog(
                MainForm.getFrame(),
                safeMessage,
                safeTitle,
                type
        );
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                task.run();
            } else {
                SwingUtilities.invokeLater(task);
            }
        } catch (Throwable ex) {
            logger.debug("show swing dialog failed: {}", ex.toString());
        }
    }
}
