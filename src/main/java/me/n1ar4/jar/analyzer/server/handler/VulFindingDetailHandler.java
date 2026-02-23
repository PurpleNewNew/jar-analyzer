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
import me.n1ar4.jar.analyzer.entity.FindingDetail;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.server.handler.api.ApiBaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

public class VulFindingDetailHandler extends ApiBaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = MainForm.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        String findingId = getParam(session, "findingId");
        if (StringUtil.isNull(findingId)) {
            findingId = getParam(session, "id");
        }
        if (StringUtil.isNull(findingId)) {
            return needParam("findingId");
        }
        FindingDetail detail = FindingEngineV2.getDetail(findingId);
        if (detail == null) {
            return buildError(
                    NanoHTTPD.Response.Status.NOT_FOUND,
                    "finding_not_found",
                    "finding not found");
        }
        return ok(detail);
    }
}
