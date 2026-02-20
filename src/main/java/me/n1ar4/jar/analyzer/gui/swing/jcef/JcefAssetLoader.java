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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class JcefAssetLoader {
    private static final Logger logger = LogManager.getLogger();

    private static final String ENTRY = "cypher-workbench/index.html";
    private static final String PREFIX = "cypher-workbench/";
    private static final String HOST = "cypher.workbench.local";
    private static final String SCHEME = "http";
    private static final String ENTRY_URL = SCHEME + "://" + HOST + "/index.html";
    private static final Map<String, String> MIME_TYPES = buildMimeTypes();
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
        return null;
    }

    private static FrontendAsset loadClasspathAsset(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String resourcePath = PREFIX + relativePath;
        try (InputStream in = JcefAssetLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            byte[] bytes = in.readAllBytes();
            String mimeType = detectMimeType(relativePath);
            return new FrontendAsset(bytes, mimeType, relativePath);
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
            byte[] bytes = readEntryBytes();
            if (bytes.length == 0) {
                throw new IllegalStateException("frontend entry empty: " + ENTRY);
            }
            entryVerified = true;
            logger.info("cypher frontend mapped to custom origin: {}", ENTRY_URL);
        }
    }

    private static byte[] readEntryBytes() {
        try (InputStream in = JcefAssetLoader.class.getClassLoader().getResourceAsStream(ENTRY)) {
            if (in == null) {
                throw new IllegalStateException("missing frontend entry: " + ENTRY);
            }
            return in.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("load frontend entry failed: " + ex.getMessage(), ex);
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
        map.put("js", "text/javascript");
        map.put("mjs", "text/javascript");
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
        return map;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class FrontendAsset {
        private final byte[] content;
        private final String mimeType;
        private final String path;

        private FrontendAsset(byte[] content, String mimeType, String path) {
            this.content = content == null ? new byte[0] : content;
            this.mimeType = safe(mimeType);
            this.path = safe(path);
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

        @Override
        public String toString() {
            return "FrontendAsset[path=" + path + ", size=" + content.length + ", mime=" + mimeType() + "]";
        }

        public String asUtf8String() {
            return new String(content, StandardCharsets.UTF_8);
        }
    }
}
