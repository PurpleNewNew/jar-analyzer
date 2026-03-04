/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.jcef;

import com.alibaba.fastjson2.JSONObject;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandler;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class JcefBridgeRouter implements AutoCloseable {
    private static final String[] QUERY_ERROR_CODES = new String[]{
            "cypher_feature_not_supported",
            "cypher_parse_error",
            "cypher_empty_query",
            "cypher_query_timeout",
            "cypher_expand_budget_exceeded",
            "cypher_path_budget_exceeded",
            "project_model_missing_rebuild",
            "graph_snapshot_missing_rebuild",
            "graph_snapshot_load_failed",
            "neo4j_store_open_fail"
    };

    private final Map<String, ChannelHandler> handlers = new ConcurrentHashMap<>();
    private final CefMessageRouter router;
    private final CefMessageRouterHandler routerHandler = new CefMessageRouterHandlerAdapter() {
        @Override
        public boolean onQuery(CefBrowser browser,
                               CefFrame frame,
                               long queryId,
                               String request,
                               boolean persistent,
                               CefQueryCallback callback) {
            return handleQuery(request, callback);
        }
    };

    public JcefBridgeRouter() {
        this.router = CefMessageRouter.create();
        this.router.addHandler(routerHandler, true);
    }

    public void register(String channel, ChannelHandler handler) {
        if (channel == null || channel.isBlank() || handler == null) {
            return;
        }
        handlers.put(channel.trim(), handler);
    }

    public CefMessageRouter router() {
        return router;
    }

    private boolean handleQuery(String request, CefQueryCallback callback) {
        CypherBridgeProtocol.BridgeRequest bridgeRequest;
        try {
            bridgeRequest = CypherBridgeProtocol.parseRequest(request);
        } catch (CypherBridgeProtocol.BridgeException ex) {
            callback.success(CypherBridgeProtocol.error("", ex.code(), ex.getMessage()));
            return true;
        }

        ChannelHandler handler = handlers.get(bridgeRequest.channel());
        if (handler == null) {
            callback.success(CypherBridgeProtocol.error(
                    bridgeRequest.channel(),
                    "bridge_unknown_channel",
                    "unsupported channel"
            ));
            return true;
        }

        Thread.ofVirtual().name("jcef-bridge-" + bridgeRequest.channel()).start(() -> {
            try {
                Object data = handler.handle(bridgeRequest.payload());
                callback.success(CypherBridgeProtocol.success(bridgeRequest.channel(), data));
            } catch (CypherBridgeProtocol.BridgeException ex) {
                callback.success(CypherBridgeProtocol.error(bridgeRequest.channel(), ex.code(), ex.getMessage()));
            } catch (Throwable ex) {
                CypherBridgeProtocol.BridgeException bridgeError = normalizeBridgeError(ex);
                callback.success(CypherBridgeProtocol.error(
                        bridgeRequest.channel(),
                        bridgeError.code(),
                        bridgeError.getMessage()
                ));
            }
        });
        return true;
    }

    private static CypherBridgeProtocol.BridgeException normalizeBridgeError(Throwable ex) {
        if (ex instanceof CypherBridgeProtocol.BridgeException bridgeException) {
            return bridgeException;
        }
        if (ex instanceof IllegalArgumentException illegalArgumentException) {
            return mapIllegalArgument(illegalArgumentException);
        }
        Throwable cause = ex == null ? null : ex.getCause();
        if (cause instanceof IllegalArgumentException illegalArgumentException) {
            return mapIllegalArgument(illegalArgumentException);
        }
        String message = safe(ex == null ? null : ex.getMessage());
        if (message.isBlank() && cause != null) {
            message = safe(cause.getMessage());
        }
        if (message.isBlank()) {
            message = "bridge handler failed";
        }
        return new CypherBridgeProtocol.BridgeException("bridge_handler_error", message);
    }

    private static CypherBridgeProtocol.BridgeException mapIllegalArgument(IllegalArgumentException ex) {
        String message = safe(ex == null ? null : ex.getMessage());
        String code = resolveQueryErrorCode(message);
        if (message.isBlank()) {
            message = code;
        }
        return new CypherBridgeProtocol.BridgeException(code, message);
    }

    private static String resolveQueryErrorCode(String message) {
        String text = safe(message);
        for (String code : QUERY_ERROR_CODES) {
            if (text.startsWith(code)) {
                return code;
            }
        }
        return "cypher_query_invalid";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public void close() {
        router.removeHandler(routerHandler);
        router.dispose();
    }

    @FunctionalInterface
    public interface ChannelHandler {
        Object handle(JSONObject payload) throws Exception;
    }
}
