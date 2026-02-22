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

import me.n1ar4.jar.analyzer.graph.query.QueryOptions;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.query.QueryServices;
import me.n1ar4.jar.analyzer.gui.runtime.api.RuntimeFacades;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchMatchMode;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchMode;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchQueryDto;
import me.n1ar4.jar.analyzer.gui.swing.SwingI18n;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class SqlAuditToolPanel extends JPanel {
    private final JComboBox<AuditTemplate> templateBox = new JComboBox<>();
    private final JLabel templateLabel = new JLabel();
    private final JLabel keywordLabel = new JLabel();
    private final JLabel classLabel = new JLabel();
    private final JLabel maxRowsLabel = new JLabel();
    private final JLabel timeoutLabel = new JLabel();
    private final JTextField keywordField = new JTextField("exec");
    private final JTextField classLikeField = new JTextField();
    private final JSpinner maxRowsSpin = new JSpinner(new SpinnerNumberModel(300, 1, 5000, 50));
    private final JSpinner timeoutMsSpin = new JSpinner(new SpinnerNumberModel(15000, 500, 120000, 500));
    private final JTextArea queryArea = new JTextArea();
    private final JTextArea statusArea = new JTextArea();
    private final JTable resultTable = new JTable();
    private final JButton runButton = new JButton();
    private final JButton applyTemplateButton = new JButton();
    private final JButton listTablesButton = new JButton();
    private final JButton explainButton = new JButton();
    private final JButton openSelectedButton = new JButton();
    private final JButton pushSearchButton = new JButton();
    private final JMenuItem menuOpen = new JMenuItem();
    private final JMenuItem menuSetSource = new JMenuItem();
    private final JMenuItem menuSetSink = new JMenuItem();
    private final JPopupMenu resultMenu = new JPopupMenu();
    private final AtomicLong runSeq = new AtomicLong(0L);

    private final JPanel templatePanel = new JPanel(new BorderLayout(6, 6));
    private final JPanel queryPanel = new JPanel(new BorderLayout(6, 6));
    private final JPanel resultPanel = new JPanel(new BorderLayout(6, 6));
    private final JPanel statusPanel = new JPanel(new BorderLayout(6, 6));

    public SqlAuditToolPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initTemplates();
        initUi();
        applyLanguage();
        applyTemplate();
    }

    private void initTemplates() {
        templateBox.addItem(new AuditTemplate(
                "runtime_exec",
                "审计：命令执行",
                "Audit: Runtime Exec",
                """
                        SELECT caller_class_name AS class_name,
                               caller_method_name AS method_name,
                               caller_method_desc AS method_desc,
                               caller_jar_id AS jar_id,
                               caller_jar_name AS jar_name,
                               callee_class_name,
                               callee_method_name,
                               confidence
                        FROM call_table
                        WHERE (callee_class_name LIKE '%Runtime%' AND lower(callee_method_name)='exec')
                           OR (callee_class_name LIKE '%ProcessBuilder%' AND lower(callee_method_name)='start')
                        ORDER BY caller_jar_id, caller_class_name, caller_method_name
                        LIMIT {{limit}};
                        """
        ));
        templateBox.addItem(new AuditTemplate(
                "method_keyword",
                "审计：方法关键词",
                "Audit: Keyword in Methods",
                """
                        SELECT class_name,
                               method_name,
                               method_desc,
                               jar_id,
                               jar_name
                        FROM method_table
                        WHERE lower(class_name) LIKE lower('%{{class_like}}%')
                           OR lower(method_name) LIKE lower('%{{keyword}}%')
                        ORDER BY jar_id, class_name, method_name
                        LIMIT {{limit}};
                        """
        ));
        templateBox.addItem(new AuditTemplate(
                "secret_strings",
                "审计：敏感字符串",
                "Audit: Secret Strings",
                """
                        SELECT class_name,
                               method_name,
                               method_desc,
                               jar_id,
                               jar_name,
                               value
                        FROM string_table
                        WHERE lower(value) LIKE '%password%'
                           OR lower(value) LIKE '%secret%'
                           OR lower(value) LIKE '%token%'
                           OR lower(value) LIKE '%apikey%'
                        ORDER BY jar_id, class_name, method_name
                        LIMIT {{limit}};
                        """
        ));
        templateBox.addItem(new AuditTemplate(
                "resource_paths",
                "审计：资源路径",
                "Audit: Resource Paths",
                """
                        SELECT rid,
                               resource_path,
                               jar_id,
                               jar_name,
                               file_size
                        FROM resource_table
                        WHERE lower(resource_path) LIKE lower('%{{keyword}}%')
                        ORDER BY jar_id, resource_path
                        LIMIT {{limit}};
                        """
        ));
        templateBox.addItem(new AuditTemplate(
                "jar_hotspots",
                "审计：Jar 热点",
                "Audit: Jar Hotspots",
                """
                        SELECT caller_jar_id AS jar_id,
                               caller_jar_name AS jar_name,
                               count(*) AS call_count
                        FROM call_table
                        GROUP BY caller_jar_id, caller_jar_name
                        ORDER BY call_count DESC
                        LIMIT {{limit}};
                        """
        ));
        templateBox.addItem(new AuditTemplate(
                "list_tables",
                "SQL：列出表",
                "SQL: List Tables",
                "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;"
        ));
    }

    private void initUi() {
        queryArea.setRows(10);
        queryArea.setLineWrap(false);
        Font base = UIManager.getFont("TextArea.font");
        if (base != null) {
            queryArea.setFont(new Font(Font.MONOSPACED, base.getStyle(), base.getSize()));
        }

        statusArea.setRows(3);
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        Color statusBg = UIManager.getColor("TextArea.background");
        if (statusBg != null) {
            statusArea.setBackground(statusBg);
        }

        resultTable.setAutoCreateRowSorter(true);
        resultTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    openSelectedResult();
                }
                maybeShowResultMenu(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowResultMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowResultMenu(e);
            }
        });

        menuOpen.addActionListener(e -> openSelectedResult());
        menuSetSource.addActionListener(e -> setSelectedAsSource());
        menuSetSink.addActionListener(e -> setSelectedAsSink());
        resultMenu.add(menuOpen);
        resultMenu.add(menuSetSource);
        resultMenu.add(menuSetSink);

        templatePanel.setBorder(BorderFactory.createTitledBorder(""));
        JPanel templateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        templateRow.add(templateLabel);
        templateRow.add(templateBox);
        templateRow.add(keywordLabel);
        keywordField.setColumns(14);
        templateRow.add(keywordField);
        templateRow.add(classLabel);
        classLikeField.setColumns(18);
        templateRow.add(classLikeField);
        templateRow.add(maxRowsLabel);
        templateRow.add(maxRowsSpin);
        templateRow.add(timeoutLabel);
        templateRow.add(timeoutMsSpin);
        templatePanel.add(templateRow, BorderLayout.CENTER);

        queryPanel.setBorder(BorderFactory.createTitledBorder(""));
        queryPanel.add(new JScrollPane(queryArea), BorderLayout.CENTER);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        applyTemplateButton.addActionListener(e -> applyTemplate());
        listTablesButton.addActionListener(e -> selectListTableTemplate());
        runButton.addActionListener(e -> runQueryAsync(false));
        explainButton.addActionListener(e -> runQueryAsync(true));
        openSelectedButton.addActionListener(e -> openSelectedResult());
        pushSearchButton.addActionListener(e -> pushQueryToSearch());
        actionRow.add(applyTemplateButton);
        actionRow.add(listTablesButton);
        actionRow.add(runButton);
        actionRow.add(explainButton);
        actionRow.add(openSelectedButton);
        actionRow.add(pushSearchButton);
        queryPanel.add(actionRow, BorderLayout.SOUTH);

        resultPanel.setBorder(BorderFactory.createTitledBorder(""));
        resultPanel.add(new JScrollPane(resultTable), BorderLayout.CENTER);

        statusPanel.setBorder(BorderFactory.createTitledBorder(""));
        statusPanel.add(new JScrollPane(statusArea), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, queryPanel, resultPanel);
        split.setResizeWeight(0.35D);
        split.setDividerLocation(0.35D);

        add(templatePanel, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }

    public void applyLanguage() {
        SwingI18n.localizeComponentTree(this);
        setTitle(templatePanel, tr("审计工作流", "Audit Workflow"));
        setTitle(queryPanel, tr("审计 SQL", "Audit SQL"));
        setTitle(resultPanel, tr("查询结果", "Result"));
        setTitle(statusPanel, tr("状态", "Status"));
        applyTemplateButton.setText(tr("套用模板", "Apply Template"));
        listTablesButton.setText(tr("列出表", "List Tables"));
        runButton.setText(tr("执行", "Run"));
        explainButton.setText(tr("Explain", "Explain"));
        openSelectedButton.setText(tr("打开选中", "Open Selected"));
        pushSearchButton.setText(tr("推送到搜索", "Push To Search"));
        templateLabel.setText(tr("模板", "Template"));
        keywordLabel.setText(tr("关键字", "keyword"));
        classLabel.setText(tr("类过滤", "class"));
        maxRowsLabel.setText("maxRows");
        timeoutLabel.setText("timeoutMs");
        menuOpen.setText(tr("打开结果", "Open Result"));
        menuSetSource.setText(tr("设为 Source", "Set As Source"));
        menuSetSink.setText(tr("设为 Sink", "Set As Sink"));
        templateBox.repaint();
    }

    private void applyTemplate() {
        AuditTemplate template = selectedTemplate();
        if (template == null) {
            return;
        }
        String sql = template.sql();
        sql = sql.replace("{{keyword}}", escapeSqlLike(safe(keywordField.getText()).trim()));
        sql = sql.replace("{{class_like}}", escapeSqlLike(safe(classLikeField.getText()).trim()));
        sql = sql.replace("{{limit}}", String.valueOf((Integer) maxRowsSpin.getValue()));
        queryArea.setText(sql);
        queryArea.setCaretPosition(0);
        setStatus(tr("模板已应用", "template applied"));
    }

    private void selectListTableTemplate() {
        for (int i = 0; i < templateBox.getItemCount(); i++) {
            AuditTemplate template = templateBox.getItemAt(i);
            if (template != null && template.is("list_tables")) {
                templateBox.setSelectedIndex(i);
                applyTemplate();
                return;
            }
        }
    }

    private void runQueryAsync(boolean explain) {
        String sql = safe(queryArea.getText()).trim();
        if (sql.isBlank()) {
            setStatus(tr("SQL 不能为空", "sql query is required"));
            return;
        }
        long seq = runSeq.incrementAndGet();
        int maxRows = (Integer) maxRowsSpin.getValue();
        int timeoutMs = (Integer) timeoutMsSpin.getValue();
        runButton.setEnabled(false);
        explainButton.setEnabled(false);
        setStatus(tr("执行中...", "running..."));
        Thread.ofVirtual().name("swing-sql-audit").start(() -> {
            long startNs = System.nanoTime();
            try {
                String script = explain ? "EXPLAIN QUERY PLAN " + sql : sql;
                QueryOptions options = new QueryOptions(maxRows, timeoutMs, 8, Math.min(maxRows, 5000));
                QueryResult result = QueryServices.sql().execute(script, Map.of(), options);
                long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
                SwingUtilities.invokeLater(() -> {
                    if (seq != runSeq.get()) {
                        return;
                    }
                    applyResult(result);
                    int rows = result == null || result.getRows() == null ? 0 : result.getRows().size();
                    String status = tr("耗时", "elapsed") + "=" + elapsedMs + "ms, "
                            + tr("行数", "rows") + "=" + rows;
                    if (result != null && result.isTruncated()) {
                        status = status + ", " + tr("已截断", "truncated");
                    }
                    setStatus(status);
                    runButton.setEnabled(true);
                    explainButton.setEnabled(true);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    if (seq != runSeq.get()) {
                        return;
                    }
                    setStatus(tr("SQL 执行失败: ", "sql query failed: ") + safe(ex.getMessage()));
                    runButton.setEnabled(true);
                    explainButton.setEnabled(true);
                });
            }
        });
    }

    private void pushQueryToSearch() {
        String sql = safe(queryArea.getText()).trim();
        if (sql.isBlank()) {
            setStatus(tr("SQL 不能为空", "sql query is required"));
            return;
        }
        RuntimeFacades.search().applyQuery(new SearchQueryDto(
                SearchMode.SQL_QUERY,
                SearchMatchMode.LIKE,
                "",
                "",
                sql,
                false
        ));
        RuntimeFacades.search().runSearch();
        setStatus(tr("已推送到搜索面板", "pushed to search workflow"));
    }

    private void applyResult(QueryResult result) {
        List<String> columns = result == null ? Collections.emptyList() : safeList(result.getColumns());
        List<List<Object>> rows = result == null ? Collections.emptyList() : safeList(result.getRows());
        DefaultTableModel model = new DefaultTableModel(columns.toArray(new String[0]), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (List<Object> row : rows) {
            if (row == null) {
                model.addRow(new Object[columns.size()]);
                continue;
            }
            Object[] values = new Object[columns.size()];
            for (int i = 0; i < columns.size() && i < row.size(); i++) {
                values[i] = row.get(i);
            }
            model.addRow(values);
        }
        resultTable.setModel(model);
        resultTable.setDefaultEditor(Object.class, new DefaultCellEditor(new JTextField()));
    }

    private void maybeShowResultMenu(MouseEvent event) {
        if (!event.isPopupTrigger()) {
            return;
        }
        int row = resultTable.rowAtPoint(event.getPoint());
        if (row < 0) {
            return;
        }
        resultTable.setRowSelectionInterval(row, row);
        TableSelection selection = selectedRow();
        boolean openEnabled = selection != null && selection.hasNavigation();
        boolean chainEnabled = selection != null && selection.hasMethod();
        menuOpen.setEnabled(openEnabled);
        menuSetSource.setEnabled(chainEnabled);
        menuSetSink.setEnabled(chainEnabled);
        resultMenu.show(resultTable, event.getX(), event.getY());
    }

    private void openSelectedResult() {
        TableSelection selection = selectedRow();
        if (selection == null) {
            return;
        }
        if (selection.hasMethod()) {
            RuntimeFacades.editor().openMethod(selection.className, selection.methodName, selection.methodDesc, selection.jarId);
            setStatus(tr("已打开方法", "opened method"));
            return;
        }
        if (selection.hasClassOnly()) {
            RuntimeFacades.editor().openClass(selection.className, selection.jarId);
            setStatus(tr("已打开类", "opened class"));
            return;
        }
        if (selection.resourceId != null && selection.resourceId > 0) {
            RuntimeFacades.projectTree().openNode("res:" + selection.resourceId);
            setStatus(tr("已打开资源", "opened resource"));
            return;
        }
        if (!safe(selection.resourcePath).isBlank()) {
            RuntimeFacades.projectTree().openNode("path:" + selection.resourcePath);
            setStatus(tr("已打开路径", "opened path"));
        }
    }

    private void setSelectedAsSource() {
        TableSelection selection = selectedRow();
        if (selection == null || !selection.hasMethod()) {
            return;
        }
        RuntimeFacades.chains().setSource(selection.className, selection.methodName, selection.methodDesc);
        setStatus(tr("已设为 Source", "set as source"));
    }

    private void setSelectedAsSink() {
        TableSelection selection = selectedRow();
        if (selection == null || !selection.hasMethod()) {
            return;
        }
        RuntimeFacades.chains().setSink(selection.className, selection.methodName, selection.methodDesc);
        setStatus(tr("已设为 Sink", "set as sink"));
    }

    private TableSelection selectedRow() {
        int row = resultTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int modelRow = resultTable.convertRowIndexToModel(row);
        TableModel model = resultTable.getModel();
        if (model == null || modelRow < 0 || modelRow >= model.getRowCount()) {
            return null;
        }
        int classIndex = findColumn(model, "class_name", "class");
        int methodIndex = findColumn(model, "method_name", "method");
        int descIndex = findColumn(model, "method_desc", "desc", "descriptor");
        int jarIndex = findColumn(model, "jar_id", "jid");
        int ridIndex = findColumn(model, "rid", "resource_id");
        int pathIndex = findColumn(model, "resource_path", "path");
        String className = normalizeClass(valueAt(model, modelRow, classIndex));
        String methodName = safe(valueAt(model, modelRow, methodIndex)).trim();
        String methodDesc = safe(valueAt(model, modelRow, descIndex)).trim();
        Integer jarId = intValueAt(model, modelRow, jarIndex);
        Integer resourceId = intValueAt(model, modelRow, ridIndex);
        String resourcePath = safe(valueAt(model, modelRow, pathIndex)).trim();
        return new TableSelection(className, methodName, methodDesc, jarId, resourceId, resourcePath);
    }

    private static int findColumn(TableModel model, String... names) {
        if (model == null || names == null || names.length == 0) {
            return -1;
        }
        for (int i = 0; i < model.getColumnCount(); i++) {
            String column = safe(model.getColumnName(i)).trim().toLowerCase(Locale.ROOT);
            for (String name : names) {
                if (column.equals(safe(name).trim().toLowerCase(Locale.ROOT))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String valueAt(TableModel model, int row, int col) {
        if (model == null || col < 0 || row < 0 || row >= model.getRowCount() || col >= model.getColumnCount()) {
            return "";
        }
        Object value = model.getValueAt(row, col);
        return safe(value == null ? "" : String.valueOf(value));
    }

    private static Integer intValueAt(TableModel model, int row, int col) {
        String value = valueAt(model, row, col).trim();
        if (value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String normalizeClass(String value) {
        return safe(value).trim().replace('.', '/');
    }

    private static String escapeSqlLike(String value) {
        return safe(value).replace("'", "''");
    }

    private static <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : new ArrayList<>(list);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void setTitle(JPanel panel, String title) {
        if (panel == null) {
            return;
        }
        if (panel.getBorder() instanceof TitledBorder border) {
            border.setTitle(title);
            panel.repaint();
        }
    }

    private static String tr(String zh, String en) {
        return SwingI18n.tr(zh, en);
    }

    private void setStatus(String text) {
        statusArea.setText(safe(text));
        statusArea.setCaretPosition(0);
    }

    private AuditTemplate selectedTemplate() {
        Object value = templateBox.getSelectedItem();
        if (value instanceof AuditTemplate template) {
            return template;
        }
        return null;
    }

    private record AuditTemplate(String key, String zh, String en, String sql) {
        private boolean is(String target) {
            return safe(key).equalsIgnoreCase(safe(target));
        }

        @Override
        public String toString() {
            return tr(zh, en);
        }
    }

    private record TableSelection(
            String className,
            String methodName,
            String methodDesc,
            Integer jarId,
            Integer resourceId,
            String resourcePath
    ) {
        private boolean hasMethod() {
            return !safe(className).isBlank() && !safe(methodName).isBlank();
        }

        private boolean hasClassOnly() {
            return !safe(className).isBlank();
        }

        private boolean hasNavigation() {
            return hasMethod() || hasClassOnly() || (resourceId != null && resourceId > 0) || !safe(resourcePath).isBlank();
        }
    }
}
