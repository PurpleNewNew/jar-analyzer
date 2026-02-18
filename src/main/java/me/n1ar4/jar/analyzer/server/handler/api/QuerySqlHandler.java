/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server.handler.api;

import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.query.QueryServices;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;

import java.util.List;
import java.util.Map;

public final class QuerySqlHandler extends ApiBaseHandler implements HttpHandler {
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
            QueryResult result = QueryServices.sql().execute(payload.query(), payload.params(), payload.options());
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            Map<String, Object> data = QueryApiUtil.buildData(result.getColumns(), result.getRows());
            return ok(data, QueryApiUtil.buildMeta(elapsedMs, result.isTruncated()), result.getWarnings());
        } catch (IllegalArgumentException ex) {
            return buildError(NanoHTTPD.Response.Status.BAD_REQUEST, safeCode(ex), safeMessage(ex));
        } catch (Exception ex) {
            return buildError(NanoHTTPD.Response.Status.INTERNAL_ERROR, "sql_query_error", ex.getMessage());
        }
    }

    private static String safeCode(IllegalArgumentException ex) {
        String msg = ex == null ? null : ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return "sql_query_invalid";
        }
        if ("sql_read_only".equals(msg)) {
            return msg;
        }
        return "sql_query_invalid";
    }

    private static String safeMessage(IllegalArgumentException ex) {
        String msg = ex == null ? null : ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return "sql query invalid";
        }
        if ("sql_read_only".equals(msg)) {
            return "sql api is read-only";
        }
        return msg;
    }
}
