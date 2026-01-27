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
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.TypeParameter;
import me.n1ar4.jar.analyzer.core.FinderRunner;
import me.n1ar4.jar.analyzer.engine.CoreHelper;
import me.n1ar4.jar.analyzer.engine.DecompileEngine;
import me.n1ar4.jar.analyzer.engine.index.IndexPluginsSupport;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.gui.LuceneSearchForm;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.OpcodeForm;
import me.n1ar4.jar.analyzer.gui.adapter.GlobalKeyListener;
import me.n1ar4.jar.analyzer.gui.adapter.SearchInputListener;
import me.n1ar4.jar.analyzer.gui.state.State;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;
import me.n1ar4.jar.analyzer.utils.RuntimeClassResolver;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import me.n1ar4.parser.JarAnalyzerParser;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxAreaHelper {
    private static final Logger logger = LogManager.getLogger();
    private static RSyntaxTextArea codeArea = null;
    private static int currentIndex = 0;
    private static ArrayList<Integer> searchResults = null;
    private static final Object PARSE_LOCK = new Object();
    private static String lastParsedCode = null;
    private static CompilationUnit lastParsedUnit = null;
    private static JTabbedPane codeTabs = null;
    private static final Map<String, Component> classTabs = new HashMap<>();
    private static final List<RSyntaxTextArea> codeAreas = new ArrayList<>();
    private static final String PROP_AREA = "codeArea";
    private static final String PROP_CLASS = "className";
    private static final String PROP_LABEL = "tabLabel";
    private static Theme currentTheme = null;
    private static String lastSearchKey = null;
    private static RSyntaxTextArea lastSearchArea = null;

    public static void buildJava(JPanel codePanel) {
        codeTabs = new JTabbedPane();
        RSyntaxTextArea rArea = createCodeArea();
        CodeMenuHelper.install(rArea);
        JPanel tab = createCodeTab(rArea, "Code", null);
        codeTabs.addTab("Code", tab);
        int idx = codeTabs.indexOfComponent(tab);
        if (idx >= 0) {
            codeTabs.setTabComponentAt(idx, buildTabHeader("Code", tab));
        }
        codeTabs.addChangeListener(e -> {
            RSyntaxTextArea active = getAreaFromTab(codeTabs.getSelectedComponent());
            if (active != null) {
                setActiveCodeArea(active);
            }
            Component selected = codeTabs.getSelectedComponent();
            String tabClass = null;
            if (selected instanceof JComponent) {
                Object value = ((JComponent) selected).getClientProperty(PROP_CLASS);
                if (value instanceof String) {
                    tabClass = (String) value;
                }
            }
            if (tabClass != null && !tabClass.trim().isEmpty()) {
                String normalized = normalizeClassName(tabClass);
                MainForm.setCurClass(normalized);
                MainForm.getInstance().getCurClassText().setText(normalized);
                String jarName = null;
                if (MainForm.getEngine() != null) {
                    jarName = MainForm.getEngine().getJarByClass(normalized);
                }
                MainForm.getInstance().getCurJarText().setText(jarName == null ? "" : jarName);
                MethodResult curMethod = MainForm.getCurMethod();
                if (curMethod == null || !normalized.equals(curMethod.getClassName())) {
                    MainForm.setCurMethod(null);
                    MainForm.getInstance().getCurMethodText().setText("");
                } else {
                    MainForm.getInstance().getCurMethodText().setText(curMethod.getMethodName());
                }
            } else {
                MainForm.setCurClass(null);
                MainForm.getInstance().getCurClassText().setText("");
                MainForm.getInstance().getCurJarText().setText("");
                MainForm.getInstance().getCurMethodText().setText("");
                MainForm.setCurMethod(null);
            }
        });
        setActiveCodeArea(rArea);
        codePanel.add(codeTabs, new GridConstraints(0, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null, 0, false));
    }

    private static void runOnEdt(Runnable task) {
        if (task == null) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    private static String buildCtrlClickHint() {
        return "<html><p>no declaration found. check dependencies or try another symbol.</p></html>";
    }

    private static String buildEmptyCallHint(String className,
                                             String methodName,
                                             boolean classMissing,
                                             boolean methodMissing,
                                             boolean filtered) {
        StringBuilder sb = new StringBuilder("<html><p>no callers/callees found.</p>");
        if (className != null && !className.trim().isEmpty()) {
            sb.append("<p>class: ").append(className).append("</p>");
        }
        if (methodName != null && !methodName.trim().isEmpty()) {
            sb.append("<p>method: ").append(methodName).append("</p>");
        }
        if (classMissing) {
            sb.append("<p>class not in database</p>");
        }
        if (methodMissing) {
            sb.append("<p>method not found in class</p>");
        }
        if (filtered) {
            sb.append("<p>filtered by rules</p>");
        }
        sb.append("</html>");
        return sb.toString();
    }

    private static MethodResult selectGlobalMethod(String methodName) {
        if (methodName == null || methodName.trim().isEmpty()) {
            return null;
        }
        if (MainForm.getEngine() == null) {
            return null;
        }
        List<MethodResult> candidates = MainForm.getEngine().getMethodLike(null, methodName, null);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        return callOnEdt(() -> {
            DefaultListModel<MethodResult> model = new DefaultListModel<>();
            for (MethodResult result : candidates) {
                model.addElement(result);
            }
            JList<MethodResult> list = new JList<>(model);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setVisibleRowCount(8);
            JScrollPane pane = new JScrollPane(list);
            int option = JOptionPane.showConfirmDialog(
                    MainForm.getInstance().getMasterPanel(),
                    pane,
                    "Select target method",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (option == JOptionPane.OK_OPTION) {
                return list.getSelectedValue();
            }
            return null;
        });
    }

    private static String resolveMethodDesc(String methodName, List<MethodResult> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0).getMethodDesc();
        }
        DefaultListModel<String> model = new DefaultListModel<>();
        for (MethodResult result : candidates) {
            String desc = result.getMethodDesc();
            if (desc == null) {
                desc = "";
            }
            model.addElement(methodName + desc);
        }
        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(8);
        JScrollPane pane = new JScrollPane(list);
        int option = JOptionPane.showConfirmDialog(
                MainForm.getInstance().getMasterPanel(),
                pane,
                "Select method overload",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (option != JOptionPane.OK_OPTION) {
            return null;
        }
        int idx = list.getSelectedIndex();
        if (idx < 0 || idx >= candidates.size()) {
            return null;
        }
        return candidates.get(idx).getMethodDesc();
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
            logger.warn("callOnEdt failed: {}", ex.toString());
            return null;
        }
        return ref.get();
    }

    private static RSyntaxTextArea createCodeArea() {
        RSyntaxTextArea rArea = new RSyntaxTextArea();

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

        if (currentTheme != null) {
            currentTheme.apply(codeArea);
        }
        codeArea.setFont(codeArea.getFont().deriveFont(MainForm.FONT_SIZE));

        Highlighter highlighter = codeArea.getHighlighter();
        codeArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setActiveCodeArea(rArea);
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
                        final int caretSnapshot = caretPosition;
                        JDialog dialog = ProcessDialog.createProgressDialog(MainForm.getInstance().getMasterPanel());
                        SwingUtilities.invokeLater(() -> dialog.setVisible(true));
                        new Thread(() -> {
                            try {
                                SemanticTarget target = resolveSemanticTarget(codeSnapshot, caretSnapshot, methodNameSnapshot);
                                if (target == null || target.kind == SymbolKind.UNKNOWN) {
                                    SyntaxAreaHelper.runOnEdt(() -> JOptionPane.showMessageDialog(
                                            MainForm.getInstance().getMasterPanel(),
                                            buildCtrlClickHint()));
                                    return;
                                }
                                if (target.kind == SymbolKind.VARIABLE || target.kind == SymbolKind.FIELD
                                        || target.kind == SymbolKind.TYPE_PARAM) {
                                    if (target.declarationRange != null && codeSnapshot != null) {
                                        if (jumpToDeclaration(codeSnapshot, target.declarationRange)) {
                                            return;
                                        }
                                    }
                                    if (target.typeName != null && !target.typeName.trim().isEmpty()) {
                                        String finalClassName = target.typeName;
                                        SyntaxAreaHelper.runOnEdt(() -> {
                                            MainForm.setCurClass(finalClassName);
                                            MainForm.getInstance().getCurClassText().setText(finalClassName);
                                        });
                                        openClassInEditor(finalClassName, null, null, true, false, true);
                                        CoreHelper.refreshAllMethods(finalClassName);
                                        SwingUtilities.invokeLater(() ->
                                                MainForm.getInstance().getTabbedPanel().setSelectedIndex(2));
                                        return;
                                    }
                                    SyntaxAreaHelper.runOnEdt(() -> JOptionPane.showMessageDialog(
                                            MainForm.getInstance().getMasterPanel(),
                                            buildCtrlClickHint()));
                                    return;
                                }
                                if (target.kind == SymbolKind.CLASS) {
                                    String cls = target.className;
                                    if (cls == null || cls.trim().isEmpty()) {
                                        SyntaxAreaHelper.runOnEdt(() -> JOptionPane.showMessageDialog(
                                                MainForm.getInstance().getMasterPanel(),
                                                "<html><p>unable to resolve class name</p></html>"));
                                        return;
                                    }
                                    String finalClassName = cls;
                                    SyntaxAreaHelper.runOnEdt(() -> {
                                        MainForm.setCurClass(finalClassName);
                                        MainForm.getInstance().getCurClassText().setText(finalClassName);
                                    });
                                    if (!openClassInEditor(finalClassName, null, null, true, true, true)) {
                                        SyntaxAreaHelper.runOnEdt(() -> JOptionPane.showMessageDialog(
                                                MainForm.getInstance().getMasterPanel(),
                                                "<html><p>need dependency or class file not found</p></html>"));
                                    }
                                    CoreHelper.refreshAllMethods(finalClassName);
                                    SwingUtilities.invokeLater(() ->
                                            MainForm.getInstance().getTabbedPanel().setSelectedIndex(2));
                                    return;
                                }

                                String className = curClassSnapshot;
                                String methodNameLocal = target.name == null ? methodNameSnapshot : target.name;
                                if (target.kind == SymbolKind.CONSTRUCTOR) {
                                    methodNameLocal = "<init>";
                                }
                                if (target.className != null && !target.className.trim().isEmpty()) {
                                    className = target.className;
                                }
                                if (className == null || className.trim().isEmpty()) {
                                    String inferred = SyntaxAreaHelper.inferClassName(codeSnapshot);
                                    if (inferred != null && !inferred.trim().isEmpty()) {
                                        className = inferred;
                                        String finalClassName = inferred;
                                        SyntaxAreaHelper.runOnEdt(() -> {
                                            MainForm.setCurClass(finalClassName);
                                            MainForm.getInstance().getCurClassText().setText(finalClassName);
                                        });
                                    } else {
                                        MethodResult picked = selectGlobalMethod(methodNameLocal);
                                        if (picked != null) {
                                            className = picked.getClassName();
                                            String finalClassName = className;
                                            SyntaxAreaHelper.runOnEdt(() -> {
                                                MainForm.setCurClass(finalClassName);
                                                MainForm.getInstance().getCurClassText().setText(finalClassName);
                                            });
                                        } else {
                                            SyntaxAreaHelper.runOnEdt(() -> JOptionPane.showMessageDialog(
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
                                        SyntaxAreaHelper.runOnEdt(() -> {
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
                                openClassInEditor(finalClassName, finalMethodName, finalMethodDesc, true, false, true);
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
                                            "<html><p>no clear target found. try a different symbol.</p></html>"));
                                }
                            } finally {
                                dialog.dispose();
                            }
                        }).start();
                    }
                }
            }
        });
        rArea.addKeyListener(new GlobalKeyListener());
        rArea.setDropTarget(new DropInstance());
        return rArea;
    }

    public static RSyntaxTextArea getActiveCodeArea() {
        return codeArea;
    }

    public static void bindActiveTab(String className) {
        if (codeTabs == null) {
            return;
        }
        Component selected = codeTabs.getSelectedComponent();
        if (selected == null) {
            return;
        }
        String normalized = className == null ? null : normalizeClassName(className);
        updateTabClass(selected, normalized);
    }

    public static int getCurrentIndex() {
        return currentIndex;
    }

    public static int addSearchAction(String key) {
        RSyntaxTextArea area = getActiveCodeArea();
        if (area == null) {
            searchResults = new ArrayList<>();
            currentIndex = 0;
            return 0;
        }
        if (key == null || key.isEmpty()) {
            searchResults = new ArrayList<>();
            currentIndex = 0;
            lastSearchKey = null;
            lastSearchArea = area;
            return 0;
        }
        String text = area.getText();
        if (text == null || text.isEmpty()) {
            searchResults = new ArrayList<>();
            currentIndex = 0;
            lastSearchKey = key;
            lastSearchArea = area;
            return 0;
        }
        ArrayList<Integer> results = new ArrayList<>();
        int idx = 0;
        while ((idx = text.indexOf(key, idx)) >= 0) {
            results.add(idx);
            idx += key.length();
        }
        searchResults = results;
        currentIndex = 0;
        lastSearchKey = key;
        lastSearchArea = area;
        if (!results.isEmpty()) {
            int pos = results.get(0);
            area.setSelectionStart(pos);
            area.setSelectionEnd(pos + key.length());
            area.setCaretPosition(pos);
        }
        return results.size();
    }

    public static void navigate(String key, boolean next) {
        RSyntaxTextArea area = getActiveCodeArea();
        if (area == null || key == null || key.isEmpty()) {
            return;
        }
        if (searchResults == null || searchResults.isEmpty()
                || area != lastSearchArea || !key.equals(lastSearchKey)) {
            addSearchAction(key);
        }
        if (searchResults == null || searchResults.isEmpty()) {
            return;
        }
        int total = searchResults.size();
        if (next) {
            currentIndex = (currentIndex + 1) % total;
        } else {
            currentIndex = (currentIndex - 1 + total) % total;
        }
        int pos = searchResults.get(currentIndex);
        area.setSelectionStart(pos);
        area.setSelectionEnd(pos + key.length());
        area.setCaretPosition(pos);
    }

    public static void applyThemeToAll(Theme theme) {
        if (theme == null) {
            return;
        }
        currentTheme = theme;
        for (RSyntaxTextArea area : codeAreas) {
            if (area != null) {
                theme.apply(area);
                area.setFont(area.getFont().deriveFont(MainForm.FONT_SIZE));
            }
        }
    }

    private static void setActiveCodeArea(RSyntaxTextArea area) {
        if (area == null) {
            return;
        }
        codeArea = area;
        MainForm.setCodeArea(area);
    }

    private static JPanel createCodeTab(RSyntaxTextArea area, String title, String className) {
        RTextScrollPane sp = new RTextScrollPane(area);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(sp, BorderLayout.CENTER);
        panel.putClientProperty(PROP_AREA, area);
        panel.putClientProperty(PROP_CLASS, className);
        codeAreas.add(area);
        if (className != null) {
            classTabs.put(className, panel);
        }
        return panel;
    }

    private static JComponent buildTabHeader(String title, Component tab) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        header.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        JButton close = new JButton("x");
        close.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        close.setContentAreaFilled(false);
        close.setFocusable(false);
        close.addActionListener(e -> closeTab(tab));
        header.add(label);
        header.add(close);
        if (tab instanceof JComponent) {
            ((JComponent) tab).putClientProperty(PROP_LABEL, label);
        }
        return header;
    }

    private static RSyntaxTextArea getAreaFromTab(Component tab) {
        if (!(tab instanceof JComponent)) {
            return null;
        }
        Object area = ((JComponent) tab).getClientProperty(PROP_AREA);
        if (area instanceof RSyntaxTextArea) {
            return (RSyntaxTextArea) area;
        }
        return null;
    }

    private static void updateTabTitle(Component tab, String title) {
        if (codeTabs == null) {
            return;
        }
        int idx = codeTabs.indexOfComponent(tab);
        if (idx >= 0) {
            codeTabs.setTitleAt(idx, title);
        }
        if (tab instanceof JComponent) {
            Object label = ((JComponent) tab).getClientProperty(PROP_LABEL);
            if (label instanceof JLabel) {
                ((JLabel) label).setText(title);
            }
        }
    }

    private static void updateTabClass(Component tab, String className) {
        if (!(tab instanceof JComponent)) {
            return;
        }
        String old = (String) ((JComponent) tab).getClientProperty(PROP_CLASS);
        if (old != null) {
            classTabs.remove(old);
        }
        if (className != null) {
            classTabs.put(className, tab);
        }
        ((JComponent) tab).putClientProperty(PROP_CLASS, className);
        updateTabTitle(tab, buildTabTitle(className));
    }

    private static void closeTab(Component tab) {
        if (codeTabs == null || tab == null) {
            return;
        }
        if (codeTabs.getTabCount() <= 1) {
            RSyntaxTextArea area = getAreaFromTab(tab);
            if (area != null) {
                area.setText("");
                area.setCaretPosition(0);
            }
            updateTabClass(tab, null);
            return;
        }
        if (tab instanceof JComponent) {
            String className = (String) ((JComponent) tab).getClientProperty(PROP_CLASS);
            if (className != null) {
                classTabs.remove(className);
            }
            RSyntaxTextArea area = getAreaFromTab(tab);
            if (area != null) {
                codeAreas.remove(area);
            }
        }
        int idx = codeTabs.indexOfComponent(tab);
        if (idx >= 0) {
            codeTabs.removeTabAt(idx);
        }
        Component selected = codeTabs.getSelectedComponent();
        RSyntaxTextArea active = getAreaFromTab(selected);
        if (active != null) {
            setActiveCodeArea(active);
        }
    }

    private static String buildTabTitle(String className) {
        if (className == null || className.trim().isEmpty()) {
            return "Code";
        }
        String normalized = className;
        if (normalized.contains("/")) {
            return normalized.substring(normalized.lastIndexOf('/') + 1);
        }
        return normalized;
    }

    private static RSyntaxTextArea ensureTabForClass(String className, boolean openInNewTab) {
        if (codeTabs == null) {
            return codeArea;
        }
        String normalized = normalizeClassName(className);
        if (openInNewTab) {
            Component existing = classTabs.get(normalized);
            if (existing != null) {
                codeTabs.setSelectedComponent(existing);
                RSyntaxTextArea area = getAreaFromTab(existing);
                if (area != null) {
                    setActiveCodeArea(area);
                }
                return area;
            }
        }
        Component selected = codeTabs.getSelectedComponent();
        RSyntaxTextArea current = getAreaFromTab(selected);
        if (openInNewTab || current == null) {
            RSyntaxTextArea area = createCodeArea();
            CodeMenuHelper.install(area);
            String title = buildTabTitle(normalized);
            JPanel tab = createCodeTab(area, title, normalized);
            codeTabs.addTab(title, tab);
            int idx = codeTabs.indexOfComponent(tab);
            if (idx >= 0) {
                codeTabs.setTabComponentAt(idx, buildTabHeader(title, tab));
            }
            codeTabs.setSelectedComponent(tab);
            setActiveCodeArea(area);
            return area;
        }
        updateTabClass(selected, normalized);
        setActiveCodeArea(current);
        return current;
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

    private static SemanticTarget resolveSemanticTarget(String code, int caretOffset, String fallbackWord) {
        if (code == null || code.trim().isEmpty()) {
            return new SemanticTarget(SymbolKind.UNKNOWN, fallbackWord, null);
        }
        if (looksLikeResource(code)) {
            return new SemanticTarget(SymbolKind.UNKNOWN, fallbackWord, null);
        }
        CompilationUnit cu = parseCompilationUnit(code);
        if (cu == null) {
            return new SemanticTarget(SymbolKind.METHOD, fallbackWord, null);
        }
        Position pos = offsetToPosition(code, caretOffset);
        SimpleName picked = selectNameAt(cu, pos);
        if (picked == null) {
            return new SemanticTarget(SymbolKind.METHOD, fallbackWord, null);
        }
        SemanticTarget target = classifyName(cu, picked);
        if (target == null || target.kind == SymbolKind.UNKNOWN) {
            return new SemanticTarget(SymbolKind.METHOD, fallbackWord, null);
        }
        return target;
    }

    private static boolean looksLikeResource(String code) {
        if (code == null || code.trim().isEmpty()) {
            return true;
        }
        String t = code.trim();
        if (t.contains("class ") || t.contains("interface ")
                || t.contains("enum ") || t.contains("record ")
                || t.startsWith("package ")) {
            return false;
        }
        if (t.startsWith("{") || t.startsWith("[") || t.startsWith("<")) {
            String lower = t.toLowerCase();
            if (lower.startsWith("<html") || lower.startsWith("<?xml") || lower.startsWith("<!doctype")) {
                return true;
            }
            return t.startsWith("{") || t.startsWith("[");
        }
        return false;
    }

    private static CompilationUnit parseCompilationUnit(String code) {
        if (code == null) {
            return null;
        }
        synchronized (PARSE_LOCK) {
            if (code.equals(lastParsedCode) && lastParsedUnit != null) {
                return lastParsedUnit;
            }
            try {
                CompilationUnit cu = JarAnalyzerParser.buildInstance(code);
                lastParsedCode = code;
                lastParsedUnit = cu;
                return cu;
            } catch (Exception ex) {
                logger.debug("parse code failed: {}", ex.getMessage());
                lastParsedCode = null;
                lastParsedUnit = null;
                return null;
            }
        }
    }

    private static SimpleName selectNameAt(CompilationUnit cu, Position pos) {
        if (cu == null || pos == null) {
            return null;
        }
        List<SimpleName> names = cu.findAll(SimpleName.class);
        if (names == null || names.isEmpty()) {
            return null;
        }
        SimpleName best = null;
        int bestLen = Integer.MAX_VALUE;
        for (SimpleName name : names) {
            if (!name.getRange().isPresent()) {
                continue;
            }
            Range r = name.getRange().get();
            if (contains(r, pos)) {
                int len = rangeLength(r);
                if (len < bestLen) {
                    best = name;
                    bestLen = len;
                }
            }
        }
        return best;
    }

    private static SemanticTarget classifyName(CompilationUnit cu, SimpleName name) {
        if (name == null) {
            return new SemanticTarget(SymbolKind.UNKNOWN, null, null);
        }
        Node parent = name.getParentNode().orElse(null);
        if (parent instanceof MethodCallExpr) {
            MethodCallExpr call = (MethodCallExpr) parent;
            if (call.getName() == name) {
                String classHint = resolveScopeClassName(cu, call);
                return new SemanticTarget(SymbolKind.METHOD, call.getNameAsString(), classHint);
            }
        }
        if (parent instanceof MethodReferenceExpr) {
            MethodReferenceExpr ref = (MethodReferenceExpr) parent;
            if (ref.getIdentifier() != null && ref.getIdentifier().equals(name.asString())) {
                return new SemanticTarget(SymbolKind.METHOD, name.asString(), null);
            }
        }
        if (parent instanceof MethodDeclaration) {
            return new SemanticTarget(SymbolKind.METHOD, name.asString(), resolveEnclosingClassName(cu, parent));
        }
        if (parent instanceof ConstructorDeclaration) {
            String cls = resolveEnclosingClassName(cu, parent);
            return new SemanticTarget(SymbolKind.CONSTRUCTOR, name.asString(), cls);
        }
        if (parent instanceof ClassOrInterfaceDeclaration
                || parent instanceof EnumDeclaration
                || parent instanceof RecordDeclaration
                || parent instanceof AnnotationDeclaration) {
            String cls = resolveQualifiedClassName(cu, name.asString());
            return new SemanticTarget(SymbolKind.CLASS, name.asString(), cls);
        }
        if (parent instanceof TypeParameter) {
            Range range = name.getRange().orElse(null);
            return new SemanticTarget(SymbolKind.TYPE_PARAM, name.asString(), null, range, null);
        }
        if (parent instanceof ClassOrInterfaceType) {
            String cls = resolveQualifiedClassName(cu, name.asString());
            if (isConstructorContext(parent)) {
                return new SemanticTarget(SymbolKind.CONSTRUCTOR, name.asString(), cls);
            }
            return new SemanticTarget(SymbolKind.CLASS, name.asString(), cls);
        }
        DeclaredSymbol decl = resolveDeclaration(cu, name);
        if (decl != null) {
            return new SemanticTarget(decl.kind, decl.name, null, decl.range, decl.typeName);
        }
        return new SemanticTarget(SymbolKind.UNKNOWN, name.asString(), null);
    }

    private static boolean isConstructorContext(Node node) {
        Node cur = node;
        while (cur != null) {
            if (cur instanceof ObjectCreationExpr) {
                return true;
            }
            cur = cur.getParentNode().orElse(null);
        }
        return false;
    }

    private static String resolveScopeClassName(CompilationUnit cu, MethodCallExpr call) {
        if (call == null) {
            return null;
        }
        if (!call.getScope().isPresent()) {
            return resolveEnclosingClassName(cu, call);
        }
        Expression scope = unwrapEnclosed(call.getScope().get());
        return resolveClassNameFromExpression(cu, scope, call);
    }

    private static Expression unwrapEnclosed(Expression expr) {
        Expression cur = expr;
        while (cur instanceof EnclosedExpr) {
            cur = ((EnclosedExpr) cur).getInner();
        }
        return cur;
    }

    private static String resolveClassNameFromExpression(CompilationUnit cu, Expression expr, Node context) {
        if (expr == null) {
            return null;
        }
        Expression scope = unwrapEnclosed(expr);
        if (scope instanceof ThisExpr || scope instanceof SuperExpr) {
            return resolveEnclosingClassName(cu, context != null ? context : scope);
        }
        if (scope instanceof NameExpr) {
            String name = ((NameExpr) scope).getNameAsString();
            if (looksLikeQualifiedClassName(name) || looksLikeClassName(name)) {
                return resolveQualifiedClassName(cu, name);
            }
            DeclaredSymbol decl = resolveDeclaration(cu, ((NameExpr) scope).getName());
            if (decl != null && decl.typeName != null) {
                return decl.typeName;
            }
            return null;
        }
        if (scope instanceof FieldAccessExpr) {
            FieldAccessExpr fa = (FieldAccessExpr) scope;
            Expression faScope = unwrapEnclosed(fa.getScope());
            if (faScope instanceof ThisExpr || faScope instanceof SuperExpr) {
                DeclaredSymbol field = resolveFieldDeclaration(cu, fa.getNameAsString(), fa);
                if (field != null && field.typeName != null) {
                    return field.typeName;
                }
            }
            String text = fa.toString();
            if (looksLikeQualifiedClassName(text)) {
                return resolveQualifiedClassName(cu, text);
            }
            if (faScope instanceof NameExpr) {
                String scopeName = ((NameExpr) faScope).getNameAsString();
                if (looksLikeClassName(scopeName)) {
                    return resolveQualifiedClassName(cu, text);
                }
            }
            return null;
        }
        if (scope instanceof ObjectCreationExpr) {
            String typeName = ((ObjectCreationExpr) scope).getTypeAsString();
            return resolveTypeToClassName(cu, typeName);
        }
        return null;
    }

    private static DeclaredSymbol resolveDeclaration(CompilationUnit cu, SimpleName name) {
        if (name == null) {
            return null;
        }
        Node parent = name.getParentNode().orElse(null);
        if (parent instanceof Parameter) {
            Parameter p = (Parameter) parent;
            Range range = p.getName().getRange().orElse(null);
            String typeName = resolveTypeToClassName(cu, p.getTypeAsString());
            return new DeclaredSymbol(SymbolKind.VARIABLE, p.getNameAsString(), range, typeName);
        }
        if (parent instanceof VariableDeclarator) {
            VariableDeclarator vd = (VariableDeclarator) parent;
            Node varParent = vd.getParentNode().orElse(null);
            SymbolKind kind = varParent instanceof com.github.javaparser.ast.body.FieldDeclaration
                    ? SymbolKind.FIELD
                    : SymbolKind.VARIABLE;
            Range range = vd.getName().getRange().orElse(null);
            String typeName = resolveTypeToClassName(cu, vd.getType().asString());
            return new DeclaredSymbol(kind, vd.getNameAsString(), range, typeName);
        }
        Position usagePos = name.getBegin().orElse(null);
        DeclaredSymbol local = resolveLocalDeclaration(cu, name.asString(), usagePos, name);
        if (local != null) {
            return local;
        }
        return resolveFieldDeclaration(cu, name.asString(), name);
    }

    private static DeclaredSymbol resolveLocalDeclaration(CompilationUnit cu,
                                                          String name,
                                                          Position usagePos,
                                                          Node context) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        Node scopeRoot = findScopeRoot(context);
        if (scopeRoot == null) {
            return null;
        }
        long usageKey = usagePos == null ? Long.MAX_VALUE : positionKey(usagePos);
        DeclaredSymbol best = null;
        long bestKey = Long.MIN_VALUE;
        List<Parameter> params = scopeRoot.findAll(Parameter.class);
        for (Parameter param : params) {
            if (!name.equals(param.getNameAsString())) {
                continue;
            }
            Position pos = param.getBegin().orElse(null);
            if (pos == null) {
                continue;
            }
            long key = positionKey(pos);
            if (key > usageKey) {
                continue;
            }
            if (key > bestKey) {
                Range range = param.getName().getRange().orElse(null);
                String typeName = resolveTypeToClassName(cu, param.getTypeAsString());
                best = new DeclaredSymbol(SymbolKind.VARIABLE, name, range, typeName);
                bestKey = key;
            }
        }
        List<VariableDeclarator> vars = scopeRoot.findAll(VariableDeclarator.class);
        for (VariableDeclarator var : vars) {
            if (!name.equals(var.getNameAsString())) {
                continue;
            }
            Node varParent = var.getParentNode().orElse(null);
            if (varParent instanceof com.github.javaparser.ast.body.FieldDeclaration) {
                continue;
            }
            Position pos = var.getBegin().orElse(null);
            if (pos == null) {
                continue;
            }
            long key = positionKey(pos);
            if (key > usageKey) {
                continue;
            }
            if (key > bestKey) {
                Range range = var.getName().getRange().orElse(null);
                String typeName = resolveTypeToClassName(cu, var.getType().asString());
                best = new DeclaredSymbol(SymbolKind.VARIABLE, name, range, typeName);
                bestKey = key;
            }
        }
        return best;
    }

    private static DeclaredSymbol resolveFieldDeclaration(CompilationUnit cu, String name, Node context) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        Node typeNode = findEnclosingType(context);
        if (typeNode instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration decl = (ClassOrInterfaceDeclaration) typeNode;
            return findFieldIn(decl.getFields(), cu, name);
        }
        if (typeNode instanceof EnumDeclaration) {
            EnumDeclaration decl = (EnumDeclaration) typeNode;
            return findFieldIn(decl.getFields(), cu, name);
        }
        if (typeNode instanceof RecordDeclaration) {
            RecordDeclaration decl = (RecordDeclaration) typeNode;
            return findFieldIn(decl.getFields(), cu, name);
        }
        return null;
    }

    private static Node findEnclosingType(Node node) {
        Node cur = node;
        while (cur != null) {
            if (cur instanceof ClassOrInterfaceDeclaration
                    || cur instanceof EnumDeclaration
                    || cur instanceof RecordDeclaration
                    || cur instanceof AnnotationDeclaration) {
                return cur;
            }
            cur = cur.getParentNode().orElse(null);
        }
        return null;
    }

    private static DeclaredSymbol findFieldIn(List<com.github.javaparser.ast.body.FieldDeclaration> fields,
                                              CompilationUnit cu,
                                              String name) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        for (com.github.javaparser.ast.body.FieldDeclaration field : fields) {
            for (VariableDeclarator var : field.getVariables()) {
                if (!name.equals(var.getNameAsString())) {
                    continue;
                }
                Range range = var.getName().getRange().orElse(null);
                String typeName = resolveTypeToClassName(cu, var.getType().asString());
                return new DeclaredSymbol(SymbolKind.FIELD, name, range, typeName);
            }
        }
        return null;
    }

    private static String resolveTypeToClassName(CompilationUnit cu, String typeName) {
        String normalized = normalizeTypeName(typeName);
        if (normalized == null) {
            return null;
        }
        return resolveQualifiedClassName(cu, normalized);
    }

    private static String normalizeTypeName(String typeName) {
        if (typeName == null) {
            return null;
        }
        String trimmed = typeName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int genericIndex = trimmed.indexOf('<');
        if (genericIndex >= 0) {
            trimmed = trimmed.substring(0, genericIndex);
        }
        while (trimmed.endsWith("[]")) {
            trimmed = trimmed.substring(0, trimmed.length() - 2);
        }
        trimmed = trimmed.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if ("var".equals(trimmed)) {
            return null;
        }
        if (isPrimitiveType(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private static boolean isPrimitiveType(String typeName) {
        if (typeName == null) {
            return false;
        }
        switch (typeName) {
            case "boolean":
            case "byte":
            case "short":
            case "int":
            case "long":
            case "char":
            case "float":
            case "double":
            case "void":
                return true;
            default:
                return false;
        }
    }

    private static long positionKey(Position pos) {
        if (pos == null) {
            return Long.MIN_VALUE;
        }
        return (((long) pos.line) << 32) | (pos.column & 0xffffffffL);
    }

    private static Node findScopeRoot(Node node) {
        Node cur = node;
        while (cur != null) {
            if (cur instanceof MethodDeclaration
                    || cur instanceof ConstructorDeclaration
                    || cur instanceof LambdaExpr
                    || cur instanceof InitializerDeclaration) {
                return cur;
            }
            cur = cur.getParentNode().orElse(null);
        }
        return null;
    }

    private static String resolveQualifiedClassName(CompilationUnit cu, String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.contains("<")) {
            trimmed = trimmed.substring(0, trimmed.indexOf('<'));
        }
        if (trimmed.contains("/")) {
            return resolveFromIndex(cu, trimmed, null);
        }
        if (trimmed.contains(".")) {
            String candidate = trimmed.replace('.', '/');
            return resolveFromIndex(cu, candidate, null);
        }
        if (cu != null) {
            String fromImport = resolveFromImports(cu, trimmed);
            if (fromImport != null) {
                return resolveFromIndex(cu, fromImport, trimmed);
            }
            if (cu.getPackageDeclaration().isPresent()) {
                String pkg = cu.getPackageDeclaration().get().getNameAsString();
                if (pkg != null && !pkg.trim().isEmpty()) {
                    String candidate = (pkg + "." + trimmed).replace('.', '/');
                    String resolved = resolveFromIndex(cu, candidate, trimmed);
                    if (resolved != null) {
                        return resolved;
                    }
                }
            }
        }
        String fallback = resolveFromIndex(cu, null, trimmed);
        return fallback == null ? trimmed : fallback;
    }

    private static String resolveFromIndex(CompilationUnit cu, String candidate, String simpleName) {
        if (candidate != null && existsInDatabase(candidate)) {
            return candidate;
        }
        if (simpleName == null || simpleName.trim().isEmpty()) {
            return candidate;
        }
        if (MainForm.getEngine() == null) {
            return candidate;
        }
        List<String> matches = MainForm.getEngine().includeClassByClassName(simpleName);
        if (matches == null || matches.isEmpty()) {
            return candidate;
        }
        String pkg = null;
        if (cu != null && cu.getPackageDeclaration().isPresent()) {
            pkg = cu.getPackageDeclaration().get().getNameAsString();
        }
        if (pkg != null && !pkg.trim().isEmpty()) {
            String pkgPrefix = pkg.replace('.', '/') + "/" + simpleName;
            for (String match : matches) {
                if (match.equals(pkgPrefix) || match.startsWith(pkgPrefix + "$")) {
                    return match;
                }
            }
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        String shortest = matches.get(0);
        for (String match : matches) {
            if (match != null && match.length() < shortest.length()) {
                shortest = match;
            }
        }
        return shortest != null ? shortest : candidate;
    }

    private static boolean existsInDatabase(String className) {
        if (className == null || className.trim().isEmpty()) {
            return false;
        }
        if (MainForm.getEngine() == null) {
            return true;
        }
        return MainForm.getEngine().getClassByClass(className) != null;
    }

    private static String resolveFromImports(CompilationUnit cu, String simpleName) {
        if (cu == null || simpleName == null) {
            return null;
        }
        List<com.github.javaparser.ast.ImportDeclaration> imports = cu.getImports();
        if (imports == null || imports.isEmpty()) {
            return null;
        }
        for (com.github.javaparser.ast.ImportDeclaration imp : imports) {
            if (imp.isStatic()) {
                continue;
            }
            String q = imp.getNameAsString();
            if (imp.isAsterisk()) {
                if (q != null && !q.trim().isEmpty()) {
                    return (q + "." + simpleName).replace('.', '/');
                }
            } else {
                if (q != null && q.endsWith("." + simpleName)) {
                    return q.replace('.', '/');
                }
            }
        }
        return null;
    }

    private static boolean looksLikeClassName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        char c = name.charAt(0);
        return Character.isUpperCase(c);
    }

    private static boolean looksLikeQualifiedClassName(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String[] parts = text.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        return looksLikeClassName(parts[parts.length - 1]);
    }

    private static String resolveEnclosingClassName(CompilationUnit cu, Node node) {
        List<String> names = new ArrayList<>();
        Node cur = node;
        while (cur != null) {
            if (cur instanceof ClassOrInterfaceDeclaration) {
                names.add(0, ((ClassOrInterfaceDeclaration) cur).getNameAsString());
            } else if (cur instanceof EnumDeclaration) {
                names.add(0, ((EnumDeclaration) cur).getNameAsString());
            } else if (cur instanceof RecordDeclaration) {
                names.add(0, ((RecordDeclaration) cur).getNameAsString());
            } else if (cur instanceof AnnotationDeclaration) {
                names.add(0, ((AnnotationDeclaration) cur).getNameAsString());
            }
            cur = cur.getParentNode().orElse(null);
        }
        if (names.isEmpty()) {
            return inferClassNameFromAst(cu);
        }
        String pkg = null;
        if (cu != null && cu.getPackageDeclaration().isPresent()) {
            pkg = cu.getPackageDeclaration().get().getNameAsString();
        }
        StringBuilder sb = new StringBuilder();
        if (pkg != null && !pkg.trim().isEmpty()) {
            sb.append(pkg.replace('.', '/')).append("/");
        }
        sb.append(names.get(0));
        for (int i = 1; i < names.size(); i++) {
            sb.append("$").append(names.get(i));
        }
        return sb.toString();
    }

    private static String inferClassNameFromAst(CompilationUnit cu) {
        if (cu == null) {
            return null;
        }
        return inferClassName(cu.toString());
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
        Matcher clsMatcher = Pattern.compile(
                "(?m)^\\s*(?:public|protected|private|abstract|final|static|sealed|non-sealed|strictfp|\\s)*" +
                        "\\s*(?:class|interface|enum|record|@interface)\\s+([\\w$]+)")
                .matcher(code);
        if (clsMatcher.find()) {
            String name = clsMatcher.group(1);
            if (name == null || name.trim().isEmpty()) {
                return null;
            }
            if (pkg != null && !pkg.trim().isEmpty()) {
                return pkg.replace('.', '/') + "/" + name;
            }
            return name;
        }
        return null;
    }

    private static boolean openClassInEditor(String className,
                                             String methodName,
                                             String methodDesc,
                                             boolean preferExisting,
                                             boolean warnOnMissing,
                                             boolean openInNewTab) {
        if (className == null || className.trim().isEmpty()) {
            return false;
        }
        if (MainForm.getEngine() == null) {
            return false;
        }
        String normalized = normalizeClassName(className);
        RSyntaxTextArea targetArea = ensureTabForClass(normalized, openInNewTab);
        if (targetArea == null) {
            return false;
        }
        String existingCode = null;
        if (preferExisting) {
            String text = targetArea.getText();
            if (text != null && !text.trim().isEmpty() && looksLikeJava(text)) {
                existingCode = text;
            }
        }
        String classPath = resolveClassPath(normalized);
        if (classPath == null) {
            if (warnOnMissing) {
                SyntaxAreaHelper.runOnEdt(() -> JOptionPane.showMessageDialog(
                        MainForm.getInstance().getMasterPanel(),
                        "<html><p>need dependency or class file not found</p></html>"));
            }
            return false;
        }
        if (LuceneSearchForm.getInstance() != null && LuceneSearchForm.usePaLucene()) {
            IndexPluginsSupport.addIndex(Paths.get(classPath).toFile());
        }
        String code = existingCode;
        if (code == null) {
            code = DecompileEngine.decompile(Paths.get(classPath));
        }
        if (code == null || code.trim().isEmpty()) {
            if (warnOnMissing) {
                SyntaxAreaHelper.runOnEdt(() -> JOptionPane.showMessageDialog(
                        MainForm.getInstance().getMasterPanel(),
                        "<html><p>code not found</p></html>"));
            }
            return false;
        }
        int caretPos = 0;
        if (methodName != null && methodDesc != null) {
            int pos = FinderRunner.find(code, methodName, methodDesc);
            caretPos = Math.max(0, pos + 1);
        }
        int finalCaret = caretPos;
        String finalCode = code;
        String finalClassName = normalized;
        RSyntaxTextArea finalArea = targetArea;
        SyntaxAreaHelper.runOnEdt(() -> {
            setActiveCodeArea(finalArea);
            SearchInputListener.getFileTree().searchPathTarget(finalClassName);
            finalArea.setText(finalCode);
            finalArea.setCaretPosition(finalCaret);
            MainForm.setCurClass(finalClassName);
            MainForm.getInstance().getCurClassText().setText(finalClassName);
            String jarName = MainForm.getEngine().getJarByClass(finalClassName);
            if (jarName == null || jarName.trim().isEmpty()) {
                jarName = RuntimeClassResolver.getJarName(finalClassName);
            }
            if (jarName != null && !jarName.trim().isEmpty()) {
                MainForm.getInstance().getCurJarText().setText(jarName);
            }
            if (methodName != null && !methodName.trim().isEmpty()) {
                MainForm.getInstance().getCurMethodText().setText(methodName);
            }
        });
        pushStateIfNeeded(normalized, methodName, methodDesc, classPath);
        return true;
    }

    public static String resolveClassPath(String className) {
        String normalized = normalizeClassName(className);
        String path = MainForm.getEngine().getAbsPath(normalized);
        if (path != null && Files.exists(Paths.get(path))) {
            return path;
        }
        String tempPath = resolveTempClassPath(normalized);
        if (tempPath != null) {
            return tempPath;
        }
        RuntimeClassResolver.ResolvedClass resolved = RuntimeClassResolver.resolve(normalized);
        if (resolved != null && resolved.getClassFile() != null) {
            return resolved.getClassFile().toAbsolutePath().toString();
        }
        return null;
    }

    private static String resolveTempClassPath(String className) {
        String tempPath = className.replace("/", File.separator);
        String direct = String.format("%s%s%s.class", Const.tempDir, File.separator, tempPath);
        if (Files.exists(Paths.get(direct))) {
            return direct;
        }
        String boot = String.format("%s%sBOOT-INF%sclasses%s%s.class",
                Const.tempDir, File.separator, File.separator, File.separator, tempPath);
        if (Files.exists(Paths.get(boot))) {
            return boot;
        }
        String web = String.format("%s%sWEB-INF%sclasses%s%s.class",
                Const.tempDir, File.separator, File.separator, File.separator, tempPath);
        if (Files.exists(Paths.get(web))) {
            return web;
        }
        return null;
    }

    private static String normalizeClassName(String className) {
        String normalized = className.trim();
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - 6);
        }
        if (normalized.contains(".")) {
            normalized = normalized.replace('.', '/');
        }
        return normalized;
    }

    private static boolean looksLikeJava(String text) {
        String t = text.trim();
        if (t.isEmpty()) {
            return false;
        }
        if (t.contains("class ") || t.contains("interface ") || t.contains("enum ")) {
            return true;
        }
        return t.contains("package ") && t.contains(";");
    }

    private static void pushStateIfNeeded(String className, String methodName, String methodDesc, String classPath) {
        if (className == null || className.trim().isEmpty()) {
            return;
        }
        if (methodName == null || methodName.trim().isEmpty()) {
            return;
        }
        if (methodDesc == null || methodDesc.trim().isEmpty()) {
            return;
        }
        if (classPath == null || classPath.trim().isEmpty()) {
            return;
        }
        MethodResult res = new MethodResult();
        res.setClassName(className);
        res.setMethodName(methodName);
        res.setMethodDesc(methodDesc);
        res.setClassPath(Paths.get(classPath));
        String jarName = MainForm.getEngine().getJarByClass(className);
        if (jarName != null && !jarName.trim().isEmpty()) {
            res.setJarName(jarName);
        }
        MainForm.setCurMethod(res);

        State newState = new State();
        newState.setClassPath(Paths.get(classPath));
        newState.setJarName(jarName);
        newState.setClassName(className);
        newState.setMethodDesc(methodDesc);
        newState.setMethodName(methodName);

        int curSI = MainForm.getCurStateIndex();
        if (curSI == -1) {
            MainForm.getStateList().add(curSI + 1, newState);
            MainForm.setCurStateIndex(curSI + 1);
            return;
        }
        if (curSI >= MainForm.getStateList().size()) {
            curSI = MainForm.getStateList().size() - 1;
        }
        State state = MainForm.getStateList().get(curSI);
        if (state != null) {
            int a = MainForm.getStateList().size();
            MainForm.getStateList().add(curSI + 1, newState);
            int b = MainForm.getStateList().size();
            if (a == b) {
                MainForm.setCurStateIndex(curSI);
            } else {
                MainForm.setCurStateIndex(curSI + 1);
            }
        } else {
            logger.warn("current state is null");
        }
    }

    private static boolean jumpToDeclaration(String code, Range range) {
        if (codeArea == null || code == null || range == null) {
            return false;
        }
        int start = positionToOffset(code, range.begin);
        int end = positionToOffset(code, range.end);
        if (start < 0 || end < 0) {
            return false;
        }
        if (end <= start) {
            end = Math.min(code.length(), start + 1);
        }
        int finalStart = start;
        int finalEnd = end;
        SyntaxAreaHelper.runOnEdt(() -> {
            codeArea.setCaretPosition(finalStart);
            Highlighter highlighter = codeArea.getHighlighter();
            highlighter.removeAllHighlights();
            try {
                highlighter.addHighlight(finalStart, finalEnd,
                        new DefaultHighlighter.DefaultHighlightPainter(Color.ORANGE));
            } catch (BadLocationException ignored) {
            }
        });
        return true;
    }

    private static int positionToOffset(String code, Position position) {
        if (code == null || position == null) {
            return -1;
        }
        String[] lines = code.split("\n", -1);
        if (position.line <= 0 || position.line > lines.length) {
            return -1;
        }
        int charCount = 0;
        for (int i = 0; i < position.line - 1; i++) {
            charCount += lines[i].length() + 1;
        }
        int lineLen = lines[position.line - 1].length();
        int column = Math.max(1, position.column);
        int offsetInLine = Math.min(column - 1, lineLen);
        charCount += offsetInLine;
        return charCount;
    }

    private static Position offsetToPosition(String code, int offset) {
        int line = 1;
        int column = 1;
        int limit = Math.min(offset, code.length());
        for (int i = 0; i < limit; i++) {
            char c = code.charAt(i);
            if (c == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new Position(line, column);
    }

    private static boolean contains(Range range, Position pos) {
        if (range == null || pos == null) {
            return false;
        }
        if (pos.line < range.begin.line || pos.line > range.end.line) {
            return false;
        }
        if (pos.line == range.begin.line && pos.column < range.begin.column) {
            return false;
        }
        if (pos.line == range.end.line && pos.column > range.end.column) {
            return false;
        }
        return true;
    }

    private static int rangeLength(Range range) {
        if (range == null) {
            return Integer.MAX_VALUE;
        }
        if (range.begin.line == range.end.line) {
            return Math.max(1, range.end.column - range.begin.column);
        }
        return Integer.MAX_VALUE - 1;
    }

    private static int findWordStart(String text, int pos) {
        if (text == null || text.isEmpty() || pos <= 0) {
            return -1;
        }
        int i = Math.min(pos - 1, text.length() - 1);
        while (i >= 0 && !Character.isJavaIdentifierPart(text.charAt(i))) {
            i--;
        }
        if (i < 0) {
            return -1;
        }
        while (i > 0 && Character.isJavaIdentifierPart(text.charAt(i - 1))) {
            i--;
        }
        return i;
    }

    private static int findWordEnd(String text, int pos) {
        if (text == null || text.isEmpty()) {
            return -1;
        }
        int i = Math.min(pos, text.length());
        while (i < text.length() && !Character.isJavaIdentifierPart(text.charAt(i))) {
            i++;
        }
        if (i >= text.length()) {
            return -1;
        }
        while (i < text.length() && Character.isJavaIdentifierPart(text.charAt(i))) {
            i++;
        }
        return i;
    }

    private enum SymbolKind {
        METHOD,
        CONSTRUCTOR,
        CLASS,
        FIELD,
        VARIABLE,
        TYPE_PARAM,
        UNKNOWN
    }

    private static final class DeclaredSymbol {
        private final SymbolKind kind;
        private final String name;
        private final Range range;
        private final String typeName;

        private DeclaredSymbol(SymbolKind kind, String name, Range range, String typeName) {
            this.kind = kind;
            this.name = name;
            this.range = range;
            this.typeName = typeName;
        }
    }

    private static final class SemanticTarget {
        private final SymbolKind kind;
        private final String name;
        private final String className;
        private final Range declarationRange;
        private final String typeName;

        private SemanticTarget(SymbolKind kind, String name, String className) {
            this(kind, name, className, null, null);
        }

        private SemanticTarget(SymbolKind kind,
                               String name,
                               String className,
                               Range declarationRange,
                               String typeName) {
            this.kind = kind;
            this.name = name;
            this.className = className;
            this.declarationRange = declarationRange;
            this.typeName = typeName;
        }
    }
}
