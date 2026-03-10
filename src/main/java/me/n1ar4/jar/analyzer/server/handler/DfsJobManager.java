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

import me.n1ar4.jar.analyzer.core.ProjectStateUtil;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.graph.flow.GraphFlowService;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DfsJobManager {
    private static final Logger logger = LogManager.getLogger();
    private static final DfsJobManager INSTANCE = new DfsJobManager();
    private static final long DEFAULT_TIMEOUT_MS = 2L * 60 * 60 * 1000; // 2h
    private static final long MAX_TIMEOUT_MS = 8L * 60 * 60 * 1000; // 8h hard cap
    private static final long JOB_TTL_MS = 6L * 60 * 60 * 1000; // 6h
    private static final int MIN_QUEUE_CAPACITY = 32;

    private final Map<String, DfsJob> jobs = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor executor;
    private final ScheduledExecutorService cleaner;

    private DfsJobManager() {
        this(
                newExecutor(resolvePoolSize(), resolveQueueCapacity(resolvePoolSize()),
                        Thread.ofPlatform().name("dfs-job-", 1).daemon(true).factory()),
                Executors.newSingleThreadScheduledExecutor(
                        Thread.ofPlatform().name("dfs-job-cleaner").daemon(true).factory()),
                true
        );
    }

    DfsJobManager(ThreadPoolExecutor executor,
                  ScheduledExecutorService cleaner,
                  boolean scheduleCleanup) {
        this.executor = executor;
        this.cleaner = cleaner;
        if (scheduleCleanup && cleaner != null) {
            cleaner.scheduleAtFixedRate(this::cleanup, 10, 10, TimeUnit.MINUTES);
        }
    }

    public static DfsJobManager getInstance() {
        return INSTANCE;
    }

    public DfsJob createJob(DfsApiUtil.DfsRequest req) {
        DfsApiUtil.DfsRequest copy = copyRequest(req);
        if (copy.timeoutMs == null || copy.timeoutMs <= 0) {
            copy.timeoutMs = (int) DEFAULT_TIMEOUT_MS;
        } else if (copy.timeoutMs > MAX_TIMEOUT_MS) {
            copy.timeoutMs = (int) MAX_TIMEOUT_MS;
        }
        String jobId = "dfs_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        String projectKey = ActiveProjectContext.resolveRequestedOrActive(copy.projectKey);
        copy.projectKey = projectKey;
        long buildSeq = ProjectStateUtil.projectBuildSnapshot(projectKey);
        if (buildSeq <= 0L) {
            throw new IllegalStateException("project_model_missing_rebuild");
        }
        long projectEpoch = ActiveProjectContext.currentEpoch();
        DfsJob job = new DfsJob(jobId, copy, projectKey, buildSeq, projectEpoch);
        jobs.put(jobId, job);
        try {
            Future<?> future = executor.submit(() -> runJob(job));
            job.attachFuture(future);
            return job;
        } catch (RejectedExecutionException ex) {
            jobs.remove(jobId);
            throw new IllegalStateException("job_queue_full", ex);
        }
    }

    public DfsJob getJob(String jobId) {
        return jobs.get(jobId);
    }

    public boolean cancelJob(String jobId) {
        DfsJob job = jobs.get(jobId);
        if (job == null) {
            return false;
        }
        return job.cancel();
    }

    void runJob(DfsJob job) {
        try {
            if (job.getStatus() == DfsJob.Status.CANCELED) {
                return;
            }
            String staleReason = executionStaleReason(job);
            if (!staleReason.isBlank()) {
                job.markFailed(new IllegalStateException(staleReason));
                return;
            }
            job.markRunning();
            if (job.getStatus() == DfsJob.Status.CANCELED) {
                return;
            }
            GraphFlowService.DfsOutcome outcome = DfsApiUtil.run(job.getRequest(), job.getCancelFlag());
            if (job.getStatus() == DfsJob.Status.CANCELED || Thread.currentThread().isInterrupted()) {
                job.markCanceled("canceled");
                return;
            }
            staleReason = executionStaleReason(job);
            if (!staleReason.isBlank()) {
                job.markFailed(new IllegalStateException(staleReason));
                return;
            }
            List<DFSResult> results = outcome == null ? null : outcome.results();
            job.markDone(results, outcome == null ? null : outcome.stats());
        } catch (Exception ex) {
            InterruptUtil.restoreInterruptIfNeeded(ex);
            if (job.getStatus() == DfsJob.Status.CANCELED
                    || Thread.currentThread().isInterrupted()
                    || InterruptUtil.isInterrupted(ex)) {
                job.markCanceled("canceled");
                return;
            }
            String staleReason = executionStaleReason(job);
            if (!staleReason.isBlank()) {
                job.markFailed(new IllegalStateException(staleReason));
                return;
            }
            logger.warn("dfs job failed: {}", ex.toString(), ex);
            job.markFailed(ex);
        }
    }

    static boolean isExecutionProjectCurrent(String projectKey) {
        return isExecutionProjectCurrent(projectKey, -1L);
    }

    static boolean isExecutionProjectCurrent(String projectKey, long projectEpoch) {
        String normalized = ActiveProjectContext.normalizeProjectKey(projectKey);
        if (normalized.isBlank()) {
            return false;
        }
        if (ActiveProjectContext.isProjectMutationInProgress(normalized)) {
            return false;
        }
        String published = ActiveProjectContext.normalizeProjectKey(
                ActiveProjectContext.getPublishedActiveProjectKey());
        if (!normalized.equals(published)) {
            return false;
        }
        return projectEpoch <= 0L || projectEpoch == ActiveProjectContext.currentEpoch();
    }

    static String executionStaleReason(DfsJob job) {
        if (job == null) {
            return "project_switch_required";
        }
        if (!isExecutionProjectCurrent(job.getProjectKey(), job.getProjectEpoch())) {
            return "project_switch_required";
        }
        if (ProjectStateUtil.isProjectBuildStale(job.getProjectKey(), job.getBuildSeq())) {
            return "db_changed";
        }
        return "";
    }

    static String viewStaleReason(DfsJob job) {
        if (job == null) {
            return "project_switch_required";
        }
        if (!isExecutionProjectCurrent(job.getProjectKey())) {
            return "project_switch_required";
        }
        if (ProjectStateUtil.isProjectBuildStale(job.getProjectKey(), job.getBuildSeq())) {
            return "db_changed";
        }
        return "";
    }

    private DfsApiUtil.DfsRequest copyRequest(DfsApiUtil.DfsRequest req) {
        DfsApiUtil.DfsRequest r = new DfsApiUtil.DfsRequest();
        r.fromSink = req.fromSink;
        r.searchAllSources = req.searchAllSources;
        r.depth = req.depth;
        r.maxLimit = req.maxLimit;
        r.maxPaths = req.maxPaths;
        r.maxNodes = req.maxNodes;
        r.maxEdges = req.maxEdges;
        r.timeoutMs = req.timeoutMs;
        r.onlyFromWeb = req.onlyFromWeb;
        r.blacklist = req.blacklist == null ? new HashSet<>() : new HashSet<>(req.blacklist);
        r.sinkName = req.sinkName;
        r.sinkClass = req.sinkClass;
        r.sinkMethod = req.sinkMethod;
        r.sinkDesc = req.sinkDesc;
        r.sinkJarId = req.sinkJarId;
        r.sourceClass = req.sourceClass;
        r.sourceMethod = req.sourceMethod;
        r.sourceDesc = req.sourceDesc;
        r.sourceJarId = req.sourceJarId;
        r.minEdgeConfidence = req.minEdgeConfidence;
        r.traversalMode = req.traversalMode;
        r.projectKey = req.projectKey;
        return r;
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        List<String> removeIds = new ArrayList<>();
        for (Map.Entry<String, DfsJob> entry : jobs.entrySet()) {
            DfsJob job = entry.getValue();
            if (job == null) {
                removeIds.add(entry.getKey());
                continue;
            }
            DfsJob.Status st = job.getStatus();
            if ((st == DfsJob.Status.DONE || st == DfsJob.Status.FAILED || st == DfsJob.Status.CANCELED)
                    && job.getFinishedAt() > 0
                    && now - job.getFinishedAt() > JOB_TTL_MS) {
                removeIds.add(entry.getKey());
            }
        }
        for (String id : removeIds) {
            jobs.remove(id);
        }
    }

    void shutdownForTest() {
        executor.shutdownNow();
        if (cleaner != null) {
            cleaner.shutdownNow();
        }
    }

    private static int resolvePoolSize() {
        return Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
    }

    private static int resolveQueueCapacity(int poolSize) {
        return Math.max(MIN_QUEUE_CAPACITY, poolSize * 8);
    }

    private static ThreadPoolExecutor newExecutor(int poolSize,
                                                  int queueCapacity,
                                                  ThreadFactory threadFactory) {
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(Math.max(1, queueCapacity));
        return new ThreadPoolExecutor(
                Math.max(1, poolSize),
                Math.max(1, poolSize),
                0L,
                TimeUnit.MILLISECONDS,
                queue,
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
