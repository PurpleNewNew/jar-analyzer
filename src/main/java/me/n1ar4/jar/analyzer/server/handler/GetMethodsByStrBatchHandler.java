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
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.server.handler.base.BaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetMethodsByStrBatchHandler extends BaseHandler implements HttpHandler {
    private static final int DEFAULT_LIMIT = 1000;
    private static final int MAX_LIMIT = 2000;

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = MainForm.getEngine();
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
        int limit = getIntParam(session, "limit", DEFAULT_LIMIT);
        if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }
        if (limit < 1) {
            limit = DEFAULT_LIMIT;
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Map<String, Object> row = new HashMap<>();
            String str = asString(item.get("str"));
            row.put("request", item);
            if (StringUtil.isNull(str)) {
                row.put("error", "missing str");
                out.add(row);
                continue;
            }
            Integer jarId = toInteger(item.get("jarId"));
            Integer itemLimit = toInteger(item.get("limit"));
            String mode = asString(item.get("mode"));
            String classLike = asString(item.get("class"));
            if (StringUtil.isNull(classLike)) {
                classLike = asString(item.get("package"));
            }
            if (StringUtil.isNull(classLike)) {
                classLike = asString(item.get("pkg"));
            }
            if (!StringUtil.isNull(classLike)) {
                classLike = classLike.replace('.', '/');
            }
            int finalLimit = itemLimit == null || itemLimit <= 0 ? limit : itemLimit;
            if (finalLimit > MAX_LIMIT) {
                finalLimit = MAX_LIMIT;
            }
            ArrayList<MethodResult> results = engine.getMethodsByStr(
                    str, jarId, classLike, finalLimit, mode);
            results = filterJdkMethods(results, session);
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

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
