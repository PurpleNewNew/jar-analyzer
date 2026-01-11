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

import me.n1ar4.jar.analyzer.dfs.DFSEngine;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DfsJobManager {
    private static final Logger logger = LogManager.getLogger();
    private static final DfsJobManager INSTANCE = new DfsJobManager();
    private static final long DEFAULT_TIMEOUT_MS = 2L * 60 * 60 * 1000; // 2h
    private static final long MAX_TIMEOUT_MS = 8L * 60 * 60 * 1000; // 8h hard cap
    private static final long JOB_TTL_MS = 6L * 60 * 60 * 1000; // 6h

    private final Map<String, DfsJob> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor;
    private final ScheduledExecutorService cleaner;

    private DfsJobManager() {
        int pool = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        this.executor = Executors.newFixedThreadPool(pool);
        this.cleaner = Executors.newSingleThreadScheduledExecutor();
        this.cleaner.scheduleAtFixedRate(this::cleanup, 10, 10, TimeUnit.MINUTES);
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
        DfsJob job = new DfsJob(jobId, copy);
        jobs.put(jobId, job);
        executor.submit(() -> runJob(job));
        return job;
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

    private void runJob(DfsJob job) {
        try {
            if (job.getStatus() == DfsJob.Status.CANCELED) {
                return;
            }
            job.markRunning();
            DFSEngine engine = DfsApiUtil.buildEngine(job.getRequest());
            job.attachEngine(engine);
            if (job.getStatus() == DfsJob.Status.CANCELED) {
                return;
            }
            engine.doAnalyze();
            List<DFSResult> results = engine.getResults();
            List<DFSResult> patched = DfsApiUtil.buildTruncatedMeta(job.getRequest(), engine, results);
            job.markDone(patched == null ? results : patched, engine);
        } catch (Throwable t) {
            logger.warn("dfs job failed: {}", t.toString());
            job.markFailed(t);
        }
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
        r.sinkClass = req.sinkClass;
        r.sinkMethod = req.sinkMethod;
        r.sinkDesc = req.sinkDesc;
        r.sourceClass = req.sourceClass;
        r.sourceMethod = req.sourceMethod;
        r.sourceDesc = req.sourceDesc;
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
}
