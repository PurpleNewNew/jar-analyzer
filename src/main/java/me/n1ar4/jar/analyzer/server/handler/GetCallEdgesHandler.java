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
import me.n1ar4.jar.analyzer.entity.MethodCallResult;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.server.handler.base.BaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetCallEdgesHandler extends BaseHandler implements HttpHandler {
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
        if (shouldExcludeNoise(session)) {
            res = filterNoise(res, byCallee);
        }

        Map<String, Object> out = new HashMap<>();
        out.put("mode", byCallee ? "callers" : "callees");
        out.put("count", res.size());
        out.put("items", res);
        String json = JSON.toJSONString(out);
        return buildJSON(json);
    }

    private ArrayList<MethodCallResult> filterNoise(List<MethodCallResult> items, boolean byCallee) {
        ArrayList<MethodCallResult> out = new ArrayList<>();
        if (items == null) {
            return out;
        }
        for (MethodCallResult r : items) {
            if (r == null) {
                continue;
            }
            String className = byCallee ? r.getCallerClassName() : r.getCalleeClassName();
            String jarName = byCallee ? r.getCallerJarName() : r.getCalleeJarName();
            if (!isJdkClass(className) && !isNoisyJar(jarName)) {
                out.add(r);
            }
        }
        return out;
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
}
