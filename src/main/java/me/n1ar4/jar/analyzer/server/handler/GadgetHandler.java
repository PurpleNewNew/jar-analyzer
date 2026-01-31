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
import me.n1ar4.jar.analyzer.gadget.GadgetAnalyzer;
import me.n1ar4.jar.analyzer.gadget.GadgetInfo;
import me.n1ar4.jar.analyzer.gadget.GadgetRule;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.server.handler.api.ApiBaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.EnumSet;
import java.util.List;

public class GadgetHandler extends ApiBaseHandler implements HttpHandler {
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
        EnumSet<GadgetAnalyzer.GadgetType> types =
                EnumSet.noneOf(GadgetAnalyzer.GadgetType.class);
        if (enableNative) {
            types.add(GadgetAnalyzer.GadgetType.NATIVE);
        }
        if (enableHessian) {
            types.add(GadgetAnalyzer.GadgetType.HESSIAN);
        }
        if (enableFastjson) {
            types.add(GadgetAnalyzer.GadgetType.FASTJSON);
        }
        if (enableJdbc) {
            types.add(GadgetAnalyzer.GadgetType.JDBC);
        }
        GadgetAnalyzer analyzer = new GadgetAnalyzer(dir, types);
        List<GadgetInfo> results = analyzer.process();
        if (results == null) {
            results = java.util.Collections.emptyList();
        }
        return ok(results, pageMeta(0, 0, results.size(), null));
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
