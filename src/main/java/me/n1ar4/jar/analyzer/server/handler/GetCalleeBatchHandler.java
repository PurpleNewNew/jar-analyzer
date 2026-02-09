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
import com.alibaba.fastjson2.TypeReference;
import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.server.handler.base.BaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetCalleeBatchHandler extends BaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = EngineContext.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        String itemsRaw = getParam(session, "items");
        if (StringUtil.isNull(itemsRaw)) {
            return needParam("items");
        }
        List<Map<String, Object>> items;
        try {
            items = JSON.parseObject(itemsRaw, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            return needParam("items");
        }
        int limit = getIntParam(session, "limit", 0);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Map<String, Object> row = new HashMap<>();
            String clazz = normalizeValue(asString(item.get("class")));
            String method = asString(item.get("method"));
            String desc = normalizeValue(asString(item.get("desc")));
            row.put("request", item);
            if (StringUtil.isNull(clazz) || StringUtil.isNull(method)) {
                row.put("error", "missing class/method");
                out.add(row);
                continue;
            }
            ArrayList<MethodResult> results = engine.getCallee(clazz, method, desc);
            results = filterJdkMethods(results, session);
            if (limit > 0 && results.size() > limit) {
                results = new ArrayList<>(results.subList(0, limit));
            }
            row.put("count", results.size());
            row.put("results", results);
            out.add(row);
        }
        String json = JSON.toJSONString(out);
        return buildJSON(json);
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

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String normalizeValue(String value) {
        if (StringUtil.isNull(value)) {
            return null;
        }
        String v = value.trim();
        if (v.isEmpty() || "null".equalsIgnoreCase(v)) {
            return null;
        }
        return v.replace('.', '/');
    }
}
