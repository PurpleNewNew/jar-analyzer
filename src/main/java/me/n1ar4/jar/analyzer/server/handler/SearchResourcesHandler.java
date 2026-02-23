/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.server.handler;

import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.jar.analyzer.entity.ResourceSearchResult;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.server.handler.api.ApiBaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.IOUtils;
import me.n1ar4.jar.analyzer.utils.PathResolver;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SearchResourcesHandler extends ApiBaseHandler implements HttpHandler {
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final int DEFAULT_MAX_BYTES = 256 * 1024;
    private static final int MAX_MAX_BYTES = 1024 * 1024;

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = MainForm.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        String query = getParam(session, "query");
        if (StringUtil.isNull(query)) {
            query = getParam(session, "q");
        }
        if (StringUtil.isNull(query)) {
            return needParam("query");
        }
        String mode = getParam(session, "mode");
        if (StringUtil.isNull(mode)) {
            mode = "or";
        }
        boolean caseSensitive = getBoolParam(session, "case", false);
        if (!caseSensitive) {
            caseSensitive = getBoolParam(session, "caseSensitive", false);
        }

        Integer jarId = getIntParamNullable(session, "jarId");
        int limit = getIntParam(session, "limit", DEFAULT_LIMIT);
        if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }
        if (limit < 1) {
            limit = DEFAULT_LIMIT;
        }
        int maxBytes = getIntParam(session, "maxBytes", DEFAULT_MAX_BYTES);
        if (maxBytes > MAX_MAX_BYTES) {
            maxBytes = MAX_MAX_BYTES;
        }
        if (maxBytes < 4096) {
            maxBytes = DEFAULT_MAX_BYTES;
        }

        List<String> keywords = splitKeywords(query);
        if (keywords.isEmpty()) {
            return needParam("query");
        }
        List<String> keywordsNorm = normalizeKeywords(keywords, caseSensitive);

        ArrayList<ResourceEntity> resources = engine.getTextResources(jarId);
        List<ResourceSearchResult> results = new ArrayList<>();
        for (ResourceEntity resource : resources) {
            if (results.size() >= limit) {
                break;
            }
            String path = resource.getResourcePath();
            if (!StringUtil.isNull(path)) {
                String pathTarget = caseSensitive ? path : path.toLowerCase();
                if (matchesKeywords(pathTarget, keywordsNorm, mode)) {
                    results.add(buildResult(resource, "path", buildPreview(path, keywords, caseSensitive, mode)));
                    continue;
                }
            }
            Path filePath = PathResolver.resolveResourceFile(resource);
            if (filePath == null || !Files.exists(filePath)) {
                continue;
            }
            String content = readText(filePath, maxBytes);
            if (StringUtil.isNull(content)) {
                continue;
            }
            String contentTarget = caseSensitive ? content : content.toLowerCase();
            if (matchesKeywords(contentTarget, keywordsNorm, mode)) {
                String preview = buildPreview(content, keywords, caseSensitive, mode);
                results.add(buildResult(resource, "content", preview));
            }
        }

        return ok(results, pageMeta(0, limit, results.size(), null));
    }

    private ResourceSearchResult buildResult(ResourceEntity resource, String matchType, String preview) {
        ResourceSearchResult result = new ResourceSearchResult();
        result.setJarId(resource.getJarId());
        result.setJarName(resource.getJarName());
        result.setResourcePath(resource.getResourcePath());
        result.setFileSize(resource.getFileSize());
        result.setIsText(resource.getIsText());
        result.setMatchType(matchType);
        result.setPreview(preview);
        return result;
    }

    private String readText(Path path, int maxBytes) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            byte[] data = IOUtils.readNBytes(inputStream, maxBytes);
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private String snippet(String content, int idx, int queryLen) {
        int start = Math.max(0, idx - 80);
        int end = Math.min(content.length(), idx + queryLen + 80);
        String part = content.substring(start, end);
        if (start > 0) {
            part = "..." + part;
        }
        if (end < content.length()) {
            part = part + "...";
        }
        return part;
    }

    private String buildPreview(String content, List<String> keywords, boolean caseSensitive, String mode) {
        if (StringUtil.isNull(content) || keywords == null || keywords.isEmpty()) {
            return "";
        }
        String target = caseSensitive ? content : content.toLowerCase();
        List<String> keys = normalizeKeywords(keywords, caseSensitive);
        int idx = -1;
        int len = 0;
        if ("and".equalsIgnoreCase(mode)) {
            String first = keys.get(0);
            idx = target.indexOf(first);
            len = first.length();
        } else {
            for (String key : keys) {
                int hit = target.indexOf(key);
                if (hit >= 0) {
                    idx = hit;
                    len = key.length();
                    break;
                }
            }
        }
        if (idx < 0) {
            return "";
        }
        return snippet(content, idx, len);
    }

    private boolean getBoolParam(NanoHTTPD.IHTTPSession session, String key, boolean def) {
        String value = getParam(session, key);
        if (StringUtil.isNull(value)) {
            return def;
        }
        String v = value.trim().toLowerCase();
        if ("1".equals(v) || "true".equals(v) || "yes".equals(v) || "on".equals(v)) {
            return true;
        }
        if ("0".equals(v) || "false".equals(v) || "no".equals(v) || "off".equals(v)) {
            return false;
        }
        return def;
    }

    private List<String> splitKeywords(String query) {
        List<String> result = new ArrayList<>();
        if (StringUtil.isNull(query)) {
            return result;
        }
        String[] parts = query.split("[,;\\r\\n]+");
        for (String part : parts) {
            if (StringUtil.isNull(part)) {
                continue;
            }
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private List<String> normalizeKeywords(List<String> keywords, boolean caseSensitive) {
        if (caseSensitive) {
            return keywords;
        }
        List<String> out = new ArrayList<>();
        for (String k : keywords) {
            out.add(k.toLowerCase());
        }
        return out;
    }

    private boolean matchesKeywords(String target, List<String> keywords, String mode) {
        if (keywords == null || keywords.isEmpty()) {
            return false;
        }
        boolean isAnd = "and".equalsIgnoreCase(mode);
        if (isAnd) {
            for (String k : keywords) {
                if (target.indexOf(k) < 0) {
                    return false;
                }
            }
            return true;
        }
        for (String k : keywords) {
            if (target.indexOf(k) >= 0) {
                return true;
            }
        }
        return false;
    }

}
