/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server.handler.api;

import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.query.QueryServices;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;

import java.util.Map;

public final class QueryCypherHandler extends ApiBaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        if (session.getMethod() != NanoHTTPD.Method.POST) {
            return buildError(
                    NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED,
                    "method_not_allowed",
                    "POST required");
        }
        long start = System.nanoTime();
        try {
            QueryApiUtil.RequestPayload payload = QueryApiUtil.parseRequest(session);
            if (payload.query().isBlank()) {
                return needParam("query");
            }
            QueryResult result = QueryServices.cypher().execute(
                    payload.query(),
                    payload.params(),
                    payload.options(),
                    payload.projectKey()
            );
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            Map<String, Object> data = QueryApiUtil.buildData(result.getColumns(), result.getRows());
            return ok(data, QueryApiUtil.buildMeta(elapsedMs, result.isTruncated()), result.getWarnings());
        } catch (IllegalStateException ex) {
            String message = safeMessage(ex);
            if (isProjectNotReady(message)) {
                return buildProjectNotReady();
            }
            return buildError(NanoHTTPD.Response.Status.INTERNAL_ERROR, "cypher_query_error", message);
        } catch (IllegalArgumentException ex) {
            String message = safeMessage(ex);
            return buildError(NanoHTTPD.Response.Status.BAD_REQUEST, safeCode(message), message);
        } catch (Exception ex) {
            if (isProjectNotReady(ex == null ? null : ex.getMessage())) {
                return buildProjectNotReady();
            }
            return buildError(NanoHTTPD.Response.Status.INTERNAL_ERROR, "cypher_query_error", ex.getMessage());
        }
    }

    private static String safeCode(String msg) {
        if (msg == null || msg.isBlank()) {
            return "cypher_query_invalid";
        }
        if (msg.startsWith("cypher_feature_not_supported")) {
            return "cypher_feature_not_supported";
        }
        if (msg.startsWith("cypher_parse_error")) {
            return "cypher_parse_error";
        }
        if (msg.startsWith("cypher_empty_query")) {
            return "cypher_empty_query";
        }
        if (msg.startsWith("cypher_query_timeout")) {
            return "cypher_query_timeout";
        }
        if (msg.startsWith("cypher_expand_budget_exceeded")) {
            return "cypher_expand_budget_exceeded";
        }
        if (msg.startsWith("cypher_path_budget_exceeded")) {
            return "cypher_path_budget_exceeded";
        }
        if (msg.startsWith("project_model_missing_rebuild")
                || msg.startsWith("graph_snapshot_missing_rebuild")
                || msg.startsWith("graph_snapshot_load_failed")
                || msg.startsWith("neo4j_store_open_fail")) {
            return "project_model_missing_rebuild";
        }
        return "cypher_query_invalid";
    }

    private static boolean isProjectNotReady(String msg) {
        String value = msg == null ? "" : msg.trim();
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

    private static String safeMessage(Throwable ex) {
        String msg = ex == null ? null : ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return "cypher query invalid";
        }
        return msg;
    }
}
