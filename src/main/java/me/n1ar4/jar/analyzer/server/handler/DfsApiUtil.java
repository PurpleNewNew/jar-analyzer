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
import me.n1ar4.jar.analyzer.chains.ChainsBuilder;
import me.n1ar4.jar.analyzer.chains.SinkModel;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.graph.flow.FlowOptions;
import me.n1ar4.jar.analyzer.graph.flow.FlowStats;
import me.n1ar4.jar.analyzer.graph.flow.FlowTruncation;
import me.n1ar4.jar.analyzer.graph.flow.GraphFlowService;
import me.n1ar4.jar.analyzer.graph.flow.TraversalMode;
import me.n1ar4.jar.analyzer.graph.query.QueryErrorClassifier;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.utils.StringUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

class DfsApiUtil {
    private static final Logger logger = LogManager.getLogger();
    private static final int DEFAULT_DEPTH = 10;
    private static final GraphFlowService FLOW_SERVICE = new GraphFlowService();

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
        String sinkName;
        String sinkClass;
        String sinkMethod;
        String sinkDesc;
        Integer sinkJarId;
        String sourceClass;
        String sourceMethod;
        String sourceDesc;
        Integer sourceJarId;
        String minEdgeConfidence = "low";
        String traversalMode = TraversalMode.CALL_ONLY.displayName();
        String projectKey;
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

        // 解析模式：仅接受显式 mode
        String mode = getParam(session, "mode");
        if (!StringUtil.isNull(mode)) {
            String m = mode.trim().toLowerCase();
            if ("source".equals(m) || "fromsource".equals(m)) {
                req.fromSink = false;
            } else if ("sink".equals(m) || "fromsink".equals(m)) {
                req.fromSink = true;
            }
        }

        req.searchAllSources = getBool(session, "searchAllSources");

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
        if (timeoutMs != null && timeoutMs > 0) {
            req.timeoutMs = timeoutMs;
        }

        if (req.maxLimit != null && req.maxPaths != null) {
            req.maxLimit = Math.min(req.maxLimit, req.maxPaths);
        } else if (req.maxLimit == null && req.maxPaths != null) {
            req.maxLimit = req.maxPaths;
        }

        req.onlyFromWeb = getOptionalBool(session, "onlyFromWeb");

        req.blacklist = parseBlacklist(getParam(session, "blacklist"));
        req.sinkName = getParam(session, "sinkName");
        req.minEdgeConfidence = normalizeConfidence(getParam(session, "minEdgeConfidence"));
        req.traversalMode = TraversalMode.parse(getParam(session, "traversalMode")).displayName();

        req.sinkClass = normalizeClass(getParam(session, "sinkClass"));
        req.sinkMethod = getParam(session, "sinkMethod");
        req.sinkDesc = normalizeDesc(getParam(session, "sinkDesc"), false);
        req.sinkJarId = normalizeJarId(getInt(session, "sinkJarId"));
        SinkModel resolvedSink = applySinkNameFallback(req);

        req.sourceClass = normalizeClass(getParam(session, "sourceClass"));
        req.sourceMethod = getParam(session, "sourceMethod");
        req.sourceDesc = normalizeDesc(getParam(session, "sourceDesc"), false);
        req.sourceJarId = normalizeJarId(getInt(session, "sourceJarId"));
        req.projectKey = getParam(session, "projectKey");

        // 参数校验：SINK 一定要有
        if (StringUtil.isNull(req.sinkClass) || StringUtil.isNull(req.sinkMethod) || StringUtil.isNull(req.sinkDesc)) {
            if (!StringUtil.isNull(req.sinkName) && resolvedSink == null) {
                return new ParseResult(null, buildError(
                        NanoHTTPD.Response.Status.BAD_REQUEST,
                        "invalid_sink_name",
                        "unknown sinkName"));
            }
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

    static NanoHTTPD.Response preflight(DfsRequest req) {
        if (req == null) {
            return buildError(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
                    "invalid_request",
                    "dfs request is null");
        }
        String projectKey = req.projectKey;
        try {
            ActiveProjectContext.withProject(projectKey, () -> {
                new GraphStore().loadSnapshot(projectKey);
                return null;
            });
            return null;
        } catch (IllegalStateException ex) {
            String message = safe(ex == null ? null : ex.getMessage());
            if (QueryErrorClassifier.isProjectBuilding(message)) {
                return buildProjectBuilding();
            }
            if (QueryErrorClassifier.isProjectNotReady(message)) {
                return buildProjectNotReady();
            }
            if (message.isBlank()) {
                message = "graph snapshot not ready";
            }
            return buildError(NanoHTTPD.Response.Status.SERVICE_UNAVAILABLE, "graph_not_ready", message);
        } catch (Exception ex) {
            String message = safe(ex == null ? null : ex.getMessage());
            if (QueryErrorClassifier.isProjectBuilding(message)) {
                return buildProjectBuilding();
            }
            return buildProjectNotReady();
        }
    }

    static FlowOptions buildOptions(DfsRequest req) {
        return FlowOptions.builder()
                .fromSink(req != null && req.fromSink)
                .searchAllSources(req != null && req.searchAllSources)
                .depth(req == null ? DEFAULT_DEPTH : req.depth)
                .maxLimit(req == null ? null : req.maxLimit)
                .maxPaths(req == null ? null : req.maxPaths)
                .maxNodes(req == null ? null : req.maxNodes)
                .maxEdges(req == null ? null : req.maxEdges)
                .timeoutMs(req == null ? null : req.timeoutMs)
                .onlyFromWeb(req != null && Boolean.TRUE.equals(req.onlyFromWeb))
                .traversalMode(req == null ? TraversalMode.CALL_ONLY : TraversalMode.parse(req.traversalMode))
                .blacklist(req == null ? Set.of() : req.blacklist)
                .minEdgeConfidence(req == null ? "low" : req.minEdgeConfidence)
                .sink(req == null ? "" : req.sinkClass,
                        req == null ? "" : req.sinkMethod,
                        req == null ? "" : req.sinkDesc)
                .sinkJarId(req == null ? null : req.sinkJarId)
                .source(req == null ? "" : req.sourceClass,
                        req == null ? "" : req.sourceMethod,
                        req == null ? "" : req.sourceDesc)
                .sourceJarId(req == null ? null : req.sourceJarId)
                .build();
    }

    static GraphFlowService.DfsOutcome run(DfsRequest req, AtomicBoolean cancelFlag) {
        String projectKey = req == null ? "" : req.projectKey;
        GraphFlowService.DfsOutcome out = ActiveProjectContext.withProject(
                projectKey,
                () -> FLOW_SERVICE.runDfs(buildOptions(req), cancelFlag)
        );
        List<DFSResult> patched = buildTruncatedMeta(req, out.stats(), out.results());
        if (patched == null) {
            return out;
        }
        return new GraphFlowService.DfsOutcome(patched, out.stats());
    }

    static List<DFSResult> buildTruncatedMeta(DfsRequest req, FlowStats stats, List<DFSResult> results) {
        FlowTruncation truncation = stats == null ? FlowTruncation.none() : stats.getTruncation();
        if ((results == null || results.isEmpty()) && truncation.truncated()) {
            DFSResult meta = new DFSResult();
            meta.setMethodList(new ArrayList<>());
            meta.setEdges(new ArrayList<>());
            meta.setDepth(0);
            boolean fromSink = req == null || req.fromSink;
            boolean allSources = req != null && req.searchAllSources;
            if (fromSink) {
                meta.setMode(allSources ? DFSResult.FROM_SOURCE_TO_ALL : DFSResult.FROM_SINK_TO_SOURCE);
            } else {
                meta.setMode(DFSResult.FROM_SOURCE_TO_SINK);
            }
            meta.setTruncated(true);
            meta.setTruncateReason(truncation.reason());
            meta.setRecommend(truncation.recommend());
            meta.setNodeCount(stats == null ? 0 : stats.getNodeCount());
            meta.setEdgeCount(stats == null ? 0 : stats.getEdgeCount());
            meta.setPathCount(0);
            meta.setElapsedMs(stats == null ? 0L : stats.getElapsedMs());
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

    private static Integer normalizeJarId(Integer jarId) {
        return jarId == null || jarId <= 0 ? null : jarId;
    }

    private static SinkModel applySinkNameFallback(DfsRequest req) {
        if (req == null || StringUtil.isNull(req.sinkName)) {
            return null;
        }
        SinkModel sink = resolveSinkByName(req.sinkName);
        if (sink == null) {
            return null;
        }
        if (StringUtil.isNull(req.sinkClass)) {
            req.sinkClass = normalizeClass(sink.getClassName());
        }
        if (StringUtil.isNull(req.sinkMethod)) {
            req.sinkMethod = safe(sink.getMethodName());
        }
        if (StringUtil.isNull(req.sinkDesc)) {
            req.sinkDesc = normalizeDesc(sink.getMethodDesc(), true);
        }
        return sink;
    }

    private static SinkModel resolveSinkByName(String sinkName) {
        if (StringUtil.isNull(sinkName)) {
            return null;
        }
        String[] parts = sinkName.split("[,;\\r\\n]+");
        for (String part : parts) {
            String key = safe(part);
            if (key.isBlank()) {
                continue;
            }
            SinkModel sink = ChainsBuilder.getSinkByName(key);
            if (sink != null) {
                return sink;
            }
        }
        return null;
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

    private static String normalizeConfidence(String confidence) {
        String value = safe(confidence).toLowerCase();
        if ("high".equals(value) || "medium".equals(value) || "low".equals(value)) {
            return value;
        }
        return "low";
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

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static NanoHTTPD.Response buildProjectNotReady() {
        return buildError(
                NanoHTTPD.Response.Status.SERVICE_UNAVAILABLE,
                QueryErrorClassifier.PROJECT_MODEL_MISSING_REBUILD,
                QueryErrorClassifier.publicMessage(
                        QueryErrorClassifier.PROJECT_MODEL_MISSING_REBUILD,
                        QueryErrorClassifier.PROJECT_MODEL_MISSING_REBUILD));
    }

    private static NanoHTTPD.Response buildProjectBuilding() {
        return buildError(
                NanoHTTPD.Response.Status.SERVICE_UNAVAILABLE,
                QueryErrorClassifier.PROJECT_BUILD_IN_PROGRESS,
                QueryErrorClassifier.publicMessage(
                        QueryErrorClassifier.PROJECT_BUILD_IN_PROGRESS,
                        QueryErrorClassifier.PROJECT_BUILD_IN_PROGRESS));
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
