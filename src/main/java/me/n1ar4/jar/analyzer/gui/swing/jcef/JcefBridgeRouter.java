/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.jcef;

import com.alibaba.fastjson2.JSONObject;
import me.n1ar4.jar.analyzer.graph.query.QueryErrorClassifier;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandler;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class JcefBridgeRouter implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger();
    private final Map<String, ChannelHandler> handlers = new ConcurrentHashMap<>();
    private final CefMessageRouter router;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final CefMessageRouterHandler routerHandler = new CefMessageRouterHandlerAdapter() {
        @Override
        public boolean onQuery(CefBrowser browser,
                               CefFrame frame,
                               long queryId,
                               String request,
                               boolean persistent,
                               CefQueryCallback callback) {
            return dispatchQuery(request, callback);
        }
    };

    public JcefBridgeRouter() {
        this.router = CefMessageRouter.create();
        this.router.addHandler(routerHandler, true);
    }

    public void register(String channel, ChannelHandler handler) {
        if (closed.get() || channel == null || channel.isBlank() || handler == null) {
            return;
        }
        handlers.put(channel.trim(), handler);
    }

    public CefMessageRouter router() {
        return router;
    }

    boolean dispatchQuery(String request, CefQueryCallback callback) {
        if (closed.get()) {
            reply(callback, CypherBridgeProtocol.error("", "bridge_closed", "bridge closed"), true);
            return true;
        }
        CypherBridgeProtocol.BridgeRequest bridgeRequest;
        try {
            bridgeRequest = CypherBridgeProtocol.parseRequest(request);
        } catch (CypherBridgeProtocol.BridgeException ex) {
            reply(callback, CypherBridgeProtocol.error("", ex.code(), ex.getMessage()));
            return true;
        }

        ChannelHandler handler = handlers.get(bridgeRequest.channel());
        if (handler == null) {
            reply(callback, CypherBridgeProtocol.error(
                    bridgeRequest.channel(),
                    "bridge_unknown_channel",
                    "unsupported channel"
            ));
            return true;
        }

        Thread.ofVirtual().name("jcef-bridge-" + bridgeRequest.channel()).start(() -> {
            try {
                Object data = handler.handle(bridgeRequest.payload());
                if (closed.get()) {
                    return;
                }
                reply(callback, CypherBridgeProtocol.success(bridgeRequest.channel(), data));
            } catch (CypherBridgeProtocol.BridgeException ex) {
                if (closed.get()) {
                    return;
                }
                reply(callback, CypherBridgeProtocol.error(bridgeRequest.channel(), ex.code(), ex.getMessage()));
            } catch (Throwable ex) {
                if (closed.get()) {
                    return;
                }
                CypherBridgeProtocol.BridgeException bridgeError = normalizeBridgeError(ex);
                reply(callback, CypherBridgeProtocol.error(
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
        String code = QueryErrorClassifier.codeOf(message);
        String publicMessage = QueryErrorClassifier.publicMessage(message, code);
        return new CypherBridgeProtocol.BridgeException(code, publicMessage.isBlank() ? code : publicMessage);
    }

    private void reply(CefQueryCallback callback, String payload) {
        reply(callback, payload, false);
    }

    private void reply(CefQueryCallback callback, String payload, boolean allowWhenClosed) {
        if (callback == null || (!allowWhenClosed && closed.get())) {
            return;
        }
        try {
            callback.success(payload);
        } catch (Throwable ex) {
            logger.debug("reply jcef bridge query failed: {}", ex.toString());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        handlers.clear();
        router.removeHandler(routerHandler);
        router.dispose();
    }

    @FunctionalInterface
    public interface ChannelHandler {
        Object handle(JSONObject payload) throws Exception;
    }
}
