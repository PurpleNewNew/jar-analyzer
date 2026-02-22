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
    private QueryApiUtil() {
    }

    static RequestPayload parseRequest(NanoHTTPD.IHTTPSession session) throws Exception {
        if (session == null) {
            return new RequestPayload("", Collections.emptyMap(), QueryOptions.defaults(), "");
        }
        if (session.getMethod() == NanoHTTPD.Method.GET) {
            String query = first(session, "query");
            String projectKey = first(session, "projectKey");
            return new RequestPayload(query, Collections.emptyMap(), QueryOptions.defaults(), projectKey);
        }
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String body = files.get("postData");
        if (body == null || body.isBlank()) {
            return new RequestPayload("", Collections.emptyMap(), QueryOptions.defaults(), "");
        }
        JSONObject obj = JSON.parseObject(body);
        if (obj == null) {
            return new RequestPayload("", Collections.emptyMap(), QueryOptions.defaults(), "");
        }
        String query = obj.getString("query");
        String projectKey = obj.getString("projectKey");
        Map<String, Object> params = asMap(obj.getJSONObject("params"));
        Map<String, Object> options = asMap(obj.getJSONObject("options"));
        return new RequestPayload(query, params, QueryOptions.fromMap(options), projectKey);
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

    private static String first(NanoHTTPD.IHTTPSession session, String key) {
        List<String> values = session.getParameters().get(key);
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.get(0);
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
