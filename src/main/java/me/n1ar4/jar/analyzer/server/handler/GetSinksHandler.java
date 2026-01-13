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
import me.n1ar4.jar.analyzer.chains.ChainsBuilder;
import me.n1ar4.jar.analyzer.chains.SinkModel;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.server.handler.base.BaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetSinksHandler extends BaseHandler implements HttpHandler {       
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = MainForm.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        List<SinkModel> sinks = new ArrayList<>(ChainsBuilder.sinkData.values());
        String category = getParam(session, "category");
        String keyword = getParam(session, "keyword");
        int offset = getIntParam(session, "offset", 0);
        int limit = getIntParam(session, "limit", 0);

        boolean usePaging = offset > 0 || limit > 0
                || !StringUtil.isNull(category) || !StringUtil.isNull(keyword);
        if (!usePaging) {
            return needParam("category/keyword/offset/limit");
        }

        List<SinkModel> filtered = new ArrayList<>();
        for (SinkModel sink : sinks) {
            if (!StringUtil.isNull(category)) {
                if (StringUtil.isNull(sink.getCategory())
                        || !sink.getCategory().trim().equalsIgnoreCase(category.trim())) {
                    continue;
                }
            }
            if (!StringUtil.isNull(keyword)) {
                String kw = keyword.trim().toLowerCase();
                if (!matchKeyword(sink, kw)) {
                    continue;
                }
            }
            filtered.add(sink);
        }

        if (!usePaging) {
            String json = JSON.toJSONString(filtered);
            return buildJSON(json);
        }

        int total = filtered.size();
        if (offset < 0) {
            offset = 0;
        }
        if (limit <= 0) {
            limit = total;
        }
        int from = Math.min(offset, total);
        int to = Math.min(from + limit, total);
        List<SinkModel> items = filtered.subList(from, to);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("offset", offset);
        result.put("limit", limit);
        result.put("items", items);
        String json = JSON.toJSONString(result);
        return buildJSON(json);
    }

    private boolean matchKeyword(SinkModel sink, String keywordLower) {
        if (!StringUtil.isNull(sink.getBoxName())
                && sink.getBoxName().toLowerCase().contains(keywordLower)) {
            return true;
        }
        if (!StringUtil.isNull(sink.getClassName())
                && sink.getClassName().toLowerCase().contains(keywordLower)) {
            return true;
        }
        if (!StringUtil.isNull(sink.getMethodName())
                && sink.getMethodName().toLowerCase().contains(keywordLower)) {
            return true;
        }
        if (!StringUtil.isNull(sink.getMethodDesc())
                && sink.getMethodDesc().toLowerCase().contains(keywordLower)) {
            return true;
        }
        if (sink.getTags() != null) {
            for (String tag : sink.getTags()) {
                if (!StringUtil.isNull(tag) && tag.toLowerCase().contains(keywordLower)) {
                    return true;
                }
            }
        }
        return false;
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
