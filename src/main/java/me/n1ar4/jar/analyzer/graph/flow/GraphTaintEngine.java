/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.flow;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.dfs.DFSEdge;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphTraversalRules;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.rules.PruningPolicy;
import me.n1ar4.jar.analyzer.rules.SourceRuleSupport;
import me.n1ar4.jar.analyzer.taint.SinkKindResolver;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import me.n1ar4.jar.analyzer.utils.StableOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GraphTaintEngine {
    private final GraphDfsEngine dfsEngine = new GraphDfsEngine();
    private final GraphPrunedPathEngine prunedPathEngine = new GraphPrunedPathEngine();
    private final GraphTaintSemantics semantics = new GraphTaintSemantics();

    public TaintRun track(GraphSnapshot snapshot, FlowOptions options, AtomicBoolean cancelFlag) {
        if (snapshot == null || snapshot.getNodeCount() <= 0 || snapshot.getEdgeCount() <= 0) {
            return new TaintRun(List.of(), FlowStats.empty());
        }
        FlowOptions base = options == null ? FlowOptions.builder().build() : options;
        SearchRun searchRun = runSearch(snapshot, base, cancelFlag);
        AnalysisResult analyzed = analyzeInternal(
                searchRun.results(),
                base.getTimeoutMs(),
                base.getMaxPaths(),
                cancelFlag,
                null,
                searchRun.mode(),
                searchRun.backend()
        );
        FlowTruncation truncation = mergeTruncation(
                searchRun.stats() == null ? FlowTruncation.none() : searchRun.stats().getTruncation(),
                analyzed.truncation()
        );
        FlowStats searchStats = searchRun.stats() == null ? FlowStats.empty() : searchRun.stats();
        FlowStats stats = new FlowStats(
                searchStats.getNodeCount(),
                searchStats.getEdgeCount(),
                analyzed.results().size(),
                analyzed.elapsedMs(),
                truncation
        );
        applyStats(analyzed.results(), stats);
        return new TaintRun(analyzed.results(), stats);
    }

    public TaintRun analyzeDfsResults(List<DFSResult> dfsResults,
                                      Integer timeoutMs,
                                      Integer maxPaths,
                                      AtomicBoolean cancelFlag,
                                      String sinkKindOverride) {
        AnalysisResult analyzed = analyzeInternal(
                dfsResults,
                timeoutMs,
                maxPaths,
                cancelFlag,
                sinkKindOverride,
                inferMode(dfsResults, DFSResult.FROM_SOURCE_TO_SINK),
                ""
        );
        FlowStats stats = new FlowStats(
                0,
                0,
                analyzed.results().size(),
                analyzed.elapsedMs(),
                analyzed.truncation()
        );
        applyStats(analyzed.results(), stats);
        return new TaintRun(analyzed.results(), stats);
    }

    private SearchRun runSearch(GraphSnapshot snapshot, FlowOptions options, AtomicBoolean cancelFlag) {
        if (!canUsePrunedSearch(options)) {
            return runGraphDfs(snapshot, options, cancelFlag);
        }
        return runPrunedSearch(snapshot, options, cancelFlag);
    }

    private SearchRun runPrunedSearch(GraphSnapshot snapshot, FlowOptions options, AtomicBoolean cancelFlag) {
        List<Long> sinkIds = resolveMethodNodeIds(
                snapshot,
                options.getSinkClass(),
                options.getSinkMethod(),
                options.getSinkDesc(),
                options.getSinkJarId()
        );
        SearchDirection direction = resolveSearchDirection(options);
        int mode = resolveSearchMode(options, List.of());
        if (sinkIds.isEmpty()) {
            return new SearchRun(List.of(), FlowStats.empty(), "graph-pruned", mode);
        }

        PrunedBudget controller = new PrunedBudget(options, cancelFlag);
        int limit = options.resolvePathLimit();
        List<DFSResult> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        if (options.isSearchAllSources()) {
            for (Long sinkId : sinkIds) {
                if (sinkId == null || sinkId <= 0L || controller.shouldStop(out.size())) {
                    continue;
                }
                MethodReference.Handle sinkHandle = resolveSinkHandle(snapshot, sinkId);
                String sinkKind = sinkHandle == null ? "" : safe(semantics.resolveSinkKind(sinkHandle));
                ModelRegistry.SinkDescriptor sinkDescriptor = sinkHandle == null
                        ? ModelRegistry.SinkDescriptor.empty()
                        : ModelRegistry.resolveSinkDescriptor(sinkHandle);
                PruningPolicy.Resolved policy = semantics.resolvePolicy(sinkKind, sinkDescriptor, true, true);
                int remaining = limit - out.size();
                if (remaining <= 0) {
                    break;
                }
                GraphPrunedPathEngine.Run run = prunedPathEngine.searchBackward(
                        snapshot,
                        new GraphPrunedPathEngine.ReverseSearchRequest(
                                sinkId,
                                -1L,
                                resolveSourceNodeIds(snapshot, options.isOnlyFromWeb(), policy),
                                true,
                                options.isOnlyFromWeb(),
                                options.getDepth(),
                                remaining,
                                options.getTraversalMode(),
                                options.getBlacklist(),
                                options.getMinEdgeConfidence()
                        ),
                        controller
                );
                if (appendCandidates(run, out, seen, limit, controller)) {
                    break;
                }
            }
            return buildPrunedSearchRun(out, controller, mode);
        }

        List<Long> sourceIds = resolveMethodNodeIds(
                snapshot,
                options.getSourceClass(),
                options.getSourceMethod(),
                options.getSourceDesc(),
                options.getSourceJarId()
        );
        if (sourceIds.isEmpty()) {
            return new SearchRun(List.of(), FlowStats.empty(), "graph-pruned", mode);
        }

        outer:
        for (Long sinkId : sinkIds) {
            if (sinkId == null || sinkId <= 0L || controller.shouldStop(out.size())) {
                continue;
            }
            for (Long sourceId : sourceIds) {
                if (sourceId == null || sourceId <= 0L || controller.shouldStop(out.size())) {
                    continue;
                }
                int remaining = limit - out.size();
                if (remaining <= 0) {
                    break outer;
                }
                if (direction == SearchDirection.BIDIRECTIONAL) {
                    GraphPrunedPathEngine.Run forward = prunedPathEngine.search(
                            snapshot,
                            new GraphPrunedPathEngine.SearchRequest(
                                    sourceId,
                                    sinkId,
                                    options.getDepth(),
                                    remaining,
                                    options.getTraversalMode(),
                                    options.getBlacklist(),
                                    options.getMinEdgeConfidence()
                            ),
                            controller
                    );
                    if (appendCandidates(forward, out, seen, limit, controller)) {
                        break outer;
                    }
                    remaining = limit - out.size();
                    if (remaining <= 0) {
                        break outer;
                    }
                    GraphPrunedPathEngine.Run backward = prunedPathEngine.searchBackward(
                            snapshot,
                            new GraphPrunedPathEngine.ReverseSearchRequest(
                                    sinkId,
                                    sourceId,
                                    Set.of(),
                                    false,
                                    false,
                                    options.getDepth(),
                                    remaining,
                                    options.getTraversalMode(),
                                    options.getBlacklist(),
                                    options.getMinEdgeConfidence()
                            ),
                            controller
                    );
                    if (appendCandidates(backward, out, seen, limit, controller)) {
                        break outer;
                    }
                    continue;
                }
                GraphPrunedPathEngine.Run run = direction == SearchDirection.BACKWARD
                        ? prunedPathEngine.searchBackward(
                                snapshot,
                                new GraphPrunedPathEngine.ReverseSearchRequest(
                                        sinkId,
                                        sourceId,
                                        Set.of(),
                                        false,
                                        false,
                                        options.getDepth(),
                                        remaining,
                                        options.getTraversalMode(),
                                        options.getBlacklist(),
                                        options.getMinEdgeConfidence()
                                ),
                                controller
                        )
                        : prunedPathEngine.search(
                                snapshot,
                                new GraphPrunedPathEngine.SearchRequest(
                                        sourceId,
                                        sinkId,
                                        options.getDepth(),
                                        remaining,
                                        options.getTraversalMode(),
                                        options.getBlacklist(),
                                        options.getMinEdgeConfidence()
                                ),
                                controller
                        );
                if (appendCandidates(run, out, seen, limit, controller)) {
                    break outer;
                }
            }
        }

        return buildPrunedSearchRun(out, controller, mode);
    }

    private SearchRun runGraphDfs(GraphSnapshot snapshot, FlowOptions options, AtomicBoolean cancelFlag) {
        SearchDirection direction = resolveSearchDirection(options);
        if (direction != SearchDirection.BIDIRECTIONAL || options == null || options.isSearchAllSources()) {
            GraphDfsEngine.DfsRun dfsRun = dfsEngine.run(snapshot, options, cancelFlag);
            FlowStats stats = new FlowStats(
                    dfsRun.stats() == null ? 0 : dfsRun.stats().getNodeCount(),
                    dfsRun.stats() == null ? 0 : dfsRun.stats().getEdgeCount(),
                    dfsRun.results() == null ? 0 : dfsRun.results().size(),
                    dfsRun.stats() == null ? 0L : dfsRun.stats().getElapsedMs(),
                    dfsRun.stats() == null ? FlowTruncation.none() : dfsRun.stats().getTruncation()
            );
            return new SearchRun(
                    dfsRun.results() == null ? List.of() : dfsRun.results(),
                    stats,
                    "graph-dfs",
                    resolveSearchMode(options, dfsRun.results())
            );
        }

        GraphDfsEngine.DfsRun forwardRun = dfsEngine.run(snapshot, copyOptions(options, SearchDirection.FORWARD), cancelFlag);
        GraphDfsEngine.DfsRun backwardRun = dfsEngine.run(snapshot, copyOptions(options, SearchDirection.BACKWARD), cancelFlag);
        List<DFSResult> merged = mergeDfsResults(forwardRun.results(), backwardRun.results(), options.resolvePathLimit());
        FlowStats stats = new FlowStats(
                safeNodeCount(forwardRun) + safeNodeCount(backwardRun),
                safeEdgeCount(forwardRun) + safeEdgeCount(backwardRun),
                merged.size(),
                Math.max(safeElapsedMs(forwardRun), safeElapsedMs(backwardRun)),
                mergeTruncation(safeTruncation(forwardRun), safeTruncation(backwardRun))
        );
        return new SearchRun(merged, stats, "graph-dfs", resolveSearchMode(options, merged));
    }

    private static boolean canUsePrunedSearch(FlowOptions options) {
        if (options == null) {
            return false;
        }
        if (safe(options.getSinkClass()).isBlank() || safe(options.getSinkMethod()).isBlank()) {
            return false;
        }
        if (isAnyDesc(options.getSinkDesc())) {
            return false;
        }
        if (options.isSearchAllSources()) {
            return true;
        }
        if (safe(options.getSourceClass()).isBlank() || safe(options.getSourceMethod()).isBlank()) {
            return false;
        }
        return !isAnyDesc(options.getSourceDesc());
    }

    private AnalysisResult analyzeInternal(List<DFSResult> dfsResults,
                                           Integer timeoutMs,
                                           Integer maxPaths,
                                           AtomicBoolean cancelFlag,
                                           String sinkKindOverride,
                                           int truncatedMode,
                                           String searchBackend) {
        semantics.refreshRuleContext();
        long startNs = System.nanoTime();
        List<DFSResult> ordered = dfsResults == null
                ? Collections.emptyList()
                : new ArrayList<>(dfsResults);
        ordered.sort(StableOrder.DFS_RESULT);

        List<TaintResult> out = new ArrayList<>();
        int processed = 0;
        String truncationReason = "";
        for (DFSResult dfs : ordered) {
            if (shouldCancel(cancelFlag)) {
                truncationReason = "taint_canceled";
                break;
            }
            if (timeoutMs != null && timeoutMs > 0) {
                long elapsed = (System.nanoTime() - startNs) / 1_000_000L;
                if (elapsed >= timeoutMs) {
                    truncationReason = "taint_timeout";
                    break;
                }
            }
            if (maxPaths != null && maxPaths > 0 && processed >= maxPaths) {
                truncationReason = "taint_maxPaths";
                break;
            }
            out.add(evaluatePath(dfs, sinkKindOverride, searchBackend));
            processed++;
        }
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        if (!truncationReason.isBlank()) {
            out.add(buildTruncatedMeta(truncationReason, processed, elapsedMs, truncatedMode));
            return new AnalysisResult(out, elapsedMs, FlowTruncation.of(truncationReason, recommendation(truncationReason)));
        }
        return new AnalysisResult(out, elapsedMs, FlowTruncation.none());
    }

    private TaintResult evaluatePath(DFSResult dfs, String sinkKindOverride, String searchBackend) {
        TaintResult result = new TaintResult();
        result.setDfsResult(dfs);
        if (dfs == null || dfs.getMethodList() == null || dfs.getMethodList().isEmpty()) {
            result.setSuccess(false);
            result.setLowConfidence(true);
            result.setTaintText("TAINT SKIP: empty_chain");
            return result;
        }

        List<MethodReference.Handle> methods = dfs.getMethodList();
        List<DFSEdge> edges = dfs.getEdges();
        MethodReference.Handle sinkHandle = resolveSinkHandle(dfs);
        String sinkKind = safe(sinkKindOverride);
        if (sinkKind.isBlank()) {
            SinkKindResolver.Result resolved = SinkKindResolver.resolve(sinkHandle);
            sinkKind = safe(resolved == null ? null : resolved.getKind());
        }
        ModelRegistry.SinkDescriptor sinkDescriptor = sinkHandle == null
                ? ModelRegistry.SinkDescriptor.empty()
                : ModelRegistry.resolveSinkDescriptor(sinkHandle);

        StringBuilder text = new StringBuilder();
        if (!safe(searchBackend).isBlank()) {
            text.append("search backend: ").append(searchBackend).append("\n");
        }
        text.append("taint backend: graph-summary\n");
        if (!sinkKind.isBlank()) {
            text.append("sink kind: ").append(sinkKind).append("\n");
        }
        GraphTaintSemantics.PortState state = semantics.initialState(methods.get(0));
        PruningPolicy.Resolved policy = semantics.resolvePolicy(sinkKind, sinkDescriptor, dfs.getMode());
        boolean lowConfidence = dfs.isTruncated();
        boolean success = true;
        boolean fullyProven = true;

        for (int i = 0; i + 1 < methods.size(); i++) {
            MethodReference.Handle from = methods.get(i);
            MethodReference.Handle to = methods.get(i + 1);
            DFSEdge edge = (edges == null || i >= edges.size()) ? null : edges.get(i);
            GraphTaintSemantics.Transition transition = semantics.transition(from, to, state, edge, sinkKind, policy);
            if (transition == null || transition.state() == null) {
                success = false;
                fullyProven = false;
                text.append("segment ").append(i + 1).append(" unproven\n");
                break;
            }
            state = transition.state();
            if (transition.lowConfidence()) {
                lowConfidence = true;
            }
            if (!transition.proven()) {
                fullyProven = false;
            }
        }
        if (success && fullyProven) {
            text.append("taint path verified\n");
        } else {
            success = false;
            text.append("taint path not fully proven\n");
            lowConfidence = true;
        }
        result.setSuccess(success);
        result.setLowConfidence(lowConfidence);
        result.setTaintText(text.toString());
        return result;
    }

    private List<Long> resolveMethodNodeIds(GraphSnapshot snapshot,
                                            String className,
                                            String methodName,
                                            String methodDesc,
                                            Integer jarId) {
        if (snapshot == null) {
            return List.of();
        }
        String clazz = normalizeClass(className);
        String method = safe(methodName);
        String desc = safe(methodDesc);
        if (clazz.isBlank() || method.isBlank()) {
            return List.of();
        }
        Set<Long> out = new LinkedHashSet<>();
        if (!isAnyDesc(desc)) {
            for (GraphNode node : snapshot.getNodesByMethodSignatureView(clazz, method, desc)) {
                if (GraphTraversalRules.isMethodNode(node) && matchesJarId(node, jarId)) {
                    out.add(node.getNodeId());
                }
            }
        } else {
            for (GraphNode node : snapshot.getNodesByClassNameView(clazz)) {
                if (!GraphTraversalRules.isMethodNode(node)) {
                    continue;
                }
                if (!method.equals(node.getMethodName())) {
                    continue;
                }
                if (!matchesJarId(node, jarId)) {
                    continue;
                }
                out.add(node.getNodeId());
            }
        }
        return new ArrayList<>(out);
    }

    private Set<Long> resolveSourceNodeIds(GraphSnapshot snapshot,
                                           boolean onlyFromWeb,
                                           PruningPolicy.Resolved policy) {
        Set<Long> out = new LinkedHashSet<>();
        if (snapshot == null) {
            return out;
        }
        SourceRuleSupport.RuleSnapshot ruleSnapshot = SourceRuleSupport.snapshotCurrentRules();
        SourceFlagResolver resolver = new SourceFlagResolver(ruleSnapshot, policy);
        for (GraphNode node : snapshot.getNodesByKindView("method")) {
            if (!GraphTraversalRules.isMethodNode(node)) {
                continue;
            }
            int sourceFlags = resolver.resolve(node);
            if (onlyFromWeb) {
                if ((sourceFlags & GraphNode.SOURCE_FLAG_WEB) != 0) {
                    out.add(node.getNodeId());
                }
            } else if ((sourceFlags & GraphNode.SOURCE_FLAG_ANY) != 0) {
                out.add(node.getNodeId());
            }
        }
        return out;
    }

    private static boolean appendCandidates(GraphPrunedPathEngine.Run run,
                                            List<DFSResult> out,
                                            Set<String> seen,
                                            int limit,
                                            GraphPrunedPathEngine.Controller controller) {
        if (run == null || run.candidates() == null || run.candidates().isEmpty()) {
            return false;
        }
        for (GraphPrunedPathEngine.Candidate candidate : run.candidates()) {
            DFSResult dfs = candidate == null ? null : candidate.dfsResult();
            if (dfs == null) {
                continue;
            }
            String key = StableOrder.dfsPathKey(dfs);
            if (!seen.add(key)) {
                continue;
            }
            out.add(dfs);
            if (out.size() >= limit || (controller != null && controller.shouldStop(out.size()))) {
                return true;
            }
        }
        return false;
    }

    private SearchRun buildPrunedSearchRun(List<DFSResult> out, PrunedBudget controller, int mode) {
        FlowStats stats = new FlowStats(
                controller.nodeCount(),
                controller.edgeCount(),
                out == null ? 0 : out.size(),
                controller.elapsedMs(),
                controller.truncation()
        );
        return new SearchRun(out, stats, "graph-pruned", mode);
    }

    private static boolean matchesJarId(GraphNode node, Integer jarId) {
        if (jarId == null || jarId <= 0) {
            return true;
        }
        return node != null && node.getJarId() == jarId;
    }

    private static int resolveSearchMode(FlowOptions options, List<DFSResult> results) {
        int inferred = inferMode(results, Integer.MIN_VALUE);
        if (inferred != Integer.MIN_VALUE) {
            return inferred;
        }
        SearchDirection direction = resolveSearchDirection(options);
        if (direction == SearchDirection.BACKWARD) {
            return options != null && options.isSearchAllSources() ? DFSResult.FROM_SOURCE_TO_ALL : DFSResult.FROM_SINK_TO_SOURCE;
        }
        if (options != null && options.isFromSink()) {
            return options.isSearchAllSources() ? DFSResult.FROM_SOURCE_TO_ALL : DFSResult.FROM_SINK_TO_SOURCE;
        }
        return DFSResult.FROM_SOURCE_TO_SINK;
    }

    private static SearchDirection resolveSearchDirection(FlowOptions options) {
        return options == null ? SearchDirection.FORWARD : options.getSearchDirection();
    }

    private static FlowOptions copyOptions(FlowOptions source, SearchDirection direction) {
        if (source == null) {
            return FlowOptions.builder().direction(direction).build();
        }
        return FlowOptions.builder()
                .direction(direction)
                .searchAllSources(source.isSearchAllSources())
                .depth(source.getDepth())
                .maxLimit(source.getMaxLimit())
                .maxPaths(source.getMaxPaths())
                .maxNodes(source.getMaxNodes())
                .maxEdges(source.getMaxEdges())
                .timeoutMs(source.getTimeoutMs())
                .onlyFromWeb(source.isOnlyFromWeb())
                .traversalMode(source.getTraversalMode())
                .blacklist(source.getBlacklist())
                .minEdgeConfidence(source.getMinEdgeConfidence())
                .source(source.getSourceClass(), source.getSourceMethod(), source.getSourceDesc())
                .sourceJarId(source.getSourceJarId())
                .sink(source.getSinkClass(), source.getSinkMethod(), source.getSinkDesc())
                .sinkJarId(source.getSinkJarId())
                .build();
    }

    private static List<DFSResult> mergeDfsResults(List<DFSResult> first, List<DFSResult> second, int limit) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        ArrayList<DFSResult> out = new ArrayList<>();
        appendMerged(out, seen, first, limit);
        appendMerged(out, seen, second, limit);
        return out;
    }

    private static void appendMerged(List<DFSResult> out,
                                     Set<String> seen,
                                     List<DFSResult> values,
                                     int limit) {
        if (values == null || values.isEmpty() || out.size() >= limit) {
            return;
        }
        for (DFSResult dfs : values) {
            if (dfs == null) {
                continue;
            }
            String key = StableOrder.dfsPathKey(dfs);
            if (!seen.add(key)) {
                continue;
            }
            out.add(dfs);
            if (out.size() >= limit) {
                return;
            }
        }
    }

    private static int safeNodeCount(GraphDfsEngine.DfsRun run) {
        return run == null || run.stats() == null ? 0 : run.stats().getNodeCount();
    }

    private static int safeEdgeCount(GraphDfsEngine.DfsRun run) {
        return run == null || run.stats() == null ? 0 : run.stats().getEdgeCount();
    }

    private static long safeElapsedMs(GraphDfsEngine.DfsRun run) {
        return run == null || run.stats() == null ? 0L : run.stats().getElapsedMs();
    }

    private static FlowTruncation safeTruncation(GraphDfsEngine.DfsRun run) {
        return run == null || run.stats() == null ? FlowTruncation.none() : run.stats().getTruncation();
    }

    private static boolean isAnyDesc(String desc) {
        String value = safe(desc);
        return value.isBlank() || "*".equals(value) || "null".equalsIgnoreCase(value);
    }

    private static String normalizeClass(String value) {
        return safe(value).replace('.', '/');
    }

    private MethodReference.Handle resolveSinkHandle(GraphSnapshot snapshot, Long sinkId) {
        if (snapshot == null || sinkId == null || sinkId <= 0L) {
            return null;
        }
        return semantics.toHandle(snapshot.getNode(sinkId));
    }

    private static MethodReference.Handle resolveSinkHandle(DFSResult result) {
        if (result == null) {
            return null;
        }
        MethodReference.Handle sink = result.getSink();
        if (sink == null && result.getMethodList() != null && !result.getMethodList().isEmpty()) {
            sink = result.getMethodList().get(result.getMethodList().size() - 1);
        }
        return sink;
    }

    private static boolean shouldCancel(AtomicBoolean cancelFlag) {
        return Thread.currentThread().isInterrupted()
                || (cancelFlag != null && cancelFlag.get());
    }

    private static int inferMode(List<DFSResult> dfsResults, int fallbackMode) {
        if (dfsResults == null || dfsResults.isEmpty()) {
            return fallbackMode;
        }
        for (DFSResult dfs : dfsResults) {
            if (dfs == null) {
                continue;
            }
            int mode = dfs.getMode();
            if (mode == DFSResult.FROM_SOURCE_TO_SINK
                    || mode == DFSResult.FROM_SINK_TO_SOURCE
                    || mode == DFSResult.FROM_SOURCE_TO_ALL) {
                return mode;
            }
        }
        return fallbackMode;
    }

    private static TaintResult buildTruncatedMeta(String reason, int processed, long elapsedMs, int mode) {
        DFSResult meta = new DFSResult();
        meta.setMethodList(new ArrayList<>());
        meta.setEdges(new ArrayList<>());
        meta.setDepth(0);
        meta.setMode(mode);
        meta.setTruncated(true);
        meta.setTruncateReason(reason);
        meta.setRecommend(recommendation(reason));
        meta.setPathCount(processed);
        meta.setElapsedMs(elapsedMs);
        TaintResult result = new TaintResult();
        result.setDfsResult(meta);
        result.setSuccess(false);
        result.setLowConfidence(true);
        result.setTaintText("TAINT TRUNCATED: " + reason);
        return result;
    }

    private static String recommendation(String reason) {
        if ("taint_timeout".equals(reason)) {
            return "Try increase timeoutMs or reduce depth/maxPaths.";
        }
        if ("taint_maxPaths".equals(reason)) {
            return "Try increase maxPaths or narrow sink/source.";
        }
        if ("taint_canceled".equals(reason)) {
            return "Taint task was canceled.";
        }
        if ("taint_maxNodes".equals(reason)) {
            return "Try increase maxNodes or narrow sink/source.";
        }
        if ("taint_maxEdges".equals(reason)) {
            return "Try increase maxEdges or reduce depth.";
        }
        return "Try reduce depth/maxPaths.";
    }

    private static FlowTruncation mergeTruncation(FlowTruncation first, FlowTruncation second) {
        FlowTruncation a = first == null ? FlowTruncation.none() : first;
        FlowTruncation b = second == null ? FlowTruncation.none() : second;
        if (a.truncated() && b.truncated()) {
            return FlowTruncation.of(join(a.reason(), b.reason()), join(a.recommend(), b.recommend()));
        }
        if (b.truncated()) {
            return b;
        }
        return a;
    }

    private static String join(String left, String right) {
        String a = left == null ? "" : left.trim();
        String b = right == null ? "" : right.trim();
        if (a.isEmpty()) {
            return b;
        }
        if (b.isEmpty() || a.equals(b)) {
            return a;
        }
        return a + "; " + b;
    }

    private static void applyStats(List<TaintResult> results, FlowStats stats) {
        if (results == null || results.isEmpty() || stats == null) {
            return;
        }
        FlowTruncation truncation = stats.getTruncation();
        for (TaintResult taint : results) {
            if (taint == null || taint.getDfsResult() == null) {
                continue;
            }
            DFSResult dfs = taint.getDfsResult();
            if (dfs.getMethodList() != null && !dfs.getMethodList().isEmpty()) {
                dfs.setTruncated(truncation.truncated());
                dfs.setTruncateReason(truncation.reason());
                dfs.setRecommend(truncation.recommend());
                dfs.setPathCount(stats.getPathCount());
                dfs.setElapsedMs(stats.getElapsedMs());
            }
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static MethodReference findMethodReference(GraphNode node) {
        if (node == null || node.getClassName().isBlank() || node.getMethodName().isBlank() || node.getMethodDesc().isBlank()) {
            return null;
        }
        List<MethodReference> rows = DatabaseManager.getMethodReferencesByClass(node.getClassName());
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        MethodReference fallback = null;
        for (MethodReference row : rows) {
            if (row == null) {
                continue;
            }
            if (!node.getMethodName().equals(row.getName()) || !node.getMethodDesc().equals(row.getDesc())) {
                continue;
            }
            Integer jarId = row.getJarId();
            if (jarId != null && jarId == node.getJarId()) {
                return row;
            }
            if (fallback == null) {
                fallback = row;
            }
        }
        return fallback;
    }

    public record TaintRun(List<TaintResult> results, FlowStats stats) {
        public TaintRun {
            results = results == null ? List.of() : results;
            stats = stats == null ? FlowStats.empty() : stats;
        }
    }

    private record AnalysisResult(List<TaintResult> results, long elapsedMs, FlowTruncation truncation) {
        private AnalysisResult {
            results = results == null ? List.of() : results;
            truncation = truncation == null ? FlowTruncation.none() : truncation;
        }
    }

    private record SearchRun(List<DFSResult> results,
                             FlowStats stats,
                             String backend,
                             int mode) {
        private SearchRun {
            results = results == null ? List.of() : results;
            stats = stats == null ? FlowStats.empty() : stats;
            backend = backend == null ? "" : backend;
        }
    }

    private static final class PrunedBudget implements GraphPrunedPathEngine.Controller {
        private final long deadlineNs;
        private final Integer maxNodes;
        private final Integer maxEdges;
        private final AtomicBoolean cancelFlag;
        private final long startNs;
        private int nodeCount;
        private int edgeCount;
        private FlowTruncation truncation;

        private PrunedBudget(FlowOptions options, AtomicBoolean cancelFlag) {
            int timeoutMs = options == null || options.getTimeoutMs() == null ? 0 : options.getTimeoutMs();
            this.deadlineNs = timeoutMs <= 0
                    ? Long.MAX_VALUE
                    : System.nanoTime() + Math.max(1L, timeoutMs) * 1_000_000L;
            this.maxNodes = options == null ? null : options.getMaxNodes();
            this.maxEdges = options == null ? null : options.getMaxEdges();
            this.cancelFlag = cancelFlag;
            this.startNs = System.nanoTime();
            this.truncation = FlowTruncation.none();
        }

        @Override
        public void onNode() {
            if (truncation.truncated()) {
                return;
            }
            nodeCount++;
            if (maxNodes != null && maxNodes > 0 && nodeCount > maxNodes) {
                truncation = FlowTruncation.of("taint_maxNodes", recommendation("taint_maxNodes"));
            }
        }

        @Override
        public void onExpand() {
            if (truncation.truncated()) {
                return;
            }
            edgeCount++;
            if (maxEdges != null && maxEdges > 0 && edgeCount > maxEdges) {
                truncation = FlowTruncation.of("taint_maxEdges", recommendation("taint_maxEdges"));
            }
        }

        @Override
        public void onPath() {
        }

        @Override
        public void checkpoint() {
            if (truncation.truncated()) {
                return;
            }
            if (shouldCancel(cancelFlag)) {
                truncation = FlowTruncation.of("taint_canceled", recommendation("taint_canceled"));
                return;
            }
            if (deadlineNs != Long.MAX_VALUE && System.nanoTime() > deadlineNs) {
                truncation = FlowTruncation.of("taint_timeout", recommendation("taint_timeout"));
            }
        }

        @Override
        public boolean shouldStop(int emittedPaths) {
            checkpoint();
            return truncation.truncated();
        }

        private int nodeCount() {
            return Math.max(0, nodeCount);
        }

        private int edgeCount() {
            return Math.max(0, edgeCount);
        }

        private long elapsedMs() {
            return (System.nanoTime() - startNs) / 1_000_000L;
        }

        private FlowTruncation truncation() {
            return truncation == null ? FlowTruncation.none() : truncation;
        }
    }

    private static final class SourceFlagResolver {
        private final SourceRuleSupport.RuleSnapshot ruleSnapshot;
        private final PruningPolicy.SourceSelection sourceSelection;
        private final Map<Long, Integer> resolvedFlags = new HashMap<>();

        private SourceFlagResolver(SourceRuleSupport.RuleSnapshot ruleSnapshot, PruningPolicy.Resolved policy) {
            this.ruleSnapshot = ruleSnapshot;
            this.sourceSelection = policy == null
                    ? PruningPolicy.defaults().resolve(PruningPolicy.MatchContext.builder().build()).sourceSelection()
                    : policy.sourceSelection();
        }

        private int resolve(GraphNode node) {
            if (node == null) {
                return 0;
            }
            Integer cached = resolvedFlags.get(node.getNodeId());
            if (cached != null) {
                return cached;
            }
            int graphFlags = node.getSourceFlags();
            MethodReference method = findMethodReference(node);
            int liveFlags;
            if (method != null) {
                liveFlags = SourceRuleSupport.resolveCurrentSourceFlags(method, ruleSnapshot);
            } else {
                liveFlags = SourceRuleSupport.resolveCurrentSourceFlags(
                        node.getClassName(),
                        node.getMethodName(),
                        node.getMethodDesc(),
                        node.getJarId(),
                        ruleSnapshot
                );
            }
            int flags = switch (sourceSelection) {
                case GRAPH -> graphFlags;
                case RULES -> liveFlags;
                case MERGED -> graphFlags | liveFlags;
            };
            resolvedFlags.put(node.getNodeId(), flags);
            return flags;
        }
    }
}
