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
import me.n1ar4.jar.analyzer.entity.AnnoMethodResult;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MethodsSearchHandler extends ApiBaseHandler implements HttpHandler {
    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 2000;

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = MainForm.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        boolean includeJdk = includeJdk(session);
        Integer jarId = getIntParamNullable(session, "jarId");
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

        String annoRaw = getStringParam(session, "anno", "annotations");
        if (!StringUtil.isNull(annoRaw)) {
            List<String> annos = splitListParam(annoRaw);
            if (annos.isEmpty()) {
                return needParam("anno");
            }
            String annoMatch = getStringParam(session, "annoMatch");
            String annoScope = getStringParam(session, "annoScope");
            List<AnnoMethodResult> results = engine.getMethodsByAnno(
                    annos, annoMatch, annoScope, jarId, offset, limit);
            List<AnnoMethodResult> filtered = filterAnnoMethods(results, includeJdk);
            Map<String, Object> meta = pageMeta(offset, limit,
                    filtered == null ? 0 : filtered.size(), null);
            meta.put("resultType", "anno");
            return ok(filtered, meta);
        }

        String str = getStringParam(session, "str", "q");
        if (!StringUtil.isNull(str)) {
            String mode = getStringParam(session, "strMode", "strMatch", "mode");
            String classLike = getStringParam(session, "classLike", "classPrefix");
            if (!StringUtil.isNull(classLike)) {
                classLike = normalizeClassName(classLike);
            }
            int fetchLimit = limit + Math.max(0, offset);
            List<MethodResult> results = engine.getMethodsByStr(
                    str, jarId, classLike, fetchLimit, mode);
            results = filterMethods(results, includeJdk);
            List<MethodResult> page = applyLimitOffset(results, offset, limit);
            Map<String, Object> meta = pageMeta(offset, limit, page.size(), null);
            meta.put("resultType", "method");
            return ok(page, meta);
        }

        String className = getClassParam(session);
        String methodName = getStringParam(session, "method", "methodName");
        String methodDesc = getStringParam(session, "desc", "methodDesc");
        if (StringUtil.isNull(className)) {
            return needParam("class|str|anno");
        }

        List<MethodResult> results;
        if (StringUtil.isNull(methodName)) {
            results = engine.getMethodsByClass(className);
        } else {
            String match = getStringParam(session, "match", "methodMatch");
            boolean like = "like".equalsIgnoreCase(match) || "fuzzy".equalsIgnoreCase(match);
            results = like
                    ? engine.getMethodLike(className, methodName, methodDesc)
                    : engine.getMethod(className, methodName, methodDesc);
        }
        results = filterMethods(results, includeJdk);
        List<MethodResult> page = applyLimitOffset(results, offset, limit);
        Map<String, Object> meta = pageMeta(offset, limit, page.size(), null);
        meta.put("resultType", "method");
        return ok(page, meta);
    }
}
