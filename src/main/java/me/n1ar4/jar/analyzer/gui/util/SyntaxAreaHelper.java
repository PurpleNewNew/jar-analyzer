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
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.CoreHelper;
import me.n1ar4.jar.analyzer.engine.DecompileDispatcher;
import me.n1ar4.jar.analyzer.engine.DecompileEngine;
import me.n1ar4.jar.analyzer.engine.DecompileType;
import me.n1ar4.jar.analyzer.entity.LineMappingEntity;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.OpcodeForm;
import me.n1ar4.jar.analyzer.gui.adapter.GlobalKeyListener;
import me.n1ar4.jar.analyzer.gui.adapter.SearchInputListener;
import me.n1ar4.jar.analyzer.gui.state.State;
import me.n1ar4.jar.analyzer.gui.util.ProcessDialog;
import me.n1ar4.jar.analyzer.semantic.CallContext;
import me.n1ar4.jar.analyzer.semantic.CallResolver;
import me.n1ar4.jar.analyzer.semantic.EnclosingCallable;
import me.n1ar4.jar.analyzer.semantic.SemanticResolver;
import me.n1ar4.jar.analyzer.semantic.SymbolRef;
import me.n1ar4.jar.analyzer.semantic.TypeRef;
import me.n1ar4.jar.analyzer.semantic.TypeSolver;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;
import me.n1ar4.jar.analyzer.utils.ExternalClassIndex;
import me.n1ar4.jar.analyzer.utils.JarUtil;
import me.n1ar4.jar.analyzer.utils.RuntimeClassResolver;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import me.n1ar4.parser.JarAnalyzerParser;
import org.objectweb.asm.Type;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
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
    private static final AtomicInteger HIGHLIGHT_SEQ = new AtomicInteger(0);
    private static final AtomicInteger NAV_SEQ = new AtomicInteger(0);
    private static final int ARG_COUNT_UNKNOWN = -1;
    private static final Map<String, ClassLineMapping> LINE_MAPPINGS = new ConcurrentHashMap<>();
    private static volatile TypeSolver TYPE_SOLVER;
    private static volatile SemanticResolver SEMANTIC_RESOLVER;
    private static volatile CoreEngine SEMANTIC_ENGINE;
    private static final String DECOMPILER_CFR = "cfr";
    private static final String DECOMPILER_FERN = "fern";

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
                String jarName = resolveJarName(normalized);
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
        return selectGlobalMethod(methodName, ARG_COUNT_UNKNOWN);
    }

    private static MethodResult selectGlobalMethod(String methodName, int argCount) {
        if (methodName == null || methodName.trim().isEmpty()) {
            return null;
        }
        if (MainForm.getEngine() == null) {
            return null;
        }
        List<MethodResult> candidates = MainForm.getEngine().getMethodLike(null, methodName, null);
        List<MethodResult> filtered = filterByArgCount(candidates, argCount);
        List<MethodResult> candidatesForPick = (filtered != null && !filtered.isEmpty())
                ? filtered
                : candidates;
        if (candidatesForPick == null || candidatesForPick.isEmpty()) {
            return null;
        }
        if (candidatesForPick.size() == 1) {
            return candidatesForPick.get(0);
        }
        final List<MethodResult> finalCandidates = candidatesForPick;
        return callOnEdt(() -> {
            DefaultListModel<MethodResult> model = new DefaultListModel<>();
            for (MethodResult result : finalCandidates) {
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

    private static String pickClassFromPackage(String pkg, List<String> classes) {
        if (classes == null || classes.isEmpty()) {
            return null;
        }
        if (classes.size() == 1) {
            return classes.get(0);
        }
        List<String> values = new ArrayList<>();
        List<String> displayValues = new ArrayList<>();
        for (String cls : classes) {
            if (cls == null || cls.trim().isEmpty()) {
                continue;
            }
            String display = cls.replace('/', '.');
            String jar = resolveJarName(cls);
            if (jar != null && !jar.trim().isEmpty()) {
                display = display + " [" + jar + "]";
            }
            values.add(cls);
            displayValues.add(display);
        }
        if (values.isEmpty()) {
            return null;
        }
        return callOnEdt(() -> {
            DefaultListModel<String> model = new DefaultListModel<>();
            for (String display : displayValues) {
                model.addElement(display);
            }
            JList<String> list = new JList<>(model);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setVisibleRowCount(Math.min(12, values.size()));
            JScrollPane pane = new JScrollPane(list);
            int option = JOptionPane.showConfirmDialog(
                    MainForm.getInstance().getMasterPanel(),
                    pane,
                    "Select class in package: " + pkg,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (option != JOptionPane.OK_OPTION) {
                return null;
            }
            int idx = list.getSelectedIndex();
            if (idx < 0 || idx >= values.size()) {
                return values.get(0);
            }
            return values.get(idx);
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

    private static List<MethodResult> filterByArgCount(List<MethodResult> candidates, int argCount) {
        if (argCount == ARG_COUNT_UNKNOWN) {
            return candidates;
        }
        if (candidates == null || candidates.isEmpty()) {
            return candidates;
        }
        List<MethodResult> filtered = new ArrayList<>();
        for (MethodResult result : candidates) {
            if (result == null) {
                continue;
            }
            String desc = result.getMethodDesc();
            if (desc == null || desc.trim().isEmpty()) {
                continue;
            }
            try {
                Type[] args = Type.getArgumentTypes(desc);
                if (args.length == argCount) {
                    filtered.add(result);
                }
            } catch (Exception ignored) {
            }
        }
        return filtered;
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
            String needle = selectedText;
            int seq = HIGHLIGHT_SEQ.incrementAndGet();
            Highlighter highlighter = rArea.getHighlighter();
            highlighter.removeAllHighlights();
            String text = rArea.getText();
            if (needle.length() < 2) {
                return;
            }
            UiExecutor.runAsync(() -> {
                List<int[]> ranges = new ArrayList<>();
                int index = 0;
                while ((index = text.indexOf(needle, index)) >= 0) {
                    ranges.add(new int[]{index, index + needle.length()});
                    index += needle.length();
                    if (ranges.size() >= 500) {
                        break;
                    }
                }
                runOnEdt(() -> {
                    if (seq != HIGHLIGHT_SEQ.get()) {
                        return;
                    }
                    Highlighter hl = rArea.getHighlighter();
                    hl.removeAllHighlights();
                    for (int[] range : ranges) {
                        try {
                            hl.addHighlight(range[0], range[1],
                                    new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW));
                        } catch (BadLocationException ignored) {
                        }
                    }
                });
            });
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
                        UiExecutor.runAsync(() -> {
                            JDialog dialog = null;
                            try {
                                SemanticTarget target = resolveSemanticTarget(codeSnapshot, caretSnapshot, methodNameSnapshot);
                                CompilationUnit cuSnapshot = parseCompilationUnit(codeSnapshot);
                                Position caretPos = codeSnapshot == null ? null : offsetToPosition(codeSnapshot, caretSnapshot);
                                CallerContext callerContext = resolveCallerContext(
                                        cuSnapshot, caretPos, curClassSnapshot, codeSnapshot);
                                CallPosition callPosition = resolveCallPosition(cuSnapshot, caretPos, callerContext);
                                MethodReferenceExpr methodRef = findMethodReferenceAt(cuSnapshot, caretPos);
                                if (methodRef != null && target != null) {
                                    int refArgCount = resolveMethodReferenceArgCount(cuSnapshot, methodRef, callerContext);
                                    if (refArgCount != ARG_COUNT_UNKNOWN && target.argCount == ARG_COUNT_UNKNOWN) {
                                        target = new SemanticTarget(target.kind, target.name, target.className,
                                                target.declarationRange, target.typeName, refArgCount);
                                    }
                                }
                                if (methodRef != null) {
                                    callPosition = null;
                                }
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
                                    String typeName = target.typeName;
                                    if ((typeName == null || typeName.trim().isEmpty())
                                            && target.kind == SymbolKind.VARIABLE
                                            && callerContext != null) {
                                        TypeSolver solver = getTypeSolver();
                                        if (solver != null) {
                                            typeName = solver.resolveLocalVarTypeFromDb(
                                                    callerContext.className,
                                                    callerContext.methodName,
                                                    callerContext.methodDesc,
                                                    target.name,
                                                    callerContext.line);
                                        }
                                    }
                                    if (typeName != null && !typeName.trim().isEmpty()) {
                                        String finalClassName = typeName;
                                        SyntaxAreaHelper.runOnEdt(() -> {
                                            MainForm.setCurClass(finalClassName);
                                            MainForm.getInstance().getCurClassText().setText(finalClassName);
                                        });
                                        OpenClassOptions options = OpenClassOptions.defaults()
                                                .openInNewTab(true)
                                                .forceDecompile(false)
                                                .recordState(true)
                                                .warnOnMissing(true);
                                        if (!openClassInEditor(finalClassName, null, null, options)) {
                                            return;
                                        }
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
                                if (target.kind == SymbolKind.PACKAGE) {
                                    String pkg = target.className;
                                    if (pkg == null || pkg.trim().isEmpty()) {
                                        SyntaxAreaHelper.runOnEdt(() -> JOptionPane.showMessageDialog(
                                                MainForm.getInstance().getMasterPanel(),
                                                "<html><p>unable to resolve package name</p></html>"));
                                        return;
                                    }
                                    List<String> candidates = ExternalClassIndex.getClassesInPackage(pkg);
                                    if (candidates != null && !candidates.isEmpty()) {
                                        String selected = pickClassFromPackage(pkg, candidates);
                                        if (selected != null && !selected.trim().isEmpty()) {
                                            String finalClassName = selected;
                                            SyntaxAreaHelper.runOnEdt(() -> {
                                                MainForm.setCurClass(finalClassName);
                                                MainForm.getInstance().getCurClassText().setText(finalClassName);
                                            });
                                            OpenClassOptions options = OpenClassOptions.defaults()
                                                    .openInNewTab(true)
                                                    .forceDecompile(true)
                                                    .recordState(true)
                                                    .warnOnMissing(true);
                                            if (!openClassInEditor(finalClassName, null, null, options)) {
                                                return;
                                            }
                                            CoreHelper.refreshAllMethods(finalClassName);
                                            SwingUtilities.invokeLater(() -> {
                                                SearchInputListener.getFileTree().searchPathTarget(finalClassName);
                                                MainForm.getInstance().getTabbedPanel().setSelectedIndex(2);
                                            });
                                        }
                                        return;
                                    }
                                    String pkgPath = pkg.replace('.', '/');
                                    SearchInputListener.getFileTree().searchPathTarget(pkgPath);
                                    return;
                                }
                                if (target.kind == SymbolKind.CLASS
                                        || target.kind == SymbolKind.INTERFACE
                                        || target.kind == SymbolKind.ENUM
                                        || target.kind == SymbolKind.ANNOTATION
                                        || target.kind == SymbolKind.RECORD) {
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
                                    OpenClassOptions options = OpenClassOptions.defaults()
                                            .openInNewTab(true)
                                            .forceDecompile(true)
                                            .recordState(true)
                                            .warnOnMissing(true);
                                    if (!openClassInEditor(finalClassName, null, null, options)) {
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
                                if (callerContext != null && callerContext.className != null
                                        && !callerContext.className.trim().isEmpty()) {
                                    className = callerContext.className;
                                }
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
                                        MethodResult picked = selectGlobalMethod(methodNameLocal, target.argCount);
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
                                String scopeHint = resolveScopeHint(callerContext, target);
                                CallSiteSelection callSelection = null;
                                if (methodRef == null) {
                                    callSelection = resolveCallSiteTarget(
                                            callerContext, methodNameLocal, target.argCount, scopeHint, callPosition);
                                }
                                String methodDesc = null;
                                if (callSelection != null) {
                                    className = callSelection.className;
                                    methodNameLocal = callSelection.methodName;
                                    methodDesc = callSelection.methodDesc;
                                }
                                List<ResolvedType> argTypes = null;
                                ResolvedType scopeType = null;
                                boolean preferStatic = false;
                                MethodCallExpr callExpr = null;
                                if (callPosition != null && callPosition.call != null) {
                                    callExpr = callPosition.call;
                                    argTypes = resolveArgumentTypes(cuSnapshot, callExpr.getArguments(), callerContext);
                                    if (callExpr.getScope().isPresent()) {
                                        TypeSolver solver = getTypeSolver();
                                        if (solver != null) {
                                            TypeRef scopeRef = solver.resolveExpressionType(
                                                    cuSnapshot, callExpr.getScope().get(), toCallContext(callerContext));
                                            scopeType = toResolvedType(scopeRef);
                                        }
                                    }
                                    preferStatic = isStaticScope(cuSnapshot, callExpr, callerContext);
                                } else if (methodRef != null) {
                                    argTypes = resolveMethodReferenceParamTypes(cuSnapshot, methodRef, callerContext);
                                    int argCount = argTypes == null ? 0 : argTypes.size();
                                    preferStatic = preferStaticForMethodRef(methodRef, className, argCount);
                                }
                                List<MethodResult> candidates = null;
                                List<MethodResult> candidatesForPick = null;
                                if (methodDesc == null) {
                                    if (callExpr != null) {
                                        String resolved = resolveMethodDescByCall(
                                                className, callExpr, callerContext, scopeType, preferStatic);
                                        if (resolved != null) {
                                            methodDesc = resolved;
                                        }
                                    }
                                    if (methodDesc == null && argTypes != null) {
                                        String resolved = resolveMethodDescByArgTypes(
                                                className, methodNameLocal, argTypes, scopeType, preferStatic);
                                        if (resolved != null) {
                                            methodDesc = resolved;
                                        }
                                    }
                                    candidates = MainForm.getEngine()
                                            .getMethod(className, methodNameLocal, null);
                                    List<MethodResult> filteredCandidates = filterByArgCount(candidates, target.argCount);
                                    candidatesForPick = (filteredCandidates != null && !filteredCandidates.isEmpty())
                                            ? filteredCandidates
                                            : candidates;
                                    if (candidatesForPick != null && !candidatesForPick.isEmpty()) {
                                        final String methodNameForUi = methodNameLocal;
                                        final List<MethodResult> finalCandidatesForPick = candidatesForPick;
                                        methodDesc = callOnEdt(() -> resolveMethodDesc(methodNameForUi, finalCandidatesForPick));
                                        if (methodDesc == null) {
                                            return;
                                        }
                                    }
                                }
                                boolean methodMissing = methodDesc == null;
                                boolean classMissing = MainForm.getEngine().getClassByClass(className) == null;
                                boolean filteredClass = CommonFilterUtil.isFilteredClass(className);
                                if ((methodMissing || classMissing) && methodDesc == null) {
                                    MethodResult picked = selectGlobalMethod(methodNameLocal, target.argCount);
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
                                        filteredClass = CommonFilterUtil.isFilteredClass(className);
                                    }
                                }
                                String finalMethodName = methodNameLocal;
                                String finalMethodDesc = methodDesc;
                                String finalClassName = className;
                                boolean finalClassMissing = classMissing;
                                boolean finalMethodMissing = methodMissing;
                                boolean finalFiltered = filteredClass;
                                OpenClassOptions options = OpenClassOptions.defaults()
                                        .openInNewTab(true)
                                        .forceDecompile(false)
                                        .recordState(true)
                                        .warnOnMissing(true);
                                if (!openClassInEditor(finalClassName, finalMethodName, finalMethodDesc, options)) {
                                    return;
                                }
                                JDialog callDialog = UiExecutor.callOnEdt(() ->
                                        ProcessDialog.createProgressDialog(MainForm.getInstance().getMasterPanel()));
                                if (callDialog != null) {
                                    UiExecutor.runOnEdt(() -> callDialog.setVisible(true));
                                }
                                try {
                                    List<MethodResult> callers = MainForm.getEngine().getCallers(finalClassName, finalMethodName, finalMethodDesc);
                                    List<MethodResult> callees = MainForm.getEngine().getCallee(finalClassName, finalMethodName, finalMethodDesc);
                                    CoreHelper.refreshCallers(finalClassName, finalMethodName, finalMethodDesc);
                                    CoreHelper.refreshCallee(finalClassName, finalMethodName, finalMethodDesc);
                                    SwingUtilities.invokeLater(() ->
                                            MainForm.getInstance().getTabbedPanel().setSelectedIndex(2));
                                    if ((callers == null || callers.isEmpty()) && (callees == null || callees.isEmpty())) {
                                        String hint = buildEmptyCallHint(finalClassName, finalMethodName, finalClassMissing,
                                                finalMethodMissing, finalFiltered);
                                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                                                MainForm.getInstance().getMasterPanel(), hint));
                                    } else if (finalMethodMissing) {
                                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                                                MainForm.getInstance().getMasterPanel(),
                                                "<html><p>no clear target found. try a different symbol.</p></html>"));
                                    }
                                } finally {
                                    NavigationHelper.disposeDialog(callDialog);
                                }
                            } finally {
                                NavigationHelper.disposeDialog(dialog);
                            }
                        });
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

    public static String snapshotText(Document doc) {
        if (doc == null) {
            return null;
        }
        AtomicReference<String> ref = new AtomicReference<>();
        doc.render(() -> {
            try {
                ref.set(doc.getText(0, doc.getLength()));
            } catch (BadLocationException ignored) {
            }
        });
        return ref.get();
    }

    public static ArrayList<Integer> findAllMatches(String text, String key) {
        ArrayList<Integer> results = new ArrayList<>();
        if (text == null || text.isEmpty() || key == null || key.isEmpty()) {
            return results;
        }
        int idx = 0;
        while ((idx = text.indexOf(key, idx)) >= 0) {
            results.add(idx);
            idx += key.length();
        }
        return results;
    }

    public static int applySearchResults(RSyntaxTextArea area, String key, List<Integer> results) {
        ArrayList<Integer> list = results == null ? new ArrayList<>() : new ArrayList<>(results);
        searchResults = list;
        currentIndex = 0;
        lastSearchKey = (key == null || key.isEmpty()) ? null : key;
        lastSearchArea = area;
        if (area == null || lastSearchKey == null || list.isEmpty()) {
            return list.size();
        }
        int pos = list.get(0);
        area.setSelectionStart(pos);
        area.setSelectionEnd(pos + lastSearchKey.length());
        area.setCaretPosition(pos);
        return list.size();
    }

    public static int addSearchAction(String key) {
        RSyntaxTextArea area = getActiveCodeArea();
        if (area == null) {
            applySearchResults(null, null, null);
            return 0;
        }
        if (key == null || key.isEmpty()) {
            applySearchResults(area, null, null);
            return 0;
        }
        String text = snapshotText(area.getDocument());
        if (text == null || text.isEmpty()) {
            applySearchResults(area, key, null);
            return 0;
        }
        ArrayList<Integer> results = findAllMatches(text, key);
        return applySearchResults(area, key, results);
    }

    public static void navigate(String key, boolean next) {
        navigateAsync(key, next, null);
    }

    public static void navigateAsync(String key, boolean next, BiConsumer<Integer, Integer> onMoved) {
        RSyntaxTextArea area = getActiveCodeArea();
        if (area == null || key == null || key.isEmpty()) {
            if (onMoved != null) {
                onMoved.accept(0, 0);
            }
            return;
        }
        if (searchResults == null || searchResults.isEmpty()
                || area != lastSearchArea || !key.equals(lastSearchKey)) {
            rebuildSearchAsync(area, key, next, onMoved);
            return;
        }
        runOnEdt(() -> {
            moveSearchSelection(area, key, next);
            if (onMoved != null) {
                onMoved.accept(currentIndex, searchResults.size());
            }
        });
    }

    private static void rebuildSearchAsync(RSyntaxTextArea area, String key, boolean next,
                                           BiConsumer<Integer, Integer> onMoved) {
        int seq = NAV_SEQ.incrementAndGet();
        UiExecutor.runAsync(() -> {
            String text = snapshotText(area.getDocument());
            ArrayList<Integer> results = findAllMatches(text, key);
            runOnEdt(() -> {
                if (seq != NAV_SEQ.get()) {
                    return;
                }
                if (area != getActiveCodeArea()) {
                    return;
                }
                applySearchResults(area, key, results);
                if (searchResults == null || searchResults.isEmpty()) {
                    if (onMoved != null) {
                        onMoved.accept(0, 0);
                    }
                    return;
                }
                moveSearchSelection(area, key, next);
                if (onMoved != null) {
                    onMoved.accept(currentIndex, searchResults.size());
                }
            });
        });
    }

    private static void moveSearchSelection(RSyntaxTextArea area, String key, boolean next) {
        if (area == null || key == null || key.isEmpty()) {
            return;
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

    private static String getTabClass(Component tab) {
        if (!(tab instanceof JComponent)) {
            return null;
        }
        Object value = ((JComponent) tab).getClientProperty(PROP_CLASS);
        if (value instanceof String) {
            return (String) value;
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

    public static boolean hasLoadedClass(String className) {
        return hasLoadedClass(className, false);
    }

    public static boolean hasLoadedClass(String className, boolean activeOnly) {
        if (className == null || className.trim().isEmpty()) {
            return false;
        }
        if (codeTabs == null) {
            return false;
        }
        String normalized = normalizeClassName(className);
        if (activeOnly) {
            Component selected = codeTabs.getSelectedComponent();
            String tabClass = getTabClass(selected);
            if (tabClass == null) {
                return false;
            }
            if (!normalizeClassName(tabClass).equals(normalized)) {
                return false;
            }
            RSyntaxTextArea area = getAreaFromTab(selected);
            return area != null && looksLikeJava(area.getText());
        }
        Component tab = classTabs.get(normalized);
        if (tab == null) {
            return false;
        }
        RSyntaxTextArea area = getAreaFromTab(tab);
        return area != null && looksLikeJava(area.getText());
    }

    private static boolean isSameClassTab(String normalizedClass, boolean openInNewTab) {
        if (normalizedClass == null || normalizedClass.trim().isEmpty()) {
            return false;
        }
        if (codeTabs == null) {
            return false;
        }
        if (openInNewTab) {
            return classTabs.containsKey(normalizedClass);
        }
        Component selected = codeTabs.getSelectedComponent();
        String tabClass = getTabClass(selected);
        if (tabClass == null || tabClass.trim().isEmpty()) {
            return false;
        }
        return normalizeClassName(tabClass).equals(normalizedClass);
    }

    public static boolean openClassInEditor(String className,
                                            String methodName,
                                            String methodDesc,
                                            OpenClassOptions options) {
        return openClassInEditor(className, methodName, methodDesc, options, null);
    }

    public static boolean openClassInEditor(String className,
                                            String methodName,
                                            String methodDesc,
                                            OpenClassOptions options,
                                            Integer jarId) {
        OpenClassOptions resolved = options == null ? OpenClassOptions.defaults() : options;
        Integer preferJarId = normalizeJarId(jarId);
        return openClassInEditorInternal(className, methodName, methodDesc, preferJarId,
                resolved.forceDecompile, resolved.openInNewTab, resolved.recordState, resolved.warnOnMissing);
    }

    public static final class OpenClassOptions {
        private boolean openInNewTab;
        private boolean forceDecompile;
        private boolean recordState;
        private boolean warnOnMissing = true;

        private OpenClassOptions() {
        }

        public static OpenClassOptions defaults() {
            return new OpenClassOptions();
        }

        public OpenClassOptions openInNewTab(boolean value) {
            this.openInNewTab = value;
            return this;
        }

        public OpenClassOptions forceDecompile(boolean value) {
            this.forceDecompile = value;
            return this;
        }

        public OpenClassOptions preferExisting(boolean value) {
            this.forceDecompile = !value;
            return this;
        }

        public OpenClassOptions recordState(boolean value) {
            this.recordState = value;
            return this;
        }

        public OpenClassOptions warnOnMissing(boolean value) {
            this.warnOnMissing = value;
            return this;
        }
    }

    private static boolean openClassInEditorInternal(String className,
                                                     String methodName,
                                                     String methodDesc,
                                                     Integer jarId,
                                                     boolean forceDecompile,
                                                     boolean openInNewTab,
                                                     boolean recordState,
                                                     boolean warnOnMissing) {
        if (className == null || className.trim().isEmpty()) {
            return false;
        }
        String normalized = normalizeClassName(className);
        String classPath = resolveClassPath(normalized, jarId);
        if (classPath == null || classPath.trim().isEmpty()
                || !Files.exists(Paths.get(classPath))) {
            if (warnOnMissing) {
                UiExecutor.showMessage(MainForm.getInstance().getMasterPanel(),
                        "<html><p>need dependency or class file not found</p></html>");
            }
            return false;
        }
        boolean sameClassTab = isSameClassTab(normalized, openInNewTab);
        RSyntaxTextArea area = ensureTabForClass(normalized, openInNewTab);
        if (area == null) {
            return false;
        }
        String code = area.getText();
        boolean useCfr = DecompileSelector.shouldUseCfr();
        String selectedDecompiler = useCfr ? DECOMPILER_CFR : DECOMPILER_FERN;
        String codeDecompiler = detectDecompilerFromCode(code);
        boolean sameDecompiler = codeDecompiler == null || codeDecompiler.equals(selectedDecompiler);
        boolean reuseExisting = !forceDecompile && sameClassTab && looksLikeJava(code) && sameDecompiler;
        List<SimpleLineMapping> mappings = null;
        String decompiler = null;
        JDialog dialog = null;
        if (!reuseExisting) {
            dialog = UiExecutor.callOnEdt(() ->
                    ProcessDialog.createDelayedProgressDialog(MainForm.getInstance().getMasterPanel(), 200));
            try {
                boolean mappingReady = loadLineMappingsFromDb(normalized, jarId, selectedDecompiler);
                if (mappingReady) {
                    DecompileType type = useCfr ? DecompileType.CFR : DecompileType.FERNFLOWER;
                    code = DecompileDispatcher.decompile(Paths.get(classPath), type);
                    decompiler = selectedDecompiler;
                } else if (useCfr) {
                    CFRDecompileEngine.CfrDecompileResult result =
                            CFRDecompileEngine.decompileWithLineMapping(classPath);
                    if (result != null) {
                        code = result.getCode();
                        mappings = toSimpleLineMappingsFromCfr(result.getLineMappings());
                        decompiler = DECOMPILER_CFR;
                    } else {
                        code = DecompileDispatcher.decompile(Paths.get(classPath), DecompileType.CFR);
                        decompiler = DECOMPILER_CFR;
                    }
                } else {
                    DecompileEngine.FernDecompileResult result =
                            DecompileEngine.decompileWithLineMapping(Paths.get(classPath));
                    if (result != null) {
                        code = result.getCode();
                        mappings = toSimpleLineMappingsFromFern(result.getLineMappings());
                        decompiler = DECOMPILER_FERN;
                    } else {
                        code = DecompileDispatcher.decompile(Paths.get(classPath), DecompileType.FERNFLOWER);
                        decompiler = DECOMPILER_FERN;
                    }
                }
            } finally {
                if (dialog != null) {
                    UiExecutor.runOnEdt(dialog::dispose);
                }
            }
        }
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        String finalCode = code;
        runOnEdt(() -> {
            area.setText(finalCode);
            area.setCaretPosition(0);
        });
        if (mappings != null && !mappings.isEmpty() && decompiler != null) {
            cacheLineMappings(normalized, mappings);
            saveLineMappingsToDb(normalized, jarId, decompiler, mappings);
        } else {
            ensureLineMappingsLoaded(normalized, jarId, code);
        }
        String jarName = resolveJarName(normalized, jarId);
        if (methodName != null && !methodName.trim().isEmpty()
                && methodDesc != null && !methodDesc.trim().isEmpty()) {
            if (recordState) {
                pushStateIfNeeded(normalized, methodName, methodDesc, classPath);
            } else {
                setCurMethodOnly(normalized, methodName, methodDesc, classPath, jarName);
            }
            jumpToMethodIfPossible(area, code, normalized, methodName, methodDesc);
        } else {
            MainForm.setCurMethod(null);
            if (recordState) {
                pushClassStateIfNeeded(normalized, classPath, jarName);
            }
        }
        runOnEdt(() -> {
            MainForm.setCurClass(normalized);
            MainForm.getInstance().getCurClassText().setText(normalized);
            MainForm.getInstance().getCurJarText().setText(jarName == null ? "" : jarName);
            if (methodName != null && !methodName.trim().isEmpty()) {
                MainForm.getInstance().getCurMethodText().setText(methodName);
            } else {
                MainForm.getInstance().getCurMethodText().setText("");
            }
        });
        return true;
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
        SemanticResolver resolver = getSemanticResolver();
        if (resolver == null) {
            return new SemanticTarget(SymbolKind.METHOD, fallbackWord, null);
        }
        SymbolRef ref = resolver.resolveSemanticTarget(cu, pos, fallbackWord);
        SemanticTarget target = toSemanticTarget(ref);
        if (target == null || target.kind == SymbolKind.UNKNOWN) {
            return new SemanticTarget(SymbolKind.METHOD, fallbackWord, null);
        }
        return target;
    }

    private static SemanticTarget toSemanticTarget(SymbolRef ref) {
        if (ref == null) {
            return null;
        }
        SymbolKind kind = toLegacyKind(ref.getKind());
        TypeRef typeRef = ref.getTypeRef();
        String typeName = typeRef == null ? null : typeRef.internalName;
        return new SemanticTarget(
                kind,
                ref.getName(),
                ref.getOwner(),
                ref.getDeclarationRange(),
                typeName,
                ref.getArgCount());
    }

    private static SymbolRef toSymbolRef(SemanticTarget target) {
        if (target == null) {
            return null;
        }
        me.n1ar4.jar.analyzer.semantic.SymbolKind kind = toSemanticKind(target.kind);
        TypeRef typeRef = null;
        if (target.typeName != null && !target.typeName.trim().isEmpty()) {
            typeRef = TypeRef.fromInternalName(target.typeName);
        }
        return new SymbolRef(
                kind,
                target.name,
                target.className,
                target.declarationRange,
                typeRef,
                target.argCount);
    }

    private static SymbolKind toLegacyKind(me.n1ar4.jar.analyzer.semantic.SymbolKind kind) {
        if (kind == null) {
            return SymbolKind.UNKNOWN;
        }
        try {
            return SymbolKind.valueOf(kind.name());
        } catch (IllegalArgumentException ex) {
            return SymbolKind.UNKNOWN;
        }
    }

    private static me.n1ar4.jar.analyzer.semantic.SymbolKind toSemanticKind(SymbolKind kind) {
        if (kind == null) {
            return me.n1ar4.jar.analyzer.semantic.SymbolKind.UNKNOWN;
        }
        try {
            return me.n1ar4.jar.analyzer.semantic.SymbolKind.valueOf(kind.name());
        } catch (IllegalArgumentException ex) {
            return me.n1ar4.jar.analyzer.semantic.SymbolKind.UNKNOWN;
        }
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

    private static String resolveScopeClassName(CompilationUnit cu, MethodCallExpr call) {
        return resolveScopeClassName(cu, call, null);
    }

    private static String resolveScopeClassName(CompilationUnit cu, MethodCallExpr call, CallerContext ctx) {
        CallResolver resolver = getCallResolver();
        if (resolver == null) {
            return null;
        }
        return resolver.resolveScopeClassName(cu, call, toCallContext(ctx));
    }

    private static boolean isStaticScope(CompilationUnit cu, MethodCallExpr call, CallerContext ctx) {
        CallResolver resolver = getCallResolver();
        if (resolver == null) {
            return false;
        }
        return resolver.isStaticScope(cu, call, toCallContext(ctx));
    }

    private static boolean preferStaticForMethodRef(MethodReferenceExpr ref,
                                                    String ownerClass,
                                                    int argCount) {
        CallResolver resolver = getCallResolver();
        if (resolver == null) {
            return false;
        }
        return resolver.preferStaticForMethodRef(ref, ownerClass, argCount);
    }

    private static int resolveMethodReferenceArgCount(CompilationUnit cu,
                                                      MethodReferenceExpr ref,
                                                      CallerContext ctx) {
        TypeSolver solver = getTypeSolver();
        if (solver == null) {
            return ARG_COUNT_UNKNOWN;
        }
        return solver.resolveMethodReferenceArgCount(cu, ref, toCallContext(ctx));
    }

    public static String resolveClassPath(String className) {
        return resolveClassPath(className, null);
    }

    public static String resolveClassPath(String className, Integer jarId) {
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeClassName(className);
        CoreEngine engine = MainForm.getEngine();
        if (engine != null) {
            Integer preferJarId = normalizeJarId(jarId);
            String path = engine.getAbsPath(normalized, preferJarId);
            if (path != null && Files.exists(Paths.get(path))) {
                return path;
            }
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

    private static String resolveJarName(String className) {
        return resolveJarName(className, null);
    }

    private static String resolveJarName(String className, Integer jarId) {
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeClassName(className);
        CoreEngine engine = MainForm.getEngine();
        if (engine != null) {
            Integer preferJarId = normalizeJarId(jarId);
            if (preferJarId != null) {
                String jar = engine.getJarNameById(preferJarId);
                if (jar != null && !jar.trim().isEmpty()) {
                    return jar;
                }
            }
            String jar = engine.getJarByClass(normalized);
            if (jar != null && !jar.trim().isEmpty()) {
                return jar;
            }
        }
        return RuntimeClassResolver.getJarName(normalized);
    }

    private static String resolveTempClassPath(String className) {
        Path resolved = JarUtil.resolveClassFileInTemp(className);
        return resolved == null ? null : resolved.toString();
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

    private static void ensureLineMappingsLoaded(String className,
                                                 Integer jarId,
                                                 String codeSnapshot) {
        if (className == null || className.trim().isEmpty()) {
            return;
        }
        String normalized = normalizeClassName(className);
        if (LINE_MAPPINGS.containsKey(normalized)) {
            return;
        }
        String decompiler = detectDecompilerFromCode(codeSnapshot);
        if (decompiler == null) {
            decompiler = DecompileSelector.shouldUseCfr() ? DECOMPILER_CFR : DECOMPILER_FERN;
        }
        loadLineMappingsFromDb(normalized, jarId, decompiler);
    }

    private static String detectDecompilerFromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        if (code.startsWith(CFRDecompileEngine.getCFR_PREFIX())) {
            return DECOMPILER_CFR;
        }
        if (code.startsWith(DecompileEngine.getFERN_PREFIX())) {
            return DECOMPILER_FERN;
        }
        return null;
    }

    private static boolean loadLineMappingsFromDb(String className,
                                                  Integer jarId,
                                                  String decompiler) {
        if (className == null || className.trim().isEmpty()) {
            return false;
        }
        if (MainForm.getEngine() == null) {
            return false;
        }
        Integer preferJarId = normalizeJarId(jarId);
        if (preferJarId == null) {
            preferJarId = resolveJarIdForClass(className);
        }
        List<LineMappingEntity> rows = MainForm.getEngine().getLineMappings(className, preferJarId, decompiler);
        if ((rows == null || rows.isEmpty()) && preferJarId != null) {
            rows = MainForm.getEngine().getLineMappings(className, null, decompiler);
        }
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        List<SimpleLineMapping> mappings = new ArrayList<>();
        for (LineMappingEntity row : rows) {
            if (row == null) {
                continue;
            }
            NavigableMap<Integer, Integer> parsed = parseLineMapping(row.getMappingText());
            if (parsed == null || parsed.isEmpty()) {
                continue;
            }
            String methodName = row.getMethodName();
            String methodDesc = row.getMethodDesc();
            if (methodName != null && methodName.trim().isEmpty()) {
                methodName = null;
            }
            if (methodDesc != null && methodDesc.trim().isEmpty()) {
                methodDesc = null;
            }
            mappings.add(new SimpleLineMapping(methodName, methodDesc, parsed));
        }
        if (mappings.isEmpty()) {
            return false;
        }
        cacheLineMappings(className, mappings);
        return true;
    }

    private static void saveLineMappingsToDb(String className,
                                             Integer jarId,
                                             String decompiler,
                                             List<SimpleLineMapping> mappings) {
        if (className == null || className.trim().isEmpty()) {
            return;
        }
        if (MainForm.getEngine() == null) {
            return;
        }
        if (mappings == null || mappings.isEmpty()) {
            return;
        }
        Integer resolvedJarId = normalizeJarId(jarId);
        if (resolvedJarId == null) {
            resolvedJarId = resolveJarIdForClass(className);
        }
        Integer jarIdValue = resolvedJarId == null ? -1 : resolvedJarId;
        List<LineMappingEntity> entities = new ArrayList<>();
        for (SimpleLineMapping mapping : mappings) {
            if (mapping == null || mapping.decompiledToSource == null
                    || mapping.decompiledToSource.isEmpty()) {
                continue;
            }
            String text = serializeLineMapping(mapping.decompiledToSource);
            if (text == null || text.isEmpty()) {
                continue;
            }
            LineMappingEntity entity = new LineMappingEntity();
            entity.setClassName(normalizeClassName(className));
            entity.setMethodName(mapping.methodName);
            entity.setMethodDesc(mapping.methodDesc);
            entity.setJarId(jarIdValue);
            entity.setDecompiler(decompiler);
            entity.setMappingText(text);
            entities.add(entity);
        }
        if (entities.isEmpty()) {
            return;
        }
        MainForm.getEngine().saveLineMappings(normalizeClassName(className), jarIdValue, decompiler, entities);
    }

    private static Integer resolveJarIdForClass(String className) {
        if (MainForm.getEngine() == null) {
            return null;
        }
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        return MainForm.getEngine().getJarIdByClass(normalizeClassName(className));
    }

    private static Integer normalizeJarId(Integer jarId) {
        if (jarId == null || jarId <= 0) {
            return null;
        }
        return jarId;
    }

    private static TypeSolver getTypeSolver() {
        CoreEngine engine = MainForm.getEngine();
        if (engine == null) {
            TYPE_SOLVER = null;
            SEMANTIC_RESOLVER = null;
            SEMANTIC_ENGINE = null;
            return null;
        }
        TypeSolver solver = TYPE_SOLVER;
        if (solver == null || SEMANTIC_ENGINE != engine) {
            solver = new TypeSolver(engine);
            solver.setLineMapper(SyntaxAreaHelper::mapLineNumber);
            TYPE_SOLVER = solver;
            SEMANTIC_RESOLVER = new SemanticResolver(engine, solver);
            SEMANTIC_ENGINE = engine;
        }
        return solver;
    }

    private static CallResolver getCallResolver() {
        TypeSolver solver = getTypeSolver();
        return solver == null ? null : solver.getCallResolver();
    }

    private static SemanticResolver getSemanticResolver() {
        getTypeSolver();
        return SEMANTIC_RESOLVER;
    }

    private static String serializeLineMapping(NavigableMap<Integer, Integer> mapping) {
        if (mapping == null || mapping.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : mapping.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(entry.getKey()).append(':').append(entry.getValue());
        }
        return sb.toString();
    }

    private static NavigableMap<Integer, Integer> parseLineMapping(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        NavigableMap<Integer, Integer> out = new TreeMap<>();
        String[] pairs = text.split(",");
        for (String pair : pairs) {
            if (pair == null || pair.trim().isEmpty()) {
                continue;
            }
            int idx = pair.indexOf(':');
            if (idx <= 0 || idx >= pair.length() - 1) {
                continue;
            }
            try {
                int dec = Integer.parseInt(pair.substring(0, idx).trim());
                int src = Integer.parseInt(pair.substring(idx + 1).trim());
                if (dec > 0 && src > 0) {
                    out.put(dec, src);
                }
            } catch (Exception ignored) {
            }
        }
        return out.isEmpty() ? null : out;
    }

    private static void cacheLineMappings(String className, List<SimpleLineMapping> mappings) {
        if (className == null || className.trim().isEmpty()) {
            return;
        }
        ClassLineMapping built = buildClassLineMapping(mappings);
        String normalized = normalizeClassName(className);
        if (built == null) {
            LINE_MAPPINGS.remove(normalized);
            return;
        }
        LINE_MAPPINGS.put(normalized, built);
    }

    private static void clearLineMappings(String className) {
        if (className == null || className.trim().isEmpty()) {
            return;
        }
        LINE_MAPPINGS.remove(normalizeClassName(className));
    }

    private static List<SimpleLineMapping> toSimpleLineMappingsFromCfr(List<CFRDecompileEngine.CfrLineMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return null;
        }
        List<SimpleLineMapping> out = new ArrayList<>();
        for (CFRDecompileEngine.CfrLineMapping mapping : mappings) {
            if (mapping == null || mapping.getDecompiledToSource() == null
                    || mapping.getDecompiledToSource().isEmpty()) {
                continue;
            }
            out.add(new SimpleLineMapping(mapping.getMethodName(),
                    mapping.getMethodDesc(), mapping.getDecompiledToSource()));
        }
        return out.isEmpty() ? null : out;
    }

    private static List<SimpleLineMapping> toSimpleLineMappingsFromFern(List<DecompileEngine.FernLineMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return null;
        }
        List<SimpleLineMapping> out = new ArrayList<>();
        for (DecompileEngine.FernLineMapping mapping : mappings) {
            if (mapping == null || mapping.getDecompiledToSource() == null
                    || mapping.getDecompiledToSource().isEmpty()) {
                continue;
            }
            out.add(new SimpleLineMapping(mapping.getMethodName(),
                    mapping.getMethodDesc(), mapping.getDecompiledToSource()));
        }
        return out.isEmpty() ? null : out;
    }

    private static ClassLineMapping buildClassLineMapping(List<SimpleLineMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return null;
        }
        List<MethodLineMapping> methodMappings = new ArrayList<>();
        Map<String, List<MethodLineMapping>> byName = new HashMap<>();
        for (SimpleLineMapping mapping : mappings) {
            if (mapping == null) {
                continue;
            }
            NavigableMap<Integer, Integer> decompiledToSource = mapping.decompiledToSource;
            if (decompiledToSource == null || decompiledToSource.isEmpty()) {
                continue;
            }
            int minLine = decompiledToSource.firstKey();
            int maxLine = decompiledToSource.lastKey();
            int argCount = argCountFromDesc(mapping.methodDesc);
            MethodLineMapping methodMapping = new MethodLineMapping(
                    mapping.methodName,
                    mapping.methodDesc,
                    argCount,
                    decompiledToSource,
                    minLine,
                    maxLine);
            methodMappings.add(methodMapping);
            if (mapping.methodName != null) {
                byName.computeIfAbsent(mapping.methodName, k -> new ArrayList<>()).add(methodMapping);
            }
        }
        if (methodMappings.isEmpty()) {
            return null;
        }
        return new ClassLineMapping(methodMappings, byName);
    }

    private static int mapLineNumber(String className,
                                     String methodName,
                                     String methodDesc,
                                     int argCount,
                                     int decompiledLine) {
        if (decompiledLine <= 0) {
            return decompiledLine;
        }
        if (className == null || className.trim().isEmpty()) {
            return decompiledLine;
        }
        ClassLineMapping mapping = LINE_MAPPINGS.get(normalizeClassName(className));
        if (mapping == null) {
            return decompiledLine;
        }
        MethodLineMapping selected = selectLineMapping(mapping, methodName, methodDesc, argCount, decompiledLine);
        if (selected == null || selected.decompiledToSource == null || selected.decompiledToSource.isEmpty()) {
            return decompiledLine;
        }
        Integer exact = selected.decompiledToSource.get(decompiledLine);
        if (exact != null) {
            return exact;
        }
        Map.Entry<Integer, Integer> floor = selected.decompiledToSource.floorEntry(decompiledLine);
        Map.Entry<Integer, Integer> ceil = selected.decompiledToSource.ceilingEntry(decompiledLine);
        if (floor == null && ceil == null) {
            return decompiledLine;
        }
        if (floor == null) {
            return ceil.getValue();
        }
        if (ceil == null) {
            return floor.getValue();
        }
        int diffFloor = Math.abs(decompiledLine - floor.getKey());
        int diffCeil = Math.abs(ceil.getKey() - decompiledLine);
        return diffCeil < diffFloor ? ceil.getValue() : floor.getValue();
    }

    private static MethodLineMapping selectLineMapping(ClassLineMapping mapping,
                                                       String methodName,
                                                       String methodDesc,
                                                       int argCount,
                                                       int line) {
        if (mapping == null) {
            return null;
        }
        List<MethodLineMapping> candidates;
        if (methodName != null && !methodName.trim().isEmpty()) {
            candidates = mapping.byName.get(methodName);
        } else {
            candidates = null;
        }
        if (candidates == null || candidates.isEmpty()) {
            candidates = mapping.mappings;
        }
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (methodDesc != null && !methodDesc.trim().isEmpty()) {
            for (MethodLineMapping candidate : candidates) {
                if (methodDesc.equals(candidate.methodDesc)) {
                    return candidate;
                }
            }
        }
        if (argCount != ARG_COUNT_UNKNOWN) {
            for (MethodLineMapping candidate : candidates) {
                if (candidate.argCount == argCount) {
                    return candidate;
                }
            }
        }
        MethodLineMapping best = null;
        long bestSpan = Long.MAX_VALUE;
        int bestDistance = Integer.MAX_VALUE;
        boolean hasContaining = false;
        if (line > 0) {
            for (MethodLineMapping candidate : candidates) {
                if (line >= candidate.minLine && line <= candidate.maxLine) {
                    long span = (long) candidate.maxLine - candidate.minLine;
                    if (!hasContaining || span < bestSpan) {
                        bestSpan = span;
                        best = candidate;
                    }
                    hasContaining = true;
                } else if (!hasContaining) {
                    int dist = Math.min(Math.abs(line - candidate.minLine), Math.abs(line - candidate.maxLine));
                    if (dist < bestDistance) {
                        bestDistance = dist;
                        best = candidate;
                    }
                }
            }
        }
        if (best == null) {
            for (MethodLineMapping candidate : candidates) {
                long span = (long) candidate.maxLine - candidate.minLine;
                if (span < bestSpan) {
                    bestSpan = span;
                    best = candidate;
                }
            }
        }
        if (best != null) {
            return best;
        }
        return candidates.get(0);
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

    private static void jumpToMethodIfPossible(RSyntaxTextArea area,
                                               String code,
                                               String className,
                                               String methodName,
                                               String methodDesc) {
        if (area == null || code == null || methodName == null || methodName.trim().isEmpty()) {
            return;
        }
        int argCount = argCountFromDesc(methodDesc);
        CompilationUnit cu = parseCompilationUnit(code);
        if (cu != null) {
            Range range = resolveMethodDeclarationRange(cu, className, methodName, argCount);
            if (range != null && jumpToRange(area, code, range)) {
                return;
            }
        }
        String target = methodName;
        if ("<init>".equals(methodName)) {
            String shortName = className == null ? null : className;
            if (shortName != null && shortName.contains("/")) {
                shortName = shortName.substring(shortName.lastIndexOf('/') + 1);
            }
            if (shortName != null && !shortName.trim().isEmpty()) {
                target = shortName;
            }
        } else if ("<clinit>".equals(methodName)) {
            int staticIdx = code.indexOf("static {");
            if (staticIdx < 0) {
                staticIdx = code.indexOf("static{");
            }
            if (staticIdx >= 0) {
                int finalIdx = staticIdx;
                runOnEdt(() -> {
                    area.setCaretPosition(finalIdx);
                    area.requestFocusInWindow();
                });
            }
            return;
        }
        int idx = code.indexOf(target + "(");
        if (idx < 0) {
            idx = code.indexOf(target + " ");
        }
        if (idx < 0) {
            return;
        }
        int finalIdx = idx;
        runOnEdt(() -> {
            area.setCaretPosition(finalIdx);
            area.requestFocusInWindow();
        });
    }

    private static boolean jumpToRange(RSyntaxTextArea area, String code, Range range) {
        if (area == null || code == null || range == null) {
            return false;
        }
        int start = positionToOffset(code, range.begin);
        int end = positionToOffset(code, range.end);
        if (start < 0) {
            return false;
        }
        if (end < start) {
            end = Math.min(code.length(), start + 1);
        }
        int finalStart = start;
        int finalEnd = end;
        runOnEdt(() -> {
            area.setCaretPosition(finalStart);
            Highlighter highlighter = area.getHighlighter();
            highlighter.removeAllHighlights();
            try {
                highlighter.addHighlight(finalStart, finalEnd,
                        new DefaultHighlighter.DefaultHighlightPainter(Color.ORANGE));
            } catch (BadLocationException ignored) {
            }
            area.requestFocusInWindow();
        });
        return true;
    }

    private static Range resolveMethodDeclarationRange(CompilationUnit cu,
                                                       String className,
                                                       String methodName,
                                                       int argCount) {
        if (cu == null || methodName == null || methodName.trim().isEmpty()) {
            return null;
        }
        String trimmed = methodName.trim();
        TypeDeclaration<?> targetType = resolveTargetType(cu, className);
        if ("<clinit>".equals(trimmed)) {
            return resolveStaticInitializerRange(cu, targetType);
        }
        if ("<init>".equals(trimmed)) {
            List<ConstructorDeclaration> ctors = targetType == null
                    ? cu.findAll(ConstructorDeclaration.class)
                    : targetType.findAll(ConstructorDeclaration.class);
            List<ConstructorDeclaration> matched = new ArrayList<>();
            String shortName = extractShortClassName(className);
            String innerName = shortName;
            if (innerName != null && innerName.contains("$")) {
                innerName = innerName.substring(innerName.lastIndexOf('$') + 1);
            }
            for (ConstructorDeclaration ctor : ctors) {
                if (ctor == null) {
                    continue;
                }
                String ctorName = ctor.getNameAsString();
                if (nameMatches(ctorName, shortName, innerName)) {
                    matched.add(ctor);
                }
            }
            Range matchedRange = selectRangeByArgCount(matched, argCount);
            if (matchedRange != null) {
                return matchedRange;
            }
            return selectRangeByArgCount(ctors, argCount);
        }
        List<MethodDeclaration> methods = targetType == null
                ? cu.findAll(MethodDeclaration.class)
                : targetType.findAll(MethodDeclaration.class);
        List<MethodDeclaration> matched = new ArrayList<>();
        for (MethodDeclaration method : methods) {
            if (method == null) {
                continue;
            }
            if (trimmed.equals(method.getNameAsString())) {
                matched.add(method);
            }
        }
        return selectRangeByArgCount(matched, argCount);
    }

    private static Range resolveStaticInitializerRange(CompilationUnit cu, TypeDeclaration<?> targetType) {
        List<InitializerDeclaration> inits = targetType == null
                ? cu.findAll(InitializerDeclaration.class)
                : targetType.findAll(InitializerDeclaration.class);
        List<Node> candidates = new ArrayList<>();
        for (InitializerDeclaration init : inits) {
            if (init != null && init.isStatic()) {
                candidates.add(init);
            }
        }
        return selectBestRange(candidates);
    }

    private static Range selectRangeByArgCount(List<? extends Node> nodes, int argCount) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        if (argCount != ARG_COUNT_UNKNOWN) {
            List<Node> filtered = new ArrayList<>();
            for (Node node : nodes) {
                if (node instanceof MethodDeclaration) {
                    MethodDeclaration md = (MethodDeclaration) node;
                    if (md.getParameters().size() == argCount) {
                        filtered.add(md);
                    }
                } else if (node instanceof ConstructorDeclaration) {
                    ConstructorDeclaration cd = (ConstructorDeclaration) node;
                    if (cd.getParameters().size() == argCount) {
                        filtered.add(cd);
                    }
                } else {
                    filtered.add(node);
                }
            }
            Range matched = selectBestRange(filtered);
            if (matched != null) {
                return matched;
            }
        }
        return selectBestRange(nodes);
    }

    private static Range selectBestRange(List<? extends Node> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        Range best = null;
        int bestLine = Integer.MAX_VALUE;
        for (Node node : nodes) {
            if (node == null) {
                continue;
            }
            Range range = node.getRange().orElse(null);
            if (range == null) {
                continue;
            }
            int line = range.begin.line;
            if (line < bestLine) {
                bestLine = line;
                best = range;
            }
        }
        return best;
    }

    private static TypeDeclaration<?> resolveTargetType(CompilationUnit cu, String className) {
        if (cu == null) {
            return null;
        }
        String shortName = extractShortClassName(className);
        String innerName = shortName;
        if (innerName != null && innerName.contains("$")) {
            innerName = innerName.substring(innerName.lastIndexOf('$') + 1);
        }
        TypeDeclaration<?> primary = cu.getPrimaryType().orElse(null);
        if (primary != null && nameMatches(primary.getNameAsString(), shortName, innerName)) {
            return primary;
        }
        List<TypeDeclaration> types = cu.findAll(TypeDeclaration.class);
        for (TypeDeclaration type : types) {
            if (type == null) {
                continue;
            }
            if (nameMatches(type.getNameAsString(), shortName, innerName)) {
                return type;
            }
        }
        return primary;
    }

    private static boolean nameMatches(String name, String candidate, String fallback) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        if (candidate != null && name.equals(candidate)) {
            return true;
        }
        return fallback != null && name.equals(fallback);
    }

    private static String extractShortClassName(String className) {
        if (className == null) {
            return null;
        }
        String name = className.trim();
        if (name.isEmpty()) {
            return null;
        }
        if (name.contains("/")) {
            name = name.substring(name.lastIndexOf('/') + 1);
        } else if (name.contains(".")) {
            name = name.substring(name.lastIndexOf('.') + 1);
        }
        return name;
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

    private static int argCountFromDesc(String desc) {
        if (desc == null || desc.trim().isEmpty()) {
            return ARG_COUNT_UNKNOWN;
        }
        try {
            return Type.getArgumentTypes(desc).length;
        } catch (Exception ignored) {
            return ARG_COUNT_UNKNOWN;
        }
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
        String jarName = resolveJarName(className);
        if (jarName != null && !jarName.trim().isEmpty()) {
            res.setJarName(jarName);
        }
        MainForm.setCurMethod(res);

        State newState = buildState(className, methodName, methodDesc, classPath, jarName);
        pushState(newState);
    }

    private static void pushClassStateIfNeeded(String className, String classPath, String jarName) {
        if (className == null || className.trim().isEmpty()) {
            return;
        }
        if (classPath == null || classPath.trim().isEmpty()) {
            return;
        }
        String resolvedJar = jarName;
        if (resolvedJar == null || resolvedJar.trim().isEmpty()) {
            resolvedJar = resolveJarName(className);
        }
        State newState = buildState(className, null, null, classPath, resolvedJar);
        pushState(newState);
    }

    private static State buildState(String className,
                                    String methodName,
                                    String methodDesc,
                                    String classPath,
                                    String jarName) {
        State newState = new State();
        if (classPath != null && !classPath.trim().isEmpty()) {
            newState.setClassPath(Paths.get(classPath));
        }
        newState.setJarName(jarName);
        newState.setClassName(className);
        newState.setMethodDesc(methodDesc);
        newState.setMethodName(methodName);
        return newState;
    }

    private static void pushState(State newState) {
        if (newState == null) {
            return;
        }
        int curSI = MainForm.getCurStateIndex();
        if (curSI < -1) {
            curSI = -1;
        }
        int size = MainForm.getStateList().size();
        if (curSI >= size) {
            curSI = size - 1;
        }
        if (curSI >= 0 && curSI < size) {
            State current = MainForm.getStateList().get(curSI);
            if (stateEquals(current, newState)) {
                return;
            }
        }
        while (MainForm.getStateList().size() > curSI + 1) {
            MainForm.getStateList().removeLast();
        }
        MainForm.getStateList().add(curSI + 1, newState);
        MainForm.setCurStateIndex(curSI + 1);
    }

    private static boolean stateEquals(State left, State right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (!Objects.equals(left.getClassName(), right.getClassName())) {
            return false;
        }
        if (!Objects.equals(left.getMethodName(), right.getMethodName())) {
            return false;
        }
        if (!Objects.equals(left.getMethodDesc(), right.getMethodDesc())) {
            return false;
        }
        if (!Objects.equals(left.getJarName(), right.getJarName())) {
            return false;
        }
        if (left.getClassPath() == null && right.getClassPath() == null) {
            return true;
        }
        if (left.getClassPath() == null || right.getClassPath() == null) {
            return false;
        }
        return left.getClassPath().toAbsolutePath().normalize()
                .equals(right.getClassPath().toAbsolutePath().normalize());
    }

    private static void setCurMethodOnly(String className,
                                         String methodName,
                                         String methodDesc,
                                         String classPath,
                                         String jarName) {
        if (className == null || className.trim().isEmpty()) {
            return;
        }
        if (methodName == null || methodName.trim().isEmpty()) {
            return;
        }
        if (methodDesc == null || methodDesc.trim().isEmpty()) {
            return;
        }
        MethodResult res = new MethodResult();
        res.setClassName(className);
        res.setMethodName(methodName);
        res.setMethodDesc(methodDesc);
        if (classPath != null && !classPath.trim().isEmpty()) {
            res.setClassPath(Paths.get(classPath));
        }
        if (jarName != null && !jarName.trim().isEmpty()) {
            res.setJarName(jarName);
        }
        MainForm.setCurMethod(res);
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

    private static CallerContext resolveCallerContext(CompilationUnit cu,
                                                      Position pos,
                                                      String curClassSnapshot,
                                                      String codeSnapshot) {
        String className = null;
        String methodName = null;
        String methodDesc = null;
        int methodArgCount = ARG_COUNT_UNKNOWN;
        CallResolver resolver = getCallResolver();
        EnclosingCallable enclosing = resolver == null ? null : resolver.resolveEnclosingCallable(cu, pos);
        if (enclosing != null) {
            className = enclosing.className;
            methodName = enclosing.methodName;
            methodArgCount = enclosing.argCount;
        }
        if (className == null || className.trim().isEmpty()) {
            className = curClassSnapshot;
        }
        if ((className == null || className.trim().isEmpty()) && codeSnapshot != null) {
            className = inferClassName(codeSnapshot);
        }
        if (className != null && !className.trim().isEmpty()) {
            className = normalizeClassName(className);
        }
        MethodResult curMethod = MainForm.getCurMethod();
        if (curMethod != null && className != null
                && className.equals(curMethod.getClassName())) {
            if (methodName == null || methodName.trim().isEmpty()) {
                methodName = curMethod.getMethodName();
            }
            if (methodDesc == null || methodDesc.trim().isEmpty()) {
                methodDesc = curMethod.getMethodDesc();
                if (methodArgCount == ARG_COUNT_UNKNOWN && methodDesc != null) {
                    methodArgCount = argCountFromDesc(methodDesc);
                }
            }
        }
        if ((methodDesc == null || methodDesc.trim().isEmpty())
                && className != null && methodName != null) {
            methodDesc = resolveMethodDescByArgCount(className, methodName, methodArgCount);
        }
        if (className != null && !className.trim().isEmpty()) {
            Integer preferJarId = curMethod == null ? null : normalizeJarId(curMethod.getJarId());
            ensureLineMappingsLoaded(className, preferJarId, codeSnapshot);
        }
        int line = pos == null ? -1 : pos.line;
        int mappedLine = mapLineNumber(className, methodName, methodDesc, methodArgCount, line);
        return new CallerContext(cu, pos, mappedLine, className, methodName, methodDesc, methodArgCount);
    }

    private static CallContext toCallContext(CallerContext ctx) {
        if (ctx == null) {
            return null;
        }
        return new CallContext(ctx.cu, ctx.position, ctx.line,
                ctx.className, ctx.methodName, ctx.methodDesc, ctx.methodArgCount);
    }

    private static String resolveMethodDescByArgCount(String className, String methodName, int argCount) {
        CallResolver resolver = getCallResolver();
        if (resolver == null) {
            return null;
        }
        return resolver.resolveMethodDescByArgCount(className, methodName, argCount);
    }

    private static String resolveMethodDescByArgTypes(String className,
                                                      String methodName,
                                                      List<ResolvedType> argTypes,
                                                      ResolvedType scopeType,
                                                      boolean preferStatic) {
        CallResolver resolver = getCallResolver();
        if (resolver == null) {
            return null;
        }
        List<TypeRef> argRefs = toTypeRefs(argTypes);
        TypeRef scopeRef = toTypeRef(scopeType);
        return resolver.resolveMethodDescByArgTypes(
                className, methodName, argRefs, scopeRef, preferStatic);
    }

    private static String resolveMethodDescByCall(String className,
                                                  MethodCallExpr call,
                                                  CallerContext ctx,
                                                  ResolvedType scopeType,
                                                  boolean preferStatic) {
        CallResolver resolver = getCallResolver();
        if (resolver == null || call == null) {
            return null;
        }
        TypeRef scopeRef = toTypeRef(scopeType);
        CompilationUnit cu = ctx == null ? null : ctx.cu;
        return resolver.resolveMethodDescByCall(cu, className, call, toCallContext(ctx), scopeRef, preferStatic);
    }

    private static TypeRef toTypeRef(ResolvedType type) {
        if (type == null) {
            return null;
        }
        if (type.internalName != null && !type.internalName.trim().isEmpty()) {
            List<String> args = type.typeArguments;
            return TypeRef.fromInternalName(type.internalName,
                    args == null || args.isEmpty() ? null : args);
        }
        if (type.asmType != null) {
            return TypeRef.fromAsmType(type.asmType);
        }
        return null;
    }

    private static TypeRef toTypeRef(ResolvedType resolved, Type fallback) {
        TypeRef fromResolved = toTypeRef(resolved);
        if (fromResolved != null) {
            return fromResolved;
        }
        if (fallback != null) {
            return TypeRef.fromAsmType(fallback);
        }
        return null;
    }

    private static List<TypeRef> toTypeRefs(List<ResolvedType> types) {
        List<TypeRef> out = new ArrayList<>();
        if (types == null) {
            return out;
        }
        for (ResolvedType type : types) {
            out.add(toTypeRef(type));
        }
        return out;
    }

    private static ResolvedType toResolvedType(TypeRef typeRef) {
        if (typeRef == null) {
            return null;
        }
        if (typeRef.internalName != null && !typeRef.internalName.trim().isEmpty()) {
            return new ResolvedType(typeRef.internalName, typeRef.typeArguments);
        }
        if (typeRef.asmType != null) {
            return new ResolvedType(typeRef.asmType);
        }
        return null;
    }

    private static List<ResolvedType> toResolvedTypes(List<TypeRef> refs) {
        List<ResolvedType> out = new ArrayList<>();
        if (refs == null) {
            return out;
        }
        for (TypeRef ref : refs) {
            out.add(toResolvedType(ref));
        }
        return out;
    }

    private static List<ResolvedType> resolveArgumentTypes(CompilationUnit cu,
                                                           List<Expression> args,
                                                           CallerContext ctx) {
        TypeSolver solver = getTypeSolver();
        if (solver == null) {
            return new ArrayList<>();
        }
        List<TypeRef> refs = solver.resolveArgumentTypes(cu, args, toCallContext(ctx));
        return toResolvedTypes(refs);
    }

    private static List<ResolvedType> resolveMethodReferenceParamTypes(CompilationUnit cu,
                                                                       MethodReferenceExpr ref,
                                                                       CallerContext ctx) {
        TypeSolver solver = getTypeSolver();
        if (solver == null) {
            return null;
        }
        List<TypeRef> refs = solver.resolveMethodReferenceParamTypes(cu, ref, toCallContext(ctx));
        if (refs == null) {
            return null;
        }
        return toResolvedTypes(refs);
    }

    private static MethodCallExpr findMethodCallAt(CompilationUnit cu, Position pos) {
        if (cu == null || pos == null) {
            return null;
        }
        CallResolver resolver = getCallResolver();
        return resolver == null ? null : resolver.findMethodCallAt(cu, pos);
    }

    private static MethodReferenceExpr findMethodReferenceAt(CompilationUnit cu, Position pos) {
        if (cu == null || pos == null) {
            return null;
        }
        CallResolver resolver = getCallResolver();
        return resolver == null ? null : resolver.findMethodReferenceAt(cu, pos);
    }

    private static CallPosition resolveCallPosition(CompilationUnit cu, Position pos) {
        return resolveCallPosition(cu, pos, null);
    }

    private static CallPosition resolveCallPosition(CompilationUnit cu, Position pos, CallerContext ctx) {
        if (cu == null || pos == null) {
            return null;
        }
        MethodCallExpr call = findMethodCallAt(cu, pos);
        if (call == null) {
            return null;
        }
        return resolveCallPosition(cu, call, ctx);
    }

    private static CallPosition resolveCallPosition(CompilationUnit cu, MethodCallExpr call) {
        return resolveCallPosition(cu, call, null);
    }

    private static CallPosition resolveCallPosition(CompilationUnit cu, MethodCallExpr call, CallerContext ctx) {
        if (cu == null || call == null) {
            return null;
        }
        CallResolver resolver = getCallResolver();
        if (resolver == null) {
            return null;
        }
        me.n1ar4.jar.analyzer.semantic.CallPosition pos =
                resolver.resolveCallPosition(cu, call, toCallContext(ctx));
        if (pos == null) {
            return null;
        }
        return new CallPosition(pos.call, pos.callIndex, pos.inLambda, pos.line);
    }

    private static String resolveScopeHint(CallerContext ctx, SemanticTarget target) {
        CallResolver resolver = getCallResolver();
        if (resolver == null) {
            return target == null ? null : target.className;
        }
        return resolver.resolveScopeHint(toCallContext(ctx), toSymbolRef(target));
    }

    private static CallSiteSelection resolveCallSiteTarget(CallerContext ctx,
                                                           String methodName,
                                                           int argCount,
                                                           String scopeHint,
                                                           CallPosition callPosition) {
        CallResolver resolver = getCallResolver();
        if (resolver == null) {
            return null;
        }
        me.n1ar4.jar.analyzer.semantic.CallPosition newPos = null;
        if (callPosition != null) {
            newPos = new me.n1ar4.jar.analyzer.semantic.CallPosition(
                    callPosition.call, callPosition.callIndex, callPosition.inLambda, callPosition.line);
        }
        me.n1ar4.jar.analyzer.semantic.CallSiteSelection selection = resolver.resolveCallSiteTarget(
                toCallContext(ctx), methodName, argCount, scopeHint, newPos);
        if (selection == null) {
            return null;
        }
        return new CallSiteSelection(selection.className, selection.methodName, selection.methodDesc);
    }

    private static final class ResolvedType {
        private final String internalName;
        private final List<String> typeArguments;
        private final Type asmType;

        private ResolvedType(String internalName, List<String> typeArguments) {
            this.internalName = internalName;
            this.typeArguments = typeArguments;
            if (internalName != null && !internalName.trim().isEmpty()) {
                this.asmType = Type.getObjectType(internalName);
            } else {
                this.asmType = null;
            }
        }

        private ResolvedType(Type asmType) {
            this.asmType = asmType;
            if (asmType != null && asmType.getSort() == Type.OBJECT) {
                this.internalName = asmType.getInternalName();
            } else {
                this.internalName = null;
            }
            this.typeArguments = null;
        }

        private ResolvedType(Type asmType, List<String> typeArguments) {
            this.asmType = asmType;
            if (asmType != null && asmType.getSort() == Type.OBJECT) {
                this.internalName = asmType.getInternalName();
            } else {
                this.internalName = null;
            }
            this.typeArguments = typeArguments;
        }

        private boolean isPrimitive() {
            return asmType != null && asmType.getSort() >= Type.BOOLEAN && asmType.getSort() <= Type.DOUBLE;
        }

        private boolean isArray() {
            return asmType != null && asmType.getSort() == Type.ARRAY;
        }
    }

    private enum SymbolKind {
        METHOD,
        CONSTRUCTOR,
        CLASS,
        INTERFACE,
        ENUM,
        ANNOTATION,
        RECORD,
        PACKAGE,
        FIELD,
        VARIABLE,
        TYPE_PARAM,
        UNKNOWN
    }

    private static final class SemanticTarget {
        private final SymbolKind kind;
        private final String name;
        private final String className;
        private final Range declarationRange;
        private final String typeName;
        private final int argCount;

        private SemanticTarget(SymbolKind kind, String name, String className) {
            this(kind, name, className, null, null, ARG_COUNT_UNKNOWN);
        }

        private SemanticTarget(SymbolKind kind,
                               String name,
                               String className,
                               Range declarationRange,
                               String typeName) {
            this(kind, name, className, declarationRange, typeName, ARG_COUNT_UNKNOWN);
        }

        private SemanticTarget(SymbolKind kind,
                               String name,
                               String className,
                               Range declarationRange,
                               String typeName,
                               int argCount) {
            this.kind = kind;
            this.name = name;
            this.className = className;
            this.declarationRange = declarationRange;
            this.typeName = typeName;
            this.argCount = argCount;
        }
    }

    private static final class SimpleLineMapping {
        private final String methodName;
        private final String methodDesc;
        private final NavigableMap<Integer, Integer> decompiledToSource;

        private SimpleLineMapping(String methodName,
                                  String methodDesc,
                                  NavigableMap<Integer, Integer> decompiledToSource) {
            this.methodName = methodName;
            this.methodDesc = methodDesc;
            this.decompiledToSource = decompiledToSource;
        }
    }

    private static final class ClassLineMapping {
        private final List<MethodLineMapping> mappings;
        private final Map<String, List<MethodLineMapping>> byName;

        private ClassLineMapping(List<MethodLineMapping> mappings,
                                 Map<String, List<MethodLineMapping>> byName) {
            this.mappings = mappings;
            this.byName = byName;
        }
    }

    private static final class MethodLineMapping {
        private final String methodName;
        private final String methodDesc;
        private final int argCount;
        private final NavigableMap<Integer, Integer> decompiledToSource;
        private final int minLine;
        private final int maxLine;

        private MethodLineMapping(String methodName,
                                  String methodDesc,
                                  int argCount,
                                  NavigableMap<Integer, Integer> decompiledToSource,
                                  int minLine,
                                  int maxLine) {
            this.methodName = methodName;
            this.methodDesc = methodDesc;
            this.argCount = argCount;
            this.decompiledToSource = decompiledToSource;
            this.minLine = minLine;
            this.maxLine = maxLine;
        }
    }

    private static final class CallerContext {
        private final CompilationUnit cu;
        private final Position position;
        private final int line;
        private final String className;
        private final String methodName;
        private final String methodDesc;
        private final int methodArgCount;

        private CallerContext(CompilationUnit cu,
                              Position position,
                              int line,
                              String className,
                              String methodName,
                              String methodDesc,
                              int methodArgCount) {
            this.cu = cu;
            this.position = position;
            this.line = line;
            this.className = className;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
            this.methodArgCount = methodArgCount;
        }
    }

    private static final class CallSiteSelection {
        private final String className;
        private final String methodName;
        private final String methodDesc;

        private CallSiteSelection(String className, String methodName, String methodDesc) {
            this.className = className;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
        }
    }

    private static final class CallPosition {
        private final MethodCallExpr call;
        private final int callIndex;
        private final boolean inLambda;
        private final int line;

        private CallPosition(MethodCallExpr call, int callIndex, boolean inLambda, int line) {
            this.call = call;
            this.callIndex = callIndex;
            this.inLambda = inLambda;
            this.line = line;
        }
    }
}
