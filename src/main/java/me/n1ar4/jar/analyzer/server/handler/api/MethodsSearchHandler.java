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
import me.n1ar4.jar.analyzer.engine.EngineContext;
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
        CoreEngine engine = EngineContext.getEngine();
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
            String annoMatch = getStringParam(session, "annoMatch");
            List<String> annos = normalizeAnnoNames(splitListParam(annoRaw), annoMatch);
            if (annos.isEmpty()) {
                return needParam("anno");
            }
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

    private List<String> normalizeAnnoNames(List<String> raw, String match) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        String matchMode = match == null ? "contains" : match.trim().toLowerCase();
        if (!"equal".equals(matchMode)) {
            matchMode = "contains";
        }
        for (String item : raw) {
            if (StringUtil.isNull(item)) {
                continue;
            }
            String val = item.trim();
            if (val.isEmpty()) {
                continue;
            }
            if (val.startsWith("@")) {
                val = val.substring(1);
            }
            if ("equal".equals(matchMode)) {
                if (!(val.startsWith("L") && val.endsWith(";"))
                        && (val.contains(".") || val.contains("/"))) {
                    val = "L" + val.replace('.', '/') + ";";
                }
            } else {
                if (val.contains(".")) {
                    val = val.replace('.', '/');
                }
            }
            out.add(val);
        }
        return out;
    }
}
