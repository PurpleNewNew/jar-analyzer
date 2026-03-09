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
import me.n1ar4.jar.analyzer.rules.RuleValidationViews;
import me.n1ar4.jar.analyzer.server.handler.api.ApiBaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RuleValidationHandler extends ApiBaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        if (session != null && session.getMethod() != NanoHTTPD.Method.GET) {
            return buildError(
                    NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED,
                    "method_not_allowed",
                    "GET required");
        }
        Map<String, Object> out = new LinkedHashMap<>(RuleValidationViews.combinedValidationMap());
        String scope = session == null ? "" : getParam(session, "scope");
        if (!RuleValidationViews.isSupportedScope(scope)) {
            return buildError(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
                    "rule_validation_scope_invalid",
                    "scope must be one of all|model|source|sink"
            );
        }
        out.put("issues", RuleValidationViews.issueMaps(scope));
        return ok(out);
    }
}
