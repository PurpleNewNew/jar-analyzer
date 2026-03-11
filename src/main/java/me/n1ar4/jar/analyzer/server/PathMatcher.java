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
import me.n1ar4.jar.analyzer.graph.query.QueryErrorClassifier;
import me.n1ar4.jar.analyzer.server.handler.*;
import me.n1ar4.jar.analyzer.server.handler.api.*;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PathMatcher {
    private static final Logger logger = LogManager.getLogger();
    private static final HttpHandler INDEX_HANDLER = new IndexHandler();
    public static Map<String, HttpHandler> handlers = new HashMap<>();
    private final ServerConfig config;

    static {
        handlers.put("/index", INDEX_HANDLER);
        handlers.put("/index.html", INDEX_HANDLER);
        handlers.put("/index.jsp", INDEX_HANDLER);
        handlers.put("/favicon.ico", new FaviconHandler());
        handlers.put("/static/boot.js", new JSHandler());
        handlers.put("/static/d3v6.js", new D3Handler());
        handlers.put("/static/boot.css", new CSSHandler());

        //
        // 以下 API 允许被 MCP 调用 对外
        //

        handlers.put("/api/meta/jars", new GetJarListHandler());
        handlers.put("/api/meta/jars/resolve", new GetJarByClassHandler());
        handlers.put("/api/class/info", new GetClassByClassHandler());

        handlers.put("/api/entrypoints", new EntryPointsHandler());
        handlers.put("/api/entrypoints/mappings", new SpringMappingsHandler());

        handlers.put("/api/methods/search", new MethodsSearchHandler());
        handlers.put("/api/methods/impls", new MethodsImplsHandler());

        handlers.put("/api/callgraph/edges", new GetCallEdgesHandler());
        handlers.put("/api/callgraph/by-sink", new GetCallersBySinkHandler());

        handlers.put("/api/code", new CodeHandler());

        handlers.put("/api/resources/list", new GetResourcesHandler());
        handlers.put("/api/resources/get", new GetResourceHandler());
        handlers.put("/api/resources/search", new SearchResourcesHandler());

        handlers.put("/api/semantic/hints", new GetSemanticHintsHandler());
        handlers.put("/api/config/usage", new GetConfigUsageHandler());

        handlers.put("/api/security/sink-rules", new GetSinkRulesHandler());
        handlers.put("/api/security/rule-validation", new RuleValidationHandler());
        handlers.put("/api/security/sink-search", new SinkSearchHandler());
        handlers.put("/api/security/sca", new ScaHandler());
        handlers.put("/api/security/leak", new LeakHandler());
        handlers.put("/api/security/gadget", new GadgetHandler());

        handlers.put("/api/flow/dfs", new DfsHandler());
        handlers.put("/api/flow/dfs/jobs/*", new DfsJobHandler());
        handlers.put("/api/flow/taint", new TaintJobHandler());
        handlers.put("/api/flow/taint/jobs", new TaintJobHandler());
        handlers.put("/api/flow/taint/jobs/*", new TaintJobHandler());

        handlers.put("/api/query/cypher", new QueryCypherHandler());
        handlers.put("/api/query/cypher/capabilities", new QueryCypherCapabilitiesHandler());
        handlers.put("/api/projects", new ProjectsHandler());
        handlers.put("/api/projects/active", new ProjectsHandler());
        handlers.put("/api/projects/register", new ProjectsHandler());
        handlers.put("/api/projects/switch", new ProjectsHandler());
        handlers.put("/api/projects/*", new ProjectsHandler());
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

    private NanoHTTPD.Response buildApiNotFound(String uri, NanoHTTPD.IHTTPSession session) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", false);
        result.put("code", "not_found");
        result.put("message", "no handler for path: " + (uri == null ? "" : uri));
        result.put("status", NanoHTTPD.Response.Status.NOT_FOUND.getRequestStatus());
        String json = JSON.toJSONString(result);
        NanoHTTPD.Response resp = NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.NOT_FOUND,
                "application/json",
                json);
        addCorsHeaders(resp, session);
        return resp;
    }

    public NanoHTTPD.Response handleReq(NanoHTTPD.IHTTPSession session) {
        String uri = session.getUri();
        if (uri == null) {
            uri = "";
        }

        if (session.getMethod() == NanoHTTPD.Method.OPTIONS && uri.startsWith("/api/")) {
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
                return invokeHandler(uri, entry.getValue(), session);
            }
        }
        if (uri.startsWith("/api/flow/dfs/jobs/")) {
            HttpHandler handler = handlers.get("/api/flow/dfs/jobs/*");
            if (handler != null) {
                return invokeHandler(uri, handler, session);
            }
        }
        if (uri.startsWith("/api/flow/taint/jobs/")) {
            HttpHandler handler = handlers.get("/api/flow/taint/jobs/*");
            if (handler != null) {
                return invokeHandler(uri, handler, session);
            }
        }
        if (uri.startsWith("/api/projects/")) {
            HttpHandler handler = handlers.get("/api/projects/*");
            if (handler != null) {
                return invokeHandler(uri, handler, session);
            }
        }
        if (uri.startsWith("/api/")) {
            return buildApiNotFound(uri, session);
        }
        return invokeHandler(uri, INDEX_HANDLER, session);
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

    private NanoHTTPD.Response invokeHandler(String uri,
                                             HttpHandler handler,
                                             NanoHTTPD.IHTTPSession session) {
        try {
            return handler.handle(session);
        } catch (IllegalArgumentException ex) {
            logger.warn("request bad input: path={} err={}", uri, ex.toString());
            return uri != null && uri.startsWith("/api/")
                    ? buildApiError(session, NanoHTTPD.Response.Status.BAD_REQUEST,
                    QueryErrorClassifier.codeOf(ex.getMessage()),
                    QueryErrorClassifier.publicMessage(ex.getMessage(), "bad request"))
                    : NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "text/plain", "Bad Request");
        } catch (IllegalStateException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage().trim();
            logger.warn("request state error: path={} err={}", uri, ex.toString());
            if (uri != null && uri.startsWith("/api/")) {
                if ("job_queue_full".equals(message)) {
                    return buildApiError(session,
                            NanoHTTPD.Response.Status.SERVICE_UNAVAILABLE,
                            "job_queue_full",
                            "job queue is full, retry later");
                }
                String code = QueryErrorClassifier.codeOf(message);
                if (!QueryErrorClassifier.isProjectBuilding(message)
                        && !QueryErrorClassifier.isProjectNotReady(message)
                        && QueryErrorClassifier.CYPHER_QUERY_INVALID.equals(code)) {
                    code = "internal_error";
                }
                NanoHTTPD.Response.Status status = QueryErrorClassifier.isProjectBuilding(message)
                        || QueryErrorClassifier.isProjectNotReady(message)
                        ? NanoHTTPD.Response.Status.SERVICE_UNAVAILABLE
                        : NanoHTTPD.Response.Status.INTERNAL_ERROR;
                String publicMessage = QueryErrorClassifier.publicMessage(message, "internal error");
                return buildApiError(session, status, code, publicMessage);
            }
            return NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.INTERNAL_ERROR,
                    "text/plain",
                    "Internal Server Error");
        } catch (Exception ex) {
            logger.error("request handler failed: path={} err={}", uri, ex.toString(), ex);
            if (uri != null && uri.startsWith("/api/")) {
                return buildApiError(session,
                        NanoHTTPD.Response.Status.INTERNAL_ERROR,
                        "internal_error",
                        ex.getMessage() == null || ex.getMessage().isBlank()
                                ? "internal error"
                                : ex.getMessage());
            }
            return NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.INTERNAL_ERROR,
                    "text/plain",
                    "Internal Server Error");
        }
    }

    private NanoHTTPD.Response buildApiError(NanoHTTPD.IHTTPSession session,
                                             NanoHTTPD.Response.Status status,
                                             String code,
                                             String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", false);
        result.put("code", code == null || code.isBlank() ? "internal_error" : code);
        result.put("message", message == null ? "" : message);
        result.put("status", status.getRequestStatus());
        String json = JSON.toJSONString(result);
        NanoHTTPD.Response resp = NanoHTTPD.newFixedLengthResponse(status, "application/json", json);
        addCorsHeaders(resp, session);
        return resp;
    }
}
