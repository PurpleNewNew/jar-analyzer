/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server.handler;

import me.n1ar4.jar.analyzer.graph.flow.model.FlowPath;
import me.n1ar4.jar.analyzer.graph.flow.FlowStats;
import me.n1ar4.jar.analyzer.graph.flow.FlowTruncation;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobCountSemanticsTest {
    @Test
    void dfsJobShouldKeepActualPathCountWhenOnlyMetaRowExists() {
        DfsApiUtil.DfsRequest req = new DfsApiUtil.DfsRequest();
        req.fromSink = true;
        req.searchAllSources = true;
        FlowStats stats = new FlowStats(12, 34, 0, 56L, FlowTruncation.of("timeout", "increase timeout"));
        List<FlowPath> results = DfsApiUtil.buildTruncatedMeta(req, stats, List.of());

        DfsJob job = new DfsJob("dfs-job", req, "demo", 1L);
        job.markDone(results, stats);

        assertEquals(0, job.getPathCount());
        assertEquals(1, job.getResultCount());
    }

    @Test
    void taintJobShouldIgnoreSyntheticMetaRowsInTotalCount() {
        FlowPath meta = new FlowPath();
        meta.setMethodList(new ArrayList<>());
        meta.setEdges(new ArrayList<>());
        meta.setMode(FlowPath.FROM_SOURCE_TO_SINK);
        meta.setTruncated(true);

        TaintResult result = new TaintResult();
        result.setDfsResult(meta);
        result.setSuccess(false);

        TaintJob job = new TaintJob("taint-job", "dfs-job", "demo", 1L, 1000, 10, null);
        job.markDone(List.of(result), 10L);

        assertEquals(0, job.getTotalCount());
    }

    @Test
    void canceledDfsJobShouldRemainCanceledWhenMarkedFailedLater() {
        DfsJob job = new DfsJob("dfs-job", new DfsApiUtil.DfsRequest(), "demo", 1L);

        assertTrue(job.cancel());
        job.markFailed(new IllegalStateException("db_changed"));

        assertEquals(DfsJob.Status.CANCELED, job.getStatus());
        assertEquals("canceled", job.getError());
    }

    @Test
    void canceledTaintJobShouldRemainCanceledWhenMarkedFailedLater() {
        TaintJob job = new TaintJob("taint-job", "dfs-job", "demo", 1L, 1000, 10, null);

        assertTrue(job.cancel());
        job.markFailed(new IllegalStateException("db_changed"));

        assertEquals(TaintJob.Status.CANCELED, job.getStatus());
        assertEquals("canceled", job.getError());
    }
}
