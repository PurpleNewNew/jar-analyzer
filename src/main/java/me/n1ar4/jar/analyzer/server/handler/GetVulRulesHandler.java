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
import me.n1ar4.jar.analyzer.gui.vul.Rule;
import me.n1ar4.jar.analyzer.server.handler.base.BaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;

import java.util.HashMap;
import java.util.Map;

public class GetVulRulesHandler extends BaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        VulRuleLoader.Result res = VulRuleLoader.load();
        Rule rule = res.getRule();
        if (rule == null) {
            return NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.INTERNAL_ERROR,
                    "text/html",
                    "<h1>JAR ANALYZER SERVER</h1>" +
                            "<h2>VULNERABILITY RULE NOT FOUND</h2>");
        }
        Map<String, Object> out = new HashMap<>();
        out.put("name", rule.getName());
        out.put("source", res.getSource());
        out.put("vulnerabilities", rule.getVulnerabilities());
        out.put("count", rule.getVulnerabilities() == null ? 0 : rule.getVulnerabilities().size());
        String json = JSON.toJSONString(out);
        return buildJSON(json);
    }
}
