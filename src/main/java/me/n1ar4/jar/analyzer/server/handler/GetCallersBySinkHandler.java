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
import me.n1ar4.jar.analyzer.server.handler.base.BaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.*;

public class GetCallersBySinkHandler extends BaseHandler implements HttpHandler {
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
        List<Map<String, Object>> items = new ArrayList<>();
        for (SinkModel sink : sinks) {
            if (sink == null || StringUtil.isNull(sink.getClassName())
                    || StringUtil.isNull(sink.getMethodName())) {
                continue;
            }
            ArrayList<MethodResult> callers =
                    engine.getCallers(sink.getClassName(), sink.getMethodName(), sink.getMethodDesc());
            if (limit > 0 && callers.size() > limit) {
                callers = new ArrayList<>(callers.subList(0, limit));
            }
            Map<String, Object> item = new HashMap<>();
            item.put("sink", sink);
            item.put("count", callers.size());
            item.put("results", callers);
            items.add(item);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("items", items);
        out.put("total", items.size());
        String json = JSON.toJSONString(out);
        return buildJSON(json);
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
                SinkModel model = ChainsBuilder.sinkData.get(key);
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
                SinkModel model = ChainsBuilder.sinkData.get(name);
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

    private String getParam(NanoHTTPD.IHTTPSession session, String key) {
        List<String> data = session.getParameters().get(key);
        if (data == null || data.isEmpty()) {
            return "";
        }
        return data.get(0);
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
}
