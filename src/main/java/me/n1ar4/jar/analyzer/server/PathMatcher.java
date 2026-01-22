/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.server;

import com.alibaba.fastjson2.JSON;
import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.server.handler.*;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PathMatcher {
    private static final Logger logger = LogManager.getLogger();
    public static Map<String, HttpHandler> handlers = new HashMap<>();
    private final ServerConfig config;

    static {
        IndexHandler handler = new IndexHandler();
        handlers.put("/index", handler);
        handlers.put("/index.html", handler);
        handlers.put("/index.jsp", handler);
        handlers.put("/favicon.ico", new FaviconHandler());
        handlers.put("/static/boot.js", new JSHandler());
        handlers.put("/static/d3v6.js", new D3Handler());
        handlers.put("/static/boot.css", new CSSHandler());

        //
        // 以下 API 允许被 MCP 调用 对外
        //

        handlers.put("/api/get_jars_list", new GetJarListHandler());
        handlers.put("/api/get_jar_by_class", new GetJarByClassHandler());
        handlers.put("/api/get_abs_path", new GetAbsPathHandler());

        handlers.put("/api/get_callers", new GetCallersHandler());
        handlers.put("/api/get_callers_like", new GetCallersLikeHandler());
        handlers.put("/api/get_callee", new GetCalleeHandler());
        handlers.put("/api/get_call_edges", new GetCallEdgesHandler());
        handlers.put("/api/get_callers_by_sink", new GetCallersBySinkHandler());
        handlers.put("/api/get_callers_batch", new GetCallersBatchHandler());
        handlers.put("/api/get_callee_batch", new GetCalleeBatchHandler());

        handlers.put("/api/get_method", new GetMethodHandler());
        handlers.put("/api/get_method_like", new GetMethodLikeHandler());
        handlers.put("/api/get_method_batch", new GetMethodBatchHandler());
        handlers.put("/api/get_methods_by_anno", new GetMethodsByAnnoHandler());
        handlers.put("/api/get_methods_by_str", new GetMethodsByStrHandler());
        handlers.put("/api/get_methods_by_str_batch", new GetMethodsByStrBatchHandler());
        handlers.put("/api/get_methods_by_class", new GetMethodsByClassHandler());
        handlers.put("/api/get_resources", new GetResourcesHandler());
        handlers.put("/api/get_resource", new GetResourceHandler());
        handlers.put("/api/search_resources", new SearchResourcesHandler());
        handlers.put("/api/get_vul_rules", new GetVulRulesHandler());
        handlers.put("/api/vul_search", new VulSearchHandler());
        handlers.put("/api/get_sinks", new GetSinksHandler());
        handlers.put("/api/get_sink_candidates", new GetSinkCandidatesHandler());
        handlers.put("/api/dfs", new DfsHandler());
        handlers.put("/api/dfs/jobs", new DfsJobHandler());
        handlers.put("/api/dfs/jobs/*", new DfsJobHandler());
        handlers.put("/api/taint", new TaintJobHandler());
        handlers.put("/api/taint/jobs", new TaintJobHandler());
        handlers.put("/api/taint/jobs/*", new TaintJobHandler());
        handlers.put("/api/sca", new ScaHandler());
        handlers.put("/api/leak", new LeakHandler());
        handlers.put("/api/gadget", new GadgetHandler());

        handlers.put("/api/get_impls", new GetImplsHandler());
        handlers.put("/api/get_super_impls", new GetSuperImplsHandler());

        handlers.put("/api/get_all_spring_controllers", new GetAllSpringControllersHandler());
        handlers.put("/api/get_all_spring_interceptors", new GetAllSpringInterceptorsHandler());
        handlers.put("/api/get_spring_mappings", new GetSpringMappingsHandler());
        handlers.put("/api/get_spring_mappings_all", new GetSpringMappingsAllHandler());

        handlers.put("/api/get_all_servlets", new GetAllServletsHandler());

        handlers.put("/api/get_all_listeners", new GetAllListenersHandler());

        handlers.put("/api/get_all_filters", new GetAllFiltersHandler());

        handlers.put("/api/get_class_by_class", new GetClassByClassHandler());

        handlers.put("/api/fernflower_code", new GetCodeFernflowerHandler());   
        handlers.put("/api/cfr_code", new GetCodeCFRHandler());
        handlers.put("/api/get_code_batch", new GetCodeBatchHandler());
    }

    public PathMatcher(ServerConfig config) {
        this.config = config;
    }

    private NanoHTTPD.Response buildTokenError(String uri) {
        if (uri != null && uri.startsWith("/api/")) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ok", false);
            result.put("code", "unauthorized");
            result.put("message", "need token header");
            result.put("status", NanoHTTPD.Response.Status.UNAUTHORIZED.getRequestStatus());
            String json = JSON.toJSONString(result);
            NanoHTTPD.Response resp = NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.UNAUTHORIZED,
                    "application/json",
                    json);
            addCorsHeaders(resp, null);
            return resp;
        }
        return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.UNAUTHORIZED,
                "text/html",
                "<h1>JAR ANALYZER SERVER</h1>" +
                        "<h2>NEED TOKEN HEADER</h2>");
    }

    public NanoHTTPD.Response handleReq(NanoHTTPD.IHTTPSession session) {
        String uri = session.getUri();

        if (session.getMethod() == NanoHTTPD.Method.OPTIONS && uri != null && uri.startsWith("/api/")) {
            return buildPreflight(session);
        }

        if (config.isAuth()) {
            // 这个框架有 BUG 大小写问题
            String authA = session.getHeaders().get("Token");
            String authB = session.getHeaders().get("token");
            boolean access = false;
            if (authA != null && !authA.trim().isEmpty()) {
                if (authA.equals(config.getToken())) {
                    access = true;
                }
            }
            if (authB != null && !authB.trim().isEmpty()) {
                if (authB.equals(config.getToken())) {
                    access = true;
                }
            }
            if (!access) {
                return buildTokenError(uri);
            }
        }

        logger.debug("receive {} from {}", session.getRemoteIpAddress(), uri);

        for (Map.Entry<String, HttpHandler> entry : handlers.entrySet()) {
            if (uri.equals(entry.getKey())) {
                return entry.getValue().handle(session);
            }
        }
        if (uri.startsWith("/api/dfs/jobs/")) {
            HttpHandler handler = handlers.get("/api/dfs/jobs/*");
            if (handler != null) {
                return handler.handle(session);
            }
        }
        if (uri.startsWith("/api/taint/jobs/")) {
            HttpHandler handler = handlers.get("/api/taint/jobs/*");
            if (handler != null) {
                return handler.handle(session);
            }
        }
        IndexHandler handler = new IndexHandler();
        return handler.handle(session);
    }

    private NanoHTTPD.Response buildPreflight(NanoHTTPD.IHTTPSession session) {
        NanoHTTPD.Response resp = NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                "text/plain",
                "");
        addCorsHeaders(resp, session);
        return resp;
    }

    private void addCorsHeaders(NanoHTTPD.Response resp, NanoHTTPD.IHTTPSession session) {
        if (resp == null) {
            return;
        }
        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE");
        String reqHeaders = null;
        if (session != null && session.getHeaders() != null) {
            reqHeaders = session.getHeaders().get("access-control-request-headers");
        }
        if (reqHeaders != null && !reqHeaders.trim().isEmpty()) {
            resp.addHeader("Access-Control-Allow-Headers", reqHeaders);
        } else {
            resp.addHeader("Access-Control-Allow-Headers", "Token, Content-Type");
        }
        resp.addHeader("Access-Control-Max-Age", "600");
        resp.addHeader("Vary", "Origin");
    }
}
