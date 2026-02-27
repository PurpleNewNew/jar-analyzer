/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server.handler.api;

import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.graph.query.QueryServices;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;

import java.util.Map;

public final class QueryCypherExplainHandler extends ApiBaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        if (session.getMethod() != NanoHTTPD.Method.POST) {
            return buildError(
                    NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED,
                    "method_not_allowed",
                    "POST required");
        }
        try {
            QueryApiUtil.RequestPayload payload = QueryApiUtil.parseRequest(session);
            if (payload.query().isBlank()) {
                return needParam("query");
            }
            Map<String, Object> explain = QueryServices.cypher().explain(payload.query(), payload.projectKey());
            return ok(explain);
        } catch (IllegalStateException ex) {
            String message = safe(ex == null ? null : ex.getMessage());
            if (isProjectNotReady(message)) {
                return buildProjectNotReady();
            }
            return buildError(NanoHTTPD.Response.Status.INTERNAL_ERROR, "cypher_explain_error", message);
        } catch (IllegalArgumentException ex) {
            String message = safe(ex == null ? null : ex.getMessage());
            if (message.isBlank()) {
                message = "cypher query invalid";
            }
            String code;
            if (message.startsWith("cypher_feature_not_supported")) {
                code = "cypher_feature_not_supported";
            } else if (message.startsWith("cypher_parse_error")) {
                code = "cypher_parse_error";
            } else if (message.startsWith("cypher_empty_query")) {
                code = "cypher_empty_query";
            } else if (isProjectNotReady(message)) {
                code = "project_model_missing_rebuild";
            } else {
                code = "cypher_query_invalid";
            }
            if ("project_model_missing_rebuild".equals(code)) {
                return buildProjectNotReady();
            }
            return buildError(NanoHTTPD.Response.Status.BAD_REQUEST, code, message);
        } catch (Exception ex) {
            if (isProjectNotReady(ex == null ? null : ex.getMessage())) {
                return buildProjectNotReady();
            }
            return buildError(NanoHTTPD.Response.Status.INTERNAL_ERROR, "cypher_explain_error", ex.getMessage());
        }
    }

    private static boolean isProjectNotReady(String msg) {
        String value = safe(msg);
        return value.startsWith("project_model_missing_rebuild")
                || value.startsWith("graph_snapshot_missing_rebuild")
                || value.startsWith("graph_snapshot_load_failed")
                || value.startsWith("neo4j_store_open_fail");
    }

    private NanoHTTPD.Response buildProjectNotReady() {
        return buildError(
                NanoHTTPD.Response.Status.SERVICE_UNAVAILABLE,
                "project_model_missing_rebuild",
                "active project is not built, rebuild required");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
