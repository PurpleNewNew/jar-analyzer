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
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.TypeParameter;
import me.n1ar4.jar.analyzer.core.FinderRunner;
import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.DecompileEngine;
import me.n1ar4.jar.analyzer.engine.CoreHelper;
import me.n1ar4.jar.analyzer.engine.index.IndexPluginsSupport;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.entity.LineMappingEntity;
import me.n1ar4.jar.analyzer.entity.MemberEntity;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;
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
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
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
    private static final Map<String, ClassSignatureCache> CLASS_SIGNATURE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, ClassLineMapping> LINE_MAPPINGS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> SUBTYPE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Integer> DISTANCE_CACHE = new ConcurrentHashMap<>();
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
        List<MethodResult> finalCandidates = candidatesForPick;
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
                                        typeName = resolveLocalVarTypeFromDb(
                                                callerContext.className,
                                                callerContext.methodName,
                                                callerContext.methodDesc,
                                                target.name,
                                                callerContext.line);
                                    }
                                    if (typeName != null && !typeName.trim().isEmpty()) {
                                        String finalClassName = typeName;
                                        SyntaxAreaHelper.runOnEdt(() -> {
                                            MainForm.setCurClass(finalClassName);
                                            MainForm.getInstance().getCurClassText().setText(finalClassName);
                                        });
                                        dialog = NavigationHelper.maybeShowProgressDialog(finalClassName, false);
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
                                if (target.kind == SymbolKind.PACKAGE) {
                                    String pkg = target.className;
                                    if (pkg == null || pkg.trim().isEmpty()) {
                                        SyntaxAreaHelper.runOnEdt(() -> JOptionPane.showMessageDialog(
                                                MainForm.getInstance().getMasterPanel(),
                                                "<html><p>unable to resolve package name</p></html>"));
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
                                    dialog = NavigationHelper.maybeShowProgressDialog(finalClassName, false);
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
                                if (callPosition != null && callPosition.call != null) {
                                    MethodCallExpr callExpr = callPosition.call;
                                    argTypes = resolveArgumentTypes(cuSnapshot, callExpr.getArguments(), callerContext);
                                    if (callExpr.getScope().isPresent()) {
                                        scopeType = resolveExpressionType(cuSnapshot, callExpr.getScope().get(), callerContext);
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
                                    if (argTypes != null) {
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
                                        String methodNameForUi = methodNameLocal;
                                        List<MethodResult> finalCandidatesForPick = candidatesForPick;
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
                                dialog = NavigationHelper.maybeShowProgressDialog(finalClassName, false);
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

    private static <T> T findAncestor(Node node, Class<T> cls) {
        Node cur = node;
        while (cur != null) {
            if (cls.isInstance(cur)) {
                return cls.cast(cur);
            }
            cur = cur.getParentNode().orElse(null);
        }
        return null;
    }

    private static SemanticTarget classifyName(CompilationUnit cu, SimpleName name) {
        if (name == null) {
            return new SemanticTarget(SymbolKind.UNKNOWN, null, null);
        }
        Node parent = name.getParentNode().orElse(null);
        PackageDeclaration pkgDecl = findAncestor(name, PackageDeclaration.class);
        if (pkgDecl != null) {
            return new SemanticTarget(SymbolKind.PACKAGE, name.asString(), pkgDecl.getNameAsString());
        }
        ImportDeclaration impDecl = findAncestor(name, ImportDeclaration.class);
        if (impDecl != null && !impDecl.isStatic()) {
            if (impDecl.isAsterisk()) {
                return new SemanticTarget(SymbolKind.PACKAGE, name.asString(), impDecl.getNameAsString());
            }
            String importName = impDecl.getNameAsString();
            if (importName != null) {
                String simple = name.asString();
                if (importName.equals(simple) || importName.endsWith("." + simple)) {
                    String cls = importName.replace('.', '/');
                    SymbolKind kind = resolveClassKind(cls, SymbolKind.CLASS);
                    return new SemanticTarget(kind, name.asString(), cls);
                }
            }
            return new SemanticTarget(SymbolKind.PACKAGE, name.asString(), impDecl.getNameAsString());
        }
        if (parent instanceof MethodCallExpr) {
            MethodCallExpr call = (MethodCallExpr) parent;
            if (call.getName() == name) {
                String classHint = resolveScopeClassName(cu, call);
                int argCount = call.getArguments() == null ? ARG_COUNT_UNKNOWN : call.getArguments().size();
                return new SemanticTarget(SymbolKind.METHOD, call.getNameAsString(), classHint, null, null, argCount);
            }
        }
        if (parent instanceof MethodReferenceExpr) {
            MethodReferenceExpr ref = (MethodReferenceExpr) parent;
            String identifier = ref.getIdentifier();
            if (identifier != null && identifier.equals(name.asString())) {
                String classHint = null;
                Expression scope = ref.getScope();
                if (scope instanceof TypeExpr) {
                    TypeExpr te = (TypeExpr) scope;
                    classHint = resolveTypeToClassName(cu, te.getTypeAsString());
                } else {
                    classHint = resolveClassNameFromExpression(cu, scope, ref, null);
                }
                if ("new".equals(identifier)) {
                    return new SemanticTarget(SymbolKind.CONSTRUCTOR, identifier, classHint);
                }
                return new SemanticTarget(SymbolKind.METHOD, identifier, classHint);
            }
        }
        if (parent instanceof MethodDeclaration) {
            MethodDeclaration decl = (MethodDeclaration) parent;
            int argCount = decl.getParameters() == null ? ARG_COUNT_UNKNOWN : decl.getParameters().size();
            return new SemanticTarget(SymbolKind.METHOD, name.asString(),
                    resolveEnclosingClassName(cu, parent), null, null, argCount);
        }
        if (parent instanceof ConstructorDeclaration) {
            String cls = resolveEnclosingClassName(cu, parent);
            ConstructorDeclaration decl = (ConstructorDeclaration) parent;
            int argCount = decl.getParameters() == null ? ARG_COUNT_UNKNOWN : decl.getParameters().size();
            return new SemanticTarget(SymbolKind.CONSTRUCTOR, name.asString(), cls, null, null, argCount);
        }
        if (parent instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration decl = (ClassOrInterfaceDeclaration) parent;
            String cls = resolveQualifiedClassName(cu, name.asString());
            SymbolKind kind = decl.isInterface() ? SymbolKind.INTERFACE : SymbolKind.CLASS;
            return new SemanticTarget(kind, name.asString(), cls);
        }
        if (parent instanceof EnumDeclaration) {
            String cls = resolveQualifiedClassName(cu, name.asString());
            return new SemanticTarget(SymbolKind.ENUM, name.asString(), cls);
        }
        if (parent instanceof RecordDeclaration) {
            String cls = resolveQualifiedClassName(cu, name.asString());
            return new SemanticTarget(SymbolKind.RECORD, name.asString(), cls);
        }
        if (parent instanceof AnnotationDeclaration) {
            String cls = resolveQualifiedClassName(cu, name.asString());
            return new SemanticTarget(SymbolKind.ANNOTATION, name.asString(), cls);
        }
        if (parent instanceof TypeParameter) {
            Range range = name.getRange().orElse(null);
            return new SemanticTarget(SymbolKind.TYPE_PARAM, name.asString(), null, range, null);
        }
        if (parent instanceof ClassOrInterfaceType) {
            String cls = resolveQualifiedClassName(cu, name.asString());
            if (isConstructorContext(parent)) {
                int argCount = resolveConstructorArgCount(parent);
                return new SemanticTarget(SymbolKind.CONSTRUCTOR, name.asString(), cls, null, null, argCount);
            }
            SymbolKind kind = resolveClassKind(cls, SymbolKind.CLASS);
            return new SemanticTarget(kind, name.asString(), cls);
        }
        DeclaredSymbol decl = resolveDeclaration(cu, name);
        if (decl != null) {
            return new SemanticTarget(decl.kind, decl.name, null, decl.range, decl.typeName);
        }
        String staticOwner = resolveStaticImportOwner(cu, name.asString());
        if (staticOwner != null) {
            if (parent instanceof MethodCallExpr) {
                MethodCallExpr call = (MethodCallExpr) parent;
                int argCount = call.getArguments() == null ? ARG_COUNT_UNKNOWN : call.getArguments().size();
                return new SemanticTarget(SymbolKind.METHOD, name.asString(), staticOwner, null, null, argCount);
            }
            return new SemanticTarget(SymbolKind.FIELD, name.asString(), staticOwner);
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

    private static int resolveConstructorArgCount(Node node) {
        Node cur = node;
        while (cur != null) {
            if (cur instanceof ObjectCreationExpr) {
                ObjectCreationExpr expr = (ObjectCreationExpr) cur;
                if (expr.getArguments() == null) {
                    return ARG_COUNT_UNKNOWN;
                }
                return expr.getArguments().size();
            }
            cur = cur.getParentNode().orElse(null);
        }
        return ARG_COUNT_UNKNOWN;
    }

    private static String resolveScopeClassName(CompilationUnit cu, MethodCallExpr call) {
        return resolveScopeClassName(cu, call, null);
    }

    private static String resolveScopeClassName(CompilationUnit cu, MethodCallExpr call, CallerContext ctx) {
        if (call == null) {
            return null;
        }
        if (!call.getScope().isPresent()) {
            String staticOwner = resolveStaticImportOwner(cu, call.getNameAsString());
            if (staticOwner != null) {
                return staticOwner;
            }
            return resolveEnclosingClassName(cu, call);
        }
        Expression scope = unwrapEnclosed(call.getScope().get());
        return resolveClassNameFromExpression(cu, scope, call, ctx);
    }

    private static boolean isStaticScope(CompilationUnit cu, MethodCallExpr call, CallerContext ctx) {
        if (call == null || !call.getScope().isPresent()) {
            String staticOwner = resolveStaticImportOwner(cu, call == null ? null : call.getNameAsString());
            return staticOwner != null;
        }
        Expression scope = unwrapEnclosed(call.getScope().get());
        if (scope instanceof TypeExpr) {
            return true;
        }
        if (scope instanceof NameExpr) {
            String name = ((NameExpr) scope).getNameAsString();
            if (looksLikeClassName(name)) {
                String cls = resolveQualifiedClassName(cu, name);
                return cls != null;
            }
        }
        ResolvedType resolved = resolveExpressionType(cu, scope, ctx);
        return resolved != null && resolved.internalName != null
                && looksLikeClassName(resolved.internalName.substring(resolved.internalName.lastIndexOf('/') + 1));
    }

    private static boolean preferStaticForMethodRef(MethodReferenceExpr ref,
                                                    String ownerClass,
                                                    int argCount) {
        if (ref == null || ownerClass == null || MainForm.getEngine() == null) {
            return false;
        }
        Expression scope = ref.getScope();
        if (!(scope instanceof TypeExpr)) {
            return false;
        }
        String name = ref.getIdentifier();
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        List<MethodResult> methods = MainForm.getEngine().getMethod(ownerClass, name, null);
        if (methods == null || methods.isEmpty()) {
            return false;
        }
        boolean hasStatic = false;
        boolean hasInstance = false;
        for (MethodResult method : methods) {
            if (method == null || method.getMethodDesc() == null) {
                continue;
            }
            int count = argCountFromDesc(method.getMethodDesc());
            if (count != argCount) {
                continue;
            }
            if (method.getIsStaticInt() == 1) {
                hasStatic = true;
            } else {
                hasInstance = true;
            }
        }
        if (hasStatic && !hasInstance) {
            return true;
        }
        if (hasInstance && !hasStatic) {
            return false;
        }
        return false;
    }

    private static Expression unwrapEnclosed(Expression expr) {
        Expression cur = expr;
        while (cur instanceof EnclosedExpr) {
            cur = ((EnclosedExpr) cur).getInner();
        }
        return cur;
    }

    private static String resolveClassNameFromExpression(CompilationUnit cu, Expression expr, Node context) {
        return resolveClassNameFromExpression(cu, expr, context, null);
    }

    private static String resolveClassNameFromExpression(CompilationUnit cu,
                                                         Expression expr,
                                                         Node context,
                                                         CallerContext ctx) {
        ResolvedType resolved = resolveExpressionType(cu, expr, ctx);
        if (resolved != null && resolved.internalName != null) {
            return resolved.internalName;
        }
        return null;
    }

    private static ResolvedType resolveExpressionType(CompilationUnit cu,
                                                      Expression expr,
                                                      CallerContext ctx) {
        if (expr == null) {
            return null;
        }
        Expression scope = unwrapEnclosed(expr);
        if (scope instanceof ThisExpr) {
            String cls = ctx != null ? ctx.className : resolveEnclosingClassName(cu, scope);
            if (cls != null) {
                return new ResolvedType(normalizeClassName(cls), null);
            }
            return null;
        }
        if (scope instanceof LiteralExpr) {
            if (scope instanceof StringLiteralExpr || scope instanceof TextBlockLiteralExpr) {
                return new ResolvedType("java/lang/String", null);
            }
            if (scope instanceof IntegerLiteralExpr) {
                return new ResolvedType(Type.INT_TYPE);
            }
            if (scope instanceof LongLiteralExpr) {
                return new ResolvedType(Type.LONG_TYPE);
            }
            if (scope instanceof DoubleLiteralExpr) {
                return new ResolvedType(Type.DOUBLE_TYPE);
            }
            if (scope instanceof BooleanLiteralExpr) {
                return new ResolvedType(Type.BOOLEAN_TYPE);
            }
            if (scope instanceof CharLiteralExpr) {
                return new ResolvedType(Type.CHAR_TYPE);
            }
            if (scope instanceof NullLiteralExpr) {
                return null;
            }
        }
        if (scope instanceof UnaryExpr) {
            UnaryExpr unary = (UnaryExpr) scope;
            return resolveExpressionType(cu, unary.getExpression(), ctx);
        }
        if (scope instanceof AssignExpr) {
            AssignExpr assign = (AssignExpr) scope;
            return resolveExpressionType(cu, assign.getValue(), ctx);
        }
        if (scope instanceof BinaryExpr) {
            BinaryExpr binary = (BinaryExpr) scope;
            ResolvedType left = resolveExpressionType(cu, binary.getLeft(), ctx);
            ResolvedType right = resolveExpressionType(cu, binary.getRight(), ctx);
            if (binary.getOperator() == BinaryExpr.Operator.PLUS) {
                if (isStringType(left) || isStringType(right)) {
                    return new ResolvedType("java/lang/String", null);
                }
            }
            if (left != null && right != null && left.isPrimitive() && right.isPrimitive()) {
                Type wider = widerPrimitiveType(left.asmType, right.asmType);
                if (wider != null) {
                    return new ResolvedType(wider);
                }
            }
        }
        if (scope instanceof ArrayCreationExpr) {
            ArrayCreationExpr array = (ArrayCreationExpr) scope;
            ResolvedType elementType = resolveTypeFromAstType(cu, array.getElementType());
            int dims = array.getLevels() == null ? 1 : array.getLevels().size();
            if (elementType != null && elementType.asmType != null) {
                try {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < Math.max(1, dims); i++) {
                        sb.append('[');
                    }
                    sb.append(elementType.asmType.getDescriptor());
                    return new ResolvedType(Type.getType(sb.toString()));
                } catch (Exception ignored) {
                }
            }
            if (elementType != null && elementType.internalName != null) {
                try {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < Math.max(1, dims); i++) {
                        sb.append('[');
                    }
                    sb.append('L').append(elementType.internalName).append(';');
                    return new ResolvedType(Type.getType(sb.toString()));
                } catch (Exception ignored) {
                }
            }
            return null;
        }
        if (scope instanceof MethodReferenceExpr) {
            ResolvedType fiType = resolveFunctionalInterfaceTypeFromContext(cu, scope, ctx);
            if (fiType != null) {
                return fiType;
            }
        }
        if (scope instanceof TypeExpr) {
            TypeExpr typeExpr = (TypeExpr) scope;
            return resolveTypeFromAstType(cu, typeExpr.getType());
        }
        if (scope instanceof SuperExpr) {
            String cls = ctx != null ? ctx.className : resolveEnclosingClassName(cu, scope);
            if (cls != null && MainForm.getEngine() != null) {
                ClassResult result = MainForm.getEngine().getClassByClass(cls);
                if (result != null && result.getSuperClassName() != null) {
                    return new ResolvedType(result.getSuperClassName(), null);
                }
            }
            return null;
        }
        if (scope instanceof NameExpr) {
            String name = ((NameExpr) scope).getNameAsString();
            if (looksLikeQualifiedClassName(name) || looksLikeClassName(name)) {
                String cls = resolveQualifiedClassName(cu, name);
                return cls == null ? null : new ResolvedType(cls, null);
            }
            ResolvedType localType = resolveLocalDeclaredType(
                    cu, name, scope.getBegin().orElse(null), scope);
            if (localType != null) {
                return localType;
            }
            ResolvedType flowType = resolveLocalAssignedType(
                    cu, name, scope.getBegin().orElse(null), scope, ctx);
            if (flowType != null) {
                return flowType;
            }
            ResolvedType narrowed = resolveNarrowedTypeFromConditions(cu, name, scope);
            if (narrowed != null) {
                return narrowed;
            }
            ResolvedType lambdaParamType = resolveLambdaParamType(
                    cu, name, scope.getBegin().orElse(null), scope, ctx);
            if (lambdaParamType != null) {
                return lambdaParamType;
            }
            ResolvedType fieldAstType = resolveFieldTypeInAst(cu, name, scope);
            if (fieldAstType != null) {
                return fieldAstType;
            }
            DeclaredSymbol decl = resolveDeclaration(cu, ((NameExpr) scope).getName());
            if (decl != null && decl.typeName != null) {
                return resolveTypeFromClassName(decl.typeName);
            }
            if (ctx != null) {
                ResolvedType bytecodeLocal = resolveLocalVarTypeFromDbType(
                        ctx.className, ctx.methodName, ctx.methodDesc, name, ctx.line);
                if (bytecodeLocal != null) {
                    return bytecodeLocal;
                }
                ResolvedType fieldType = resolveFieldTypeFromDb(ctx.className, name);
                if (fieldType != null) {
                    return fieldType;
                }
            }
            String staticOwner = resolveStaticImportOwner(cu, name);
            if (staticOwner != null) {
                ResolvedType fieldType = resolveFieldTypeFromDb(staticOwner, name);
                if (fieldType != null) {
                    return fieldType;
                }
                return new ResolvedType(staticOwner, null);
            }
            return null;
        }
        if (scope instanceof FieldAccessExpr) {
            FieldAccessExpr fa = (FieldAccessExpr) scope;
            Expression faScope = unwrapEnclosed(fa.getScope());
            if (faScope instanceof ThisExpr || faScope instanceof SuperExpr) {
                ResolvedType astField = resolveFieldTypeInAst(cu, fa.getNameAsString(), fa);
                if (astField != null) {
                    return astField;
                }
                DeclaredSymbol field = resolveFieldDeclaration(cu, fa.getNameAsString(), fa);
                if (field != null && field.typeName != null) {
                    return resolveTypeFromClassName(field.typeName);
                }
            }
            ResolvedType scopeType = resolveExpressionType(cu, faScope, ctx);
            if (scopeType != null && scopeType.internalName != null) {
                ResolvedType fieldType = resolveFieldTypeFromDb(scopeType.internalName, fa.getNameAsString());
                if (fieldType != null) {
                    return fieldType;
                }
            }
            String text = fa.toString();
            if (looksLikeQualifiedClassName(text)) {
                String cls = resolveQualifiedClassName(cu, text);
                return cls == null ? null : new ResolvedType(cls, null);
            }
            if (faScope instanceof NameExpr) {
                String scopeName = ((NameExpr) faScope).getNameAsString();
                if (looksLikeClassName(scopeName)) {
                    String cls = resolveQualifiedClassName(cu, text);
                    return cls == null ? null : new ResolvedType(cls, null);
                }
            }
            return null;
        }
        if (scope instanceof ObjectCreationExpr) {
            ObjectCreationExpr oce = (ObjectCreationExpr) scope;
            ResolvedType type = resolveTypeFromAstType(cu, oce.getType());
            return type;
        }
        if (scope instanceof CastExpr) {
            CastExpr cast = (CastExpr) scope;
            return resolveTypeFromAstType(cu, cast.getType());
        }
        if (scope instanceof ConditionalExpr) {
            ConditionalExpr cond = (ConditionalExpr) scope;
            ResolvedType thenType = resolveExpressionType(cu, cond.getThenExpr(), ctx);
            ResolvedType elseType = resolveExpressionType(cu, cond.getElseExpr(), ctx);
            if (thenType != null && elseType != null
                    && thenType.internalName != null
                    && thenType.internalName.equals(elseType.internalName)) {
                return thenType;
            }
            return thenType != null ? thenType : elseType;
        }
        if (scope instanceof LambdaExpr) {
            ResolvedType functionalType = resolveFunctionalInterfaceTypeFromContext(cu, scope, ctx);
            if (functionalType != null) {
                return functionalType;
            }
            Node parent = scope.getParentNode().orElse(null);
            if (parent instanceof VariableDeclarator) {
                VariableDeclarator vd = (VariableDeclarator) parent;
                return resolveTypeFromAstType(cu, vd.getType());
            }
            if (parent instanceof Parameter) {
                Parameter param = (Parameter) parent;
                return resolveTypeFromAstType(cu, param.getType());
            }
        }
        if (scope instanceof MethodCallExpr) {
            return resolveMethodCallReturnType(cu, (MethodCallExpr) scope, ctx);
        }
        return null;
    }

    private static ResolvedType resolveTypeFromClassName(String className) {
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeClassName(className);
        return new ResolvedType(normalized, null);
    }

    private static ResolvedType resolveTypeFromAstType(CompilationUnit cu,
                                                       com.github.javaparser.ast.type.Type type) {
        if (type == null) {
            return null;
        }
        if (type.isVarType()) {
            return null;
        }
        if (type.isPrimitiveType()) {
            try {
                com.github.javaparser.ast.type.PrimitiveType prim = type.asPrimitiveType();
                switch (prim.getType()) {
                    case BOOLEAN:
                        return new ResolvedType(Type.BOOLEAN_TYPE);
                    case BYTE:
                        return new ResolvedType(Type.BYTE_TYPE);
                    case CHAR:
                        return new ResolvedType(Type.CHAR_TYPE);
                    case SHORT:
                        return new ResolvedType(Type.SHORT_TYPE);
                    case INT:
                        return new ResolvedType(Type.INT_TYPE);
                    case LONG:
                        return new ResolvedType(Type.LONG_TYPE);
                    case FLOAT:
                        return new ResolvedType(Type.FLOAT_TYPE);
                    case DOUBLE:
                        return new ResolvedType(Type.DOUBLE_TYPE);
                    default:
                        return null;
                }
            } catch (Exception ignored) {
                return null;
            }
        }
        if (type.isArrayType()) {
            com.github.javaparser.ast.type.Type element = type.getElementType();
            ResolvedType elementType = resolveTypeFromAstType(cu, element);
            if (elementType != null && elementType.asmType != null) {
                try {
                    String desc = elementType.asmType.getDescriptor();
                    return new ResolvedType(Type.getType("[" + desc));
                } catch (Exception ignored) {
                }
            }
            if (elementType != null && elementType.internalName != null) {
                try {
                    return new ResolvedType(Type.getType("[L" + elementType.internalName + ";"));
                } catch (Exception ignored) {
                }
            }
            return null;
        }
        String name = type.asString();
        String raw = normalizeTypeName(name);
        String className = resolveQualifiedClassName(cu, raw == null ? name : raw);
        List<String> args = resolveAstTypeArguments(cu, type);
        return className == null ? null : new ResolvedType(className, args);
    }

    private static List<String> resolveAstTypeArguments(CompilationUnit cu,
                                                        com.github.javaparser.ast.type.Type type) {
        if (type == null || !type.isClassOrInterfaceType()) {
            return null;
        }
        ClassOrInterfaceType cit = type.asClassOrInterfaceType();
        if (!cit.getTypeArguments().isPresent()) {
            return null;
        }
        List<String> out = new ArrayList<>();
        for (com.github.javaparser.ast.type.Type arg : cit.getTypeArguments().get()) {
            ResolvedType resolved = resolveTypeFromAstType(cu, arg);
            if (resolved != null) {
                out.add(resolved.internalName);
            }
        }
        return out.isEmpty() ? null : out;
    }

    private static ResolvedType resolveMethodCallReturnType(CompilationUnit cu,
                                                            MethodCallExpr call,
                                                            CallerContext ctx) {
        if (call == null) {
            return null;
        }
        String scopeHint = null;
        ResolvedType scopeType = null;
        if (call.getScope().isPresent()) {
            scopeType = resolveExpressionType(cu, call.getScope().get(), ctx);
            if (scopeType != null) {
                scopeHint = scopeType.internalName;
            }
        } else {
            scopeHint = resolveStaticImportOwner(cu, call.getNameAsString());
            if (scopeHint == null && ctx != null) {
                scopeHint = ctx.className;
            }
            if (scopeHint == null) {
                scopeHint = resolveEnclosingClassName(cu, call);
            }
        }
        CallSiteSelection selection = null;
        if (ctx != null) {
            CallPosition callPos = resolveCallPosition(cu, call, ctx);
            selection = resolveCallSiteTarget(
                    ctx,
                    call.getNameAsString(),
                    call.getArguments() == null ? ARG_COUNT_UNKNOWN : call.getArguments().size(),
                    scopeHint,
                    callPos);
        }
        if (selection == null && scopeHint != null) {
            List<ResolvedType> argTypes = resolveArgumentTypes(cu, call.getArguments(), ctx);
            boolean preferStatic = isStaticScope(cu, call, ctx);
            String methodDesc = resolveMethodDescByArgTypes(
                    scopeHint,
                    call.getNameAsString(),
                    argTypes,
                    scopeType,
                    preferStatic);
            if (methodDesc == null) {
                methodDesc = resolveMethodDescByArgCount(
                        scopeHint,
                        call.getNameAsString(),
                        call.getArguments() == null ? ARG_COUNT_UNKNOWN : call.getArguments().size());
            }
            if (methodDesc != null) {
                selection = new CallSiteSelection(scopeHint, call.getNameAsString(), methodDesc);
            }
        }
        if (selection == null) {
            return null;
        }
        return resolveMethodReturnType(selection.className,
                selection.methodName, selection.methodDesc, scopeType);
    }

    private static int resolveMethodReferenceArgCount(CompilationUnit cu,
                                                      MethodReferenceExpr ref,
                                                      CallerContext ctx) {
        if (ref == null) {
            return ARG_COUNT_UNKNOWN;
        }
        ResolvedType fiType = resolveFunctionalInterfaceTypeFromContext(cu, ref, ctx);
        if (fiType == null || fiType.internalName == null) {
            return ARG_COUNT_UNKNOWN;
        }
        MethodResult sam = resolveSamMethod(fiType.internalName);
        if (sam == null) {
            return ARG_COUNT_UNKNOWN;
        }
        int samArgs = argCountFromDesc(sam.getMethodDesc());
        if (samArgs <= 0) {
            return samArgs;
        }
        String identifier = ref.getIdentifier();
        if ("new".equals(identifier)) {
            return samArgs;
        }
        Expression scope = ref.getScope();
        if (!(scope instanceof TypeExpr)) {
            return samArgs;
        }
        String classHint = null;
        ResolvedType scopeType = resolveExpressionType(cu, scope, ctx);
        if (scopeType != null && scopeType.internalName != null) {
            classHint = scopeType.internalName;
        }
        if (classHint == null || MainForm.getEngine() == null) {
            return samArgs;
        }
        List<MethodResult> methods = MainForm.getEngine().getMethod(classHint, identifier, null);
        if (methods == null || methods.isEmpty()) {
            return samArgs;
        }
        boolean hasStatic = false;
        boolean hasInstance = false;
        int instanceArgs = Math.max(0, samArgs - 1);
        for (MethodResult method : methods) {
            if (method == null || method.getMethodDesc() == null) {
                continue;
            }
            int count = argCountFromDesc(method.getMethodDesc());
            if (method.getIsStaticInt() == 1 && count == samArgs) {
                hasStatic = true;
            }
            if (method.getIsStaticInt() == 0 && count == instanceArgs) {
                hasInstance = true;
            }
        }
        if (hasStatic) {
            return samArgs;
        }
        if (hasInstance) {
            return instanceArgs;
        }
        return samArgs;
    }

    private static ResolvedType resolveLambdaParamType(CompilationUnit cu,
                                                       String name,
                                                       Position usagePos,
                                                       Node context,
                                                       CallerContext ctx) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        Node scopeRoot = findScopeRoot(context);
        if (!(scopeRoot instanceof LambdaExpr)) {
            return null;
        }
        LambdaExpr lambda = (LambdaExpr) scopeRoot;
        int paramIndex = -1;
        if (lambda.getParameters() != null) {
            for (int i = 0; i < lambda.getParameters().size(); i++) {
                Parameter param = lambda.getParameters().get(i);
                if (param != null && name.equals(param.getNameAsString())) {
                    paramIndex = i;
                    break;
                }
            }
        }
        if (paramIndex < 0) {
            return null;
        }
        ResolvedType fiType = resolveFunctionalInterfaceTypeFromContext(cu, lambda, ctx);
        if (fiType == null || fiType.internalName == null) {
            return null;
        }
        MethodResult sam = resolveSamMethod(fiType.internalName);
        if (sam == null || sam.getMethodDesc() == null) {
            return null;
        }
        ResolvedType genericResolved = resolveSamParamType(fiType, sam, paramIndex);
        if (genericResolved != null) {
            return genericResolved;
        }
        try {
            Type[] args = Type.getArgumentTypes(sam.getMethodDesc());
            if (paramIndex < 0 || paramIndex >= args.length) {
                return null;
            }
            Type arg = args[paramIndex];
            if (arg.getSort() == Type.OBJECT) {
                return new ResolvedType(arg.getInternalName(), null);
            }
            if (arg.getSort() == Type.ARRAY) {
                Type element = arg.getElementType();
                if (element != null && element.getSort() == Type.OBJECT) {
                    return new ResolvedType(element.getInternalName(), null);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static ResolvedType resolveFunctionalInterfaceTypeFromContext(CompilationUnit cu,
                                                                          Node node,
                                                                          CallerContext ctx) {
        if (node == null) {
            return null;
        }
        Node parent = node.getParentNode().orElse(null);
        if (parent instanceof EnclosedExpr) {
            parent = parent.getParentNode().orElse(null);
        }
        if (parent instanceof CastExpr) {
            return resolveTypeFromAstType(cu, ((CastExpr) parent).getType());
        }
        if (parent instanceof VariableDeclarator) {
            VariableDeclarator vd = (VariableDeclarator) parent;
            return resolveTypeFromAstType(cu, vd.getType());
        }
        if (parent instanceof AssignExpr) {
            AssignExpr assign = (AssignExpr) parent;
            return resolveExpressionType(cu, assign.getTarget(), ctx);
        }
        if (parent instanceof MethodCallExpr) {
            MethodCallExpr call = (MethodCallExpr) parent;
            int argIndex = resolveArgumentIndex(call.getArguments(), node);
            if (argIndex >= 0) {
                return resolveFunctionalInterfaceTypeFromCallArg(cu, call, argIndex, ctx);
            }
        }
        if (parent instanceof ObjectCreationExpr) {
            ObjectCreationExpr call = (ObjectCreationExpr) parent;
            int argIndex = resolveArgumentIndex(call.getArguments(), node);
            if (argIndex >= 0) {
                return resolveFunctionalInterfaceTypeFromCtorArg(cu, call, argIndex);
            }
        }
        if (parent instanceof com.github.javaparser.ast.stmt.ReturnStmt) {
            if (ctx != null && ctx.className != null
                    && ctx.methodName != null && ctx.methodDesc != null) {
                return resolveMethodReturnType(ctx.className, ctx.methodName, ctx.methodDesc, null);
            }
        }
        return null;
    }

    private static int resolveArgumentIndex(List<Expression> args, Node target) {
        if (args == null || args.isEmpty() || target == null) {
            return -1;
        }
        for (int i = 0; i < args.size(); i++) {
            Expression arg = args.get(i);
            if (arg == null) {
                continue;
            }
            if (arg == target) {
                return i;
            }
            Range r1 = arg.getRange().orElse(null);
            Range r2 = target.getRange().orElse(null);
            if (r1 != null && r2 != null && r1.equals(r2)) {
                return i;
            }
        }
        return -1;
    }

    private static ResolvedType resolveFunctionalInterfaceTypeFromCallArg(CompilationUnit cu,
                                                                          MethodCallExpr call,
                                                                          int argIndex,
                                                                          CallerContext ctx) {
        if (call == null) {
            return null;
        }
        String scopeHint = resolveScopeClassName(cu, call, ctx);
        if (scopeHint == null) {
            return null;
        }
        int argCount = call.getArguments() == null ? ARG_COUNT_UNKNOWN : call.getArguments().size();
        CallSiteSelection selection = null;
        if (ctx != null) {
            CallPosition callPos = resolveCallPosition(cu, call, ctx);
            selection = resolveCallSiteTarget(ctx,
                    call.getNameAsString(),
                    argCount,
                    scopeHint,
                    callPos);
        }
        if (selection == null) {
            String methodDesc = resolveMethodDescByArgCount(scopeHint, call.getNameAsString(), argCount);
            if (methodDesc != null) {
                selection = new CallSiteSelection(scopeHint, call.getNameAsString(), methodDesc);
            }
        }
        if (selection == null || selection.methodDesc == null) {
            return null;
        }
        try {
            Type[] args = Type.getArgumentTypes(selection.methodDesc);
            if (argIndex < 0 || argIndex >= args.length) {
                return null;
            }
            Type arg = args[argIndex];
            if (arg.getSort() == Type.OBJECT) {
                return new ResolvedType(arg.getInternalName(), null);
            }
            if (arg.getSort() == Type.ARRAY) {
                Type element = arg.getElementType();
                if (element != null && element.getSort() == Type.OBJECT) {
                    return new ResolvedType(element.getInternalName(), null);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static ResolvedType resolveFunctionalInterfaceTypeFromCtorArg(CompilationUnit cu,
                                                                          ObjectCreationExpr call,
                                                                          int argIndex) {
        if (call == null) {
            return null;
        }
        ResolvedType owner = resolveTypeFromAstType(cu, call.getType());
        if (owner == null || owner.internalName == null) {
            return null;
        }
        int argCount = call.getArguments() == null ? ARG_COUNT_UNKNOWN : call.getArguments().size();
        String desc = resolveMethodDescByArgCount(owner.internalName, "<init>", argCount);
        if (desc == null) {
            return null;
        }
        try {
            Type[] args = Type.getArgumentTypes(desc);
            if (argIndex < 0 || argIndex >= args.length) {
                return null;
            }
            Type arg = args[argIndex];
            if (arg.getSort() == Type.OBJECT) {
                return new ResolvedType(arg.getInternalName(), null);
            }
            if (arg.getSort() == Type.ARRAY) {
                Type element = arg.getElementType();
                if (element != null && element.getSort() == Type.OBJECT) {
                    return new ResolvedType(element.getInternalName(), null);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static MethodResult resolveSamMethod(String interfaceName) {
        if (interfaceName == null || interfaceName.trim().isEmpty()) {
            return null;
        }
        if (MainForm.getEngine() == null) {
            return null;
        }
        List<MethodResult> methods = MainForm.getEngine().getMethod(interfaceName, null, null);
        if (methods == null || methods.isEmpty()) {
            return null;
        }
        List<MethodResult> candidates = new ArrayList<>();
        for (MethodResult method : methods) {
            if (method == null || method.getMethodName() == null) {
                continue;
            }
            String name = method.getMethodName();
            if ("<init>".equals(name) || "<clinit>".equals(name)) {
                continue;
            }
            if (method.getIsStaticInt() == 1) {
                continue;
            }
            if ((method.getAccessInt() & Opcodes.ACC_ABSTRACT) == 0) {
                continue;
            }
            candidates.add(method);
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        return null;
    }

    private static ResolvedType resolveSamParamType(ResolvedType interfaceType,
                                                    MethodResult sam,
                                                    int paramIndex) {
        if (interfaceType == null || sam == null) {
            return null;
        }
        if (interfaceType.internalName == null || interfaceType.internalName.trim().isEmpty()) {
            return null;
        }
        ClassSignatureCache cache = getClassSignatureCache(interfaceType.internalName);
        if (cache == null || cache.methodSignatures == null || cache.methodSignatures.isEmpty()) {
            return null;
        }
        String signature = cache.methodSignatures.get(sam.getMethodName() + sam.getMethodDesc());
        if (signature == null || signature.trim().isEmpty()) {
            return null;
        }
        List<GenericType> params = parseMethodParamSignatures(signature);
        if (params == null || paramIndex < 0 || paramIndex >= params.size()) {
            return null;
        }
        Map<String, String> bindings = buildGenericBindings(cache, interfaceType);
        return resolveGenericType(params.get(paramIndex), bindings);
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
            if (isInsideExcludedScope(param, scopeRoot)) {
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
            if (isInsideExcludedScope(var, scopeRoot)) {
                continue;
            }
            if (!isInSameBranch(var, usagePos, scopeRoot)) {
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

    private static ResolvedType resolveLocalDeclaredType(CompilationUnit cu,
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
        ResolvedType best = null;
        long bestKey = Long.MIN_VALUE;
        List<Parameter> params = scopeRoot.findAll(Parameter.class);
        for (Parameter param : params) {
            if (!name.equals(param.getNameAsString())) {
                continue;
            }
            if (isInsideExcludedScope(param, scopeRoot)) {
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
                ResolvedType type = resolveTypeFromAstType(cu, param.getType());
                if (type != null) {
                    best = type;
                    bestKey = key;
                }
            }
        }
        List<VariableDeclarator> vars = scopeRoot.findAll(VariableDeclarator.class);
        for (VariableDeclarator var : vars) {
            if (!name.equals(var.getNameAsString())) {
                continue;
            }
            if (isInsideExcludedScope(var, scopeRoot)) {
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
                ResolvedType type = resolveTypeFromAstType(cu, var.getType());
                if (type != null) {
                    best = type;
                    bestKey = key;
                }
            }
        }
        return best;
    }

    private static ResolvedType resolveLocalAssignedType(CompilationUnit cu,
                                                         String name,
                                                         Position usagePos,
                                                         Node context,
                                                         CallerContext ctx) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        Node scopeRoot = findScopeRoot(context);
        if (scopeRoot == null) {
            return null;
        }
        long usageKey = usagePos == null ? Long.MAX_VALUE : positionKey(usagePos);
        ResolvedType best = null;
        long bestKey = Long.MIN_VALUE;
        List<VariableDeclarator> vars = scopeRoot.findAll(VariableDeclarator.class);
        for (VariableDeclarator var : vars) {
            if (!name.equals(var.getNameAsString())) {
                continue;
            }
            if (isInsideExcludedScope(var, scopeRoot)) {
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
            ResolvedType declared = resolveTypeFromAstType(cu, var.getType());
            if (declared != null) {
                continue;
            }
            if (!var.getInitializer().isPresent()) {
                continue;
            }
            Expression init = var.getInitializer().get();
            if (init instanceof NameExpr && name.equals(((NameExpr) init).getNameAsString())) {
                continue;
            }
            ResolvedType inferred = resolveExpressionType(cu, init, ctx);
            if (inferred != null && key > bestKey) {
                best = inferred;
                bestKey = key;
            }
        }
        List<AssignExpr> assigns = scopeRoot.findAll(AssignExpr.class);
        for (AssignExpr assign : assigns) {
            if (assign == null) {
                continue;
            }
            if (isInsideExcludedScope(assign, scopeRoot)) {
                continue;
            }
            if (!isInSameBranch(assign, usagePos, scopeRoot)) {
                continue;
            }
            Expression target = unwrapEnclosed(assign.getTarget());
            if (!(target instanceof NameExpr)) {
                continue;
            }
            String varName = ((NameExpr) target).getNameAsString();
            if (!name.equals(varName)) {
                continue;
            }
            Position pos = assign.getBegin().orElse(null);
            if (pos == null) {
                continue;
            }
            long key = positionKey(pos);
            if (key > usageKey) {
                continue;
            }
            Expression value = assign.getValue();
            if (value instanceof NameExpr && name.equals(((NameExpr) value).getNameAsString())) {
                continue;
            }
            ResolvedType inferred = resolveExpressionType(cu, value, ctx);
            if (inferred != null && key > bestKey) {
                best = inferred;
                bestKey = key;
            }
        }
        return best;
    }

    private static ResolvedType resolveNarrowedTypeFromConditions(CompilationUnit cu,
                                                                  String name,
                                                                  Node context) {
        if (name == null || name.trim().isEmpty() || context == null) {
            return null;
        }
        Node cur = context;
        while (cur != null) {
            Node parent = cur.getParentNode().orElse(null);
            if (parent instanceof com.github.javaparser.ast.stmt.IfStmt) {
                com.github.javaparser.ast.stmt.IfStmt ifs =
                        (com.github.javaparser.ast.stmt.IfStmt) parent;
                Node thenStmt = ifs.getThenStmt();
                Node elseStmt = ifs.getElseStmt().orElse(null);
                boolean inThen = isWithinNode(thenStmt, context);
                boolean inElse = elseStmt != null && isWithinNode(elseStmt, context);
                if (inThen) {
                    ResolvedType narrowed = extractInstanceofType(cu, ifs.getCondition(), name, true);
                    if (narrowed != null) {
                        return narrowed;
                    }
                } else if (inElse) {
                    ResolvedType narrowed = extractInstanceofType(cu, ifs.getCondition(), name, false);
                    if (narrowed != null) {
                        return narrowed;
                    }
                }
            }
            cur = parent;
        }
        return null;
    }

    private static ResolvedType extractInstanceofType(CompilationUnit cu,
                                                      Expression condition,
                                                      String name,
                                                      boolean positive) {
        if (condition == null) {
            return null;
        }
        Expression expr = unwrapEnclosed(condition);
        if (expr instanceof UnaryExpr) {
            UnaryExpr unary = (UnaryExpr) expr;
            if (unary.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
                return extractInstanceofType(cu, unary.getExpression(), name, !positive);
            }
        }
        if (expr instanceof InstanceOfExpr) {
            InstanceOfExpr inst = (InstanceOfExpr) expr;
            if (!positive) {
                return null;
            }
            Expression left = unwrapEnclosed(inst.getExpression());
            if (left instanceof NameExpr
                    && name.equals(((NameExpr) left).getNameAsString())) {
                return resolveTypeFromAstType(cu, inst.getType());
            }
            return null;
        }
        if (expr instanceof BinaryExpr) {
            BinaryExpr binary = (BinaryExpr) expr;
            if (binary.getOperator() == BinaryExpr.Operator.AND) {
                ResolvedType left = extractInstanceofType(cu, binary.getLeft(), name, positive);
                if (left != null) {
                    return left;
                }
                return extractInstanceofType(cu, binary.getRight(), name, positive);
            }
            if (binary.getOperator() == BinaryExpr.Operator.OR) {
                return null;
            }
        }
        return null;
    }

    private static ResolvedType resolveFieldTypeInAst(CompilationUnit cu,
                                                      String name,
                                                      Node context) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        Node typeNode = findEnclosingType(context);
        if (typeNode == null) {
            return null;
        }
        if (typeNode instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration decl = (ClassOrInterfaceDeclaration) typeNode;
            return findFieldTypeInAst(decl.getFields(), cu, name);
        }
        if (typeNode instanceof EnumDeclaration) {
            EnumDeclaration decl = (EnumDeclaration) typeNode;
            return findFieldTypeInAst(decl.getFields(), cu, name);
        }
        if (typeNode instanceof RecordDeclaration) {
            RecordDeclaration decl = (RecordDeclaration) typeNode;
            return findFieldTypeInAst(decl.getFields(), cu, name);
        }
        return null;
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
        String className = resolveEnclosingClassName(cu, context);
        ResolvedType dbType = resolveFieldTypeFromDb(className, name);
        if (dbType != null && dbType.internalName != null) {
            return new DeclaredSymbol(SymbolKind.FIELD, name, null, dbType.internalName);
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

    private static ResolvedType findFieldTypeInAst(List<com.github.javaparser.ast.body.FieldDeclaration> fields,
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
                return resolveTypeFromAstType(cu, var.getType());
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

    private static boolean isInsideExcludedScope(Node node, Node scopeRoot) {
        Node cur = node;
        while (cur != null && cur != scopeRoot) {
            if (cur instanceof LambdaExpr) {
                return true;
            }
            if (cur instanceof ClassOrInterfaceDeclaration
                    || cur instanceof EnumDeclaration
                    || cur instanceof RecordDeclaration
                    || cur instanceof AnnotationDeclaration) {
                return true;
            }
            cur = cur.getParentNode().orElse(null);
        }
        return false;
    }

    private static boolean isInSameBranch(Node candidate, Position usagePos, Node scopeRoot) {
        if (usagePos == null) {
            return true;
        }
        Node cur = candidate;
        while (cur != null && cur != scopeRoot) {
            Node parent = cur.getParentNode().orElse(null);
            if (parent instanceof com.github.javaparser.ast.stmt.IfStmt) {
                com.github.javaparser.ast.stmt.IfStmt ifs = (com.github.javaparser.ast.stmt.IfStmt) parent;
                Node thenStmt = ifs.getThenStmt();
                Node elseStmt = ifs.getElseStmt().orElse(null);
                boolean candidateInThen = isWithinNode(thenStmt, candidate);
                boolean candidateInElse = elseStmt != null && isWithinNode(elseStmt, candidate);
                boolean usageInThen = isWithinNode(thenStmt, usagePos);
                boolean usageInElse = elseStmt != null && isWithinNode(elseStmt, usagePos);
                if (candidateInThen && !usageInThen) {
                    return false;
                }
                if (candidateInElse && !usageInElse) {
                    return false;
                }
            }
            if (parent instanceof com.github.javaparser.ast.stmt.SwitchEntry) {
                com.github.javaparser.ast.stmt.SwitchEntry entry =
                        (com.github.javaparser.ast.stmt.SwitchEntry) parent;
                boolean candidateInEntry = isWithinNode(entry, candidate);
                boolean usageInEntry = isWithinNode(entry, usagePos);
                if (candidateInEntry && !usageInEntry) {
                    return false;
                }
            }
            cur = parent;
        }
        return true;
    }

    private static boolean isWithinNode(Node node, Node target) {
        if (node == null || target == null) {
            return false;
        }
        Range r1 = node.getRange().orElse(null);
        Range r2 = target.getRange().orElse(null);
        if (r1 == null || r2 == null) {
            return false;
        }
        return contains(r1, r2.begin);
    }

    private static boolean isWithinNode(Node node, Position pos) {
        if (node == null || pos == null) {
            return false;
        }
        Range range = node.getRange().orElse(null);
        return range != null && contains(range, pos);
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

    private static SymbolKind resolveClassKind(String className, SymbolKind fallback) {
        if (className == null || className.trim().isEmpty()) {
            return fallback;
        }
        if (MainForm.getEngine() == null) {
            return fallback;
        }
        String normalized = normalizeClassName(className);
        ClassResult result = MainForm.getEngine().getClassByClass(normalized);
        if (result != null && result.getIsInterfaceInt() == 1) {
            return SymbolKind.INTERFACE;
        }
        return fallback;
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

    private static String resolveStaticImportOwner(CompilationUnit cu, String memberName) {
        if (cu == null || memberName == null || memberName.trim().isEmpty()) {
            return null;
        }
        List<com.github.javaparser.ast.ImportDeclaration> imports = cu.getImports();
        if (imports == null || imports.isEmpty()) {
            return null;
        }
        String fallback = null;
        for (com.github.javaparser.ast.ImportDeclaration imp : imports) {
            if (imp == null || !imp.isStatic()) {
                continue;
            }
            String q = imp.getNameAsString();
            if (q == null || q.trim().isEmpty()) {
                continue;
            }
            if (imp.isAsterisk()) {
                String owner = q.replace('.', '/');
                if (MainForm.getEngine() != null) {
                    try {
                        if (MainForm.getEngine().getMemberByClassAndName(owner, memberName) != null) {
                            return owner;
                        }
                        List<MethodResult> methods = MainForm.getEngine().getMethod(owner, memberName, null);
                        if (methods != null && !methods.isEmpty()) {
                            return owner;
                        }
                    } catch (Exception ignored) {
                    }
                }
                if (fallback == null) {
                    fallback = owner;
                }
                continue;
            }
            int lastDot = q.lastIndexOf('.');
            if (lastDot <= 0) {
                continue;
            }
            String member = q.substring(lastDot + 1);
            if (!memberName.equals(member)) {
                continue;
            }
            String owner = q.substring(0, lastDot);
            return owner.replace('.', '/');
        }
        return fallback;
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

    private static boolean isSubtype(String child, String parent) {
        if (child == null || parent == null) {
            return false;
        }
        if (child.equals(parent)) {
            return true;
        }
        if ("java/lang/Object".equals(parent)) {
            return true;
        }
        String key = child + "->" + parent;
        Boolean cached = SUBTYPE_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        boolean result = isSubtypeInternal(child, parent);
        SUBTYPE_CACHE.put(key, result);
        return result;
    }

    private static boolean isSubtypeInternal(String child, String parent) {
        if (MainForm.getEngine() == null) {
            return false;
        }
        if (child == null || parent == null) {
            return false;
        }
        if (child.equals(parent)) {
            return true;
        }
        int depthLimit = 20;
        List<String> queue = new ArrayList<>();
        queue.add(child);
        int idx = 0;
        while (idx < queue.size() && depthLimit-- > 0) {
            String cur = queue.get(idx++);
            if (cur == null || cur.trim().isEmpty()) {
                continue;
            }
            if (parent.equals(cur)) {
                return true;
            }
            ClassResult result = MainForm.getEngine().getClassByClass(cur);
            if (result != null && result.getSuperClassName() != null
                    && !result.getSuperClassName().trim().isEmpty()) {
                String sup = result.getSuperClassName();
                if (!queue.contains(sup)) {
                    queue.add(sup);
                }
            }
            List<String> interfaces = MainForm.getEngine().getInterfacesByClass(cur);
            if (interfaces != null) {
                for (String itf : interfaces) {
                    if (itf == null || itf.trim().isEmpty()) {
                        continue;
                    }
                    if (!queue.contains(itf)) {
                        queue.add(itf);
                    }
                }
            }
        }
        return false;
    }

    private static int inheritanceDistance(String child, String parent) {
        if (child == null || parent == null) {
            return Integer.MAX_VALUE;
        }
        if (child.equals(parent)) {
            return 0;
        }
        String key = child + "=>" + parent;
        Integer cached = DISTANCE_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        int distance = inheritanceDistanceInternal(child, parent);
        DISTANCE_CACHE.put(key, distance);
        return distance;
    }

    private static int inheritanceDistanceInternal(String child, String parent) {
        if (MainForm.getEngine() == null) {
            return Integer.MAX_VALUE;
        }
        List<String> queue = new ArrayList<>();
        List<Integer> depth = new ArrayList<>();
        queue.add(child);
        depth.add(0);
        int idx = 0;
        while (idx < queue.size()) {
            String cur = queue.get(idx);
            int curDepth = depth.get(idx);
            idx++;
            if (cur == null) {
                continue;
            }
            if (cur.equals(parent)) {
                return curDepth;
            }
            if (curDepth > 20) {
                continue;
            }
            ClassResult result = MainForm.getEngine().getClassByClass(cur);
            if (result != null && result.getSuperClassName() != null
                    && !result.getSuperClassName().trim().isEmpty()) {
                String sup = result.getSuperClassName();
                if (!queue.contains(sup)) {
                    queue.add(sup);
                    depth.add(curDepth + 1);
                }
            }
            List<String> interfaces = MainForm.getEngine().getInterfacesByClass(cur);
            if (interfaces != null) {
                for (String itf : interfaces) {
                    if (itf == null || itf.trim().isEmpty()) {
                        continue;
                    }
                    if (!queue.contains(itf)) {
                        queue.add(itf);
                        depth.add(curDepth + 1);
                    }
                }
            }
        }
        return Integer.MAX_VALUE;
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

    public static boolean openClassInEditor(String className,
                                            String methodName,
                                            String methodDesc,
                                            boolean preferExisting,
                                            boolean warnOnMissing,
                                            boolean openInNewTab,
                                            boolean recordState) {
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
            boolean sameClass = false;
            if (codeTabs != null) {
                Component selected = codeTabs.getSelectedComponent();
                String tabClass = getTabClass(selected);
                if (tabClass != null && normalizeClassName(tabClass).equals(normalized)) {
                    sameClass = true;
                }
            } else {
                String curClass = MainForm.getCurClass();
                if (curClass != null && normalizeClassName(curClass).equals(normalized)) {
                    sameClass = true;
                }
            }
            if (sameClass) {
                String text = targetArea.getText();
                if (text != null && !text.trim().isEmpty() && looksLikeJava(text)) {
                    existingCode = text;
                }
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
        CFRDecompileEngine.CfrDecompileResult cfrMapping = null;
        DecompileEngine.FernDecompileResult fernMapping = null;
        boolean decompiledFresh = false;
        if (code == null) {
            decompiledFresh = true;
            if (DecompileSelector.shouldUseCfr()) {
                cfrMapping = CFRDecompileEngine.decompileWithLineMapping(classPath);
                if (cfrMapping != null) {
                    code = cfrMapping.getCode();
                }
                if (code == null) {
                    code = DecompileSelector.decompile(Paths.get(classPath));
                }
            } else {
                fernMapping = DecompileEngine.decompileWithLineMapping(Paths.get(classPath));
                if (fernMapping != null) {
                    code = fernMapping.getCode();
                }
                if (code == null) {
                    code = DecompileSelector.decompile(Paths.get(classPath));
                }
            }
        }
        if (decompiledFresh) {
            List<SimpleLineMapping> mappings = null;
            String usedDecompiler = null;
            if (cfrMapping != null && cfrMapping.getLineMappings() != null
                    && !cfrMapping.getLineMappings().isEmpty()) {
                mappings = toSimpleLineMappingsFromCfr(cfrMapping.getLineMappings());
                cacheLineMappings(normalized, mappings);
                usedDecompiler = DECOMPILER_CFR;
            } else if (fernMapping != null && fernMapping.getLineMappings() != null
                    && !fernMapping.getLineMappings().isEmpty()) {
                mappings = toSimpleLineMappingsFromFern(fernMapping.getLineMappings());
                cacheLineMappings(normalized, mappings);
                usedDecompiler = DECOMPILER_FERN;
            } else {
                clearLineMappings(normalized);
            }
            if (mappings != null && usedDecompiler != null) {
                saveLineMappingsToDb(normalized, usedDecompiler, mappings);
            } else {
                ensureLineMappingsLoaded(normalized, code);
            }
        } else {
            ensureLineMappingsLoaded(normalized, code);
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
        String jarName = MainForm.getEngine().getJarByClass(finalClassName);
        if (jarName == null || jarName.trim().isEmpty()) {
            jarName = RuntimeClassResolver.getJarName(finalClassName);
        }
        String finalJarName = jarName == null ? "" : jarName;
        SyntaxAreaHelper.runOnEdt(() -> {
            setActiveCodeArea(finalArea);
            SearchInputListener.getFileTree().searchPathTarget(finalClassName);
            finalArea.setText(finalCode);
            finalArea.setCaretPosition(finalCaret);
            MainForm.setCurClass(finalClassName);
            MainForm.getInstance().getCurClassText().setText(finalClassName);
            if (!finalJarName.trim().isEmpty()) {
                MainForm.getInstance().getCurJarText().setText(finalJarName);
            }
            if (methodName != null && !methodName.trim().isEmpty()) {
                MainForm.getInstance().getCurMethodText().setText(methodName);
            }
        });
        if (recordState) {
            pushStateIfNeeded(normalized, methodName, methodDesc, classPath);
        } else {
            setCurMethodOnly(normalized, methodName, methodDesc, classPath, finalJarName);
        }
        return true;
    }

    private static boolean openClassInEditor(String className,
                                             String methodName,
                                             String methodDesc,
                                             boolean preferExisting,
                                             boolean warnOnMissing,
                                             boolean openInNewTab) {
        return openClassInEditor(className, methodName, methodDesc, preferExisting, warnOnMissing,
                openInNewTab, true);
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

    private static void ensureLineMappingsLoaded(String className, String codeSnapshot) {
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
        loadLineMappingsFromDb(normalized, decompiler);
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

    private static boolean loadLineMappingsFromDb(String className, String decompiler) {
        if (className == null || className.trim().isEmpty()) {
            return false;
        }
        if (MainForm.getEngine() == null) {
            return false;
        }
        Integer jarId = resolveJarIdForClass(className);
        List<LineMappingEntity> rows = MainForm.getEngine().getLineMappings(className, jarId, decompiler);
        if ((rows == null || rows.isEmpty()) && jarId != null) {
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
        Integer jarId = resolveJarIdForClass(className);
        Integer jarIdValue = jarId == null ? -1 : jarId;
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
        if (line > 0) {
            for (MethodLineMapping candidate : candidates) {
                if (line >= candidate.minLine && line <= candidate.maxLine) {
                    long span = (long) candidate.maxLine - candidate.minLine;
                    if (span < bestSpan) {
                        bestSpan = span;
                        best = candidate;
                    }
                } else {
                    int dist = Math.min(Math.abs(line - candidate.minLine), Math.abs(line - candidate.maxLine));
                    if (best == null && dist < bestDistance) {
                        bestDistance = dist;
                        best = candidate;
                    }
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

    private static CallerContext resolveCallerContext(CompilationUnit cu,
                                                      Position pos,
                                                      String curClassSnapshot,
                                                      String codeSnapshot) {
        String className = null;
        String methodName = null;
        String methodDesc = null;
        int methodArgCount = ARG_COUNT_UNKNOWN;
        EnclosingCallable enclosing = resolveEnclosingCallable(cu, pos);
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
            ensureLineMappingsLoaded(className, codeSnapshot);
        }
        int line = pos == null ? -1 : pos.line;
        int mappedLine = mapLineNumber(className, methodName, methodDesc, methodArgCount, line);
        return new CallerContext(cu, pos, mappedLine, className, methodName, methodDesc, methodArgCount);
    }

    private static EnclosingCallable resolveEnclosingCallable(CompilationUnit cu, Position pos) {
        if (cu == null || pos == null) {
            return null;
        }
        EnclosingCallable best = null;
        long bestSpan = Long.MAX_VALUE;
        List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
        for (MethodDeclaration decl : methods) {
            Range range = decl.getRange().orElse(null);
            if (range == null || !contains(range, pos)) {
                continue;
            }
            long span = rangeSpan(range);
            if (span < bestSpan) {
                String className = resolveEnclosingClassName(cu, decl);
                int argCount = decl.getParameters() == null ? ARG_COUNT_UNKNOWN : decl.getParameters().size();
                best = new EnclosingCallable(className, decl.getNameAsString(), argCount);
                bestSpan = span;
            }
        }
        List<ConstructorDeclaration> constructors = cu.findAll(ConstructorDeclaration.class);
        for (ConstructorDeclaration decl : constructors) {
            Range range = decl.getRange().orElse(null);
            if (range == null || !contains(range, pos)) {
                continue;
            }
            long span = rangeSpan(range);
            if (span < bestSpan) {
                String className = resolveEnclosingClassName(cu, decl);
                int argCount = decl.getParameters() == null ? ARG_COUNT_UNKNOWN : decl.getParameters().size();
                best = new EnclosingCallable(className, "<init>", argCount);
                bestSpan = span;
            }
        }
        List<InitializerDeclaration> inits = cu.findAll(InitializerDeclaration.class);
        for (InitializerDeclaration decl : inits) {
            Range range = decl.getRange().orElse(null);
            if (range == null || !contains(range, pos)) {
                continue;
            }
            long span = rangeSpan(range);
            if (span < bestSpan) {
                String className = resolveEnclosingClassName(cu, decl);
                String methodName = decl.isStatic() ? "<clinit>" : "<init>";
                best = new EnclosingCallable(className, methodName, ARG_COUNT_UNKNOWN);
                bestSpan = span;
            }
        }
        return best;
    }

    private static long rangeSpan(Range range) {
        if (range == null) {
            return Long.MAX_VALUE;
        }
        long lines = Math.max(0, range.end.line - range.begin.line);
        long cols = Math.max(0, range.end.column - range.begin.column);
        return lines * 10000L + cols;
    }

    private static String resolveMethodDescByArgCount(String className, String methodName, int argCount) {
        if (MainForm.getEngine() == null) {
            return null;
        }
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        if (methodName == null || methodName.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeClassName(className);
        List<MethodResult> candidates = MainForm.getEngine().getMethod(normalized, methodName, null);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        List<MethodResult> filtered = filterByArgCount(candidates, argCount);
        List<MethodResult> pool = (filtered != null && !filtered.isEmpty())
                ? filtered
                : candidates;
        if (pool.size() == 1) {
            return pool.get(0).getMethodDesc();
        }
        return null;
    }

    private static String resolveMethodDescByArgTypes(String className,
                                                      String methodName,
                                                      List<ResolvedType> argTypes,
                                                      ResolvedType scopeType,
                                                      boolean preferStatic) {
        if (MainForm.getEngine() == null) {
            return null;
        }
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        if (methodName == null || methodName.trim().isEmpty()) {
            return null;
        }
        List<MethodResult> candidates = MainForm.getEngine().getMethod(
                normalizeClassName(className), methodName, null);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        List<ResolvedType> args = argTypes == null ? new ArrayList<>() : argTypes;
        int argCount = args.size();
        MethodResult best = null;
        int bestScore = Integer.MIN_VALUE;
        int bestUnknown = Integer.MAX_VALUE;
        boolean bestVarargs = true;
        for (MethodResult candidate : candidates) {
            if (candidate == null || candidate.getMethodDesc() == null) {
                continue;
            }
            Type[] params;
            try {
                params = Type.getArgumentTypes(candidate.getMethodDesc());
            } catch (Exception ex) {
                continue;
            }
            boolean varargs = (candidate.getAccessInt() & Opcodes.ACC_VARARGS) != 0;
            if (!varargs && params.length != argCount) {
                continue;
            }
            if (varargs && argCount < Math.max(0, params.length - 1)) {
                continue;
            }
            List<ParamType> paramTypes = resolveCandidateParamTypes(
                    normalizeClassName(className), candidate, scopeType, params);
            int score = 0;
            int unknown = 0;
            boolean mismatch = false;
            if (!varargs) {
                for (int i = 0; i < params.length; i++) {
                    MatchScore s = scoreParamMatch(args.get(i), paramTypes.get(i));
                    if (s.incompatible) {
                        mismatch = true;
                        break;
                    }
                    score += s.score;
                    unknown += s.unknown ? 1 : 0;
                }
            } else {
                int fixedCount = Math.max(0, params.length - 1);
                for (int i = 0; i < fixedCount; i++) {
                    MatchScore s = scoreParamMatch(args.get(i), paramTypes.get(i));
                    if (s.incompatible) {
                        mismatch = true;
                        break;
                    }
                    score += s.score;
                    unknown += s.unknown ? 1 : 0;
                }
                if (!mismatch) {
                    Type varType = params.length == 0 ? null : params[params.length - 1];
                    ParamType varParam = paramTypes.isEmpty() ? null : paramTypes.get(paramTypes.size() - 1);
                    Type elemType = varType != null && varType.getSort() == Type.ARRAY
                            ? varType.getElementType()
                            : varType;
                    ParamType elemParam = varParam == null
                            ? null
                            : new ParamType(elemType, varParam.resolved);
                    for (int i = fixedCount; i < argCount; i++) {
                        MatchScore s = scoreParamMatch(args.get(i), elemParam);
                        if (s.incompatible) {
                            mismatch = true;
                            break;
                        }
                        score += Math.max(1, s.score - 2);
                        unknown += s.unknown ? 1 : 0;
                    }
                    score -= 2; // varargs penalty
                }
            }
            if (mismatch) {
                continue;
            }
            if (preferStatic) {
                score += candidate.getIsStaticInt() == 1 ? 2 : -1;
            } else {
                score += candidate.getIsStaticInt() == 0 ? 1 : 0;
            }
            if (score > bestScore
                    || (score == bestScore && unknown < bestUnknown)
                    || (score == bestScore && unknown == bestUnknown && !varargs && bestVarargs)) {
                best = candidate;
                bestScore = score;
                bestUnknown = unknown;
                bestVarargs = varargs;
            }
        }
        if (best == null) {
            return null;
        }
        if (bestScore <= 0) {
            return null;
        }
        return best.getMethodDesc();
    }

    private static List<ParamType> resolveCandidateParamTypes(String className,
                                                              MethodResult candidate,
                                                              ResolvedType scopeType,
                                                              Type[] descParams) {
        List<ParamType> out = new ArrayList<>();
        List<ResolvedType> resolvedParams = null;
        if (candidate != null && candidate.getMethodDesc() != null) {
            ClassSignatureCache cache = getClassSignatureCache(className);
            ResolvedType bindingScope = scopeType;
            if (bindingScope != null && bindingScope.internalName != null
                    && !bindingScope.internalName.equals(className)) {
                bindingScope = null;
            }
            if (cache != null && cache.methodSignatures != null) {
                String sig = cache.methodSignatures.get(
                        candidate.getMethodName() + candidate.getMethodDesc());
                if (sig != null && !sig.trim().isEmpty()) {
                    List<GenericType> params = parseMethodParamSignatures(sig);
                    if (params != null && !params.isEmpty()) {
                        Map<String, String> bindings = buildGenericBindings(cache, bindingScope);
                        List<ResolvedType> temp = new ArrayList<>();
                        for (GenericType gt : params) {
                            temp.add(resolveGenericType(gt, bindings));
                        }
                        resolvedParams = temp;
                    }
                }
            }
        }
        for (int i = 0; i < descParams.length; i++) {
            ResolvedType resolved = null;
            if (resolvedParams != null && i < resolvedParams.size()) {
                resolved = resolvedParams.get(i);
            }
            out.add(new ParamType(descParams[i], resolved));
        }
        return out;
    }

    private static List<ResolvedType> resolveArgumentTypes(CompilationUnit cu,
                                                           List<Expression> args,
                                                           CallerContext ctx) {
        if (args == null || args.isEmpty()) {
            return new ArrayList<>();
        }
        List<ResolvedType> out = new ArrayList<>();
        for (Expression arg : args) {
            if (arg == null) {
                out.add(null);
                continue;
            }
            out.add(resolveExpressionType(cu, arg, ctx));
        }
        return out;
    }

    private static List<ResolvedType> resolveMethodReferenceParamTypes(CompilationUnit cu,
                                                                       MethodReferenceExpr ref,
                                                                       CallerContext ctx) {
        ResolvedType fiType = resolveFunctionalInterfaceTypeFromContext(cu, ref, ctx);
        if (fiType == null || fiType.internalName == null) {
            return null;
        }
        MethodResult sam = resolveSamMethod(fiType.internalName);
        if (sam == null || sam.getMethodDesc() == null) {
            return null;
        }
        int argCount = argCountFromDesc(sam.getMethodDesc());
        List<ResolvedType> params = new ArrayList<>();
        for (int i = 0; i < argCount; i++) {
            ResolvedType param = resolveSamParamType(fiType, sam, i);
            if (param == null) {
                try {
                    Type[] args = Type.getArgumentTypes(sam.getMethodDesc());
                    if (i < args.length) {
                        Type t = args[i];
                        if (t.getSort() >= Type.BOOLEAN && t.getSort() <= Type.DOUBLE) {
                            param = new ResolvedType(t);
                        } else if (t.getSort() == Type.OBJECT) {
                            param = new ResolvedType(t.getInternalName(), null);
                        } else if (t.getSort() == Type.ARRAY) {
                            param = new ResolvedType(t);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            params.add(param);
        }
        String identifier = ref.getIdentifier();
        if (identifier == null) {
            return params;
        }
        if ("new".equals(identifier)) {
            return params;
        }
        Expression scope = ref.getScope();
        if (scope instanceof TypeExpr) {
            String owner = resolveClassNameFromExpression(cu, scope, ref, ctx);
            if (owner != null) {
                List<MethodResult> methods = MainForm.getEngine() == null
                        ? null
                        : MainForm.getEngine().getMethod(owner, identifier, null);
                if (methods != null && !methods.isEmpty() && !params.isEmpty()) {
                    boolean hasInstance = false;
                    boolean hasStaticExact = false;
                    int instanceArgCount = Math.max(0, params.size() - 1);
                    for (MethodResult method : methods) {
                        if (method == null || method.getMethodDesc() == null) {
                            continue;
                        }
                        int count = argCountFromDesc(method.getMethodDesc());
                        if (method.getIsStaticInt() == 1 && count == params.size()) {
                            hasStaticExact = true;
                        }
                        if (method.getIsStaticInt() == 0 && count == instanceArgCount) {
                            hasInstance = true;
                        }
                    }
                    if (hasInstance && !hasStaticExact) {
                        params = new ArrayList<>(params.subList(1, params.size()));
                    }
                }
            }
        }
        return params;
    }

    private static final class ParamType {
        private final Type asmType;
        private final ResolvedType resolved;

        private ParamType(Type asmType, ResolvedType resolved) {
            this.asmType = asmType;
            this.resolved = resolved;
        }
    }

    private static final class MatchScore {
        private final int score;
        private final boolean incompatible;
        private final boolean unknown;

        private MatchScore(int score, boolean incompatible, boolean unknown) {
            this.score = score;
            this.incompatible = incompatible;
            this.unknown = unknown;
        }
    }

    private static MatchScore scoreParamMatch(ResolvedType arg, ParamType param) {
        if (param == null || param.asmType == null) {
            return new MatchScore(0, false, true);
        }
        if (arg == null) {
            return new MatchScore(0, false, true);
        }
        Type paramType = param.asmType;
        if (arg.isPrimitive()) {
            if (paramType.getSort() >= Type.BOOLEAN && paramType.getSort() <= Type.DOUBLE) {
                int score = primitiveMatchScore(arg.asmType, paramType);
                return score <= Integer.MIN_VALUE / 2
                        ? new MatchScore(score, true, false)
                        : new MatchScore(score, false, false);
            }
            if (paramType.getSort() == Type.OBJECT) {
                String wrapper = wrapperForPrimitive(arg.asmType.getSort());
                if (wrapper != null && wrapper.equals(paramType.getInternalName())) {
                    return new MatchScore(8, false, false);
                }
                if ("java/lang/Object".equals(paramType.getInternalName())) {
                    return new MatchScore(2, false, false);
                }
                return new MatchScore(Integer.MIN_VALUE / 2, true, false);
            }
            return new MatchScore(Integer.MIN_VALUE / 2, true, false);
        }
        if (paramType.getSort() >= Type.BOOLEAN && paramType.getSort() <= Type.DOUBLE) {
            String internal = arg.internalName;
            Integer sort = primitiveForWrapper(internal);
            if (sort != null && sort == paramType.getSort()) {
                return new MatchScore(7, false, false);
            }
            return new MatchScore(Integer.MIN_VALUE / 2, true, false);
        }
        if (paramType.getSort() == Type.ARRAY) {
            if (arg.isArray()) {
                return new MatchScore(6, false, false);
            }
            if (arg.internalName != null && "java/lang/Object".equals(paramType.getInternalName())) {
                return new MatchScore(2, false, false);
            }
            return new MatchScore(Integer.MIN_VALUE / 2, true, false);
        }
        String target = param.resolved != null && param.resolved.internalName != null
                ? param.resolved.internalName
                : paramType.getInternalName();
        if (arg.internalName == null) {
            return new MatchScore(0, false, true);
        }
        if (arg.internalName.equals(target)) {
            return new MatchScore(10, false, false);
        }
        if (isSubtype(arg.internalName, target)) {
            int distance = inheritanceDistance(arg.internalName, target);
            int bonus = Math.max(1, 5 - Math.min(4, distance));
            return new MatchScore(7 + bonus, false, false);
        }
        if ("java/lang/Object".equals(target)) {
            return new MatchScore(2, false, false);
        }
        return new MatchScore(Integer.MIN_VALUE / 2, true, false);
    }

    private static int primitiveMatchScore(Type argType, Type paramType) {
        if (argType == null || paramType == null) {
            return Integer.MIN_VALUE / 2;
        }
        if (argType.getSort() == paramType.getSort()) {
            return 10;
        }
        if (isWideningPrimitive(argType.getSort(), paramType.getSort())) {
            return 6;
        }
        return Integer.MIN_VALUE / 2;
    }

    private static Type widerPrimitiveType(Type left, Type right) {
        if (left == null || right == null) {
            return null;
        }
        int l = left.getSort();
        int r = right.getSort();
        int rank = Math.max(primitiveRank(l), primitiveRank(r));
        switch (rank) {
            case 6:
                return Type.DOUBLE_TYPE;
            case 5:
                return Type.FLOAT_TYPE;
            case 4:
                return Type.LONG_TYPE;
            case 3:
                return Type.INT_TYPE;
            case 2:
                return Type.CHAR_TYPE;
            case 1:
                return Type.SHORT_TYPE;
            case 0:
                return Type.BYTE_TYPE;
            default:
                return null;
        }
    }

    private static int primitiveRank(int sort) {
        switch (sort) {
            case Type.DOUBLE:
                return 6;
            case Type.FLOAT:
                return 5;
            case Type.LONG:
                return 4;
            case Type.INT:
                return 3;
            case Type.CHAR:
                return 2;
            case Type.SHORT:
                return 1;
            case Type.BYTE:
                return 0;
            default:
                return -1;
        }
    }

    private static boolean isWideningPrimitive(int from, int to) {
        if (from == to) {
            return true;
        }
        switch (from) {
            case Type.BYTE:
                return to == Type.SHORT || to == Type.INT || to == Type.LONG || to == Type.FLOAT || to == Type.DOUBLE;
            case Type.SHORT:
                return to == Type.INT || to == Type.LONG || to == Type.FLOAT || to == Type.DOUBLE;
            case Type.CHAR:
                return to == Type.INT || to == Type.LONG || to == Type.FLOAT || to == Type.DOUBLE;
            case Type.INT:
                return to == Type.LONG || to == Type.FLOAT || to == Type.DOUBLE;
            case Type.LONG:
                return to == Type.FLOAT || to == Type.DOUBLE;
            case Type.FLOAT:
                return to == Type.DOUBLE;
            default:
                return false;
        }
    }

    private static String wrapperForPrimitive(int sort) {
        switch (sort) {
            case Type.BOOLEAN:
                return "java/lang/Boolean";
            case Type.BYTE:
                return "java/lang/Byte";
            case Type.CHAR:
                return "java/lang/Character";
            case Type.SHORT:
                return "java/lang/Short";
            case Type.INT:
                return "java/lang/Integer";
            case Type.LONG:
                return "java/lang/Long";
            case Type.FLOAT:
                return "java/lang/Float";
            case Type.DOUBLE:
                return "java/lang/Double";
            default:
                return null;
        }
    }

    private static Integer primitiveForWrapper(String internalName) {
        if (internalName == null) {
            return null;
        }
        switch (internalName) {
            case "java/lang/Boolean":
                return Type.BOOLEAN;
            case "java/lang/Byte":
                return Type.BYTE;
            case "java/lang/Character":
                return Type.CHAR;
            case "java/lang/Short":
                return Type.SHORT;
            case "java/lang/Integer":
                return Type.INT;
            case "java/lang/Long":
                return Type.LONG;
            case "java/lang/Float":
                return Type.FLOAT;
            case "java/lang/Double":
                return Type.DOUBLE;
            default:
                return null;
        }
    }

    private static MethodCallExpr findMethodCallAt(CompilationUnit cu, Position pos) {
        if (cu == null || pos == null) {
            return null;
        }
        List<MethodCallExpr> calls = cu.findAll(MethodCallExpr.class);
        MethodCallExpr best = null;
        long bestSpan = Long.MAX_VALUE;
        for (MethodCallExpr call : calls) {
            if (!call.getName().getRange().isPresent()) {
                continue;
            }
            Range range = call.getName().getRange().get();
            if (!contains(range, pos)) {
                continue;
            }
            long span = rangeSpan(range);
            if (span < bestSpan) {
                best = call;
                bestSpan = span;
            }
        }
        return best;
    }

    private static MethodReferenceExpr findMethodReferenceAt(CompilationUnit cu, Position pos) {
        if (cu == null || pos == null) {
            return null;
        }
        List<MethodReferenceExpr> refs = cu.findAll(MethodReferenceExpr.class);
        MethodReferenceExpr best = null;
        long bestSpan = Long.MAX_VALUE;
        for (MethodReferenceExpr ref : refs) {
            Range range = ref.getRange().orElse(null);
            if (range == null || !contains(range, pos)) {
                continue;
            }
            long span = rangeSpan(range);
            if (span < bestSpan) {
                best = ref;
                bestSpan = span;
            }
        }
        return best;
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
        Node scopeRoot = findScopeRoot(call);
        if (scopeRoot == null) {
            return null;
        }
        boolean inLambda = scopeRoot instanceof LambdaExpr;
        List<MethodCallExpr> calls = collectMethodCalls(scopeRoot);
        int line = call.getBegin().map(p -> p.line).orElse(-1);
        int mappedLine = line;
        if (line > 0) {
            String className = ctx == null ? null : ctx.className;
            String methodName = ctx == null ? null : ctx.methodName;
            String methodDesc = ctx == null ? null : ctx.methodDesc;
            int argCount = ctx == null ? ARG_COUNT_UNKNOWN : ctx.methodArgCount;
            if (className == null || className.trim().isEmpty()) {
                className = resolveEnclosingClassName(cu, call);
            }
            if (methodName == null || methodName.trim().isEmpty()) {
                EnclosingCallable enclosing = resolveEnclosingCallable(cu, call.getBegin().orElse(null));
                if (enclosing != null) {
                    if (className == null || className.trim().isEmpty()) {
                        className = enclosing.className;
                    }
                    methodName = enclosing.methodName;
                    if (argCount == ARG_COUNT_UNKNOWN) {
                        argCount = enclosing.argCount;
                    }
                }
            }
            if (className != null && !className.trim().isEmpty()) {
                className = normalizeClassName(className);
            }
            if ((methodDesc == null || methodDesc.trim().isEmpty())
                    && className != null && methodName != null) {
                methodDesc = resolveMethodDescByArgCount(className, methodName, argCount);
            }
            mappedLine = mapLineNumber(className, methodName, methodDesc, argCount, line);
        }
        if (calls.isEmpty()) {
            return new CallPosition(call, -1, inLambda, mappedLine);
        }
        calls.sort((a, b) -> {
            Range ra = a.getRange().orElse(null);
            Range rb = b.getRange().orElse(null);
            long ka = ra == null ? Long.MAX_VALUE : rangeBeginKey(ra);
            long kb = rb == null ? Long.MAX_VALUE : rangeBeginKey(rb);
            return Long.compare(ka, kb);
        });
        int index = -1;
        for (int i = 0; i < calls.size(); i++) {
            MethodCallExpr item = calls.get(i);
            if (item == call) {
                index = i;
                break;
            }
            Range r1 = item.getRange().orElse(null);
            Range r2 = call.getRange().orElse(null);
            if (r1 != null && r2 != null && r1.equals(r2)) {
                index = i;
                break;
            }
        }
        return new CallPosition(call, index, inLambda, mappedLine);
    }

    private static List<MethodCallExpr> collectMethodCalls(Node scopeRoot) {
        List<MethodCallExpr> out = new ArrayList<>();
        collectMethodCalls(scopeRoot, scopeRoot, out);
        return out;
    }

    private static void collectMethodCalls(Node node, Node scopeRoot, List<MethodCallExpr> out) {
        if (node == null) {
            return;
        }
        if (node != scopeRoot) {
            if (node instanceof LambdaExpr) {
                return;
            }
            if (node instanceof ClassOrInterfaceDeclaration
                    || node instanceof EnumDeclaration
                    || node instanceof RecordDeclaration
                    || node instanceof AnnotationDeclaration) {
                return;
            }
        }
        if (node instanceof MethodCallExpr) {
            out.add((MethodCallExpr) node);
        }
        for (Node child : node.getChildNodes()) {
            collectMethodCalls(child, scopeRoot, out);
        }
    }

    private static long rangeBeginKey(Range range) {
        if (range == null) {
            return Long.MAX_VALUE;
        }
        return positionKey(range.begin);
    }

    private static String resolveScopeHint(CallerContext ctx, SemanticTarget target) {
        if (target != null && target.className != null && !target.className.trim().isEmpty()) {
            return target.className;
        }
        if (ctx == null || ctx.cu == null || ctx.position == null) {
            return null;
        }
        MethodCallExpr callExpr = findMethodCallAt(ctx.cu, ctx.position);
        if (callExpr == null) {
            return null;
        }
        String scopeHint = resolveScopeClassName(ctx.cu, callExpr, ctx);
        if (scopeHint != null && !scopeHint.trim().isEmpty()) {
            return scopeHint;
        }
        if (callExpr.getScope().isPresent()) {
            Expression scope = unwrapEnclosed(callExpr.getScope().get());
            ResolvedType resolved = resolveExpressionType(ctx.cu, scope, ctx);
            if (resolved != null && resolved.internalName != null) {
                return resolved.internalName;
            }
        }
        return null;
    }

    private static CallSiteSelection resolveCallSiteTarget(CallerContext ctx,
                                                           String methodName,
                                                           int argCount,
                                                           String scopeHint,
                                                           CallPosition callPosition) {
        if (ctx == null || MainForm.getEngine() == null) {
            return null;
        }
        if (methodName == null || methodName.trim().isEmpty()) {
            return null;
        }
        String callerClass = ctx.className;
        if (callerClass == null || callerClass.trim().isEmpty()) {
            return null;
        }
        if (ctx.methodName == null || ctx.methodName.trim().isEmpty()) {
            return null;
        }
        callerClass = normalizeClassName(callerClass);
        if (callPosition != null && callPosition.inLambda) {
            return resolveLambdaCallSiteTarget(ctx, callerClass, methodName, argCount, scopeHint, callPosition);
        }
        List<CallSiteEntity> sites = MainForm.getEngine()
                .getCallSitesByCaller(callerClass, ctx.methodName, ctx.methodDesc);
        if ((sites == null || sites.isEmpty()) && ctx.methodDesc != null) {
            sites = MainForm.getEngine().getCallSitesByCaller(callerClass, ctx.methodName, null);
        }
        if (sites == null || sites.isEmpty()) {
            return null;
        }
        int callIndex = callPosition == null ? -1 : callPosition.callIndex;
        int line = ctx.line;
        if (callPosition != null && callPosition.line > 0) {
            line = callPosition.line;
        }
        CallSiteEntity picked = pickBestCallSite(
                sites, methodName, argCount, line, callIndex, scopeHint);
        if (picked == null) {
            return null;
        }
        String className = chooseCallSiteClass(picked, scopeHint);
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        return new CallSiteSelection(className, picked.getCalleeMethodName(), picked.getCalleeMethodDesc());
    }

    private static CallSiteSelection resolveLambdaCallSiteTarget(CallerContext ctx,
                                                                 String callerClass,
                                                                 String methodName,
                                                                 int argCount,
                                                                 String scopeHint,
                                                                 CallPosition callPosition) {
        if (ctx == null || MainForm.getEngine() == null) {
            return null;
        }
        if (callerClass == null || callerClass.trim().isEmpty()) {
            return null;
        }
        List<CallSiteEntity> sites = MainForm.getEngine()
                .getCallSitesByCaller(callerClass, null, null);
        if (sites == null || sites.isEmpty()) {
            return null;
        }
        List<CallSiteEntity> lambdaSites = new ArrayList<>();
        for (CallSiteEntity site : sites) {
            if (site == null || site.getCallerMethodName() == null) {
                continue;
            }
            if (site.getCallerMethodName().startsWith("lambda$")) {
                lambdaSites.add(site);
            }
        }
        if (lambdaSites.isEmpty()) {
            return null;
        }
        int callIndex = callPosition == null ? -1 : callPosition.callIndex;
        int line = ctx.line;
        if (callPosition != null && callPosition.line > 0) {
            line = callPosition.line;
        }
        CallSiteEntity picked = pickBestCallSite(
                lambdaSites, methodName, argCount, line, callIndex, scopeHint);
        if (picked == null) {
            return null;
        }
        String className = chooseCallSiteClass(picked, scopeHint);
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        return new CallSiteSelection(className, picked.getCalleeMethodName(), picked.getCalleeMethodDesc());
    }

    private static CallSiteEntity pickBestCallSite(List<CallSiteEntity> sites,
                                                   String methodName,
                                                   int argCount,
                                                   int line,
                                                   int callIndex,
                                                   String scopeHint) {
        if (sites == null || sites.isEmpty()) {
            return null;
        }
        String normalizedScope = scopeHint == null ? null : normalizeClassName(scopeHint);
        CallSiteEntity best = null;
        int bestScore = Integer.MIN_VALUE;
        for (CallSiteEntity site : sites) {
            if (site == null) {
                continue;
            }
            if (methodName != null && !methodName.equals(site.getCalleeMethodName())) {
                continue;
            }
            int score = 0;
            Integer lineNumber = site.getLineNumber();
            if (lineNumber != null && lineNumber > 0 && line > 0) {
                int diff = Math.abs(lineNumber - line);
                if (diff == 0) {
                    score += 6;
                } else if (diff == 1) {
                    score += 4;
                } else if (diff <= 3) {
                    score += 2;
                } else if (diff <= 5) {
                    score += 1;
                } else {
                    score -= 2;
                }
            }
            if (callIndex >= 0) {
                Integer siteIndex = site.getCallIndex();
                if (siteIndex != null && siteIndex >= 0) {
                    int diff = Math.abs(siteIndex - callIndex);
                    if (diff == 0) {
                        score += 6;
                    } else if (diff == 1) {
                        score += 4;
                    } else if (diff <= 2) {
                        score += 2;
                    } else if (diff <= 4) {
                        score += 1;
                    } else {
                        score -= 2;
                    }
                }
            }
            if (argCount != ARG_COUNT_UNKNOWN) {
                int siteArgs = argCountFromDesc(site.getCalleeMethodDesc());
                if (siteArgs == argCount) {
                    score += 3;
                } else if (siteArgs >= 0) {
                    score -= 2;
                }
            }
            if (normalizedScope != null) {
                String owner = site.getCalleeOwner();
                String receiver = site.getReceiverType();
                if (normalizedScope.equals(owner)) {
                    score += 3;
                }
                if (receiver != null && normalizedScope.equals(receiver)) {
                    score += 5;
                }
            }
            if (score > bestScore) {
                best = site;
                bestScore = score;
            }
        }
        if (bestScore < 3) {
            return null;
        }
        return best;
    }

    private static String chooseCallSiteClass(CallSiteEntity site, String scopeHint) {
        if (site == null) {
            return null;
        }
        String owner = site.getCalleeOwner();
        String receiver = site.getReceiverType();
        String normalizedScope = scopeHint == null ? null : normalizeClassName(scopeHint);
        if (normalizedScope != null) {
            if (receiver != null && normalizedScope.equals(receiver)) {
                return receiver;
            }
            if (owner != null && normalizedScope.equals(owner)) {
                return owner;
            }
        }
        if (receiver != null && owner != null && !receiver.equals(owner)
                && MainForm.getEngine() != null) {
            List<MethodResult> methods = MainForm.getEngine()
                    .getMethod(receiver, site.getCalleeMethodName(), site.getCalleeMethodDesc());
            if (methods != null && !methods.isEmpty()) {
                return receiver;
            }
        }
        return owner;
    }

    private static ResolvedType resolveLocalVarTypeFromDbType(String className,
                                                              String methodName,
                                                              String methodDesc,
                                                              String varName,
                                                              int line) {
        if (MainForm.getEngine() == null) {
            return null;
        }
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        className = normalizeClassName(className);
        if (varName == null || varName.trim().isEmpty()) {
            return null;
        }
        List<LocalVarEntity> vars = MainForm.getEngine().getLocalVarsByMethod(
                className, methodName, methodDesc);
        if (vars == null || vars.isEmpty()) {
            return null;
        }
        LocalVarEntity best = null;
        int bestSpan = Integer.MAX_VALUE;
        for (LocalVarEntity var : vars) {
            if (var == null) {
                continue;
            }
            if (!varName.equals(var.getVarName())) {
                continue;
            }
            int start = var.getStartLine() == null ? -1 : var.getStartLine();
            int end = var.getEndLine() == null ? -1 : var.getEndLine();
            boolean inRange = true;
            if (line > 0 && start > 0 && end > 0) {
                inRange = line >= start && line <= end;
            }
            if (!inRange) {
                continue;
            }
            int span = (start > 0 && end > 0) ? (end - start) : Integer.MAX_VALUE - 1;
            if (span < bestSpan) {
                best = var;
                bestSpan = span;
            }
        }
        if (best == null) {
            return null;
        }
        return resolveTypeFromDescAndSignature(best.getVarDesc(), best.getVarSignature());
    }

    private static String resolveLocalVarTypeFromDb(String className,
                                                    String methodName,
                                                    String methodDesc,
                                                    String varName,
                                                    int line) {
        ResolvedType type = resolveLocalVarTypeFromDbType(className, methodName, methodDesc, varName, line);
        return type == null ? null : type.internalName;
    }

    private static ResolvedType resolveTypeFromDescAndSignature(String desc, String signature) {
        if ((signature == null || signature.trim().isEmpty())
                && (desc == null || desc.trim().isEmpty())) {
            return null;
        }
        if (signature != null && !signature.trim().isEmpty()) {
            GenericType gt = parseTypeSignature(signature);
            if (gt != null) {
                return resolveGenericType(gt, null);
            }
        }
        if (desc != null && !desc.trim().isEmpty()) {
            try {
                Type type = Type.getType(desc);
                if (type.getSort() >= Type.BOOLEAN && type.getSort() <= Type.DOUBLE) {
                    return new ResolvedType(type);
                }
                if (type.getSort() == Type.OBJECT) {
                    return new ResolvedType(type.getInternalName(), null);
                }
                if (type.getSort() == Type.ARRAY) {
                    return new ResolvedType(type);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static ResolvedType resolveFieldTypeFromDb(String className, String fieldName) {
        if (MainForm.getEngine() == null) {
            return null;
        }
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        if (fieldName == null || fieldName.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeClassName(className);
        MemberEntity member = null;
        String current = normalized;
        for (int i = 0; i < 6; i++) {
            member = MainForm.getEngine().getMemberByClassAndName(current, fieldName);
            if (member != null) {
                break;
            }
            ClassResult result = MainForm.getEngine().getClassByClass(current);
            if (result == null || result.getSuperClassName() == null
                    || result.getSuperClassName().trim().isEmpty()) {
                break;
            }
            current = result.getSuperClassName();
        }
        if (member == null) {
            return null;
        }
        if (member.getMethodDesc() != null) {
            ResolvedType fromSig = resolveTypeFromDescAndSignature(
                    member.getMethodDesc(), member.getMethodSignature());
            if (fromSig != null) {
                return fromSig;
            }
        }
        if (member.getTypeClassName() != null && !member.getTypeClassName().trim().isEmpty()) {
            return new ResolvedType(member.getTypeClassName(), null);
        }
        return null;
    }

    private static ResolvedType resolveMethodReturnType(String owner,
                                                        String methodName,
                                                        String methodDesc,
                                                        ResolvedType scopeType) {
        if (owner == null || methodName == null || methodDesc == null) {
            return null;
        }
        String normalizedOwner = normalizeClassName(owner);
        ClassSignatureCache cache = getClassSignatureCache(normalizedOwner);
        String signature = null;
        if (cache != null && cache.methodSignatures != null) {
            signature = cache.methodSignatures.get(methodName + methodDesc);
        }
        ResolvedType bindingScope = scopeType;
        if (bindingScope != null && bindingScope.internalName != null
                && !bindingScope.internalName.equals(normalizedOwner)) {
            bindingScope = null;
        }
        Map<String, String> bindings = buildGenericBindings(cache, bindingScope);
        if (signature != null && !signature.trim().isEmpty()) {
            GenericType ret = parseMethodReturnSignature(signature);
            ResolvedType resolved = resolveGenericType(ret, bindings);
            if (resolved != null) {
                return resolved;
            }
        }
        try {
            Type ret = Type.getReturnType(methodDesc);
            if (ret.getSort() >= Type.BOOLEAN && ret.getSort() <= Type.DOUBLE) {
                return new ResolvedType(ret);
            }
            if (ret.getSort() == Type.OBJECT) {
                return new ResolvedType(ret.getInternalName(), null);
            }
            if (ret.getSort() == Type.ARRAY) {
                return new ResolvedType(ret);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static ClassSignatureCache getClassSignatureCache(String className) {
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeClassName(className);
        ClassSignatureCache cached = CLASS_SIGNATURE_CACHE.get(normalized);
        if (cached != null) {
            return cached;
        }
        ClassSignatureCache loaded = loadClassSignatureCache(normalized);
        if (loaded == null) {
            return null;
        }
        CLASS_SIGNATURE_CACHE.put(normalized, loaded);
        return loaded;
    }

    private static ClassSignatureCache loadClassSignatureCache(String className) {
        String classPath = resolveClassPath(className);
        if (classPath == null || classPath.trim().isEmpty()) {
            return new ClassSignatureCache(null, null, new HashMap<>());
        }
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(classPath));
            if (bytes == null || bytes.length == 0) {
                return new ClassSignatureCache(null, null, new HashMap<>());
            }
            ClassReader reader = new ClassReader(bytes);
            ClassNode node = new ClassNode();
            reader.accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
            String signature = node.signature;
            List<String> typeParams = signature == null ? null : parseClassTypeParams(signature);
            Map<String, String> methodSigs = new HashMap<>();
            if (node.methods != null) {
                for (MethodNode mn : node.methods) {
                    if (mn == null || mn.signature == null) {
                        continue;
                    }
                    methodSigs.put(mn.name + mn.desc, mn.signature);
                }
            }
            return new ClassSignatureCache(signature, typeParams, methodSigs);
        } catch (Exception ex) {
            return new ClassSignatureCache(null, null, new HashMap<>());
        }
    }

    private static List<String> parseClassTypeParams(String signature) {
        if (signature == null || signature.trim().isEmpty()) {
            return null;
        }
        List<String> params = new ArrayList<>();
        try {
            SignatureReader reader = new SignatureReader(signature);
            reader.accept(new SignatureVisitor(Opcodes.ASM9) {
                @Override
                public void visitFormalTypeParameter(String name) {
                    if (name != null && !name.trim().isEmpty()) {
                        params.add(name);
                    }
                }
            });
        } catch (Exception ignored) {
        }
        return params.isEmpty() ? null : params;
    }

    private static GenericType parseTypeSignature(String signature) {
        if (signature == null || signature.trim().isEmpty()) {
            return null;
        }
        try {
            SignatureReader reader = new SignatureReader(signature);
            GenericType type = new GenericType();
            reader.acceptType(new TypeCapture(type));
            return type;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static GenericType parseMethodReturnSignature(String signature) {
        if (signature == null || signature.trim().isEmpty()) {
            return null;
        }
        try {
            GenericType returnType = new GenericType();
            SignatureReader reader = new SignatureReader(signature);
            reader.accept(new SignatureVisitor(Opcodes.ASM9) {
                @Override
                public SignatureVisitor visitReturnType() {
                    return new TypeCapture(returnType);
                }
            });
            return returnType;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<GenericType> parseMethodParamSignatures(String signature) {
        if (signature == null || signature.trim().isEmpty()) {
            return null;
        }
        List<GenericType> params = new ArrayList<>();
        try {
            SignatureReader reader = new SignatureReader(signature);
            reader.accept(new SignatureVisitor(Opcodes.ASM9) {
                @Override
                public SignatureVisitor visitParameterType() {
                    GenericType paramType = new GenericType();
                    params.add(paramType);
                    return new TypeCapture(paramType);
                }
            });
        } catch (Exception ignored) {
            return null;
        }
        return params.isEmpty() ? null : params;
    }

    private static Map<String, String> buildGenericBindings(ClassSignatureCache cache,
                                                            ResolvedType scopeType) {
        if (cache == null || cache.typeParams == null || cache.typeParams.isEmpty()) {
            return null;
        }
        if (scopeType == null || scopeType.typeArguments == null || scopeType.typeArguments.isEmpty()) {
            return null;
        }
        Map<String, String> bindings = new HashMap<>();
        int limit = Math.min(cache.typeParams.size(), scopeType.typeArguments.size());
        for (int i = 0; i < limit; i++) {
            String param = cache.typeParams.get(i);
            String arg = scopeType.typeArguments.get(i);
            if (param != null && arg != null && !arg.trim().isEmpty()) {
                bindings.put(param, arg);
            }
        }
        return bindings.isEmpty() ? null : bindings;
    }

    private static ResolvedType resolveGenericType(GenericType type, Map<String, String> bindings) {
        if (type == null) {
            return null;
        }
        if (type.typeVar != null) {
            if (bindings == null) {
                return null;
            }
            String bound = bindings.get(type.typeVar);
            if (bound == null || bound.trim().isEmpty()) {
                return null;
            }
            return new ResolvedType(bound, null);
        }
        if (type.internalName == null || type.internalName.trim().isEmpty()) {
            return null;
        }
        List<String> args = null;
        if (type.typeArgs != null && !type.typeArgs.isEmpty()) {
            args = new ArrayList<>();
            for (GenericType arg : type.typeArgs) {
                ResolvedType resolved = resolveGenericType(arg, bindings);
                if (resolved != null && resolved.internalName != null) {
                    args.add(resolved.internalName);
                }
            }
        }
        return new ResolvedType(type.internalName, args == null || args.isEmpty() ? null : args);
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

    private static boolean isStringType(ResolvedType type) {
        if (type == null) {
            return false;
        }
        if (type.internalName != null) {
            return "java/lang/String".equals(type.internalName);
        }
        return false;
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

    private static final class GenericType {
        private String internalName;
        private String typeVar;
        private List<GenericType> typeArgs;
    }

    private static final class TypeCapture extends SignatureVisitor {
        private final GenericType target;

        private TypeCapture(GenericType target) {
            super(Opcodes.ASM9);
            this.target = target;
        }

        @Override
        public void visitClassType(String name) {
            target.internalName = name;
        }

        @Override
        public void visitInnerClassType(String name) {
            if (target.internalName == null || target.internalName.trim().isEmpty()) {
                target.internalName = name;
            } else {
                target.internalName = target.internalName + "$" + name;
            }
        }

        @Override
        public void visitTypeVariable(String name) {
            target.typeVar = name;
        }

        @Override
        public SignatureVisitor visitTypeArgument(char wildcard) {
            if (target.typeArgs == null) {
                target.typeArgs = new ArrayList<>();
            }
            GenericType arg = new GenericType();
            target.typeArgs.add(arg);
            return new TypeCapture(arg);
        }
    }

    private static final class ClassSignatureCache {
        private final String classSignature;
        private final List<String> typeParams;
        private final Map<String, String> methodSignatures;

        private ClassSignatureCache(String classSignature,
                                    List<String> typeParams,
                                    Map<String, String> methodSignatures) {
            this.classSignature = classSignature;
            this.typeParams = typeParams;
            this.methodSignatures = methodSignatures;
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

    private static final class EnclosingCallable {
        private final String className;
        private final String methodName;
        private final int argCount;

        private EnclosingCallable(String className, String methodName, int argCount) {
            this.className = className;
            this.methodName = methodName;
            this.argCount = argCount;
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
