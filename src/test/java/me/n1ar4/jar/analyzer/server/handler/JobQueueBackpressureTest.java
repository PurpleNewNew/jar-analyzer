/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server.handler;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobQueueBackpressureTest {
    private final String originalProjectKey = ActiveProjectContext.getPublishedActiveProjectKey();
    private final String originalProjectAlias = ActiveProjectContext.getPublishedActiveProjectAlias();

    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
        ActiveProjectContext.setActiveProject(originalProjectKey, originalProjectAlias);
    }

    @Test
    void dfsQueueShouldRejectWhenWorkerAndQueueAreFull() throws Exception {
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
        BlockingDfsJobManager manager = new BlockingDfsJobManager(cleaner);
        try {
            prepareReadyProject("dfs-backpressure");
            DfsApiUtil.DfsRequest req = dummyDfsRequest();
            manager.createJob(req);
            manager.awaitWorker();
            manager.createJob(req);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> manager.createJob(req));
            assertEquals("job_queue_full", ex.getMessage());
        } finally {
            manager.release();
            manager.shutdownForTest();
            cleaner.shutdownNow();
        }
    }

    @Test
    void taintQueueShouldRejectWhenWorkerAndQueueAreFull() throws Exception {
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
        BlockingTaintJobManager manager = new BlockingTaintJobManager(cleaner);
        try {
            prepareReadyProject("taint-backpressure");
            manager.createJob("dfs-a", "demo", 1000, 10, null, 1L);
            manager.awaitWorker();
            manager.createJob("dfs-b", "demo", 1000, 10, null, 1L);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> manager.createJob("dfs-c", "demo", 1000, 10, null, 1L));
            assertEquals("job_queue_full", ex.getMessage());
        } finally {
            manager.release();
            manager.shutdownForTest();
            cleaner.shutdownNow();
        }
    }

    private static DfsApiUtil.DfsRequest dummyDfsRequest() {
        DfsApiUtil.DfsRequest req = new DfsApiUtil.DfsRequest();
        req.fromSink = true;
        req.searchAllSources = true;
        req.depth = 3;
        req.blacklist = new HashSet<>();
        req.sinkClass = "demo/Sink";
        req.sinkMethod = "sink";
        req.sinkDesc = "()V";
        return req;
    }

    private static void prepareReadyProject(String projectKey) {
        ActiveProjectContext.setActiveProject(projectKey, projectKey);
        DatabaseManager.restoreProjectRuntime(projectKey, snapshotFor(1L));
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

    private static final class BlockingDfsJobManager extends DfsJobManager {
        private final java.util.concurrent.CountDownLatch started = new java.util.concurrent.CountDownLatch(1);
        private final java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);

        private BlockingDfsJobManager(ScheduledExecutorService cleaner) {
            super(
                    new ThreadPoolExecutor(
                            1,
                            1,
                            0L,
                            TimeUnit.MILLISECONDS,
                            new ArrayBlockingQueue<>(1),
                            Thread.ofPlatform().name("dfs-test-", 1).daemon(true).factory(),
                            new ThreadPoolExecutor.AbortPolicy()
                    ),
                    cleaner,
                    false
            );
        }

        @Override
        void runJob(DfsJob job) {
            started.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
                job.markCanceled("released");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                job.markCanceled("interrupted");
            }
        }

        private void awaitWorker() throws InterruptedException {
            started.await(5, TimeUnit.SECONDS);
        }

        private void release() {
            release.countDown();
        }
    }

    private static final class BlockingTaintJobManager extends TaintJobManager {
        private final java.util.concurrent.CountDownLatch started = new java.util.concurrent.CountDownLatch(1);
        private final java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);

        private BlockingTaintJobManager(ScheduledExecutorService cleaner) {
            super(
                    new ThreadPoolExecutor(
                            1,
                            1,
                            0L,
                            TimeUnit.MILLISECONDS,
                            new ArrayBlockingQueue<>(1),
                            Thread.ofPlatform().name("taint-test-", 1).daemon(true).factory(),
                            new ThreadPoolExecutor.AbortPolicy()
                    ),
                    cleaner,
                    false
            );
        }

        @Override
        void runJob(TaintJob job) {
            started.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
                job.markCanceled("released");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                job.markCanceled("interrupted");
            }
        }

        private void awaitWorker() throws InterruptedException {
            started.await(5, TimeUnit.SECONDS);
        }

        private void release() {
            release.countDown();
        }
    }
}
