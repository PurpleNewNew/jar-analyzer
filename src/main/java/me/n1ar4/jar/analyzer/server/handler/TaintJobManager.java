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

import me.n1ar4.jar.analyzer.graph.flow.model.FlowPath;
import me.n1ar4.jar.analyzer.graph.flow.model.FlowPathEdge;
import me.n1ar4.jar.analyzer.graph.flow.GraphFlowService;
import me.n1ar4.jar.analyzer.core.ProjectStateUtil;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TaintJobManager {
    private static final Logger logger = LogManager.getLogger();
    private static final TaintJobManager INSTANCE = new TaintJobManager();
    private static final GraphFlowService FLOW_SERVICE = new GraphFlowService();
    private static final long DEFAULT_TIMEOUT_MS = 30L * 60 * 1000; // 30m
    private static final long MAX_TIMEOUT_MS = 4L * 60 * 60 * 1000; // 4h
    private static final long JOB_TTL_MS = 6L * 60 * 60 * 1000; // 6h
    private static final int MIN_QUEUE_CAPACITY = 16;

    private final Map<String, TaintJob> jobs = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor executor;
    private final ScheduledExecutorService cleaner;

    private TaintJobManager() {
        this(
                newExecutor(resolvePoolSize(), resolveQueueCapacity(resolvePoolSize()),
                        Thread.ofPlatform().name("taint-job-", 1).daemon(true).factory()),
                Executors.newSingleThreadScheduledExecutor(
                        Thread.ofPlatform().name("taint-job-cleaner").daemon(true).factory()),
                true
        );
    }

    TaintJobManager(ThreadPoolExecutor executor,
                    ScheduledExecutorService cleaner,
                    boolean scheduleCleanup) {
        this.executor = executor;
        this.cleaner = cleaner;
        if (scheduleCleanup && cleaner != null) {
            cleaner.scheduleAtFixedRate(this::cleanup, 10, 10, TimeUnit.MINUTES);
        }
    }

    public static TaintJobManager getInstance() {
        return INSTANCE;
    }

    public TaintJob createJob(String dfsJobId,
                              String projectKey,
                              Integer timeoutMs,
                              Integer maxPaths,
                              String sinkKind,
                              long buildSeq) {
        if (timeoutMs == null || timeoutMs <= 0) {
            timeoutMs = (int) DEFAULT_TIMEOUT_MS;
        } else if (timeoutMs > MAX_TIMEOUT_MS) {
            timeoutMs = (int) MAX_TIMEOUT_MS;
        }
        String jobId = "taint_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        long projectEpoch = ActiveProjectContext.currentEpoch();
        TaintJob job = new TaintJob(jobId, dfsJobId, projectKey, buildSeq, projectEpoch, timeoutMs, maxPaths, sinkKind);
        jobs.put(jobId, job);
        try {
            job.attachFuture(executor.submit(() -> runJob(job)));
            return job;
        } catch (RejectedExecutionException ex) {
            jobs.remove(jobId);
            throw new IllegalStateException("job_queue_full", ex);
        }
    }

    public TaintJob getJob(String jobId) {
        return jobs.get(jobId);
    }

    public boolean cancelJob(String jobId) {
        TaintJob job = jobs.get(jobId);
        if (job == null) {
            return false;
        }
        return job.cancel();
    }

    void runJob(TaintJob job) {
        try {
            if (job.getStatus() == TaintJob.Status.CANCELED) {
                return;
            }
            String staleReason = executionStaleReason(job);
            if (!staleReason.isBlank()) {
                job.markFailed(new IllegalStateException(staleReason));
                return;
            }
            job.markRunning();
            DfsJob dfsJob = DfsJobManager.getInstance().getJob(job.getDfsJobId());
            if (dfsJob == null) {
                job.markFailed(new IllegalStateException("dfs job not found"));
                return;
            }
            if (dfsJob.getStatus() != DfsJob.Status.DONE) {
                job.markFailed(new IllegalStateException("dfs job not finished"));
                return;
            }
            List<FlowPath> dfsResults = dfsJob.getResultsSnapshot(0, 0);
            if (!supportsTaintInput(dfsResults)) {
                job.markFailed(new IllegalStateException("dfs_job_direction_not_supported"));
                return;
            }
            if (dfsJob.getBuildSeq() != job.getBuildSeq()
                    || ProjectStateUtil.isProjectBuildStale(dfsJob.getProjectKey(), dfsJob.getBuildSeq())) {
                job.markFailed(new IllegalStateException("db_changed"));
                return;
            }
            job.setDfsMeta(dfsJob.getStatus().name().toLowerCase(),
                    dfsJob.getResultCount(),
                    dfsJob.isTruncated(),
                    dfsJob.getTruncateReason());
            String projectKey = job.getProjectKey();
            GraphFlowService.TaintOutcome outcome = ActiveProjectContext.withProject(projectKey, () ->
                    FLOW_SERVICE.analyzeDfsResults(
                            dfsResults,
                            job.getTimeoutMs(),
                            job.getMaxPaths(),
                            job.getCancelFlag(),
                            job.getSinkKind()
                    ));
            List<TaintResult> taintResults = outcome == null ? List.of() : outcome.results();
            long elapsedMs = outcome == null ? 0L : outcome.stats().getElapsedMs();
            if (job.getStatus() == TaintJob.Status.CANCELED || Thread.currentThread().isInterrupted()) {
                job.markCanceled("canceled");
                return;
            }
            staleReason = executionStaleReason(job);
            if (!staleReason.isBlank()) {
                job.markFailed(new IllegalStateException(staleReason));
                return;
            }
            job.markDone(taintResults, elapsedMs);
        } catch (Exception ex) {
            InterruptUtil.restoreInterruptIfNeeded(ex);
            if (job.getStatus() == TaintJob.Status.CANCELED
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
            logger.warn("taint job failed: {}", ex.toString(), ex);
            job.markFailed(ex);
        }
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        List<String> removeIds = new ArrayList<>();
        for (Map.Entry<String, TaintJob> entry : jobs.entrySet()) {
            TaintJob job = entry.getValue();
            if (job == null) {
                removeIds.add(entry.getKey());
                continue;
            }
            TaintJob.Status st = job.getStatus();
            if ((st == TaintJob.Status.DONE || st == TaintJob.Status.FAILED || st == TaintJob.Status.CANCELED)
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
        return Math.max(MIN_QUEUE_CAPACITY, poolSize * 4);
    }

    private static ThreadPoolExecutor newExecutor(int poolSize,
                                                  int queueCapacity,
                                                  ThreadFactory threadFactory) {
        return new ThreadPoolExecutor(
                Math.max(1, poolSize),
                Math.max(1, poolSize),
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(Math.max(1, queueCapacity)),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    static boolean supportsTaintInput(List<FlowPath> dfsResults) {
        if (dfsResults == null || dfsResults.isEmpty()) {
            return true;
        }
        for (FlowPath dfs : dfsResults) {
            if (!isForwardOrdered(dfs)) {
                return false;
            }
        }
        return true;
    }

    static boolean isExecutionProjectCurrent(String projectKey) {
        return DfsJobManager.isExecutionProjectCurrent(projectKey);
    }

    static boolean isExecutionProjectCurrent(String projectKey, long projectEpoch) {
        return DfsJobManager.isExecutionProjectCurrent(projectKey, projectEpoch);
    }

    static String executionStaleReason(TaintJob job) {
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

    static String viewStaleReason(TaintJob job) {
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

    private static boolean isForwardOrdered(FlowPath dfs) {
        if (dfs == null) {
            return true;
        }
        List<MethodReference.Handle> methods = dfs.getMethodList();
        if (methods == null || methods.size() < 2) {
            return true;
        }
        MethodReference.Handle first = methods.get(0);
        MethodReference.Handle last = methods.get(methods.size() - 1);
        if (dfs.getSource() != null && dfs.getSink() != null) {
            if (sameHandle(dfs.getSource(), first) && sameHandle(dfs.getSink(), last)) {
                return true;
            }
            if (sameHandle(dfs.getSource(), last) && sameHandle(dfs.getSink(), first)) {
                return false;
            }
        }
        List<FlowPathEdge> edges = dfs.getEdges();
        if (matchesEdgeOrder(methods, edges, false)) {
            return true;
        }
        if (matchesEdgeOrder(methods, edges, true)) {
            return false;
        }
        return false;
    }

    private static boolean matchesEdgeOrder(List<MethodReference.Handle> methods,
                                            List<FlowPathEdge> edges,
                                            boolean reverse) {
        if (methods == null || methods.size() < 2 || edges == null || edges.size() != methods.size() - 1) {
            return false;
        }
        for (int i = 0; i < edges.size(); i++) {
            FlowPathEdge edge = edges.get(i);
            MethodReference.Handle expectedFrom = reverse ? methods.get(i + 1) : methods.get(i);
            MethodReference.Handle expectedTo = reverse ? methods.get(i) : methods.get(i + 1);
            if (!sameHandle(edge == null ? null : edge.getFrom(), expectedFrom)
                    || !sameHandle(edge == null ? null : edge.getTo(), expectedTo)) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameHandle(MethodReference.Handle left, MethodReference.Handle right) {
        return Objects.equals(left, right);
    }
}
