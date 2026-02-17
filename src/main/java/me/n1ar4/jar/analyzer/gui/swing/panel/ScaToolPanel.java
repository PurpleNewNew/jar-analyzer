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
import me.n1ar4.jar.analyzer.gui.runtime.model.ScaOutputMode;
import me.n1ar4.jar.analyzer.gui.runtime.model.ScaSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ScaSnapshotDto;
import me.n1ar4.jar.analyzer.gui.swing.SwingI18n;
import me.n1ar4.jar.analyzer.gui.swing.SwingTextSync;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;

public final class ScaToolPanel extends JPanel {
    private final JCheckBox scanLog4jBox = new JCheckBox("Apache Log4j2");
    private final JCheckBox scanShiroBox = new JCheckBox("Apache Shiro");
    private final JCheckBox scanFastjsonBox = new JCheckBox("Fastjson");

    private final JTextField inputPathText = new JTextField();
    private final JTextField outputFileText = new JTextField();
    private final JButton inputBrowseButton = new JButton();
    private final JButton outputBrowseButton = new JButton();
    private final JRadioButton outConsole = new JRadioButton("Console");
    private final JRadioButton outHtml = new JRadioButton("HTML");
    private final JTextArea logArea = new JTextArea();

    private volatile boolean syncing;

    public ScaToolPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initUi();
    }

    private void initUi() {
        JPanel modules = new JPanel(new GridLayout(1, 3, 4, 4));
        modules.setBorder(BorderFactory.createTitledBorder("Modules"));
        modules.add(scanLog4jBox);
        modules.add(scanShiroBox);
        modules.add(scanFastjsonBox);

        JPanel ioPanel = new JPanel(new GridLayout(3, 1, 4, 4));
        ioPanel.setBorder(BorderFactory.createTitledBorder("Input / Output"));
        ioPanel.add(pathRow("input", inputPathText, inputBrowseButton, this::chooseInputPath));
        ioPanel.add(pathRow("output", outputFileText, outputBrowseButton, this::chooseOutputFile));

        JPanel outputModePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        ButtonGroup group = new ButtonGroup();
        group.add(outConsole);
        group.add(outHtml);
        outHtml.setSelected(true);
        outputModePanel.add(new JLabel("output mode"));
        outputModePanel.add(outConsole);
        outputModePanel.add(outHtml);
        ioPanel.add(outputModePanel);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton applyBtn = new JButton("Apply");
        applyBtn.addActionListener(e -> applySettings());
        JButton startBtn = new JButton("Start");
        startBtn.addActionListener(e -> {
            applySettings();
            RuntimeFacades.sca().start();
        });
        JButton openResultBtn = new JButton("Open Result");
        openResultBtn.addActionListener(e -> RuntimeFacades.sca().openResult());
        actionPanel.add(applyBtn);
        actionPanel.add(startBtn);
        actionPanel.add(openResultBtn);

        logArea.setEditable(false);
        logArea.setRows(10);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("SCA Log"));

        JPanel north = new JPanel(new BorderLayout(6, 6));
        north.add(modules, BorderLayout.NORTH);
        north.add(ioPanel, BorderLayout.CENTER);
        north.add(actionPanel, BorderLayout.SOUTH);

        add(north, BorderLayout.NORTH);
        add(logScroll, BorderLayout.CENTER);
        applyLanguage();
    }

    public void applySnapshot(ScaSnapshotDto snapshot) {
        if (snapshot == null) {
            return;
        }
        ScaSettingsDto settings = snapshot.settings();
        if (settings != null) {
            syncing = true;
            try {
                scanLog4jBox.setSelected(settings.scanLog4j());
                scanShiroBox.setSelected(settings.scanShiro());
                scanFastjsonBox.setSelected(settings.scanFastjson());
                setTextIfIdle(inputPathText, settings.inputPath());
                setTextIfIdle(outputFileText, settings.outputFile());
                if (settings.outputMode() == ScaOutputMode.CONSOLE) {
                    outConsole.setSelected(true);
                } else {
                    outHtml.setSelected(true);
                }
            } finally {
                syncing = false;
            }
        }
        logArea.setText(safe(snapshot.logTail()));
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private JPanel pathRow(String label, JTextField textField, JButton browseButton, Runnable chooseAction) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.add(new JLabel(label), BorderLayout.WEST);
        row.add(textField, BorderLayout.CENTER);
        SwingI18n.setupBrowseButton(browseButton, textField, "选择路径", "Browse path");
        browseButton.addActionListener(e -> chooseAction.run());
        row.add(browseButton, BorderLayout.EAST);
        return row;
    }

    private void chooseInputPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(SwingI18n.tr("选择 Jar 文件或目录", "Select Jar File or Directory"));
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

    private void chooseOutputFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(SwingI18n.tr("选择 SCA 报告输出文件", "Select SCA Report Output"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        String current = outputFileText.getText();
        if (current != null && !current.isBlank()) {
            chooser.setSelectedFile(new File(current));
        }
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            outputFileText.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void applySettings() {
        if (syncing) {
            return;
        }
        RuntimeFacades.sca().apply(new ScaSettingsDto(
                scanLog4jBox.isSelected(),
                scanShiroBox.isSelected(),
                scanFastjsonBox.isSelected(),
                safe(inputPathText.getText()),
                outConsole.isSelected() ? ScaOutputMode.CONSOLE : ScaOutputMode.HTML,
                safe(outputFileText.getText())
        ));
    }

    public void applyLanguage() {
        SwingI18n.localizeComponentTree(this);
        SwingI18n.setupBrowseButton(inputBrowseButton, inputPathText, "选择输入路径", "Browse input path");
        SwingI18n.setupBrowseButton(outputBrowseButton, outputFileText, "选择输出路径", "Browse output path");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void setTextIfIdle(JTextField field, String value) {
        SwingTextSync.setTextIfIdle(field, value);
    }
}
