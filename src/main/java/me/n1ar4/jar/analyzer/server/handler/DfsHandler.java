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
import me.n1ar4.jar.analyzer.server.handler.api.ApiBaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;

import java.util.HashMap;
import java.util.Map;

public class DfsHandler extends ApiBaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        DfsApiUtil.ParseResult parse = DfsApiUtil.parse(session);
        if (parse.getError() != null) {
            return parse.getError();
        }
        NanoHTTPD.Response projectRequestError = requireActiveProjectRequest(parse.getRequest().projectKey);
        if (projectRequestError != null) {
            return projectRequestError;
        }
        parse.getRequest().projectKey = ActiveProjectContext.getPublishedActiveProjectKey();
        NanoHTTPD.Response preflight = DfsApiUtil.preflight(parse.getRequest());
        if (preflight != null) {
            return preflight;
        }
        DfsJob job = DfsJobManager.getInstance().createJob(parse.getRequest());
        Map<String, Object> result = new HashMap<>();
        result.put("jobId", job.getJobId());
        result.put("status", job.getStatus().name().toLowerCase());
        result.put("buildSeq", job.getProjectSnapshot());
        result.put("acceptedAt", job.getCreatedAt());
        return ok(result);
    }
}
