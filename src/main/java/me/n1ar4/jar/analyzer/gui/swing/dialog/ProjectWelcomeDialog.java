/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.swing.dialog;

import me.n1ar4.jar.analyzer.gui.runtime.api.RuntimeFacades;
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingConfigSnapshotDto;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistryEntry;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistryService;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistrySnapshot;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class ProjectWelcomeDialog {
    private static final Logger logger = LogManager.getLogger();
    private static final DateTimeFormatter PROJECT_NAME_FORMAT = DateTimeFormatter.ofPattern("MMdd-HHmmss");

    private ProjectWelcomeDialog() {
    }

    public static void show(JFrame owner) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> show(owner));
            return;
        }

        JDialog dialog = new JDialog(owner, tr("开始", "Welcome"), true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(0, 0));

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 18, 24));

        JLabel title = new JLabel(tr("Jar Analyzer", "Jar Analyzer"));
        title.setHorizontalAlignment(SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(20f));

        JLabel desc = new JLabel(tr("选择项目模式后进入工作区", "Choose project mode before entering workspace"));
        desc.setHorizontalAlignment(SwingConstants.LEFT);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(desc);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton temporaryButton = new JButton(tr("新建临时项目", "New Temporary Project"));
        JButton createButton = new JButton(tr("新建项目", "New Project"));
        JButton openButton = new JButton(tr("打开项目", "Open Project"));

        temporaryButton.addActionListener(e -> {
            if (activateTemporary(dialog)) {
                dialog.dispose();
            }
        });
        createButton.addActionListener(e -> {
            if (createProject(dialog)) {
                dialog.dispose();
            }
        });
        openButton.addActionListener(e -> {
            if (openProject(dialog)) {
                dialog.dispose();
            }
        });

        actions.add(temporaryButton);
        actions.add(createButton);
        actions.add(openButton);

        root.add(header, BorderLayout.NORTH);
        root.add(actions, BorderLayout.CENTER);
        dialog.setContentPane(root);
        dialog.setMinimumSize(new Dimension(620, 200));
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static boolean activateTemporary(Component parent) {
        try {
            ProjectRegistryEntry entry = ProjectRegistryService.getInstance().activateTemporaryProject();
            syncBuildSettings(entry, true);
            return true;
        } catch (Exception ex) {
            logger.warn("activate temporary project fail: {}", ex.toString());
            JOptionPane.showMessageDialog(
                    parent,
                    tr("激活临时项目失败: ", "activate temporary project failed: ") + safe(ex.getMessage()),
                    tr("错误", "Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }

    private static boolean createProject(Component parent) {
        String suggested = "project-" + PROJECT_NAME_FORMAT.format(LocalDateTime.now());
        String input = JOptionPane.showInputDialog(
                parent,
                tr("输入项目名称", "Input project name"),
                suggested
        );
        if (input == null) {
            return false;
        }
        String alias = input.trim();
        if (alias.isBlank()) {
            alias = suggested;
        }
        try {
            ProjectRegistryEntry entry = ProjectRegistryService.getInstance().createProject(alias);
            syncBuildSettings(entry, true);
            return true;
        } catch (Exception ex) {
            logger.warn("create project fail: {}", ex.toString());
            JOptionPane.showMessageDialog(
                    parent,
                    tr("创建项目失败: ", "create project failed: ") + safe(ex.getMessage()),
                    tr("错误", "Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }

    private static boolean openProject(Component parent) {
        ProjectRegistrySnapshot snapshot = ProjectRegistryService.getInstance().snapshot();
        List<ProjectRegistryEntry> entries = snapshot.projects();
        if (entries == null || entries.isEmpty()) {
            JOptionPane.showMessageDialog(
                    parent,
                    tr("当前无可用项目，请先新建项目。", "No project found, please create one first."),
                    tr("提示", "Hint"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return false;
        }
        ProjectRegistryEntry selected = chooseProject(parent, entries);
        if (selected == null) {
            return false;
        }
        try {
            ProjectRegistryEntry entry = ProjectRegistryService.getInstance().switchActive(selected.projectKey());
            syncBuildSettings(entry, false);
            return true;
        } catch (Exception ex) {
            logger.warn("switch project fail: {}", ex.toString());
            JOptionPane.showMessageDialog(
                    parent,
                    tr("打开项目失败: ", "open project failed: ") + safe(ex.getMessage()),
                    tr("错误", "Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }

    private static ProjectRegistryEntry chooseProject(Component parent, List<ProjectRegistryEntry> entries) {
        JList<ProjectRegistryEntry> list = new JList<>(entries.toArray(new ProjectRegistryEntry[0]));
        list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(Math.min(entries.size(), 8));
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof ProjectRegistryEntry entry) {
                    String alias = safe(entry.alias());
                    if (alias.isBlank()) {
                        alias = safe(entry.projectKey());
                    }
                    String inputPath = safe(entry.inputPath());
                    if (inputPath.isBlank()) {
                        inputPath = tr("未设置输入路径", "no input path");
                    }
                    label.setText(alias + "  [" + safe(entry.projectKey()) + "]  -  " + inputPath);
                }
                return label;
            }
        });
        int selectedIndex = findActiveIndex(entries);
        if (selectedIndex >= 0) {
            list.setSelectedIndex(selectedIndex);
        } else {
            list.setSelectedIndex(0);
        }
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(720, 220));
        int result = JOptionPane.showConfirmDialog(
                parent,
                scroll,
                tr("选择项目", "Choose Project"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        return list.getSelectedValue();
    }

    private static int findActiveIndex(List<ProjectRegistryEntry> entries) {
        String activeKey = safe(ProjectRegistryService.getInstance().snapshot().activeProjectKey());
        for (int i = 0; i < entries.size(); i++) {
            ProjectRegistryEntry entry = entries.get(i);
            if (entry != null && activeKey.equals(safe(entry.projectKey()))) {
                return i;
            }
        }
        return -1;
    }

    private static void syncBuildSettings(ProjectRegistryEntry entry, boolean clearInputIfEmpty) {
        if (entry == null) {
            return;
        }
        BuildSnapshotDto snapshot = RuntimeFacades.build().snapshot();
        BuildSettingsDto current = snapshot == null || snapshot.settings() == null
                ? new BuildSettingsDto("", "", false, false, false)
                : snapshot.settings();

        String nextInput = safe(entry.inputPath());
        if (nextInput.isBlank()) {
            nextInput = clearInputIfEmpty ? "" : safe(current.activeInputPath());
        }
        String nextRuntime = safe(entry.runtimePath());
        if (nextRuntime.isBlank()) {
            nextRuntime = safe(current.sdkPath());
        }
        boolean resolveNested = entry.resolveNestedJars();
        if (safe(entry.inputPath()).isBlank() && safe(entry.runtimePath()).isBlank()) {
            resolveNested = current.resolveNestedJars();
        }
        RuntimeFacades.build().apply(new BuildSettingsDto(
                nextInput,
                nextRuntime,
                resolveNested,
                current.fixClassPath(),
                current.quickMode()
        ));
    }

    private static String tr(String zh, String en) {
        try {
            ToolingConfigSnapshotDto snapshot = RuntimeFacades.tooling().configSnapshot();
            String language = snapshot == null ? "zh" : safe(snapshot.language()).toLowerCase(Locale.ROOT);
            if ("en".equals(language)) {
                return safe(en);
            }
        } catch (Exception ignored) {
            // fall back to zh
        }
        return safe(zh);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
