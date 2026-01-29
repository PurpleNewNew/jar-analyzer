/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui;

import com.formdev.flatlaf.FlatLaf;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.dfs.DFSEdge;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.taint.Sanitizer;
import me.n1ar4.jar.analyzer.taint.SanitizerRule;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("all")
public class TaintResultDialog extends JFrame {
    private static final Logger logger = LogManager.getLogger();
    private static final String[] RESULT_COLUMNS = {
            "序号", "Source类", "Source方法", "Sink类", "Sink方法", "调用链深度", "分析结果"
    };
    private static final String[] SANITIZER_COLUMNS = {
            "类名", "方法名", "方法描述", "参数索引"
    };
    private static final int[] RESULT_COLUMN_WIDTHS = {50, 200, 150, 200, 150, 80, 100};
    private static final int[] SANITIZER_COLUMN_WIDTHS = {300, 150, 250, 80};
    private static final int LOAD_FAST_PATH_LIMIT = 20000;
    private static final int LOAD_BATCH_SIZE = 5000;

    private JTable resultTable;
    private JTextArea detailTextArea;
    private JLabel summaryLabel;
    private JLabel sanitizerCountLabel;
    private JTable sanitizerTable;
    private DefaultTableModel resultTableModel;
    private DefaultTableModel sanitizerTableModel;

    // 保存原始数据用于详情显示
    private final List<TaintResult> originalTaintResults;
    private final AtomicInteger detailSeq = new AtomicInteger(0);

    public TaintResultDialog(Frame parent, List<TaintResult> taintResults) {
        super("污点分析结果详情");
        this.originalTaintResults = taintResults == null
                ? Collections.emptyList()
                : new ArrayList<>(taintResults);
        initializeComponents();
        setupLayout();
        loadDataAsync(this.originalTaintResults);
        setupEventHandlers();

        setSize(1200, 800);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setResizable(true);
        setMinimumSize(new Dimension(800, 600));

        // 首先设置为最上层
        setAlwaysOnTop(true);
        setVisible(true);
    }

    private void initializeComponents() {
        // 创建结果表格
        resultTableModel = new DefaultTableModel(RESULT_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable = new JTable(resultTableModel);
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultTable.getTableHeader().setReorderingAllowed(false);

        // 设置表格列宽
        applyColumnWidths(resultTable, RESULT_COLUMN_WIDTHS);

        // 创建详情文本区域
        detailTextArea = new JTextArea();
        detailTextArea.setEditable(false);
        detailTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        detailTextArea.setText("请选择一行查看详细的污点分析过程...");

        // 创建统计标签
        summaryLabel = new JLabel();
        summaryLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        if (FlatLaf.isLafDark()) {
            summaryLabel.setForeground(new Color(100, 200, 100));
        } else {
            summaryLabel.setForeground(new Color(0, 100, 0));
        }

        sanitizerCountLabel = new JLabel();
        sanitizerCountLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        if (FlatLaf.isLafDark()) {
            sanitizerCountLabel.setForeground(new Color(100, 150, 255));
        } else {
        sanitizerCountLabel.setForeground(new Color(0, 0, 150));
        }

        // 创建Sanitizer规则表格
        sanitizerTableModel = new DefaultTableModel(SANITIZER_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        sanitizerTable = new JTable(sanitizerTableModel);
        sanitizerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sanitizerTable.getTableHeader().setReorderingAllowed(false);

        // 设置Sanitizer表格列宽
        applyColumnWidths(sanitizerTable, SANITIZER_COLUMN_WIDTHS);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // 顶部统计信息面板
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(new TitledBorder("统计信息"));
        topPanel.add(summaryLabel);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(sanitizerCountLabel);

        // 中间分割面板
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setResizeWeight(0.4);

        // 上半部分：结果表格
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(new TitledBorder("污点分析结果"));
        JScrollPane resultScrollPane = new JScrollPane(resultTable);
        resultPanel.add(resultScrollPane, BorderLayout.CENTER);

        // 下半部分：详情和Sanitizer规则的分割面板
        JSplitPane bottomSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        bottomSplitPane.setResizeWeight(0.6);

        // 左侧：详情文本
        JPanel detailPanel = new JPanel(new BorderLayout());
        detailPanel.setBorder(new TitledBorder("污点分析详情"));
        JScrollPane detailScrollPane = new JScrollPane(detailTextArea);
        detailPanel.add(detailScrollPane, BorderLayout.CENTER);

        // 右侧：Sanitizer规则表格
        JPanel sanitizerPanel = new JPanel(new BorderLayout());
        sanitizerPanel.setBorder(new TitledBorder("Sanitizer规则"));
        JScrollPane sanitizerScrollPane = new JScrollPane(sanitizerTable);
        sanitizerPanel.add(sanitizerScrollPane, BorderLayout.CENTER);

        bottomSplitPane.setLeftComponent(detailPanel);
        bottomSplitPane.setRightComponent(sanitizerPanel);

        mainSplitPane.setTopComponent(resultPanel);
        mainSplitPane.setBottomComponent(bottomSplitPane);

        // 底部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton exportButton = new JButton("导出结果");
        JButton closeButton = new JButton("关闭");

        exportButton.addActionListener(e -> exportResults());
        closeButton.addActionListener(e -> dispose());

        buttonPanel.add(exportButton);
        buttonPanel.add(closeButton);

        add(topPanel, BorderLayout.NORTH);
        add(mainSplitPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadDataAsync(List<TaintResult> taintResults) {
        summaryLabel.setText("loading...");
        sanitizerCountLabel.setText("loading...");
        UiExecutor.runAsync(() -> {
            TaintLoad taintLoad = buildTaintLoad(taintResults);
            SanitizerLoad sanitizerLoad = buildSanitizerLoad();
            UiExecutor.runOnEdt(() -> {
                if (!isDisplayable()) {
                    return;
                }
                applyRows(resultTableModel, resultTable, taintLoad.rows, RESULT_COLUMNS, RESULT_COLUMN_WIDTHS);
                summaryLabel.setText(taintLoad.summaryText);
                applyRows(sanitizerTableModel, sanitizerTable, sanitizerLoad.rows, SANITIZER_COLUMNS, SANITIZER_COLUMN_WIDTHS);
                sanitizerCountLabel.setText(sanitizerLoad.labelText);
            });
        });
    }

    private void setupEventHandlers() {
        // 结果表格选择事件
        resultTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = resultTable.getSelectedRow();
                if (selectedRow >= 0) {
                    showDetailForRowAsync(selectedRow);
                }
            }
        });
    }

    private String formatParamLabel(int paramIndex) {
        if (paramIndex == Sanitizer.THIS_PARAM) {
            return "this";
        }
        if (paramIndex == Sanitizer.ALL_PARAMS) {
            return "all";
        }
        if (paramIndex == Sanitizer.NO_PARAM) {
            return "none";
        }
        return String.valueOf(paramIndex);
    }

    private String formatEdgeMeta(DFSEdge edge) {
        if (edge == null) {
            return "";
        }
        String type = edge.getType();
        String conf = edge.getConfidence();
        if (type == null || type.trim().isEmpty()) {
            type = "unknown";
        }
        if (conf == null || conf.trim().isEmpty()) {
            return type;
        }
        return type + "/" + conf;
    }

    private void showDetailForRowAsync(int row) {
        int seq = detailSeq.incrementAndGet();
        detailTextArea.setText("loading...");
        UiExecutor.runAsync(() -> {
            String detail = buildDetailText(row);
            UiExecutor.runOnEdt(() -> {
                if (seq != detailSeq.get() || !isDisplayable()) {
                    return;
                }
                detailTextArea.setText(detail);
                detailTextArea.setCaretPosition(0);
            });
        });
    }

    private String buildDetailText(int row) {
        if (originalTaintResults == null || row >= originalTaintResults.size() || row < 0) {
            return "无法获取详细信息";
        }

        TaintResult taintResult = originalTaintResults.get(row);
        StringBuilder detailText = new StringBuilder();

        // 显示基本信息
        detailText.append("==================== 污点分析详情 ===================\n");
        detailText.append("序号: ").append(row + 1).append("\n");

        DFSResult dfsResult = taintResult.getDfsResult();
        if (dfsResult != null) {
            // Source信息
            if (dfsResult.getSource() != null) {
                detailText.append("Source类: ").append(dfsResult.getSource().getClassReference().getName()).append("\n");
                detailText.append("Source方法: ").append(dfsResult.getSource().getName()).append("\n");
                detailText.append("Source描述: ").append(dfsResult.getSource().getDesc()).append("\n");
            }

            // Sink信息
            if (dfsResult.getSink() != null) {
                detailText.append("Sink类: ").append(dfsResult.getSink().getClassReference().getName()).append("\n");
                detailText.append("Sink方法: ").append(dfsResult.getSink().getName()).append("\n");
                detailText.append("Sink描述: ").append(dfsResult.getSink().getDesc()).append("\n");
            }

            detailText.append("调用链深度: ").append(dfsResult.getDepth()).append("\n");
            detailText.append("分析模式: ");
            switch (dfsResult.getMode()) {
                case DFSResult.FROM_SOURCE_TO_SINK:
                    detailText.append("从Source到Sink");
                    break;
                case DFSResult.FROM_SINK_TO_SOURCE:
                    detailText.append("从Sink到Source");
                    break;
                case DFSResult.FROM_SOURCE_TO_ALL:
                    detailText.append("从Source到所有可能点");
                    break;
                default:
                    detailText.append("未知模式");
            }
            detailText.append("\n");
            detailText.append("置信度: ").append(taintResult.isLowConfidence() ? "低" : "高").append("\n");
            detailText.append("\n");

            // 调用链详情
            detailText.append("==================== 调用链详情 ===================\n");
            List<MethodReference.Handle> methodList = dfsResult.getMethodList();
            List<DFSEdge> edges = dfsResult.getEdges();
            if (methodList != null && !methodList.isEmpty()) {
                for (int i = 0; i < methodList.size(); i++) {
                    MethodReference.Handle method = methodList.get(i);
                    detailText.append(String.format("[%d] %s.%s%s\n",
                            i + 1,
                            method.getClassReference().getName(),
                            method.getName(),
                            method.getDesc()));
                    if (edges != null && i < edges.size()) {
                        DFSEdge edge = edges.get(i);
                        String meta = formatEdgeMeta(edge);
                        if (!meta.isEmpty()) {
                            detailText.append("    edge: ").append(meta).append("\n");
                        }
                        String evidence = edge == null ? null : edge.getEvidence();
                        if (evidence != null && !evidence.trim().isEmpty()) {
                            detailText.append("    evidence: ").append(evidence).append("\n");
                        }
                    }
                }
            } else {
                detailText.append("无调用链信息\n");
            }
        }

        detailText.append("\n==================== 污点分析过程 ===================\n");
        // 显示污点分析的详细文本
        String taintText = taintResult.getTaintText();
        if (taintText != null && !taintText.trim().isEmpty()) {
            detailText.append(taintText);
        } else {
            detailText.append("无污点分析过程信息");
        }
        return detailText.toString();
    }

    private TaintLoad buildTaintLoad(List<TaintResult> taintResults) {
        if (taintResults == null || taintResults.isEmpty()) {
            return new TaintLoad(new Object[0][0], "无污点分析结果");
        }
        List<Object[]> rows = new ArrayList<>(taintResults.size());
        long passedCount = 0;
        long lowConfidenceCount = 0;
        for (int i = 0; i < taintResults.size(); i++) {
            TaintResult result = taintResults.get(i);
            if (result == null) {
                continue;
            }
            if (result.isSuccess()) {
                if (result.isLowConfidence()) {
                    lowConfidenceCount++;
                } else {
                    passedCount++;
                }
            }
            DFSResult dfsResult = result.getDfsResult();
            if (dfsResult == null) {
                continue;
            }
            String sourceClass = "";
            String sourceMethod = "";
            String sinkClass = "";
            String sinkMethod = "";

            if (dfsResult.getSource() != null) {
                sourceClass = dfsResult.getSource().getClassReference().getName();
                sourceMethod = dfsResult.getSource().getName();
            }

            if (dfsResult.getSink() != null) {
                sinkClass = dfsResult.getSink().getClassReference().getName();
                sinkMethod = dfsResult.getSink().getName();
            }

            String analysisResult = result.isSuccess() ? "通过" : "未通过";
            if (result.isLowConfidence()) {
                analysisResult += "(低置信)";
            }

            Object[] rowData = {
                    i + 1,
                    sourceClass,
                    sourceMethod,
                    sinkClass,
                    sinkMethod,
                    dfsResult.getDepth(),
                    analysisResult
            };
            rows.add(rowData);
        }
        String summary = String.format("总计: %d 条调用链, 通过: %d 条, 低置信: %d 条, 未通过: %d 条",
                taintResults.size(),
                passedCount,
                lowConfidenceCount,
                taintResults.size() - passedCount - lowConfidenceCount);
        return new TaintLoad(rows.toArray(new Object[0][]), summary);
    }

    private SanitizerLoad buildSanitizerLoad() {
        try {
            SanitizerRule rule = ModelRegistry.getSanitizerRule();
            List<Sanitizer> rules = rule.getRules();
            if (rules == null || rules.isEmpty()) {
                return new SanitizerLoad(new Object[0][0], "Sanitizer规则数量: 0 条");
            }
            List<Object[]> rows = new ArrayList<>(rules.size());
            for (Sanitizer sanitizer : rules) {
                String paramLabel = formatParamLabel(sanitizer.getParamIndex());
                rows.add(new Object[]{
                        sanitizer.getClassName(),
                        sanitizer.getMethodName(),
                        sanitizer.getMethodDesc(),
                        paramLabel
                });
            }
            return new SanitizerLoad(rows.toArray(new Object[0][]),
                    String.format("Sanitizer规则数量: %d 条", rules.size()));
        } catch (Exception e) {
            logger.error("加载Sanitizer规则失败: {}", e.getMessage());
            return new SanitizerLoad(new Object[0][0], "加载Sanitizer规则失败");
        }
    }

    private void applyRows(DefaultTableModel model,
                           JTable table,
                           Object[][] rows,
                           String[] columns,
                           int[] widths) {
        if (model == null) {
            return;
        }
        final Object[][] finalRows = rows == null ? new Object[0][0] : rows;
        model.setRowCount(0);
        if (finalRows.length == 0) {
            return;
        }
        if (finalRows.length <= LOAD_FAST_PATH_LIMIT) {
            model.setDataVector(finalRows, columns);
            applyColumnWidths(table, widths);
            return;
        }
        model.setColumnIdentifiers(columns);
        applyColumnWidths(table, widths);
        final int total = finalRows.length;
        final int[] index = {0};
        Runnable batch = new Runnable() {
            @Override
            public void run() {
                int start = index[0];
                int end = Math.min(start + LOAD_BATCH_SIZE, total);
                for (int i = start; i < end; i++) {
                    model.addRow(finalRows[i]);
                }
                index[0] = end;
                if (end < total) {
                    SwingUtilities.invokeLater(this);
                }
            }
        };
        batch.run();
    }

    private void applyColumnWidths(JTable table, int[] widths) {
        if (table == null || widths == null) {
            return;
        }
        int count = Math.min(table.getColumnCount(), widths.length);
        for (int i = 0; i < count; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    private static final class TaintLoad {
        private final Object[][] rows;
        private final String summaryText;

        private TaintLoad(Object[][] rows, String summaryText) {
            this.rows = rows;
            this.summaryText = summaryText;
        }
    }

    private static final class SanitizerLoad {
        private final Object[][] rows;
        private final String labelText;

        private SanitizerLoad(Object[][] rows, String labelText) {
            this.rows = rows;
            this.labelText = labelText;
        }
    }

    private void exportResults() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出污点分析结果");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "文本文件 (*.txt)", "txt"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File selected = fileChooser.getSelectedFile();
            UiExecutor.runAsync(() -> {
                try {
                    java.io.File file = selected;
                    if (!file.getName().endsWith(".txt")) {
                        file = new java.io.File(file.getAbsolutePath() + ".txt");
                    }
                    StringBuilder exportContent = new StringBuilder();
                    exportContent.append("污点分析结果导出\n");
                    exportContent.append("导出时间: ").append(new java.util.Date()).append("\n\n");
                    if (originalTaintResults != null) {
                        for (int i = 0; i < originalTaintResults.size(); i++) {
                            exportContent.append("==================== 结果 ").append(i + 1).append(" ===================\n");
                            TaintResult result = originalTaintResults.get(i);

                            if (result.getDfsResult() != null) {
                                DFSResult dfs = result.getDfsResult();
                                if (dfs.getSource() != null) {
                                    exportContent.append("Source: ").append(dfs.getSource().getClassReference().getName())
                                            .append(".").append(dfs.getSource().getName()).append("\n");
                                }
                                if (dfs.getSink() != null) {
                                    exportContent.append("Sink: ").append(dfs.getSink().getClassReference().getName())
                                            .append(".").append(dfs.getSink().getName()).append("\n");
                                }
                                exportContent.append("深度: ").append(dfs.getDepth()).append("\n");
                            }

                            if (result.getTaintText() != null) {
                                exportContent.append("分析过程:\n").append(result.getTaintText()).append("\n");
                            }
                            exportContent.append("\n");
                        }
                    }
                    java.nio.file.Files.write(file.toPath(),
                            exportContent.toString().getBytes(StandardCharsets.UTF_8));
                    java.io.File finalFile = file;
                    UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(this, "导出成功: " +
                            finalFile.getAbsolutePath(), "导出完成", JOptionPane.INFORMATION_MESSAGE));

                } catch (Exception ex) {
                    logger.error("导出失败: {}", ex.getMessage());
                    UiExecutor.runOnEdt(() -> JOptionPane.showMessageDialog(this, "导出失败: " +
                            ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE));
                }
            });
        }
    }

    // 静态方法用于显示对话框
    public static void showTaintResults(Frame parent, List<TaintResult> taintResults) {
        SwingUtilities.invokeLater(() -> {
            new TaintResultDialog(parent, taintResults);
        });
    }
}
