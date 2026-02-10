/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.mcp.transport;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import me.n1ar4.jar.analyzer.mcp.McpConstants;
import me.n1ar4.jar.analyzer.mcp.McpDispatcher;
import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Embedded MCP server with SSE (/sse + /message) and streamable-http (/mcp).
 * <p>
 * This intentionally implements only what's needed for Jar Analyzer MCP tools.
 */
public final class McpHttpServer {
    private static final Logger logger = LogManager.getLogger();
    private static final String SESSION_PREFIX = "mcp-session-";

    private final String bind;
    private final int port;
    private final McpDispatcher dispatcher;

    private final Map<String, SseSession> sseSessions = new HashMap<>();
    private final Object sseLock = new Object();

    private HttpServer server;
    private ExecutorService executor;
    private ExecutorService messageExecutor;
    private final AtomicBoolean started = new AtomicBoolean(false);

    public McpHttpServer(String bind, int port, McpDispatcher dispatcher) {
        this.bind = (bind == null || bind.isBlank()) ? "0.0.0.0" : bind.strip();
        this.port = port;
        this.dispatcher = dispatcher;
    }

    public String getBind() {
        return bind;
    }

    public int getPort() {
        return port;
    }

    public boolean isStarted() {
        return started.get();
    }

    public void start() throws IOException {
        if (started.get()) {
            return;
        }
        if (dispatcher == null) {
            throw new IllegalStateException("dispatcher is null");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("invalid port: " + port);
        }
        InetSocketAddress addr = new InetSocketAddress(bind, port);
        HttpServer httpServer = HttpServer.create(addr, 0);

        // Virtual threads scale better for IO-heavy HTTP request handling.
        ExecutorService pool = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("jar-analyzer-mcp-" + port + "-", 0).factory()
        );
        httpServer.setExecutor(pool);
        this.executor = pool;
        this.messageExecutor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("jar-analyzer-mcp-msg-" + port + "-", 0).factory()
        );

        httpServer.createContext("/sse", new SseHandler());
        httpServer.createContext("/message", new MessageHandler());
        // Prefix context: handles /mcp and /mcp/*
        httpServer.createContext("/mcp", new StreamableHttpHandler());

        httpServer.start();
        this.server = httpServer;
        started.set(true);
        logger.info("start mcp server: {}:{}", bind, port);
    }

    public void stop() {
        if (!started.get()) {
            return;
        }
        started.set(false);
        synchronized (sseLock) {
            for (SseSession s : sseSessions.values()) {
                if (s != null) {
                    s.close();
                }
            }
            sseSessions.clear();
        }
        try {
            if (server != null) {
                server.stop(0);
            }
        } catch (Exception ignored) {
        }
        shutdownExecutor(messageExecutor);
        shutdownExecutor(executor);
        logger.info("stop mcp server: {}:{}", bind, port);
    }

    private static void shutdownExecutor(ExecutorService es) {
        if (es == null) {
            return;
        }
        es.shutdownNow();
        try {
            es.awaitTermination(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private final class SseHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writePlain(exchange, 405, "Method not allowed");
                return;
            }

            Headers resp = exchange.getResponseHeaders();
            resp.set("Content-Type", "text/event-stream");
            resp.set("Cache-Control", "no-cache");
            resp.set("Connection", "keep-alive");
            resp.set("Access-Control-Allow-Origin", "*");

            // Chunked encoding.
            exchange.sendResponseHeaders(200, 0);

            String sessionId = UUID.randomUUID().toString();
            SseSession session = new SseSession(sessionId);
            synchronized (sseLock) {
                sseSessions.put(sessionId, session);
            }

            try (OutputStream os = exchange.getResponseBody()) {
                // Initial endpoint event: data contains message endpoint with sessionId.
                String endpoint = "/message?sessionId=" + sessionId;
                os.write(("event: endpoint\ndata: " + endpoint + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                os.flush();

                while (started.get() && !session.closed.get()) {
                    String event;
                    try {
                        event = session.queue.poll(30, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    if (event == null) {
                        continue;
                    }
                    try {
                        os.write(event.getBytes(StandardCharsets.UTF_8));
                        os.flush();
                    } catch (IOException io) {
                        // Client disconnected.
                        return;
                    }
                }
            } finally {
                session.close();
                synchronized (sseLock) {
                    sseSessions.remove(sessionId);
                }
            }
        }
    }

    private final class MessageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJsonRpcError(exchange, null, McpConstants.INVALID_REQUEST, "Method not allowed");
                return;
            }
            URI uri = exchange.getRequestURI();
            String sessionId = parseQuery(uri == null ? null : uri.getRawQuery(), "sessionId");
            if (sessionId == null || sessionId.isEmpty()) {
                writeJsonRpcError(exchange, null, McpConstants.INVALID_PARAMS, "Missing sessionId");
                return;
            }
            SseSession session;
            synchronized (sseLock) {
                session = sseSessions.get(sessionId);
            }
            if (session == null) {
                writeJsonRpcError(exchange, null, McpConstants.INVALID_PARAMS, "Invalid session ID");
                return;
            }

            byte[] body = exchange.getRequestBody().readAllBytes();
            // Validate JSON early so we can return a proper parse error.
            try {
                JSON.parseObject(new String(body, StandardCharsets.UTF_8));
            } catch (Exception ex) {
                writeJsonRpcError(exchange, null, McpConstants.PARSE_ERROR, "Parse error");
                return;
            }

            // Fast return 202 with no body; process asynchronously and send response via SSE.
            Map<String, List<String>> headers = copyHeaders(exchange.getRequestHeaders());
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
            messageExecutor.submit(() -> {
                try {
                    JSONObject resp = dispatcher.handle(body, headers);
                    if (resp == null) {
                        return;
                    }
                    String json = resp.toJSONString();
                    session.enqueue("event: message\ndata: " + json + "\n\n");
                } catch (Throwable t) {
                    InterruptUtil.restoreInterruptIfNeeded(t);
                    logger.debug("mcp message handle failed: {}", t.toString());
                }
            });
        }
    }

    private final class StreamableHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ("POST".equalsIgnoreCase(method)) {
                handlePost(exchange);
                return;
            }
            if ("DELETE".equalsIgnoreCase(method)) {
                // StatelessGeneratingSessionIdManager Terminate is a no-op.
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }
            writePlain(exchange, 404, "Not found");
        }

        private void handlePost(HttpExchange exchange) throws IOException {
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
                writePlain(exchange, 400, "Invalid content type: must be 'application/json'");
                return;
            }
            byte[] body = exchange.getRequestBody().readAllBytes();
            JSONObject base;
            try {
                base = JSON.parseObject(new String(body, StandardCharsets.UTF_8));
            } catch (Exception ex) {
                writeJsonRpcError(exchange, null, McpConstants.PARSE_ERROR, "request body is not valid json");
                return;
            }
            String m = base == null ? null : base.getString("method");
            boolean isInitialize = "initialize".equals(m);

            String sessionId = null;
            if (isInitialize) {
                sessionId = SESSION_PREFIX + UUID.randomUUID();
            } else {
                sessionId = exchange.getRequestHeaders().getFirst(McpConstants.HEADER_SESSION_ID);
                if (!isValidStreamableSessionId(sessionId)) {
                    writePlain(exchange, 400, "Invalid session ID");
                    return;
                }
            }

            Map<String, List<String>> headers = copyHeaders(exchange.getRequestHeaders());
            JSONObject resp = dispatcher.handle(body, headers);
            if (resp == null) {
                exchange.sendResponseHeaders(202, -1);
                exchange.close();
                return;
            }
            byte[] out = resp.toJSONString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            if (isInitialize && sessionId != null) {
                exchange.getResponseHeaders().set(McpConstants.HEADER_SESSION_ID, sessionId);
            }
            exchange.sendResponseHeaders(200, out.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(out);
            }
        }
    }

    private static boolean isValidStreamableSessionId(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }
        if (!sessionId.startsWith(SESSION_PREFIX)) {
            return false;
        }
        String uuid = sessionId.substring(SESSION_PREFIX.length());
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static Map<String, List<String>> copyHeaders(Headers headers) {
        if (headers == null || headers.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> out = new HashMap<>();
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            List<String> values = e.getValue();
            out.put(e.getKey(), values == null ? new ArrayList<>() : new ArrayList<>(values));
        }
        return out;
    }

    private static void writePlain(HttpExchange exchange, int status, String body) throws IOException {
        if (exchange == null) {
            return;
        }
        byte[] out = (body == null ? "" : body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, out.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(out);
        }
    }

    private static void writeJsonRpcError(HttpExchange exchange, Object id, int code, String message) throws IOException {
        JSONObject err = new JSONObject();
        err.put("code", code);
        err.put("message", message == null ? "" : message);
        JSONObject resp = new JSONObject();
        resp.put("jsonrpc", McpConstants.JSONRPC_VERSION);
        resp.put("id", id);
        resp.put("error", err);
        byte[] out = resp.toJSONString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(400, out.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(out);
        }
    }

    private static String parseQuery(String rawQuery, String key) {
        if (rawQuery == null || rawQuery.isEmpty() || key == null || key.isEmpty()) {
            return null;
        }
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            if (pair == null || pair.isEmpty()) {
                continue;
            }
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String k = pair.substring(0, idx);
            if (!key.equals(k)) {
                continue;
            }
            return pair.substring(idx + 1);
        }
        return null;
    }

    private static final class SseSession {
        private final String sessionId;
        private final ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(100);
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private SseSession(String sessionId) {
            this.sessionId = sessionId;
        }

        private void enqueue(String event) {
            if (event == null || closed.get()) {
                return;
            }
            // Best-effort: drop when full.
            queue.offer(event);
        }

        private void close() {
            closed.set(true);
        }
    }
}
