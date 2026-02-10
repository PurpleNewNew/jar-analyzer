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
import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;

public final class SwingNotifier implements Notifier {
    private static final Logger logger = LogManager.getLogger();

    @Override
    public void info(String title, String message) {
        show(JOptionPane.INFORMATION_MESSAGE, title, message);
    }

    @Override
    public void warn(String title, String message) {
        show(JOptionPane.WARNING_MESSAGE, title, message);
    }

    @Override
    public void error(String title, String message) {
        show(JOptionPane.ERROR_MESSAGE, title, message);
    }

    private static void show(int type, String title, String message) {
        Runnable action = () -> {
            try {
                Component parent = MainForm.getFrame();
                JOptionPane.showMessageDialog(
                        parent,
                        message == null ? "" : message,
                        title == null ? "Jar Analyzer" : title,
                        type);
            } catch (Throwable t) {
                InterruptUtil.restoreInterruptIfNeeded(t);
                if (t instanceof Error) {
                    throw (Error) t;
                }
                logger.debug("swing notifier failed: {}", t.toString());
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }
}
