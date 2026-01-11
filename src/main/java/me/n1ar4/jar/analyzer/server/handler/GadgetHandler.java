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
import me.n1ar4.jar.analyzer.gadget.GadgetAnalyzer;
import me.n1ar4.jar.analyzer.gadget.GadgetInfo;
import me.n1ar4.jar.analyzer.gadget.GadgetRule;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.server.handler.base.BaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.List;

public class GadgetHandler extends BaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = MainForm.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        String dir = getParam(session, "dir");
        if (StringUtil.isNull(dir)) {
            dir = getParam(session, "path");
        }
        if (StringUtil.isNull(dir)) {
            return needParam("dir");
        }
        boolean enableNative = getBool(session, "native", true);
        boolean enableHessian = getBool(session, "hessian", true);
        boolean enableFastjson = getBool(session, "fastjson", true);
        boolean enableJdbc = getBool(session, "jdbc", true);
        if (!enableNative && !enableHessian && !enableFastjson && !enableJdbc) {
            return buildError(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
                    "no_rule_enabled",
                    "no rule enabled");
        }
        if (GadgetRule.rules.isEmpty()) {
            GadgetRule.build();
        }
        GadgetAnalyzer analyzer = new GadgetAnalyzer(dir, enableNative, enableHessian, enableFastjson, enableJdbc);
        List<GadgetInfo> results = analyzer.process();
        if (results == null) {
            results = java.util.Collections.emptyList();
        }
        String json = JSON.toJSONString(results);
        return buildJSON(json);
    }

    private String getParam(NanoHTTPD.IHTTPSession session, String key) {
        List<String> data = session.getParameters().get(key);
        if (data == null || data.isEmpty()) {
            return "";
        }
        return data.get(0);
    }

    private boolean getBool(NanoHTTPD.IHTTPSession session, String key, boolean def) {
        String value = getParam(session, key);
        if (StringUtil.isNull(value)) {
            return def;
        }
        String v = value.trim().toLowerCase();
        return "1".equals(v) || "true".equals(v) || "yes".equals(v) || "on".equals(v);
    }

}
