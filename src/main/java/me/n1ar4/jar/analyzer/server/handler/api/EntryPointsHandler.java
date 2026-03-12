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
import me.n1ar4.jar.analyzer.engine.model.ClassView;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EntryPointsHandler extends ApiBaseHandler implements HttpHandler {
    private static final int DEFAULT_LIMIT = 2000;
    private static final int MAX_LIMIT = 10000;

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = requireReadyEngine();
        if (engine == null) {
            return projectNotReady();
        }
        String typesRaw = getStringParam(session, "type", "types");
        if (StringUtil.isNull(typesRaw)) {
            return needParam("type");
        }
        List<String> types = splitListParam(typesRaw);
        if (types.isEmpty()) {
            return needParam("type");
        }
        boolean includeJdk = includeJdk(session);
        int limit = normalizeLimit(getIntParam(session, "limit", DEFAULT_LIMIT), DEFAULT_LIMIT, MAX_LIMIT);

        List<String> warnings = new ArrayList<>();
        Map<String, Object> data = new LinkedHashMap<>();
        List<String> normalizedTypes = normalizeTypes(types);
        if (normalizedTypes.contains("all")) {
            normalizedTypes = List.of(
                    "spring_controller",
                    "spring_interceptor",
                    "servlet",
                    "filter",
                    "listener"
            );
        }

        for (String type : normalizedTypes) {
            List<ClassView> items = resolveType(engine, type);
            if (items == null) {
                warnings.add("unknown type: " + type);
                continue;
            }
            List<ClassView> filtered = filterClasses(items, includeJdk);
            if (limit > 0 && filtered.size() > limit) {
                filtered = new ArrayList<>(filtered.subList(0, limit));
            }
            data.put(type, filtered);
        }

        if (data.size() == 1) {
            Object only = data.values().iterator().next();
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("type", data.keySet().iterator().next());
            meta.put("count", only instanceof List ? ((List<?>) only).size() : 0);
            return ok(only, meta, warnings);
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("types", data.keySet());
        return ok(data, meta, warnings);
    }

    private List<ClassView> resolveType(CoreEngine engine, String type) {
        return switch (type) {
            case "spring_controller" -> engine.getAllSpringC();
            case "spring_interceptor" -> engine.getAllSpringI();
            case "servlet" -> engine.getAllServlets();
            case "filter" -> engine.getAllFilters();
            case "listener" -> engine.getAllListeners();
            default -> null;
        };
    }

    private List<String> normalizeTypes(List<String> types) {
        List<String> out = new ArrayList<>();
        for (String t : types) {
            if (StringUtil.isNull(t)) {
                continue;
            }
            String v = t.trim().toLowerCase();
            if (v.isEmpty()) {
                continue;
            }
            if ("controller".equals(v)) {
                v = "spring_controller";
            } else if ("interceptor".equals(v)) {
                v = "spring_interceptor";
            }
            out.add(v);
        }
        return out;
    }
}
