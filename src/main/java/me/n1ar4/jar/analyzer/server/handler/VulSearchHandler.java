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
import me.n1ar4.jar.analyzer.entity.FindingSummary;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.server.handler.api.ApiBaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.*;

public class VulSearchHandler extends ApiBaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = MainForm.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        Set<String> nameFilter = parseNames(getParam(session, "name"));
        String level = normalizeLevel(getParam(session, "level"));
        int offset = getIntParam(session, "offset", 0);
        if (offset < 0) {
            offset = 0;
        }
        int limit = getIntParam(session, "limit", 200);
        if (limit < 1) {
            limit = 200;
        }
        List<FindingSummary> summaries = FindingEngineV2.search(
                engine, nameFilter, level, offset, limit);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("scope", "app");
        meta.put("schema", "finding_summary_v2");
        meta.put("offset", offset);
        meta.put("limit", limit);
        meta.put("count", summaries.size());
        return ok(summaries, meta);
    }

    private Set<String> parseNames(String raw) {
        if (StringUtil.isNull(raw)) {
            return Collections.emptySet();
        }
        Set<String> out = new LinkedHashSet<>();
        String[] parts = raw.split("[,;\\r\\n]+");
        for (String part : parts) {
            if (StringUtil.isNull(part)) {
                continue;
            }
            out.add(part.trim());
        }
        return out;
    }

    private String normalizeLevel(String level) {
        if (StringUtil.isNull(level)) {
            return null;
        }
        String value = level.trim().toLowerCase(Locale.ROOT);
        if ("high".equals(value) || "medium".equals(value) || "low".equals(value)) {
            return value;
        }
        return null;
    }
}
