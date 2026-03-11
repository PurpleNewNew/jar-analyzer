/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.jcef;

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class JcefAssetLoader {
    private static final Logger logger = LogManager.getLogger();
    private static final byte[] EMPTY_BYTES = new byte[0];

    private static final String ENTRY = "cypher-workbench/index.html";
    private static final String PREFIX = "cypher-workbench/";
    private static final String HOST = "cypher.workbench.local";
    private static final String SCHEME = "http";
    private static final String ENTRY_URL = SCHEME + "://" + HOST + "/index.html";
    private static final Map<String, String> MIME_TYPES = buildMimeTypes();
    private static final Map<String, FrontendAsset> ASSET_CACHE = new ConcurrentHashMap<>();
    private static volatile boolean entryVerified;

    private JcefAssetLoader() {
    }

    public static String resolveEntryUrl() {
        ensureEntryExists();
        return ENTRY_URL;
    }

    public static boolean isWorkbenchUrl(String rawUrl) {
        URI uri = parseUri(rawUrl);
        if (uri == null) {
            return false;
        }
        String scheme = safe(uri.getScheme()).toLowerCase(Locale.ROOT);
        String host = safe(uri.getHost()).toLowerCase(Locale.ROOT);
        return SCHEME.equals(scheme) && HOST.equals(host);
    }

    public static FrontendAsset resolveAsset(String rawUrl) {
        URI uri = parseUri(rawUrl);
        if (uri == null) {
            return null;
        }
        if (!isWorkbenchUrl(rawUrl)) {
            return null;
        }
        String normalizedPath = normalizePath(uri.getPath());
        if (normalizedPath == null) {
            return null;
        }
        FrontendAsset direct = loadClasspathAsset(normalizedPath);
        if (direct != null) {
            return direct;
        }
        if (!hasFileExtension(normalizedPath)) {
            return loadClasspathAsset("index.html");
        }
        return notFoundAsset(normalizedPath);
    }

    private static FrontendAsset loadClasspathAsset(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        FrontendAsset cached = ASSET_CACHE.get(relativePath);
        if (cached != null) {
            return cached;
        }
        String resourcePath = PREFIX + relativePath;
        try (InputStream in = JcefAssetLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            byte[] bytes = in.readAllBytes();
            String mimeType = detectMimeType(relativePath);
            FrontendAsset asset = FrontendAsset.ok(bytes, mimeType, relativePath);
            FrontendAsset raced = ASSET_CACHE.putIfAbsent(relativePath, asset);
            return raced == null ? asset : raced;
        } catch (IOException ex) {
            logger.warn("load cypher frontend resource failed: {} ({})", resourcePath, ex.toString());
            return null;
        }
    }

    private static void ensureEntryExists() {
        if (entryVerified) {
            return;
        }
        synchronized (JcefAssetLoader.class) {
            if (entryVerified) {
                return;
            }
            FrontendAsset entry = loadClasspathAsset("index.html");
            if (entry == null) {
                throw new IllegalStateException("missing frontend entry: " + ENTRY);
            }
            if (entry.content().length == 0) {
                throw new IllegalStateException("frontend entry empty: " + ENTRY);
            }
            entryVerified = true;
            logger.debug("cypher frontend mapped to custom origin: {}", ENTRY_URL);
        }
    }

    private static URI parseUri(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        try {
            return new URI(rawUrl.trim());
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private static String normalizePath(String rawPath) {
        String value = safe(rawPath);
        if (value.isBlank() || "/".equals(value)) {
            return "index.html";
        }
        String normalized = value.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank()) {
            return "index.html";
        }
        if (normalized.contains("..")) {
            return null;
        }
        return normalized;
    }

    private static boolean hasFileExtension(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        int slash = value.lastIndexOf('/');
        int dot = value.lastIndexOf('.');
        return dot > slash;
    }

    private static String detectMimeType(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return "application/octet-stream";
        }
        int dot = relativePath.lastIndexOf('.');
        if (dot <= 0 || dot >= relativePath.length() - 1) {
            return "application/octet-stream";
        }
        String ext = relativePath.substring(dot + 1).toLowerCase(Locale.ROOT);
        return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
    }

    private static Map<String, String> buildMimeTypes() {
        Map<String, String> map = new HashMap<>();
        map.put("html", "text/html");
        map.put("htm", "text/html");
        map.put("css", "text/css");
        map.put("js", "application/javascript");
        map.put("mjs", "application/javascript");
        map.put("json", "application/json");
        map.put("map", "application/json");
        map.put("svg", "image/svg+xml");
        map.put("png", "image/png");
        map.put("jpg", "image/jpeg");
        map.put("jpeg", "image/jpeg");
        map.put("gif", "image/gif");
        map.put("ico", "image/x-icon");
        map.put("woff", "font/woff");
        map.put("woff2", "font/woff2");
        map.put("ttf", "font/ttf");
        map.put("txt", "text/plain");
        return Collections.unmodifiableMap(map);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static FrontendAsset notFoundAsset(String relativePath) {
        return FrontendAsset.notFound(relativePath);
    }

    public static final class FrontendAsset {
        private final byte[] content;
        private final String mimeType;
        private final String path;
        private final int statusCode;
        private final String statusText;

        private FrontendAsset(byte[] content, String mimeType, String path, int statusCode, String statusText) {
            this.content = content == null ? EMPTY_BYTES : content;
            this.mimeType = safe(mimeType);
            this.path = safe(path);
            this.statusCode = statusCode <= 0 ? 200 : statusCode;
            this.statusText = safe(statusText);
        }

        public byte[] content() {
            return content;
        }

        public String mimeType() {
            return mimeType.isBlank() ? "application/octet-stream" : mimeType;
        }

        public String path() {
            return path;
        }

        public int statusCode() {
            return statusCode;
        }

        public String statusText() {
            return statusText.isBlank() ? "OK" : statusText;
        }

        @Override
        public String toString() {
            return "FrontendAsset[path=" + path + ", status=" + statusCode()
                    + ", size=" + content.length + ", mime=" + mimeType() + "]";
        }

        public String asUtf8String() {
            return new String(content, StandardCharsets.UTF_8);
        }

        private static FrontendAsset ok(byte[] content, String mimeType, String path) {
            return new FrontendAsset(content, mimeType, path, 200, "OK");
        }

        private static FrontendAsset notFound(String path) {
            return new FrontendAsset(
                    ("Not Found: " + safe(path)).getBytes(StandardCharsets.UTF_8),
                    "text/plain",
                    path,
                    404,
                    "Not Found"
            );
        }
    }
}
