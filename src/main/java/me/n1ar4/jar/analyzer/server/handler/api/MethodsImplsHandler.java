/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.server.handler.api;

import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.List;
import java.util.Map;

public class MethodsImplsHandler extends ApiBaseHandler implements HttpHandler {
    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 2000;

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = EngineContext.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        boolean includeJdk = includeJdk(session);
        String className = getClassParam(session);
        String methodName = getStringParam(session, "method", "methodName");
        String methodDesc = getStringParam(session, "desc", "methodDesc");
        if (StringUtil.isNull(className)) {
            return needParam("class");
        }
        if (StringUtil.isNull(methodName)) {
            return needParam("method");
        }
        String direction = getStringParam(session, "direction", "mode");
        boolean superImpls = "super".equalsIgnoreCase(direction)
                || "super_impls".equalsIgnoreCase(direction)
                || "parent".equalsIgnoreCase(direction);

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

        List<MethodResult> results = superImpls
                ? engine.getSuperImpls(className, methodName, methodDesc)
                : engine.getImpls(className, methodName, methodDesc);
        results = filterMethods(results, includeJdk);
        List<MethodResult> page = applyLimitOffset(results, offset, limit);
        Map<String, Object> meta = pageMeta(offset, limit, page.size(), null);
        meta.put("direction", superImpls ? "super" : "impls");
        return ok(page, meta);
    }
}
