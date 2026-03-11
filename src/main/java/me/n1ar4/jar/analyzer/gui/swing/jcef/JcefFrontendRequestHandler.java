/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.jcef;

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefCallback;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.handler.CefResourceHandler;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.handler.CefResourceRequestHandler;
import org.cef.handler.CefResourceRequestHandlerAdapter;
import org.cef.misc.BoolRef;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

public final class JcefFrontendRequestHandler extends CefRequestHandlerAdapter {
    private static final Logger logger = LogManager.getLogger();
    private final CefResourceRequestHandler resourceRequestHandler = new CefResourceRequestHandlerAdapter() {
        @Override
        public CefResourceHandler getResourceHandler(CefBrowser browser, CefFrame frame, CefRequest request) {
            String url = request == null ? "" : request.getURL();
            JcefAssetLoader.FrontendAsset asset = JcefAssetLoader.resolveAsset(url);
            if (asset == null) {
                logger.debug("jcef frontend resource missing: {}", safe(url));
                return null;
            }
            return new InMemoryResourceHandler(asset);
        }
    };

    @Override
    public CefResourceRequestHandler getResourceRequestHandler(CefBrowser browser,
                                                               CefFrame frame,
                                                               CefRequest request,
                                                               boolean isNavigation,
                                                               boolean isDownload,
                                                               String requestInitiator,
                                                               BoolRef disableDefaultHandling) {
        String url = request == null ? "" : request.getURL();
        if (!JcefAssetLoader.isWorkbenchUrl(url)) {
            return null;
        }
        if (disableDefaultHandling != null) {
            disableDefaultHandling.set(true);
        }
        return resourceRequestHandler;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class InMemoryResourceHandler extends CefResourceHandlerAdapter {
        private final byte[] content;
        private final String mimeType;
        private final int statusCode;
        private final String statusText;
        private int offset;

        private InMemoryResourceHandler(JcefAssetLoader.FrontendAsset asset) {
            this.content = asset.content();
            this.mimeType = asset.mimeType();
            this.statusCode = asset.statusCode();
            this.statusText = asset.statusText();
            this.offset = 0;
        }

        @Override
        public boolean processRequest(CefRequest request, CefCallback callback) {
            offset = 0;
            if (callback != null) {
                callback.Continue();
            }
            return true;
        }

        @Override
        public void getResponseHeaders(CefResponse response, IntRef responseLength, StringRef redirectUrl) {
            String normalizedMime = normalizeMime(mimeType);
            int normalizedStatusCode = Math.max(100, statusCode);
            response.setStatus(normalizedStatusCode);
            response.setStatusText(statusText);
            if (normalizedStatusCode >= 400) {
                response.setError(CefLoadHandler.ErrorCode.ERR_FILE_NOT_FOUND);
            }
            response.setMimeType(normalizedMime);
            response.setHeaderByName("Content-Type", buildContentType(normalizedMime), true);
            response.setHeaderByName("Cache-Control", "no-cache, no-store, must-revalidate", true);
            response.setHeaderByName("Pragma", "no-cache", true);
            response.setHeaderByName("X-Content-Type-Options", "nosniff", true);
            responseLength.set(content.length);
        }

        @Override
        public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback callback) {
            if (offset >= content.length) {
                bytesRead.set(0);
                return false;
            }
            int actual = Math.min(bytesToRead, content.length - offset);
            System.arraycopy(content, offset, dataOut, 0, actual);
            offset += actual;
            bytesRead.set(actual);
            return true;
        }

        @Override
        public void cancel() {
            offset = content.length;
        }

        private static String normalizeMime(String value) {
            if (value == null || value.isBlank()) {
                return "application/octet-stream";
            }
            return value.trim().toLowerCase();
        }

        private static String buildContentType(String mime) {
            if (mime.startsWith("text/") || "application/json".equals(mime) || "application/javascript".equals(mime)) {
                return mime + "; charset=UTF-8";
            }
            return mime;
        }
    }
}
