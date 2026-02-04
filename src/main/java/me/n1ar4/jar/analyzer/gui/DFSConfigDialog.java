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

import com.alibaba.fastjson2.JSON;
import me.n1ar4.jar.analyzer.chains.SinkModel;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.gui.util.LogUtil;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.taint.TaintCache;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public class DFSConfigDialog extends JDialog {
    private int maxLimit;
    private String blacklist;
    private String minEdgeConfidence;
    private String minRuleTier;
    private boolean showEdgeMeta;
    private boolean summaryEnabled;
    private Integer taintSeedParam;
    private boolean taintSeedStrict;
    private boolean saved = false;

    public DFSConfigDialog(JFrame parent,
                           int currentLimit,
                           String currentBlacklist,
                           String currentMinEdgeConfidence,
                           String currentRuleTier,
                           boolean currentShowEdgeMeta,
                           boolean currentSummaryEnabled,
                           Integer currentSeedParam,
                           boolean currentSeedStrict) {
        super(parent, "DFS Advance Settings (高级设置)", true);
        this.maxLimit = currentLimit;
        this.blacklist = currentBlacklist;
        this.minEdgeConfidence = currentMinEdgeConfidence;
        this.minRuleTier = currentRuleTier;
        this.showEdgeMeta = currentShowEdgeMeta;
        this.summaryEnabled = currentSummaryEnabled;
        this.taintSeedParam = currentSeedParam;
        this.taintSeedStrict = currentSeedStrict;
        initUI();
    }

    private void initUI() {
        setSize(700, 640);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();

        // Title Area
        JLabel titleLabel = new JLabel("DFS Algorithm Configuration / DFS 算法配置");
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(titleLabel, gbc);

        // Limit Setting
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.3;
        mainPanel.add(new JLabel("Max Results Limit (最大分析数量):"), gbc);

        JSpinner limitSpin = new JSpinner();
        limitSpin.setValue(maxLimit);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.7;
        mainPanel.add(limitSpin, gbc);

        // Limit Description
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 5, 10, 5);
        JLabel limitDesc = new JLabel("<html><font color='gray'>Stop analysis after finding N chains (Default 30)<br>找到 N 条利用链后停止分析（默认 30）</font></html>");
        mainPanel.add(limitDesc, gbc);

        // Blacklist Label
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(new JLabel("Class Blacklist (类名黑名单):"), gbc);

        // Blacklist Text Area
        JTextArea blackArea = new JTextArea();
        blackArea.setText(blacklist);
        blackArea.setRows(10);
        JScrollPane scrollPane = new JScrollPane(blackArea);
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        mainPanel.add(scrollPane, gbc);

        // Blacklist Description
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 10, 5);
        JLabel blackDesc = new JLabel("<html><font color='gray'>Ignored classes during DFS (One class per line)<br>DFS 分析过程中忽略的类（每行一个类名）</font></html>");
        mainPanel.add(blackDesc, gbc);

        // Min Edge Confidence
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 0.3;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(new JLabel("Min Edge Confidence (最小边置信度):"), gbc);

        JComboBox<String> edgeConfidenceBox = new JComboBox<>(new String[]{
                MethodCallMeta.CONF_LOW,
                MethodCallMeta.CONF_MEDIUM,
                MethodCallMeta.CONF_HIGH
        });
        if (minEdgeConfidence != null && !minEdgeConfidence.trim().isEmpty()) {
            edgeConfidenceBox.setSelectedItem(minEdgeConfidence);
        }
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.weightx = 0.7;
        mainPanel.add(edgeConfidenceBox, gbc);

        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 5, 10, 5);
        JLabel edgeDesc = new JLabel("<html><font color='gray'>Filter chains by edge confidence (low/medium/high)<br>按边置信度筛选调用链</font></html>");
        mainPanel.add(edgeDesc, gbc);

        // Summary Pruning
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 0.3;
        mainPanel.add(new JLabel("Summary Pruning (Enable):"), gbc);

        JCheckBox summaryCheck = new JCheckBox("Enable", summaryEnabled);
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.weightx = 0.7;
        mainPanel.add(summaryCheck, gbc);

        gbc.gridx = 1;
        gbc.gridy = 8;
        gbc.insets = new Insets(0, 5, 10, 5);
        JLabel summaryDesc = new JLabel("<html><font color='gray'>Speed up DFS using summary reachability (may reduce recall)<br>Use with caution if you need maximum coverage</font></html>");
        mainPanel.add(summaryDesc, gbc);

        // Min Rule Tier
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 0.3;
        mainPanel.add(new JLabel("Min Rule Tier (最小规则层级):"), gbc);

        JComboBox<String> ruleTierBox = new JComboBox<>(new String[]{
                SinkModel.TIER_CLUE,
                SinkModel.TIER_SOFT,
                SinkModel.TIER_HARD
        });
        if (minRuleTier != null && !minRuleTier.trim().isEmpty()) {
            ruleTierBox.setSelectedItem(minRuleTier);
        }
        gbc.gridx = 1;
        gbc.gridy = 9;
        gbc.weightx = 0.7;
        mainPanel.add(ruleTierBox, gbc);

        gbc.gridx = 1;
        gbc.gridy = 10;
        gbc.insets = new Insets(0, 5, 10, 5);
        JLabel tierDesc = new JLabel("<html><font color='gray'>Filter by rule tier (clue/soft/hard)<br>按规则层级筛选调用链</font></html>");
        mainPanel.add(tierDesc, gbc);

        // Show Edge Meta
        gbc.gridx = 0;
        gbc.gridy = 11;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 0.3;
        mainPanel.add(new JLabel("Show Edge Meta (显示边元信息):"), gbc);

        JCheckBox showEdgeMetaCheck = new JCheckBox("Enable", showEdgeMeta);
        gbc.gridx = 1;
        gbc.gridy = 11;
        gbc.weightx = 0.7;
        mainPanel.add(showEdgeMetaCheck, gbc);

        // Taint Seed Param
        gbc.gridx = 0;
        gbc.gridy = 12;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 0.3;
        mainPanel.add(new JLabel("Taint Seed Param (污点起点参数):"), gbc);

        boolean seedEnabled = taintSeedParam != null;
        int seedValue = seedEnabled ? taintSeedParam : -3;
        JCheckBox seedEnableCheck = new JCheckBox("Enable (启用)", seedEnabled);
        JSpinner seedSpinner = new JSpinner(new SpinnerNumberModel(seedValue, -3, 128, 1));
        JCheckBox seedStrictCheck = new JCheckBox("Strict (严格)", taintSeedStrict);
        seedSpinner.setEnabled(seedEnabled);
        seedStrictCheck.setEnabled(seedEnabled);
        seedEnableCheck.addItemListener(e -> {
            boolean enabled = seedEnableCheck.isSelected();
            seedSpinner.setEnabled(enabled);
            seedStrictCheck.setEnabled(enabled);
        });
        JPanel seedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        seedPanel.add(seedEnableCheck);
        seedPanel.add(seedSpinner);
        seedPanel.add(seedStrictCheck);
        gbc.gridx = 1;
        gbc.gridy = 12;
        gbc.weightx = 0.7;
        mainPanel.add(seedPanel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 13;
        gbc.insets = new Insets(0, 5, 10, 5);
        JLabel seedDesc = new JLabel("<html><font color='gray'>-3: none  -2: this  -1: all  &gt;=0: param index<br>配置固定的污点起点参数</font></html>");
        mainPanel.add(seedDesc, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton exportBtn = new JButton("Export JSON (导出结果)");
        exportBtn.addActionListener(e -> {
            if (TaintCache.dfsCache.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No DFS results to export.\n请先运行 DFS 分析。");
                return;
            }
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save JSON Lines File");
            fileChooser.setSelectedFile(new File("dfs-results.json"));
            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                UiExecutor.runAsync(() -> {
                    try {
                        ArrayList<String> lines = new ArrayList<>();
                        for (DFSResult result : TaintCache.dfsCache) {
                            lines.add(JSON.toJSONString(result));
                        }
                        Files.write(Paths.get(fileToSave.getAbsolutePath()), lines, StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                        UiExecutor.runOnEdt(() ->
                                JOptionPane.showMessageDialog(this, "Export Success: " + fileToSave.getAbsolutePath()));
                    } catch (Exception ex) {
                        LogUtil.error("Export failed: " + ex.getMessage());
                        UiExecutor.runOnEdt(() ->
                                JOptionPane.showMessageDialog(this, "Export Failed: " + ex.getMessage()));
                    }
                });
            }
        });

        JButton cancelBtn = new JButton("Cancel (取消)");
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = new JButton("Save Config (保存配置)");
        saveBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        saveBtn.addActionListener(e -> {
            try {
                this.maxLimit = (int) limitSpin.getValue();
                if (this.maxLimit <= 0) {
                    JOptionPane.showMessageDialog(this, "Limit must be > 0");
                    return;
                }
                this.blacklist = blackArea.getText();
                this.minEdgeConfidence = (String) edgeConfidenceBox.getSelectedItem();
                this.minRuleTier = (String) ruleTierBox.getSelectedItem();
                this.showEdgeMeta = showEdgeMetaCheck.isSelected();
                this.summaryEnabled = summaryCheck.isSelected();
                if (seedEnableCheck.isSelected()) {
                    this.taintSeedParam = (Integer) seedSpinner.getValue();
                    this.taintSeedStrict = seedStrictCheck.isSelected();
                } else {
                    this.taintSeedParam = null;
                    this.taintSeedStrict = false;
                }
                this.saved = true;
                LogUtil.info("DFS config saved");
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid Input");
            }
        });

        btnPanel.add(exportBtn);
        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    public int getMaxLimit() {
        return maxLimit;
    }

    public String getBlacklist() {
        return blacklist;
    }

    public String getMinEdgeConfidence() {
        return minEdgeConfidence;
    }

    public String getMinRuleTier() {
        return minRuleTier;
    }

    public boolean isShowEdgeMeta() {
        return showEdgeMeta;
    }

    public boolean isSummaryEnabled() {
        return summaryEnabled;
    }

    public Integer getTaintSeedParam() {
        return taintSeedParam;
    }

    public boolean isTaintSeedStrict() {
        return taintSeedStrict;
    }

    public boolean isSaved() {
        return saved;
    }
}
