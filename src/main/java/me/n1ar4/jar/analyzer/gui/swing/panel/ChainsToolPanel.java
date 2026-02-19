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
import me.n1ar4.jar.analyzer.gui.swing.SwingI18n;
import me.n1ar4.jar.analyzer.gui.swing.SwingTextSync;
import me.n1ar4.jar.analyzer.gui.swing.SwingUiApplyGuard;

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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

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
    private final JLabel backendStatusValue = new JLabel(SwingI18n.tr("就绪", "ready"));
    private final JTextArea hintArea = new JTextArea();
    private final SwingUiApplyGuard.Throttle snapshotThrottle = new SwingUiApplyGuard.Throttle();

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

        JPanel sourcePanel = pointPanel("Source", sourceClassText, sourceMethodText, sourceDescText);
        JPanel sinkPanel = pointPanel("Sink", sinkClassText, sinkMethodText, sinkDescText);

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

        tunePointField(taintSeedParamText);
        JPanel taintSeedPanel = new JPanel(new GridBagLayout());
        taintSeedPanel.setBorder(BorderFactory.createTitledBorder("Taint Seed"));
        GridBagConstraints tc = new GridBagConstraints();
        tc.insets = new Insets(2, 2, 2, 2);
        tc.fill = GridBagConstraints.HORIZONTAL;
        tc.anchor = GridBagConstraints.WEST;
        tc.gridx = 0;
        tc.gridy = 0;
        tc.weightx = 0.0;
        taintSeedPanel.add(new JLabel("seed param index"), tc);
        tc.gridx = 1;
        tc.weightx = 1.0;
        taintSeedPanel.add(taintSeedParamText, tc);
        tc.gridx = 0;
        tc.gridy = 1;
        tc.gridwidth = 2;
        tc.fill = GridBagConstraints.NONE;
        tc.weightx = 0.0;
        tc.insets = new Insets(4, 2, 2, 2);
        taintSeedPanel.add(taintSeedStrictBox, tc);
        tc.gridy = 2;
        tc.weighty = 1.0;
        tc.fill = GridBagConstraints.BOTH;
        taintSeedPanel.add(new JPanel(), tc);

        JPanel blacklistPanel = new JPanel(new BorderLayout(6, 0));
        blacklistPanel.setBorder(BorderFactory.createTitledBorder("Blacklist (split by ';')"));
        blacklistPanel.add(blacklistText, BorderLayout.CENTER);

        JPanel pointsRow = new JPanel(new GridLayout(1, 2, 6, 0));
        pointsRow.add(sourcePanel);
        pointsRow.add(sinkPanel);

        JPanel settingsRow = new JPanel(new GridLayout(1, 2, 6, 0));
        settingsRow.add(taintSeedPanel);
        settingsRow.add(configPanel);

        JPanel middle = new JPanel(new BorderLayout(0, 6));
        middle.add(pointsRow, BorderLayout.NORTH);
        middle.add(settingsRow, BorderLayout.CENTER);

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

        JPanel status = new JPanel(new BorderLayout(4, 4));
        status.setBorder(BorderFactory.createTitledBorder("Status"));
        JPanel counters = new JPanel(new GridLayout(1, 4, 4, 4));
        counters.add(new JLabel("dfs"));
        counters.add(dfsCountValue);
        counters.add(new JLabel("taint"));
        counters.add(taintCountValue);
        JPanel backend = new JPanel(new GridLayout(1, 2, 4, 4));
        backend.add(new JLabel("backend"));
        backend.add(backendStatusValue);
        status.add(counters, BorderLayout.NORTH);
        status.add(backend, BorderLayout.SOUTH);

        hintArea.setEditable(false);
        hintArea.setRows(3);
        hintArea.setText(SwingI18n.tr(
                "在 search/note 中右键可将方法设置为 source/sink。",
                "Right click in search/note to send source or sink if needed."));
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
        applyLanguage();
    }

    public void applySnapshot(ChainsSnapshotDto snapshot) {
        if (snapshot == null) {
            return;
        }
        if (!SwingUiApplyGuard.ensureEdt("ChainsToolPanel.applySnapshot", () -> applySnapshot(snapshot))) {
            return;
        }
        if (!snapshotThrottle.allow(SwingUiApplyGuard.fingerprint(snapshot))) {
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
        String backend = safe(snapshot.statusText()).trim();
        backendStatusValue.setText(backend.isEmpty() ? SwingI18n.tr("就绪", "ready") : backend);
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

    public void applyLanguage() {
        SwingI18n.localizeComponentTree(this);
        hintArea.setText(SwingI18n.tr(
                "在 search/note 中右键可将方法设置为 source/sink。",
                "Right click in search/note to send source or sink if needed."));
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

    private static JPanel pointPanel(
            String title,
            JTextField classField,
            JTextField methodField,
            JTextField descField
    ) {
        tunePointField(classField);
        tunePointField(methodField);
        tunePointField(descField);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 2, 2, 2);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;
        gc.weightx = 0.0;
        gc.gridx = 0;
        gc.gridy = 0;
        form.add(narrowLabel("class"), gc);
        gc.gridx = 1;
        gc.weightx = 1.0;
        form.add(classField, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        gc.weightx = 0.0;
        form.add(narrowLabel("method"), gc);
        gc.gridx = 1;
        gc.weightx = 1.0;
        form.add(methodField, gc);

        gc.gridx = 0;
        gc.gridy = 2;
        gc.weightx = 0.0;
        form.add(narrowLabel("desc"), gc);
        gc.gridx = 1;
        gc.weightx = 1.0;
        form.add(descField, gc);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(form, BorderLayout.NORTH);
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        panel.add(filler, BorderLayout.CENTER);
        return panel;
    }

    private static void tunePointField(JTextField field) {
        if (field == null) {
            return;
        }
        field.setPreferredSize(new Dimension(200, 22));
        field.setMinimumSize(new Dimension(120, 22));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
    }

    private static JLabel narrowLabel(String text) {
        JLabel label = new JLabel(text);
        Dimension size = new Dimension(46, 20);
        label.setPreferredSize(size);
        label.setMinimumSize(size);
        return label;
    }

    private static void setTextIfIdle(JTextField field, String value) {
        SwingTextSync.setTextIfIdle(field, value);
    }
}
