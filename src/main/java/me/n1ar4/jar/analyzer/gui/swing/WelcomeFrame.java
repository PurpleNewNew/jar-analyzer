/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.swing;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import me.n1ar4.jar.analyzer.gui.runtime.api.RuntimeFacades;
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSettingsDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingConfigSnapshotDto;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistryEntry;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistryService;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistrySnapshot;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class WelcomeFrame extends JFrame {
    private static final Logger logger = LogManager.getLogger();
    private static final DateTimeFormatter PROJECT_NAME_FORMAT = DateTimeFormatter.ofPattern("MMdd-HHmmss");
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int SIDEBAR_WIDTH = 240;

    private final DefaultListModel<ProjectRegistryEntry> projectListModel = new DefaultListModel<>();
    private final JList<ProjectRegistryEntry> projectList = new JList<>(projectListModel);

    public WelcomeFrame() {
        super(tr("欢迎访问 Jar Analyzer", "Welcome to Jar Analyzer"));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(860, 560));
        setSize(new Dimension(920, 620));
        setLocationRelativeTo(null);
        setResizable(true);
        setLayout(new BorderLayout(0, 0));

        add(buildSidebar(), BorderLayout.WEST);
        add(buildContent(), BorderLayout.CENTER);

        refreshProjectList();
    }

    // ── sidebar ──────────────────────────────────────────────────────────────

    private JPanel buildSidebar() {
        Color sidebarBg = deriveSidebarBackground();

        JPanel sidebar = new JPanel(new BorderLayout(0, 0));
        sidebar.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        sidebar.setBackground(sidebarBg);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIManager.getColor("Separator.foreground")));

        // top: branding
        JPanel brandPanel = new JPanel();
        brandPanel.setOpaque(false);
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 12, 18));

        JLabel appName = new JLabel("Jar Analyzer");
        appName.setFont(appName.getFont().deriveFont(Font.BOLD, 16f));
        appName.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandPanel.add(appName);

        JLabel versionLabel = new JLabel(Const.version);
        versionLabel.setFont(versionLabel.getFont().deriveFont(12f));
        versionLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandPanel.add(Box.createVerticalStrut(2));
        brandPanel.add(versionLabel);

        // tab: projects
        JPanel tabPanel = new JPanel();
        tabPanel.setOpaque(false);
        tabPanel.setLayout(new BoxLayout(tabPanel, BoxLayout.Y_AXIS));
        tabPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JPanel projectsTab = createSidebarTab(tr("项目", "Projects"), true);
        tabPanel.add(projectsTab);

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setOpaque(false);
        topSection.add(brandPanel, BorderLayout.NORTH);
        topSection.add(tabPanel, BorderLayout.CENTER);

        sidebar.add(topSection, BorderLayout.NORTH);

        // bottom: settings icon placeholder
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        bottomPanel.setOpaque(false);
        Icon settingsIcon = loadSvgIcon("icons/jadx/settings.svg", 16);
        if (settingsIcon != null) {
            JLabel settingsLabel = new JLabel(settingsIcon);
            settingsLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            settingsLabel.setToolTipText(tr("设置", "Settings"));
            bottomPanel.add(settingsLabel);
        }
        sidebar.add(bottomPanel, BorderLayout.SOUTH);

        return sidebar;
    }

    private JPanel createSidebarTab(String text, boolean selected) {
        Color selectedBg = UIManager.getColor("List.selectionBackground");
        if (selectedBg == null) {
            selectedBg = new Color(0xD0D4FC);
        }

        JPanel tab = new JPanel(new BorderLayout());
        tab.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        tab.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        if (selected) {
            tab.setBackground(selectedBg);
            tab.setOpaque(true);
        } else {
            tab.setOpaque(false);
        }

        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(14f));
        tab.add(label, BorderLayout.CENTER);
        return tab;
    }

    // ── content area ─────────────────────────────────────────────────────────

    private JPanel buildContent() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(30, 36, 24, 36));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // title
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 4, 0);
        JLabel title = new JLabel(tr("欢迎访问 Jar Analyzer", "Welcome to Jar Analyzer"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        content.add(title, gbc);

        // subtitle
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 24, 0);
        JLabel subtitle = new JLabel(tr(
                "从头创建新项目。打开历史项目继续工作。",
                "Create a new project from scratch. Open a recent project to continue."
        ));
        subtitle.setForeground(UIManager.getColor("Label.disabledForeground"));
        content.add(subtitle, gbc);

        // action buttons
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 20, 0);
        content.add(buildActionButtons(), gbc);

        // project list (fills remaining space)
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        content.add(buildProjectListPanel(), gbc);

        return content;
    }

    private JPanel buildActionButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        panel.setOpaque(false);

        panel.add(createActionCard(
                loadSvgIcon("icons/jadx/addFile.svg", 28),
                tr("新建项目", "New Project"),
                this::onNewProject
        ));
        panel.add(createActionCard(
                loadSvgIcon("icons/jadx/folder.svg", 28),
                tr("新建临时项目", "Temporary Project"),
                this::onNewTemporary
        ));

        return panel;
    }

    private JPanel createActionCard(Icon icon, String text, Runnable action) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setOpaque(false);

        if (icon != null) {
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(iconLabel);
            card.add(Box.createVerticalStrut(8));
        }

        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(12f));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(label);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setOpaque(true);
                card.setBackground(UIManager.getColor("List.selectionBackground"));
                card.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setOpaque(false);
                card.repaint();
            }
        });

        return card;
    }

    private JPanel buildProjectListPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setOpaque(false);

        projectList.setCellRenderer(new ProjectListCellRenderer());
        projectList.setFixedCellHeight(54);
        projectList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int idx = projectList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        openSelectedProject(projectListModel.getElementAt(idx));
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                showPopupIfNeeded(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showPopupIfNeeded(e);
            }
        });

        JScrollPane scroll = new JScrollPane(projectList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void showPopupIfNeeded(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        int idx = projectList.locationToIndex(e.getPoint());
        if (idx < 0) {
            return;
        }
        projectList.setSelectedIndex(idx);
        ProjectRegistryEntry entry = projectListModel.getElementAt(idx);

        JPopupMenu popup = new JPopupMenu();
        JMenuItem openItem = new JMenuItem(tr("打开", "Open"));
        openItem.addActionListener(a -> openSelectedProject(entry));
        popup.add(openItem);

        JMenuItem removeItem = new JMenuItem(tr("删除", "Remove"));
        removeItem.addActionListener(a -> removeProject(entry));
        popup.add(removeItem);

        popup.show(projectList, e.getX(), e.getY());
    }

    // ── actions ──────────────────────────────────────────────────────────────

    private void onNewProject() {
        String suggested = "project-" + PROJECT_NAME_FORMAT.format(LocalDateTime.now());
        String input = JOptionPane.showInputDialog(
                this,
                tr("输入项目名称", "Input project name"),
                suggested
        );
        if (input == null) {
            return;
        }
        String alias = input.trim();
        if (alias.isBlank()) {
            alias = suggested;
        }
        try {
            ProjectRegistryEntry entry = ProjectRegistryService.getInstance().createProject(alias);
            syncBuildSettings(entry, true);
            launchMainFrame();
        } catch (Exception ex) {
            logger.warn("create project fail: {}", ex.toString());
            JOptionPane.showMessageDialog(this,
                    tr("创建项目失败: ", "create project failed: ") + safe(ex.getMessage()),
                    tr("错误", "Error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onNewTemporary() {
        try {
            ProjectRegistryEntry entry = ProjectRegistryService.getInstance().activateTemporaryProject();
            syncBuildSettings(entry, true);
            launchMainFrame();
        } catch (Exception ex) {
            logger.warn("activate temporary project fail: {}", ex.toString());
            JOptionPane.showMessageDialog(this,
                    tr("激活临时项目失败: ", "activate temporary project failed: ") + safe(ex.getMessage()),
                    tr("错误", "Error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openSelectedProject(ProjectRegistryEntry entry) {
        if (entry == null) {
            return;
        }
        try {
            ProjectRegistryEntry activated = ProjectRegistryService.getInstance().switchActive(entry.projectKey());
            syncBuildSettings(activated, false);
            launchMainFrame();
        } catch (Exception ex) {
            logger.warn("switch project fail: {}", ex.toString());
            JOptionPane.showMessageDialog(this,
                    tr("打开项目失败: ", "open project failed: ") + safe(ex.getMessage()),
                    tr("错误", "Error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeProject(ProjectRegistryEntry entry) {
        if (entry == null) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                tr("确定删除项目 \"" + safe(entry.alias()) + "\" 吗？",
                        "Remove project \"" + safe(entry.alias()) + "\"?"),
                tr("确认", "Confirm"),
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            ProjectRegistryService.getInstance().remove(entry.projectKey(), true);
            refreshProjectList();
        } catch (Exception ex) {
            logger.warn("remove project fail: {}", ex.toString());
        }
    }

    // ── launch main frame ────────────────────────────────────────────────────

    private void launchMainFrame() {
        SwingMainFrame frame = new SwingMainFrame();
        SwingWindowAnchor.setFrame(frame);
        SwingWindowAnchor.setMasterComponent(frame.getContentPane());
        frame.setVisible(true);
        dispose();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void refreshProjectList() {
        projectListModel.clear();
        String temporaryKey = ActiveProjectContext.normalizeProjectKey(null);
        ProjectRegistrySnapshot snapshot = ProjectRegistryService.getInstance().snapshot();
        List<ProjectRegistryEntry> entries = snapshot.projects();
        if (entries != null) {
            entries.stream()
                    .filter(e -> !temporaryKey.equals(e.projectKey()))
                    .sorted(Comparator.comparingLong(ProjectRegistryEntry::updatedAt).reversed())
                    .forEach(projectListModel::addElement);
        }
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

    private static Color deriveSidebarBackground() {
        Color panelBg = UIManager.getColor("Panel.background");
        if (panelBg == null) {
            return new Color(0xF0F0F0);
        }
        float[] hsb = Color.RGBtoHSB(panelBg.getRed(), panelBg.getGreen(), panelBg.getBlue(), null);
        if (hsb[2] > 0.5f) {
            // light theme: slightly darker
            return panelBg.darker();
        } else {
            // dark theme: slightly lighter
            float b = Math.min(1.0f, hsb[2] + 0.05f);
            return Color.getHSBColor(hsb[0], hsb[1], b);
        }
    }

    private static Icon loadSvgIcon(String path, int size) {
        try {
            return new FlatSVGIcon(path, size, size);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String tr(String zh, String en) {
        try {
            ToolingConfigSnapshotDto snapshot = RuntimeFacades.tooling().configSnapshot();
            String language = snapshot == null ? "zh" : safe(snapshot.language()).toLowerCase(Locale.ROOT);
            if ("en".equals(language)) {
                return safe(en);
            }
        } catch (Exception ignored) {
        }
        return safe(zh);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    // ── cell renderer ────────────────────────────────────────────────────────

    private static final class ProjectListCellRenderer extends JPanel implements ListCellRenderer<ProjectRegistryEntry> {
        private final JLabel aliasLabel = new JLabel();
        private final JLabel pathLabel = new JLabel();
        private final JLabel timeLabel = new JLabel();

        ProjectListCellRenderer() {
            setLayout(new BorderLayout(8, 0));
            setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

            aliasLabel.setFont(aliasLabel.getFont().deriveFont(Font.BOLD, 14f));
            pathLabel.setFont(pathLabel.getFont().deriveFont(12f));
            pathLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            timeLabel.setFont(timeLabel.getFont().deriveFont(11f));
            timeLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

            JPanel textPanel = new JPanel();
            textPanel.setOpaque(false);
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.add(aliasLabel);
            textPanel.add(Box.createVerticalStrut(2));
            textPanel.add(pathLabel);

            add(textPanel, BorderLayout.CENTER);
            add(timeLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ProjectRegistryEntry> list,
                                                      ProjectRegistryEntry entry,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            String alias = safe(entry.alias());
            if (alias.isBlank()) {
                alias = safe(entry.projectKey());
            }
            aliasLabel.setText(alias);

            String inputPath = safe(entry.inputPath());
            pathLabel.setText(inputPath.isBlank() ? tr("未设置输入路径", "no input path") : inputPath);

            if (entry.updatedAt() > 0) {
                LocalDateTime dt = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(entry.updatedAt()), ZoneId.systemDefault());
                timeLabel.setText(DISPLAY_DATE_FORMAT.format(dt));
            } else {
                timeLabel.setText("");
            }

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
                aliasLabel.setForeground(list.getSelectionForeground());
                pathLabel.setForeground(list.getSelectionForeground());
                timeLabel.setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
                aliasLabel.setForeground(list.getForeground());
                pathLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
                timeLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            }
            setOpaque(true);
            return this;
        }

    }
}
