/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.flow;

import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import me.n1ar4.jar.analyzer.utils.StableOrder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GraphFlowService {
    private final GraphStore store;
    private final GraphDfsEngine dfsEngine;
    private final GraphTaintEngine taintEngine;
    private final HybridFlowBridgeEngine bridgeEngine;

    public GraphFlowService() {
        this(new GraphStore(), new GraphDfsEngine(), new GraphTaintEngine(), new HybridFlowBridgeEngine());
    }

    public GraphFlowService(GraphStore store, GraphDfsEngine dfsEngine, GraphTaintEngine taintEngine) {
        this(store, dfsEngine, taintEngine, new HybridFlowBridgeEngine());
    }

    public GraphFlowService(GraphStore store,
                            GraphDfsEngine dfsEngine,
                            GraphTaintEngine taintEngine,
                            HybridFlowBridgeEngine bridgeEngine) {
        this.store = store == null ? new GraphStore() : store;
        this.dfsEngine = dfsEngine == null ? new GraphDfsEngine() : dfsEngine;
        this.taintEngine = taintEngine == null ? new GraphTaintEngine() : taintEngine;
        this.bridgeEngine = bridgeEngine == null ? new HybridFlowBridgeEngine() : bridgeEngine;
    }

    public DfsOutcome runDfs(FlowOptions options, AtomicBoolean cancelFlag) {
        GraphSnapshot snapshot = store.loadSnapshot();
        GraphDfsEngine.DfsRun base = dfsEngine.run(snapshot, options, cancelFlag);
        HybridFlowBridgeEngine.BridgeRun bridge = bridgeEngine.run(snapshot, options, cancelFlag);
        List<DFSResult> merged = mergeDfsResults(base.results(), bridge.results());
        FlowStats stats = mergeDfsStats(base.stats(), bridge.stats(), merged.size());
        applyStats(merged, stats);
        return new DfsOutcome(merged, stats);
    }

    public TaintOutcome runTaint(FlowOptions options, AtomicBoolean cancelFlag) {
        DfsOutcome dfs = runDfs(options, cancelFlag);
        return analyzeDfsResults(
                dfs.results(),
                options == null ? null : options.getTimeoutMs(),
                options == null ? null : options.getMaxPaths(),
                cancelFlag,
                null
        );
    }

    public TaintOutcome analyzeDfsResults(List<DFSResult> dfsResults,
                                          Integer timeoutMs,
                                          Integer maxPaths,
                                          AtomicBoolean cancelFlag,
                                          String sinkKindOverride) {
        GraphTaintEngine.TaintRun run = taintEngine.analyzeDfsResults(
                dfsResults,
                timeoutMs,
                maxPaths,
                cancelFlag,
                sinkKindOverride
        );
        return new TaintOutcome(run.results(), run.stats());
    }

    public record DfsOutcome(List<DFSResult> results, FlowStats stats) {
        public DfsOutcome {
            results = results == null ? List.of() : results;
            stats = stats == null ? FlowStats.empty() : stats;
        }
    }

    public record TaintOutcome(List<TaintResult> results, FlowStats stats) {
        public TaintOutcome {
            results = results == null ? List.of() : results;
            stats = stats == null ? FlowStats.empty() : stats;
        }
    }

    private static List<DFSResult> mergeDfsResults(List<DFSResult> base, List<DFSResult> bridge) {
        List<DFSResult> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        appendResults(base, out, seen);
        appendResults(bridge, out, seen);
        out.sort(StableOrder.DFS_RESULT);
        return out;
    }

    private static void appendResults(List<DFSResult> input, List<DFSResult> out, Set<String> seen) {
        if (input == null || input.isEmpty()) {
            return;
        }
        for (DFSResult result : input) {
            if (result == null) {
                continue;
            }
            String key = StableOrder.dfsPathKey(result);
            if (key.isBlank() || !seen.add(key)) {
                continue;
            }
            out.add(result);
        }
    }

    private static FlowStats mergeDfsStats(FlowStats base, FlowStats bridge, int mergedPathCount) {
        FlowStats left = base == null ? FlowStats.empty() : base;
        FlowStats right = bridge == null ? FlowStats.empty() : bridge;
        FlowTruncation truncation = left.getTruncation() != null && left.getTruncation().truncated()
                ? left.getTruncation()
                : right.getTruncation();
        return new FlowStats(
                Math.max(left.getNodeCount(), right.getNodeCount()),
                Math.max(left.getEdgeCount(), right.getEdgeCount()),
                Math.max(0, mergedPathCount),
                left.getElapsedMs() + right.getElapsedMs(),
                truncation
        );
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
}
