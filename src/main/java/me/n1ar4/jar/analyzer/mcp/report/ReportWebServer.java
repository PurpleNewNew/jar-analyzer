/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.mcp.report;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;
import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Embedded web UI for report MCP (history + websocket broadcast).
 */
public final class ReportWebServer extends NanoWSD {
    private static final Logger logger = LogManager.getLogger();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final ReportHub hub;
    private final String host;
    private final int port;
    private final String wsAddr;
    private final String indexHtml;

    public ReportWebServer(String host, int port, ReportHub hub) {
        super(host, port);
        this.hub = hub == null ? new ReportHub() : hub;
        this.host = host == null ? "127.0.0.1" : host;
        this.port = port;
        this.wsAddr = this.host + ":" + this.port;
        this.indexHtml = loadIndexHtml();
    }

    public boolean isStarted() {
        return started.get();
    }

    public void startServer() {
        if (started.get()) {
            return;
        }
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            started.set(true);
            logger.info("start report web server: {}:{}", host, port);
        } catch (Exception ex) {
            started.set(false);
            logger.warn("start report web server failed: {}", ex.toString());
        }
    }

    public void stopServer() {
        if (!started.get()) {
            return;
        }
        started.set(false);
        try {
            stop();
        } catch (Exception ignored) {
        }
        logger.info("stop report web server: {}:{}", host, port);
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) {
        String uri = handshake == null ? "" : handshake.getUri();
        boolean allowed = "/ws".equals(uri);
        return new ReportSocket(handshake, hub, allowed);
    }

    @Override
    protected NanoHTTPD.Response serveHttp(IHTTPSession session) {
        try {
            String uri = session == null ? "" : session.getUri();
            if ("/api/history".equals(uri)) {
                NanoHTTPD.Response resp = NanoHTTPD.newFixedLengthResponse(
                        NanoHTTPD.Response.Status.OK,
                        "application/json",
                        hub.loadHistory().toJSONString());
                resp.addHeader("Access-Control-Allow-Origin", "*");
                return resp;
            }
            if ("/".equals(uri) || "/index.html".equals(uri)) {
                String addr = wsAddr;
                try {
                    if (session != null && session.getHeaders() != null) {
                        // NanoHTTPD uses lower-case header keys.
                        String hostHeader = session.getHeaders().get("host");
                        if (hostHeader == null) {
                            hostHeader = session.getHeaders().get("Host");
                        }
                        if (hostHeader != null && !hostHeader.trim().isEmpty()) {
                            addr = hostHeader.trim();
                        }
                    }
                } catch (Exception ignored) {
                }
                String html = indexHtml == null ? "" : indexHtml.replace("__JAR_ANALYZER_REPORT_MCP__", addr);
                return NanoHTTPD.newFixedLengthResponse(
                        NanoHTTPD.Response.Status.OK,
                        "text/html",
                        html);
            }
        } catch (Throwable t) {
            InterruptUtil.restoreInterruptIfNeeded(t);
            logger.debug("report web serve failed: {}", t.toString());
        }
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "not found");
    }

    private static String loadIndexHtml() {
        try (InputStream is = ReportWebServer.class.getResourceAsStream("/mcp/report/index.html")) {
            if (is == null) {
                return "";
            }
            byte[] bytes = is.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            logger.debug("load report index html failed: {}", ex.toString());
            return "";
        }
    }

    private static final class ReportSocket extends NanoWSD.WebSocket {
        private final ReportHub hub;
        private final boolean allowed;

        private ReportSocket(IHTTPSession handshakeRequest, ReportHub hub, boolean allowed) {
            super(handshakeRequest);
            this.hub = hub;
            this.allowed = allowed;
        }

        @Override
        protected void onOpen() {
            if (!allowed) {
                try {
                    close(WebSocketFrame.CloseCode.PolicyViolation, "invalid path", false);
                } catch (Exception ignored) {
                }
                return;
            }
            if (hub != null) {
                hub.addClient(this);
            }
        }

        @Override
        protected void onClose(WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
            if (hub != null) {
                hub.removeClient(this);
            }
        }

        @Override
        protected void onMessage(WebSocketFrame message) {
            // Server push only.
        }

        @Override
        protected void onPong(WebSocketFrame pong) {
            // Ignore.
        }

        @Override
        protected void onException(java.io.IOException exception) {
            if (hub != null) {
                hub.removeClient(this);
            }
        }
    }
}
