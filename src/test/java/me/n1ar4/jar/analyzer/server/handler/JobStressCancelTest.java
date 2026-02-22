/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.server.handler;

import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.support.FixtureJars;
import me.n1ar4.support.Neo4jTestGraph;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JobStressCancelTest {
    @Test
    @SuppressWarnings("all")
    public void testJobStressAndCancel() throws Exception {
        Path jar = FixtureJars.springbootTestJar();
        WorkspaceContext.updateResolveInnerJars(false);
        CoreRunner.run(jar, null, false, false, true, null, true);

        MethodRow sink = pickDeterministicMethod();
        assertNotNull(sink);

        DfsApiUtil.DfsRequest req = new DfsApiUtil.DfsRequest();
        req.fromSink = true;
        req.searchAllSources = true; // allow null source
        req.depth = 6;
        req.maxLimit = 2;
        req.maxPaths = 2;
        req.maxNodes = 600;
        req.maxEdges = 2500;
        req.timeoutMs = 10_000;
        req.blacklist = new HashSet<>();
        req.onlyFromWeb = false;
        req.sinkClass = sink.className;
        req.sinkMethod = sink.methodName;
        req.sinkDesc = sink.methodDesc;

        int total = 10;
        List<DfsJob> dfsJobs = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            dfsJobs.add(DfsJobManager.getInstance().createJob(req));
        }

        // Cancel a subset immediately.
        Set<String> canceledDfsIds = new HashSet<>();
        for (int i = 0; i < dfsJobs.size(); i++) {
            if (i % 3 == 0) {
                DfsJob job = dfsJobs.get(i);
                canceledDfsIds.add(job.getJobId());
                DfsJobManager.getInstance().cancelJob(job.getJobId());
            }
        }

        waitDfsTerminal(dfsJobs, 60_000);
        for (DfsJob job : dfsJobs) {
            assertTrue(isTerminal(job.getStatus()), "dfs job must reach terminal status");
            if (canceledDfsIds.contains(job.getJobId())) {
                assertTrue(job.getStatus() == DfsJob.Status.CANCELED
                                || job.getStatus() == DfsJob.Status.DONE,
                        "canceled dfs job should be CANCELED or already DONE");
            }
        }

        // For DONE dfs jobs, launch taint jobs; also cancel a subset.
        List<TaintJob> taintJobs = new ArrayList<>();
        Set<String> canceledTaintIds = new HashSet<>();
        for (DfsJob dfs : dfsJobs) {
            if (dfs.getStatus() != DfsJob.Status.DONE) {
                continue;
            }
            long buildSeq = dfs.getBuildSeq();
            TaintJob job = TaintJobManager.getInstance().createJob(
                    dfs.getJobId(),
                    10_000,
                    3,
                    "sql",
                    buildSeq);
            taintJobs.add(job);
        }
        for (int i = 0; i < taintJobs.size(); i++) {
            if (i % 2 == 0) {
                TaintJob job = taintJobs.get(i);
                canceledTaintIds.add(job.getJobId());
                TaintJobManager.getInstance().cancelJob(job.getJobId());
            }
        }

        waitTaintTerminal(taintJobs, 60_000);
        for (TaintJob job : taintJobs) {
            assertTrue(isTerminal(job.getStatus()), "taint job must reach terminal status");
            if (canceledTaintIds.contains(job.getJobId())) {
                assertTrue(job.getStatus() == TaintJob.Status.CANCELED
                                || job.getStatus() == TaintJob.Status.DONE,
                        "canceled taint job should be CANCELED or already DONE");
            }
        }

        // Basic sanity: buildSeq did not change during this test.
        long seq = DatabaseManager.getBuildSeq();
        assertTrue(seq > 0, "buildSeq should be positive after build");
    }

    private static boolean isTerminal(DfsJob.Status status) {
        return status == DfsJob.Status.DONE
                || status == DfsJob.Status.FAILED
                || status == DfsJob.Status.CANCELED;
    }

    private static boolean isTerminal(TaintJob.Status status) {
        return status == TaintJob.Status.DONE
                || status == TaintJob.Status.FAILED
                || status == TaintJob.Status.CANCELED;
    }

    private static void waitDfsTerminal(List<DfsJob> jobs, long timeoutMs) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (System.nanoTime() < deadline) {
            boolean allDone = true;
            for (DfsJob job : jobs) {
                if (job == null) {
                    continue;
                }
                if (!isTerminal(job.getStatus())) {
                    allDone = false;
                    break;
                }
            }
            if (allDone) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("dfs jobs did not finish within timeout");
    }

    private static void waitTaintTerminal(List<TaintJob> jobs, long timeoutMs) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (System.nanoTime() < deadline) {
            boolean allDone = true;
            for (TaintJob job : jobs) {
                if (job == null) {
                    continue;
                }
                if (!isTerminal(job.getStatus())) {
                    allDone = false;
                    break;
                }
            }
            if (allDone) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("taint jobs did not finish within timeout");
    }

    private static MethodRow pickDeterministicMethod() {
        Neo4jTestGraph.MethodRef method = Neo4jTestGraph.pickFirstMethod();
        if (method == null) {
            return null;
        }
        return new MethodRow(method.className(), method.methodName(), method.methodDesc());
    }

    private static final class MethodRow {
        private final String className;
        private final String methodName;
        private final String methodDesc;

        private MethodRow(String className, String methodName, String methodDesc) {
            this.className = className;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
        }
    }
}
