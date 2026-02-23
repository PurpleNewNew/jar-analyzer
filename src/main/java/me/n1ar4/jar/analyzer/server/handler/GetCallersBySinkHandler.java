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
import me.n1ar4.jar.analyzer.chains.ChainsBuilder;
import me.n1ar4.jar.analyzer.chains.SinkModel;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.server.handler.api.ApiBaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.*;

public class GetCallersBySinkHandler extends ApiBaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = MainForm.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        List<SinkModel> sinks = parseSinks(session);
        if (sinks.isEmpty()) {
            return needParam("sinkName/sinkClass/items");
        }
        int limit = getIntParam(session, "limit", 0);
        String scope = normalizeScope(getParam(session, "scope"));
        List<Map<String, Object>> items = new ArrayList<>();
        for (SinkModel sink : sinks) {
            if (sink == null || StringUtil.isNull(sink.getClassName())
                    || StringUtil.isNull(sink.getMethodName())) {
                continue;
            }
            String methodDesc = normalizeDescForQuery(sink.getMethodDesc());
            ArrayList<MethodResult> callers =
                    engine.getCallers(sink.getClassName(), sink.getMethodName(), methodDesc);
            callers = new ArrayList<>(filterByScope(engine, callers, scope));
            if (limit > 0 && callers.size() > limit) {
                callers = new ArrayList<>(callers.subList(0, limit));
            }
            Map<String, Object> item = new HashMap<>();
            item.put("sink", sink);
            item.put("count", callers.size());
            item.put("results", callers);
            items.add(item);
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("count", items.size());
        return ok(items, meta);
    }

    private List<SinkModel> parseSinks(NanoHTTPD.IHTTPSession session) {
        String itemsRaw = getParam(session, "items");
        if (!StringUtil.isNull(itemsRaw)) {
            try {
                List<Map<String, Object>> items = JSON.parseObject(
                        itemsRaw, new TypeReference<List<Map<String, Object>>>() {
                        });
                return mapItems(items);
            } catch (Exception ignored) {
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
                SinkModel model = ChainsBuilder.getSinkByName(key);
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
                SinkModel model = ChainsBuilder.getSinkByName(name);
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

    private String normalizeScope(String scope) {
        if (StringUtil.isNull(scope)) {
            return "app";
        }
        String value = scope.trim().toLowerCase();
        if ("all".equals(value)) {
            return "all";
        }
        return "app";
    }

    private List<MethodResult> filterByScope(CoreEngine engine,
                                             List<MethodResult> input,
                                             String scope) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        if ("all".equals(scope)) {
            return input;
        }
        List<MethodResult> out = new ArrayList<>();
        for (MethodResult item : input) {
            if (item == null) {
                continue;
            }
            String role = engine.getClassRole(item.getClassName(), item.getJarId());
            if (!"APP".equalsIgnoreCase(role)) {
                continue;
            }
            out.add(item);
        }
        return out;
    }
}
