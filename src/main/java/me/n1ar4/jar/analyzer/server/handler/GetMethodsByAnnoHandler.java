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
import me.n1ar4.jar.analyzer.entity.AnnoMethodResult;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.server.handler.base.BaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GetMethodsByAnnoHandler extends BaseHandler implements HttpHandler {
    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 2000;

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = EngineContext.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        String match = getParam(session, "match");
        if (StringUtil.isNull(match)) {
            match = getParam(session, "mode");
        }
        String scope = getParam(session, "scope");
        List<String> annoNames = parseAnnoNames(session, match);
        if (annoNames.isEmpty()) {
            return needParam("anno/items");
        }
        Integer jarId = getIntParam(session, "jarId");
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

        ArrayList<AnnoMethodResult> res = engine.getMethodsByAnno(
                annoNames, match, scope, jarId, offset, limit);
        res = filterNoise(res);
        String json = JSON.toJSONString(res);
        return buildJSON(json);
    }

    private List<String> parseAnnoNames(NanoHTTPD.IHTTPSession session, String match) {
        String itemsRaw = getParam(session, "items");
        if (!StringUtil.isNull(itemsRaw)) {
            try {
                List<String> items = JSON.parseArray(itemsRaw, String.class);
                if (items != null && !items.isEmpty()) {
                    return normalizeAnnoNames(items, match);
                }
            } catch (Exception ex) {
                logger.debug("invalid anno items json: {}", ex.toString());
            }
        }

        String annoRaw = getParam(session, "anno");
        if (StringUtil.isNull(annoRaw)) {
            annoRaw = getParam(session, "annoName");
        }
        if (StringUtil.isNull(annoRaw)) {
            annoRaw = getParam(session, "annoNames");
        }
        if (StringUtil.isNull(annoRaw)) {
            annoRaw = getParam(session, "q");
        }
        if (StringUtil.isNull(annoRaw)) {
            return Collections.emptyList();
        }
        List<String> names = splitNames(annoRaw);
        return normalizeAnnoNames(names, match);
    }

    private List<String> splitNames(String raw) {
        List<String> res = new ArrayList<>();
        if (StringUtil.isNull(raw)) {
            return res;
        }
        String[] parts = raw.split("[,;\\r\\n]+");
        for (String part : parts) {
            if (StringUtil.isNull(part)) {
                continue;
            }
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                res.add(trimmed);
            }
        }
        return res;
    }

    private List<String> normalizeAnnoNames(List<String> raw, String match) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        String matchMode = match == null ? "contains" : match.trim().toLowerCase();
        if (!"equal".equals(matchMode)) {
            matchMode = "contains";
        }
        List<String> out = new ArrayList<>();
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

    private ArrayList<AnnoMethodResult> filterNoise(List<AnnoMethodResult> res) {
        ArrayList<AnnoMethodResult> out = new ArrayList<>();
        if (res == null) {
            return out;
        }
        for (AnnoMethodResult r : res) {
            if (r == null) {
                continue;
            }
            String className = r.getClassName();
            if (!isJdkClass(className) && !isNoisyJar(r.getJarName())) {
                out.add(r);
            }
        }
        return out;
    }

    private Integer getIntParam(NanoHTTPD.IHTTPSession session, String key) {
        String value = getParam(session, key);
        if (StringUtil.isNull(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            logger.debug("invalid int param: {}={}", key, value);
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
        } catch (NumberFormatException ex) {
            logger.debug("invalid int param: {}={}", key, value);
            return def;
        }
    }
}
