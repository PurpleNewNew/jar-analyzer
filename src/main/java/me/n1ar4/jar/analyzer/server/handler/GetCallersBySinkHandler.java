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
import com.alibaba.fastjson2.TypeReference;
import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.rules.SinkModel;
import me.n1ar4.jar.analyzer.rules.SinkRuleRegistry;
import me.n1ar4.jar.analyzer.server.handler.api.ApiBaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.*;

public class GetCallersBySinkHandler extends ApiBaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = requireReadyEngine();
        if (engine == null) {
            return projectNotReady();
        }
        List<SinkModel> sinks = parseSinks(session);
        if (sinks.isEmpty()) {
            return needParam("sinkName/sinkClass/items");
        }
        int limit = getIntParam(session, "limit", 0);
        boolean includeJdk = includeJdk(session);
        List<Map<String, Object>> items = new ArrayList<>();
        for (SinkModel sink : sinks) {
            if (sink == null || StringUtil.isNull(sink.getClassName())
                    || StringUtil.isNull(sink.getMethodName())) {
                continue;
            }
            List<MethodReference> resolvedSinks = resolveSinkMethods(engine, sink);
            if (resolvedSinks.isEmpty()) {
                Map<String, Object> item = new HashMap<>();
                item.put("sink", buildSinkPayload(sink, null));
                item.put("count", 0);
                item.put("results", List.of());
                items.add(item);
                continue;
            }
            for (MethodReference resolvedSink : resolvedSinks) {
                ArrayList<MethodResult> callers = engine.getCallers(
                        resolvedClassName(resolvedSink),
                        safe(resolvedSink.getName()),
                        safe(resolvedSink.getDesc()),
                        resolvedSink.getJarId()
                );
                callers = new ArrayList<>(filterMethods(callers, includeJdk));
                if (limit > 0 && callers.size() > limit) {
                    callers = new ArrayList<>(callers.subList(0, limit));
                }
                Map<String, Object> item = new HashMap<>();
                item.put("sink", buildSinkPayload(sink, resolvedSink));
                item.put("count", callers.size());
                item.put("results", callers);
                items.add(item);
            }
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("count", items.size());
        return ok(items, meta);
    }

    private List<MethodReference> resolveSinkMethods(CoreEngine engine, SinkModel sink) {
        if (engine == null || sink == null) {
            return Collections.emptyList();
        }
        String methodDesc = normalizeDescForQuery(sink.getMethodDesc());
        ArrayList<MethodReference> matches = engine.getMethodReferences(
                sink.getClassName(),
                sink.getMethodName(),
                methodDesc
        );
        if (matches == null || matches.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashMap<String, MethodReference> dedup = new LinkedHashMap<>();
        for (MethodReference match : matches) {
            if (match == null) {
                continue;
            }
            String key = resolvedClassName(match) + "#" + safe(match.getName()) + "#"
                    + safe(match.getDesc()) + "#" + match.getJarId();
            dedup.putIfAbsent(key, match);
        }
        return dedup.isEmpty() ? Collections.emptyList() : new ArrayList<>(dedup.values());
    }

    private Map<String, Object> buildSinkPayload(SinkModel sink, MethodReference resolvedSink) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("boxName", sink == null ? null : sink.getBoxName());
        payload.put("className", resolvedSink == null ? safe(sink == null ? null : sink.getClassName()) : resolvedClassName(resolvedSink));
        payload.put("methodName", resolvedSink == null ? safe(sink == null ? null : sink.getMethodName()) : safe(resolvedSink.getName()));
        payload.put("methodDesc", resolvedSink == null ? safe(sink == null ? null : sink.getMethodDesc()) : safe(resolvedSink.getDesc()));
        payload.put("category", sink == null ? null : sink.getCategory());
        payload.put("severity", sink == null ? null : sink.getSeverity());
        payload.put("ruleTier", sink == null ? null : sink.getRuleTier());
        payload.put("tags", sink == null ? null : sink.getTags());
        if (resolvedSink != null) {
            payload.put("jarId", resolvedSink.getJarId());
            payload.put("jarName", resolvedSink.getJarName());
        }
        payload.put("resolved", resolvedSink != null);
        return payload;
    }

    private List<SinkModel> parseSinks(NanoHTTPD.IHTTPSession session) {
        String itemsRaw = getParam(session, "items");
        if (!StringUtil.isNull(itemsRaw)) {
            try {
                List<Map<String, Object>> items = JSON.parseObject(
                        itemsRaw, new TypeReference<List<Map<String, Object>>>() {
                        });
                return mapItems(items);
            } catch (Exception ex) {
                logger.debug("invalid sinks items json: {}", ex.toString());
            }
        }

        String sinkName = getParam(session, "sinkName");
        if (!StringUtil.isNull(sinkName)) {
            List<SinkModel> sinks = new ArrayList<>();
            String[] names = sinkName.split(",");
            for (String name : names) {
                if (StringUtil.isNull(name)) {
                    continue;
                }
                String key = name.trim();
                if (key.isEmpty()) {
                    continue;
                }
                SinkModel model = SinkRuleRegistry.findSinkByName(key);
                if (model != null) {
                    sinks.add(model);
                }
            }
            return sinks;
        }

        String sinkClass = getParam(session, "sinkClass");
        String sinkMethod = getParam(session, "sinkMethod");
        String sinkDesc = getParam(session, "sinkDesc");
        if (!StringUtil.isNull(sinkClass) && !StringUtil.isNull(sinkMethod)) {
            SinkModel m = new SinkModel();
            m.setClassName(normalizeValue(sinkClass));
            m.setMethodName(sinkMethod);
            m.setMethodDesc(normalizeValue(sinkDesc));
            List<SinkModel> out = new ArrayList<>();
            out.add(m);
            return out;
        }
        return Collections.emptyList();
    }

    private List<SinkModel> mapItems(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<SinkModel> sinks = new ArrayList<>();
        for (Map<String, Object> item : items) {
            if (item == null) {
                continue;
            }
            String name = asString(item.get("sinkName"));
            if (!StringUtil.isNull(name)) {
                SinkModel model = SinkRuleRegistry.findSinkByName(name);
                if (model != null) {
                    sinks.add(model);
                    continue;
                }
            }
            String cls = normalizeValue(asString(item.get("class")));
            if (StringUtil.isNull(cls)) {
                cls = normalizeValue(asString(item.get("sinkClass")));
            }
            String method = asString(item.get("method"));
            if (StringUtil.isNull(method)) {
                method = asString(item.get("sinkMethod"));
            }
            String desc = normalizeValue(asString(item.get("desc")));
            if (StringUtil.isNull(desc)) {
                desc = normalizeValue(asString(item.get("sinkDesc")));
            }
            if (StringUtil.isNull(cls) || StringUtil.isNull(method)) {
                continue;
            }
            SinkModel m = new SinkModel();
            m.setClassName(cls);
            m.setMethodName(method);
            m.setMethodDesc(desc);
            sinks.add(m);
        }
        return sinks;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String normalizeValue(String value) {
        if (StringUtil.isNull(value)) {
            return null;
        }
        String v = value.trim();
        if (v.isEmpty() || "null".equalsIgnoreCase(v)) {
            return null;
        }
        return v.replace('.', '/');
    }

    private String normalizeDescForQuery(String desc) {
        if (desc == null) {
            return null;
        }
        String v = desc.trim();
        if (v.isEmpty() || "*".equals(v) || "null".equalsIgnoreCase(v)) {
            return null;
        }
        return desc;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String resolvedClassName(MethodReference reference) {
        if (reference == null || reference.getClassReference() == null) {
            return "";
        }
        return safe(reference.getClassReference().getName());
    }
}
