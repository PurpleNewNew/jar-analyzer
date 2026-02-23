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
import me.n1ar4.jar.analyzer.entity.MethodCallResult;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.server.handler.api.ApiBaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GetCallEdgesHandler extends ApiBaseHandler implements HttpHandler {
    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 2000;

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = MainForm.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        String mode = getParam(session, "mode");
        if (StringUtil.isNull(mode)) {
            mode = getParam(session, "direction");
        }
        boolean byCallee = true;
        if (!StringUtil.isNull(mode)) {
            String m = mode.trim().toLowerCase();
            if ("callee".equals(m) || "callees".equals(m) || "to".equals(m)) {
                byCallee = false;
            }
        }

        String clazz = getClassName(session);
        String method = getMethodName(session);
        String desc = getMethodDesc(session);
        if (StringUtil.isNull(clazz)) {
            return needParam("class");
        }
        if (StringUtil.isNull(method)) {
            return needParam("method");
        }

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

        ArrayList<MethodCallResult> res;
        if (byCallee) {
            res = engine.getCallEdgesByCallee(clazz, method, desc, offset, limit);
        } else {
            res = engine.getCallEdgesByCaller(clazz, method, desc, offset, limit);
        }
        String scope = normalizeScope(getParam(session, "scope"));
        List<MethodCallResult> filtered = filterByScope(engine, res, byCallee, scope);
        Map<String, Object> meta = pageMeta(offset, limit, filtered.size(), null);
        meta.put("direction", byCallee ? "callers" : "callees");
        meta.put("view", "edges");
        meta.put("scope", scope);
        return ok(filtered, meta);
    }

    private String normalizeScope(String scope) {
        if (StringUtil.isNull(scope)) {
            return "app";
        }
        String value = scope.trim().toLowerCase();
        if ("all".equals(value)) {
            return "all";
        }
        return "app";
    }

    private List<MethodCallResult> filterByScope(CoreEngine engine,
                                                 List<MethodCallResult> input,
                                                 boolean byCallee,
                                                 String scope) {
        if (input == null || input.isEmpty()) {
            return new ArrayList<>();
        }
        if ("all".equals(scope)) {
            return input;
        }
        List<MethodCallResult> out = new ArrayList<>();
        for (MethodCallResult edge : input) {
            if (edge == null) {
                continue;
            }
            String className = byCallee ? edge.getCallerClassName() : edge.getCalleeClassName();
            Integer jarId = byCallee ? edge.getCallerJarId() : edge.getCalleeJarId();
            String role = engine.getClassRole(className, jarId);
            if (!"APP".equalsIgnoreCase(role)) {
                continue;
            }
            out.add(edge);
        }
        return out;
    }
}
