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

import com.alibaba.fastjson2.JSON;
import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.taint.TaintResult;
import me.n1ar4.jar.analyzer.server.handler.base.BaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaintJobHandler extends BaseHandler implements HttpHandler {
    private static final String BASE = "/api/taint/jobs";
    private static final String ALIAS = "/api/taint";
    private static final String PREFIX = "/api/taint/jobs/";
    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 2000;

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
        CoreEngine engine = MainForm.getEngine();
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
        Integer timeoutMs = getIntParam(session, "timeoutMs");
        Integer maxPaths = getIntParam(session, "maxPaths");
        TaintJob job = TaintJobManager.getInstance().createJob(dfsJobId, timeoutMs, maxPaths);
        Map<String, Object> result = new HashMap<>();
        result.put("jobId", job.getJobId());
        result.put("status", job.getStatus().name().toLowerCase());
        result.put("createdAt", job.getCreatedAt());
        result.put("dfsJobId", job.getDfsJobId());
        String json = JSON.toJSONString(result);
        return buildJSON(json);
    }

    private NanoHTTPD.Response status(String jobId, TaintJob job) {
        Map<String, Object> result = new HashMap<>();
        result.put("jobId", jobId);
        result.put("dfsJobId", job.getDfsJobId());
        result.put("status", job.getStatus().name().toLowerCase());
        result.put("createdAt", job.getCreatedAt());
        result.put("startedAt", job.getStartedAt());
        result.put("updatedAt", job.getUpdatedAt());
        result.put("finishedAt", job.getFinishedAt());
        result.put("timeoutMs", job.getTimeoutMs());
        result.put("maxPaths", job.getMaxPaths());

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
        String json = JSON.toJSONString(result);
        return buildJSON(json);
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
        result.put("status", job.getStatus().name().toLowerCase());
        result.put("offset", offset);
        result.put("limit", limit);
        result.put("totalFound", job.getTotalCount());
        result.put("items", items);
        String json = JSON.toJSONString(result);
        return buildJSON(json);
    }

    private NanoHTTPD.Response cancel(String jobId, TaintJob job) {
        job.cancel();
        Map<String, Object> result = new HashMap<>();
        result.put("jobId", jobId);
        result.put("status", job.getStatus().name().toLowerCase());
        String json = JSON.toJSONString(result);
        return buildJSON(json);
    }

    private Integer getIntParam(NanoHTTPD.IHTTPSession session, String key) {
        String value = getParam(session, key);
        if (StringUtil.isNull(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private int getIntParam(NanoHTTPD.IHTTPSession session, String key, int def) {
        String value = getParam(session, key);
        if (StringUtil.isNull(value)) {
            return def;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return def;
        }
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
