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
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.server.handler.base.BaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class GetMethodsByStrHandler extends BaseHandler implements HttpHandler {
    private static final int DEFAULT_LIMIT = 1000;
    private static final int MAX_LIMIT = 2000;
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = EngineContext.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        String str = getStr(session);
        if (StringUtil.isNull(str)) {
            return needParam("str");
        }
        Integer jarId = getIntParam(session, "jarId");
        int limit = getIntParam(session, "limit", DEFAULT_LIMIT);
        if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }
        if (limit < 1) {
            limit = DEFAULT_LIMIT;
        }
        String mode = getParam(session, "mode");
        String classLike = getParam(session, "class");
        if (StringUtil.isNull(classLike)) {
            classLike = getParam(session, "package");
        }
        if (StringUtil.isNull(classLike)) {
            classLike = getParam(session, "pkg");
        }
        if (!StringUtil.isNull(classLike)) {
            classLike = classLike.replace('.', '/');
        }
        ArrayList<MethodResult> res = engine.getMethodsByStr(str, jarId, classLike, limit, mode);
        res = filterJdkMethods(res, session);
        String json = JSON.toJSONString(res);
        return buildJSON(json);
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
