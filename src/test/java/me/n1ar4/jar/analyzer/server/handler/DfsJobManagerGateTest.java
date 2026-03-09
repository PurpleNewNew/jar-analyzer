/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server.handler;

import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DfsJobManagerGateTest {
    private final String originalProjectKey = ActiveProjectContext.getPublishedActiveProjectKey();
    private final String originalProjectAlias = ActiveProjectContext.getPublishedActiveProjectAlias();

    @AfterEach
    void cleanup() {
        ProjectRuntimeContext.clear();
        ActiveProjectContext.setActiveProject(originalProjectKey, originalProjectAlias);
    }

    @Test
    void runJobShouldFailWhenProjectIsNoLongerActiveAtExecutionTime() {
        String projectA = "dfs-project-a";
        String projectB = "dfs-project-b";
        DfsApiUtil.DfsRequest request = new DfsApiUtil.DfsRequest();
        request.fromSink = false;
        request.projectKey = projectA;
        request.sourceClass = "demo/Source";
        request.sourceMethod = "entry";
        request.sourceDesc = "()V";
        request.sinkClass = "demo/Sink";
        request.sinkMethod = "sink";
        request.sinkDesc = "()V";

        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
        DfsJobManager manager = new DfsJobManager(
                new ThreadPoolExecutor(
                        1,
                        1,
                        0L,
                        TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(1),
                        Thread.ofPlatform().name("dfs-gate-test-", 1).daemon(true).factory(),
                        new ThreadPoolExecutor.AbortPolicy()
                ),
                cleaner,
                false
        );
        try {
            ActiveProjectContext.setActiveProject(projectA, projectA);
            long projectEpoch = ActiveProjectContext.currentEpoch();
            DfsJob job = new DfsJob("dfs-job", request, projectA, 7L, projectEpoch);

            ActiveProjectContext.setActiveProject(projectB, projectB);
            manager.runJob(job);

            assertEquals(DfsJob.Status.FAILED, job.getStatus());
            assertTrue(job.getError().contains("project_switch_required"));
        } finally {
            manager.shutdownForTest();
            cleaner.shutdownNow();
        }
    }

    @Test
    void runJobShouldFailWhenRuntimeSnapshotChangesWhileProjectRemainsActive() {
        String projectKey = "dfs-project-runtime";
        DfsApiUtil.DfsRequest request = new DfsApiUtil.DfsRequest();
        request.fromSink = false;
        request.projectKey = projectKey;
        request.sourceClass = "demo/Source";
        request.sourceMethod = "entry";
        request.sourceDesc = "()V";
        request.sinkClass = "demo/Sink";
        request.sinkMethod = "sink";
        request.sinkDesc = "()V";
        ActiveProjectContext.setActiveProject(projectKey, projectKey);
        ProjectRuntimeContext.clear();
        ProjectRuntimeContext.restoreProjectRuntime(projectKey, 1L,
                ProjectModel.artifact(null, null, java.util.List.of(), false));

        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
        DfsJobManager manager = new DfsJobManager(
                new ThreadPoolExecutor(
                        1,
                        1,
                        0L,
                        TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(1),
                        Thread.ofPlatform().name("dfs-gate-test-", 1).daemon(true).factory(),
                        new ThreadPoolExecutor.AbortPolicy()
                ),
                cleaner,
                false
        );
        try {
            DfsJob job = new DfsJob("dfs-job", request, projectKey,
                    ProjectRuntimeContext.stateVersion(), ActiveProjectContext.currentEpoch());
            ProjectRuntimeContext.replaceProjectModel(ProjectModel.artifact(null, null, java.util.List.of(), true));

            manager.runJob(job);

            assertEquals(DfsJob.Status.FAILED, job.getStatus());
            assertTrue(job.getError().contains("db_changed"));
        } finally {
            manager.shutdownForTest();
            cleaner.shutdownNow();
        }
    }
}
