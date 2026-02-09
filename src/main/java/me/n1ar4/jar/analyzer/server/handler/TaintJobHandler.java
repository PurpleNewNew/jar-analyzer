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

import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.core.BuildSeqUtil;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import me.n1ar4.jar.analyzer.server.handler.api.ApiBaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaintJobHandler extends ApiBaseHandler implements HttpHandler {
    private static final String BASE = "/api/flow/taint/jobs";
    private static final String ALIAS = "/api/flow/taint";
    private static final String PREFIX = "/api/flow/taint/jobs/";
    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 2000;
    private static final String SCHEMA_VERSION = "1";

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        String uri = session.getUri();
        if (BASE.equals(uri) || ALIAS.equals(uri)) {
            return create(session);
        }
        if (StringUtil.isNull(uri) || !uri.startsWith(PREFIX)) {
            return needParam("jobId");
        }
        String rest = uri.substring(PREFIX.length());
        if (StringUtil.isNull(rest)) {
            return needParam("jobId");
        }
        String jobId = rest;
        String action = null;
        int idx = rest.indexOf('/');
        if (idx >= 0) {
            jobId = rest.substring(0, idx);
            action = rest.substring(idx + 1);
        }
        if (StringUtil.isNull(jobId)) {
            return needParam("jobId");
        }
        TaintJobManager manager = TaintJobManager.getInstance();
        TaintJob job = manager.getJob(jobId);
        if (job == null) {
            return notFound();
        }
        if ("results".equalsIgnoreCase(action)) {
            return results(jobId, job, session);
        }
        if ("cancel".equalsIgnoreCase(action) || session.getMethod() == NanoHTTPD.Method.DELETE) {
            return cancel(jobId, job);
        }
        return status(jobId, job);
    }

    private NanoHTTPD.Response create(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = EngineContext.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        String dfsJobId = getParam(session, "dfsJobId");
        if (StringUtil.isNull(dfsJobId)) {
            return needParam("dfsJobId");
        }
        DfsJob dfsJob = DfsJobManager.getInstance().getJob(dfsJobId);
        if (dfsJob == null) {
            return dfsNotFound();
        }
        if (dfsJob.getStatus() != DfsJob.Status.DONE) {
            return dfsNotReady();
        }
        if (BuildSeqUtil.isStale(dfsJob.getBuildSeq())) {
            return buildError(
                    NanoHTTPD.Response.Status.CONFLICT,
                    "db_changed",
                    "db changed, dfs job result is stale");
        }
        String sinkKind = getParam(session, "sinkKind");
        Integer timeoutMs = getIntParamNullable(session, "timeoutMs");
        Integer maxPaths = getIntParamNullable(session, "maxPaths");
        TaintJob job = TaintJobManager.getInstance().createJob(dfsJobId, timeoutMs, maxPaths, sinkKind, dfsJob.getBuildSeq());
        Map<String, Object> result = new HashMap<>();
        result.put("jobId", job.getJobId());
        result.put("schemaVersion", SCHEMA_VERSION);
        result.put("buildSeq", job.getBuildSeq());
        result.put("status", job.getStatus().name().toLowerCase());
        result.put("createdAt", job.getCreatedAt());
        result.put("dfsJobId", job.getDfsJobId());
        if (!StringUtil.isNull(job.getSinkKind())) {
            result.put("sinkKind", job.getSinkKind());
        }
        return ok(result);
    }

    private NanoHTTPD.Response status(String jobId, TaintJob job) {
        Map<String, Object> result = new HashMap<>();
        result.put("jobId", jobId);
        result.put("dfsJobId", job.getDfsJobId());
        result.put("schemaVersion", SCHEMA_VERSION);
        result.put("buildSeq", job.getBuildSeq());
        result.put("status", job.getStatus().name().toLowerCase());
        result.put("createdAt", job.getCreatedAt());
        result.put("startedAt", job.getStartedAt());
        result.put("updatedAt", job.getUpdatedAt());
        result.put("finishedAt", job.getFinishedAt());
        result.put("timeoutMs", job.getTimeoutMs());
        result.put("maxPaths", job.getMaxPaths());
        if (!StringUtil.isNull(job.getSinkKind())) {
            result.put("sinkKind", job.getSinkKind());
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", job.getTotalCount());
        stats.put("successCount", job.getSuccessCount());
        stats.put("elapsedMs", job.getElapsedMs());
        stats.put("truncated", job.isTruncated());
        stats.put("truncateReason", job.getTruncateReason());
        result.put("stats", stats);

        Map<String, Object> dfs = new HashMap<>();
        dfs.put("status", job.getDfsStatus());
        dfs.put("totalFound", job.getDfsTotalFound());
        dfs.put("truncated", job.isDfsTruncated());
        dfs.put("truncateReason", job.getDfsTruncateReason());
        result.put("dfs", dfs);

        if (!StringUtil.isNull(job.getError())) {
            result.put("error", job.getError());
        }
        return ok(result);
    }

    private NanoHTTPD.Response results(String jobId, TaintJob job, NanoHTTPD.IHTTPSession session) {
        int offset = getIntParam(session, "offset", 0);
        int limit = getIntParam(session, "limit", DEFAULT_LIMIT);
        if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }
        if (limit < 1) {
            limit = DEFAULT_LIMIT;
        }
        if (offset < 0) {
            offset = 0;
        }
        List<TaintResult> items = job.getResultsSnapshot(offset, limit);
        Map<String, Object> result = new HashMap<>();
        result.put("jobId", jobId);
        result.put("dfsJobId", job.getDfsJobId());
        result.put("schemaVersion", SCHEMA_VERSION);
        result.put("buildSeq", job.getBuildSeq());
        result.put("status", job.getStatus().name().toLowerCase());
        result.put("offset", offset);
        result.put("limit", limit);
        result.put("totalFound", job.getTotalCount());
        result.put("items", items);
        if (!StringUtil.isNull(job.getSinkKind())) {
            result.put("sinkKind", job.getSinkKind());
        }
        return ok(result);
    }

    private NanoHTTPD.Response cancel(String jobId, TaintJob job) {
        job.cancel();
        Map<String, Object> result = new HashMap<>();
        result.put("jobId", jobId);
        result.put("schemaVersion", SCHEMA_VERSION);
        result.put("status", job.getStatus().name().toLowerCase());
        return ok(result);
    }

    private NanoHTTPD.Response notFound() {
        return buildError(
                NanoHTTPD.Response.Status.NOT_FOUND,
                "taint_job_not_found",
                "taint job not found");
    }

    private NanoHTTPD.Response dfsNotFound() {
        return buildError(
                NanoHTTPD.Response.Status.NOT_FOUND,
                "dfs_job_not_found",
                "dfs job not found");
    }

    private NanoHTTPD.Response dfsNotReady() {
        return buildError(
                NanoHTTPD.Response.Status.CONFLICT,
                "dfs_job_not_finished",
                "dfs job not finished");
    }
}
