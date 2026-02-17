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
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.CommonBlacklistUtil;
import me.n1ar4.jar.analyzer.utils.CommonWhitelistUtil;
import me.n1ar4.jar.analyzer.utils.ListParser;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public final class StartToolPanel extends JPanel {
    private static final Logger logger = LogManager.getLogger();
    private static final Color PANEL_LINE = new Color(0xD8D8D8);
    private static final com.sun.management.OperatingSystemMXBean OS_BEAN = resolveOsBean();
    private static final Icon LIST_ICON = loadScaledIcon("img/list.png", 14, 14);
    private static final Icon AUTHOR_4RA1N_ICON = loadScaledIcon("img/au.png", 56, 56);
    private static final Icon AUTHOR_NEWNEW_ICON = loadScaledIcon("img/purplenewnew.jpg", 56, 56);
    private static final Icon GITHUB_ICON = loadScaledIcon("img/github.png", 13, 13);

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
    private final JButton editBuildBlacklistButton = new JButton("Edit Build Blacklist");
    private final JButton editBuildWhitelistButton = new JButton("Edit Build Whitelist");
    private final JLabel blacklistSummaryValue = new JLabel();
    private final JLabel whitelistSummaryValue = new JLabel();
    private final ResourceMonitorPanel statusMonitorPanel = new ResourceMonitorPanel();
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JLabel buildStatusValue = new JLabel("0%");

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

        JPanel snapshotPanel = new JPanel(new GridBagLayout());
        snapshotPanel.setBorder(BorderFactory.createTitledBorder("Build Snapshot"));
        snapshotPanel.setMinimumSize(new Dimension(220, 250));
        GridBagConstraints snapshotGbc = new GridBagConstraints();
        snapshotGbc.gridx = 0;
        snapshotGbc.anchor = GridBagConstraints.NORTHWEST;
        snapshotGbc.weightx = 1.0;
        snapshotGbc.weighty = 0.0;
        snapshotGbc.fill = GridBagConstraints.HORIZONTAL;
        snapshotGbc.insets = new Insets(0, 0, 8, 0);
        snapshotGbc.gridy = 0;
        snapshotPanel.add(buildFilterPanel(), snapshotGbc);
        snapshotGbc.gridy = 1;
        snapshotGbc.weighty = 0.0;
        snapshotGbc.fill = GridBagConstraints.HORIZONTAL;
        snapshotGbc.insets = new Insets(0, 0, 8, 0);
        snapshotPanel.add(buildSnapshotMetricsPanel(), snapshotGbc);
        snapshotGbc.gridy = 2;
        snapshotGbc.weighty = 0.0;
        snapshotGbc.fill = GridBagConstraints.HORIZONTAL;
        snapshotGbc.insets = new Insets(0, 0, 0, 0);
        snapshotPanel.add(buildAuthorPanel(), snapshotGbc);

        JPanel statusPanel = new JPanel(new BorderLayout(6, 6));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Status"));
        statusPanel.setMinimumSize(new Dimension(220, 130));
        JPanel progressRow = new JPanel(new BorderLayout(6, 0));
        progressRow.add(new JLabel("progress"), BorderLayout.WEST);
        progressRow.add(progressBar, BorderLayout.CENTER);
        buildStatusValue.setHorizontalAlignment(SwingConstants.RIGHT);
        progressRow.add(buildStatusValue, BorderLayout.EAST);
        statusMonitorPanel.setMinimumSize(new Dimension(10, 96));
        statusMonitorPanel.setPreferredSize(new Dimension(10, 124));
        statusPanel.add(statusMonitorPanel, BorderLayout.CENTER);
        statusPanel.add(progressRow, BorderLayout.SOUTH);
        settingsPanel.setMinimumSize(new Dimension(220, 165));

        JPanel stackPanel = new JPanel(new GridBagLayout());
        GridBagConstraints stackGbc = new GridBagConstraints();
        stackGbc.gridx = 0;
        stackGbc.weightx = 1.0;
        stackGbc.anchor = GridBagConstraints.NORTHWEST;
        stackGbc.fill = GridBagConstraints.HORIZONTAL;
        stackGbc.insets = new Insets(0, 0, 8, 0);
        stackGbc.gridy = 0;
        stackPanel.add(settingsPanel, stackGbc);
        stackGbc.gridy = 1;
        stackPanel.add(snapshotPanel, stackGbc);
        stackGbc.gridy = 2;
        stackGbc.weighty = 1.0;
        stackGbc.insets = new Insets(0, 0, 0, 0);
        stackGbc.fill = GridBagConstraints.BOTH;
        stackPanel.add(statusPanel, stackGbc);

        add(stackPanel, BorderLayout.CENTER);
        bindFilterActions();
        refreshFilterSummary();
        statusMonitorPanel.updateSample(readCpuUsage(), readMemoryUsage());
        applyLanguage();
    }

    private JPanel buildFilterPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Build Filters"));

        if (LIST_ICON != null) {
            editBuildBlacklistButton.setIcon(LIST_ICON);
            editBuildWhitelistButton.setIcon(LIST_ICON);
        }
        editBuildBlacklistButton.setFocusable(false);
        editBuildWhitelistButton.setFocusable(false);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actions.add(editBuildBlacklistButton);
        actions.add(editBuildWhitelistButton);
        panel.add(actions, BorderLayout.NORTH);

        JPanel summary = new JPanel(new GridLayout(2, 1, 0, 2));
        summary.add(blacklistSummaryValue);
        summary.add(whitelistSummaryValue);
        panel.add(summary, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSnapshotMetricsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createLineBorder(PANEL_LINE));
        int row = 0;
        row = addSnapshotRow(panel, row, "engine", engineStatusValue);
        row = addSnapshotRow(panel, row, "jar", totalJarValue);
        row = addSnapshotRow(panel, row, "class", totalClassValue);
        row = addSnapshotRow(panel, row, "method", totalMethodValue);
        row = addSnapshotRow(panel, row, "edge", totalEdgeValue);
        row = addSnapshotRow(panel, row, "db", dbSizeValue);
        return panel;
    }

    private int addSnapshotRow(JPanel panel, int row, String key, JLabel value) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.weightx = 0.35;
        left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(4, 8, 4, 8);
        panel.add(new JLabel(key), left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 0.65;
        right.anchor = GridBagConstraints.WEST;
        right.insets = new Insets(4, 8, 4, 8);
        panel.add(value, right);
        return row + 1;
    }

    private JPanel buildAuthorPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 0));
        panel.setBorder(BorderFactory.createTitledBorder("Authors"));
        panel.add(createAuthorCard("4ra1n", AUTHOR_4RA1N_ICON, Const.authorUrl));
        panel.add(createAuthorCard("NewNew", AUTHOR_NEWNEW_ICON, Const.coAuthorUrl));
        return panel;
    }

    private JPanel createAuthorCard(String name, Icon avatarIcon, String url) {
        JPanel card = new JPanel(new BorderLayout(6, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PANEL_LINE),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));

        JLabel avatar = new JLabel();
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.setPreferredSize(new java.awt.Dimension(56, 56));
        avatar.setMinimumSize(new java.awt.Dimension(56, 56));
        avatar.setMaximumSize(new java.awt.Dimension(56, 56));
        if (avatarIcon != null) {
            avatar.setIcon(avatarIcon);
        } else {
            avatar.setText(name.substring(0, 1));
        }

        JLabel nameLabel = new JLabel(name);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton linkButton = new JButton("GitHub");
        linkButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        linkButton.setFocusable(false);
        linkButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (GITHUB_ICON != null) {
            linkButton.setIcon(GITHUB_ICON);
        }
        linkButton.addActionListener(e -> openUrl(url));

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.add(nameLabel);
        right.add(Box.createVerticalStrut(4));
        right.add(linkButton);

        card.add(avatar, BorderLayout.WEST);
        card.add(right, BorderLayout.CENTER);
        return card;
    }

    private void bindFilterActions() {
        editBuildBlacklistButton.addActionListener(e -> showCommonListEditor(false));
        editBuildWhitelistButton.addActionListener(e -> showCommonListEditor(true));
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
        applyBuildProgress(snapshot.buildProgress(), snapshot.statusText());
        statusMonitorPanel.updateSample(readCpuUsage(), readMemoryUsage());
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

    private void showCommonListEditor(boolean whitelist) {
        String title = whitelist
                ? SwingI18n.tr("编辑构建白名单", "Edit Build Whitelist")
                : SwingI18n.tr("编辑构建黑名单", "Edit Build Blacklist");
        JDialog dialog = createDialog(title);
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea classArea = new JTextArea();
        JTextArea jarArea = new JTextArea();
        classArea.setText(buildPlainText(whitelist
                ? CommonWhitelistUtil.getClassPrefixes()
                : CommonBlacklistUtil.getClassPrefixes()));
        jarArea.setText(buildPlainText(whitelist
                ? CommonWhitelistUtil.getJarPrefixes()
                : CommonBlacklistUtil.getJarPrefixes()));

        JScrollPane classScroll = new JScrollPane(classArea);
        JScrollPane jarScroll = new JScrollPane(jarArea);
        classScroll.setBorder(BorderFactory.createLineBorder(PANEL_LINE));
        jarScroll.setBorder(BorderFactory.createLineBorder(PANEL_LINE));

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(new JLabel(SwingI18n.tr("类 / 包 列表", "Class / Package List")));
        center.add(classScroll);
        center.add(Box.createVerticalStrut(6));
        center.add(new JLabel(SwingI18n.tr("Jar 前缀列表", "Jar Prefix List")));
        center.add(jarScroll);
        center.add(Box.createVerticalStrut(6));
        center.add(new JLabel(SwingI18n.tr(
                "支持 #、//、/* 注释；支持 ; 分隔；* 会自动清理。",
                "Supports #,//,/* comments, ';' split and trailing '*' cleanup."
        )));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton cancel = new JButton(SwingI18n.tr("取消", "Cancel"));
        JButton save = new JButton(SwingI18n.tr("保存", "Save"));
        actions.add(cancel);
        actions.add(save);

        cancel.addActionListener(e -> dialog.dispose());
        save.addActionListener(e -> {
            try {
                ArrayList<String> classList = ListParser.parse(safe(classArea.getText()));
                ArrayList<String> jarList = parseJarList(jarArea.getText());
                if (whitelist) {
                    CommonWhitelistUtil.saveClassPrefixes(classList);
                    CommonWhitelistUtil.saveJarPrefixes(jarList);
                } else {
                    CommonBlacklistUtil.saveClassPrefixes(classList);
                    CommonBlacklistUtil.saveJarPrefixes(jarList);
                }
                refreshFilterSummary();
                dialog.dispose();
            } catch (Throwable ex) {
                logger.warn("save build list failed: {}", ex.toString());
            }
        });

        panel.add(center, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        dialog.setContentPane(panel);
        dialog.setSize(640, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JDialog createDialog(String title) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        if (owner instanceof java.awt.Frame frame) {
            return new JDialog(frame, title, true);
        }
        if (owner instanceof Dialog dialog) {
            return new JDialog(dialog, title, true);
        }
        return new JDialog((java.awt.Frame) null, title, true);
    }

    private void refreshFilterSummary() {
        int blacklistClass = safeSize(CommonBlacklistUtil.getClassPrefixes());
        int blacklistJar = safeSize(CommonBlacklistUtil.getJarPrefixes());
        int whitelistClass = safeSize(CommonWhitelistUtil.getClassPrefixes());
        int whitelistJar = safeSize(CommonWhitelistUtil.getJarPrefixes());
        blacklistSummaryValue.setText(SwingI18n.tr(
                "黑名单：类/包 " + blacklistClass + "，Jar " + blacklistJar,
                "blacklist: class/pkg " + blacklistClass + ", jar " + blacklistJar
        ));
        whitelistSummaryValue.setText(SwingI18n.tr(
                "白名单：类/包 " + whitelistClass + "，Jar " + whitelistJar,
                "whitelist: class/pkg " + whitelistClass + ", jar " + whitelistJar
        ));
    }

    private void openUrl(String url) {
        try {
            if (!Desktop.isDesktopSupported() || url == null || url.isBlank()) {
                return;
            }
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception ex) {
            logger.warn("open author url failed: {}", ex.toString());
        }
    }

    private void applyBuildProgress(int progress, String statusText) {
        int value = Math.max(0, Math.min(100, progress));
        progressBar.setValue(value);
        String raw = safe(statusText).trim();
        if (raw.isEmpty()) {
            raw = value >= 100
                    ? SwingI18n.tr("就绪", "ready")
                    : SwingI18n.tr("运行中", "running");
        }
        buildStatusValue.setText(value + "%");
        progressBar.setToolTipText(raw);
        buildStatusValue.setToolTipText(raw);
    }

    public void applyLanguage() {
        SwingI18n.localizeComponentTree(this);
        SwingI18n.setupBrowseButton(inputBrowseButton, inputPathText, "选择输入路径", "Browse input path");
        SwingI18n.setupBrowseButton(runtimeBrowseButton, runtimePathText, "选择运行时路径", "Browse runtime path");
        refreshFilterSummary();
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

    private static double readCpuUsage() {
        try {
            if (OS_BEAN != null) {
                double value = OS_BEAN.getSystemCpuLoad();
                if (value < 0 || Double.isNaN(value)) {
                    value = OS_BEAN.getProcessCpuLoad();
                }
                if (!Double.isNaN(value) && value >= 0) {
                    return Math.max(0.0, Math.min(1.0, value));
                }
            }
        } catch (Throwable ignored) {
        }
        return -1.0;
    }

    private static double readMemoryUsage() {
        try {
            if (OS_BEAN != null) {
                long total = OS_BEAN.getTotalPhysicalMemorySize();
                long free = OS_BEAN.getFreePhysicalMemorySize();
                if (total > 0 && free >= 0) {
                    double value = 1.0 - ((double) free / (double) total);
                    return Math.max(0.0, Math.min(1.0, value));
                }
            }
        } catch (Throwable ignored) {
        }
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long used = runtime.totalMemory() - runtime.freeMemory();
        if (max <= 0) {
            return -1.0;
        }
        double value = (double) used / (double) max;
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static int safeSize(List<String> list) {
        return list == null ? 0 : list.size();
    }

    private static ArrayList<String> parseJarList(String text) {
        if (text == null) {
            return new ArrayList<>();
        }
        String[] temp = text.trim().split("\n");
        if (temp.length == 0) {
            return new ArrayList<>();
        }
        ArrayList<String> list = new ArrayList<>();
        for (String s : temp) {
            if (s == null) {
                continue;
            }
            s = s.trim().toLowerCase();
            if (s.isEmpty()) {
                continue;
            }
            if (s.endsWith("\r")) {
                s = s.substring(0, s.length() - 1);
            }
            if (s.startsWith("#") || s.startsWith("/*") || s.startsWith("//")) {
                continue;
            }
            if (s.contains(";")) {
                String[] items = s.split(";");
                for (String item : items) {
                    if (item == null) {
                        continue;
                    }
                    String value = item.trim().toLowerCase();
                    if (value.isEmpty()) {
                        continue;
                    }
                    while (value.endsWith("*")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    if (!value.isEmpty()) {
                        list.add(value);
                    }
                }
                continue;
            }
            while (s.endsWith("*")) {
                s = s.substring(0, s.length() - 1);
            }
            if (!s.isEmpty()) {
                list.add(s);
            }
        }
        return list;
    }

    private static String buildPlainText(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String v = value.trim();
            if (v.isEmpty()) {
                continue;
            }
            sb.append(v).append("\n");
        }
        return sb.toString();
    }

    private static Icon loadScaledIcon(String path, int width, int height) {
        try (InputStream is = StartToolPanel.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                return null;
            }
            Image image = ImageIO.read(is);
            if (image == null) {
                return null;
            }
            Image scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static com.sun.management.OperatingSystemMXBean resolveOsBean() {
        try {
            java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean osBean) {
                return osBean;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static final class ResourceMonitorPanel extends JPanel {
        private static final int MAX_POINTS = 260;
        private static final Color CPU_FILL = new Color(126, 196, 255, 95);
        private static final Color CPU_LINE = new Color(92, 171, 245, 215);
        private static final Color MEM_FILL = new Color(164, 244, 182, 90);
        private static final Color MEM_LINE = new Color(90, 194, 118, 215);
        private static final Font INFO_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
        private final List<Double> cpuHistory = new ArrayList<>();
        private final List<Double> memoryHistory = new ArrayList<>();
        private double cpuCurrent;
        private double memoryCurrent;

        ResourceMonitorPanel() {
            setBorder(BorderFactory.createLineBorder(PANEL_LINE));
            setOpaque(true);
            setBackground(resolvePanelBackground());
        }

        void updateSample(double cpu, double memory) {
            if (cpu < 0) {
                cpu = cpuCurrent;
            }
            if (memory < 0) {
                memory = memoryCurrent;
            }
            cpuCurrent = smooth(cpuCurrent, clamp(cpu));
            memoryCurrent = smooth(memoryCurrent, clamp(memory));
            append(cpuHistory, cpuCurrent);
            append(memoryHistory, memoryCurrent);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = Math.max(1, getWidth());
            int h = Math.max(1, getHeight());
            int leftGaugeW = 18;
            int rightGaugeW = 18;
            int labelBandH = 18;
            int chartX = leftGaugeW;
            int chartY = labelBandH + 1;
            int chartW = Math.max(10, w - leftGaugeW - rightGaugeW);
            int chartH = Math.max(8, h - chartY - 2);
            Palette palette = resolvePalette();

            g2.setColor(palette.background());
            g2.fillRect(0, 0, w, h);
            g2.setColor(palette.chartBackground());
            g2.fillRect(chartX, chartY, chartW, chartH);

            drawGrid(g2, chartX, chartY, chartW, chartH, palette.grid());
            drawArea(g2, memoryHistory, memoryCurrent, chartX, chartY, chartW, chartH, MEM_FILL, MEM_LINE);
            drawArea(g2, cpuHistory, cpuCurrent, chartX, chartY, chartW, chartH, CPU_FILL, CPU_LINE);
            drawGauge(g2, 1, chartY, leftGaugeW - 2, chartH, cpuCurrent, palette.gaugeBackground(), CPU_LINE);
            drawGauge(g2, chartX + chartW + 1, chartY, rightGaugeW - 2, chartH, memoryCurrent, palette.gaugeBackground(), MEM_LINE);

            g2.setColor(palette.border());
            g2.drawRect(chartX, chartY, chartW - 1, chartH - 1);
            g2.setFont(INFO_FONT);
            drawTopLabels(g2, chartX, 0, chartW, labelBandH, cpuCurrent, memoryCurrent, CPU_LINE, MEM_LINE);
            g2.dispose();
        }

        private static void drawGrid(Graphics2D g2, int x, int y, int w, int h, Color gridColor) {
            g2.setColor(gridColor);
            for (int gx = x; gx < x + w; gx += 16) {
                g2.drawLine(gx, y, gx, y + h - 1);
            }
            for (int gy = y; gy < y + h; gy += 12) {
                g2.drawLine(x, gy, x + w - 1, gy);
            }
        }

        private static void drawArea(
                Graphics2D g2,
                List<Double> history,
                double current,
                int x,
                int y,
                int w,
                int h,
                Color fill,
                Color line) {
            if (w <= 2 || h <= 2) {
                return;
            }
            int points = Math.min(Math.max(2, w), history.size());
            if (points < 2) {
                points = 2;
            }
            Path2D.Double area = new Path2D.Double();
            Path2D.Double path = new Path2D.Double();
            area.moveTo(x, y + h - 1);
            for (int i = 0; i < points; i++) {
                int idx = history.size() - points + i;
                double value = idx >= 0 ? history.get(idx) : current;
                int px = x + (int) ((i / (double) (points - 1)) * (w - 1));
                int py = y + h - 1 - (int) (value * (h - 1));
                if (i == 0) {
                    path.moveTo(px, py);
                    area.lineTo(px, py);
                } else {
                    path.lineTo(px, py);
                    area.lineTo(px, py);
                }
            }
            area.lineTo(x + w - 1, y + h - 1);
            area.closePath();
            g2.setColor(fill);
            g2.fill(area);
            g2.setColor(line);
            g2.draw(path);
        }

        private static void drawGauge(
                Graphics2D g2,
                int x,
                int y,
                int w,
                int h,
                double value,
                Color gaugeBackground,
                Color gaugeColor) {
            int safeW = Math.max(6, w);
            int safeH = Math.max(8, h);
            g2.setColor(gaugeBackground);
            g2.fillRect(x, y, safeW, safeH);
            int fillH = (int) Math.round(safeH * value);
            int fillY = y + safeH - fillH;
            g2.setColor(gaugeColor);
            g2.fillRect(x + 1, fillY, Math.max(1, safeW - 2), Math.max(1, fillH));
            g2.setColor(withAlpha(gaugeColor, 190));
            g2.drawRect(x, y, safeW - 1, safeH - 1);
        }

        private static void drawTopLabels(
                Graphics2D g2,
                int chartX,
                int chartY,
                int chartW,
                int labelH,
                double cpu,
                double mem,
                Color cpuColor,
                Color memColor) {
            String cpuText = "CPU " + (int) Math.round(cpu * 100.0) + "%";
            String memText = "MEM " + (int) Math.round(mem * 100.0) + "%";
            var fm = g2.getFontMetrics();
            int leftPadding = 6;
            int rightPadding = 6;
            int cpuW = fm.stringWidth(cpuText);
            int memW = fm.stringWidth(memText);
            int splitGap = 10;
            int cpuX = chartX + leftPadding;
            int memX = chartX + chartW - memW - rightPadding;
            int baselineY = chartY + ((labelH - fm.getHeight()) / 2) + fm.getAscent();
            if (cpuX + cpuW + splitGap <= memX) {
                g2.setColor(cpuColor);
                g2.drawString(cpuText, cpuX, baselineY);
                g2.setColor(memColor);
                g2.drawString(memText, memX, baselineY);
                return;
            }

            int cpuPercent = (int) Math.round(cpu * 100.0);
            int memPercent = (int) Math.round(mem * 100.0);
            String[] cpuVariants = {
                "CPU " + cpuPercent + "%",
                "CPU" + cpuPercent + "%",
                "C" + cpuPercent + "%"
            };
            String[] memVariants = {
                "MEM " + memPercent + "%",
                "MEM" + memPercent + "%",
                "M" + memPercent + "%"
            };
            String compactCpu = cpuVariants[cpuVariants.length - 1];
            String compactMem = memVariants[memVariants.length - 1];
            int inlineGap = 8;
            int available = Math.max(1, chartW - leftPadding - rightPadding);
            for (int i = 0; i < cpuVariants.length; i++) {
                int variantW = fm.stringWidth(cpuVariants[i]) + inlineGap + fm.stringWidth(memVariants[i]);
                if (variantW <= available) {
                    compactCpu = cpuVariants[i];
                    compactMem = memVariants[i];
                    break;
                }
            }
            int compactCpuX = chartX + leftPadding;
            int compactMemX = compactCpuX + fm.stringWidth(compactCpu) + inlineGap;
            g2.setColor(cpuColor);
            g2.drawString(compactCpu, compactCpuX, baselineY);
            g2.setColor(memColor);
            g2.drawString(compactMem, compactMemX, baselineY);
        }

        private static Palette resolvePalette() {
            Color panel = resolvePanelBackground();
            boolean dark = luminance(panel) < 0.45;
            Color background = dark
                    ? blend(panel, Color.BLACK, 0.10)
                    : blend(panel, Color.WHITE, 0.14);
            Color chartBackground = dark
                    ? blend(panel, new Color(0x0F1D32), 0.32)
                    : blend(panel, new Color(0xEAF3FF), 0.68);
            Color grid = dark
                    ? withAlpha(blend(panel, Color.WHITE, 0.55), 82)
                    : withAlpha(blend(panel, Color.BLACK, 0.60), 46);
            Color border = dark
                    ? blend(panel, Color.WHITE, 0.34)
                    : blend(panel, Color.BLACK, 0.24);
            Color gaugeBg = dark
                    ? blend(panel, Color.BLACK, 0.26)
                    : blend(panel, Color.WHITE, 0.36);
            Color label = dark
                    ? withAlpha(Color.WHITE, 218)
                    : withAlpha(new Color(0x1F2A36), 218);
            return new Palette(background, chartBackground, grid, border, gaugeBg, label);
        }

        private static Color resolvePanelBackground() {
            Color panelBg = UIManager.getColor("Panel.background");
            if (panelBg == null) {
                panelBg = UIManager.getColor("control");
            }
            return panelBg == null ? new Color(0xECECEC) : panelBg;
        }

        private static double luminance(Color c) {
            return (0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue()) / 255.0;
        }

        private static Color blend(Color base, Color target, double ratio) {
            double r = Math.max(0.0, Math.min(1.0, ratio));
            int rr = (int) Math.round(base.getRed() * (1.0 - r) + target.getRed() * r);
            int gg = (int) Math.round(base.getGreen() * (1.0 - r) + target.getGreen() * r);
            int bb = (int) Math.round(base.getBlue() * (1.0 - r) + target.getBlue() * r);
            return new Color(clampChannel(rr), clampChannel(gg), clampChannel(bb));
        }

        private static Color withAlpha(Color color, int alpha) {
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), clampChannel(alpha));
        }

        private static int clampChannel(int value) {
            return Math.max(0, Math.min(255, value));
        }

        private static void append(List<Double> history, double value) {
            history.add(value);
            if (history.size() > MAX_POINTS) {
                history.remove(0);
            }
        }

        private static double smooth(double oldValue, double newValue) {
            return oldValue <= 0.0 ? newValue : (oldValue * 0.72 + newValue * 0.28);
        }

        private static double clamp(double value) {
            return Math.max(0.0, Math.min(1.0, value));
        }

        private record Palette(
                Color background,
                Color chartBackground,
                Color grid,
                Color border,
                Color gaugeBackground,
                Color label
        ) {
        }
    }
}
