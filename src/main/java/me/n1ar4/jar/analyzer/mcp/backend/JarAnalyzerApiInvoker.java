/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.mcp.backend;

import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.server.PathMatcher;
import me.n1ar4.jar.analyzer.server.ServerConfig;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Invoke Jar Analyzer HTTP API handlers in-process (no real network).
 * <p>
 * This keeps tool behavior consistent with the legacy Go MCP proxy (which called /api/*).
 */
public final class JarAnalyzerApiInvoker {
    private static final NanoHTTPD COOKIE_OUTER = new NanoHTTPD("127.0.0.1", 0) {
        @Override
        public Response serve(IHTTPSession session) {
            return null;
        }
    };

    private final PathMatcher matcher;
    private final ServerConfig config;

    public JarAnalyzerApiInvoker(ServerConfig config) {
        this.config = config == null ? new ServerConfig() : config;
        this.matcher = new PathMatcher(this.config);
    }

    public String get(String path, Map<String, String> params) throws Exception {
        String uri = (path == null || path.trim().isEmpty()) ? "/" : path.trim();

        Map<String, List<String>> parameters = new HashMap<>();
        Map<String, String> flat = new HashMap<>();
        StringBuilder query = new StringBuilder();
        if (params != null) {
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (e.getKey() == null) {
                    continue;
                }
                String k = e.getKey();
                String v = e.getValue() == null ? "" : e.getValue();
                parameters.put(k, Collections.singletonList(v));
                flat.put(k, v);
                if (query.length() > 0) {
                    query.append('&');
                }
                query.append(k).append('=').append(v);
            }
        }

        Map<String, String> headers = new HashMap<>();
        if (config.isAuth()) {
            String token = config.getToken();
            if (token != null && !token.trim().isEmpty()) {
                // PathMatcher checks both Token and token due to framework casing issues.
                headers.put("Token", token);
                headers.put("token", token);
            }
        }

        NanoHTTPD.IHTTPSession session = new FakeSession(
                NanoHTTPD.Method.GET,
                uri,
                query.toString(),
                parameters,
                flat,
                headers
        );
        NanoHTTPD.Response resp = matcher.handleReq(session);
        if (resp == null) {
            throw new Exception("empty response");
        }
        int status = resp.getStatus() == null ? 0 : resp.getStatus().getRequestStatus();
        String body = readResponseBody(resp);
        if (status != 200) {
            throw new Exception("http " + status + ": " + body);
        }
        return body;
    }

    private static String readResponseBody(NanoHTTPD.Response resp) throws Exception {
        InputStream is = resp.getData();
        if (is == null) {
            return "";
        }
        byte[] bytes = is.readAllBytes();
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static final class FakeSession implements NanoHTTPD.IHTTPSession {
        private final NanoHTTPD.Method method;
        private final String uri;
        private final String query;
        private final Map<String, List<String>> parameters;
        private final Map<String, String> parms;
        private final Map<String, String> headers;

        private FakeSession(NanoHTTPD.Method method,
                            String uri,
                            String query,
                            Map<String, List<String>> parameters,
                            Map<String, String> parms,
                            Map<String, String> headers) {
            this.method = method == null ? NanoHTTPD.Method.GET : method;
            this.uri = uri == null ? "/" : uri;
            this.query = query == null ? "" : query;
            this.parameters = parameters == null ? new HashMap<>() : parameters;
            this.parms = parms == null ? new HashMap<>() : parms;
            this.headers = headers == null ? new HashMap<>() : headers;
        }

        @Override
        public void execute() {
            // No-op.
        }

        @Override
        public NanoHTTPD.CookieHandler getCookies() {
            // CookieHandler is a non-static inner class in NanoHTTPD.
            return COOKIE_OUTER.new CookieHandler(headers);
        }

        @Override
        public Map<String, String> getHeaders() {
            return headers;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public NanoHTTPD.Method getMethod() {
            return method;
        }

        @Override
        public Map<String, String> getParms() {
            return parms;
        }

        @Override
        public Map<String, List<String>> getParameters() {
            return parameters;
        }

        @Override
        public String getQueryParameterString() {
            return query;
        }

        @Override
        public String getUri() {
            return uri;
        }

        @Override
        public void parseBody(Map<String, String> files) {
            // No-op for GET endpoints.
        }

        @Override
        public String getRemoteIpAddress() {
            return "127.0.0.1";
        }

        @Override
        public String getRemoteHostName() {
            return "localhost";
        }
    }
}
