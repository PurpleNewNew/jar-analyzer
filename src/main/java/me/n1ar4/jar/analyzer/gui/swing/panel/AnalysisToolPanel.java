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

import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowAction;
import me.n1ar4.jar.analyzer.gui.swing.SwingI18n;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class AnalysisToolPanel extends JPanel {
    private final JTabbedPane resultTabs = new JTabbedPane();
    private final JTextField keywordField = new JTextField();
    private final JButton prevButton = new JButton();
    private final JButton nextButton = new JButton();
    private final JButton copyButton = new JButton();
    private final JButton saveButton = new JButton();
    private final JButton clearButton = new JButton();
    private final JLabel statusLabel = new JLabel();
    private final Map<ToolingWindowAction, AnalysisView> views = new EnumMap<>(ToolingWindowAction.class);

    public AnalysisToolPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        initUi();
        applyLanguage();
    }

    private void initUi() {
        JPanel north = new JPanel(new BorderLayout(6, 0));
        north.add(keywordField, BorderLayout.CENTER);
        JPanel navButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        navButtons.add(prevButton);
        navButtons.add(nextButton);
        north.add(navButtons, BorderLayout.EAST);

        JPanel south = new JPanel(new BorderLayout(6, 0));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actions.add(copyButton);
        actions.add(saveButton);
        actions.add(clearButton);
        south.add(actions, BorderLayout.WEST);
        south.add(statusLabel, BorderLayout.CENTER);

        prevButton.addActionListener(e -> findMatch(false));
        nextButton.addActionListener(e -> findMatch(true));
        keywordField.addActionListener(e -> findMatch(true));
        copyButton.addActionListener(e -> copyCurrentText());
        saveButton.addActionListener(e -> saveCurrentText());
        clearButton.addActionListener(e -> clearCurrentView());
        resultTabs.addChangeListener(e -> statusLabel.setText(tr("就绪", "ready")));

        add(north, BorderLayout.NORTH);
        add(resultTabs, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    public boolean showResult(ToolingWindowAction action, String title, String content) {
        if (!supports(action)) {
            return false;
        }
        AnalysisView view = views.get(action);
        if (view == null) {
            view = createView(action);
            views.put(action, view);
            resultTabs.addTab(tabTitle(action), view.scrollPane());
        }
        view.update(title, content);
        int tabIndex = resultTabs.indexOfComponent(view.scrollPane());
        if (tabIndex >= 0) {
            resultTabs.setTitleAt(tabIndex, tabTitle(action));
            resultTabs.setToolTipTextAt(tabIndex, safe(title));
            resultTabs.setSelectedIndex(tabIndex);
        }
        statusLabel.setText(tr("已更新", "updated"));
        return true;
    }

    public void applyLanguage() {
        prevButton.setText(tr("上一个", "Prev"));
        nextButton.setText(tr("下一个", "Next"));
        copyButton.setText(tr("复制", "Copy"));
        saveButton.setText(tr("保存", "Save"));
        clearButton.setText(tr("清空当前", "Clear Current"));
        if (statusLabel.getText() == null || statusLabel.getText().isBlank()) {
            statusLabel.setText(tr("就绪", "ready"));
        }
        for (Map.Entry<ToolingWindowAction, AnalysisView> entry : views.entrySet()) {
            int idx = resultTabs.indexOfComponent(entry.getValue().scrollPane());
            if (idx >= 0) {
                resultTabs.setTitleAt(idx, tabTitle(entry.getKey()));
            }
        }
    }

    private AnalysisView createView(ToolingWindowAction action) {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(false);
        area.setWrapStyleWord(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(area);
        return new AnalysisView(action, scroll, area);
    }

    private void findMatch(boolean forward) {
        AnalysisView view = currentView();
        if (view == null) {
            statusLabel.setText(tr("无可搜索内容", "no content"));
            return;
        }
        if (selectTextMatch(view.textArea(), safe(keywordField.getText()), forward)) {
            statusLabel.setText(tr("已定位", "matched"));
        } else {
            statusLabel.setText(tr("未找到", "not found"));
        }
    }

    private void copyCurrentText() {
        AnalysisView view = currentView();
        if (view == null) {
            return;
        }
        String selected = safe(view.textArea().getSelectedText());
        if (selected.isBlank()) {
            selected = safe(view.textArea().getText());
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(selected), null);
        statusLabel.setText(tr("已复制", "copied"));
    }

    private void saveCurrentText() {
        AnalysisView view = currentView();
        if (view == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(tr("保存文本", "Save Text"));
        if (!safe(view.title()).isBlank()) {
            chooser.setSelectedFile(new java.io.File(safe(view.title()) + ".txt"));
        }
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
            return;
        }
        try {
            Files.writeString(chooser.getSelectedFile().toPath(), safe(view.textArea().getText()), StandardCharsets.UTF_8);
            statusLabel.setText(tr("已保存", "saved"));
        } catch (Throwable ex) {
            statusLabel.setText(tr("保存失败: ", "save failed: ") + safe(ex.getMessage()));
        }
    }

    private void clearCurrentView() {
        AnalysisView view = currentView();
        if (view == null) {
            return;
        }
        view.update(view.title(), "");
        statusLabel.setText(tr("已清空", "cleared"));
    }

    private AnalysisView currentView() {
        java.awt.Component selected = resultTabs.getSelectedComponent();
        if (selected == null) {
            return null;
        }
        for (AnalysisView view : views.values()) {
            if (view.scrollPane() == selected) {
                return view;
            }
        }
        return null;
    }

    private String tabTitle(ToolingWindowAction action) {
        return switch (action) {
            case CFG -> "CFG";
            case FRAME -> tr("Frame", "Frame");
            case OPCODE -> tr("Opcode", "Opcode");
            case ASM -> "ASM";
            case BCEL_TOOL -> "BCEL";
            case ALL_STRINGS -> tr("字符串总览", "All Strings");
            default -> safe(action == null ? "" : action.name());
        };
    }

    private boolean supports(ToolingWindowAction action) {
        return action == ToolingWindowAction.CFG
                || action == ToolingWindowAction.FRAME
                || action == ToolingWindowAction.OPCODE
                || action == ToolingWindowAction.ASM
                || action == ToolingWindowAction.BCEL_TOOL
                || action == ToolingWindowAction.ALL_STRINGS;
    }

    private static boolean selectTextMatch(JTextArea area, String keyword, boolean forward) {
        if (area == null) {
            return false;
        }
        String key = safe(keyword).trim();
        String text = safe(area.getText());
        if (key.isEmpty() || text.isEmpty()) {
            return false;
        }
        String lowerText = text.toLowerCase(Locale.ROOT);
        String lowerKey = key.toLowerCase(Locale.ROOT);

        int found;
        if (forward) {
            int start = Math.max(area.getSelectionEnd(), area.getCaretPosition());
            found = lowerText.indexOf(lowerKey, start);
            if (found < 0 && start > 0) {
                found = lowerText.indexOf(lowerKey);
            }
        } else {
            int start = Math.min(area.getSelectionStart(), area.getCaretPosition()) - 1;
            if (start < 0) {
                start = lowerText.length() - 1;
            }
            found = lowerText.lastIndexOf(lowerKey, start);
            if (found < 0) {
                found = lowerText.lastIndexOf(lowerKey);
            }
        }
        if (found < 0) {
            return false;
        }
        int end = found + lowerKey.length();
        area.requestFocusInWindow();
        area.select(found, end);
        return true;
    }

    private static String tr(String zh, String en) {
        return SwingI18n.tr(zh, en);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class AnalysisView {
        private final ToolingWindowAction action;
        private final JScrollPane scrollPane;
        private final JTextArea textArea;
        private String title = "";
        private String content = "";

        private AnalysisView(ToolingWindowAction action, JScrollPane scrollPane, JTextArea textArea) {
            this.action = action;
            this.scrollPane = scrollPane;
            this.textArea = textArea;
        }

        private ToolingWindowAction action() {
            return action;
        }

        private JScrollPane scrollPane() {
            return scrollPane;
        }

        private JTextArea textArea() {
            return textArea;
        }

        private String title() {
            return title;
        }

        private void update(String nextTitle, String nextContent) {
            this.title = safe(nextTitle);
            this.content = safe(nextContent);
            textArea.setText(content);
            textArea.setCaretPosition(0);
        }
    }
}
