/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server.handler.api;

import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.graph.query.QueryErrorClassifier;
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
            NanoHTTPD.Response projectRequestError = requireActiveProjectRequest(payload.projectKey());
            if (projectRequestError != null) {
                return projectRequestError;
            }
            QueryResult result = QueryServices.cypher().execute(
                    payload.query(),
                    payload.params(),
                    payload.options()
            );
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            Map<String, Object> data = QueryApiUtil.buildData(result.getColumns(), result.getRows());
            return ok(data, QueryApiUtil.buildMeta(elapsedMs, result.isTruncated()), result.getWarnings());
        } catch (IllegalStateException ex) {
            String message = safeMessage(ex, "cypher query failed");
            NanoHTTPD.Response stateResponse = projectStateError(message);
            if (stateResponse != null) {
                return stateResponse;
            }
            return buildError(NanoHTTPD.Response.Status.INTERNAL_ERROR, QueryErrorClassifier.CYPHER_QUERY_ERROR, message);
        } catch (IllegalArgumentException ex) {
            String message = safeMessage(ex, "cypher query invalid");
            NanoHTTPD.Response stateResponse = projectStateError(message);
            if (stateResponse != null) {
                return stateResponse;
            }
            return buildError(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
                    QueryErrorClassifier.codeOf(message),
                    QueryErrorClassifier.publicMessage(message, "cypher query invalid"));
        } catch (Exception ex) {
            String message = safeMessage(ex, "cypher query failed");
            NanoHTTPD.Response stateResponse = projectStateError(message);
            if (stateResponse != null) {
                return stateResponse;
            }
            return buildError(NanoHTTPD.Response.Status.INTERNAL_ERROR, QueryErrorClassifier.CYPHER_QUERY_ERROR, message);
        }
    }

    private static String safeMessage(Throwable ex, String fallback) {
        String msg = ex == null ? null : ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return fallback;
        }
        return msg;
    }
}
