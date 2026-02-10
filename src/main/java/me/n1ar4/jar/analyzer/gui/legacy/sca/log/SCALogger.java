/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.legacy.sca.log;

import me.n1ar4.jar.analyzer.gui.util.UiExecutor;

import javax.swing.*;

public class SCALogger {
    public static SCALogger logger;
    private final JTextArea logArea;

    public SCALogger(JTextArea area) {
        this.logArea = area;
    }

    public void print(String s) {
        appendText(s);
    }

    private void log(String level, String msg) {
        String logInfo = "[" + level + "] " + msg + "\n";
        appendText(logInfo);
    }

    private void appendText(String text) {
        if (text == null || logArea == null) {
            return;
        }
        UiExecutor.runOnEdt(() -> {
            logArea.append(text);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void info(String msg) {
        log("INFO", msg);
    }

    public void warn(String msg) {
        log("WARN", msg);
    }

    public void error(String msg) {
        log("ERROR", msg);
    }
}
