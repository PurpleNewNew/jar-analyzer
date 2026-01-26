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
import me.n1ar4.jar.analyzer.dfs.DFSEdge;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.server.handler.api.ApiBaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DfsJobHandler extends ApiBaseHandler implements HttpHandler {
    private static final String PREFIX = "/api/flow/dfs/jobs/";
    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 2000;
    private static final String SCHEMA_VERSION = "1";

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        String uri = session.getUri();
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
        DfsJobManager manager = DfsJobManager.getInstance();
        DfsJob job = manager.getJob(jobId);
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

    private NanoHTTPD.Response status(String jobId, DfsJob job) {
        Map<String, Object> result = new HashMap<>();
        result.put("jobId", jobId);
        result.put("schemaVersion", SCHEMA_VERSION);
        result.put("status", job.getStatus().name().toLowerCase());
        result.put("createdAt", job.getCreatedAt());
        result.put("startedAt", job.getStartedAt());
        result.put("updatedAt", job.getUpdatedAt());
        result.put("finishedAt", job.getFinishedAt());
        Map<String, Object> stats = new HashMap<>();
        stats.put("nodeCount", job.getNodeCount());
        stats.put("edgeCount", job.getEdgeCount());
        stats.put("pathCount", job.getResultCount());
        stats.put("elapsedMs", job.getElapsedMs());
        stats.put("truncated", job.isTruncated());
        stats.put("truncateReason", job.getTruncateReason());
        stats.put("recommend", job.getRecommend());
        result.put("stats", stats);
        if (!StringUtil.isNull(job.getError())) {
            result.put("error", job.getError());
        }
        return ok(result);
    }

    private NanoHTTPD.Response results(String jobId, DfsJob job, NanoHTTPD.IHTTPSession session) {
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
        List<DFSResult> items = job.getResultsSnapshot(offset, limit);
        boolean compact = getBoolParam(session, "compact");
        Map<String, Object> result = new HashMap<>();
        result.put("jobId", jobId);
        result.put("schemaVersion", SCHEMA_VERSION);
        result.put("status", job.getStatus().name().toLowerCase());
        result.put("offset", offset);
        result.put("limit", limit);
        result.put("totalFound", job.getResultCount());
        result.put("truncated", job.isTruncated());
        if (compact) {
            result.put("items", compactItems(items));
        } else {
            result.put("items", items);
        }
        return ok(result);
    }

    private NanoHTTPD.Response cancel(String jobId, DfsJob job) {
        job.cancel();
        Map<String, Object> result = new HashMap<>();
        result.put("jobId", jobId);
        result.put("schemaVersion", SCHEMA_VERSION);
        result.put("status", job.getStatus().name().toLowerCase());
        return ok(result);
    }

    private List<Map<String, Object>> compactItems(List<DFSResult> items) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return out;
        }
        for (DFSResult r : items) {
            if (r == null) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("depth", r.getDepth());
            item.put("mode", r.getMode());
            item.put("source", r.getSource());
            item.put("sink", r.getSink());
            item.put("truncated", r.isTruncated());
            item.put("truncateReason", r.getTruncateReason());
            List<Map<String, String>> methods = new ArrayList<>();
            if (r.getMethodList() != null) {
                for (me.n1ar4.jar.analyzer.core.reference.MethodReference.Handle m : r.getMethodList()) {
                    if (m == null || m.getClassReference() == null) {
                        continue;
                    }
                    Map<String, String> mm = new HashMap<>();
                    mm.put("class", m.getClassReference().getName());
                    mm.put("method", m.getName());
                    mm.put("desc", m.getDesc());
                    methods.add(mm);
                }
            }
            item.put("methods", methods);
            List<Map<String, Object>> edges = new ArrayList<>();
            if (r.getEdges() != null) {
                for (DFSEdge e : r.getEdges()) {
                    if (e == null) {
                        continue;
                    }
                    Map<String, Object> em = new HashMap<>();
                    if (e.getFrom() != null && e.getFrom().getClassReference() != null) {
                        em.put("fromClass", e.getFrom().getClassReference().getName());
                        em.put("fromMethod", e.getFrom().getName());
                        em.put("fromDesc", e.getFrom().getDesc());
                    }
                    if (e.getTo() != null && e.getTo().getClassReference() != null) {
                        em.put("toClass", e.getTo().getClassReference().getName());
                        em.put("toMethod", e.getTo().getName());
                        em.put("toDesc", e.getTo().getDesc());
                    }
                    em.put("type", e.getType());
                    em.put("confidence", e.getConfidence());
                    em.put("evidence", e.getEvidence());
                    edges.add(em);
                }
            }
            item.put("edges", edges);
            out.add(item);
        }
        return out;
    }

    private NanoHTTPD.Response notFound() {
        return buildError(
                NanoHTTPD.Response.Status.NOT_FOUND,
                "dfs_job_not_found",
                "dfs job not found");
    }
}
