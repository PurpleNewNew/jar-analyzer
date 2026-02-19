/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.flow;

import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.dfs.DFSEdge;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.taint.SinkKindResolver;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import me.n1ar4.jar.analyzer.taint.summary.CallFlow;
import me.n1ar4.jar.analyzer.taint.summary.FlowPort;
import me.n1ar4.jar.analyzer.taint.summary.MethodSummary;
import me.n1ar4.jar.analyzer.taint.summary.SummaryEngine;
import me.n1ar4.jar.analyzer.utils.StableOrder;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GraphTaintEngine {
    private final GraphDfsEngine dfsEngine = new GraphDfsEngine();
    private final SummaryEngine summaryEngine = new SummaryEngine();

    public TaintRun track(GraphSnapshot snapshot, FlowOptions options, AtomicBoolean cancelFlag) {
        if (snapshot == null || snapshot.getNodeCount() <= 0 || snapshot.getEdgeCount() <= 0) {
            return new TaintRun(List.of(), FlowStats.empty());
        }
        FlowOptions base = options == null ? FlowOptions.builder().build() : options;
        FlowOptions dfsOptions = FlowOptions.builder()
                .fromSink(false)
                .searchAllSources(false)
                .depth(base.getDepth())
                .maxLimit(base.getMaxLimit())
                .maxPaths(base.getMaxPaths())
                .maxNodes(base.getMaxNodes())
                .maxEdges(base.getMaxEdges())
                .timeoutMs(base.getTimeoutMs())
                .blacklist(base.getBlacklist())
                .minEdgeConfidence(base.getMinEdgeConfidence())
                .sink(base.getSinkClass(), base.getSinkMethod(), base.getSinkDesc())
                .source(base.getSourceClass(), base.getSourceMethod(), base.getSourceDesc())
                .build();
        GraphDfsEngine.DfsRun dfsRun = dfsEngine.run(snapshot, dfsOptions, cancelFlag);
        AnalysisResult analyzed = analyzeInternal(dfsRun.results(), base.getTimeoutMs(), base.getMaxPaths(), cancelFlag, null);
        FlowTruncation truncation = mergeTruncation(
                dfsRun.stats() == null ? FlowTruncation.none() : dfsRun.stats().getTruncation(),
                analyzed.truncation()
        );
        FlowStats dfsStats = dfsRun.stats() == null ? FlowStats.empty() : dfsRun.stats();
        FlowStats stats = new FlowStats(
                dfsStats.getNodeCount(),
                dfsStats.getEdgeCount(),
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
        AnalysisResult analyzed = analyzeInternal(dfsResults, timeoutMs, maxPaths, cancelFlag, sinkKindOverride);
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

    private AnalysisResult analyzeInternal(List<DFSResult> dfsResults,
                                           Integer timeoutMs,
                                           Integer maxPaths,
                                           AtomicBoolean cancelFlag,
                                           String sinkKindOverride) {
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
            out.add(evaluatePath(dfs, sinkKindOverride));
            processed++;
        }
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        if (!truncationReason.isBlank()) {
            out.add(buildTruncatedMeta(truncationReason, processed, elapsedMs));
            return new AnalysisResult(out, elapsedMs, FlowTruncation.of(truncationReason, recommendation(truncationReason)));
        }
        return new AnalysisResult(out, elapsedMs, FlowTruncation.none());
    }

    private TaintResult evaluatePath(DFSResult dfs, String sinkKindOverride) {
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
        String sinkKind = safe(sinkKindOverride);
        if (sinkKind.isBlank()) {
            SinkKindResolver.Result resolved = SinkKindResolver.resolve(resolveSinkHandle(dfs));
            sinkKind = safe(resolved == null ? null : resolved.getKind());
        }

        StringBuilder text = new StringBuilder();
        text.append("taint backend: graph-summary\n");
        if (!sinkKind.isBlank()) {
            text.append("sink kind: ").append(sinkKind).append("\n");
        }
        PortState state = allInputPorts(methods.get(0), false);
        boolean lowConfidence = dfs.isTruncated();
        boolean success = true;

        for (int i = 0; i + 1 < methods.size(); i++) {
            MethodReference.Handle from = methods.get(i);
            MethodReference.Handle to = methods.get(i + 1);
            DFSEdge edge = (edges == null || i >= edges.size()) ? null : edges.get(i);
            Transition transition = nextState(from, to, state, edge);
            if (transition == null || transition.state == null) {
                success = false;
                text.append("segment ").append(i + 1).append(" unproven\n");
                break;
            }
            state = transition.state;
            if (transition.lowConfidence) {
                lowConfidence = true;
            }
        }
        if (success) {
            text.append("taint path verified\n");
        } else {
            text.append("taint path not fully proven\n");
            lowConfidence = true;
        }
        result.setSuccess(success);
        result.setLowConfidence(lowConfidence);
        result.setTaintText(text.toString());
        return result;
    }

    private Transition nextState(MethodReference.Handle from,
                                 MethodReference.Handle to,
                                 PortState state,
                                 DFSEdge edge) {
        if (from == null || to == null || state == null) {
            return new Transition(PortState.any(), true);
        }
        if (state.anyPorts || !isPreciseEdge(edge)) {
            return new Transition(allInputPorts(to, true), true);
        }
        MethodSummary summary = summaryEngine.getSummary(from);
        if (summary == null || summary.isUnknown()) {
            return new Transition(allInputPorts(to, true), true);
        }
        BitSet next = new BitSet();
        boolean matched = false;
        boolean sawLow = false;
        for (CallFlow flow : summary.getCallFlows()) {
            if (flow == null || flow.getCallee() == null || flow.getFrom() == null || flow.getTo() == null) {
                continue;
            }
            if (!flow.getCallee().equals(to)) {
                continue;
            }
            if (!state.matches(flow.getFrom())) {
                continue;
            }
            matched = true;
            int idx = portBitIndex(flow.getTo());
            if (idx < 0) {
                return new Transition(allInputPorts(to, true), true);
            }
            next.set(idx);
            if ("low".equalsIgnoreCase(safe(flow.getConfidence()))) {
                sawLow = true;
            }
        }
        if (!matched || next.isEmpty()) {
            if (!state.uncertain) {
                return null;
            }
            return new Transition(allInputPorts(to, true), true);
        }
        return new Transition(new PortState(next, state.uncertain || sawLow, false), sawLow);
    }

    private boolean isPreciseEdge(DFSEdge edge) {
        if (edge == null) {
            return false;
        }
        String type = safe(edge.getType()).toLowerCase();
        if (!"direct".equals(type) && !"calls_direct".equals(type)) {
            return false;
        }
        return !"low".equalsIgnoreCase(safe(edge.getConfidence()));
    }

    private static PortState allInputPorts(MethodReference.Handle method, boolean uncertain) {
        if (method == null || method.getDesc() == null || method.getDesc().isBlank()) {
            return PortState.any();
        }
        try {
            int paramCount = Type.getArgumentTypes(method.getDesc()).length;
            BitSet bits = new BitSet(paramCount + 1);
            bits.set(0); // this
            if (paramCount > 0) {
                bits.set(1, paramCount + 1);
            }
            return new PortState(bits, uncertain, false);
        } catch (Exception ignored) {
            return PortState.any();
        }
    }

    private static int portBitIndex(FlowPort port) {
        if (port == null || port.getKind() == null) {
            return -1;
        }
        return switch (port.getKind()) {
            case THIS -> 0;
            case PARAM -> port.getIndex() >= 0 ? (port.getIndex() + 1) : -1;
            default -> -1;
        };
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

    private static TaintResult buildTruncatedMeta(String reason, int processed, long elapsedMs) {
        DFSResult meta = new DFSResult();
        meta.setMethodList(new ArrayList<>());
        meta.setEdges(new ArrayList<>());
        meta.setDepth(0);
        meta.setMode(DFSResult.FROM_SOURCE_TO_ALL);
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
        return "Try reduce depth/maxPaths.";
    }

    private static FlowTruncation mergeTruncation(FlowTruncation first, FlowTruncation second) {
        FlowTruncation a = first == null ? FlowTruncation.none() : first;
        FlowTruncation b = second == null ? FlowTruncation.none() : second;
        if (b.truncated()) {
            return b;
        }
        return a;
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

    private record Transition(PortState state, boolean lowConfidence) {
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

        private static PortState any() {
            return new PortState(new BitSet(), true, true);
        }

        private boolean matches(FlowPort port) {
            if (anyPorts) {
                return portBitIndex(port) >= 0;
            }
            int idx = portBitIndex(port);
            return idx >= 0 && ports.get(idx);
        }
    }
}
