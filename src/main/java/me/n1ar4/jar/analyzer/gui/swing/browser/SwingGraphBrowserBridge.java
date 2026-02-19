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

import com.alibaba.fastjson2.JSON;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class SwingGraphBrowserBridge implements BrowserBridge {
    private final JPanel root = new JPanel(new BorderLayout(6, 6));
    private final JLabel summaryLabel = new JLabel("Graph view ready");
    private final DefaultTableModel nodeModel = new DefaultTableModel(new String[]{"Node", "Hits"}, 0);
    private final DefaultTableModel edgeModel = new DefaultTableModel(new String[]{"Source", "Target", "Hits"}, 0);
    private final DefaultTableModel rowModel = new DefaultTableModel();
    private final JTable nodeTable = new JTable(nodeModel);
    private final JTable edgeTable = new JTable(edgeModel);
    private final JTable rowTable = new JTable(rowModel);
    private final JTextArea rawArea = new JTextArea();

    public SwingGraphBrowserBridge() {
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        rawArea.setEditable(false);

        JPanel graphPanel = new JPanel(new BorderLayout(6, 6));
        JSplitPane graphSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(nodeTable),
                new JScrollPane(edgeTable)
        );
        graphSplit.setResizeWeight(0.5D);
        graphSplit.setDividerLocation(0.5D);
        graphPanel.add(graphSplit, BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Graph", graphPanel);
        tabs.addTab("Rows", new JScrollPane(rowTable));
        tabs.addTab("Raw", new JScrollPane(rawArea));

        root.add(summaryLabel, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
    }

    @Override
    public JComponent getComponent() {
        return root;
    }

    @Override
    public void renderQueryResult(QueryResult result) {
        List<String> columns = result == null || result.getColumns() == null
                ? List.of()
                : result.getColumns();
        List<List<Object>> rows = result == null || result.getRows() == null
                ? List.of()
                : result.getRows();

        renderRows(columns, rows);
        GraphView graph = buildGraph(columns, rows);
        renderNodes(graph.nodes());
        renderEdges(graph.edges());

        String summary = "rows=" + rows.size()
                + ", columns=" + columns.size()
                + ", nodes=" + graph.nodes().size()
                + ", edges=" + graph.edges().size()
                + ", truncated=" + (result != null && result.isTruncated());
        summaryLabel.setText(summary);
        rawArea.setText(JSON.toJSONString(rows));
    }

    @Override
    public void renderInfo(String message) {
        summaryLabel.setText(message == null ? "" : message);
        rawArea.setText(message == null ? "" : message);
        clearRows(nodeModel);
        clearRows(edgeModel);
        clearModel(rowModel);
    }

    private void renderRows(List<String> columns, List<List<Object>> rows) {
        clearModel(rowModel);
        for (String col : columns) {
            rowModel.addColumn(col);
        }
        for (List<Object> row : rows) {
            if (row == null) {
                rowModel.addRow(new Object[columns.size()]);
                continue;
            }
            Object[] out = new Object[columns.size()];
            for (int i = 0; i < columns.size() && i < row.size(); i++) {
                out[i] = row.get(i);
            }
            rowModel.addRow(out);
        }
    }

    private void renderNodes(Map<String, Integer> nodes) {
        clearRows(nodeModel);
        for (Map.Entry<String, Integer> entry : nodes.entrySet()) {
            nodeModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }
    }

    private void renderEdges(Map<EdgeKey, Integer> edges) {
        clearRows(edgeModel);
        for (Map.Entry<EdgeKey, Integer> entry : edges.entrySet()) {
            edgeModel.addRow(new Object[]{
                    entry.getKey().source(),
                    entry.getKey().target(),
                    entry.getValue()
            });
        }
    }

    private GraphView buildGraph(List<String> columns, List<List<Object>> rows) {
        Map<String, Integer> nodes = new LinkedHashMap<>();
        Map<EdgeKey, Integer> edges = new LinkedHashMap<>();
        Map<String, Integer> idx = buildIndex(columns);
        int callerIdx = firstIndex(idx, "caller_class_name", "caller_class", "caller");
        int calleeIdx = firstIndex(idx, "callee_class_name", "callee_class", "callee");
        int sourceIdx = firstIndex(idx, "source", "from");
        int targetIdx = firstIndex(idx, "target", "to");

        List<Integer> nodeIndexes = collectNodeIndexes(columns);
        for (List<Object> row : rows) {
            if (row == null) {
                continue;
            }
            for (Integer i : nodeIndexes) {
                if (i == null || i < 0 || i >= row.size()) {
                    continue;
                }
                String node = normalizeNode(row.get(i));
                if (node == null) {
                    continue;
                }
                nodes.merge(node, 1, Integer::sum);
            }
            String caller = valueAt(row, callerIdx);
            String callee = valueAt(row, calleeIdx);
            if (caller == null || callee == null) {
                caller = valueAt(row, sourceIdx);
                callee = valueAt(row, targetIdx);
            }
            if (caller != null && callee != null) {
                nodes.merge(caller, 1, Integer::sum);
                nodes.merge(callee, 1, Integer::sum);
                edges.merge(new EdgeKey(caller, callee), 1, Integer::sum);
            }
        }
        return new GraphView(nodes, edges);
    }

    private static Map<String, Integer> buildIndex(List<String> columns) {
        Map<String, Integer> out = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            String key = columns.get(i);
            if (key == null) {
                continue;
            }
            out.put(key.trim().toLowerCase(Locale.ROOT), i);
        }
        return out;
    }

    private static List<Integer> collectNodeIndexes(List<String> columns) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            String col = columns.get(i);
            if (col == null) {
                continue;
            }
            String key = col.trim().toLowerCase(Locale.ROOT);
            if (key.contains("class") || key.endsWith("owner") || key.equals("source") || key.equals("target")) {
                out.add(i);
            }
        }
        return out;
    }

    private static int firstIndex(Map<String, Integer> indexes, String... keys) {
        if (indexes == null || keys == null) {
            return -1;
        }
        for (String key : keys) {
            Integer idx = indexes.get(key);
            if (idx != null) {
                return idx;
            }
        }
        return -1;
    }

    private static String valueAt(List<Object> row, int idx) {
        if (row == null || idx < 0 || idx >= row.size()) {
            return null;
        }
        return normalizeNode(row.get(idx));
    }

    private static String normalizeNode(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.length() > 256) {
            return text.substring(0, 256);
        }
        return text;
    }

    private static void clearModel(DefaultTableModel model) {
        if (model == null) {
            return;
        }
        model.setRowCount(0);
        model.setColumnCount(0);
    }

    private static void clearRows(DefaultTableModel model) {
        if (model == null) {
            return;
        }
        model.setRowCount(0);
    }

    private record GraphView(Map<String, Integer> nodes, Map<EdgeKey, Integer> edges) {
        private GraphView {
            nodes = Objects.requireNonNullElseGet(nodes, LinkedHashMap::new);
            edges = Objects.requireNonNullElseGet(edges, LinkedHashMap::new);
        }
    }

    private record EdgeKey(String source, String target) {
    }
}
