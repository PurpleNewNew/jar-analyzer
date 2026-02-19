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
import me.n1ar4.jar.analyzer.meta.CompatibilityCode;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;

@CompatibilityCode(
        primary = "JCEF-backed BrowserBridge rendering",
        reason = "Noop bridge retained as compatibility fallback when JCEF integration is unavailable in runtime build"
)
public final class NoopBrowserBridge implements BrowserBridge {
    private final JPanel root = new JPanel(new BorderLayout(6, 6));
    private final JTextArea viewArea = new JTextArea();

    public NoopBrowserBridge() {
        JLabel title = new JLabel("BrowserBridge (JCEF placeholder)");
        title.setBorder(BorderFactory.createEmptyBorder(4, 4, 2, 4));

        viewArea.setEditable(false);
        viewArea.setBackground(new Color(0xFAFAFA));
        viewArea.setText("JCEF bridge is not enabled in this build.\n"
                + "Result summary and rendering payload will appear here.");

        root.setBorder(BorderFactory.createLineBorder(new Color(0xD9D9D9)));
        root.add(title, BorderLayout.NORTH);
        root.add(new JScrollPane(viewArea), BorderLayout.CENTER);
    }

    @Override
    public JComponent getComponent() {
        return root;
    }

    @Override
    public void renderQueryResult(QueryResult result) {
        int rows = result == null || result.getRows() == null ? 0 : result.getRows().size();
        int cols = result == null || result.getColumns() == null ? 0 : result.getColumns().size();
        viewArea.setText("render_result\n"
                + "rows=" + rows + "\n"
                + "columns=" + cols + "\n"
                + "truncated=" + (result != null && result.isTruncated()));
    }

    @Override
    public void renderInfo(String message) {
        viewArea.setText(message == null ? "" : message);
    }
}
