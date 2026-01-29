/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.engine;

import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.el.ResObj;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.entity.MethodCallResult;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.entity.MethodResultEdge;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.render.AllMethodsRender;
import me.n1ar4.jar.analyzer.gui.util.LogUtil;
import me.n1ar4.jar.analyzer.gui.util.MenuUtil;
import me.n1ar4.jar.analyzer.gui.util.UiExecutor;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("all")
public class CoreHelper {
    private static final String STRING_SEARCH_TIPS = "<html>" +
            "<p style='color: #FF4444; font-weight: bold;'>result is null</p>" +
            "<p>注意：字符串搜索：搜索字符串类型的常量</p>" +
            "<p style='color: #666666;'>字符串搜索不是类似 IDEA 中的字符串代码搜索</p>" +
            "<p style='margin-top: 10px;'>" +
            "例如：" +
            "<span style='font-family: monospace; background-color: #F0F0F0; padding: 2px 4px;'>" +
            "String prefix = \"rmi://\";</span></p>" +
            "<p>你可以搜索 <span style='color: #2196F3; font-weight: bold;'>" +
            "rmi" +
            "</span> 字符串定位到这个方法</p>" +
            "</html>";
    private static final AtomicInteger ALL_METHODS_SEQ = new AtomicInteger(0);
    private static final AtomicInteger CALLERS_SEQ = new AtomicInteger(0);
    private static final AtomicInteger CALLEES_SEQ = new AtomicInteger(0);
    private static final AtomicInteger IMPLS_SEQ = new AtomicInteger(0);
    private static final AtomicInteger SUPER_IMPLS_SEQ = new AtomicInteger(0);

    public static void refreshMethodContextAsync(String className, String methodName, String methodDesc, JDialog dialog) {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            if (dialog != null) {
                runOnEdt(dialog::dispose);
            }
            return;
        }
        SwingWorker<MethodContext, Void> worker = new SwingWorker<MethodContext, Void>() {
            @Override
            protected MethodContext doInBackground() {
                return loadMethodContext(engine, className, methodName, methodDesc);
            }

            @Override
            protected void done() {
                try {
                    applyMethodContext(get());
                } catch (Exception ignored) {
                } finally {
                    if (dialog != null) {
                        dialog.dispose();
                    }
                }
            }
        };
        worker.execute();
        if (dialog != null) {
            runOnEdt(() -> dialog.setVisible(true));
        }
    }

    private static MethodContext loadMethodContext(CoreEngine engine,
                                                   String className,
                                                   String methodName,
                                                   String methodDesc) {
        List<MethodResult> allMethods = loadAllMethods(engine, className);
        List<MethodResult> callers = loadCallers(engine, className, methodName, methodDesc);
        List<MethodResult> callees = loadCallees(engine, className, methodName, methodDesc);
        List<MethodResult> impls = loadImpls(engine, className, methodName, methodDesc);
        List<MethodResult> superImpls = loadSuperImpls(engine, className, methodName, methodDesc);
        MethodResult historyItem = buildHistoryItem(className, methodName, methodDesc);
        engine.insertHistory(historyItem);
        return new MethodContext(allMethods, callers, callees, impls, superImpls, historyItem);
    }

    private static void applyMethodContext(MethodContext context) {
        if (context == null) {
            return;
        }
        applyAllMethods(context.allMethods);
        applyCallers(context.callers);
        applyCallees(context.callees);
        applyImpls(context.impls);
        applySuperImpls(context.superImpls);
        applyHistoryItem(context.historyItem);
    }

    private static List<MethodResult> loadAllMethods(CoreEngine engine, String className) {
        ArrayList<MethodResult> results = engine.getMethodsByClass(className);
        if (results.isEmpty()) {
            results = engine.getMethodsByClassNoJar(className);
        }
        ArrayList<MethodResult> filtered = new ArrayList<>();
        for (MethodResult result : results) {
            if (result.getMethodName().startsWith("access$")) {
                continue;
            }
            filtered.add(result);
        }
        filtered.sort(Comparator.comparing(MethodResult::getMethodName));
        return filtered;
    }

    private static void applyAllMethods(List<MethodResult> results) {
        applyListModelAsync(results, ALL_METHODS_SEQ, methodsList -> {
            MainForm.getInstance().getAllMethodList().setCellRenderer(new AllMethodsRender());
            MainForm.getInstance().getAllMethodList().setModel(methodsList);
            MainForm.getInstance().getAllMethodList().repaint();
            MainForm.getInstance().getAllMethodList().revalidate();
        });
    }

    private static List<MethodResult> loadCallers(CoreEngine engine,
                                                  String className,
                                                  String methodName,
                                                  String methodDesc) {
        ArrayList<MethodCallResult> edges = engine
                .getCallEdgesByCallee(className, methodName, methodDesc, null, null);
        ArrayList<MethodResult> results = convertCallEdges(edges, true);
        sortByMenu(results);
        return results;
    }

    private static void applyCallers(List<MethodResult> results) {
        applyListModelAsync(results, CALLERS_SEQ, methodsList -> {
            MainForm.getInstance().getCallerList().setModel(methodsList);
            MainForm.getInstance().getCallerList().repaint();
            MainForm.getInstance().getCallerList().revalidate();
        });
    }

    private static List<MethodResult> loadCallees(CoreEngine engine,
                                                  String className,
                                                  String methodName,
                                                  String methodDesc) {
        ArrayList<MethodCallResult> edges = engine
                .getCallEdgesByCaller(className, methodName, methodDesc, null, null);
        ArrayList<MethodResult> results = convertCallEdges(edges, false);
        sortByMenu(results);
        return results;
    }

    private static void applyCallees(List<MethodResult> results) {
        applyListModelAsync(results, CALLEES_SEQ, methodsList -> {
            MainForm.getInstance().getCalleeList().setModel(methodsList);
            MainForm.getInstance().getCalleeList().repaint();
            MainForm.getInstance().getCalleeList().revalidate();
        });
    }

    private static List<MethodResult> loadImpls(CoreEngine engine,
                                                String className,
                                                String methodName,
                                                String methodDesc) {
        ArrayList<MethodResult> results = engine.getImpls(className, methodName, methodDesc);
        sortByMenu(results);
        return results;
    }

    private static void applyImpls(List<MethodResult> results) {
        applyListModelAsync(results, IMPLS_SEQ, methodsList -> {
            MainForm.getInstance().getMethodImplList().setModel(methodsList);
            MainForm.getInstance().getMethodImplList().repaint();
            MainForm.getInstance().getMethodImplList().revalidate();
        });
    }

    private static List<MethodResult> loadSuperImpls(CoreEngine engine,
                                                     String className,
                                                     String methodName,
                                                     String methodDesc) {
        ArrayList<MethodResult> results = engine.getSuperImpls(className, methodName, methodDesc);
        sortByMenu(results);
        return results;
    }

    private static void applySuperImpls(List<MethodResult> results) {
        applyListModelAsync(results, SUPER_IMPLS_SEQ, methodsList -> {
            MainForm.getInstance().getSuperImplList().setModel(methodsList);
            MainForm.getInstance().getSuperImplList().repaint();
            MainForm.getInstance().getSuperImplList().revalidate();
        });
    }

    private static DefaultListModel<MethodResult> buildMethodListModel(List<MethodResult> results) {
        DefaultListModel<MethodResult> methodsList = new DefaultListModel<>();
        if (results != null) {
            for (MethodResult result : results) {
                methodsList.addElement(result);
            }
        }
        return methodsList;
    }

    private static void applyListModelAsync(List<MethodResult> results,
                                            AtomicInteger seq,
                                            Consumer<DefaultListModel<MethodResult>> applier) {
        if (applier == null) {
            return;
        }
        int stamp = seq.incrementAndGet();
        Runnable buildTask = () -> {
            DefaultListModel<MethodResult> model = buildMethodListModel(results);
            runOnEdt(() -> {
                if (stamp != seq.get()) {
                    return;
                }
                applier.accept(model);
            });
        };
        if (SwingUtilities.isEventDispatchThread()) {
            UiExecutor.runAsync(buildTask);
        } else {
            buildTask.run();
        }
    }

    private static MethodResult buildHistoryItem(String className, String methodName, String methodDesc) {
        MethodResult methodResult = new MethodResult();
        methodResult.setClassName(className);
        methodResult.setMethodName(methodName);
        methodResult.setMethodDesc(methodDesc);
        return methodResult;
    }

    private static void applyHistoryItem(MethodResult methodResult) {
        if (methodResult == null) {
            return;
        }
        runOnEdt(() -> {
            DefaultListModel<MethodResult> methodsList = MainForm.getHistoryListData();
            methodsList.addElement(methodResult);
            MainForm.getInstance().getHistoryList().repaint();
            MainForm.getInstance().getHistoryList().revalidate();
        });
    }

    private static void sortByMenu(List<MethodResult> results) {
        if (results == null) {
            return;
        }
        if (MenuUtil.sortedByMethod()) {
            results.sort(Comparator.comparing(MethodResult::getMethodName));
        } else if (MenuUtil.sortedByClass()) {
            results.sort(Comparator.comparing(MethodResult::getClassName));
        } else {
            throw new RuntimeException("invalid sort");
        }
    }

    private static CoreEngine requireEngine() {
        CoreEngine engine = MainForm.getInstance().getEngine();
        if (engine == null) {
            showMessage("PLEASE BUILD DATABASE FIRST");
            return null;
        }
        return engine;
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
        } catch (Exception ignored) {
            return null;
        }
        return ref.get();
    }

    private static void runOnEdt(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    private static void showMessage(String message) {
        runOnEdt(() -> JOptionPane.showMessageDialog(MainForm.getInstance().getMasterPanel(), message));
    }

    private static void disposeDialog(JDialog dialog) {
        if (dialog == null) {
            return;
        }
        runOnEdt(() -> {
            dialog.dispose();
            dialog.setVisible(false);
        });
    }

    private static final class MethodContext {
        private final List<MethodResult> allMethods;
        private final List<MethodResult> callers;
        private final List<MethodResult> callees;
        private final List<MethodResult> impls;
        private final List<MethodResult> superImpls;
        private final MethodResult historyItem;

        private MethodContext(List<MethodResult> allMethods,
                              List<MethodResult> callers,
                              List<MethodResult> callees,
                              List<MethodResult> impls,
                              List<MethodResult> superImpls,
                              MethodResult historyItem) {
            this.allMethods = allMethods;
            this.callers = callers;
            this.callees = callees;
            this.impls = impls;
            this.superImpls = superImpls;
            this.historyItem = historyItem;
        }
    }
    public static void refreshAllMethods(String className) {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            return;
        }
        applyAllMethods(loadAllMethods(engine, className));
    }

    public static void refreshCallers(String className, String methodName, String methodDesc) {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            return;
        }
        applyCallers(loadCallers(engine, className, methodName, methodDesc));
    }

    public static void refreshImpls(String className, String methodName, String methodDesc) {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            return;
        }
        applyImpls(loadImpls(engine, className, methodName, methodDesc));
    }

    public static void refreshSuperImpls(String className, String methodName, String methodDesc) {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            return;
        }
        applySuperImpls(loadSuperImpls(engine, className, methodName, methodDesc));
    }

    public static void refreshCallee(String className, String methodName, String methodDesc) {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            return;
        }
        applyCallees(loadCallees(engine, className, methodName, methodDesc));
    }

    private static ArrayList<MethodResult> convertCallEdges(List<MethodCallResult> edges, boolean useCaller) {
        ArrayList<MethodResult> out = new ArrayList<>();
        if (edges == null || edges.isEmpty()) {
            return out;
        }
        Map<String, MethodResultEdge> merged = new LinkedHashMap<>();
        for (MethodCallResult edge : edges) {
            if (edge == null) {
                continue;
            }
            MethodResultEdge result = new MethodResultEdge();
            if (useCaller) {
                result.setClassName(edge.getCallerClassName());
                result.setMethodName(edge.getCallerMethodName());
                result.setMethodDesc(edge.getCallerMethodDesc());
                result.setJarName(edge.getCallerJarName());
                result.setJarId(edge.getCallerJarId() == null ? 0 : edge.getCallerJarId());
            } else {
                result.setClassName(edge.getCalleeClassName());
                result.setMethodName(edge.getCalleeMethodName());
                result.setMethodDesc(edge.getCalleeMethodDesc());
                result.setJarName(edge.getCalleeJarName());
                result.setJarId(edge.getCalleeJarId() == null ? 0 : edge.getCalleeJarId());
            }
            result.setEdgeType(edge.getEdgeType());
            result.setEdgeConfidence(edge.getEdgeConfidence());
            result.setEdgeEvidence(edge.getEdgeEvidence());
            String key = result.getClassName() + "#" + result.getMethodName() + "#" + result.getMethodDesc();
            MethodResultEdge existing = merged.get(key);
            if (existing == null) {
                merged.put(key, result);
            } else {
                mergeEdgeInfo(existing, result);
            }
        }
        out.addAll(merged.values());
        return out;
    }

    private static void mergeEdgeInfo(MethodResultEdge target, MethodResultEdge incoming) {
        if (target == null || incoming == null) {
            return;
        }
        if (StringUtil.isNull(target.getEdgeEvidence()) && !StringUtil.isNull(incoming.getEdgeEvidence())) {
            target.setEdgeEvidence(incoming.getEdgeEvidence());
        }
        String tType = target.getEdgeType();
        String iType = incoming.getEdgeType();
        if ((tType == null || "direct".equalsIgnoreCase(tType)) && iType != null
                && !"direct".equalsIgnoreCase(iType)) {
            target.setEdgeType(iType);
        }
        String tConf = normalizeConfidence(target.getEdgeConfidence());
        String iConf = normalizeConfidence(incoming.getEdgeConfidence());
        if (confidenceRank(iConf) > confidenceRank(tConf)) {
            target.setEdgeConfidence(incoming.getEdgeConfidence());
        }
    }

    private static String normalizeConfidence(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static int confidenceRank(String value) {
        if ("high".equals(value)) {
            return 3;
        }
        if ("medium".equals(value)) {
            return 2;
        }
        if ("low".equals(value)) {
            return 1;
        }
        return 0;
    }

    public static void refreshSpringC() {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            return;
        }
        ArrayList<ClassResult> results = engine.getAllSpringC();
        results.sort(Comparator.comparing(ClassResult::getClassName));
        DefaultListModel<ClassResult> springCModel = new DefaultListModel<>();
        for (ClassResult result : results) {
            springCModel.addElement(result);
        }
        runOnEdt(() -> MainForm.getInstance().getSpringCList().setModel(springCModel));
    }

    public static void refreshSpringI() {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            return;
        }
        ArrayList<ClassResult> results = engine.getAllSpringI();
        results.sort(Comparator.comparing(ClassResult::getClassName));
        DefaultListModel<ClassResult> springIModel = new DefaultListModel<>();
        for (ClassResult result : results) {
            springIModel.addElement(result);
        }
        runOnEdt(() -> MainForm.getInstance().getSpringIList().setModel(springIModel));
    }

    public static void refreshServlets() {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            return;
        }
        ArrayList<ClassResult> results = engine.getAllServlets();
        results.sort(Comparator.comparing(ClassResult::getClassName));
        DefaultListModel<ClassResult> servletsModel = new DefaultListModel<>();
        for (ClassResult result : results) {
            servletsModel.addElement(result);
        }
        runOnEdt(() -> MainForm.getInstance().getServletList().setModel(servletsModel));
    }

    public static void refreshFilters() {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            return;
        }
        ArrayList<ClassResult> results = engine.getAllFilters();
        results.sort(Comparator.comparing(ClassResult::getClassName));
        DefaultListModel<ClassResult> filtersModel = new DefaultListModel<>();
        for (ClassResult result : results) {
            filtersModel.addElement(result);
        }
        runOnEdt(() -> MainForm.getInstance().getFilterList().setModel(filtersModel));
    }

    public static void refreshLiteners() {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            return;
        }
        ArrayList<ClassResult> results = engine.getAllListeners();
        results.sort(Comparator.comparing(ClassResult::getClassName));
        DefaultListModel<ClassResult> listenersModel = new DefaultListModel<>();
        for (ClassResult result : results) {
            listenersModel.addElement(result);
        }
        runOnEdt(() -> MainForm.getInstance().getListenerList().setModel(listenersModel));
    }

    public static void pathSearchC() {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            return;
        }
        String text = callOnEdt(() -> MainForm.getInstance().getPathSearchTextField().getText());
        if (StringUtil.isNull(text)) {
            LogUtil.info("please enter the search keyword");
            return;
        }
        runOnEdt(() -> MainForm.getInstance().getSpringCList().setModel(new DefaultListModel<>()));
        ArrayList<ClassResult> results = engine.getAllSpringC();
        HashSet<ClassResult> classResults = new HashSet<>();
        ArrayList<MethodResult> methodResultsTotal = new ArrayList<>();
        results.forEach(result -> {
            ArrayList<MethodResult> methodResults = engine.getSpringM(result.getClassName());
            List<MethodResult> collect = methodResults.stream().filter(a -> a.getPath().contains(text)).collect(Collectors.toList());
            if (!collect.isEmpty()) {
                classResults.add(result);
                methodResultsTotal.addAll(collect);
            }
        });
        methodResultsTotal.sort(Comparator.comparing(MethodResult::getPath));
        classResults.stream().sorted(Comparator.comparing(ClassResult::getClassName));
        DefaultListModel<ClassResult> springCModelvar0 = new DefaultListModel<>();
        DefaultListModel<MethodResult> springMModel = new DefaultListModel<>();
        for (ClassResult result : classResults) {
            springCModelvar0.addElement(result);
        }
        for (MethodResult result : methodResultsTotal) {
            springMModel.addElement(result);
        }
        LogUtil.info("total spring controller records ：" + springCModelvar0.size());
        LogUtil.info("total path method records ：" + springMModel.size());
        runOnEdt(() -> {
            MainForm.getInstance().getSpringMList().setModel(springMModel);
            MainForm.getInstance().getSpringCList().setModel(springCModelvar0);
        });
    }

    public static void refreshSpringM(String className) {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            return;
        }
        ArrayList<MethodResult> results = engine.getSpringM(className);
        results.sort(Comparator.comparing(MethodResult::getMethodName));
        DefaultListModel<MethodResult> springCModel = new DefaultListModel<>();
        for (MethodResult result : results) {
            springCModel.addElement(result);
        }
        runOnEdt(() -> MainForm.getInstance().getSpringMList().setModel(springCModel));
    }

    public static void refreshCallSearch(String className, String methodName, String methodDesc, JDialog dialog) {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            disposeDialog(dialog);
            return;
        }
        // java.lang.String java/lang/String
        if (className != null) {
            className = className.replace(".", "/");
        }
        ArrayList<MethodResult> results = engine.getCallers(className, methodName, methodDesc);

        ArrayList<MethodResult> newReulst = applyCommonFilter(results);

        if (MenuUtil.sortedByMethod()) {
            newReulst.sort(Comparator.comparing(MethodResult::getMethodName));
        } else if (MenuUtil.sortedByClass()) {
            newReulst.sort(Comparator.comparing(MethodResult::getClassName));
        } else {
            throw new RuntimeException("invalid sort");
        }
        Boolean nullParamSelected = callOnEdt(() -> MainForm.getInstance().getNullParamBox().isSelected());
        if (Boolean.TRUE.equals(nullParamSelected)) {
            ArrayList<MethodResult> newResults = new ArrayList<>();
            for (MethodResult result : newReulst) {
                if (result.getMethodDesc().contains("()")) {
                    continue;
                }
                newResults.add(result);
            }
            newReulst.clear();
            newReulst.addAll(newResults);
        }
        DefaultListModel<MethodResult> methodsList = new DefaultListModel<>();
        for (MethodResult result : newReulst) {
            methodsList.addElement(result);
        }

        if (methodsList.isEmpty() || methodsList.size() == 0) {
            disposeDialog(dialog);
            showMessage("result is null");
            return;
        }

        runOnEdt(() -> {
            MainForm.getInstance().getSearchList().setModel(methodsList);
            MainForm.getInstance().getSearchList().repaint();
            MainForm.getInstance().getSearchList().revalidate();
            MainForm.getInstance().getTabbedPanel().setSelectedIndex(1);
        });
        showMessage(String.format("result number: %d (已应用黑名单过滤)", methodsList.size()));
        disposeDialog(dialog);
    }

    public static void refreshCallSearchList(List<SearchCondition> conditions) {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            return;
        }
        ArrayList<MethodResult> totalResults = new ArrayList<>();
        for (SearchCondition condition : conditions) {
            String className = condition.getClassName();
            String methodName = condition.getMethodName();
            String methodDesc = condition.getMethodDesc();
            if (className == null || className.trim().equals("null")) {
                className = null;
            }
            if (methodName == null || methodName.trim().equals("null")) {
                methodName = null;
            }
            if (methodDesc == null || methodDesc.trim().equals("null")) {
                methodDesc = null;
            }
            // java.lang.String java/lang/String
            if (className != null) {
                className = className.replace(".", "/");
            }
            ArrayList<MethodResult> results = engine.getCallers(className, methodName, methodDesc);
            totalResults.addAll(results);
        }
        // BALCK LIST
        ArrayList<MethodResult> newReulst = applyCommonFilter(totalResults);

        if (MenuUtil.sortedByMethod()) {
            newReulst.sort(Comparator.comparing(MethodResult::getMethodName));
        } else if (MenuUtil.sortedByClass()) {
            newReulst.sort(Comparator.comparing(MethodResult::getClassName));
        } else {
            throw new RuntimeException("invalid sort");
        }

        Boolean nullParamSelected = callOnEdt(() -> MainForm.getInstance().getNullParamBox().isSelected());
        if (Boolean.TRUE.equals(nullParamSelected)) {
            ArrayList<MethodResult> newResults = new ArrayList<>();
            for (MethodResult result : newReulst) {
                if (result.getMethodDesc().contains("()")) {
                    continue;
                }
                newResults.add(result);
            }
            newReulst.clear();
            newReulst.addAll(newResults);
        }

        DefaultListModel<MethodResult> methodsList = new DefaultListModel<>();
        for (MethodResult result : newReulst) {
            methodsList.addElement(result);
        }

        if (methodsList.isEmpty() || methodsList.size() == 0) {
            showMessage("result is null");
            return;
        }

        runOnEdt(() -> {
            MainForm.getInstance().getSearchList().setModel(methodsList);
            MainForm.getInstance().getSearchList().repaint();
            MainForm.getInstance().getSearchList().revalidate();
            MainForm.getInstance().getTabbedPanel().setSelectedIndex(1);
        });
        showMessage(String.format("result number: %d", methodsList.size()));
    }

    public static void refreshDefSearch(String className, String methodName, String methodDesc, JDialog dialog) {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            disposeDialog(dialog);
            return;
        }
        // java.lang.String java/lang/String
        if (className != null) {
            className = className.replace(".", "/");
        }
        ArrayList<MethodResult> results = engine.getMethod(className, methodName, methodDesc);

        ArrayList<MethodResult> newReulst = applyCommonFilter(results);

        if (MenuUtil.sortedByMethod()) {
            newReulst.sort(Comparator.comparing(MethodResult::getMethodName));
        } else if (MenuUtil.sortedByClass()) {
            newReulst.sort(Comparator.comparing(MethodResult::getClassName));
        } else {
            throw new RuntimeException("invalid sort");
        }

        Boolean nullParamSelected = callOnEdt(() -> MainForm.getInstance().getNullParamBox().isSelected());
        if (Boolean.TRUE.equals(nullParamSelected)) {
            ArrayList<MethodResult> newResults = new ArrayList<>();
            for (MethodResult result : newReulst) {
                if (result.getMethodDesc().contains("()")) {
                    continue;
                }
                newResults.add(result);
            }
            newReulst.clear();
            newReulst.addAll(newResults);
        }

        DefaultListModel<MethodResult> methodsList = new DefaultListModel<>();
        for (MethodResult result : newReulst) {
            methodsList.addElement(result);
        }

        if (methodsList.isEmpty() || methodsList.size() == 0) {
            disposeDialog(dialog);
            showMessage("result is null");
            return;
        }

        runOnEdt(() -> {
            MainForm.getInstance().getSearchList().setModel(methodsList);
            MainForm.getInstance().getSearchList().repaint();
            MainForm.getInstance().getSearchList().revalidate();
            MainForm.getInstance().getTabbedPanel().setSelectedIndex(1);
        });
        showMessage(String.format("result number: %d", methodsList.size()));
        disposeDialog(dialog);
    }

    public static void refreshStrSearch(String className, String val, JDialog dialog) {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            disposeDialog(dialog);
            return;
        }
        ArrayList<MethodResult> results = engine.getMethodsByStr(val);
        // 2025/04/08 允许字符串搜索根据类名过滤
        if (className != null && !className.isEmpty()) {
            className = className.replace(".", "/");
            ArrayList<MethodResult> newReulst = new ArrayList<>();
            for (MethodResult m : results) {
                if (m.getClassName().equals(className)) {
                    newReulst.add(m);
                }
            }
            results.clear();
            results.addAll(newReulst);
        }

        ArrayList<MethodResult> newReulst = applyCommonFilter(results);

        if (MenuUtil.sortedByMethod()) {
            newReulst.sort(Comparator.comparing(MethodResult::getMethodName));
        } else if (MenuUtil.sortedByClass()) {
            newReulst.sort(Comparator.comparing(MethodResult::getClassName));
        } else {
            throw new RuntimeException("invalid sort");
        }

        Boolean nullParamSelected = callOnEdt(() -> MainForm.getInstance().getNullParamBox().isSelected());
        if (Boolean.TRUE.equals(nullParamSelected)) {
            ArrayList<MethodResult> newResults = new ArrayList<>();
            for (MethodResult result : newReulst) {
                if (result.getMethodDesc().contains("()")) {
                    continue;
                }
                newResults.add(result);
            }
            newReulst.clear();
            newReulst.addAll(newResults);
        }

        DefaultListModel<MethodResult> methodsList = new DefaultListModel<>();
        for (MethodResult result : newReulst) {
            methodsList.addElement(result);
        }

        if (methodsList.isEmpty() || methodsList.size() == 0) {
            // 2025/08/01 字符串搜不到的时候应该添加说明
            disposeDialog(dialog);
            showMessage(STRING_SEARCH_TIPS);
            return;
        }

        runOnEdt(() -> {
            MainForm.getInstance().getSearchList().setModel(methodsList);
            MainForm.getInstance().getSearchList().repaint();
            MainForm.getInstance().getSearchList().revalidate();
            MainForm.getInstance().getTabbedPanel().setSelectedIndex(1);
        });
        showMessage(String.format("result number: %d", methodsList.size()));
        disposeDialog(dialog);
    }

    public static void refreshStrSearchEqual(String className, String val, JDialog dialog) {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            disposeDialog(dialog);
            return;
        }
        ArrayList<MethodResult> results = engine.getMethodsByStrEqual(val);
        // 2025/04/08 允许字符串搜索根据类名过滤
        if (className != null && !className.isEmpty()) {
            className = className.replace(".", "/");
            ArrayList<MethodResult> newReulst = new ArrayList<>();
            for (MethodResult m : results) {
                if (m.getClassName().equals(className)) {
                    newReulst.add(m);
                }
            }
            results.clear();
            results.addAll(newReulst);
        }

        ArrayList<MethodResult> newReulst = applyCommonFilter(results);

        if (MenuUtil.sortedByMethod()) {
            newReulst.sort(Comparator.comparing(MethodResult::getMethodName));
        } else if (MenuUtil.sortedByClass()) {
            newReulst.sort(Comparator.comparing(MethodResult::getClassName));
        } else {
            throw new RuntimeException("invalid sort");
        }

        Boolean nullParamSelected = callOnEdt(() -> MainForm.getInstance().getNullParamBox().isSelected());
        if (Boolean.TRUE.equals(nullParamSelected)) {
            ArrayList<MethodResult> newResults = new ArrayList<>();
            for (MethodResult result : newReulst) {
                if (result.getMethodDesc().contains("()")) {
                    continue;
                }
                newResults.add(result);
            }
            newReulst.clear();
            newReulst.addAll(newResults);
        }

        DefaultListModel<MethodResult> methodsList = new DefaultListModel<>();
        for (MethodResult result : newReulst) {
            methodsList.addElement(result);
        }

        if (methodsList.isEmpty() || methodsList.size() == 0) {
            // 2025/08/01 字符串搜不到的时候应该添加说明
            disposeDialog(dialog);
            showMessage(STRING_SEARCH_TIPS);
            return;
        }

        runOnEdt(() -> {
            MainForm.getInstance().getSearchList().setModel(methodsList);
            MainForm.getInstance().getSearchList().repaint();
            MainForm.getInstance().getSearchList().revalidate();
            MainForm.getInstance().getTabbedPanel().setSelectedIndex(1);
        });
        showMessage(String.format("result number: %d", methodsList.size()));
        disposeDialog(dialog);
    }

    public static void refreshHistory(String className, String methodName, String methodDesc) {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            return;
        }
        MethodResult methodResult = buildHistoryItem(className, methodName, methodDesc);
        engine.insertHistory(methodResult);
        applyHistoryItem(methodResult);
    }

    public static void refreshCallSearchLike(String className, String methodName, String methodDesc, JDialog dialog) {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            disposeDialog(dialog);
            return;
        }
        // java.lang.String java/lang/String
        if (className != null) {
            className = className.replace(".", "/");
        }
        ArrayList<MethodResult> results = engine.getCallersLike(className, methodName, methodDesc);

        ArrayList<MethodResult> newReulst = applyCommonFilter(results);

        if (MenuUtil.sortedByMethod()) {
            newReulst.sort(Comparator.comparing(MethodResult::getMethodName));
        } else if (MenuUtil.sortedByClass()) {
            newReulst.sort(Comparator.comparing(MethodResult::getClassName));
        } else {
            throw new RuntimeException("invalid sort");
        }

        Boolean nullParamSelected = callOnEdt(() -> MainForm.getInstance().getNullParamBox().isSelected());
        if (Boolean.TRUE.equals(nullParamSelected)) {
            ArrayList<MethodResult> newResults = new ArrayList<>();
            for (MethodResult result : newReulst) {
                if (result.getMethodDesc().contains("()")) {
                    continue;
                }
                newResults.add(result);
            }
            newReulst.clear();
            newReulst.addAll(newResults);
        }

        DefaultListModel<MethodResult> methodsList = new DefaultListModel<>();
        for (MethodResult result : newReulst) {
            methodsList.addElement(result);
        }

        if (methodsList.isEmpty() || methodsList.size() == 0) {
            disposeDialog(dialog);
            showMessage("result is null");
            return;
        }

        runOnEdt(() -> {
            MainForm.getInstance().getSearchList().setModel(methodsList);
            MainForm.getInstance().getSearchList().repaint();
            MainForm.getInstance().getSearchList().revalidate();
            MainForm.getInstance().getTabbedPanel().setSelectedIndex(1);
        });
        showMessage(String.format("result number: %d", methodsList.size()));
        disposeDialog(dialog);
    }

    public static void refreshFindUsagesApprox(String className,
                                               String methodName,
                                               String methodDesc,
                                               JDialog dialog) {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            disposeDialog(dialog);
            return;
        }
        if (className != null) {
            className = className.replace(".", "/");
        }
        ArrayList<MethodResult> targets = new ArrayList<>();
        if (className != null || methodName != null) {
            MethodResult base = new MethodResult();
            base.setClassName(className);
            base.setMethodName(methodName);
            base.setMethodDesc(methodDesc);
            targets.add(base);
        }
        ArrayList<MethodResult> impls = engine.getImpls(className, methodName, methodDesc);
        ArrayList<MethodResult> supers = engine.getSuperImpls(className, methodName, methodDesc);
        if (impls != null && !impls.isEmpty()) {
            targets.addAll(impls);
        }
        if (supers != null && !supers.isEmpty()) {
            targets.addAll(supers);
        }

        ArrayList<MethodResult> results = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        for (MethodResult target : targets) {
            if (target == null) {
                continue;
            }
            ArrayList<MethodResult> callers = engine.getCallers(
                    target.getClassName(), target.getMethodName(), target.getMethodDesc());
            appendUniqueResults(results, seen, callers);
        }
        if (results.isEmpty()) {
            ArrayList<MethodResult> fuzzy = engine
                    .getCallersLike(className, methodName, methodDesc);
            appendUniqueResults(results, seen, fuzzy);
        }

        ArrayList<MethodResult> newReulst = applyCommonFilter(results);

        if (MenuUtil.sortedByMethod()) {
            newReulst.sort(Comparator.comparing(MethodResult::getMethodName));
        } else if (MenuUtil.sortedByClass()) {
            newReulst.sort(Comparator.comparing(MethodResult::getClassName));
        } else {
            throw new RuntimeException("invalid sort");
        }
        Boolean nullParamSelected = callOnEdt(() -> MainForm.getInstance().getNullParamBox().isSelected());
        if (Boolean.TRUE.equals(nullParamSelected)) {
            ArrayList<MethodResult> newResults = new ArrayList<>();
            for (MethodResult result : newReulst) {
                if (result.getMethodDesc().contains("()")) {
                    continue;
                }
                newResults.add(result);
            }
            newReulst.clear();
            newReulst.addAll(newResults);
        }

        DefaultListModel<MethodResult> methodsList = new DefaultListModel<>();
        for (MethodResult result : newReulst) {
            methodsList.addElement(result);
        }

        if (methodsList.isEmpty() || methodsList.size() == 0) {
            disposeDialog(dialog);
            showMessage("result is null");
            return;
        }

        runOnEdt(() -> {
            MainForm.getInstance().getSearchList().setModel(methodsList);
            MainForm.getInstance().getSearchList().repaint();
            MainForm.getInstance().getSearchList().revalidate();
            MainForm.getInstance().getTabbedPanel().setSelectedIndex(1);
        });
        showMessage(String.format("result number: %d (approx usages)", methodsList.size()));
        disposeDialog(dialog);
    }

    public static void refreshDefSearchLike(String className, String methodName, String methodDesc, JDialog dialog) {
        CoreEngine engine = requireEngine();
        if (engine == null) {
            disposeDialog(dialog);
            return;
        }
        // java.lang.String java/lang/String
        if (className != null) {
            className = className.replace(".", "/");
        }
        ArrayList<MethodResult> results = engine.getMethodLike(className, methodName, methodDesc);

        ArrayList<MethodResult> newReulst = applyCommonFilter(results);

        if (MenuUtil.sortedByMethod()) {
            newReulst.sort(Comparator.comparing(MethodResult::getMethodName));
        } else if (MenuUtil.sortedByClass()) {
            newReulst.sort(Comparator.comparing(MethodResult::getClassName));
        } else {
            throw new RuntimeException("invalid sort");
        }

        Boolean nullParamSelected = callOnEdt(() -> MainForm.getInstance().getNullParamBox().isSelected());
        if (Boolean.TRUE.equals(nullParamSelected)) {
            ArrayList<MethodResult> newResults = new ArrayList<>();
            for (MethodResult result : newReulst) {
                if (result.getMethodDesc().contains("()")) {
                    continue;
                }
                newResults.add(result);
            }
            newReulst.clear();
            newReulst.addAll(newResults);
        }

        DefaultListModel<MethodResult> methodsList = new DefaultListModel<>();
        for (MethodResult result : newReulst) {
            methodsList.addElement(result);
        }

        if (methodsList.isEmpty() || methodsList.size() == 0) {
            disposeDialog(dialog);
            showMessage("result is null");
            return;
        }

        runOnEdt(() -> {
            MainForm.getInstance().getSearchList().setModel(methodsList);
            MainForm.getInstance().getSearchList().repaint();
            MainForm.getInstance().getSearchList().revalidate();
            MainForm.getInstance().getTabbedPanel().setSelectedIndex(1);
        });
        showMessage(String.format("result number: %d", methodsList.size()));
        disposeDialog(dialog);
    }

    public static void refreshMethods(List<ResObj> methods) {
        if (requireEngine() == null) {
            return;
        }
        if (methods.isEmpty()) {
            showMessage("result is null");
            return;
        }

        List<MethodResult> methodResultList = methods.stream().map(new Function<ResObj, MethodResult>() {
            @Override
            public MethodResult apply(ResObj resObj) {
                MethodReference.Handle methodHandler = resObj.getMethod();
                MethodResult methodResult = new MethodResult();
                methodResult.setClassName(resObj.getClassName());
                methodResult.setMethodName(methodHandler.getName());
                methodResult.setMethodDesc(methodHandler.getDesc());
                methodResult.setLineNumber(resObj.getLineNumber());
                return methodResult;
            }
        }).collect(Collectors.toList());

        if (MenuUtil.sortedByMethod()) {
            methodResultList.sort(Comparator.comparing(MethodResult::getMethodName));
        }
        if (MenuUtil.sortedByClass()) {
            Collections.sort(methodResultList, Comparator.comparing(MethodResult::getClassName)
                    .thenComparing(MethodResult::getLineNumber));
        }

        DefaultListModel<MethodResult> methodsList = new DefaultListModel<>();
        methodResultList.forEach(methodsList::addElement);

        runOnEdt(() -> {
            MainForm.getInstance().getSearchList().setModel(methodsList);
            MainForm.getInstance().getSearchList().repaint();
            MainForm.getInstance().getSearchList().revalidate();
            MainForm.getInstance().getTabbedPanel().setSelectedIndex(1);
        });
        showMessage(String.format("result number: %d", methodsList.size()));
    }

    private static ArrayList<MethodResult> applyCommonFilter(List<MethodResult> results) {
        ArrayList<MethodResult> filtered = new ArrayList<>();
        if (results == null || results.isEmpty()) {
            return filtered;
        }
        for (MethodResult m : results) {
            if (CommonFilterUtil.isFilteredClass(m.getClassName())
                    || CommonFilterUtil.isFilteredJar(m.getJarName())) {
                continue;
            }
            filtered.add(m);
        }
        return filtered;
    }

    private static void appendUniqueResults(List<MethodResult> out,
                                            Set<String> seen,
                                            List<MethodResult> input) {
        if (input == null || input.isEmpty()) {
            return;
        }
        for (MethodResult m : input) {
            if (m == null) {
                continue;
            }
            String key = m.getClassName() + "#" + m.getMethodName() + "#" + m.getMethodDesc();
            if (seen.add(key)) {
                out.add(m);
            }
        }
    }
}
