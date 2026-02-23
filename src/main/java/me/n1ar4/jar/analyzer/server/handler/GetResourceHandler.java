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
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetResourceHandler extends ApiBaseHandler implements HttpHandler {
    private static final int DEFAULT_LIMIT = 256 * 1024;
    private static final int MAX_LIMIT = 1024 * 1024;

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = MainForm.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        Integer rid = getIntParamNullable(session, "id");
        if (rid == null) {
            rid = getIntParamNullable(session, "rid");
        }
        Integer jarId = getIntParamNullable(session, "jarId");
        String path = getParam(session, "path");
        ResourceEntity resource;
        if (rid != null) {
            resource = engine.getResourceById(rid);
        } else if (!StringUtil.isNull(path)) {
            if (jarId == null) {
                List<ResourceEntity> candidates = engine.getResourcesByPath(path, 20);
                if (candidates == null || candidates.isEmpty()) {
                    return notFound();
                }
                if (candidates.size() == 1) {
                    resource = candidates.get(0);
                } else {
                    List<Map<String, Object>> items = new ArrayList<>();
                    for (ResourceEntity c : candidates) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("rid", c.getRid());
                        item.put("jarId", c.getJarId());
                        item.put("jarName", c.getJarName());
                        item.put("resourcePath", c.getResourcePath());
                        item.put("fileSize", c.getFileSize());
                        item.put("isText", c.getIsText());
                        items.add(item);
                    }
                    Map<String, Object> result = new HashMap<>();
                    result.put("path", path);
                    result.put("needJarId", true);
                    result.put("candidateCount", items.size());
                    result.put("candidates", items);
                    return ok(result);
                }
            } else {
                resource = engine.getResourceByPath(jarId, path);
            }
        } else {
            return needParam("id|path");
        }
        if (resource == null) {
            return notFound();
        }

        Path filePath = PathResolver.resolveResourceFile(resource);
        if (filePath == null || !Files.exists(filePath)) {
            return notFound();
        }

        int offset = getIntParam(session, "offset", 0);
        int limit = getIntParam(session, "limit", DEFAULT_LIMIT);
        if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }
        if (limit < 1) {
            limit = DEFAULT_LIMIT;
        }
        if (offset < 0) {
            offset = 0;
        }
        boolean forceBase64 = getBoolParam(session, "base64");

        byte[] data;
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            skipFully(inputStream, offset);
            data = IOUtils.readNBytes(inputStream, limit);
        } catch (Exception e) {
            return error();
        }

        boolean isText = resource.getIsText() == 1;
        String content;
        String encoding;
        if (isText && !forceBase64) {
            content = new String(data, StandardCharsets.UTF_8);
            encoding = "utf-8";
        } else {
            content = Base64.getEncoder().encodeToString(data);
            encoding = "base64";
        }

        long fileSize = resource.getFileSize();
        boolean truncated = fileSize > 0 && (offset + data.length) < fileSize;

        Map<String, Object> result = new HashMap<>();
        result.put("jarId", resource.getJarId());
        result.put("jarName", resource.getJarName());
        result.put("resourcePath", resource.getResourcePath());
        result.put("fileSize", fileSize);
        result.put("isText", resource.getIsText());
        result.put("offset", offset);
        result.put("limit", limit);
        result.put("truncated", truncated);
        result.put("encoding", encoding);
        result.put("content", content);

        return ok(result);
    }

    private void skipFully(InputStream inputStream, long bytes) throws Exception {
        long remaining = bytes;
        while (remaining > 0) {
            long skipped = inputStream.skip(remaining);
            if (skipped <= 0) {
                if (inputStream.read() == -1) {
                    break;
                }
                remaining--;
            } else {
                remaining -= skipped;
            }
        }
    }

    private NanoHTTPD.Response notFound() {
        return buildError(
                NanoHTTPD.Response.Status.NOT_FOUND,
                "resource_not_found",
                "resource not found");
    }
}
