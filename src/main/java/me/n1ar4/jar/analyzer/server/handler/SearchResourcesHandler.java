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

import com.alibaba.fastjson2.JSON;
import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.jar.analyzer.entity.ResourceSearchResult;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.server.handler.base.BaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.IOUtils;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SearchResourcesHandler extends BaseHandler implements HttpHandler {
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

        Integer jarId = getIntParam(session, "jarId");
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

        String queryLower = query.toLowerCase();
        ArrayList<ResourceEntity> resources = engine.getTextResources(jarId);
        List<ResourceSearchResult> results = new ArrayList<>();
        for (ResourceEntity resource : resources) {
            if (results.size() >= limit) {
                break;
            }
            String path = resource.getResourcePath();
            if (!StringUtil.isNull(path) && path.toLowerCase().contains(queryLower)) {
                results.add(buildResult(resource, "path", buildPreview(path, query, queryLower)));
                continue;
            }
            Path filePath = Paths.get(resource.getPathStr());
            if (!Files.exists(filePath)) {
                continue;
            }
            String content = readText(filePath, maxBytes);
            if (StringUtil.isNull(content)) {
                continue;
            }
            String contentLower = content.toLowerCase();
            int idx = contentLower.indexOf(queryLower);
            if (idx >= 0) {
                results.add(buildResult(resource, "content", snippet(content, idx, query.length())));
            }
        }

        String json = JSON.toJSONString(results);
        return buildJSON(json);
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

    private String buildPreview(String content, String query, String queryLower) {
        if (StringUtil.isNull(content)) {
            return "";
        }
        String contentLower = content.toLowerCase();
        int idx = contentLower.indexOf(queryLower);
        if (idx < 0) {
            return "";
        }
        return snippet(content, idx, query.length());
    }

    private String getParam(NanoHTTPD.IHTTPSession session, String key) {
        List<String> data = session.getParameters().get(key);
        if (data == null || data.isEmpty()) {
            return "";
        }
        return data.get(0);
    }

    private Integer getIntParam(NanoHTTPD.IHTTPSession session, String key) {
        String value = getParam(session, key);
        if (StringUtil.isNull(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private int getIntParam(NanoHTTPD.IHTTPSession session, String key, int def) {
        String value = getParam(session, key);
        if (StringUtil.isNull(value)) {
            return def;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return def;
        }
    }
}
