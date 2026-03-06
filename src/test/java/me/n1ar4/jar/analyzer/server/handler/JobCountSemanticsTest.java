/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server.handler;

import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.graph.flow.FlowStats;
import me.n1ar4.jar.analyzer.graph.flow.FlowTruncation;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobCountSemanticsTest {
    @Test
    void dfsJobShouldKeepActualPathCountWhenOnlyMetaRowExists() {
        DfsApiUtil.DfsRequest req = new DfsApiUtil.DfsRequest();
        req.fromSink = true;
        req.searchAllSources = true;
        FlowStats stats = new FlowStats(12, 34, 0, 56L, FlowTruncation.of("timeout", "increase timeout"));
        List<DFSResult> results = DfsApiUtil.buildTruncatedMeta(req, stats, List.of());

        DfsJob job = new DfsJob("dfs-job", req, "demo", 1L);
        job.markDone(results, stats);

        assertEquals(0, job.getPathCount());
        assertEquals(1, job.getResultCount());
    }

    @Test
    void taintJobShouldIgnoreSyntheticMetaRowsInTotalCount() {
        DFSResult meta = new DFSResult();
        meta.setMethodList(new ArrayList<>());
        meta.setEdges(new ArrayList<>());
        meta.setMode(DFSResult.FROM_SOURCE_TO_SINK);
        meta.setTruncated(true);

        TaintResult result = new TaintResult();
        result.setDfsResult(meta);
        result.setSuccess(false);

        TaintJob job = new TaintJob("taint-job", "dfs-job", "demo", 1L, 1000, 10, null);
        job.markDone(List.of(result), 10L);

        assertEquals(0, job.getTotalCount());
    }
}
