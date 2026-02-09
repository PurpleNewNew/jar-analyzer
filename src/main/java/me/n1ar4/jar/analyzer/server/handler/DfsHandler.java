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
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.server.handler.api.ApiBaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;

import java.util.HashMap;
import java.util.Map;

public class DfsHandler extends ApiBaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = EngineContext.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        DfsApiUtil.ParseResult parse = DfsApiUtil.parse(session);
        if (parse.getError() != null) {
            return parse.getError();
        }
        DfsJob job = DfsJobManager.getInstance().createJob(parse.getRequest());
        Map<String, Object> result = new HashMap<>();
        result.put("jobId", job.getJobId());
        result.put("status", job.getStatus().name().toLowerCase());
        result.put("buildSeq", job.getBuildSeq());
        result.put("acceptedAt", job.getCreatedAt());
        return ok(result);
    }
}
