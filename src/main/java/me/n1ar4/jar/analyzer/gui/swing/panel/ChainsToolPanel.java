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

import me.n1ar4.jar.analyzer.gui.runtime.api.RuntimeFacades;
import me.n1ar4.jar.analyzer.gui.runtime.model.ChainsSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ChainsSnapshotDto;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

public final class ChainsToolPanel extends JPanel {
    private final JRadioButton fromSinkRadio = new JRadioButton("from sink");
    private final JRadioButton fromSourceRadio = new JRadioButton("from source");

    private final JTextField sinkClassText = new JTextField();
    private final JTextField sinkMethodText = new JTextField();
    private final JTextField sinkDescText = new JTextField();

    private final JTextField sourceClassText = new JTextField();
    private final JTextField sourceMethodText = new JTextField();
    private final JTextField sourceDescText = new JTextField();

    private final JRadioButton sourceNullRadio = new JRadioButton("null source list");
    private final JRadioButton sourceEnableRadio = new JRadioButton("specified source");
    private final JCheckBox onlyWebSourceBox = new JCheckBox("source only from spring/servlet");

    private final JSpinner maxDepthSpin = new JSpinner(new SpinnerNumberModel(10, 1, 200, 1));
    private final JSpinner maxResultSpin = new JSpinner(new SpinnerNumberModel(30, 1, 5000, 1));
    private final JCheckBox taintEnabledBox = new JCheckBox("taint verify");
    private final JTextField blacklistText = new JTextField();
    private final JComboBox<String> minConfidenceCombo = new JComboBox<>(new String[]{"low", "medium", "high"});
    private final JCheckBox showEdgeMetaBox = new JCheckBox("show edge meta");
    private final JCheckBox summaryBox = new JCheckBox("summary");
    private final JTextField taintSeedParamText = new JTextField();
    private final JCheckBox taintSeedStrictBox = new JCheckBox("seed strict");

    private final JLabel dfsCountValue = new JLabel("0");
    private final JLabel taintCountValue = new JLabel("0");
    private final JTextArea hintArea = new JTextArea();

    private volatile boolean syncing;

    public ChainsToolPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initUi();
    }

    private void initUi() {
        ButtonGroup rootGroup = new ButtonGroup();
        rootGroup.add(fromSinkRadio);
        rootGroup.add(fromSourceRadio);
        fromSinkRadio.setSelected(true);

        ButtonGroup sourceMode = new ButtonGroup();
        sourceMode.add(sourceNullRadio);
        sourceMode.add(sourceEnableRadio);
        sourceEnableRadio.setSelected(true);

        JPanel head = new JPanel(new GridLayout(2, 1, 4, 4));
        head.setBorder(BorderFactory.createTitledBorder("Analyze Mode"));
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row1.add(fromSinkRadio);
        row1.add(fromSourceRadio);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row2.add(sourceNullRadio);
        row2.add(sourceEnableRadio);
        row2.add(onlyWebSourceBox);
        head.add(row1);
        head.add(row2);

        JPanel sinkPanel = new JPanel(new GridLayout(3, 2, 4, 4));
        sinkPanel.setBorder(BorderFactory.createTitledBorder("Sink"));
        sinkPanel.add(new JLabel("class"));
        sinkPanel.add(sinkClassText);
        sinkPanel.add(new JLabel("method"));
        sinkPanel.add(sinkMethodText);
        sinkPanel.add(new JLabel("desc"));
        sinkPanel.add(sinkDescText);

        JPanel sourcePanel = new JPanel(new GridLayout(3, 2, 4, 4));
        sourcePanel.setBorder(BorderFactory.createTitledBorder("Source"));
        sourcePanel.add(new JLabel("class"));
        sourcePanel.add(sourceClassText);
        sourcePanel.add(new JLabel("method"));
        sourcePanel.add(sourceMethodText);
        sourcePanel.add(new JLabel("desc"));
        sourcePanel.add(sourceDescText);

        JPanel configPanel = new JPanel(new GridLayout(5, 2, 4, 4));
        configPanel.setBorder(BorderFactory.createTitledBorder("Config"));
        configPanel.add(new JLabel("max depth"));
        configPanel.add(maxDepthSpin);
        configPanel.add(new JLabel("max results"));
        configPanel.add(maxResultSpin);
        configPanel.add(new JLabel("min edge confidence"));
        configPanel.add(minConfidenceCombo);
        configPanel.add(showEdgeMetaBox);
        configPanel.add(summaryBox);
        configPanel.add(taintEnabledBox);
        configPanel.add(new JLabel(""));

        JPanel taintSeedPanel = new JPanel(new GridLayout(1, 4, 4, 4));
        taintSeedPanel.setBorder(BorderFactory.createTitledBorder("Taint Seed"));
        taintSeedPanel.add(new JLabel("seed param index"));
        taintSeedPanel.add(taintSeedParamText);
        taintSeedPanel.add(taintSeedStrictBox);
        taintSeedPanel.add(new JLabel(""));

        JPanel blacklistPanel = new JPanel(new BorderLayout(6, 0));
        blacklistPanel.setBorder(BorderFactory.createTitledBorder("Blacklist (split by ';')"));
        blacklistPanel.add(blacklistText, BorderLayout.CENTER);

        JPanel middle = new JPanel(new GridLayout(2, 2, 6, 6));
        middle.add(sinkPanel);
        middle.add(sourcePanel);
        middle.add(configPanel);
        middle.add(taintSeedPanel);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton applyBtn = new JButton("Apply");
        applyBtn.addActionListener(e -> applySettings());
        JButton startDfsBtn = new JButton("Start DFS");
        startDfsBtn.addActionListener(e -> {
            applySettings();
            RuntimeFacades.chains().startDfs();
        });
        JButton startTaintBtn = new JButton("Start Taint");
        startTaintBtn.addActionListener(e -> {
            applySettings();
            RuntimeFacades.chains().startTaint();
        });
        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> RuntimeFacades.chains().clearResults());
        JButton openDfsBtn = new JButton("View DFS");
        openDfsBtn.addActionListener(e -> RuntimeFacades.tooling().openChainsDfsResult());
        JButton openTaintBtn = new JButton("View Taint");
        openTaintBtn.addActionListener(e -> RuntimeFacades.tooling().openChainsTaintResult());
        JButton advanceBtn = new JButton("Advanced");
        advanceBtn.addActionListener(e -> RuntimeFacades.chains().openAdvanceSettings());
        actions.add(applyBtn);
        actions.add(startDfsBtn);
        actions.add(startTaintBtn);
        actions.add(clearBtn);
        actions.add(openDfsBtn);
        actions.add(openTaintBtn);
        actions.add(advanceBtn);

        JPanel status = new JPanel(new GridLayout(1, 4, 4, 4));
        status.setBorder(BorderFactory.createTitledBorder("Status"));
        status.add(new JLabel("dfs"));
        status.add(dfsCountValue);
        status.add(new JLabel("taint"));
        status.add(taintCountValue);

        hintArea.setEditable(false);
        hintArea.setRows(3);
        hintArea.setText("Right click in search/note to send source or sink if needed.");
        JScrollPane hintScroll = new JScrollPane(hintArea);
        hintScroll.setBorder(BorderFactory.createTitledBorder("Hint"));

        JPanel north = new JPanel(new BorderLayout(6, 6));
        north.add(head, BorderLayout.NORTH);
        north.add(middle, BorderLayout.CENTER);
        north.add(blacklistPanel, BorderLayout.SOUTH);

        JPanel south = new JPanel(new BorderLayout(6, 6));
        south.add(actions, BorderLayout.NORTH);
        south.add(status, BorderLayout.CENTER);
        south.add(hintScroll, BorderLayout.SOUTH);

        add(north, BorderLayout.NORTH);
        add(south, BorderLayout.CENTER);
    }

    public void applySnapshot(ChainsSnapshotDto snapshot) {
        if (snapshot == null) {
            return;
        }
        ChainsSettingsDto settings = snapshot.settings();
        if (settings != null) {
            syncing = true;
            try {
                fromSinkRadio.setSelected(settings.sinkSelected());
                fromSourceRadio.setSelected(settings.sourceSelected());
                setTextIfIdle(sinkClassText, settings.sinkClass());
                setTextIfIdle(sinkMethodText, settings.sinkMethod());
                setTextIfIdle(sinkDescText, settings.sinkDesc());
                setTextIfIdle(sourceClassText, settings.sourceClass());
                setTextIfIdle(sourceMethodText, settings.sourceMethod());
                setTextIfIdle(sourceDescText, settings.sourceDesc());
                sourceNullRadio.setSelected(settings.sourceNull());
                sourceEnableRadio.setSelected(settings.sourceEnabled());
                onlyWebSourceBox.setSelected(settings.onlyFromWeb());
                maxDepthSpin.setValue(Math.max(1, settings.maxDepth()));
                maxResultSpin.setValue(Math.max(1, settings.maxResultLimit()));
                taintEnabledBox.setSelected(settings.taintEnabled());
                setTextIfIdle(blacklistText, settings.blacklist());
                minConfidenceCombo.setSelectedItem(safe(settings.minEdgeConfidence()).isBlank()
                        ? "low" : settings.minEdgeConfidence());
                showEdgeMetaBox.setSelected(settings.showEdgeMeta());
                summaryBox.setSelected(settings.summaryEnabled());
                setTextIfIdle(taintSeedParamText, settings.taintSeedParam() == null ? "" : String.valueOf(settings.taintSeedParam()));
                taintSeedStrictBox.setSelected(settings.taintSeedStrict());
            } finally {
                syncing = false;
            }
        }
        dfsCountValue.setText(String.valueOf(Math.max(0, snapshot.dfsCount())));
        taintCountValue.setText(String.valueOf(Math.max(0, snapshot.taintCount())));
    }

    private void applySettings() {
        if (syncing) {
            return;
        }
        Integer seedParam = parseNullableInt(taintSeedParamText.getText());
        RuntimeFacades.chains().apply(new ChainsSettingsDto(
                fromSinkRadio.isSelected(),
                fromSourceRadio.isSelected(),
                safe(sinkClassText.getText()),
                safe(sinkMethodText.getText()),
                safe(sinkDescText.getText()),
                safe(sourceClassText.getText()),
                safe(sourceMethodText.getText()),
                safe(sourceDescText.getText()),
                sourceNullRadio.isSelected(),
                sourceEnableRadio.isSelected(),
                (Integer) maxDepthSpin.getValue(),
                onlyWebSourceBox.isSelected(),
                taintEnabledBox.isSelected(),
                safe(blacklistText.getText()),
                safe((String) minConfidenceCombo.getSelectedItem()),
                showEdgeMetaBox.isSelected(),
                summaryBox.isSelected(),
                seedParam,
                taintSeedStrictBox.isSelected(),
                (Integer) maxResultSpin.getValue()
        ));
    }

    private static Integer parseNullableInt(String value) {
        String text = safe(value).trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void setTextIfIdle(JTextField field, String value) {
        if (field == null || field.isFocusOwner()) {
            return;
        }
        String next = safe(value);
        if (!next.equals(field.getText())) {
            field.setText(next);
        }
    }
}

