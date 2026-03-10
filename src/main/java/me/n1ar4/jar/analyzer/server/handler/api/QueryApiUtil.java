/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server.handler.api;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.graph.query.QueryOptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class QueryApiUtil {
    private static final String INVALID_REQUEST = "invalid_request";

    private QueryApiUtil() {
    }

    static RequestPayload parseRequest(NanoHTTPD.IHTTPSession session) throws Exception {
        JSONObject obj = parseBodyObject(session);
        if (obj == null || obj.isEmpty()) {
            return new RequestPayload("", Collections.emptyMap(), QueryOptions.defaults(), "");
        }
        String query = obj.getString("query");
        String projectKey = obj.getString("projectKey");
        Map<String, Object> params = asMap(obj.getJSONObject("params"));
        Map<String, Object> options = asMap(obj.getJSONObject("options"));
        return new RequestPayload(query, params, QueryOptions.fromMap(options), projectKey);
    }

    static JSONObject parseBodyObject(NanoHTTPD.IHTTPSession session) throws Exception {
        if (session == null) {
            return new JSONObject();
        }
        Map<String, String> files = new HashMap<>();
        try {
            session.parseBody(files);
        } catch (Exception ex) {
            throw invalidRequest("invalid request body", ex);
        }
        String body = files.get("postData");
        if (body == null || body.isBlank()) {
            return new JSONObject();
        }
        try {
            JSONObject obj = JSON.parseObject(body);
            return obj == null ? new JSONObject() : obj;
        } catch (Exception ex) {
            throw invalidRequest("invalid json request body", ex);
        }
    }

    static boolean isInvalidRequest(String message) {
        return safe(message).startsWith(INVALID_REQUEST);
    }

    static String invalidRequestMessage(String message, String fallback) {
        String text = safe(message);
        if (text.startsWith(INVALID_REQUEST + ":")) {
            String detail = text.substring((INVALID_REQUEST + ":").length()).trim();
            if (!detail.isEmpty()) {
                return detail;
            }
        }
        return safe(fallback);
    }

    static String invalidRequestDetail(Throwable ex, String fallback) {
        return invalidRequestMessage(message(ex, ""), fallback);
    }

    static String message(Throwable ex, String fallback) {
        String text = ex == null ? null : ex.getMessage();
        return safe(text).isEmpty() ? safe(fallback) : safe(text);
    }

    static Map<String, Object> buildData(List<String> columns, List<List<Object>> rows) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("columns", columns == null ? List.of() : columns);
        data.put("rows", rows == null ? List.of() : rows);
        return data;
    }

    static Map<String, Object> buildMeta(long elapsedMs, boolean truncated) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("elapsedMs", elapsedMs);
        meta.put("truncated", truncated);
        return meta;
    }

    private static Map<String, Object> asMap(JSONObject obj) {
        if (obj == null || obj.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            out.put(entry.getKey(), entry.getValue());
        }
        return out;
    }

    private static IllegalArgumentException invalidRequest(String detail, Exception cause) {
        return new IllegalArgumentException(INVALID_REQUEST + ": " + safe(detail), cause);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    static final class RequestPayload {
        private final String query;
        private final Map<String, Object> params;
        private final QueryOptions options;
        private final String projectKey;

        private RequestPayload(String query,
                               Map<String, Object> params,
                               QueryOptions options,
                               String projectKey) {
            this.query = query == null ? "" : query;
            this.params = params == null ? Collections.emptyMap() : params;
            this.options = options == null ? QueryOptions.defaults() : options;
            this.projectKey = projectKey == null ? "" : projectKey.trim();
        }

        String query() {
            return query;
        }

        Map<String, Object> params() {
            return params;
        }

        QueryOptions options() {
            return options;
        }

        String projectKey() {
            return projectKey;
        }
    }
}
