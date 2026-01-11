/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.dfs;

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.gui.ChainsResultPanel;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DFSEngine {
    private static final Logger logger = LogManager.getLogger();

    private String sinkClass;
    private String sinkMethod;
    private String sinkDesc;
    private String sourceClass;
    private String sourceMethod;
    private String sourceDesc;

    private final CoreEngine engine;
    private final Object resultArea; // 可以是JTextArea或ChainsResultPanel

    private final boolean fromSink;
    private final boolean searchNullSource;
    private final int depth;
    // 仅在 API/MCP 场景下覆盖 GUI 选项，避免依赖界面状态
    private Boolean onlyFromWebOverride = null;

    private int chainCount = 0;
    private int sourceCount = 0;

    private int maxLimit = 30;
    private final Set<String> blacklist = new HashSet<>();
    private long timeoutMs = -1;
    private int maxNodes = -1;
    private int maxEdges = -1;
    private int maxPaths = -1;

    private int nodeCount = 0;
    private int edgeCount = 0;
    private boolean truncated = false;
    private String truncateReason = "";
    private long startNs = 0L;
    private long elapsedMs = 0L;

    public void setMaxLimit(int maxLimit) {
        this.maxLimit = maxLimit;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public void setMaxNodes(int maxNodes) {
        this.maxNodes = maxNodes;
    }

    public void setMaxEdges(int maxEdges) {
        this.maxEdges = maxEdges;
    }

    public void setMaxPaths(int maxPaths) {
        this.maxPaths = maxPaths;
    }

    public void setBlacklist(Set<String> blacklist) {
        for (String s : blacklist) {
            s = s.replace(".", "/");
            this.blacklist.add(s);
        }
    }

    // 新增：结果收集列表（异步读取需要线程安全）
    private final List<DFSResult> results = Collections.synchronizedList(new ArrayList<>());

    private volatile boolean canceled = false;

    public void cancel() {
        this.canceled = true;
    }

    private void clean() {
        if (resultArea instanceof JTextArea) {
            JTextArea textArea = (JTextArea) resultArea;
            textArea.setText("");
        } else if (resultArea instanceof ChainsResultPanel) {
            ChainsResultPanel panel = (ChainsResultPanel) resultArea;
            panel.clear();
        }
    }

    /**
     * 更新结果显示区域
     */
    private void update(String msg) {
        if (resultArea instanceof JTextArea) {
            JTextArea textArea = (JTextArea) resultArea;
            textArea.append(msg + "\n");
            textArea.setCaretPosition(textArea.getDocument().getLength());
        } else if (resultArea instanceof ChainsResultPanel) {
            ChainsResultPanel panel = (ChainsResultPanel) resultArea;
            panel.append(msg);
        }
    }

    /**
     * 添加调用链到结果面板
     */
    private void addChain(String chainId, String title, List<String> methods) {
        if (resultArea instanceof ChainsResultPanel) {
            ChainsResultPanel panel = (ChainsResultPanel) resultArea;
            panel.addChain(chainId, title, methods);
        } else {
            // 对于JTextArea，保持原有的输出方式
            update(title);
            for (int i = 0; i < methods.size(); i++) {
                String method = methods.get(i);
                String arrow = (i == 0) ? "" : " -> ";
                update(arrow + method);
            }
            update("");
        }
    }

    private void resetBudget() {
        this.nodeCount = 0;
        this.edgeCount = 0;
        this.truncated = false;
        this.truncateReason = "";
        this.startNs = System.nanoTime();
        this.elapsedMs = 0L;
    }

    private int getPathLimit() {
        int limit = maxLimit;
        if (maxPaths > 0 && (limit <= 0 || maxPaths < limit)) {
            limit = maxPaths;
        }
        return limit;
    }

    private void markTruncated(String reason) {
        if (!this.truncated) {
            this.truncated = true;
            this.truncateReason = reason == null ? "" : reason;
        }
    }

    private boolean stopNow() {
        if (canceled) {
            markTruncated("canceled");
            return true;
        }
        if (truncated) {
            return true;
        }
        if (timeoutMs > 0) {
            long elapsed = (System.nanoTime() - startNs) / 1_000_000L;
            if (elapsed >= timeoutMs) {
                markTruncated("timeout");
                return true;
            }
        }
        int limit = getPathLimit();
        if (limit > 0 && results.size() >= limit) {
            markTruncated(maxPaths > 0 ? "maxPaths" : "maxLimit");
            return true;
        }
        if (maxNodes > 0 && nodeCount >= maxNodes) {
            markTruncated("maxNodes");
            return true;
        }
        if (maxEdges > 0 && edgeCount >= maxEdges) {
            markTruncated("maxEdges");
            return true;
        }
        return false;
    }

    private boolean tickNode() {
        nodeCount++;
        return stopNow();
    }

    private boolean tickEdge() {
        edgeCount++;
        return stopNow();
    }

    private String buildRecommendation() {
        StringBuilder sb = new StringBuilder();
        sb.append("Try ");
        if ("timeout".equals(truncateReason)) {
            sb.append("increase timeoutMs or reduce depth/maxLimit, enable onlyFromWeb, add blacklist.");
        } else if ("maxNodes".equals(truncateReason) || "maxEdges".equals(truncateReason)) {
            sb.append("increase maxNodes/maxEdges or reduce depth, enable onlyFromWeb, add blacklist.");
        } else if ("maxPaths".equals(truncateReason) || "maxLimit".equals(truncateReason)) {
            sb.append("increase maxPaths/maxLimit or narrow sink/source.");
        } else {
            sb.append("reduce depth/maxLimit or enable onlyFromWeb.");
        }
        return sb.toString();
    }

    private void applyMetaToResults() {
        if (results == null || results.isEmpty()) {
            return;
        }
        String rec = truncated ? buildRecommendation() : "";
        int pathCount = results.size();
        for (DFSResult r : results) {
            r.setTruncated(truncated);
            r.setTruncateReason(truncateReason);
            r.setRecommend(rec);
            r.setNodeCount(nodeCount);
            r.setEdgeCount(edgeCount);
            r.setPathCount(pathCount);
            r.setElapsedMs(elapsedMs);
        }
    }

    public boolean isTruncated() {
        return truncated;
    }

    public String getTruncateReason() {
        return truncateReason;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public int getEdgeCount() {
        return edgeCount;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public String getRecommendation() {
        return truncated ? buildRecommendation() : "";
    }

    public DFSEngine(
            Object resultArea,
            boolean fromSink,
            boolean searchNullSource,
            int depth) {
        this.engine = MainForm.getEngine();
        this.resultArea = resultArea;
        this.fromSink = fromSink;
        this.searchNullSource = searchNullSource;
        this.depth = depth;
    }

    /**
     * 覆盖“仅从 Web 入口找源点”的配置（null 表示使用 GUI 勾选项）
     */
    public void setOnlyFromWebOverride(Boolean onlyFromWeb) {
        this.onlyFromWebOverride = onlyFromWeb;
    }

    public void setSink(
            String sinkClass,
            String sinkMethod,
            String sinkDesc) {
        this.sinkClass = sinkClass;
        this.sinkMethod = sinkMethod;
        this.sinkDesc = sinkDesc;
    }

    public void setSource(
            String sourceClass,
            String sourceMethod,
            String sourceDesc) {
        this.sourceClass = sourceClass;
        this.sourceMethod = sourceMethod;
        this.sourceDesc = sourceDesc;
    }

    public void doAnalyze() {
        if (this.fromSink) {
            if (this.sinkClass == null || this.sinkClass.trim().isEmpty()) {
                update("错误：SINK 分析模式不允许 SINK 为空");
                return;
            }
            if (!this.searchNullSource) {
                if (this.sourceClass == null || this.sourceClass.trim().isEmpty()) {
                    update("错误：SINK 分析模式 - 精确搜索 - 不允许 SOURCE 为空");
                    return;
                }
            }
        } else {
            if (this.searchNullSource) {
                update("错误：SOURCE 分析模式不允许选择 空 SOURCE 分析");
                return;
            }
            if (this.sourceClass == null || this.sourceClass.trim().isEmpty()) {
                update("错误：SOURCE 分析模式不允许 SOURCE 为空");
                return;
            }
        }
        logger.info("start chains dfs analyze");
        update("分析最大深度：" + depth);

        // 检查是否为查找所有 SOURCE 的模式
        boolean findAllSources = this.fromSink && this.searchNullSource;
        // 如果 API 传入了覆盖值，优先使用；否则沿用 GUI 选项
        boolean onlyFromWeb = onlyFromWebOverride != null
                ? onlyFromWebOverride
                : MainForm.getInstance().getSourceOnlyWebBox().isSelected();

        logger.info("find all sources from sink : " + findAllSources);

        if (findAllSources) {
            update("SINK: " + sinkClass + "." + sinkMethod);
            update("SOURCE: [查找所有可能的SOURCE点]");
        } else {
            update("SOURCE: " + sourceClass + "." + sourceMethod);
            update("SINK: " + sinkClass + "." + sinkMethod);
        }
        update("===========================================");

        // 2025/10/13 每次重新 DFS 分析应该清除之前的记录
        clean();
        resetBudget();

        chainCount = 0;
        sourceCount = 0;

        if (this.fromSink) {
            if (findAllSources) {
                update("从 SINK 开始反向分析 查找所有可能的 SOURCE 点");
            } else {
                update("从 SINK 开始反向分析");
            }
            MethodResult startMethod = new MethodResult(sinkClass, sinkMethod, sinkDesc);
            List<MethodResult> path = new ArrayList<>();
            path.add(startMethod);
            Set<String> visited = new HashSet<>();

            if (findAllSources) {
                if (!onlyFromWeb) {
                    dfsFromSinkFindAllSources(startMethod, path, visited, 0);
                } else {
                    ArrayList<ClassResult> springC = MainForm.getEngine().getAllSpringC();
                    ArrayList<MethodResult> webSources = new ArrayList<>();
                    for (ClassResult cr : springC) {
                        webSources.addAll(MainForm.getEngine().getSpringM(cr.getClassName()));
                    }
                    ArrayList<ClassResult> servlets = MainForm.getEngine().getAllServlets();
                    for (ClassResult cr : servlets) {
                        webSources.addAll(MainForm.getEngine().getMethodsByClass(cr.getClassName()));
                    }
                    // 完成
                    dfsFromSinkFindWebSources(startMethod, path, visited, webSources);
                }
            } else {
                dfsFromSink(startMethod, path, visited, 0);
            }
        } else {
            update("从 SOURCE 开始正向分析");
            MethodResult startMethod = new MethodResult(sourceClass, sourceMethod, sourceDesc);
            List<MethodResult> path = new ArrayList<>();
            path.add(startMethod);
            Set<String> visited = new HashSet<>();

            dfsFromSource(startMethod, path, visited, 0);
        }

        this.elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        applyMetaToResults();
        if (truncated) {
            update("分析已截断: " + truncateReason + " (elapsedMs=" + elapsedMs + ")");
            update(buildRecommendation());
        }
        update("===========================================");
        if (findAllSources) {
            update("总共找到 " + sourceCount + " 个可能的 SOURCE 点");
        } else {
            update("总共找到 " + chainCount + " 条可能的调用链");
        }
    }

    private void dfsFromSinkFindWebSources(
            MethodResult startMethod,
            List<MethodResult> path,
            Set<String> visited,
            ArrayList<MethodResult> webSources) {

        // 对每个 web source 进行 DFS 搜索
        for (MethodResult webSource : webSources) {
            if (stopNow()) {
                return;
            }
            // 重置访问状态和路径
            Set<String> currentVisited = new HashSet<>(visited);
            List<MethodResult> currentPath = new ArrayList<>(path);

            // 从 sink 开始向上搜索，看是否能到达这个 web source
            if (dfsFromSinkToWebSource(startMethod, webSource, currentPath, currentVisited, 0)) {
                outputSourceChain(currentPath, webSource);
            }
        }
    }

    private boolean dfsFromSinkToWebSource(
            MethodResult currentMethod,
            MethodResult targetWebSource,
            List<MethodResult> path,
            Set<String> visited,
            int currentDepth) {

        if (stopNow()) {
            return false;
        }
        if (currentDepth >= depth) {
            return false;
        }

        if (blacklist.contains(currentMethod.getClassName())) {
            return false;
        }

        String methodKey = getMethodKey(currentMethod);

        // 检查是否到达目标 web source
        if (isTargetMethod(currentMethod,
                targetWebSource.getClassName(),
                targetWebSource.getMethodName(),
                targetWebSource.getMethodDesc())) {
            return true;
        }

        if (visited.contains(methodKey)) {
            return false;
        }

        visited.add(methodKey);
        if (tickNode()) {
            visited.remove(methodKey);
            return false;
        }

        ArrayList<MethodResult> callerMethods = engine.getCallers(
                currentMethod.getClassName(),
                currentMethod.getMethodName(),
                currentMethod.getMethodDesc());

        for (MethodResult caller : callerMethods) {
            if (tickEdge()) {
                visited.remove(methodKey);
                return false;
            }
            path.add(caller);
            if (dfsFromSinkToWebSource(caller, targetWebSource, path, visited, currentDepth + 1)) {
                visited.remove(methodKey);
                return true;
            }
            path.remove(path.size() - 1);
        }

        visited.remove(methodKey);
        return false;
    }

    private void dfsFromSink(
            MethodResult currentMethod,
            List<MethodResult> path,
            Set<String> visited,
            int currentDepth) {
        if (stopNow()) {
            return;
        }
        if (currentDepth >= depth) {
            return;
        }

        if (blacklist.contains(currentMethod.getClassName())) {
            return;
        }

        String methodKey = getMethodKey(currentMethod);

        if (isTargetMethod(currentMethod, sourceClass, sourceMethod, sourceDesc)) {
            outputChain(path, true);
            return;
        }

        if (visited.contains(methodKey)) {
            return;
        }

        visited.add(methodKey);
        if (tickNode()) {
            visited.remove(methodKey);
            return;
        }

        ArrayList<MethodResult> callerMethods = engine.getCallers(
                currentMethod.getClassName(),
                currentMethod.getMethodName(),
                currentMethod.getMethodDesc());

        for (MethodResult caller : callerMethods) {
            if (tickEdge()) {
                visited.remove(methodKey);
                return;
            }
            path.add(caller);
            dfsFromSink(caller, path, visited, currentDepth + 1);
            path.remove(path.size() - 1);
        }

        visited.remove(methodKey);
    }

    private void dfsFromSinkFindAllSources(
            MethodResult currentMethod,
            List<MethodResult> path,
            Set<String> visited,
            int currentDepth) {
        if (stopNow()) {
            return;
        }
        if (currentDepth >= depth) {
            return;
        }

        if (blacklist.contains(currentMethod.getClassName())) {
            return;
        }

        String methodKey = getMethodKey(currentMethod);

        if (visited.contains(methodKey)) {
            return;
        }

        visited.add(methodKey);
        if (tickNode()) {
            visited.remove(methodKey);
            return;
        }

        ArrayList<MethodResult> callerMethods = engine.getCallers(
                currentMethod.getClassName(),
                currentMethod.getMethodName(),
                currentMethod.getMethodDesc());

        if (callerMethods.isEmpty()) {
            outputSourceChain(path, currentMethod);
        } else {
            for (MethodResult caller : callerMethods) {
                if (tickEdge()) {
                    visited.remove(methodKey);
                    return;
                }
                path.add(caller);
                dfsFromSinkFindAllSources(caller, path, visited, currentDepth + 1);
                path.remove(path.size() - 1);
            }
        }

        visited.remove(methodKey);
    }

    private void dfsFromSource(
            MethodResult currentMethod,
            List<MethodResult> path,
            Set<String> visited,
            int currentDepth) {
        if (stopNow()) {
            return;
        }
        if (currentDepth >= depth) {
            return;
        }

        if (blacklist.contains(currentMethod.getClassName())) {
            return;
        }

        String methodKey = getMethodKey(currentMethod);

        if (isTargetMethod(currentMethod, sinkClass, sinkMethod, sinkDesc)) {
            outputChain(path, false);
            return;
        }

        if (visited.contains(methodKey)) {
            return;
        }

        visited.add(methodKey);
        if (tickNode()) {
            visited.remove(methodKey);
            return;
        }

        ArrayList<MethodResult> calleeMethods = engine.getCallee(
                currentMethod.getClassName(),
                currentMethod.getMethodName(),
                currentMethod.getMethodDesc());

        for (MethodResult callee : calleeMethods) {
            if (tickEdge()) {
                visited.remove(methodKey);
                return;
            }
            path.add(callee);
            dfsFromSource(callee, path, visited, currentDepth + 1);
            path.remove(path.size() - 1);
        }

        visited.remove(methodKey);
    }

    private String getMethodKey(MethodResult method) {
        return method.getClassName() + "." + method.getMethodName() + "." + method.getMethodDesc();
    }

    private boolean isTargetMethod(MethodResult method, String targetClass, String targetMethod, String targetDesc) {
        if (targetClass == null || targetMethod == null || targetDesc == null) {
            return false;
        }
        return method.getClassName().equals(targetClass) &&
                method.getMethodName().equals(targetMethod) &&
                method.getMethodDesc().equals(targetDesc);
    }

    /**
     * 将 MethodResult 转换为 MethodReference.Handle
     */
    private MethodReference.Handle convertToHandle(MethodResult method) {
        ClassReference.Handle classHandle = new ClassReference.Handle(method.getClassName());
        return new MethodReference.Handle(classHandle, method.getMethodName(), method.getMethodDesc());
    }

    /**
     * 将 MethodResult 路径转换为 MethodReference.Handle 列表
     */
    private List<MethodReference.Handle> convertPathToHandles(List<MethodResult> path, boolean isReverse) {
        List<MethodReference.Handle> handles = new ArrayList<>();

        if (isReverse) {
            // 反向搜索时，路径需要反转
            for (int i = path.size() - 1; i >= 0; i--) {
                handles.add(convertToHandle(path.get(i)));
            }
        } else {
            // 正向搜索时，直接转换
            for (MethodResult method : path) {
                handles.add(convertToHandle(method));
            }
        }

        return handles;
    }

    private void outputChain(List<MethodResult> path, boolean isReverse) {
        if (stopNow()) {
            return;
        }
        chainCount++;
        String chainId = "chain_" + chainCount;
        String title = "调用链 #" + chainCount + " (长度: " + path.size() + ")";

        List<String> methods = new ArrayList<>();

        if (isReverse) {
            // 反向搜索时，路径需要反转输出
            for (int i = path.size() - 1; i >= 0; i--) {
                MethodResult method = path.get(i);
                methods.add(formatMethod(method));
            }
        } else {
            // 正向搜索时，直接输出
            for (MethodResult method : path) {
                methods.add(formatMethod(method));
            }
        }

        addChain(chainId, title, methods);

        // 新增：保存结果到 DFSResult
        DFSResult result = new DFSResult();
        result.setMethodList(convertPathToHandles(path, isReverse));
        result.setDepth(path.size());

        // 设置模式
        if (fromSink) {
            if (searchNullSource) {
                result.setMode(DFSResult.FROM_SOURCE_TO_ALL);
            } else {
                result.setMode(DFSResult.FROM_SINK_TO_SOURCE);
            }
        } else {
            result.setMode(DFSResult.FROM_SOURCE_TO_SINK);
        }

        // 设置 source 和 sink
        if (!path.isEmpty()) {
            if (isReverse) {
                // 反向搜索：路径的最后一个是source，第一个是sink
                result.setSource(convertToHandle(path.get(path.size() - 1)));
                result.setSink(convertToHandle(path.get(0)));
            } else {
                // 正向搜索：路径的第一个是source，最后一个是sink
                result.setSource(convertToHandle(path.get(0)));
                result.setSink(convertToHandle(path.get(path.size() - 1)));
            }
        }

        results.add(result);
        stopNow();
    }

    private void outputSourceChain(List<MethodResult> path, MethodResult sourceMethod) {
        if (stopNow()) {
            return;
        }
        sourceCount++;
        String chainId = "source_" + sourceCount;
        String title = " (调用链长度: " + path.size() + ") #" + sourceCount + ": " + formatMethod(sourceMethod);

        List<String> methods = new ArrayList<>();

        // 反向搜索时，路径需要反转输出（从SOURCE到SINK）
        for (int i = path.size() - 1; i >= 0; i--) {
            MethodResult method = path.get(i);
            methods.add(formatMethod(method));
        }

        addChain(chainId, title, methods);

        // 新增：保存结果到 DFSResult
        DFSResult result = new DFSResult();
        result.setMethodList(convertPathToHandles(path, true)); // 反向输出
        result.setDepth(path.size());
        result.setMode(DFSResult.FROM_SOURCE_TO_ALL);

        // 设置 source 和 sink
        if (!path.isEmpty()) {
            result.setSource(convertToHandle(sourceMethod));
            result.setSink(convertToHandle(path.get(0))); // sink 是路径的起点
        }

        results.add(result);
        stopNow();
    }

    private String formatMethod(MethodResult method) {
        return method.getClassName() + "." + method.getMethodName() + method.getMethodDesc();
    }

    /**
     * 获取所有分析结果
     *
     * @return DFSResult 列表
     */
    public List<DFSResult> getResults() {
        return new ArrayList<>(results);
    }

    /**
     * 清空结果列表
     */
    public void clearResults() {
        results.clear();
        chainCount = 0;
        sourceCount = 0;
    }

    /**
     * 获取结果数量
     *
     * @return 找到的调用链数量
     */
    public int getResultCount() {
        return results.size();
    }
}
