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
import me.n1ar4.jar.analyzer.sca.ScaScanService;
import me.n1ar4.jar.analyzer.sca.dto.SCAApiResult;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class ScaApiUtil {
    private static final ScaScanService SCA_SCAN_SERVICE = new ScaScanService();

    static class ParseResult {
        private final ScaScanService.Request request;
        private final NanoHTTPD.Response error;

        ParseResult(ScaScanService.Request request, NanoHTTPD.Response error) {
            this.request = request;
            this.error = error;
        }

        ScaScanService.Request getRequest() {
            return request;
        }

        NanoHTTPD.Response getError() {
            return error;
        }
    }

    static ParseResult parse(NanoHTTPD.IHTTPSession session, CoreEngine engine) {
        EnumSet<ScaScanService.RuleKind> enabled = ScaScanService.enabledRules(
                getBool(session, "log4j", true),
                getBool(session, "fastjson", true),
                getBool(session, "shiro", true)
        );
        if (enabled.isEmpty()) {
            return new ParseResult(null, buildError(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
                    "no_rule_enabled",
                    "no rule enabled"));
        }

        List<String> inputPaths = new ArrayList<>();
        inputPaths.addAll(getParams(session, "path"));
        inputPaths.addAll(splitParam(getParam(session, "paths")));

        List<String> jarList = new ArrayList<>();
        if (inputPaths.isEmpty()) {
            if (engine != null) {
                jarList.addAll(engine.getJarsPath());
            }
        } else {
            for (String p : inputPaths) {
                if (StringUtil.isNull(p)) {
                    continue;
                }
                jarList.addAll(ScaScanService.resolveJarList(p.trim()));
            }
        }

        if (jarList.isEmpty()) {
            return new ParseResult(null, buildError(
                    NanoHTTPD.Response.Status.NOT_FOUND,
                    "no_jar_found",
                    "no jar found"));
        }

        return new ParseResult(new ScaScanService.Request(jarList, enabled), null);
    }

    static List<SCAApiResult> scan(ScaScanService.Request req) {
        if (req == null) {
            return Collections.emptyList();
        }
        return SCA_SCAN_SERVICE.scan(req);
    }

    private static String getParam(NanoHTTPD.IHTTPSession session, String key) {
        List<String> data = session.getParameters().get(key);
        if (data == null || data.isEmpty()) {
            return "";
        }
        return data.get(0);
    }

    private static List<String> getParams(NanoHTTPD.IHTTPSession session, String key) {
        List<String> data = session.getParameters().get(key);
        if (data == null || data.isEmpty()) {
            return Collections.emptyList();
        }
        return data;
    }

    private static List<String> splitParam(String value) {
        if (StringUtil.isNull(value)) {
            return Collections.emptyList();
        }
        String[] parts = value.split("[,;\\r\\n]+");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (StringUtil.isNull(part)) {
                continue;
            }
            result.add(part.trim());
        }
        return result;
    }

    private static boolean getBool(NanoHTTPD.IHTTPSession session, String key, boolean def) {
        String value = getParam(session, key);
        if (StringUtil.isNull(value)) {
            return def;
        }
        String v = value.trim().toLowerCase();
        return "1".equals(v) || "true".equals(v) || "yes".equals(v) || "on".equals(v);
    }

    private static NanoHTTPD.Response buildError(NanoHTTPD.Response.Status status, String code, String msg) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", false);
        result.put("code", code);
        result.put("message", msg);
        result.put("status", status.getRequestStatus());
        String json = JSON.toJSONString(result);
        return NanoHTTPD.newFixedLengthResponse(
                status,
                "application/json",
                json);
    }
}
