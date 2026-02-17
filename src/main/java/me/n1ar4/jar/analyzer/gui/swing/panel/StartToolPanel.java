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
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSnapshotDto;
import me.n1ar4.jar.analyzer.gui.swing.SwingI18n;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;

public final class StartToolPanel extends JPanel {
    private static final Logger logger = LogManager.getLogger();

    private final JTextField inputPathText = new JTextField();
    private final JTextField runtimePathText = new JTextField();
    private final JButton inputBrowseButton = new JButton();
    private final JButton runtimeBrowseButton = new JButton();
    private final JCheckBox resolveNestedJarsBox = new JCheckBox("resolve nested jars");
    private final JCheckBox autoFindRuntimeJarBox = new JCheckBox("auto find runtime jar");
    private final JCheckBox addRuntimeJarBox = new JCheckBox("add runtime jar");
    private final JCheckBox deleteTempBeforeBuildBox = new JCheckBox("delete temp before build");
    private final JCheckBox fixClassPathBox = new JCheckBox("fix class path");
    private final JCheckBox fixMethodImplBox = new JCheckBox("fix method impl");
    private final JCheckBox quickModeBox = new JCheckBox("quick mode");
    private final JLabel engineStatusValue = new JLabel("-");
    private final JLabel totalJarValue = new JLabel("0");
    private final JLabel totalClassValue = new JLabel("0");
    private final JLabel totalMethodValue = new JLabel("0");
    private final JLabel totalEdgeValue = new JLabel("0");
    private final JLabel dbSizeValue = new JLabel("0");
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JTextArea statusArea = new JTextArea();

    public StartToolPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initUi();
    }

    private void initUi() {
        JPanel settingsPanel = new JPanel(new BorderLayout(6, 6));
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Build Settings"));

        JPanel pathPanel = new JPanel(new GridLayout(2, 1, 6, 6));
        pathPanel.add(createPathRow("input", inputPathText, inputBrowseButton, this::chooseInputPath));
        pathPanel.add(createPathRow("runtime", runtimePathText, runtimeBrowseButton, this::chooseRuntimePath));
        settingsPanel.add(pathPanel, BorderLayout.NORTH);

        JPanel optionsPanel = new JPanel(new GridLayout(4, 2, 4, 4));
        optionsPanel.add(resolveNestedJarsBox);
        optionsPanel.add(autoFindRuntimeJarBox);
        optionsPanel.add(addRuntimeJarBox);
        optionsPanel.add(deleteTempBeforeBuildBox);
        optionsPanel.add(fixClassPathBox);
        optionsPanel.add(fixMethodImplBox);
        optionsPanel.add(quickModeBox);
        settingsPanel.add(optionsPanel, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> applySettings());
        JButton buildButton = new JButton("Start Build");
        buildButton.addActionListener(e -> {
            applySettings();
            RuntimeFacades.build().startBuild();
        });
        JButton clearButton = new JButton("Clear Cache");
        clearButton.addActionListener(e -> RuntimeFacades.build().clearCache());
        actionPanel.add(applyButton);
        actionPanel.add(buildButton);
        actionPanel.add(clearButton);
        settingsPanel.add(actionPanel, BorderLayout.SOUTH);

        JPanel snapshotPanel = new JPanel(new GridLayout(6, 2, 4, 4));
        snapshotPanel.setBorder(BorderFactory.createTitledBorder("Build Snapshot"));
        snapshotPanel.add(new JLabel("engine"));
        snapshotPanel.add(engineStatusValue);
        snapshotPanel.add(new JLabel("jar"));
        snapshotPanel.add(totalJarValue);
        snapshotPanel.add(new JLabel("class"));
        snapshotPanel.add(totalClassValue);
        snapshotPanel.add(new JLabel("method"));
        snapshotPanel.add(totalMethodValue);
        snapshotPanel.add(new JLabel("edge"));
        snapshotPanel.add(totalEdgeValue);
        snapshotPanel.add(new JLabel("db"));
        snapshotPanel.add(dbSizeValue);

        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setRows(6);
        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setBorder(BorderFactory.createLineBorder(new java.awt.Color(0xD8D8D8)));

        JPanel statusPanel = new JPanel(new BorderLayout(6, 6));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Status"));
        JPanel progressRow = new JPanel(new BorderLayout(6, 0));
        progressRow.add(new JLabel("progress"), BorderLayout.WEST);
        progressRow.add(progressBar, BorderLayout.CENTER);
        statusPanel.add(progressRow, BorderLayout.NORTH);
        statusPanel.add(statusScroll, BorderLayout.CENTER);

        add(settingsPanel, BorderLayout.NORTH);
        add(snapshotPanel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
        applyLanguage();
    }

    public void applySnapshot(BuildSnapshotDto snapshot) {
        if (snapshot == null) {
            return;
        }
        BuildSettingsDto settings = snapshot.settings();
        if (settings != null) {
            setTextIfIdle(inputPathText, settings.inputPath());
            setTextIfIdle(runtimePathText, settings.runtimePath());
            resolveNestedJarsBox.setSelected(settings.resolveNestedJars());
            autoFindRuntimeJarBox.setSelected(settings.autoFindRuntimeJar());
            addRuntimeJarBox.setSelected(settings.addRuntimeJar());
            deleteTempBeforeBuildBox.setSelected(settings.deleteTempBeforeBuild());
            fixClassPathBox.setSelected(settings.fixClassPath());
            fixMethodImplBox.setSelected(settings.fixMethodImpl());
            quickModeBox.setSelected(settings.quickMode());
        }
        engineStatusValue.setText(safe(snapshot.engineStatus()));
        totalJarValue.setText(safe(snapshot.totalJar()));
        totalClassValue.setText(safe(snapshot.totalClass()));
        totalMethodValue.setText(safe(snapshot.totalMethod()));
        totalEdgeValue.setText(safe(snapshot.totalEdge()));
        dbSizeValue.setText(safe(snapshot.databaseSize()));
        progressBar.setValue(Math.max(0, Math.min(100, snapshot.buildProgress())));
        statusArea.setText(safe(snapshot.statusText()));
    }

    private JPanel createPathRow(String label, JTextField textField, JButton button, Runnable chooserAction) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.add(new JLabel(label), BorderLayout.WEST);
        row.add(textField, BorderLayout.CENTER);
        SwingI18n.setupBrowseButton(button, textField, "选择路径", "Browse path");
        button.addActionListener(e -> chooserAction.run());
        row.add(button, BorderLayout.EAST);
        return row;
    }

    private void chooseInputPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(SwingI18n.tr("选择输入 Jar/目录", "Select Input Jar/Directory"));
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        String current = inputPathText.getText();
        if (current != null && !current.isBlank()) {
            chooser.setSelectedFile(new File(current));
        }
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            inputPathText.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void chooseRuntimePath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(SwingI18n.tr("选择运行时 Jar", "Select Runtime Jar"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        String current = runtimePathText.getText();
        if (current != null && !current.isBlank()) {
            chooser.setSelectedFile(new File(current));
        }
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            runtimePathText.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void applySettings() {
        try {
            BuildSettingsDto next = new BuildSettingsDto(
                    safe(inputPathText.getText()),
                    safe(runtimePathText.getText()),
                    resolveNestedJarsBox.isSelected(),
                    autoFindRuntimeJarBox.isSelected(),
                    addRuntimeJarBox.isSelected(),
                    deleteTempBeforeBuildBox.isSelected(),
                    fixClassPathBox.isSelected(),
                    fixMethodImplBox.isSelected(),
                    quickModeBox.isSelected()
            );
            RuntimeFacades.build().apply(next);
        } catch (Throwable ex) {
            logger.warn("apply build settings failed: {}", ex.toString());
        }
    }

    public void applyLanguage() {
        SwingI18n.localizeComponentTree(this);
        SwingI18n.setupBrowseButton(inputBrowseButton, inputPathText, "选择输入路径", "Browse input path");
        SwingI18n.setupBrowseButton(runtimeBrowseButton, runtimePathText, "选择运行时路径", "Browse runtime path");
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

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
