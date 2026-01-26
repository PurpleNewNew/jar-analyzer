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
import me.n1ar4.jar.analyzer.gui.vul.Rule;
import me.n1ar4.jar.analyzer.server.handler.api.ApiBaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;

import java.util.HashMap;
import java.util.Map;

public class GetVulRulesHandler extends ApiBaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        VulRuleLoader.Result res = VulRuleLoader.load();
        Rule rule = res.getRule();
        if (rule == null) {
            return buildError(
                    NanoHTTPD.Response.Status.INTERNAL_ERROR,
                    "vul_rule_not_found",
                    "vulnerability rule not found");
        }
        Map<String, Object> out = new HashMap<>();
        out.put("name", rule.getName());
        out.put("source", res.getSource());
        out.put("levels", rule.getLevels());
        int count = 0;
        if (rule.getLevels() != null) {
            for (Map<String, ?> byType : rule.getLevels().values()) {
                if (byType != null) {
                    count += byType.size();
                }
            }
        }
        out.put("count", count);
        return ok(out);
    }
}
