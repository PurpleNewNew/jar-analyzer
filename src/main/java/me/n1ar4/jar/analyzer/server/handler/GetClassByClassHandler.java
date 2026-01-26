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
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.server.handler.api.ApiBaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class GetClassByClassHandler extends ApiBaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = MainForm.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        String className = getClassParam(session);
        if (StringUtil.isNull(className)) {
            return needParam("class");
        }
        ClassResult clazz = engine.getClassByClass(className);
        boolean includeJdk = includeJdk(session);
        boolean filtered = false;
        if (clazz != null && !includeJdk) {
            if (isJdkClass(clazz.getClassName()) || isNoisyJar(clazz.getJarName())) {
                clazz = null;
                filtered = true;
            }
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("found", clazz != null);
        if (filtered) {
            meta.put("filtered", true);
        }
        return ok(clazz, meta);
    }
}
