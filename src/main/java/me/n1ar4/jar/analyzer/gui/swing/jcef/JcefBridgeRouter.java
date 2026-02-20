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
                String message = ex.getMessage() == null ? "bridge handler failed" : ex.getMessage();
                callback.success(CypherBridgeProtocol.error(bridgeRequest.channel(), "bridge_handler_error", message));
            }
        });
        return true;
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
