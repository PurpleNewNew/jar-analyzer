/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.util;

import com.intellij.uiDesigner.core.GridConstraints;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.OpcodeForm;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class SyntaxAreaHelper {
    private static final Logger logger = LogManager.getLogger();
    private static RSyntaxTextArea codeArea = null;
    private static int currentIndex = 0;
    private static ArrayList<Integer> searchResults = null;

    public static void buildJava(JPanel codePanel) {
        RSyntaxTextArea rArea = new RSyntaxTextArea();
        // 不要使用其他字体
        // 默认字体支持中文 其他的不一定

        rArea.addCaretListener(e -> {
            String selectedText = rArea.getSelectedText();
            if (selectedText == null || selectedText.trim().isEmpty()) {
                Highlighter highlighter = rArea.getHighlighter();
                highlighter.removeAllHighlights();
                return;
            }
            Highlighter highlighter = rArea.getHighlighter();
            highlighter.removeAllHighlights();

            String text = rArea.getText();
            int index = 0;

            while ((index = text.indexOf(selectedText, index)) >= 0) {
                try {
                    highlighter.addHighlight(index, index + selectedText.length(),
                            new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW));
                    index += selectedText.length();
                } catch (BadLocationException ignored) {
                }
            }
        });

        codeArea = rArea;
        codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        codeArea.setCodeFoldingEnabled(true);

        codeArea.setFont(codeArea.getFont().deriveFont(MainForm.FONT_SIZE));

        Highlighter highlighter = codeArea.getHighlighter();
        codeArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.isControlDown()) {
                    int caretPosition = codeArea.getCaretPosition();
                    int start = findWordStart(codeArea.getText(), caretPosition);
                    int end = findWordEnd(codeArea.getText(), caretPosition);
                    if (start != -1 && end != -1) {
                        String word = codeArea.getText().substring(start, end);
                        highlighter.removeAllHighlights();
                        try {
                            highlighter.addHighlight(start, end,
                                    new DefaultHighlighter.DefaultHighlightPainter(Color.BLUE));
                        } catch (BadLocationException ignored) {
                        }

                        String methodName = word.trim();
                        if (methodName.isEmpty()) {
                            return;
                        }
                        logger.info("user selected string: {}", methodName);
                        String className = MainForm.getCurClass();
                        if (className == null || className.trim().isEmpty()) {
                            JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                                    buildCtrlClickHint());
                            return;
                        }
                        if (MainForm.getEngine() == null) {
                            JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                                    "PLEASE BUILD DATABASE FIRST");
                            return;
                        }
                        if (className.contains("/")) {
                            String shortClassName = className.substring(className.lastIndexOf('/') + 1);
                            if (methodName.equals(shortClassName)) {
                                methodName = "<init>";
                            }
                        } else {
                            if (methodName.equals(className)) {
                                methodName = "<init>";
                            }
                        }
                        String finalMethodName = methodName;
                        JDialog dialog = ProcessDialog.createProgressDialog(MainForm.getInstance().getMasterPanel());
                        new Thread(() -> dialog.setVisible(true)).start();
                        new Thread(() -> {
                            try {
                                CoreHelper.refreshCallers(className, finalMethodName, null);
                                CoreHelper.refreshCallee(className, finalMethodName, null);
                                MainForm.getInstance().getTabbedPanel().setSelectedIndex(2);
                            } finally {
                                dialog.dispose();
                            }
                        }).start();
                    }
                }
            }
        });
        RTextScrollPane sp = new RTextScrollPane(codeArea);
        codePanel.add(sp, new GridConstraints(0, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null, 0, false));
        MainForm.setCodeArea(codeArea);
    }

    private static String buildCtrlClickHint() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<p>无法定位调用关系</p>");
        String text = codeArea == null ? null : codeArea.getText();
        if (text == null || text.trim().isEmpty()) {
            sb.append("<p>当前未加载代码</p>");
        } else if (looksLikeResource(text)) {
            sb.append("<p>当前打开的是资源/配置文件，不支持 Ctrl+点击</p>");
        } else if (text.contains("Jar Analyzer by 4ra1n")) {
            sb.append("<p>可能是反编译失败或依赖缺失，类信息未就绪</p>");
        } else {
            sb.append("<p>当前类信息未就绪</p>");
        }
        try {
            String curText = MainForm.getInstance().getCurClassText().getText();
            if (curText == null || curText.trim().isEmpty()) {
                sb.append("<p>请先在左侧类/方法列表中选择具体方法</p>");
            } else {
                sb.append("<p>可尝试重新点击类/方法或重新反编译</p>");
            }
        } catch (Exception ignored) {
        }
        sb.append("</html>");
        return sb.toString();
    }

    private static boolean looksLikeResource(String text) {
        String t = text.trim();
        if (t.startsWith("<") && t.contains(">")) {
            return true;
        }
        if (t.contains("=") && !t.contains("class ")
                && !t.contains("interface ")
                && !t.contains("enum ")) {
            return true;
        }
        return false;
    }

    private static int findWordStart(String text, int position) {
        while (position > 0 && Character.isLetterOrDigit(text.charAt(position - 1))) {
            position--;
        }
        return position;
    }

    private static int findWordEnd(String text, int position) {
        while (position < text.length() && Character.isLetterOrDigit(text.charAt(position))) {
            position++;
        }
        return position;
    }

    public static int addSearchAction(String text) {
        searchResults = new ArrayList<>();
        currentIndex = 0;
        String content = codeArea.getText();

        int index = content.indexOf(text);
        while (index >= 0) {
            searchResults.add(index);
            index = content.indexOf(text, index + 1);
        }
        currentIndex = 0;
        return searchResults.size();
    }

    public static int getCurrentIndex() {
        return currentIndex;
    }

    public static void navigate(String text, boolean forward) {
        if (searchResults == null || codeArea == null) {
            return;
        }
        if (searchResults.isEmpty()) {
            return;
        }
        if (forward) {
            currentIndex = (currentIndex + 1) % searchResults.size();
        } else {
            currentIndex = (currentIndex - 1 + searchResults.size()) % searchResults.size();
        }
        highlightResult(text);
    }

    private static void highlightResult(String text) {
        if (searchResults.isEmpty()) return;
        int index = searchResults.get(currentIndex);
        try {
            codeArea.setCaretPosition(index);
            Highlighter highlighter = codeArea.getHighlighter();
            Highlighter.HighlightPainter painter =
                    new DefaultHighlighter.DefaultHighlightPainter(Color.CYAN);
            highlighter.removeAllHighlights();
            highlighter.addHighlight(index, index + text.length(), painter);
        } catch (BadLocationException ex) {
            logger.error("bad location: {}", ex.toString());
        }
    }

    public static void buildJavaOpcode(JPanel codePanel) {
        RSyntaxTextArea textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setCodeFoldingEnabled(false);

        textArea.setFont(textArea.getFont().deriveFont(MainForm.FONT_SIZE));

        RTextScrollPane sp = new RTextScrollPane(textArea);
        codePanel.add(sp, new GridConstraints(0, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null, 0, false));
        OpcodeForm.setCodeArea(textArea);
    }

    public static JTextArea buildSQL(JPanel codePanel) {
        RSyntaxTextArea textArea = new RSyntaxTextArea(10, 80);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
        textArea.setCodeFoldingEnabled(true);

        textArea.setFont(textArea.getFont().deriveFont(MainForm.FONT_SIZE));

        RTextScrollPane sp = new RTextScrollPane(textArea);
        codePanel.add(sp, new GridConstraints(0, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null, 0, false));
        return textArea;
    }
}
