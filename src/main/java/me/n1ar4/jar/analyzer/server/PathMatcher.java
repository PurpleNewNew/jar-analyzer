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
import me.n1ar4.jar.analyzer.server.handler.api.*;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public class PathMatcher {
    private static final Logger logger = LogManager.getLogger();
    private static final Map<String, HttpHandler> HANDLERS = new LinkedHashMap<>();
    private final ServerConfig config;

    static {
        IndexHandler handler = new IndexHandler();
        HANDLERS.put("/index", handler);
        HANDLERS.put("/index.html", handler);
        HANDLERS.put("/index.jsp", handler);
        HANDLERS.put("/favicon.ico", new FaviconHandler());
        HANDLERS.put("/static/boot.js", new JSHandler());
        HANDLERS.put("/static/d3v6.js", new D3Handler());
        HANDLERS.put("/static/boot.css", new CSSHandler());

        //
        // 以下 API 允许被 MCP 调用 对外
        //

        HANDLERS.put("/api/meta/jars", new GetJarListHandler());
        HANDLERS.put("/api/meta/jars/resolve", new GetJarByClassHandler());
        HANDLERS.put("/api/class/info", new GetClassByClassHandler());

        HANDLERS.put("/api/entrypoints", new EntryPointsHandler());
        HANDLERS.put("/api/entrypoints/mappings", new SpringMappingsHandler());

        HANDLERS.put("/api/methods/search", new MethodsSearchHandler());
        HANDLERS.put("/api/methods/impls", new MethodsImplsHandler());

        HANDLERS.put("/api/callgraph/edges", new GetCallEdgesHandler());
        HANDLERS.put("/api/callgraph/by-sink", new GetCallersBySinkHandler());

        HANDLERS.put("/api/code", new CodeHandler());

        HANDLERS.put("/api/resources/list", new GetResourcesHandler());
        HANDLERS.put("/api/resources/get", new GetResourceHandler());
        HANDLERS.put("/api/resources/search", new SearchResourcesHandler());

        HANDLERS.put("/api/semantic/hints", new GetSemanticHintsHandler());
        HANDLERS.put("/api/config/usage", new GetConfigUsageHandler());

        HANDLERS.put("/api/security/vul-rules", new GetVulRulesHandler());
        HANDLERS.put("/api/security/vul-search", new VulSearchHandler());
        HANDLERS.put("/api/security/sca", new ScaHandler());
        HANDLERS.put("/api/security/leak", new LeakHandler());
        HANDLERS.put("/api/security/gadget", new GadgetHandler());

        HANDLERS.put("/api/flow/dfs", new DfsHandler());
        HANDLERS.put("/api/flow/dfs/jobs", new DfsJobHandler());
        HANDLERS.put("/api/flow/dfs/jobs/*", new DfsJobHandler());
        HANDLERS.put("/api/flow/taint", new TaintJobHandler());
        HANDLERS.put("/api/flow/taint/jobs", new TaintJobHandler());
        HANDLERS.put("/api/flow/taint/jobs/*", new TaintJobHandler());

        HANDLERS.put("/api/query/sql", new QuerySqlHandler());
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

        HttpHandler exact = HANDLERS.get(uri);
        if (exact != null) {
            return exact.handle(session);
        }
        if (uri.startsWith("/api/flow/dfs/jobs/")) {
            HttpHandler handler = HANDLERS.get("/api/flow/dfs/jobs/*");
            if (handler != null) {
                return handler.handle(session);
            }
        }
        if (uri.startsWith("/api/flow/taint/jobs/")) {
            HttpHandler handler = HANDLERS.get("/api/flow/taint/jobs/*");
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
