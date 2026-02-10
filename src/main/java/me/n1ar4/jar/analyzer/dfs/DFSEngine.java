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

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.CallGraphCache;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.rules.SourceModel;
import me.n1ar4.jar.analyzer.taint.summary.CallFlow;
import me.n1ar4.jar.analyzer.taint.summary.GlobalPropagator;
import me.n1ar4.jar.analyzer.taint.summary.FlowPort;
import me.n1ar4.jar.analyzer.taint.summary.MethodSummary;
import me.n1ar4.jar.analyzer.taint.summary.ReachabilitySerde;
import me.n1ar4.jar.analyzer.taint.summary.ReachabilityIndex;
import me.n1ar4.jar.analyzer.taint.summary.SummaryEngine;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;
import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.jar.analyzer.utils.StableOrder;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.BitSet;
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
import java.util.concurrent.atomic.AtomicReference;

public class DFSEngine {
    private static final Logger logger = LogManager.getLogger();
    private static final String CACHE_TYPE_REACHABILITY = "reachability";
    private static final String PROP_REACHABILITY_DB_CACHE = "jar.analyzer.dfs.reachability.cache.db";
    private static final String PROP_REACHABILITY_DB_CACHE_MAX = "jar.analyzer.dfs.reachability.cache.db.maxEntries";
    private static final int REACHABILITY_DB_CACHE_MAX_DEFAULT = 200_000;
    private static final int REACHABILITY_DB_CACHE_MAX_CHARS = 8 * 1024 * 1024;
    private static final String PROP_STATEFUL_PRUNE = "jar.analyzer.dfs.statefulPrune";
    private static final String PROP_STATEFUL_PRUNE_AGGRESSIVE = "jar.analyzer.dfs.statefulPrune.aggressive";

    private String sinkClass;
    private String sinkMethod;
    private String sinkDesc;
    private String sourceClass;
    private String sourceMethod;
    private String sourceDesc;

    private final CoreEngine engine;
    private final DfsOutput output;
    private final Map<MethodCallKey, MethodCallMeta> edgeMetaCache = new ConcurrentHashMap<>();
    private final Map<String, ArrayList<MethodResult>> callerCache = new ConcurrentHashMap<>();
    private final Map<String, ArrayList<MethodResult>> calleeCache = new ConcurrentHashMap<>();
    private final Set<String> chainKeySet = ConcurrentHashMap.newKeySet();
    private CallGraphCache callGraphCache;
    private ReachabilityIndex reachabilityIndex;

    private final boolean fromSink;
    private final boolean searchNullSource;
    private final int depth;
    private boolean onlyFromWeb = false;
    private Set<String> knownSourceKeys = Collections.emptySet();
    private boolean reachabilityForFindAllSources = false;
    private boolean edgeConfidenceFilterEnabled = false;
    private Boolean summaryEnabledOverride = null;
    private Boolean statefulPruneOverride = null;
    private Boolean aggressiveStatefulPruneOverride = null;
    private boolean statefulPruneEnabled = false;
    private boolean aggressiveStatefulPrune = false;
    private volatile SummaryEngine statefulSummaryEngine;

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

    public void setStatefulPrune(Boolean enabled) {
        this.statefulPruneOverride = enabled;
    }

    public void setAggressiveStatefulPrune(Boolean aggressive) {
        this.aggressiveStatefulPruneOverride = aggressive;
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

    public void cancel() {
        this.canceled.set(true);
    }

    private void clean() {
        try {
            output.clear();
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            InterruptUtil.restoreInterruptIfNeeded(t);
            logger.debug("dfs output clear failed: {}", t.toString());
        }
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

    private boolean isReachabilityDbCacheEnabled() {
        String raw = System.getProperty(PROP_REACHABILITY_DB_CACHE);
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        return !"false".equalsIgnoreCase(raw.trim());
    }

    private int resolveReachabilityDbMaxEntries() {
        String raw = System.getProperty(PROP_REACHABILITY_DB_CACHE_MAX);
        if (raw == null || raw.trim().isEmpty()) {
            return REACHABILITY_DB_CACHE_MAX_DEFAULT;
        }
        try {
            int v = Integer.parseInt(raw.trim());
            return v <= 0 ? REACHABILITY_DB_CACHE_MAX_DEFAULT : v;
        } catch (NumberFormatException ex) {
            return REACHABILITY_DB_CACHE_MAX_DEFAULT;
        }
    }

    private ReachabilityIndex loadReachabilityFromDb(String cacheKey) {
        try {
            String value = DatabaseManager.getSemanticCacheValue(cacheKey, CACHE_TYPE_REACHABILITY);
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            return ReachabilitySerde.fromCacheValue(value);
        } catch (Exception ex) {
            logger.debug("load reachability cache failed: {}", ex.toString());
            return null;
        }
    }

    private void persistReachabilityToDb(String cacheKey, ReachabilityIndex index) {
        if (cacheKey == null || index == null) {
            return;
        }
        int maxEntries = resolveReachabilityDbMaxEntries();
        int total = index.getReachableToSink().size() + index.getReachableFromSource().size();
        if (total <= 0 || total > maxEntries) {
            logger.debug("skip persist reachability cache: total={} max={}", total, maxEntries);
            return;
        }
        String value = ReachabilitySerde.toCacheValue(index);
        if (value == null || value.isEmpty() || value.length() > REACHABILITY_DB_CACHE_MAX_CHARS) {
            logger.debug("skip persist reachability cache: encodedSize={}", value == null ? -1 : value.length());
            return;
        }
        try {
            DatabaseManager.putSemanticCacheValue(cacheKey, CACHE_TYPE_REACHABILITY, value);
        } catch (Exception ex) {
            logger.debug("persist reachability cache failed: {}", ex.toString());
        }
    }

    private String buildReachabilityCacheKey(boolean findAllSources) {
        long buildSeq = DatabaseManager.getBuildSeq();
        String sink = sinkClass + "#" + sinkMethod + "#" + (sinkDesc == null ? "" : sinkDesc);
        String source = findAllSources ? "" : (sourceClass + "#" + sourceMethod + "#" + (sourceDesc == null ? "" : sourceDesc));
        String minConf = normalizeConfidence(minEdgeConfidence);
        String blacklistFp = fingerprintSet(blacklist);
        return buildSeq
                + "|sink=" + sink
                + "|source=" + source
                + "|fromSink=" + fromSink
                + "|findAllSources=" + findAllSources
                + "|onlyFromWeb=" + onlyFromWeb
                + "|depth=" + depth
                + "|minConf=" + minConf
                + "|black=" + blacklistFp;
    }

    private static String fingerprintSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return "0";
        }
        List<String> list = new ArrayList<>(values);
        Collections.sort(list);
        int h = 1;
        for (String s : list) {
            if (s != null) {
                h = 31 * h + s.hashCode();
            }
        }
        return list.size() + ":" + Integer.toHexString(h);
    }

    private boolean resolveStatefulPruneEnabled() {
        if (statefulPruneOverride != null) {
            return statefulPruneOverride;
        }
        String raw = System.getProperty(PROP_STATEFUL_PRUNE);
        if (raw == null || raw.trim().isEmpty()) {
            return false;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    private boolean resolveAggressiveStatefulPruneEnabled() {
        if (aggressiveStatefulPruneOverride != null) {
            return aggressiveStatefulPruneOverride;
        }
        String raw = System.getProperty(PROP_STATEFUL_PRUNE_AGGRESSIVE);
        if (raw == null || raw.trim().isEmpty()) {
            return false;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    private SummaryEngine summaryEngine() {
        SummaryEngine engine = statefulSummaryEngine;
        if (engine != null) {
            return engine;
        }
        synchronized (this) {
            if (statefulSummaryEngine == null) {
                statefulSummaryEngine = new SummaryEngine();
            }
            return statefulSummaryEngine;
        }
    }

    private PortState initialPortState(MethodResult method) {
        return allInputPorts(method, false);
    }

    private PortState allInputPorts(MethodResult method, boolean uncertain) {
        if (method == null) {
            return PortState.any();
        }
        String desc = method.getMethodDesc();
        if (isAnyDesc(desc)) {
            return PortState.any();
        }
        try {
            int paramCount = Type.getArgumentTypes(desc).length;
            BitSet bits = new BitSet(paramCount + 1);
            bits.set(0); // receiver/this (even for static methods; harmless and keeps state compact)
            if (paramCount > 0) {
                bits.set(1, paramCount + 1);
            }
            return new PortState(bits, uncertain, false);
        } catch (Exception ignored) {
            return PortState.any();
        }
    }

    private boolean isPrunableEdge(MethodCallMeta meta) {
        if (meta == null) {
            return false;
        }
        String type = meta.getType();
        if (!MethodCallMeta.TYPE_DIRECT.equals(type)) {
            return false;
        }
        String conf = normalizeConfidence(meta.getConfidence());
        return !MethodCallMeta.CONF_LOW.equals(conf);
    }

    private PortState nextStateForward(MethodResult from, MethodResult to, PortState state) {
        if (from == null || to == null || state == null) {
            return PortState.any();
        }
        MethodReference.Handle fromHandle = convertToHandle(from);
        MethodReference.Handle toHandle = convertToHandle(to);
        MethodCallMeta meta = getEdgeMeta(fromHandle, toHandle);
        if (!isPrunableEdge(meta) || state.anyPorts) {
            return allInputPorts(to, true);
        }
        MethodSummary summary = summaryEngine().getSummary(fromHandle);
        if (summary == null || summary.isUnknown()) {
            return allInputPorts(to, true);
        }
        BitSet next = new BitSet();
        boolean sawLow = false;
        boolean matched = false;
        for (CallFlow flow : summary.getCallFlows()) {
            if (flow == null || flow.getCallee() == null || flow.getFrom() == null || flow.getTo() == null) {
                continue;
            }
            if (!flow.getCallee().equals(toHandle)) {
                continue;
            }
            if (!state.matches(flow.getFrom())) {
                continue;
            }
            matched = true;
            int idx = portBitIndex(flow.getTo());
            if (idx < 0) {
                return allInputPorts(to, true);
            }
            next.set(idx);
            if (isLow(flow.getConfidence())) {
                sawLow = true;
            }
        }
        if (!matched || next.isEmpty()) {
            if (aggressiveStatefulPrune) {
                return null;
            }
            if (!state.uncertain) {
                return null;
            }
            return allInputPorts(to, true);
        }
        return new PortState(next, state.uncertain || sawLow, false);
    }

    private PortState nextStateBackward(MethodResult caller, MethodResult callee, PortState calleeState) {
        if (caller == null || callee == null || calleeState == null) {
            return PortState.any();
        }
        MethodReference.Handle callerHandle = convertToHandle(caller);
        MethodReference.Handle calleeHandle = convertToHandle(callee);
        MethodCallMeta meta = getEdgeMeta(callerHandle, calleeHandle);
        if (!isPrunableEdge(meta) || calleeState.anyPorts) {
            return allInputPorts(caller, true);
        }
        MethodSummary summary = summaryEngine().getSummary(callerHandle);
        if (summary == null || summary.isUnknown()) {
            return allInputPorts(caller, true);
        }
        BitSet next = new BitSet();
        boolean sawLow = false;
        boolean matched = false;
        for (CallFlow flow : summary.getCallFlows()) {
            if (flow == null || flow.getCallee() == null || flow.getFrom() == null || flow.getTo() == null) {
                continue;
            }
            if (!flow.getCallee().equals(calleeHandle)) {
                continue;
            }
            if (!calleeState.matches(flow.getTo())) {
                continue;
            }
            matched = true;
            int idx = portBitIndex(flow.getFrom());
            if (idx < 0) {
                return allInputPorts(caller, true);
            }
            next.set(idx);
            if (isLow(flow.getConfidence())) {
                sawLow = true;
            }
        }
        if (!matched || next.isEmpty()) {
            if (aggressiveStatefulPrune) {
                return null;
            }
            if (!calleeState.uncertain) {
                return null;
            }
            return allInputPorts(caller, true);
        }
        return new PortState(next, calleeState.uncertain || sawLow, false);
    }

    private static boolean isLow(String confidence) {
        if (confidence == null) {
            return false;
        }
        return "low".equalsIgnoreCase(confidence.trim());
    }

    private static int portBitIndex(FlowPort port) {
        if (port == null || port.getKind() == null) {
            return -1;
        }
        switch (port.getKind()) {
            case THIS:
                return 0;
            case PARAM:
                return port.getIndex() >= 0 ? (port.getIndex() + 1) : -1;
            default:
                return -1;
        }
    }

    private static final class PortState {
        private final BitSet ports;
        private final boolean uncertain;
        private final boolean anyPorts;

        private PortState(BitSet ports, boolean uncertain, boolean anyPorts) {
            this.ports = ports == null ? new BitSet() : (BitSet) ports.clone();
            this.uncertain = uncertain;
            this.anyPorts = anyPorts;
        }

        static PortState any() {
            return new PortState(new BitSet(), true, true);
        }

        boolean matches(FlowPort port) {
            if (anyPorts) {
                return portBitIndex(port) >= 0;
            }
            int idx = portBitIndex(port);
            return idx >= 0 && ports.get(idx);
        }
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

    /**
     * 更新结果显示区域
     */
    private void update(String msg) {
        try {
            output.onMessage(msg);
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            InterruptUtil.restoreInterruptIfNeeded(t);
            logger.debug("dfs output message failed: {}", t.toString());
        }
    }

    /**
     * 添加调用链到结果面板
     */
    private void addChain(String chainId, String title, List<String> methods, List<DFSEdge> edges) {
        try {
            output.onChainFound(chainId, title, methods, edges, showEdgeMeta);
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            InterruptUtil.restoreInterruptIfNeeded(t);
            logger.debug("dfs output chain failed: {}", t.toString());
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
        if (Thread.currentThread().isInterrupted()) {
            canceled.set(true);
            markTruncated("canceled");
            return true;
        }
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
            DfsOutput output,
            boolean fromSink,
            boolean searchNullSource,
            int depth) {
        this.engine = EngineContext.getEngine();
        this.output = output == null ? DfsOutputs.noop() : output;
        this.fromSink = fromSink;
        this.searchNullSource = searchNullSource;
        this.depth = depth;
    }

    public void setOnlyFromWeb(boolean onlyFromWeb) {
        this.onlyFromWeb = onlyFromWeb;
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
        boolean onlyFromWeb = this.onlyFromWeb;
        reachabilityForFindAllSources = findAllSources && onlyFromWeb;
        edgeConfidenceFilterEnabled = isEdgeConfidenceFilterEnabled();
        statefulPruneEnabled = resolveStatefulPruneEnabled() && isSummaryEnabled();
        aggressiveStatefulPrune = resolveAggressiveStatefulPruneEnabled();
        if (statefulPruneEnabled) {
            update("stateful prune: on (aggressive=" + aggressiveStatefulPrune + ")");
        }

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
        chainCount.set(0);
        sourceCount.set(0);
        reachabilityIndex = null;
        if (callGraphCache != null && engine != null && isSummaryEnabled()) {
            try {
                String cacheKey = buildReachabilityCacheKey(findAllSources);
                if (isReachabilityDbCacheEnabled() && cacheKey != null) {
                    ReachabilityIndex cached = loadReachabilityFromDb(cacheKey);
                    if (cached != null) {
                        reachabilityIndex = cached;
                        update("summary reachability (db cache): toSink=" + reachabilityIndex.getReachableToSink().size()
                                + " fromSource=" + reachabilityIndex.getReachableFromSource().size());
                    }
                }
                if (reachabilityIndex == null) {
                    Set<MethodReference.Handle> extraSources = buildExtraSources(findAllSources);
                    Set<MethodReference.Handle> extraSinks = buildExtraSinks();
                    reachabilityIndex = new GlobalPropagator().propagate(engine, callGraphCache, extraSources, extraSinks);
                    update("summary reachability: toSink=" + reachabilityIndex.getReachableToSink().size()
                            + " fromSource=" + reachabilityIndex.getReachableFromSource().size());
                    if (isReachabilityDbCacheEnabled() && cacheKey != null) {
                        persistReachabilityToDb(cacheKey, reachabilityIndex);
                    }
                }
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
                PortState startState = statefulPruneEnabled ? initialPortState(startMethod) : null;

                if (findAllSources) {
                    if (!onlyFromWeb) {
                        knownSourceKeys = buildKnownSourceKeys();
                        if (parallelEnabled) {
                            pool.invoke(new SinkAllTask(startMethod, path, visited, 0, startState));
                        } else {
                            if (statefulPruneEnabled && startState != null) {
                                dfsFromSinkFindAllSourcesStateful(startMethod, path, visited, 0, startState);
                            } else {
                                dfsFromSinkFindAllSources(startMethod, path, visited, 0);
                            }
                        }
                    } else {
                        if (engine == null) {
                            return;
                        }
                        ArrayList<ClassResult> springC = engine.getAllSpringC();
                        ArrayList<MethodResult> webSources = new ArrayList<>();
                        for (ClassResult cr : springC) {
                            webSources.addAll(engine.getSpringM(cr.getClassName()));
                        }
                        ArrayList<ClassResult> servlets = engine.getAllServlets();
                        for (ClassResult cr : servlets) {
                            for (MethodResult method : engine.getMethodsByClass(cr.getClassName())) {
                                if (isServletEntry(method)) {
                                    webSources.add(method);
                                }
                            }
                        }
                        List<String> annoSources = ModelRegistry.getSourceAnnotations();
                        if (annoSources != null && !annoSources.isEmpty()) {
                            webSources.addAll(engine.getMethodsByAnnoNames(annoSources));
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
                                    webSourceKeys, newConcurrentSourceSet(), startState));
                        } else {
                            if (statefulPruneEnabled && startState != null) {
                                dfsFromSinkFindWebSourcesOnceStateful(startMethod, path, visited, 0,
                                        webSourceKeys, new HashSet<>(), startState);
                            } else {
                                dfsFromSinkFindWebSourcesOnce(startMethod, path, visited, 0,
                                        webSourceKeys, new HashSet<>());
                            }
                        }
                    }
                } else {
                    if (parallelEnabled) {
                        pool.invoke(new SinkTask(startMethod, path, visited, 0, startState));
                    } else {
                        if (statefulPruneEnabled && startState != null) {
                            dfsFromSinkStateful(startMethod, path, visited, 0, startState);
                        } else {
                            dfsFromSink(startMethod, path, visited, 0);
                        }
                    }
                }
            } else {
                update("从 SOURCE 开始正向分析");
                MethodResult startMethod = new MethodResult(sourceClass, sourceMethod, sourceDesc);
                List<MethodResult> path = new ArrayList<>();
                path.add(startMethod);
                Set<String> visited = new HashSet<>();
                PortState startState = statefulPruneEnabled ? initialPortState(startMethod) : null;
                if (parallelEnabled) {
                    pool.invoke(new SourceTask(startMethod, path, visited, 0, startState));
                } else {
                    if (statefulPruneEnabled && startState != null) {
                        dfsFromSourceStateful(startMethod, path, visited, 0, startState);
                    } else {
                        dfsFromSource(startMethod, path, visited, 0);
                    }
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
        }
    }

    private boolean shouldFork(int currentDepth) {
        return parallelEnabled && pool != null && currentDepth < parallelDepth;
    }

    private Set<String> newConcurrentSourceSet() {
        return ConcurrentHashMap.newKeySet();
    }

    private void dfsFromSinkFindWebSourcesOnceStateful(
            MethodResult currentMethod,
            List<MethodResult> path,
            Set<String> visited,
            int currentDepth,
            Set<String> webSourceKeys,
            Set<String> foundSources,
            PortState state) {

        if (stopNow()) {
            return;
        }
        if (currentDepth >= depth) {
            return;
        }
        if (currentMethod == null) {
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
                PortState nextState = nextStateBackward(caller, currentMethod, state);
                if (nextState == null) {
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
                        currentDepth + 1, webSourceKeys, foundSources, nextState));
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
                PortState nextState = nextStateBackward(caller, currentMethod, state);
                if (nextState == null) {
                    continue;
                }
                if (tickEdge()) {
                    visited.remove(methodKey);
                    return;
                }
                path.add(caller);
                dfsFromSinkFindWebSourcesOnceStateful(caller, path, visited, currentDepth + 1,
                        webSourceKeys, foundSources, nextState);
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

    private void dfsFromSinkStateful(
            MethodResult currentMethod,
            List<MethodResult> path,
            Set<String> visited,
            int currentDepth,
            PortState state) {
        if (stopNow()) {
            return;
        }
        if (currentDepth >= depth) {
            return;
        }
        if (currentMethod == null) {
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
                PortState nextState = nextStateBackward(caller, currentMethod, state);
                if (nextState == null) {
                    continue;
                }
                if (tickEdge()) {
                    visited.remove(methodKey);
                    return;
                }
                List<MethodResult> nextPath = new ArrayList<>(path);
                nextPath.add(caller);
                Set<String> nextVisited = new HashSet<>(visited);
                tasks.add(new SinkTask(caller, nextPath, nextVisited, currentDepth + 1, nextState));
            }
            ForkJoinTask.invokeAll(tasks);
        } else {
            for (MethodResult caller : callerMethods) {
                if (!isEdgeAllowed(caller, currentMethod)) {
                    continue;
                }
                PortState nextState = nextStateBackward(caller, currentMethod, state);
                if (nextState == null) {
                    continue;
                }
                if (tickEdge()) {
                    visited.remove(methodKey);
                    return;
                }
                path.add(caller);
                dfsFromSinkStateful(caller, path, visited, currentDepth + 1, nextState);
                path.remove(path.size() - 1);
            }
        }

        visited.remove(methodKey);
    }

    private void dfsFromSinkFindAllSourcesStateful(
            MethodResult currentMethod,
            List<MethodResult> path,
            Set<String> visited,
            int currentDepth,
            PortState state) {
        if (stopNow()) {
            return;
        }
        if (currentDepth >= depth) {
            return;
        }
        if (currentMethod == null) {
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
                    PortState nextState = nextStateBackward(caller, currentMethod, state);
                    if (nextState == null) {
                        continue;
                    }
                    if (tickEdge()) {
                        visited.remove(methodKey);
                        return;
                    }
                    List<MethodResult> nextPath = new ArrayList<>(path);
                    nextPath.add(caller);
                    Set<String> nextVisited = new HashSet<>(visited);
                    tasks.add(new SinkAllTask(caller, nextPath, nextVisited, currentDepth + 1, nextState));
                }
                ForkJoinTask.invokeAll(tasks);
            } else {
                for (MethodResult caller : callerMethods) {
                    if (!isEdgeAllowed(caller, currentMethod)) {
                        continue;
                    }
                    PortState nextState = nextStateBackward(caller, currentMethod, state);
                    if (nextState == null) {
                        continue;
                    }
                    if (tickEdge()) {
                        visited.remove(methodKey);
                        return;
                    }
                    path.add(caller);
                    dfsFromSinkFindAllSourcesStateful(caller, path, visited, currentDepth + 1, nextState);
                    path.remove(path.size() - 1);
                }
            }
        }

        visited.remove(methodKey);
    }

    private void dfsFromSourceStateful(
            MethodResult currentMethod,
            List<MethodResult> path,
            Set<String> visited,
            int currentDepth,
            PortState state) {
        if (stopNow()) {
            return;
        }
        if (currentDepth >= depth) {
            return;
        }
        if (currentMethod == null) {
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
                PortState nextState = nextStateForward(currentMethod, callee, state);
                if (nextState == null) {
                    continue;
                }
                if (tickEdge()) {
                    visited.remove(methodKey);
                    return;
                }
                List<MethodResult> nextPath = new ArrayList<>(path);
                nextPath.add(callee);
                Set<String> nextVisited = new HashSet<>(visited);
                tasks.add(new SourceTask(callee, nextPath, nextVisited, currentDepth + 1, nextState));
            }
            ForkJoinTask.invokeAll(tasks);
        } else {
            for (MethodResult callee : calleeMethods) {
                if (!isEdgeAllowed(currentMethod, callee)) {
                    continue;
                }
                PortState nextState = nextStateForward(currentMethod, callee, state);
                if (nextState == null) {
                    continue;
                }
                if (tickEdge()) {
                    visited.remove(methodKey);
                    return;
                }
                path.add(callee);
                dfsFromSourceStateful(callee, path, visited, currentDepth + 1, nextState);
                path.remove(path.size() - 1);
            }
        }

        visited.remove(methodKey);
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
        private final PortState state;

        private SinkTask(MethodResult current, List<MethodResult> path, Set<String> visited, int depth) {
            this(current, path, visited, depth, null);
        }

        private SinkTask(MethodResult current, List<MethodResult> path, Set<String> visited, int depth, PortState state) {
            this.current = current;
            this.path = path;
            this.visited = visited;
            this.depth = depth;
            this.state = state;
        }

        @Override
        protected void compute() {
            if (statefulPruneEnabled && state != null) {
                dfsFromSinkStateful(current, path, visited, depth, state);
            } else {
                dfsFromSink(current, path, visited, depth);
            }
        }
    }

    private class SourceTask extends RecursiveAction {
        private final MethodResult current;
        private final List<MethodResult> path;
        private final Set<String> visited;
        private final int depth;
        private final PortState state;

        private SourceTask(MethodResult current, List<MethodResult> path, Set<String> visited, int depth) {
            this(current, path, visited, depth, null);
        }

        private SourceTask(MethodResult current, List<MethodResult> path, Set<String> visited, int depth, PortState state) {
            this.current = current;
            this.path = path;
            this.visited = visited;
            this.depth = depth;
            this.state = state;
        }

        @Override
        protected void compute() {
            if (statefulPruneEnabled && state != null) {
                dfsFromSourceStateful(current, path, visited, depth, state);
            } else {
                dfsFromSource(current, path, visited, depth);
            }
        }
    }

    private class SinkAllTask extends RecursiveAction {
        private final MethodResult current;
        private final List<MethodResult> path;
        private final Set<String> visited;
        private final int depth;
        private final PortState state;

        private SinkAllTask(MethodResult current, List<MethodResult> path, Set<String> visited, int depth) {
            this(current, path, visited, depth, null);
        }

        private SinkAllTask(MethodResult current, List<MethodResult> path, Set<String> visited, int depth, PortState state) {
            this.current = current;
            this.path = path;
            this.visited = visited;
            this.depth = depth;
            this.state = state;
        }

        @Override
        protected void compute() {
            if (statefulPruneEnabled && state != null) {
                dfsFromSinkFindAllSourcesStateful(current, path, visited, depth, state);
            } else {
                dfsFromSinkFindAllSources(current, path, visited, depth);
            }
        }
    }

    private class SinkWebTask extends RecursiveAction {
        private final MethodResult current;
        private final List<MethodResult> path;
        private final Set<String> visited;
        private final int depth;
        private final Set<String> webSourceKeys;
        private final Set<String> foundSources;
        private final PortState state;

        private SinkWebTask(MethodResult current,
                            List<MethodResult> path,
                            Set<String> visited,
                            int depth,
                            Set<String> webSourceKeys,
                            Set<String> foundSources) {
            this(current, path, visited, depth, webSourceKeys, foundSources, null);
        }

        private SinkWebTask(MethodResult current,
                            List<MethodResult> path,
                            Set<String> visited,
                            int depth,
                            Set<String> webSourceKeys,
                            Set<String> foundSources,
                            PortState state) {
            this.current = current;
            this.path = path;
            this.visited = visited;
            this.depth = depth;
            this.webSourceKeys = webSourceKeys;
            this.foundSources = foundSources;
            this.state = state;
        }

        @Override
        protected void compute() {
            if (statefulPruneEnabled && state != null) {
                dfsFromSinkFindWebSourcesOnceStateful(current, path, visited, depth, webSourceKeys, foundSources, state);
            } else {
                dfsFromSinkFindWebSourcesOnce(current, path, visited, depth, webSourceKeys, foundSources);
            }
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
        List<DFSResult> out = new ArrayList<>(results);
        out.sort(StableOrder.DFS_RESULT);
        return out;
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
