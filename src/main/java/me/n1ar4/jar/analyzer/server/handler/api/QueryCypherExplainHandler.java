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
        } catch (IllegalArgumentException ex) {
            String message = ex.getMessage() == null ? "cypher query invalid" : ex.getMessage();
            String code;
            if (message.startsWith("cypher_feature_not_supported")) {
                code = "cypher_feature_not_supported";
            } else if (message.startsWith("cypher_parse_error")) {
                code = "cypher_parse_error";
            } else if (message.startsWith("cypher_empty_query")) {
                code = "cypher_empty_query";
            } else {
                code = "cypher_query_invalid";
            }
            return buildError(NanoHTTPD.Response.Status.BAD_REQUEST, code, message);
        } catch (Exception ex) {
            return buildError(NanoHTTPD.Response.Status.INTERNAL_ERROR, "cypher_explain_error", ex.getMessage());
        }
    }
}
