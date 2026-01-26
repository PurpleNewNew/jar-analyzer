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
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.server.handler.api.ApiBaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.JarFingerprintUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GetJarListHandler extends ApiBaseHandler implements HttpHandler {
    private static final int DEFAULT_LIMIT = 1000;
    private static final int MAX_LIMIT = 5000;

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = MainForm.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        List<JarEntity> jars = engine.getJarsMeta();
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
        List<Map<String, Object>> out = new ArrayList<>();
        int end = Math.min(jars.size(), offset + limit);
        for (int i = offset; i < end; i++) {
            JarEntity jar = jars.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("jar_id", jar.getJid());
            item.put("jar_name", jar.getJarName());
            item.put("jar_fingerprint", JarFingerprintUtil.sha256(jar.getJarAbsPath()));
            out.add(item);
        }
        return ok(out, pageMeta(offset, limit, out.size(), jars.size()));
    }
}
