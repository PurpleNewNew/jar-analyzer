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
import me.n1ar4.jar.analyzer.dfs.DFSEngine;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.dfs.DfsOutputs;
import me.n1ar4.jar.analyzer.utils.StringUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class DfsApiUtil {
    private static final Logger logger = LogManager.getLogger();
    private static final int DEFAULT_DEPTH = 10;

    static class DfsRequest {
        boolean fromSink = true;
        boolean searchAllSources = false;
        int depth = DEFAULT_DEPTH;
        Integer maxLimit = null;
        Integer maxPaths = null;
        Integer maxNodes = null;
        Integer maxEdges = null;
        Integer timeoutMs = null;
        Boolean onlyFromWeb = null;
        Set<String> blacklist = new HashSet<>();
        String sinkClass;
        String sinkMethod;
        String sinkDesc;
        String sourceClass;
        String sourceMethod;
        String sourceDesc;
    }

    static class ParseResult {
        private final DfsRequest request;
        private final NanoHTTPD.Response error;

        ParseResult(DfsRequest request, NanoHTTPD.Response error) {
            this.request = request;
            this.error = error;
        }

        DfsRequest getRequest() {
            return request;
        }

        NanoHTTPD.Response getError() {
            return error;
        }
    }

    static ParseResult parse(NanoHTTPD.IHTTPSession session) {
        DfsRequest req = new DfsRequest();

        // 解析模式：优先使用 mode/fromSink/fromSource
        String mode = getParam(session, "mode");
        if (!StringUtil.isNull(mode)) {
            String m = mode.trim().toLowerCase();
            if ("source".equals(m) || "fromsource".equals(m)) {
                req.fromSink = false;
            } else if ("sink".equals(m) || "fromsink".equals(m)) {
                req.fromSink = true;
            }
        }
        Boolean fromSink = getOptionalBool(session, "fromSink");
        if (fromSink != null) {
            req.fromSink = fromSink;
        }
        Boolean fromSource = getOptionalBool(session, "fromSource");
        if (fromSource != null) {
            req.fromSink = !fromSource;
        }

        // 仅在 SINK 模式下允许“查找所有 SOURCE”
        req.searchAllSources = getBool(session, "searchAllSources")
                || getBool(session, "searchNullSource")
                || getBool(session, "allSources");

        int depth = getInt(session, "depth", DEFAULT_DEPTH);
        if (depth > 0) {
            req.depth = depth;
        }
        Integer maxLimit = getInt(session, "maxLimit");
        if (maxLimit != null && maxLimit > 0) {
            req.maxLimit = maxLimit;
        }
        Integer maxPaths = getInt(session, "maxPaths");
        if (maxPaths != null && maxPaths > 0) {
            req.maxPaths = maxPaths;
        }
        Integer maxNodes = getInt(session, "maxNodes");
        if (maxNodes != null && maxNodes > 0) {
            req.maxNodes = maxNodes;
        }
        Integer maxEdges = getInt(session, "maxEdges");
        if (maxEdges != null && maxEdges > 0) {
            req.maxEdges = maxEdges;
        }
        Integer timeoutMs = getInt(session, "timeoutMs");
        if (timeoutMs == null || timeoutMs <= 0) {
            timeoutMs = getInt(session, "timeout");
        }
        if (timeoutMs != null && timeoutMs > 0) {
            req.timeoutMs = timeoutMs;
        }

        if (req.maxLimit != null && req.maxPaths != null) {
            req.maxLimit = Math.min(req.maxLimit, req.maxPaths);
        } else if (req.maxLimit == null && req.maxPaths != null) {
            req.maxLimit = req.maxPaths;
        }

        // API 可覆盖“只从 Web 入口找 SOURCE”的 GUI 选项
        req.onlyFromWeb = getOptionalBool(session, "onlyFromWeb");
        if (req.onlyFromWeb == null) {
            req.onlyFromWeb = getOptionalBool(session, "sourceOnlyWeb");
        }

        req.blacklist = parseBlacklist(getParam(session, "blacklist"));

        req.sinkClass = normalizeClass(getParam(session, "sinkClass"));
        req.sinkMethod = getParam(session, "sinkMethod");
        req.sinkDesc = normalizeDesc(getParam(session, "sinkDesc"), false);

        req.sourceClass = normalizeClass(getParam(session, "sourceClass"));
        req.sourceMethod = getParam(session, "sourceMethod");
        req.sourceDesc = normalizeDesc(getParam(session, "sourceDesc"), false);

        // 参数校验：SINK 一定要有
        if (StringUtil.isNull(req.sinkClass) || StringUtil.isNull(req.sinkMethod) || StringUtil.isNull(req.sinkDesc)) {
            return new ParseResult(null, buildNeedParam("sinkClass/sinkMethod/sinkDesc"));
        }

        if (req.fromSink) {
            // SINK 分析模式：searchAllSources=true 时允许 SOURCE 为空
            if (!req.searchAllSources) {
                if (StringUtil.isNull(req.sourceClass)
                        || StringUtil.isNull(req.sourceMethod)
                        || StringUtil.isNull(req.sourceDesc)) {
                    return new ParseResult(null,
                            buildNeedParam("sourceClass/sourceMethod/sourceDesc 或 searchAllSources=true"));
                }
            }
        } else {
            if (req.searchAllSources) {
                return new ParseResult(null, buildError(
                        NanoHTTPD.Response.Status.BAD_REQUEST,
                        "invalid_request",
                        "source mode does not support searchAllSources"));
            }
            if (StringUtil.isNull(req.sourceClass)
                    || StringUtil.isNull(req.sourceMethod)
                    || StringUtil.isNull(req.sourceDesc)) {
                return new ParseResult(null, buildNeedParam("sourceClass/sourceMethod/sourceDesc"));
            }
        }

        return new ParseResult(req, null);
    }

    static DFSEngine buildEngine(DfsRequest req) {
        DFSEngine dfsEngine = new DFSEngine(DfsOutputs.noop(), req.fromSink, req.searchAllSources, req.depth);
        if (req.maxLimit != null) {
            dfsEngine.setMaxLimit(req.maxLimit);
        }
        if (req.maxPaths != null) {
            dfsEngine.setMaxPaths(req.maxPaths);
        }
        if (req.maxNodes != null) {
            dfsEngine.setMaxNodes(req.maxNodes);
        }
        if (req.maxEdges != null) {
            dfsEngine.setMaxEdges(req.maxEdges);
        }
        if (req.timeoutMs != null) {
            dfsEngine.setTimeoutMs(req.timeoutMs);
        }
        if (req.blacklist != null && !req.blacklist.isEmpty()) {
            dfsEngine.setBlacklist(req.blacklist);
        }
        dfsEngine.setOnlyFromWeb(Boolean.TRUE.equals(req.onlyFromWeb));
        dfsEngine.setSink(req.sinkClass, req.sinkMethod, req.sinkDesc);
        dfsEngine.setSource(req.sourceClass, req.sourceMethod, req.sourceDesc);
        return dfsEngine;
    }

    static List<DFSResult> run(DfsRequest req) {
        DFSEngine dfsEngine = buildEngine(req);
        dfsEngine.doAnalyze();
        List<DFSResult> results = dfsEngine.getResults();
        List<DFSResult> patched = buildTruncatedMeta(req, dfsEngine, results);
        return patched == null ? results : patched;
    }

    static List<DFSResult> buildTruncatedMeta(DfsRequest req, DFSEngine dfsEngine, List<DFSResult> results) {
        if ((results == null || results.isEmpty()) && dfsEngine.isTruncated()) {
            DFSResult meta = new DFSResult();
            meta.setMethodList(new ArrayList<>());
            meta.setEdges(new ArrayList<>());
            meta.setDepth(0);
            if (req.fromSink) {
                meta.setMode(req.searchAllSources ? DFSResult.FROM_SOURCE_TO_ALL : DFSResult.FROM_SINK_TO_SOURCE);
            } else {
                meta.setMode(DFSResult.FROM_SOURCE_TO_SINK);
            }
            meta.setTruncated(true);
            meta.setTruncateReason(dfsEngine.getTruncateReason());
            meta.setRecommend(dfsEngine.getRecommendation());
            meta.setNodeCount(dfsEngine.getNodeCount());
            meta.setEdgeCount(dfsEngine.getEdgeCount());
            meta.setPathCount(0);
            meta.setElapsedMs(dfsEngine.getElapsedMs());
            List<DFSResult> out = new ArrayList<>();
            out.add(meta);
            return out;
        }
        return null;
    }

    private static String normalizeClass(String className) {
        if (StringUtil.isNull(className)) {
            return "";
        }
        return className.replace('.', '/');
    }

    private static String normalizeDesc(String desc, boolean allowEmpty) {
        if (desc == null) {
            return allowEmpty ? "*" : "";
        }
        String v = desc.trim();
        if (v.isEmpty()) {
            return allowEmpty ? "*" : "";
        }
        if ("null".equalsIgnoreCase(v)) {
            return "*";
        }
        return v;
    }

    private static String getParam(NanoHTTPD.IHTTPSession session, String key) {
        List<String> data = session.getParameters().get(key);
        if (data == null || data.isEmpty()) {
            return "";
        }
        if (data.size() == 1) {
            return data.get(0);
        }
        return String.join("\n", data);
    }

    private static Integer getInt(NanoHTTPD.IHTTPSession session, String key) {
        String value = getParam(session, key);
        if (StringUtil.isNull(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            logger.debug("invalid int param {}={}", key, value);
            return null;
        }
    }

    private static int getInt(NanoHTTPD.IHTTPSession session, String key, int def) {
        Integer value = getInt(session, key);
        return value == null ? def : value;
    }

    private static boolean getBool(NanoHTTPD.IHTTPSession session, String key) {
        String value = getParam(session, key);
        if (StringUtil.isNull(value)) {
            return false;
        }
        String v = value.trim().toLowerCase();
        return "1".equals(v) || "true".equals(v) || "yes".equals(v) || "on".equals(v);
    }

    private static Boolean getOptionalBool(NanoHTTPD.IHTTPSession session, String key) {
        String value = getParam(session, key);
        if (StringUtil.isNull(value)) {
            return null;
        }
        String v = value.trim().toLowerCase();
        return "1".equals(v) || "true".equals(v) || "yes".equals(v) || "on".equals(v);
    }

    private static Set<String> parseBlacklist(String value) {
        Set<String> result = new HashSet<>();
        if (StringUtil.isNull(value)) {
            return result;
        }
        String[] parts = value.split("[,;\\r\\n]+");
        for (String part : parts) {
            if (StringUtil.isNull(part)) {
                continue;
            }
            result.add(part.trim());
        }
        return result;
    }

    private static NanoHTTPD.Response buildNeedParam(String s) {
        return buildError(
                NanoHTTPD.Response.Status.BAD_REQUEST,
                "missing_param",
                "need param: " + s);
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
