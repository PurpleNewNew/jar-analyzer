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
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.server.handler.base.BaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 返回项目中真实命中的 SINK 候选列表（基于 model.json）。
 */
public class GetSinkCandidatesHandler extends BaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = EngineContext.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }

        List<SinkModel> sinks = new ArrayList<>(ChainsBuilder.sinkData.values());
        String category = getParam(session, "category");
        String keyword = getParam(session, "keyword");
        int offset = getIntParam(session, "offset", 0);
        int limit = getIntParam(session, "limit", 0);
        int sampleLimit = getIntParam(session, "sampleLimit", 0);

        List<Map<String, Object>> matched = new ArrayList<>();
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

            ArrayList<MethodResult> hits = resolveHits(engine, sink);
            if (hits.isEmpty()) {
                continue;
            }
            hits = filterJdkMethods(hits, session);
            if (hits.isEmpty()) {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sinkName", sink.getBoxName());
            item.put("className", sink.getClassName());
            item.put("methodName", sink.getMethodName());
            item.put("methodDesc", sink.getMethodDesc());
            item.put("category", sink.getCategory());
            item.put("severity", sink.getSeverity());
            item.put("hitCount", hits.size());

            if (sampleLimit > 0) {
                List<Map<String, String>> samples = new ArrayList<>();
                int count = 0;
                for (MethodResult m : hits) {
                    if (count >= sampleLimit) {
                        break;
                    }
                    Map<String, String> sample = new HashMap<>();
                    sample.put("className", m.getClassName());
                    sample.put("methodName", m.getMethodName());
                    sample.put("methodDesc", m.getMethodDesc());
                    sample.put("jarName", m.getJarName());
                    samples.add(sample);
                    count++;
                }
                item.put("samples", samples);
            }

            matched.add(item);
        }

        int total = matched.size();
        if (offset < 0) {
            offset = 0;
        }
        if (limit <= 0) {
            limit = total;
        }
        int from = Math.min(offset, total);
        int to = Math.min(from + limit, total);
        List<Map<String, Object>> items = matched.subList(from, to);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("offset", offset);
        result.put("limit", limit);
        result.put("items", items);

        String json = JSON.toJSONString(result);
        return buildJSON(json);
    }

    private ArrayList<MethodResult> resolveHits(CoreEngine engine, SinkModel sink) {
        if (engine == null || sink == null) {
            return new ArrayList<>();
        }
        String className = normalizeClass(sink.getClassName());
        String methodName = sink.getMethodName();
        String methodDesc = sink.getMethodDesc();
        if (StringUtil.isNull(className) || StringUtil.isNull(methodName)) {
            return new ArrayList<>();
        }
        if (isAnyDesc(methodDesc)) {
            return engine.getMethodLike(className, methodName, "");
        }
        return engine.getMethod(className, methodName, methodDesc);
    }

    private String normalizeClass(String className) {
        if (StringUtil.isNull(className)) {
            return "";
        }
        return className.trim().replace(".", "/");
    }

    private boolean isAnyDesc(String desc) {
        if (StringUtil.isNull(desc)) {
            return true;
        }
        String v = desc.trim();
        return v.isEmpty() || "*".equals(v) || "null".equalsIgnoreCase(v);
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
        } catch (NumberFormatException ex) {
            logger.debug("invalid int param: {}={}", key, value);
            return def;
        }
    }
}
