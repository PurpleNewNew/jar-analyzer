/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server.handler;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.dfs.DFSEdge;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.graph.flow.FlowStats;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaintJobManagerGateTest {
    private final String originalProjectKey = ActiveProjectContext.getPublishedActiveProjectKey();
    private final String originalProjectAlias = ActiveProjectContext.getPublishedActiveProjectAlias();

    @AfterEach
    void cleanup() throws Exception {
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
        clearDfsJobs();
        ActiveProjectContext.setActiveProject(originalProjectKey, originalProjectAlias);
    }

    @Test
    void supportsTaintInputShouldAcceptForwardOrderedSinkModeResults() {
        DFSResult dfs = forwardResult(DFSResult.FROM_SINK_TO_SOURCE);

        assertTrue(TaintJobManager.supportsTaintInput(List.of(dfs)));
    }

    @Test
    void supportsTaintInputShouldRejectReverseOrderedResults() {
        DFSResult dfs = reverseResult();

        assertFalse(TaintJobManager.supportsTaintInput(List.of(dfs)));
    }

    @Test
    void runJobShouldFailWhenProjectIsNoLongerActiveAtExecutionTime() throws Exception {
        String projectA = "taint-project-a";
        String projectB = "taint-project-b";
        DfsApiUtil.DfsRequest request = new DfsApiUtil.DfsRequest();
        request.fromSink = true;
        request.searchAllSources = false;
        request.projectKey = projectA;
        request.sourceClass = "demo/Source";
        request.sourceMethod = "entry";
        request.sourceDesc = "()V";
        request.sinkClass = "demo/Sink";
        request.sinkMethod = "sink";
        request.sinkDesc = "()V";

        DfsJob dfsJob = new DfsJob("dfs-job", request, projectA, 7L);
        dfsJob.markDone(List.of(forwardResult(DFSResult.FROM_SINK_TO_SOURCE)), FlowStats.empty());
        storeDfsJob(dfsJob);

        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
        TaintJobManager manager = new TaintJobManager(
                new ThreadPoolExecutor(
                        1,
                        1,
                        0L,
                        TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(1),
                        Thread.ofPlatform().name("taint-gate-test-", 1).daemon(true).factory(),
                        new ThreadPoolExecutor.AbortPolicy()
                ),
                cleaner,
                false
        );
        try {
            ActiveProjectContext.setActiveProject(projectB, projectB);
            TaintJob job = new TaintJob("taint-job", dfsJob.getJobId(), projectA, 7L, 1000, 10, null);

            manager.runJob(job);

            assertEquals(TaintJob.Status.FAILED, job.getStatus());
            assertTrue(job.getError().contains("project_switch_required"));
        } finally {
            manager.shutdownForTest();
            cleaner.shutdownNow();
        }
    }

    @Test
    void runJobShouldFailWhenProjectBuildChangesWhileProjectRemainsActive() throws Exception {
        String projectKey = "taint-project-build";
        DfsApiUtil.DfsRequest request = new DfsApiUtil.DfsRequest();
        request.fromSink = true;
        request.searchAllSources = false;
        request.projectKey = projectKey;
        request.sourceClass = "demo/Source";
        request.sourceMethod = "entry";
        request.sourceDesc = "()V";
        request.sinkClass = "demo/Sink";
        request.sinkMethod = "sink";
        request.sinkDesc = "()V";
        ActiveProjectContext.setActiveProject(projectKey, projectKey);
        DatabaseManager.restoreProjectRuntime(projectKey, snapshotFor(2L));

        DfsJob dfsJob = new DfsJob("dfs-job", request, projectKey, 1L);
        dfsJob.markDone(List.of(forwardResult(DFSResult.FROM_SINK_TO_SOURCE)), FlowStats.empty());
        storeDfsJob(dfsJob);

        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
        TaintJobManager manager = new TaintJobManager(
                new ThreadPoolExecutor(
                        1,
                        1,
                        0L,
                        TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(1),
                        Thread.ofPlatform().name("taint-gate-test-", 1).daemon(true).factory(),
                        new ThreadPoolExecutor.AbortPolicy()
                ),
                cleaner,
                false
        );
        try {
            TaintJob job = new TaintJob("taint-job", dfsJob.getJobId(), projectKey, 1L, 1000, 10, null);

            manager.runJob(job);

            assertEquals(TaintJob.Status.FAILED, job.getStatus());
            assertTrue(job.getError().contains("db_changed"));
        } finally {
            manager.shutdownForTest();
            cleaner.shutdownNow();
        }
    }

    private static ProjectRuntimeSnapshot snapshotFor(long buildSeq) {
        return new ProjectRuntimeSnapshot(
                ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION,
                buildSeq,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                java.util.Set.of(),
                java.util.Set.of(),
                java.util.Set.of(),
                java.util.Set.of()
        );
    }

    private static DFSResult forwardResult(int mode) {
        MethodReference.Handle source = handle("demo/Source", "entry", "()V");
        MethodReference.Handle sink = handle("demo/Sink", "sink", "()V");
        DFSEdge edge = new DFSEdge();
        edge.setFrom(source);
        edge.setTo(sink);
        edge.setType("direct");

        DFSResult dfs = new DFSResult();
        dfs.setMode(mode);
        dfs.setMethodList(List.of(source, sink));
        dfs.setEdges(List.of(edge));
        dfs.setSource(source);
        dfs.setSink(sink);
        return dfs;
    }

    private static DFSResult reverseResult() {
        MethodReference.Handle source = handle("demo/Source", "entry", "()V");
        MethodReference.Handle sink = handle("demo/Sink", "sink", "()V");
        DFSEdge edge = new DFSEdge();
        edge.setFrom(sink);
        edge.setTo(source);
        edge.setType("direct");

        DFSResult dfs = new DFSResult();
        dfs.setMode(DFSResult.FROM_SINK_TO_SOURCE);
        dfs.setMethodList(List.of(sink, source));
        dfs.setEdges(List.of(edge));
        dfs.setSource(source);
        dfs.setSink(sink);
        return dfs;
    }

    private static MethodReference.Handle handle(String className, String methodName, String methodDesc) {
        return new MethodReference.Handle(new ClassReference.Handle(className, 1), methodName, methodDesc);
    }

    @SuppressWarnings("unchecked")
    private static void storeDfsJob(DfsJob job) throws Exception {
        Field field = DfsJobManager.class.getDeclaredField("jobs");
        field.setAccessible(true);
        Map<String, DfsJob> jobs = (Map<String, DfsJob>) field.get(DfsJobManager.getInstance());
        jobs.put(job.getJobId(), job);
    }

    @SuppressWarnings("unchecked")
    private static void clearDfsJobs() throws Exception {
        Field field = DfsJobManager.class.getDeclaredField("jobs");
        field.setAccessible(true);
        Map<String, DfsJob> jobs = (Map<String, DfsJob>) field.get(DfsJobManager.getInstance());
        if (jobs instanceof ConcurrentHashMap<?, ?> concurrent) {
            concurrent.clear();
            return;
        }
        jobs.clear();
    }
}
