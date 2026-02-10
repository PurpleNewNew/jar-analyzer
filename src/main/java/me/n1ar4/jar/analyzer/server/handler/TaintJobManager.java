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

import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.core.BuildSeqUtil;
import me.n1ar4.jar.analyzer.taint.SinkKindResolver;
import me.n1ar4.jar.analyzer.taint.TaintAnalyzer;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TaintJobManager {
    private static final Logger logger = LogManager.getLogger();
    private static final TaintJobManager INSTANCE = new TaintJobManager();
    private static final long DEFAULT_TIMEOUT_MS = 30L * 60 * 1000; // 30m
    private static final long MAX_TIMEOUT_MS = 4L * 60 * 60 * 1000; // 4h
    private static final long JOB_TTL_MS = 6L * 60 * 60 * 1000; // 6h

    private final Map<String, TaintJob> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor;
    private final ScheduledExecutorService cleaner;

    private TaintJobManager() {
        int pool = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        AtomicInteger idx = new AtomicInteger(0);
        ThreadFactory workerFactory = r -> {
            Thread t = new Thread(r, "taint-job-" + idx.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
        ThreadFactory cleanerFactory = r -> {
            Thread t = new Thread(r, "taint-job-cleaner");
            t.setDaemon(true);
            return t;
        };
        this.executor = Executors.newFixedThreadPool(pool, workerFactory);
        this.cleaner = Executors.newSingleThreadScheduledExecutor(cleanerFactory);
        this.cleaner.scheduleAtFixedRate(this::cleanup, 10, 10, TimeUnit.MINUTES);
    }

    public static TaintJobManager getInstance() {
        return INSTANCE;
    }

    public TaintJob createJob(String dfsJobId, Integer timeoutMs, Integer maxPaths, String sinkKind, long buildSeq) {
        if (timeoutMs == null || timeoutMs <= 0) {
            timeoutMs = (int) DEFAULT_TIMEOUT_MS;
        } else if (timeoutMs > MAX_TIMEOUT_MS) {
            timeoutMs = (int) MAX_TIMEOUT_MS;
        }
        String jobId = "taint_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        TaintJob job = new TaintJob(jobId, dfsJobId, buildSeq, timeoutMs, maxPaths, sinkKind);
        jobs.put(jobId, job);
        job.attachFuture(executor.submit(() -> runJob(job)));
        return job;
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

    private void runJob(TaintJob job) {
        SinkKindResolver.clearOverride();
        String sinkKind = job == null ? null : job.getSinkKind();
        if (sinkKind != null && !sinkKind.trim().isEmpty()) {
            SinkKindResolver.setOverride(sinkKind);
        }
        try {
            if (job.getStatus() == TaintJob.Status.CANCELED) {
                return;
            }
            if (BuildSeqUtil.isStale(job.getBuildSeq())) {
                job.markFailed(new IllegalStateException("db_changed"));
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
            if (dfsJob.getBuildSeq() != job.getBuildSeq() || BuildSeqUtil.isStale(dfsJob.getBuildSeq())) {
                job.markFailed(new IllegalStateException("db_changed"));
                return;
            }
            job.setDfsMeta(dfsJob.getStatus().name().toLowerCase(),
                    dfsJob.getResultCount(),
                    dfsJob.isTruncated(),
                    dfsJob.getTruncateReason());
            List<DFSResult> dfsResults = dfsJob.getResultsSnapshot(0, 0);
            long startNs = System.nanoTime();
            List<TaintResult> taintResults = TaintAnalyzer.analyze(
                    dfsResults, job.getTimeoutMs(), job.getMaxPaths(), job.getCancelFlag());
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
            if (BuildSeqUtil.isStale(job.getBuildSeq())) {
                job.markFailed(new IllegalStateException("db_changed"));
                return;
            }
            job.markDone(taintResults, elapsedMs);
        } catch (Exception ex) {
            InterruptUtil.restoreInterruptIfNeeded(ex);
            if (BuildSeqUtil.isStale(job.getBuildSeq())) {
                job.markFailed(new IllegalStateException("db_changed"));
                return;
            }
            if (job.getStatus() == TaintJob.Status.CANCELED
                    || Thread.currentThread().isInterrupted()
                    || InterruptUtil.isInterrupted(ex)) {
                job.markCanceled("canceled");
                return;
            }
            logger.warn("taint job failed: {}", ex.toString());
            job.markFailed(ex);
        } finally {
            SinkKindResolver.clearOverride();
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
}
