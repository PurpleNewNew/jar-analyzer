/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.swing.panel;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import me.n1ar4.jar.analyzer.graph.query.QueryOptions;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.query.QueryServices;
import me.n1ar4.jar.analyzer.gui.swing.SwingI18n;
import me.n1ar4.jar.analyzer.gui.swing.browser.BrowserBridge;
import me.n1ar4.jar.analyzer.gui.swing.browser.SwingGraphBrowserBridge;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class CypherToolPanel extends JPanel {
    private final JTextArea queryArea = new JTextArea(
            "MATCH (m:Method)-[r]->(n)\nRETURN m, r, n\nLIMIT 50");
    private final JTextArea paramsArea = new JTextArea("{}");
    private final JTextArea statusArea = new JTextArea();
    private final JTable resultTable = new JTable();
    private final JSpinner maxRowsSpin = new JSpinner(new SpinnerNumberModel(500, 1, 10000, 50));
    private final BrowserBridge browserBridge = new SwingGraphBrowserBridge();
    private final JButton runButton = new JButton("Run");
    private final JButton explainButton = new JButton("Explain");
    private final JButton capabilitiesButton = new JButton("Capabilities");

    public CypherToolPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initUi();
    }

    private void initUi() {
        queryArea.setRows(8);
        paramsArea.setRows(4);
        statusArea.setRows(4);
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);

        JPanel queryPanel = new JPanel(new BorderLayout(6, 6));
        queryPanel.setBorder(BorderFactory.createTitledBorder("Cypher"));
        queryPanel.add(new JScrollPane(queryArea), BorderLayout.CENTER);

        JPanel paramsPanel = new JPanel(new BorderLayout(6, 6));
        paramsPanel.setBorder(BorderFactory.createTitledBorder("Params JSON"));
        paramsPanel.add(new JScrollPane(paramsArea), BorderLayout.CENTER);

        JPanel topLeft = new JPanel(new BorderLayout(6, 6));
        topLeft.add(queryPanel, BorderLayout.CENTER);
        topLeft.add(paramsPanel, BorderLayout.SOUTH);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actionPanel.add(runButton);
        actionPanel.add(explainButton);
        actionPanel.add(capabilitiesButton);
        actionPanel.add(new JLabel("maxRows"));
        actionPanel.add(maxRowsSpin);
        runButton.addActionListener(e -> executeQuery());
        explainButton.addActionListener(e -> explainQuery());
        capabilitiesButton.addActionListener(e -> showCapabilities());

        JPanel tablePanel = new JPanel(new BorderLayout(6, 6));
        tablePanel.setBorder(BorderFactory.createTitledBorder("Result"));
        tablePanel.add(new JScrollPane(resultTable), BorderLayout.CENTER);

        JPanel left = new JPanel(new BorderLayout(6, 6));
        left.add(topLeft, BorderLayout.NORTH);
        left.add(actionPanel, BorderLayout.CENTER);
        left.add(tablePanel, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.setBorder(BorderFactory.createTitledBorder("Graph Browser"));
        JLabel hint = new JLabel("Native graph result browser");
        hint.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        hint.setOpaque(true);
        hint.setBackground(new Color(0xF7F7F7));
        right.add(hint, BorderLayout.NORTH);
        right.add(browserBridge.getComponent(), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.6D);
        split.setDividerLocation(0.6D);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder("Status"));
        statusPanel.add(new JScrollPane(statusArea), BorderLayout.CENTER);

        add(split, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
        applyLanguage();
    }

    public void applyLanguage() {
        SwingI18n.localizeComponentTree(this);
    }

    private void executeQuery() {
        String query = safe(queryArea.getText()).trim();
        if (query.isEmpty()) {
            statusArea.setText("query required");
            return;
        }
        Map<String, Object> params;
        try {
            params = parseParams(paramsArea.getText());
        } catch (IllegalArgumentException ex) {
            statusArea.setText(ex.getMessage());
            return;
        }
        int maxRows = (Integer) maxRowsSpin.getValue();
        QueryOptions options = new QueryOptions(maxRows, 15000, 8, 500);
        runButton.setEnabled(false);
        statusArea.setText("running...");
        Thread.ofVirtual().name("swing-cypher-query").start(() -> {
            long start = System.nanoTime();
            try {
                QueryResult result = QueryServices.cypher().execute(query, params, options);
                long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
                SwingUtilities.invokeLater(() -> {
                    applyResult(result);
                    browserBridge.renderQueryResult(result);
                    statusArea.setText("elapsedMs=" + elapsedMs
                            + ", rows=" + safeSize(result.getRows())
                            + ", truncated=" + result.isTruncated());
                    runButton.setEnabled(true);
                });
            } catch (Throwable ex) {
                SwingUtilities.invokeLater(() -> {
                    statusArea.setText("cypher query failed: " + ex.getMessage());
                    browserBridge.renderInfo("query failed: " + ex.getMessage());
                    runButton.setEnabled(true);
                });
            }
        });
    }

    private void explainQuery() {
        String query = safe(queryArea.getText()).trim();
        if (query.isEmpty()) {
            statusArea.setText("query required");
            return;
        }
        try {
            Map<String, Object> explain = QueryServices.cypher().explain(query);
            String text = JSON.toJSONString(explain);
            statusArea.setText(text);
            browserBridge.renderInfo(text);
        } catch (Exception ex) {
            statusArea.setText("cypher explain failed: " + ex.getMessage());
            browserBridge.renderInfo("explain failed: " + ex.getMessage());
        }
    }

    private void showCapabilities() {
        Map<String, Object> data = QueryServices.cypher().capabilities();
        String text = JSON.toJSONString(data);
        statusArea.setText(text);
        browserBridge.renderInfo(text);
    }

    private void applyResult(QueryResult result) {
        List<String> columns = result == null ? Collections.emptyList() : result.getColumns();
        List<List<Object>> rows = result == null ? Collections.emptyList() : result.getRows();
        String[] head = columns.toArray(new String[0]);
        DefaultTableModel model = new DefaultTableModel(head, 0);
        for (List<Object> row : rows) {
            if (row == null) {
                model.addRow(new Object[head.length]);
                continue;
            }
            Object[] item = new Object[head.length];
            for (int i = 0; i < head.length && i < row.size(); i++) {
                item[i] = row.get(i);
            }
            model.addRow(item);
        }
        resultTable.setModel(model);
    }

    private static Map<String, Object> parseParams(String raw) {
        String text = safe(raw).trim();
        if (text.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            JSONObject obj = JSON.parseObject(text);
            if (obj == null || obj.isEmpty()) {
                return Collections.emptyMap();
            }
            return obj;
        } catch (Exception ex) {
            throw new IllegalArgumentException("params json invalid: " + ex.getMessage());
        }
    }

    private static int safeSize(List<?> value) {
        return value == null ? 0 : value.size();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
