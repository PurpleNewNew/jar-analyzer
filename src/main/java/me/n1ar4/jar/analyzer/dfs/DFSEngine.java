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

import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.CallGraphCache;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.gui.ChainsResultPanel;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.rules.SourceModel;
import me.n1ar4.jar.analyzer.taint.summary.GlobalPropagator;
import me.n1ar4.jar.analyzer.taint.summary.ReachabilityIndex;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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
    private final Map<MethodCallKey, MethodCallMeta> edgeMetaCache = new ConcurrentHashMap<>();
    private final Map<String, ArrayList<MethodResult>> callerCache = new ConcurrentHashMap<>();
    private final Map<String, ArrayList<MethodResult>> calleeCache = new ConcurrentHashMap<>();
    private final Set<String> chainKeySet = ConcurrentHashMap.newKeySet();
    private CallGraphCache callGraphCache;
    private ReachabilityIndex reachabilityIndex;

    private final boolean fromSink;
    private final boolean searchNullSource;
    private final int depth;
    // 仅在 API/MCP 场景下覆盖 GUI 选项，避免依赖界面状态
    private Boolean onlyFromWebOverride = null;
    private Set<String> knownSourceKeys = Collections.emptySet();
    private boolean reachabilityForFindAllSources = false;
    private boolean edgeConfidenceFilterEnabled = false;
    private Boolean summaryEnabledOverride = null;

    private final AtomicInteger chainCount = new AtomicInteger(0);
    private final AtomicInteger sourceCount = new AtomicInteger(0);
    private final AtomicInteger resultCount = new AtomicInteger(0);

    private int maxLimit = 30;
    private final Set<String> blacklist = new HashSet<>();
    private String minEdgeConfidence = MethodCallMeta.CONF_LOW;
    private boolean showEdgeMeta = true;
    private long timeoutMs = -1;
    private int maxNodes = -1;
    private int maxEdges = -1;
    private int maxPaths = -1;

    private final AtomicInteger nodeCount = new AtomicInteger(0);
    private final AtomicInteger edgeCount = new AtomicInteger(0);
    private final AtomicBoolean truncated = new AtomicBoolean(false);
    private final AtomicReference<String> truncateReason = new AtomicReference<>("");
    private long startNs = 0L;
    private long elapsedMs = 0L;

    private static final String PROP_SUMMARY_ENABLE = "jar.analyzer.taint.summary.enable";

    public void setMaxLimit(int maxLimit) {
        this.maxLimit = maxLimit;
    }

    public void setSummaryEnabled(Boolean summaryEnabled) {
        this.summaryEnabledOverride = summaryEnabled;
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

    public void setMinEdgeConfidence(String minEdgeConfidence) {
        this.minEdgeConfidence = normalizeConfidence(minEdgeConfidence);
    }


    public void setShowEdgeMeta(boolean showEdgeMeta) {
        this.showEdgeMeta = showEdgeMeta;
    }

    private final ConcurrentLinkedQueue<DFSResult> results = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean canceled = new AtomicBoolean(false);
    private final int parallelism = Math.max(1, Runtime.getRuntime().availableProcessors());
    private final int parallelDepth = 3;
    private boolean parallelEnabled = false;
    private ForkJoinPool pool;
    private static final int UI_BATCH_SIZE = 500;
    private static final int UI_MAX_FLUSH = 2000;
    private static final long UI_FLUSH_INTERVAL_MS = 60L;
    private final ConcurrentLinkedQueue<Runnable> uiTasks = new ConcurrentLinkedQueue<>();
    private final AtomicInteger uiTaskCount = new AtomicInteger(0);
    private final AtomicBoolean uiFlushScheduled = new AtomicBoolean(false);
    private final AtomicLong uiLastFlushMs = new AtomicLong(0L);
    private final Object uiDelayLock = new Object();
    private Timer uiDelayTimer;

    public void cancel() {
        this.canceled.set(true);
    }

    private void clean() {
        runOnEdt(() -> {
            if (resultArea instanceof JTextArea) {
                JTextArea textArea = (JTextArea) resultArea;
                textArea.setText("");
            } else if (resultArea instanceof ChainsResultPanel) {
                ChainsResultPanel panel = (ChainsResultPanel) resultArea;
                panel.clear();
            }
        });
    }

    private void resetSearchState() {
        chainKeySet.clear();
        callerCache.clear();
        calleeCache.clear();
        edgeMetaCache.clear();
        knownSourceKeys = Collections.emptySet();
    }

    private boolean isSummaryEnabled() {
        if (summaryEnabledOverride != null) {
            return summaryEnabledOverride;
        }
        String raw = System.getProperty(PROP_SUMMARY_ENABLE);
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        return !"false".equalsIgnoreCase(raw.trim());
    }

    private Set<MethodReference.Handle> buildExtraSources(boolean findAllSources) {
        if (findAllSources) {
            return Collections.emptySet();
        }
        if (sourceClass == null || sourceClass.trim().isEmpty()
                || sourceMethod == null || sourceMethod.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<MethodReference.Handle> extra = new HashSet<>();
        if (engine != null && isAnyDesc(sourceDesc)) {
            List<MethodResult> methods = engine.getMethod(sourceClass, sourceMethod, "");
            for (MethodResult method : methods) {
                if (method != null) {
                    extra.add(convertToHandle(method));
                }
            }
            if (!extra.isEmpty()) {
                return extra;
            }
        }
        String desc = sourceDesc == null ? "" : sourceDesc;
        extra.add(new MethodReference.Handle(new ClassReference.Handle(sourceClass), sourceMethod, desc));
        return extra;
    }

    private Set<MethodReference.Handle> buildExtraSinks() {
        if (sinkClass == null || sinkClass.trim().isEmpty()
                || sinkMethod == null || sinkMethod.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<MethodReference.Handle> extra = new HashSet<>();
        if (engine != null && isAnyDesc(sinkDesc)) {
            List<MethodResult> methods = engine.getMethod(sinkClass, sinkMethod, "");
            for (MethodResult method : methods) {
                if (method != null) {
                    extra.add(convertToHandle(method));
                }
            }
            if (!extra.isEmpty()) {
                return extra;
            }
        }
        String desc = sinkDesc == null ? "" : sinkDesc;
        extra.add(new MethodReference.Handle(new ClassReference.Handle(sinkClass), sinkMethod, desc));
        return extra;
    }

    private boolean shouldSkipByReachability(MethodResult method, boolean findAllSources) {
        if (reachabilityIndex == null || method == null) {
            return false;
        }
        if (fromSink) {
            if (findAllSources) {
                if (!reachabilityForFindAllSources) {
                    return false;
                }
                MethodReference.Handle handle = convertToHandle(method);
                return !reachabilityIndex.isReachableFromSource(handle);
            }
            MethodReference.Handle handle = convertToHandle(method);
            return !reachabilityIndex.isReachableFromSource(handle);
        }
        MethodReference.Handle handle = convertToHandle(method);
        return !reachabilityIndex.isReachableToSink(handle);
    }

    private void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    private void enqueueUi(Runnable action) {
        if (action == null) {
            return;
        }
        uiTasks.add(action);
        int count = uiTaskCount.incrementAndGet();
        scheduleUiFlush(count, false);
    }

    private void flushUiTasks() {
        scheduleUiFlush(uiTaskCount.get(), true);
    }

    private void scheduleUiFlush(int count, boolean force) {
        if (!force) {
            long now = System.currentTimeMillis();
            long last = uiLastFlushMs.get();
            if (count < UI_BATCH_SIZE && now - last < UI_FLUSH_INTERVAL_MS) {
                scheduleUiDelay(now, last);
                return;
            }
        }
        if (!uiFlushScheduled.compareAndSet(false, true)) {
            return;
        }
        runOnEdt(() -> {
            uiFlushScheduled.set(false);
            flushUiTasksInternal();
        });
    }

    private void scheduleUiDelay(long now, long last) {
        int delay = (int) Math.max(1L, UI_FLUSH_INTERVAL_MS - (now - last));
        synchronized (uiDelayLock) {
            if (uiDelayTimer == null) {
                uiDelayTimer = new Timer(delay, e -> scheduleUiFlush(uiTaskCount.get(), true));
                uiDelayTimer.setRepeats(false);
            } else {
                uiDelayTimer.setInitialDelay(delay);
            }
            if (uiDelayTimer.isRunning()) {
                uiDelayTimer.restart();
            } else {
                uiDelayTimer.start();
            }
        }
    }

    private void stopUiDelayTimer() {
        synchronized (uiDelayLock) {
            if (uiDelayTimer != null) {
                uiDelayTimer.stop();
            }
        }
    }

    private void flushUiTasksInternal() {
        int processed = 0;
        Runnable task;
        while (processed < UI_MAX_FLUSH && (task = uiTasks.poll()) != null) {
            uiTaskCount.decrementAndGet();
            try {
                task.run();
            } catch (Exception ignored) {
            }
            processed++;
        }
        uiLastFlushMs.set(System.currentTimeMillis());
        if (!uiTasks.isEmpty()) {
            scheduleUiFlush(uiTaskCount.get(), true);
        }
    }

    /**
     * 更新结果显示区域
     */
    private void update(String msg) {
        enqueueUi(() -> {
            if (resultArea instanceof JTextArea) {
                JTextArea textArea = (JTextArea) resultArea;
                textArea.append(msg + "\n");
                textArea.setCaretPosition(textArea.getDocument().getLength());
            } else if (resultArea instanceof ChainsResultPanel) {
                ChainsResultPanel panel = (ChainsResultPanel) resultArea;
                panel.append(msg);
            }
        });
    }

    /**
     * 添加调用链到结果面板
     */
    private void addChain(String chainId, String title, List<String> methods, List<DFSEdge> edges) {
        if (resultArea instanceof ChainsResultPanel) {
            ChainsResultPanel panel = (ChainsResultPanel) resultArea;
            enqueueUi(() -> panel.addChain(chainId, title, methods, edges, showEdgeMeta));
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
        this.nodeCount.set(0);
        this.edgeCount.set(0);
        this.resultCount.set(0);
        this.truncated.set(false);
        this.truncateReason.set("");
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
        if (this.truncated.compareAndSet(false, true)) {
            this.truncateReason.set(reason == null ? "" : reason);
        }
    }

    private boolean stopNow() {
        if (canceled.get()) {
            markTruncated("canceled");
            return true;
        }
        if (truncated.get()) {
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
        if (limit > 0 && resultCount.get() >= limit) {
            markTruncated(maxPaths > 0 ? "maxPaths" : "maxLimit");
            return true;
        }
        if (maxNodes > 0 && nodeCount.get() >= maxNodes) {
            markTruncated("maxNodes");
            return true;
        }
        if (maxEdges > 0 && edgeCount.get() >= maxEdges) {
            markTruncated("maxEdges");
            return true;
        }
        return false;
    }

    private boolean tickNode() {
        nodeCount.incrementAndGet();
        return stopNow();
    }

    private boolean tickEdge() {
        edgeCount.incrementAndGet();
        return stopNow();
    }

    private String buildRecommendation() {
        StringBuilder sb = new StringBuilder();
        sb.append("Try ");
        String reason = truncateReason.get();
        if ("timeout".equals(reason)) {
            sb.append("increase timeoutMs or reduce depth/maxLimit, enable onlyFromWeb, add blacklist.");
        } else if ("maxNodes".equals(reason) || "maxEdges".equals(reason)) {
            sb.append("increase maxNodes/maxEdges or reduce depth, enable onlyFromWeb, add blacklist.");
        } else if ("maxPaths".equals(reason) || "maxLimit".equals(reason)) {
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
        String rec = truncated.get() ? buildRecommendation() : "";
        int pathCount = resultCount.get();
        for (DFSResult r : results) {
            r.setTruncated(truncated.get());
            r.setTruncateReason(truncateReason.get());
            r.setRecommend(rec);
            r.setNodeCount(nodeCount.get());
            r.setEdgeCount(edgeCount.get());
            r.setPathCount(pathCount);
            r.setElapsedMs(elapsedMs);
        }
    }

    public boolean isTruncated() {
        return truncated.get();
    }

    public String getTruncateReason() {
        return truncateReason.get();
    }

    public int getNodeCount() {
        return nodeCount.get();
    }

    public int getEdgeCount() {
        return edgeCount.get();
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public String getRecommendation() {
        return truncated.get() ? buildRecommendation() : "";
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

        parallelEnabled = parallelism > 1;
        pool = parallelEnabled ? new ForkJoinPool(parallelism) : null;
        if (engine != null) {
            try {
                callGraphCache = engine.getCallGraphCache();
            } catch (Exception ex) {
                logger.warn("load call graph cache failed: {}", ex.toString());
                callGraphCache = null;
            }
        }
        if (callGraphCache != null) {
            update("call graph cache: edges=" + callGraphCache.getEdgeCount()
                    + " methods=" + callGraphCache.getMethodCount());
        }

        update("分析最大深度：" + depth);

        // 检查是否为查找所有 SOURCE 的模式
        boolean findAllSources = this.fromSink && this.searchNullSource;
        // 如果 API 传入了覆盖值，优先使用；否则沿用 GUI 选项
        boolean onlyFromWeb = onlyFromWebOverride != null
                ? onlyFromWebOverride
                : MainForm.getInstance().getSourceOnlyWebBox().isSelected();
        reachabilityForFindAllSources = findAllSources && onlyFromWeb;
        edgeConfidenceFilterEnabled = isEdgeConfidenceFilterEnabled();

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
        resetSearchState();
        results.clear();
        uiTasks.clear();
        uiTaskCount.set(0);
        uiFlushScheduled.set(false);
        stopUiDelayTimer();
        chainCount.set(0);
        sourceCount.set(0);
        reachabilityIndex = null;
        if (callGraphCache != null && engine != null && isSummaryEnabled()) {
            try {
                Set<MethodReference.Handle> extraSources = buildExtraSources(findAllSources);
                Set<MethodReference.Handle> extraSinks = buildExtraSinks();
                reachabilityIndex = new GlobalPropagator().propagate(engine, callGraphCache, extraSources, extraSinks, false);
                update("summary reachability: toSink=" + reachabilityIndex.getReachableToSink().size()
                        + " fromSource=" + reachabilityIndex.getReachableFromSource().size());
            } catch (Exception ex) {
                logger.debug("summary reachability failed: {}", ex.toString());
                reachabilityIndex = null;
            }
        }

        try {
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
                        knownSourceKeys = buildKnownSourceKeys();
                        if (parallelEnabled) {
                            pool.invoke(new SinkAllTask(startMethod, path, visited, 0));
                        } else {
                            dfsFromSinkFindAllSources(startMethod, path, visited, 0);
                        }
                    } else {
                        ArrayList<ClassResult> springC = MainForm.getEngine().getAllSpringC();
                        ArrayList<MethodResult> webSources = new ArrayList<>();
                        for (ClassResult cr : springC) {
                            webSources.addAll(MainForm.getEngine().getSpringM(cr.getClassName()));
                        }
                        ArrayList<ClassResult> servlets = MainForm.getEngine().getAllServlets();
                        for (ClassResult cr : servlets) {
                            for (MethodResult method : MainForm.getEngine().getMethodsByClass(cr.getClassName())) {
                                if (isServletEntry(method)) {
                                    webSources.add(method);
                                }
                            }
                        }
                        List<String> annoSources = ModelRegistry.getSourceAnnotations();
                        if (annoSources != null && !annoSources.isEmpty()) {
                            webSources.addAll(MainForm.getEngine().getMethodsByAnnoNames(annoSources));
                        }
                        List<SourceModel> sourceModels = ModelRegistry.getSourceModels();
                        if (sourceModels != null && !sourceModels.isEmpty()) {
                            for (SourceModel model : sourceModels) {
                                webSources.addAll(resolveSourceModel(model));
                            }
                        }
                        Set<String> webSourceKeys = buildSourceKeySet(webSources);
                        knownSourceKeys = webSourceKeys;
                        if (webSourceKeys.isEmpty()) {
                            return;
                        }
                        if (parallelEnabled) {
                            pool.invoke(new SinkWebTask(startMethod, path, visited, 0,
                                    webSourceKeys, newConcurrentSourceSet()));
                        } else {
                            dfsFromSinkFindWebSourcesOnce(startMethod, path, visited, 0,
                                    webSourceKeys, new HashSet<>());
                        }
                    }
                } else {
                    if (parallelEnabled) {
                        pool.invoke(new SinkTask(startMethod, path, visited, 0));
                    } else {
                        dfsFromSink(startMethod, path, visited, 0);
                    }
                }
            } else {
                update("从 SOURCE 开始正向分析");
                MethodResult startMethod = new MethodResult(sourceClass, sourceMethod, sourceDesc);
                List<MethodResult> path = new ArrayList<>();
                path.add(startMethod);
                Set<String> visited = new HashSet<>();
                if (parallelEnabled) {
                    pool.invoke(new SourceTask(startMethod, path, visited, 0));
                } else {
                    dfsFromSource(startMethod, path, visited, 0);
                }
            }
        } finally {
            if (pool != null) {
                pool.shutdown();
                pool = null;
            }
            this.elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
            applyMetaToResults();
            if (truncated.get()) {
                update("分析已截断: " + truncateReason.get() + " (elapsedMs=" + elapsedMs + ")");
                update(buildRecommendation());
            }
            update("===========================================");
            if (findAllSources) {
                update("总共找到 " + sourceCount.get() + " 个可能的 SOURCE 点");
            } else {
                update("总共找到 " + chainCount.get() + " 条可能的调用链");
            }
            flushUiTasks();
        }
    }

    private boolean shouldFork(int currentDepth) {
        return parallelEnabled && pool != null && currentDepth < parallelDepth;
    }

    private Set<String> newConcurrentSourceSet() {
        return ConcurrentHashMap.newKeySet();
    }

    private void dfsFromSinkFindWebSourcesOnce(
            MethodResult currentMethod,
            List<MethodResult> path,
            Set<String> visited,
            int currentDepth,
            Set<String> webSourceKeys,
            Set<String> foundSources) {

        if (stopNow()) {
            return;
        }
        if (currentDepth >= depth) {
            return;
        }

        if (blacklist.contains(currentMethod.getClassName())) {
            return;
        }

        if (shouldSkipByReachability(currentMethod, true)) {
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

        if (webSourceKeys.contains(methodKey)) {
            if (foundSources.add(methodKey)) {
                outputSourceChain(path, currentMethod);
                if (stopNow()) {
                    visited.remove(methodKey);
                    return;
                }
                if (foundSources.size() >= webSourceKeys.size()) {
                    visited.remove(methodKey);
                    return;
                }
            }
        }

        ArrayList<MethodResult> callerMethods = getCallersCached(currentMethod);

        if (shouldFork(currentDepth)) {
            List<RecursiveAction> tasks = new ArrayList<>();
            for (MethodResult caller : callerMethods) {
                if (!isEdgeAllowed(caller, currentMethod)) {
                    continue;
                }
                if (tickEdge()) {
                    visited.remove(methodKey);
                    return;
                }
                List<MethodResult> nextPath = new ArrayList<>(path);
                nextPath.add(caller);
                Set<String> nextVisited = new HashSet<>(visited);
                tasks.add(new SinkWebTask(caller, nextPath, nextVisited,
                        currentDepth + 1, webSourceKeys, foundSources));
            }
            ForkJoinTask.invokeAll(tasks);
            if (foundSources.size() >= webSourceKeys.size()) {
                visited.remove(methodKey);
                return;
            }
        } else {
            for (MethodResult caller : callerMethods) {
                if (!isEdgeAllowed(caller, currentMethod)) {
                    continue;
                }
                if (tickEdge()) {
                    visited.remove(methodKey);
                    return;
                }
                path.add(caller);
                dfsFromSinkFindWebSourcesOnce(caller, path, visited, currentDepth + 1, webSourceKeys, foundSources);
                path.remove(path.size() - 1);
                if (stopNow()) {
                    visited.remove(methodKey);
                    return;
                }
                if (foundSources.size() >= webSourceKeys.size()) {
                    visited.remove(methodKey);
                    return;
                }
            }
        }

        visited.remove(methodKey);
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

        if (shouldSkipByReachability(currentMethod, false)) {
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

        ArrayList<MethodResult> callerMethods = getCallersCached(currentMethod);

        if (shouldFork(currentDepth)) {
            List<RecursiveAction> tasks = new ArrayList<>();
            for (MethodResult caller : callerMethods) {
                if (!isEdgeAllowed(caller, currentMethod)) {
                    continue;
                }
                if (tickEdge()) {
                    visited.remove(methodKey);
                    return;
                }
                List<MethodResult> nextPath = new ArrayList<>(path);
                nextPath.add(caller);
                Set<String> nextVisited = new HashSet<>(visited);
                tasks.add(new SinkTask(caller, nextPath, nextVisited, currentDepth + 1));
            }
            ForkJoinTask.invokeAll(tasks);
        } else {
            for (MethodResult caller : callerMethods) {
                if (!isEdgeAllowed(caller, currentMethod)) {
                    continue;
                }
                if (tickEdge()) {
                    visited.remove(methodKey);
                    return;
                }
                path.add(caller);
                dfsFromSink(caller, path, visited, currentDepth + 1);
                path.remove(path.size() - 1);
            }
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

        if (shouldSkipByReachability(currentMethod, true)) {
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

        ArrayList<MethodResult> callerMethods = getCallersCached(currentMethod);

        if (callerMethods.isEmpty()) {
            if (shouldOutputRootSource(currentMethod)) {
                outputSourceChain(path, currentMethod);
            }
        } else {
            if (shouldFork(currentDepth)) {
                List<RecursiveAction> tasks = new ArrayList<>();
                for (MethodResult caller : callerMethods) {
                    if (!isEdgeAllowed(caller, currentMethod)) {
                        continue;
                    }
                    if (tickEdge()) {
                        visited.remove(methodKey);
                        return;
                    }
                    List<MethodResult> nextPath = new ArrayList<>(path);
                    nextPath.add(caller);
                    Set<String> nextVisited = new HashSet<>(visited);
                    tasks.add(new SinkAllTask(caller, nextPath, nextVisited, currentDepth + 1));
                }
                ForkJoinTask.invokeAll(tasks);
            } else {
                for (MethodResult caller : callerMethods) {
                    if (!isEdgeAllowed(caller, currentMethod)) {
                        continue;
                    }
                    if (tickEdge()) {
                        visited.remove(methodKey);
                        return;
                    }
                    path.add(caller);
                    dfsFromSinkFindAllSources(caller, path, visited, currentDepth + 1);
                    path.remove(path.size() - 1);
                }
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

        if (shouldSkipByReachability(currentMethod, false)) {
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

        ArrayList<MethodResult> calleeMethods = getCalleeCached(currentMethod);

        if (shouldFork(currentDepth)) {
            List<RecursiveAction> tasks = new ArrayList<>();
            for (MethodResult callee : calleeMethods) {
                if (!isEdgeAllowed(currentMethod, callee)) {
                    continue;
                }
                if (tickEdge()) {
                    visited.remove(methodKey);
                    return;
                }
                List<MethodResult> nextPath = new ArrayList<>(path);
                nextPath.add(callee);
                Set<String> nextVisited = new HashSet<>(visited);
                tasks.add(new SourceTask(callee, nextPath, nextVisited, currentDepth + 1));
            }
            ForkJoinTask.invokeAll(tasks);
        } else {
            for (MethodResult callee : calleeMethods) {
                if (!isEdgeAllowed(currentMethod, callee)) {
                    continue;
                }
                if (tickEdge()) {
                    visited.remove(methodKey);
                    return;
                }
                path.add(callee);
                dfsFromSource(callee, path, visited, currentDepth + 1);
                path.remove(path.size() - 1);
            }
        }

        visited.remove(methodKey);
    }

    private class SinkTask extends RecursiveAction {
        private final MethodResult current;
        private final List<MethodResult> path;
        private final Set<String> visited;
        private final int depth;

        private SinkTask(MethodResult current, List<MethodResult> path, Set<String> visited, int depth) {
            this.current = current;
            this.path = path;
            this.visited = visited;
            this.depth = depth;
        }

        @Override
        protected void compute() {
            dfsFromSink(current, path, visited, depth);
        }
    }

    private class SourceTask extends RecursiveAction {
        private final MethodResult current;
        private final List<MethodResult> path;
        private final Set<String> visited;
        private final int depth;

        private SourceTask(MethodResult current, List<MethodResult> path, Set<String> visited, int depth) {
            this.current = current;
            this.path = path;
            this.visited = visited;
            this.depth = depth;
        }

        @Override
        protected void compute() {
            dfsFromSource(current, path, visited, depth);
        }
    }

    private class SinkAllTask extends RecursiveAction {
        private final MethodResult current;
        private final List<MethodResult> path;
        private final Set<String> visited;
        private final int depth;

        private SinkAllTask(MethodResult current, List<MethodResult> path, Set<String> visited, int depth) {
            this.current = current;
            this.path = path;
            this.visited = visited;
            this.depth = depth;
        }

        @Override
        protected void compute() {
            dfsFromSinkFindAllSources(current, path, visited, depth);
        }
    }

    private class SinkWebTask extends RecursiveAction {
        private final MethodResult current;
        private final List<MethodResult> path;
        private final Set<String> visited;
        private final int depth;
        private final Set<String> webSourceKeys;
        private final Set<String> foundSources;

        private SinkWebTask(MethodResult current,
                            List<MethodResult> path,
                            Set<String> visited,
                            int depth,
                            Set<String> webSourceKeys,
                            Set<String> foundSources) {
            this.current = current;
            this.path = path;
            this.visited = visited;
            this.depth = depth;
            this.webSourceKeys = webSourceKeys;
            this.foundSources = foundSources;
        }

        @Override
        protected void compute() {
            dfsFromSinkFindWebSourcesOnce(current, path, visited, depth, webSourceKeys, foundSources);
        }
    }

    private String getMethodKey(MethodResult method) {
        return method.getClassName() + "." + method.getMethodName() + "." + method.getMethodDesc();
    }

    private String normalizeDescForQuery(String desc) {
        if (desc == null) {
            return null;
        }
        String v = desc.trim();
        if (v.isEmpty() || "*".equals(v) || "null".equalsIgnoreCase(v)) {
            return null;
        }
        return desc;
    }

    private ArrayList<MethodResult> getCallersCached(MethodResult method) {
        if (method == null) {
            return new ArrayList<>();
        }
        CallGraphCache cache = callGraphCache;
        if (cache != null) {
            return cache.getCallers(method);
        }
        if (engine == null) {
            return new ArrayList<>();
        }
        String key = getMethodKey(method);
        return callerCache.computeIfAbsent(key, k -> {
            String desc = normalizeDescForQuery(method.getMethodDesc());
            return engine.getCallers(
                    method.getClassName(),
                    method.getMethodName(),
                    desc);
        });
    }

    private ArrayList<MethodResult> getCalleeCached(MethodResult method) {
        if (method == null) {
            return new ArrayList<>();
        }
        CallGraphCache cache = callGraphCache;
        if (cache != null) {
            return cache.getCallees(method);
        }
        if (engine == null) {
            return new ArrayList<>();
        }
        String key = getMethodKey(method);
        return calleeCache.computeIfAbsent(key, k -> {
            String desc = normalizeDescForQuery(method.getMethodDesc());
            return engine.getCallee(
                    method.getClassName(),
                    method.getMethodName(),
                    desc);
        });
    }

    private boolean isServletEntry(MethodResult method) {
        if (method == null) {
            return false;
        }
        String name = method.getMethodName();
        String desc = method.getMethodDesc();
        if (name == null || desc == null) {
            return false;
        }
        if (!("doGet".equals(name) || "doPost".equals(name) || "doPut".equals(name)
                || "doDelete".equals(name) || "doHead".equals(name)
                || "doOptions".equals(name) || "doTrace".equals(name)
                || "service".equals(name))) {
            return false;
        }
        return "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V".equals(desc)
                || "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V".equals(desc)
                || "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)V".equals(desc)
                || "(Ljakarta/servlet/ServletRequest;Ljakarta/servlet/ServletResponse;)V".equals(desc);
    }

    private Set<String> buildKnownSourceKeys() {
        if (engine == null) {
            return Collections.emptySet();
        }
        Set<String> keys = new HashSet<>();
        ArrayList<ClassResult> springC = engine.getAllSpringC();
        for (ClassResult cr : springC) {
            for (MethodResult method : engine.getSpringM(cr.getClassName())) {
                keys.add(getMethodKey(method));
            }
        }
        ArrayList<ClassResult> servlets = engine.getAllServlets();
        for (ClassResult cr : servlets) {
            for (MethodResult method : engine.getMethodsByClass(cr.getClassName())) {
                if (isServletEntry(method)) {
                    keys.add(getMethodKey(method));
                }
            }
        }
        List<String> annoSources = ModelRegistry.getSourceAnnotations();
        if (annoSources != null && !annoSources.isEmpty()) {
            for (MethodResult method : engine.getMethodsByAnnoNames(annoSources)) {
                keys.add(getMethodKey(method));
            }
        }
        List<SourceModel> sourceModels = ModelRegistry.getSourceModels();
        if (sourceModels != null && !sourceModels.isEmpty()) {
            for (SourceModel model : sourceModels) {
                for (MethodResult method : resolveSourceModel(model)) {
                    keys.add(getMethodKey(method));
                }
            }
        }
        return keys;
    }

    private Set<String> buildSourceKeySet(List<MethodResult> methods) {
        if (methods == null || methods.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> keys = new HashSet<>();
        for (MethodResult method : methods) {
            if (method == null) {
                continue;
            }
            keys.add(getMethodKey(method));
        }
        return keys;
    }

    private boolean shouldOutputRootSource(MethodResult method) {
        return method != null;
    }

    private boolean isThirdPartyRoot(MethodResult method) {
        if (method == null) {
            return false;
        }
        String key = getMethodKey(method);
        if (knownSourceKeys != null && knownSourceKeys.contains(key)) {
            return false;
        }
        String jarName = method.getJarName();
        return CommonFilterUtil.isFilteredJar(jarName);
    }

    private boolean isTargetMethod(MethodResult method, String targetClass, String targetMethod, String targetDesc) {
        if (targetClass == null || targetMethod == null) {
            return false;
        }
        return method.getClassName().equals(targetClass) &&
                method.getMethodName().equals(targetMethod) &&
                (isAnyDesc(targetDesc) || method.getMethodDesc().equals(targetDesc));
    }

    private boolean isAnyDesc(String desc) {
        if (desc == null) {
            return true;
        }
        String v = desc.trim();
        if (v.isEmpty()) {
            return true;
        }
        return "*".equals(v) || "null".equalsIgnoreCase(v);
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

    private MethodCallMeta getEdgeMeta(MethodReference.Handle caller, MethodReference.Handle callee) {
        MethodCallKey key = MethodCallKey.of(caller, callee);
        if (key == null) {
            return null;
        }
        MethodCallMeta cached = edgeMetaCache.get(key);
        if (cached != null) {
            return cached;
        }
        MethodCallMeta meta = null;
        CallGraphCache cache = callGraphCache;
        if (cache != null) {
            meta = cache.getEdgeMeta(caller, callee);
        } else if (engine != null) {
            meta = engine.getEdgeMeta(caller, callee);
        }
        if (meta != null) {
            edgeMetaCache.put(key, meta);
        }
        return meta;
    }

    private boolean isEdgeAllowed(MethodResult from, MethodResult to) {
        if (!edgeConfidenceFilterEnabled) {
            return true;
        }
        if (from == null || to == null) {
            return false;
        }
        MethodCallMeta meta = getEdgeMeta(convertToHandle(from), convertToHandle(to));
        String conf = meta == null ? MethodCallMeta.CONF_LOW : meta.getConfidence();
        return meetsMinConfidence(conf);
    }

    private boolean isEdgeConfidenceFilterEnabled() {
        return !MethodCallMeta.CONF_LOW.equals(normalizeConfidence(minEdgeConfidence));
    }

    private List<DFSEdge> buildEdges(List<MethodReference.Handle> handles) {
        if (handles == null || handles.size() < 2) {
            return Collections.emptyList();
        }
        if (callGraphCache != null) {
            List<DFSEdge> edges = new ArrayList<>();
            for (int i = 0; i < handles.size() - 1; i++) {
                MethodReference.Handle from = handles.get(i);
                MethodReference.Handle to = handles.get(i + 1);
                DFSEdge edge = new DFSEdge();
                edge.setFrom(from);
                edge.setTo(to);
                MethodCallMeta meta = getEdgeMeta(from, to);
                if (meta != null) {
                    edge.setType(meta.getType());
                    edge.setConfidence(meta.getConfidence());
                    edge.setEvidence(meta.getEvidence());
                } else {
                    edge.setType(MethodCallMeta.TYPE_UNKNOWN);
                    edge.setConfidence(MethodCallMeta.CONF_LOW);
                    edge.setEvidence("");
                }
                edges.add(edge);
            }
            return edges;
        }
        List<MethodCallKey> missing = new ArrayList<>();
        for (int i = 0; i < handles.size() - 1; i++) {
            MethodReference.Handle from = handles.get(i);
            MethodReference.Handle to = handles.get(i + 1);
            MethodCallKey key = MethodCallKey.of(from, to);
            if (key != null && !edgeMetaCache.containsKey(key)) {
                missing.add(key);
            }
        }
        if (!missing.isEmpty() && engine != null) {
            Map<MethodCallKey, MethodCallMeta> batch = engine.getEdgeMetaBatch(missing);
            if (batch != null && !batch.isEmpty()) {
                edgeMetaCache.putAll(batch);
            }
        }
        List<DFSEdge> edges = new ArrayList<>();
        for (int i = 0; i < handles.size() - 1; i++) {
            MethodReference.Handle from = handles.get(i);
            MethodReference.Handle to = handles.get(i + 1);
            DFSEdge edge = new DFSEdge();
            edge.setFrom(from);
            edge.setTo(to);
            MethodCallMeta meta = getEdgeMeta(from, to);
            if (meta != null) {
                edge.setType(meta.getType());
                edge.setConfidence(meta.getConfidence());
                edge.setEvidence(meta.getEvidence());
            } else {
                edge.setType(MethodCallMeta.TYPE_UNKNOWN);
                edge.setConfidence(MethodCallMeta.CONF_LOW);
                edge.setEvidence("");
            }
            edges.add(edge);
        }
        return edges;
    }

    private String buildChainSignature(List<MethodResult> path, boolean isReverse) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (isReverse) {
            for (int i = path.size() - 1; i >= 0; i--) {
                if (sb.length() > 0) {
                    sb.append("->");
                }
                sb.append(getMethodSignatureForChain(path.get(i)));
            }
        } else {
            for (MethodResult method : path) {
                if (sb.length() > 0) {
                    sb.append("->");
                }
                sb.append(getMethodSignatureForChain(method));
            }
        }
        return sb.toString();
    }

    private String getMethodSignatureForChain(MethodResult method) {
        if (method == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String jarName = method.getJarName();
        if (jarName != null && !jarName.trim().isEmpty()) {
            sb.append(jarName.trim()).append("|");
        } else if (method.getJarId() > 0) {
            sb.append(method.getJarId()).append("|");
        }
        sb.append(getMethodKey(method));
        return sb.toString();
    }

    private String resolveChainConfidence(List<DFSEdge> edges) {
        if (edges == null || edges.isEmpty()) {
            return MethodCallMeta.CONF_LOW;
        }
        int best = Integer.MAX_VALUE;
        String bestLabel = MethodCallMeta.CONF_LOW;
        for (DFSEdge edge : edges) {
            String conf = edge == null ? null : edge.getConfidence();
            int score = confidenceScore(conf);
            if (score > 0 && score < best) {
                best = score;
                bestLabel = conf;
            }
        }
        return best == Integer.MAX_VALUE ? MethodCallMeta.CONF_LOW : bestLabel;
    }

    private boolean meetsMinConfidence(String confidence) {
        int current = confidenceScore(normalizeConfidence(confidence));
        int min = confidenceScore(normalizeConfidence(minEdgeConfidence));
        return current >= min;
    }

    private String normalizeConfidence(String confidence) {
        if (confidence == null) {
            return MethodCallMeta.CONF_LOW;
        }
        String v = confidence.trim().toLowerCase();
        if (MethodCallMeta.CONF_HIGH.equals(v)) {
            return MethodCallMeta.CONF_HIGH;
        }
        if (MethodCallMeta.CONF_MEDIUM.equals(v)) {
            return MethodCallMeta.CONF_MEDIUM;
        }
        if (MethodCallMeta.CONF_LOW.equals(v)) {
            return MethodCallMeta.CONF_LOW;
        }
        return MethodCallMeta.CONF_LOW;
    }

    private int confidenceScore(String confidence) {
        if (MethodCallMeta.CONF_HIGH.equals(confidence)) {
            return 3;
        }
        if (MethodCallMeta.CONF_MEDIUM.equals(confidence)) {
            return 2;
        }
        if (MethodCallMeta.CONF_LOW.equals(confidence)) {
            return 1;
        }
        return 0;
    }

    private void outputChain(List<MethodResult> path, boolean isReverse) {
        if (stopNow()) {
            return;
        }
        String signature = buildChainSignature(path, isReverse);
        if (!chainKeySet.add(signature)) {
            return;
        }
        List<MethodReference.Handle> handles = convertPathToHandles(path, isReverse);
        List<DFSEdge> edges = buildEdges(handles);
        String conf = resolveChainConfidence(edges);
        if (!meetsMinConfidence(conf)) {
            return;
        }
        int chainIndex = chainCount.incrementAndGet();
        String chainId = "chain_" + chainIndex;

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

        String title = "调用链 #" + chainIndex + " (长度: " + path.size() + ", 置信度: " + conf + ")";

        addChain(chainId, title, methods, edges);

        // 新增：保存结果到 DFSResult
        DFSResult result = new DFSResult();
        result.setMethodList(handles);
        result.setEdges(edges);
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
        resultCount.incrementAndGet();
        stopNow();
    }

    private void outputSourceChain(List<MethodResult> path, MethodResult sourceMethod) {
        if (stopNow()) {
            return;
        }
        String signature = buildChainSignature(path, true);
        if (!chainKeySet.add(signature)) {
            return;
        }
        List<MethodReference.Handle> handles = convertPathToHandles(path, true);
        List<DFSEdge> edges = buildEdges(handles);
        String conf = resolveChainConfidence(edges);
        if (!meetsMinConfidence(conf)) {
            return;
        }
        int sourceIndex = sourceCount.incrementAndGet();
        String chainId = "source_" + sourceIndex;

        List<String> methods = new ArrayList<>();

        // 反向搜索时，路径需要反转输出（从SOURCE到SINK）
        for (int i = path.size() - 1; i >= 0; i--) {
            MethodResult method = path.get(i);
            methods.add(formatMethod(method));
        }

        String thirdPartyMark = isThirdPartyRoot(sourceMethod) ? " [third-party]" : "";
        String title = " (调用链长度: " + path.size() + ", 置信度: " + conf + ")" + thirdPartyMark
                + " #" + sourceIndex + ": " + formatMethod(sourceMethod);

        addChain(chainId, title, methods, edges);

        // 新增：保存结果到 DFSResult
        DFSResult result = new DFSResult();
        result.setMethodList(handles); // 反向输出
        result.setEdges(edges);
        result.setDepth(path.size());
        result.setMode(DFSResult.FROM_SOURCE_TO_ALL);

        // 设置 source 和 sink
        if (!path.isEmpty()) {
            result.setSource(convertToHandle(sourceMethod));
            result.setSink(convertToHandle(path.get(0))); // sink 是路径的起点
        }

        results.add(result);
        resultCount.incrementAndGet();
        stopNow();
    }

    private String formatMethod(MethodResult method) {
        return method.getClassName() + "." + method.getMethodName() + method.getMethodDesc();
    }

    private List<MethodResult> resolveSourceModel(SourceModel model) {
        if (model == null || engine == null) {
            return Collections.emptyList();
        }
        String className = model.getClassName();
        String methodName = model.getMethodName();
        if (className == null || className.trim().isEmpty() ||
                methodName == null || methodName.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String cls = className.replace('.', '/').trim();
        String desc = model.getMethodDesc();
        if (desc == null || desc.trim().isEmpty() || "*".equals(desc.trim())) {
            return engine.getMethod(cls, methodName, "");
        }
        return engine.getMethod(cls, methodName, desc);
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
        chainCount.set(0);
        sourceCount.set(0);
        resultCount.set(0);
        chainKeySet.clear();
        callerCache.clear();
        calleeCache.clear();
        edgeMetaCache.clear();
        knownSourceKeys = Collections.emptySet();
    }

    /**
     * 获取结果数量
     *
     * @return 找到的调用链数量
     */
    public int getResultCount() {
        return resultCount.get();
    }
}
