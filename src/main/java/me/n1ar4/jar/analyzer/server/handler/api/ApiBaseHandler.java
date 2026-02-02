/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.server.handler.api;

import com.alibaba.fastjson2.JSON;
import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.jar.analyzer.entity.AnnoMethodResult;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.entity.MethodCallResult;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.server.handler.base.BaseHandler;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApiBaseHandler extends BaseHandler {
    protected NanoHTTPD.Response ok(Object data) {
        return ok(data, null, null);
    }

    protected NanoHTTPD.Response ok(Object data, Map<String, Object> meta) {
        return ok(data, meta, null);
    }

    protected NanoHTTPD.Response ok(Object data, Map<String, Object> meta, List<String> warnings) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("data", data);
        if (meta != null && !meta.isEmpty()) {
            out.put("meta", meta);
        }
        if (warnings != null && !warnings.isEmpty()) {
            out.put("warnings", warnings);
        }
        return buildJSON(JSON.toJSONString(out));
    }

    protected Map<String, Object> pageMeta(int offset, int limit, int count, Integer total) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("offset", offset);
        meta.put("limit", limit);
        meta.put("count", count);
        if (total != null) {
            meta.put("total", total);
        }
        meta.put("truncated", limit > 0 && count >= limit);
        return meta;
    }

    protected int getIntParam(NanoHTTPD.IHTTPSession session, String key, int def) {
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

    protected Integer getIntParamNullable(NanoHTTPD.IHTTPSession session, String key) {
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

    protected boolean getBoolParam(NanoHTTPD.IHTTPSession session, String key) {
        String value = getParam(session, key);
        if (StringUtil.isNull(value)) {
            return false;
        }
        String v = value.trim().toLowerCase();
        return "1".equals(v) || "true".equals(v) || "yes".equals(v) || "on".equals(v);
    }

    protected String getStringParam(NanoHTTPD.IHTTPSession session, String... keys) {
        if (keys == null) {
            return "";
        }
        for (String key : keys) {
            if (key == null || key.trim().isEmpty()) {
                continue;
            }
            String value = getParam(session, key);
            if (!StringUtil.isNull(value)) {
                return value;
            }
        }
        return "";
    }

    protected String getClassParam(NanoHTTPD.IHTTPSession session) {
        String value = getStringParam(session, "class", "className", "clazz", "cls");
        if (StringUtil.isNull(value)) {
            return "";
        }
        return normalizeClassName(value);
    }

    protected String normalizeClassName(String value) {
        if (StringUtil.isNull(value)) {
            return "";
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return "";
        }
        return v.replace('.', '/');
    }

    protected List<String> splitListParam(String raw) {
        List<String> out = new ArrayList<>();
        if (StringUtil.isNull(raw)) {
            return out;
        }
        String[] parts = raw.split("[\\n,]");
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            String v = part.trim();
            if (!v.isEmpty()) {
                out.add(v);
            }
        }
        return out;
    }

    protected boolean includeJdk(NanoHTTPD.IHTTPSession session) {
        String scope = getParam(session, "scope");
        if (!StringUtil.isNull(scope)) {
            String v = scope.trim().toLowerCase();
            if ("all".equals(v) || "full".equals(v) || "jdk".equals(v)) {
                return true;
            }
            return false;
        }
        String include = getParam(session, "includeJdk");
        if (!StringUtil.isNull(include)) {
            String v = include.trim().toLowerCase();
            return "1".equals(v) || "true".equals(v) || "yes".equals(v) || "on".equals(v);
        }
        return false;
    }

    protected <T> List<T> applyLimitOffset(List<T> items, int offset, int limit) {
        if (items == null) {
            return new ArrayList<>();
        }
        int size = items.size();
        int start = Math.max(0, offset);
        if (start >= size) {
            return new ArrayList<>();
        }
        int end;
        if (limit <= 0) {
            end = size;
        } else {
            end = Math.min(size, start + limit);
        }
        return new ArrayList<>(items.subList(start, end));
    }

    protected List<MethodResult> filterMethods(List<MethodResult> results, boolean includeJdk) {
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }
        if (includeJdk) {
            List<MethodResult> out = new ArrayList<>();
            for (MethodResult r : results) {
                if (r == null) {
                    continue;
                }
                if (CommonFilterUtil.isModuleInfoClassName(r.getClassName())) {
                    continue;
                }
                out.add(r);
            }
            return out;
        }
        return filterJdkMethods(results, null);
    }

    protected List<AnnoMethodResult> filterAnnoMethods(List<AnnoMethodResult> results, boolean includeJdk) {
        List<AnnoMethodResult> out = new ArrayList<>();
        if (results == null || results.isEmpty()) {
            return out;
        }
        if (includeJdk) {
            for (AnnoMethodResult r : results) {
                if (r == null) {
                    continue;
                }
                if (CommonFilterUtil.isModuleInfoClassName(r.getClassName())) {
                    continue;
                }
                out.add(r);
            }
            return out;
        }
        for (AnnoMethodResult r : results) {
            if (r == null) {
                continue;
            }
            String className = r.getClassName();
            String jarName = r.getJarName();
            if (!isJdkClass(className) && !isNoisyJar(jarName)) {
                out.add(r);
            }
        }
        return out;
    }

    protected List<MethodCallResult> filterEdges(List<MethodCallResult> items, boolean includeJdk, boolean byCallee) {
        List<MethodCallResult> out = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return out;
        }
        if (includeJdk) {
            for (MethodCallResult r : items) {
                if (r == null) {
                    continue;
                }
                String className = byCallee ? r.getCallerClassName() : r.getCalleeClassName();
                if (CommonFilterUtil.isModuleInfoClassName(className)) {
                    continue;
                }
                out.add(r);
            }
            return out;
        }
        for (MethodCallResult r : items) {
            if (r == null) {
                continue;
            }
            String className = byCallee ? r.getCallerClassName() : r.getCalleeClassName();
            String jarName = byCallee ? r.getCallerJarName() : r.getCalleeJarName();
            if (!isJdkClass(className) && !isNoisyJar(jarName)) {
                out.add(r);
            }
        }
        return out;
    }

    protected List<ClassResult> filterClasses(List<ClassResult> items, boolean includeJdk) {
        List<ClassResult> out = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return out;
        }
        if (includeJdk) {
            for (ClassResult r : items) {
                if (r == null) {
                    continue;
                }
                if (CommonFilterUtil.isModuleInfoClassName(r.getClassName())) {
                    continue;
                }
                out.add(r);
            }
            return out;
        }
        for (ClassResult r : items) {
            if (r == null) {
                continue;
            }
            String className = r.getClassName();
            String jarName = r.getJarName();
            if (!isJdkClass(className) && !isNoisyJar(jarName)) {
                out.add(r);
            }
        }
        return out;
    }
}
