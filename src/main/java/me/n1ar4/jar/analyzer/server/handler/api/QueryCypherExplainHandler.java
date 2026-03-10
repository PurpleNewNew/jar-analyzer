/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server.handler.api;

import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.graph.query.QueryErrorClassifier;
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
            NanoHTTPD.Response projectRequestError = requireActiveProjectRequest(payload.projectKey());
            if (projectRequestError != null) {
                return projectRequestError;
            }
            Map<String, Object> explain = QueryServices.cypher().explain(payload.query(), payload.projectKey());
            return ok(explain);
        } catch (IllegalStateException ex) {
            String message = QueryApiUtil.message(ex, "cypher query failed");
            NanoHTTPD.Response stateResponse = projectStateError(message);
            if (stateResponse != null) {
                return stateResponse;
            }
            return buildError(NanoHTTPD.Response.Status.INTERNAL_ERROR, "cypher_explain_error", message);
        } catch (IllegalArgumentException ex) {
            String message = QueryApiUtil.message(ex, "cypher query invalid");
            if (QueryApiUtil.isInvalidRequest(message)) {
                return buildError(
                        NanoHTTPD.Response.Status.BAD_REQUEST,
                        "invalid_request",
                        QueryApiUtil.invalidRequestDetail(ex, "invalid request body"));
            }
            NanoHTTPD.Response stateResponse = projectStateError(message);
            if (stateResponse != null) {
                return stateResponse;
            }
            return buildError(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
                    QueryErrorClassifier.codeOf(message),
                    QueryErrorClassifier.publicMessage(message, "cypher query invalid"));
        } catch (Exception ex) {
            String message = QueryApiUtil.message(ex, "cypher query failed");
            NanoHTTPD.Response stateResponse = projectStateError(message);
            if (stateResponse != null) {
                return stateResponse;
            }
            return buildError(NanoHTTPD.Response.Status.INTERNAL_ERROR, "cypher_explain_error", message);
        }
    }
}
