/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.flow;

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.dfs.DFSEdge;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.entity.MethodCallResult;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.utils.ExternalSymbolResolver;
import me.n1ar4.jar.analyzer.utils.StableOrder;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bridge DFS fallback for unresolved dependency edges.
 * <p>
 * It supplements graph snapshot traversal with:
 * <ul>
 *     <li>method_call_table caller edges</li>
 *     <li>lazy external bytecode call edges (per class)</li>
 * </ul>
 */
public final class HybridFlowBridgeEngine {
    private static final Logger logger = LogManager.getLogger();
    private static final int DB_EDGE_LIMIT = 512;
    private static final int EXTERNAL_CLASS_LIMIT = 256;
    private static final int NEIGHBOR_LIMIT = 512;

    public BridgeRun run(GraphSnapshot snapshot, FlowOptions options, AtomicBoolean cancelFlag) {
        long startNs = System.nanoTime();
        if (snapshot == null || options == null || !hasExplicitEndpoints(options)) {
            return new BridgeRun(List.of(), FlowStats.empty());
        }
        if (options.isFromSink() && options.isSearchAllSources()) {
            return new BridgeRun(List.of(), FlowStats.empty());
        }
        Budget budget = new Budget(options, cancelFlag, startNs);
        Context ctx = new Context(snapshot, options, budget);
        List<MethodSig> sources = resolveSeeds(snapshot, options.getSourceClass(), options.getSourceMethod(), options.getSourceDesc(), true, ctx);
        List<MethodSig> sinks = resolveSeeds(snapshot, options.getSinkClass(), options.getSinkMethod(), options.getSinkDesc(), false, ctx);
        if (sources.isEmpty() || sinks.isEmpty()) {
            return new BridgeRun(List.of(), budget.toStats(0));
        }

        List<DFSResult> out = new ArrayList<>();
        Set<String> pathKeys = new HashSet<>();
        for (MethodSig source : sources) {
            if (budget.shouldStop(out.size())) {
                break;
            }
            for (MethodSig sink : sinks) {
                if (budget.shouldStop(out.size())) {
                    break;
                }
                enumerate(source, sink, ctx, budget, out, pathKeys);
            }
        }
        out.sort(StableOrder.DFS_RESULT);
        FlowStats stats = budget.toStats(out.size());
        applyStats(out, stats);
        return new BridgeRun(out, stats);
    }

    private void enumerate(MethodSig source,
                           MethodSig sink,
                           Context ctx,
                           Budget budget,
                           List<DFSResult> out,
                           Set<String> pathKeys) {
        List<MethodSig> nodePath = new ArrayList<>();
        List<EdgeRef> edgePath = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        ArrayDeque<PathFrame> stack = new ArrayDeque<>();
        nodePath.add(source);
        visited.add(source.key());
        budget.onNode(source, out.size());
        stack.push(new PathFrame(source, rankEdges(ctx.outgoing(source), sink)));

        while (!stack.isEmpty() && !budget.shouldStop(out.size())) {
            PathFrame frame = stack.peek();
            MethodSig current = frame.node;
            int hops = Math.max(0, nodePath.size() - 1);
            if (current.equalsIgnoreJar(sink)) {
                DFSResult result = toDfsResult(nodePath, edgePath, ctx.options);
                if (result != null) {
                    String key = StableOrder.dfsPathKey(result);
                    if (!key.isBlank() && pathKeys.add(key)) {
                        out.add(result);
                        budget.onPath(out.size());
                    }
                }
                pop(stack, nodePath, edgePath, visited);
                continue;
            }
            if (hops >= ctx.options.getDepth() || frame.cursor >= frame.edges.size()) {
                pop(stack, nodePath, edgePath, visited);
                continue;
            }
            EdgeRef edge = frame.edges.get(frame.cursor++);
            MethodSig next = edge.to;
            if (visited.contains(next.key())) {
                continue;
            }
            if (budget.onEdge(out.size())) {
                return;
            }
            visited.add(next.key());
            nodePath.add(next);
            edgePath.add(edge);
            budget.onNode(next, out.size());
            stack.push(new PathFrame(next, rankEdges(ctx.outgoing(next), sink)));
        }
    }

    private static void pop(ArrayDeque<PathFrame> stack,
                            List<MethodSig> nodePath,
                            List<EdgeRef> edgePath,
                            Set<String> visited) {
        stack.pop();
        if (stack.isEmpty()) {
            return;
        }
        MethodSig removed = nodePath.remove(nodePath.size() - 1);
        visited.remove(removed.key());
        edgePath.remove(edgePath.size() - 1);
    }

    private List<MethodSig> resolveSeeds(GraphSnapshot snapshot,
                                         String className,
                                         String methodName,
                                         String methodDesc,
                                         boolean source,
                                         Context ctx) {
        String clazz = normalizeClass(className);
        String method = safe(methodName);
        String desc = normalizeDesc(methodDesc);
        if (clazz.isBlank() || method.isBlank()) {
            return List.of();
        }
        LinkedHashSet<MethodSig> out = new LinkedHashSet<>();
        if (!isAnyDesc(desc)) {
            for (GraphNode node : snapshot.getNodesByMethodSignatureView(clazz, method, desc)) {
                if (node != null && "method".equalsIgnoreCase(node.getKind())) {
                    out.add(MethodSig.fromNode(node));
                }
            }
        } else {
            for (GraphNode node : snapshot.getNodesByClassNameView(clazz)) {
                if (node == null || !"method".equalsIgnoreCase(node.getKind())) {
                    continue;
                }
                if (method.equals(safe(node.getMethodName()))) {
                    out.add(MethodSig.fromNode(node));
                }
            }
        }
        if (!out.isEmpty()) {
            return new ArrayList<>(out);
        }

        MethodSig fuzzy = new MethodSig(clazz, method, desc, null, false);
        List<EdgeRef> dbEdges = source ? ctx.dbOutgoing(fuzzy) : ctx.dbIncoming(fuzzy);
        for (EdgeRef edge : dbEdges) {
            out.add(source ? edge.from : edge.to);
        }
        if (!out.isEmpty()) {
            return new ArrayList<>(out);
        }
        if (isAnyDesc(desc)) {
            for (MethodResult mr : ExternalSymbolResolver.resolveMethods(clazz)) {
                if (mr == null || !method.equals(safe(mr.getMethodName()))) {
                    continue;
                }
                out.add(MethodSig.of(mr.getClassName(), mr.getMethodName(), mr.getMethodDesc(), mr.getJarId(), snapshot));
            }
        }
        if (!out.isEmpty()) {
            return new ArrayList<>(out);
        }
        return List.of(fuzzy);
    }

    private static List<EdgeRef> rankEdges(List<EdgeRef> edges, MethodSig sink) {
        if (edges == null || edges.isEmpty()) {
            return List.of();
        }
        List<EdgeRef> out = new ArrayList<>(edges);
        out.sort(Comparator.comparingInt((EdgeRef e) -> e.to.equalsIgnoreJar(sink) ? 0 : 1)
                .thenComparingInt(e -> 4 - confidenceScore(e.confidence))
                .thenComparing(e -> e.to.key()));
        return out;
    }

    private DFSResult toDfsResult(List<MethodSig> nodePath, List<EdgeRef> edgePath, FlowOptions options) {
        if (nodePath == null || nodePath.isEmpty()) {
            return null;
        }
        List<MethodReference.Handle> handles = new ArrayList<>();
        for (MethodSig sig : nodePath) {
            MethodReference.Handle handle = sig.toHandle();
            if (handle == null) {
                return null;
            }
            if (!handles.isEmpty() && handles.get(handles.size() - 1).equals(handle)) {
                continue;
            }
            handles.add(handle);
        }
        if (handles.isEmpty()) {
            return null;
        }
        DFSResult result = new DFSResult();
        result.setMethodList(handles);
        result.setEdges(toEdges(edgePath));
        result.setDepth(handles.size());
        result.setSource(handles.get(0));
        result.setSink(handles.get(handles.size() - 1));
        if (options.isFromSink()) {
            result.setMode(options.isSearchAllSources() ? DFSResult.FROM_SOURCE_TO_ALL : DFSResult.FROM_SINK_TO_SOURCE);
        } else {
            result.setMode(DFSResult.FROM_SOURCE_TO_SINK);
        }
        return result;
    }

    private List<DFSEdge> toEdges(List<EdgeRef> edgePath) {
        if (edgePath == null || edgePath.isEmpty()) {
            return List.of();
        }
        List<DFSEdge> out = new ArrayList<>();
        for (EdgeRef edge : edgePath) {
            DFSEdge item = new DFSEdge();
            item.setFrom(edge.from.toHandle());
            item.setTo(edge.to.toHandle());
            item.setType(normalizeEdgeType(edge.type));
            item.setConfidence(normalizeConfidence(edge.confidence));
            item.setEvidence(edge.evidence);
            out.add(item);
        }
        return out;
    }

    private static String normalizeEdgeType(String edgeType) {
        String value = safe(edgeType).toLowerCase(Locale.ROOT);
        if (value.startsWith("calls_")) {
            value = value.substring("calls_".length());
        }
        return value.isBlank() ? "direct" : value;
    }

    private static void applyStats(List<DFSResult> results, FlowStats stats) {
        if (results == null || results.isEmpty() || stats == null) {
            return;
        }
        FlowTruncation truncation = stats.getTruncation();
        for (DFSResult result : results) {
            if (result == null) {
                continue;
            }
            result.setTruncated(truncation.truncated());
            result.setTruncateReason(truncation.reason());
            result.setRecommend(truncation.recommend());
            result.setNodeCount(stats.getNodeCount());
            result.setEdgeCount(stats.getEdgeCount());
            result.setPathCount(stats.getPathCount());
            result.setElapsedMs(stats.getElapsedMs());
        }
    }

    private static boolean hasExplicitEndpoints(FlowOptions options) {
        return !normalizeClass(options.getSourceClass()).isBlank()
                && !safe(options.getSourceMethod()).isBlank()
                && !normalizeClass(options.getSinkClass()).isBlank()
                && !safe(options.getSinkMethod()).isBlank();
    }

    private static boolean isAnyDesc(String desc) {
        String value = safe(desc);
        return value.isBlank() || "*".equals(value) || "null".equalsIgnoreCase(value);
    }

    private static String normalizeDesc(String desc) {
        return isAnyDesc(desc) ? "" : safe(desc);
    }

    private static String normalizeClass(String className) {
        return safe(className).replace('.', '/');
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeConfidence(String confidence) {
        String value = safe(confidence).toLowerCase(Locale.ROOT);
        if ("high".equals(value) || "medium".equals(value) || "low".equals(value)) {
            return value;
        }
        return "low";
    }

    private static int confidenceScore(String confidence) {
        return switch (normalizeConfidence(confidence)) {
            case "high" -> 3;
            case "medium" -> 2;
            default -> 1;
        };
    }

    private static boolean meetsConfidence(String edgeConfidence, String minConfidence) {
        return confidenceScore(edgeConfidence) >= confidenceScore(minConfidence);
    }

    public record BridgeRun(List<DFSResult> results, FlowStats stats) {
        public BridgeRun {
            results = results == null ? List.of() : results;
            stats = stats == null ? FlowStats.empty() : stats;
        }
    }

    private static final class PathFrame {
        private final MethodSig node;
        private final List<EdgeRef> edges;
        private int cursor = 0;

        private PathFrame(MethodSig node, List<EdgeRef> edges) {
            this.node = node;
            this.edges = edges == null ? List.of() : edges;
        }
    }

    private interface EdgeProvider {
        default List<EdgeRef> outgoing(MethodSig from, Context ctx) {
            return List.of();
        }

        default List<EdgeRef> incoming(MethodSig to, Context ctx) {
            return List.of();
        }
    }

    private static final class IndexedDbEdgeProvider implements EdgeProvider {
        @Override
        public List<EdgeRef> outgoing(MethodSig from, Context ctx) {
            return ctx.queryDbOutgoing(from);
        }

        @Override
        public List<EdgeRef> incoming(MethodSig to, Context ctx) {
            return ctx.queryDbIncoming(to);
        }
    }

    private static final class DependencyLazyEdgeProvider implements EdgeProvider {
        @Override
        public List<EdgeRef> outgoing(MethodSig from, Context ctx) {
            return ctx.queryExternalOutgoing(from);
        }
    }

    private static final class Context {
        private final GraphSnapshot snapshot;
        private final FlowOptions options;
        private final Budget budget;
        private final CoreEngine engine;
        private final Set<String> externalClasses = new HashSet<>();
        private final EdgeProvider indexedProvider = new IndexedDbEdgeProvider();
        private final EdgeProvider dependencyProvider = new DependencyLazyEdgeProvider();
        private final List<EdgeProvider> edgeProviders = List.of(indexedProvider, dependencyProvider);

        private Context(GraphSnapshot snapshot, FlowOptions options, Budget budget) {
            this.snapshot = snapshot;
            this.options = options;
            this.budget = budget;
            this.engine = EngineContext.getEngine();
        }

        private List<EdgeRef> outgoing(MethodSig from) {
            List<EdgeRef> merged = new ArrayList<>();
            for (EdgeProvider provider : edgeProviders) {
                if (provider == null) {
                    continue;
                }
                merged.addAll(provider.outgoing(from, this));
            }
            if (merged.size() > NEIGHBOR_LIMIT) {
                budget.mark("bridge_neighbor_limit");
                merged = merged.subList(0, NEIGHBOR_LIMIT);
            }
            return merged;
        }

        private List<EdgeRef> dbOutgoing(MethodSig from) {
            return indexedProvider.outgoing(from, this);
        }

        private List<EdgeRef> dbIncoming(MethodSig to) {
            return indexedProvider.incoming(to, this);
        }

        private List<EdgeRef> queryDbOutgoing(MethodSig from) {
            if (engine == null || !engine.isEnabled() || from.empty()) {
                return List.of();
            }
            List<EdgeRef> out = new ArrayList<>();
            try {
                List<MethodCallResult> rows = engine.getCallEdgesByCaller(
                        from.className,
                        from.methodName,
                        isAnyDesc(from.methodDesc) ? "" : from.methodDesc,
                        0,
                        DB_EDGE_LIMIT
                );
                for (MethodCallResult row : rows) {
                    MethodSig caller = MethodSig.of(row.getCallerClassName(), row.getCallerMethodName(), row.getCallerMethodDesc(), row.getCallerJarId(), snapshot);
                    if (!from.matches(caller)) {
                        continue;
                    }
                    MethodSig callee = MethodSig.of(row.getCalleeClassName(), row.getCalleeMethodName(), row.getCalleeMethodDesc(), row.getCalleeJarId(), snapshot);
                    if (rejectByPolicy(caller, callee, row.getEdgeConfidence())) {
                        continue;
                    }
                    out.add(new EdgeRef(caller, callee, safe(row.getEdgeType()), normalizeConfidence(row.getEdgeConfidence()), safe(row.getEdgeEvidence())));
                }
            } catch (Throwable ex) {
                logger.debug("bridge db outgoing failed: {}", ex.toString());
            }
            return dedupeByTarget(out);
        }

        private List<EdgeRef> queryDbIncoming(MethodSig to) {
            if (engine == null || !engine.isEnabled() || to.empty()) {
                return List.of();
            }
            List<EdgeRef> out = new ArrayList<>();
            try {
                List<MethodCallResult> rows = engine.getCallEdgesByCallee(
                        to.className,
                        to.methodName,
                        isAnyDesc(to.methodDesc) ? "" : to.methodDesc,
                        0,
                        DB_EDGE_LIMIT
                );
                for (MethodCallResult row : rows) {
                    MethodSig callee = MethodSig.of(row.getCalleeClassName(), row.getCalleeMethodName(), row.getCalleeMethodDesc(), row.getCalleeJarId(), snapshot);
                    if (!to.matches(callee)) {
                        continue;
                    }
                    MethodSig caller = MethodSig.of(row.getCallerClassName(), row.getCallerMethodName(), row.getCallerMethodDesc(), row.getCallerJarId(), snapshot);
                    if (rejectByPolicy(caller, callee, row.getEdgeConfidence())) {
                        continue;
                    }
                    out.add(new EdgeRef(caller, callee, safe(row.getEdgeType()), normalizeConfidence(row.getEdgeConfidence()), safe(row.getEdgeEvidence())));
                }
            } catch (Throwable ex) {
                logger.debug("bridge db incoming failed: {}", ex.toString());
            }
            return dedupeByTarget(out);
        }

        private List<EdgeRef> queryExternalOutgoing(MethodSig from) {
            if (from.empty() || isAnyDesc(from.methodDesc)) {
                return List.of();
            }
            if (!externalClasses.contains(from.className)) {
                if (externalClasses.size() >= EXTERNAL_CLASS_LIMIT) {
                    budget.mark("bridge_external_limit");
                    return List.of();
                }
                externalClasses.add(from.className);
            }
            List<EdgeRef> out = new ArrayList<>();
            try {
                List<MethodResult> rows = ExternalSymbolResolver.resolveCallees(from.className, from.methodName, from.methodDesc);
                for (MethodResult row : rows) {
                    MethodSig callee = MethodSig.of(row.getClassName(), row.getMethodName(), row.getMethodDesc(), row.getJarId(), snapshot);
                    if (rejectByPolicy(from, callee, "low")) {
                        continue;
                    }
                    out.add(new EdgeRef(from, callee, "external", "low", "external"));
                }
            } catch (Throwable ex) {
                logger.debug("bridge external outgoing failed: {}", ex.toString());
            }
            return dedupeByTarget(out);
        }

        private boolean rejectByPolicy(MethodSig from, MethodSig to, String confidence) {
            if (from.empty() || to.empty()) {
                return true;
            }
            if (!meetsConfidence(confidence, options.getMinEdgeConfidence())) {
                return true;
            }
            return options.getBlacklist().contains(from.className) || options.getBlacklist().contains(to.className);
        }

        private List<EdgeRef> dedupeByTarget(List<EdgeRef> input) {
            if (input == null || input.isEmpty()) {
                return List.of();
            }
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            List<EdgeRef> out = new ArrayList<>();
            for (EdgeRef edge : input) {
                if (edge == null || edge.to.empty()) {
                    continue;
                }
                String key = edge.to.key() + "|" + normalizeEdgeType(edge.type);
                if (seen.add(key)) {
                    out.add(edge);
                }
            }
            return out;
        }
    }

    private record EdgeRef(MethodSig from, MethodSig to, String type, String confidence, String evidence) {
    }

    private record MethodSig(String className, String methodName, String methodDesc, Integer jarId, boolean indexed) {
        private MethodSig {
            className = normalizeClass(className);
            methodName = safe(methodName);
            methodDesc = normalizeDesc(methodDesc);
            jarId = jarId == null || jarId <= 0 ? null : jarId;
        }

        private static MethodSig fromNode(GraphNode node) {
            Integer jar = node.getJarId() > 0 ? node.getJarId() : null;
            return new MethodSig(node.getClassName(), node.getMethodName(), node.getMethodDesc(), jar, true);
        }

        private static MethodSig of(String className, String methodName, String methodDesc, Integer jarId, GraphSnapshot snapshot) {
            String cls = normalizeClass(className);
            String mtd = safe(methodName);
            String desc = normalizeDesc(methodDesc);
            Integer jar = jarId == null || jarId <= 0 ? null : jarId;
            boolean indexed = false;
            if (!cls.isBlank() && !mtd.isBlank() && !desc.isBlank() && snapshot != null) {
                indexed = snapshot.findMethodNodeId(cls, mtd, desc, jar) > 0L;
            }
            return new MethodSig(cls, mtd, desc, jar, indexed);
        }

        private boolean matches(MethodSig other) {
            if (other == null) {
                return false;
            }
            if (!className.equals(other.className) || !methodName.equals(other.methodName)) {
                return false;
            }
            if (isAnyDesc(methodDesc) || isAnyDesc(other.methodDesc)) {
                return true;
            }
            return methodDesc.equals(other.methodDesc);
        }

        private boolean equalsIgnoreJar(MethodSig other) {
            return other != null
                    && className.equals(other.className)
                    && methodName.equals(other.methodName)
                    && methodDesc.equals(other.methodDesc);
        }

        private String key() {
            return className + "#" + methodName + "#" + methodDesc + "#" + (jarId == null ? -1 : jarId);
        }

        private boolean empty() {
            return className.isBlank() || methodName.isBlank() || methodDesc.isBlank();
        }

        private MethodReference.Handle toHandle() {
            if (empty()) {
                return null;
            }
            return new MethodReference.Handle(new ClassReference.Handle(className, jarId == null ? -1 : jarId), methodName, methodDesc);
        }
    }

    private static final class Budget {
        private final FlowOptions options;
        private final AtomicBoolean cancelFlag;
        private final long startNs;
        private final int pathLimit;
        private final Integer maxNodes;
        private final Integer maxEdges;
        private final Integer timeoutMs;
        private final Set<String> nodes = new HashSet<>();
        private int edges;
        private String reason = "";

        private Budget(FlowOptions options, AtomicBoolean cancelFlag, long startNs) {
            this.options = options;
            this.cancelFlag = cancelFlag;
            this.startNs = startNs;
            this.pathLimit = options.resolvePathLimit();
            this.maxNodes = options.getMaxNodes();
            this.maxEdges = options.getMaxEdges();
            this.timeoutMs = options.getTimeoutMs();
        }

        private void onNode(MethodSig node, int currentPaths) {
            if (node == null) {
                return;
            }
            nodes.add(node.key());
            if (maxNodes != null && maxNodes > 0 && nodes.size() >= maxNodes) {
                shouldStop(currentPaths);
            }
        }

        private boolean onEdge(int currentPaths) {
            edges++;
            if (maxEdges != null && maxEdges > 0 && edges >= maxEdges) {
                return shouldStop(currentPaths);
            }
            return false;
        }

        private void onPath(int currentPaths) {
            if (pathLimit > 0 && currentPaths >= pathLimit) {
                shouldStop(currentPaths);
            }
        }

        private boolean shouldStop(int currentPaths) {
            if (cancelFlag != null && cancelFlag.get()) {
                mark("bridge_canceled");
                return true;
            }
            if (timeoutMs != null && timeoutMs > 0 && elapsedMs() >= timeoutMs) {
                mark("bridge_timeout");
                return true;
            }
            if (pathLimit > 0 && currentPaths >= pathLimit) {
                mark("bridge_max_paths");
                return true;
            }
            if (maxNodes != null && maxNodes > 0 && nodes.size() >= maxNodes) {
                mark("bridge_max_nodes");
                return true;
            }
            if (maxEdges != null && maxEdges > 0 && edges >= maxEdges) {
                mark("bridge_max_edges");
                return true;
            }
            return false;
        }

        private void mark(String value) {
            if (reason.isBlank()) {
                reason = safe(value);
            }
        }

        private long elapsedMs() {
            return Math.max(0L, (System.nanoTime() - startNs) / 1_000_000L);
        }

        private FlowStats toStats(int pathCount) {
            FlowTruncation truncation = reason.isBlank()
                    ? FlowTruncation.none()
                    : FlowTruncation.of(reason, "refine source/sink or increase limits");
            return new FlowStats(nodes.size(), edges, pathCount, elapsedMs(), truncation);
        }
    }
}
