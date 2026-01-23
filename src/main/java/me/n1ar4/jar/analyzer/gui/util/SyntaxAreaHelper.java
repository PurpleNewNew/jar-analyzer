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
import me.n1ar4.jar.analyzer.engine.CoreHelper;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.OpcodeForm;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                        if (MainForm.getEngine() == null) {
                            JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                                    "PLEASE BUILD DATABASE FIRST");
                            return;
                        }
                        final String curClassSnapshot = MainForm.getCurClass();
                        final String codeSnapshot = codeArea == null ? null : codeArea.getText();
                        final String methodNameSnapshot = methodName;
                        JDialog dialog = ProcessDialog.createProgressDialog(MainForm.getInstance().getMasterPanel());
                        SwingUtilities.invokeLater(() -> dialog.setVisible(true));
                        new Thread(() -> {
                            try {
                                String className = curClassSnapshot;
                                String methodNameLocal = methodNameSnapshot;
                                if (className == null || className.trim().isEmpty()) {
                                    String inferred = inferClassName(codeSnapshot);
                                    if (inferred != null && !inferred.trim().isEmpty()) {
                                        className = inferred;
                                        String finalClassName = inferred;
                                        runOnEdt(() -> {
                                            MainForm.setCurClass(finalClassName);
                                            MainForm.getInstance().getCurClassText().setText(finalClassName);
                                        });
                                    } else {
                                        MethodResult picked = selectGlobalMethod(methodNameLocal);
                                        if (picked != null) {
                                            className = picked.getClassName();
                                            String finalClassName = className;
                                            runOnEdt(() -> {
                                                MainForm.setCurClass(finalClassName);
                                                MainForm.getInstance().getCurClassText().setText(finalClassName);
                                            });
                                        } else {
                                            runOnEdt(() -> JOptionPane.showMessageDialog(
                                                    MainForm.getInstance().getMasterPanel(),
                                                    buildCtrlClickHint()));
                                            return;
                                        }
                                    }
                                }
                                if (className.contains("/")) {
                                    String shortClassName = className.substring(className.lastIndexOf('/') + 1);
                                    if (methodNameLocal.equals(shortClassName)) {
                                        methodNameLocal = "<init>";
                                    }
                                } else {
                                    if (methodNameLocal.equals(className)) {
                                        methodNameLocal = "<init>";
                                    }
                                }
                                List<MethodResult> candidates = MainForm.getEngine()
                                        .getMethod(className, methodNameLocal, null);
                                String methodDesc = null;
                                if (candidates != null && !candidates.isEmpty()) {
                                    String methodNameForUi = methodNameLocal;
                                    methodDesc = callOnEdt(() -> resolveMethodDesc(methodNameForUi, candidates));
                                    if (methodDesc == null) {
                                        return;
                                    }
                                }
                                boolean methodMissing = candidates == null || candidates.isEmpty();
                                boolean classMissing = MainForm.getEngine().getClassByClass(className) == null;
                                boolean filtered = CommonFilterUtil.isFilteredClass(className);
                                if (methodMissing || classMissing) {
                                    MethodResult picked = selectGlobalMethod(methodNameLocal);
                                    if (picked != null) {
                                        className = picked.getClassName();
                                        methodDesc = picked.getMethodDesc();
                                        String finalClassName = className;
                                        runOnEdt(() -> {
                                            MainForm.setCurClass(finalClassName);
                                            MainForm.getInstance().getCurClassText().setText(finalClassName);
                                        });
                                        methodMissing = false;
                                        classMissing = false;
                                        filtered = CommonFilterUtil.isFilteredClass(className);
                                    }
                                }
                                String finalMethodName = methodNameLocal;
                                String finalMethodDesc = methodDesc;
                                String finalClassName = className;
                                boolean finalClassMissing = classMissing;
                                boolean finalMethodMissing = methodMissing;
                                boolean finalFiltered = filtered;
                                List<MethodResult> callers = MainForm.getEngine().getCallers(finalClassName, finalMethodName, finalMethodDesc);
                                List<MethodResult> callees = MainForm.getEngine().getCallee(finalClassName, finalMethodName, finalMethodDesc);
                                CoreHelper.refreshCallers(finalClassName, finalMethodName, finalMethodDesc);
                                CoreHelper.refreshCallee(finalClassName, finalMethodName, finalMethodDesc);
                                SwingUtilities.invokeLater(() ->
                                        MainForm.getInstance().getTabbedPanel().setSelectedIndex(2));
                                if ((callers == null || callers.isEmpty()) && (callees == null || callees.isEmpty())) {
                                    String hint = buildEmptyCallHint(finalClassName, finalMethodName, finalClassMissing, finalMethodMissing, finalFiltered);
                                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                                            MainForm.getInstance().getMasterPanel(), hint));
                                } else if (finalMethodMissing) {
                                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                                            MainForm.getInstance().getMasterPanel(),
                                            "<html><p>未找到精确方法，已使用方法名进行模糊查找</p></html>"));
                                }
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

    private static String buildEmptyCallHint(String className,
                                             String methodName,
                                             boolean classMissing,
                                             boolean methodMissing,
                                             boolean filtered) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<p>无法定位调用关系</p>");
        sb.append("<p>类: ").append(className).append("</p>");
        sb.append("<p>方法: ").append(methodName).append("</p>");
        if (classMissing) {
            sb.append("<p>当前类不在数据库中</p>");
        }
        if (methodMissing) {
            sb.append("<p>当前类未找到该方法</p>");
        }
        if (filtered) {
            sb.append("<p>可能被黑名单过滤 (rules/common-filter.json)</p>");
        }
        sb.append("</html>");
        return sb.toString();
    }

    private static String resolveMethodDesc(String methodName, List<MethodResult> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0).getMethodDesc();
        }
        String[] options = new String[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            MethodResult mr = candidates.get(i);
            String jarName = mr.getJarName();
            String label = mr.getMethodName() + mr.getMethodDesc();
            if (jarName != null && !jarName.trim().isEmpty()) {
                label = label + " [" + jarName.trim() + "]";
            }
            options[i] = label;
        }
        Object choice = JOptionPane.showInputDialog(
                MainForm.getInstance().getMasterPanel(),
                "选择方法签名",
                "Overload Select",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]);
        if (choice == null) {
            return null;
        }
        String selected = choice.toString();
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(selected)) {
                return candidates.get(i).getMethodDesc();
            }
        }
        return candidates.get(0).getMethodDesc();
    }

    private static MethodResult selectGlobalMethod(String methodName) {
        if (methodName == null) {
            return null;
        }
        String trimmed = methodName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        ArrayList<MethodResult> results = MainForm.getEngine().getMethod(null, trimmed, null);
        boolean likeSearch = false;
        if (results == null || results.isEmpty()) {
            results = MainForm.getEngine().getMethodLike(null, trimmed, null);
            likeSearch = true;
        }
        if (results == null || results.isEmpty()) {
            return null;
        }
        final int maxCandidates = 200;
        if (results.size() > maxCandidates) {
            String classFilter = callOnEdt(() -> JOptionPane.showInputDialog(
                    MainForm.getInstance().getMasterPanel(),
                    "候选过多，请输入类名过滤",
                    "Filter",
                    JOptionPane.PLAIN_MESSAGE));
            if (classFilter == null) {
                return null;
            }
            String filter = classFilter.trim();
            if (filter.isEmpty()) {
                return null;
            }
            if (filter.contains(".") && !filter.contains("/")) {
                filter = filter.replace('.', '/');
            }
            results = MainForm.getEngine().getMethodLike(filter, trimmed, null);
            likeSearch = true;
            if (results == null || results.isEmpty()) {
                runOnEdt(() -> JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                        "<html><p>未找到匹配方法</p></html>"));
                return null;
            }
            if (results.size() > maxCandidates) {
                runOnEdt(() -> JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(),
                        "<html><p>候选过多，请更具体过滤</p></html>"));
                return null;
            }
        }
        if (results.size() == 1) {
            return results.get(0);
        }
        String[] options = new String[results.size()];
        for (int i = 0; i < results.size(); i++) {
            MethodResult mr = results.get(i);
            String cls = mr.getClassName();
            String desc = mr.getMethodDesc();
            String jarName = mr.getJarName();
            StringBuilder label = new StringBuilder();
            label.append(i + 1).append(". ");
            label.append(cls == null ? "unknown" : cls);
            label.append("#").append(mr.getMethodName());
            if (desc != null) {
                label.append(desc);
            }
            if (jarName != null && !jarName.trim().isEmpty()) {
                label.append(" [").append(jarName.trim()).append("]");
            }
            if (cls != null && CommonFilterUtil.isFilteredClass(cls)) {
                label.append(" [filtered]");
            }
            options[i] = label.toString();
        }
        String title = likeSearch ? "方法选择 (模糊匹配)" : "方法选择";
        Object choice = callOnEdt(() -> JOptionPane.showInputDialog(
                MainForm.getInstance().getMasterPanel(),
                "选择方法",
                title,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]));
        if (choice == null) {
            return null;
        }
        String selected = choice.toString();
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(selected)) {
                return results.get(i);
            }
        }
        return results.get(0);
    }

    private static String inferClassName(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        String pkg = null;
        Matcher pkgMatcher = Pattern.compile("(?m)^\\s*package\\s+([\\w\\.]+)\\s*;").matcher(code);
        if (pkgMatcher.find()) {
            pkg = pkgMatcher.group(1);
        }
        Matcher clsMatcher = Pattern.compile("(?m)^\\s*(?:public\\s+|protected\\s+|private\\s+|abstract\\s+|final\\s+|static\\s+)*" +
                "(class|interface|enum|record)\\s+([A-Za-z_$][\\w$]*)").matcher(code);
        if (clsMatcher.find()) {
            String cls = clsMatcher.group(2);
            if (cls == null || cls.trim().isEmpty()) {
                return null;
            }
            if (pkg == null || pkg.trim().isEmpty()) {
                return cls;
            }
            return pkg.replace('.', '/') + "/" + cls;
        }
        return null;
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
        while (position > 0 && isWordChar(text.charAt(position - 1))) {
            position--;
        }
        return position;
    }

    private static int findWordEnd(String text, int position) {
        while (position < text.length() && isWordChar(text.charAt(position))) {
            position++;
        }
        return position;
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private static void runOnEdt(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    private static <T> T callOnEdt(Supplier<T> supplier) {
        if (supplier == null) {
            return null;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            return supplier.get();
        }
        AtomicReference<T> ref = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> ref.set(supplier.get()));
        } catch (Exception ex) {
            logger.debug("invokeAndWait error: {}", ex.toString());
        }
        return ref.get();
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
