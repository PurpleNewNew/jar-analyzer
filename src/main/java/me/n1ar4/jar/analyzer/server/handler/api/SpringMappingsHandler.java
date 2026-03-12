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
import me.n1ar4.jar.analyzer.engine.model.MethodView;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SpringMappingsHandler extends ApiBaseHandler implements HttpHandler {
    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 2000;

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = requireReadyEngine();
        if (engine == null) {
            return projectNotReady();
        }
        boolean includeJdk = includeJdk(session);
        String className = getClassParam(session);
        Integer jarId = getIntParamNullable(session, "jarId");
        String keyword = getStringParam(session, "keyword", "q");

        List<MethodView> results;
        List<MethodView> filtered;
        int offset = 0;
        int limit = DEFAULT_LIMIT;
        if (!StringUtil.isNull(className)) {
            results = engine.getSpringM(className);
            filtered = filterMethods(results, includeJdk);
        } else {
            offset = normalizeOffset(getIntParam(session, "offset", 0));
            limit = normalizeLimit(getIntParam(session, "limit", DEFAULT_LIMIT), DEFAULT_LIMIT, MAX_LIMIT);
            CoreEngine.PageSlice<MethodView> page = engine.getSpringMappingsPage(
                    jarId, keyword, offset, limit, includeJdk);
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.putAll(pageMeta(offset, limit, page.items().size(), page.total(), page.truncated()));
            return ok(page.items(), meta);
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        if (!StringUtil.isNull(className)) {
            meta.put("count", filtered.size());
            meta.put("class", className);
        }
        return ok(filtered, meta);
    }
}
