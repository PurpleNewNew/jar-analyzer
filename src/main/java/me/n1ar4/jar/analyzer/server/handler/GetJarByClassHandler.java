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
import me.n1ar4.jar.analyzer.entity.JarEntity;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.server.handler.api.ApiBaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.JarFingerprintUtil;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GetJarByClassHandler extends ApiBaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = EngineContext.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        String className = getClassParam(session);
        if (StringUtil.isNull(className)) {
            return needParam("class");
        }
        String jarName = engine.getJarByClass(className);
        List<Map<String, Object>> items = new ArrayList<>();
        if (!StringUtil.isNull(jarName)) {
            List<JarEntity> jars = engine.getJarsMeta();
            for (JarEntity jar : jars) {
                if (jar == null) {
                    continue;
                }
                if (!jarName.equals(jar.getJarName())) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("jar_id", jar.getJid());
                item.put("jar_name", jar.getJarName());
                item.put("jar_fingerprint", JarFingerprintUtil.sha256(jar.getJarAbsPath()));
                items.add(item);
            }
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("count", items.size());
        return ok(items, meta);
    }
}
