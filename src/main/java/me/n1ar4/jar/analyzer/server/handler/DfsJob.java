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
import me.n1ar4.jar.analyzer.graph.flow.FlowStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class DfsJob {
    enum Status {
        QUEUED,
        RUNNING,
        DONE,
        FAILED,
        CANCELED
    }

    private final String jobId;
    private final DfsApiUtil.DfsRequest request;
    private final long buildSeq;
    private final long createdAt;
    private volatile long startedAt;
    private volatile long updatedAt;
    private volatile long finishedAt;
    private volatile Status status;
    private volatile String error;
    private final AtomicBoolean canceled = new AtomicBoolean(false);
    private volatile Future<?> future;
    private volatile List<DFSResult> results;

    private volatile boolean truncated;
    private volatile String truncateReason;
    private volatile String recommend;
    private volatile int nodeCount;
    private volatile int edgeCount;
    private volatile int pathCount;
    private volatile long elapsedMs;

    DfsJob(String jobId, DfsApiUtil.DfsRequest request, long buildSeq) {
        this.jobId = jobId;
        this.request = request;
        this.buildSeq = buildSeq;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.status = Status.QUEUED;
    }

    void attachFuture(Future<?> future) {
        this.future = future;
    }

    void markRunning() {
        this.status = Status.RUNNING;
        this.startedAt = System.currentTimeMillis();
        this.updatedAt = this.startedAt;
    }

    void markDone(List<DFSResult> results, FlowStats stats) {
        this.results = results == null ? Collections.emptyList() : results;
        updateStats(stats);
        this.finishedAt = System.currentTimeMillis();
        this.updatedAt = this.finishedAt;
        this.status = canceled.get() ? Status.CANCELED : Status.DONE;
    }

    void markFailed(Throwable t) {
        this.error = t == null ? "" : t.toString();
        this.finishedAt = System.currentTimeMillis();
        this.updatedAt = this.finishedAt;
        this.status = Status.FAILED;
    }

    void markCanceled(String reason) {
        this.error = reason == null ? "" : reason;
        this.finishedAt = System.currentTimeMillis();
        this.updatedAt = this.finishedAt;
        this.status = Status.CANCELED;
    }

    boolean cancel() {
        if (canceled.compareAndSet(false, true)) {
            Future<?> f = this.future;
            if (f != null) {
                f.cancel(true);
            }
            if (status != Status.DONE && status != Status.FAILED) {
                status = Status.CANCELED;
                updatedAt = System.currentTimeMillis();
            }
            return true;
        }
        return false;
    }

    private void updateStats(FlowStats stats) {
        if (stats == null) {
            return;
        }
        this.truncated = stats.getTruncation().truncated();
        this.truncateReason = stats.getTruncation().reason();
        this.recommend = stats.getTruncation().recommend();
        this.nodeCount = stats.getNodeCount();
        this.edgeCount = stats.getEdgeCount();
        this.pathCount = stats.getPathCount();
        this.elapsedMs = stats.getElapsedMs();
    }

    int getResultCount() {
        return results == null ? 0 : results.size();
    }

    List<DFSResult> getResultsSnapshot(int offset, int limit) {
        List<DFSResult> base = results == null ? Collections.emptyList() : results;
        int size = base.size();
        if (size == 0) {
            return Collections.emptyList();
        }
        int start = Math.max(0, offset);
        if (start >= size) {
            return Collections.emptyList();
        }
        int end = limit <= 0 ? size : Math.min(size, start + limit);
        return new ArrayList<>(base.subList(start, end));
    }

    boolean isTruncated() {
        return truncated;
    }

    String getTruncateReason() {
        return truncateReason;
    }

    String getRecommend() {
        return recommend;
    }

    int getNodeCount() {
        return nodeCount;
    }

    int getEdgeCount() {
        return edgeCount;
    }

    long getElapsedMs() {
        return elapsedMs;
    }

    AtomicBoolean getCancelFlag() {
        return canceled;
    }

    Status getStatus() {
        return status;
    }

    String getJobId() {
        return jobId;
    }

    DfsApiUtil.DfsRequest getRequest() {
        return request;
    }

    long getCreatedAt() {
        return createdAt;
    }

    long getStartedAt() {
        return startedAt;
    }

    long getUpdatedAt() {
        return updatedAt;
    }

    long getFinishedAt() {
        return finishedAt;
    }

    String getError() {
        return error;
    }

    long getBuildSeq() {
        return buildSeq;
    }
}
