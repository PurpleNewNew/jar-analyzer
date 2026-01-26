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
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class GetResourcesHandler extends ApiBaseHandler implements HttpHandler {
    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 2000;

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = MainForm.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        String path = getParam(session, "path");
        if (StringUtil.isNull(path)) {
            path = "";
        }
        Integer jarId = getIntParamNullable(session, "jarId");
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
        ArrayList<ResourceEntity> res = engine.getResources(path, jarId, offset, limit);
        ArrayList<ResourceEntity> safe = new ArrayList<>();
        for (ResourceEntity resource : res) {
            if (resource == null) {
                continue;
            }
            safe.add(toPublicResource(resource));
        }
        return ok(safe, pageMeta(offset, limit, safe.size(), null));
    }

    private ResourceEntity toPublicResource(ResourceEntity resource) {
        ResourceEntity out = new ResourceEntity();
        out.setRid(resource.getRid());
        out.setJarId(resource.getJarId());
        out.setJarName(resource.getJarName());
        out.setResourcePath(resource.getResourcePath());
        out.setPathStr(resource.getResourcePath());
        out.setFileSize(resource.getFileSize());
        out.setIsText(resource.getIsText());
        return out;
    }

}
