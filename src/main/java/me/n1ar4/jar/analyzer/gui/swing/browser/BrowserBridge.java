/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.swing.browser;

import me.n1ar4.jar.analyzer.graph.query.QueryResult;

import javax.swing.JComponent;

public interface BrowserBridge {
    JComponent getComponent();

    void renderQueryResult(QueryResult result);

    void renderInfo(String message);
}

