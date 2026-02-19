/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.flow;

import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.taint.TaintResult;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GraphFlowService {
    private final GraphStore store;
    private final GraphDfsEngine dfsEngine;
    private final GraphTaintEngine taintEngine;

    public GraphFlowService() {
        this(new GraphStore(), new GraphDfsEngine(), new GraphTaintEngine());
    }

    public GraphFlowService(GraphStore store, GraphDfsEngine dfsEngine, GraphTaintEngine taintEngine) {
        this.store = store == null ? new GraphStore() : store;
        this.dfsEngine = dfsEngine == null ? new GraphDfsEngine() : dfsEngine;
        this.taintEngine = taintEngine == null ? new GraphTaintEngine() : taintEngine;
    }

    public DfsOutcome runDfs(FlowOptions options, AtomicBoolean cancelFlag) {
        GraphSnapshot snapshot = store.loadSnapshot();
        GraphDfsEngine.DfsRun run = dfsEngine.run(snapshot, options, cancelFlag);
        return new DfsOutcome(run.results(), run.stats());
    }

    public TaintOutcome runTaint(FlowOptions options, AtomicBoolean cancelFlag) {
        GraphSnapshot snapshot = store.loadSnapshot();
        GraphTaintEngine.TaintRun run = taintEngine.track(snapshot, options, cancelFlag);
        return new TaintOutcome(run.results(), run.stats());
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
}
