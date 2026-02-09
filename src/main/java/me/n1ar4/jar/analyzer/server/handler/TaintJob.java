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

import me.n1ar4.jar.analyzer.taint.TaintResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaintJob {
    enum Status {
        QUEUED,
        RUNNING,
        DONE,
        FAILED,
        CANCELED
    }

    private final String jobId;
    private final String dfsJobId;
    private final Integer timeoutMs;
    private final Integer maxPaths;
    private final String sinkKind;
    private final long createdAt;
    private volatile long startedAt;
    private volatile long updatedAt;
    private volatile long finishedAt;
    private volatile Status status;
    private volatile String error;
    private final AtomicBoolean canceled = new AtomicBoolean(false);
    private volatile Future<?> future;
    private volatile List<TaintResult> results;
    private volatile int totalCount;
    private volatile int successCount;
    private volatile boolean truncated;
    private volatile String truncateReason;
    private volatile long elapsedMs;

    private volatile String dfsStatus;
    private volatile int dfsTotalFound;
    private volatile boolean dfsTruncated;
    private volatile String dfsTruncateReason;

    TaintJob(String jobId, String dfsJobId, Integer timeoutMs, Integer maxPaths, String sinkKind) {
        this.jobId = jobId;
        this.dfsJobId = dfsJobId;
        this.timeoutMs = timeoutMs;
        this.maxPaths = maxPaths;
        this.sinkKind = sinkKind;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.status = Status.QUEUED;
    }

    void markRunning() {
        this.status = Status.RUNNING;
        this.startedAt = System.currentTimeMillis();
        this.updatedAt = this.startedAt;
    }

    void markDone(List<TaintResult> results, long elapsedMs) {
        this.results = results == null ? Collections.emptyList() : results;
        this.elapsedMs = elapsedMs;
        int success = 0;
        boolean trunc = false;
        String truncReason = null;
        for (TaintResult r : this.results) {
            if (r != null && r.isSuccess()) {
                success++;
            }
            if (r != null && r.getDfsResult() != null && r.getDfsResult().isTruncated()) {
                trunc = true;
                truncReason = r.getDfsResult().getTruncateReason();
            }
        }
        this.totalCount = this.results.size();
        this.successCount = success;
        this.truncated = trunc;
        this.truncateReason = truncReason;
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

    void attachFuture(Future<?> future) {
        this.future = future;
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

    List<TaintResult> getResultsSnapshot(int offset, int limit) {
        List<TaintResult> base = results == null ? Collections.emptyList() : results;
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

    boolean isCanceled() {
        return canceled.get();
    }

    AtomicBoolean getCancelFlag() {
        return canceled;
    }

    String getJobId() {
        return jobId;
    }

    String getDfsJobId() {
        return dfsJobId;
    }

    Integer getTimeoutMs() {
        return timeoutMs;
    }

    Integer getMaxPaths() {
        return maxPaths;
    }

    String getSinkKind() {
        return sinkKind;
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

    Status getStatus() {
        return status;
    }

    String getError() {
        return error;
    }

    int getTotalCount() {
        return totalCount;
    }

    int getSuccessCount() {
        return successCount;
    }

    boolean isTruncated() {
        return truncated;
    }

    String getTruncateReason() {
        return truncateReason;
    }

    long getElapsedMs() {
        return elapsedMs;
    }

    void setDfsMeta(String status, int totalFound, boolean truncated, String reason) {
        this.dfsStatus = status;
        this.dfsTotalFound = totalFound;
        this.dfsTruncated = truncated;
        this.dfsTruncateReason = reason;
    }

    String getDfsStatus() {
        return dfsStatus;
    }

    int getDfsTotalFound() {
        return dfsTotalFound;
    }

    boolean isDfsTruncated() {
        return dfsTruncated;
    }

    String getDfsTruncateReason() {
        return dfsTruncateReason;
    }
}
