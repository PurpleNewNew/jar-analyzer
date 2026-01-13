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
import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.DecompileEngine;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.server.handler.base.BaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetCodeBatchHandler extends BaseHandler implements HttpHandler {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = MainForm.getEngine();
        if (engine == null || !engine.isEnabled()) {
            return error();
        }
        String itemsRaw = getParam(session, "items");
        if (StringUtil.isNull(itemsRaw)) {
            return needParam("items");
        }
        List<Map<String, Object>> items;
        try {
            items = JSON.parseObject(itemsRaw, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            return needParam("items");
        }

        boolean includeFullDefault = getBoolParam(session, "includeFull", false);
        String defaultDecompiler = getParam(session, "decompiler");
        if (StringUtil.isNull(defaultDecompiler)) {
            defaultDecompiler = "fernflower";
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Map<String, Object> row = new HashMap<>();
            row.put("request", item);
            String className = normalizeValue(asString(item.get("class")));
            String methodName = asString(item.get("method"));
            String methodDesc = normalizeDesc(asString(item.get("desc")));
            boolean includeFull = getBoolValue(item.get("includeFull"), includeFullDefault);
            String decompiler = asString(item.get("decompiler"));
            if (StringUtil.isNull(decompiler)) {
                decompiler = defaultDecompiler;
            }
            decompiler = decompiler.trim().toLowerCase();

            if (StringUtil.isNull(className) || StringUtil.isNull(methodName)) {
                row.put("success", false);
                row.put("message", "missing class/method");
                out.add(row);
                continue;
            }

            String absPath = engine.getAbsPath(className);
            if (StringUtil.isNull(absPath)) {
                row.put("success", false);
                row.put("message", "class file not found: " + className);
                out.add(row);
                continue;
            }

            String decompiledCode;
            if ("cfr".equalsIgnoreCase(decompiler)) {
                decompiledCode = CFRDecompileEngine.decompile(absPath);
            } else {
                decompiledCode = DecompileEngine.decompile(Paths.get(absPath), true);
            }
            if (StringUtil.isNull(decompiledCode)) {
                row.put("success", false);
                row.put("message", "failed to decompile class: " + className);
                out.add(row);
                continue;
            }

            String methodCode = extractMethodCode(decompiledCode, methodName, methodDesc);
            if (methodCode == null) {
                methodCode = "";
            }
            row.put("success", true);
            row.put("className", className);
            row.put("methodName", methodName);
            row.put("methodDesc", methodDesc);
            row.put("decompiler", decompiler);
            if (includeFull) {
                row.put("fullClassCode", decompiledCode);
            }
            row.put("methodCode", methodCode);
            out.add(row);
        }

        String json = JSON.toJSONString(out);
        return buildJSON(json);
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

    private boolean getBoolValue(Object value, boolean def) {
        if (value == null) {
            return def;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String v = String.valueOf(value).trim().toLowerCase();
        if ("1".equals(v) || "true".equals(v) || "yes".equals(v) || "on".equals(v)) {
            return true;
        }
        if ("0".equals(v) || "false".equals(v) || "no".equals(v) || "off".equals(v)) {
            return false;
        }
        return def;
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

    private String normalizeDesc(String value) {
        if (StringUtil.isNull(value)) {
            return null;
        }
        String v = value.trim();
        if (v.isEmpty() || "null".equalsIgnoreCase(v)) {
            return null;
        }
        return v;
    }
}
