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
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.model.ClassView;
import me.n1ar4.jar.analyzer.entity.ConfigItem;
import me.n1ar4.jar.analyzer.entity.ConfigUsageResult;
import me.n1ar4.jar.analyzer.engine.model.CallEdgeView;
import me.n1ar4.jar.analyzer.engine.model.MethodView;
import me.n1ar4.jar.analyzer.core.facts.ResourceEntity;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.server.handler.api.ApiBaseHandler;
import me.n1ar4.jar.analyzer.server.handler.base.HttpHandler;
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil;
import me.n1ar4.jar.analyzer.utils.ConfigParseUtil;
import me.n1ar4.jar.analyzer.utils.SemanticHintUtil;
import me.n1ar4.jar.analyzer.utils.StringUtil;

import java.util.*;

public class GetConfigUsageHandler extends ApiBaseHandler implements HttpHandler {
    private static final int DEFAULT_MAX_KEYS = 15;
    private static final int MAX_MAX_KEYS = 60;
    private static final int DEFAULT_MAX_PER_KEY = 20;
    private static final int MAX_MAX_PER_KEY = 100;
    private static final int DEFAULT_MAX_RESOURCES = 50;
    private static final int MAX_MAX_RESOURCES = 200;
    private static final int DEFAULT_MAX_BYTES = 512 * 1024;
    private static final int MAX_MAX_BYTES = 1024 * 1024;
    private static final int DEFAULT_MAX_DEPTH = 2;
    private static final int MAX_MAX_DEPTH = 4;
    private static final int DEFAULT_MAPPING_LIMIT = 5000;
    private static final int MAX_MAPPING_LIMIT = 20000;
    private static final int DEFAULT_MAX_ENTRYPOINTS = 8;
    private static final int CALLER_PAGE_SIZE = 200;

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        CoreEngine engine = requireReadyEngine();
        if (engine == null) {
            return projectNotReady();
        }
        Integer jarId = getIntParamNullable(session, "jarId");
        int maxKeys = clamp(getIntParam(session, "maxKeys", DEFAULT_MAX_KEYS), 1, MAX_MAX_KEYS);
        int maxPerKey = clamp(getIntParam(session, "maxPerKey", DEFAULT_MAX_PER_KEY), 1, MAX_MAX_PER_KEY);
        int maxResources = clamp(getIntParam(session, "maxResources", DEFAULT_MAX_RESOURCES), 1, MAX_MAX_RESOURCES);
        int maxBytes = clamp(getIntParam(session, "maxBytes", DEFAULT_MAX_BYTES), 4096, MAX_MAX_BYTES);
        int maxDepth = clamp(getIntParam(session, "maxDepth", DEFAULT_MAX_DEPTH), 0, MAX_MAX_DEPTH);
        int mappingLimit = clamp(getIntParam(session, "mappingLimit", DEFAULT_MAPPING_LIMIT), 100, MAX_MAPPING_LIMIT);
        int maxEntryPoints = clamp(getIntParam(session, "maxEntry", DEFAULT_MAX_ENTRYPOINTS), 1, 20);
        boolean maskValue = getBoolParam(session, "mask", true);
        boolean includeResources = getBoolParam(session, "includeResources", true);
        boolean includeJdk = includeJdk(session);

        List<String> keys = parseKeys(getParam(session, "keys"));
        if (keys.isEmpty()) {
            keys = parseKeys(getParam(session, "items"));
        }

        LinkedHashMap<String, List<ConfigItem>> configMap = new LinkedHashMap<>();
        if (keys.isEmpty() || includeResources) {
            List<ResourceEntity> resources = engine.getTextResources(jarId);
            for (ResourceEntity resource : resources) {
                if (!ConfigParseUtil.isConfigResource(resource)) {
                    continue;
                }
                List<ConfigItem> items = ConfigParseUtil.parse(resource, maxBytes, maxResources, maskValue);
                for (ConfigItem item : items) {
                    String key = item.getKey();
                    if (StringUtil.isNull(key)) {
                        continue;
                    }
                    if (!keys.isEmpty() && !keys.contains(key)) {
                        continue;
                    }
                    configMap.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
                }
            }
        }

        if (keys.isEmpty()) {
            keys = new ArrayList<>(configMap.keySet());
        }

        if (keys.size() > maxKeys) {
            keys = keys.subList(0, maxKeys);
        }

        EntryIndex entryIndex = new EntryIndex(engine, jarId, mappingLimit);

        List<ConfigUsageResult> out = new ArrayList<>();
        for (String key : keys) {
            if (StringUtil.isNull(key)) {
                continue;
            }
            ConfigUsageResult result = new ConfigUsageResult();
            result.setKey(key);
            List<ConfigItem> items = configMap.getOrDefault(key, Collections.emptyList());
            if (items.size() > maxResources) {
                items = items.subList(0, maxResources);
            }
            result.setItems(items);
            result.setResourceCount(items.size());

            ArrayList<MethodView> methods = engine.getMethodsByStr(key, jarId, null, maxPerKey, "auto");
            methods = new ArrayList<>(filterMethods(methods, includeJdk));
            result.setMethodCount(methods.size());

            List<ConfigUsageResult.Usage> usages = new ArrayList<>();
            for (MethodView method : methods) {
                ConfigUsageResult.Usage usage = new ConfigUsageResult.Usage();
                usage.setMethod(method);

                List<String> annos = new ArrayList<>();
                annos.addAll(engine.getAnnoByClassName(method.getClassName()));
                annos.addAll(engine.getAnnoByClassAndMethod(method.getClassName(), method.getMethodName()));
                usage.setAnnoNames(annos);
                usage.setSemanticTags(SemanticHintUtil.resolveTags(annos));
                usage.setEntrypoints(entryIndex.resolve(method, maxDepth, maxEntryPoints));

                usages.add(usage);
            }
            result.setUsages(usages);
            out.add(result);
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("count", out.size());
        return ok(out, meta);
    }

    private List<String> parseKeys(String raw) {
        if (StringUtil.isNull(raw)) {
            return new ArrayList<>();
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            if (text.startsWith("[") && text.endsWith("]")) {
                List<String> items = JSON.parseObject(text, new TypeReference<List<String>>() {
                });
                return uniqueKeys(items);
            }
        } catch (Exception ex) {
            logger.debug("parse keys json failed: {}", ex.toString());
        }
        String[] parts = text.split("[,;\\r\\n]+");
        List<String> out = new ArrayList<>();
        for (String part : parts) {
            if (StringUtil.isNull(part)) {
                continue;
            }
            String v = part.trim();
            if (!v.isEmpty()) {
                out.add(v);
            }
        }
        return uniqueKeys(out);
    }

    private List<String> uniqueKeys(List<String> input) {
        if (input == null) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String s : input) {
            if (!StringUtil.isNull(s)) {
                set.add(s.trim());
            }
        }
        return new ArrayList<>(set);
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

    private int clamp(int v, int min, int max) {
        if (v < min) {
            return min;
        }
        if (v > max) {
            return max;
        }
        return v;
    }

    private static class EntryIndex {
        private final Map<String, MethodView> springMap = new HashMap<>();
        private final Set<String> servletClasses = new HashSet<>();
        private final Set<String> filterClasses = new HashSet<>();
        private final Set<String> listenerClasses = new HashSet<>();
        private final CoreEngine engine;

        private static final Set<String> SERVLET_METHODS = new HashSet<>(Arrays.asList(
                "doGet", "doPost", "doPut", "doDelete", "doPatch", "doHead", "doOptions", "service"));
        private static final Set<String> FILTER_METHODS = new HashSet<>(Collections.singletonList("doFilter"));
        private static final Set<String> LISTENER_METHODS = new HashSet<>(Arrays.asList(
                "contextInitialized", "contextDestroyed", "requestInitialized", "requestDestroyed",
                "sessionCreated", "sessionDestroyed"));

        EntryIndex(CoreEngine engine, Integer jarId, int mappingLimit) {
            this.engine = engine;
            List<MethodView> mappings = engine.getSpringMappingsAll(jarId, null, 0, mappingLimit);
            for (MethodView m : mappings) {
                springMap.put(methodKey(m.getClassName(), m.getMethodName(), m.getMethodDesc(), m.getJarId()), m);
            }
            for (ClassView c : engine.getAllServlets()) {
                if (!CommonFilterUtil.isFilteredClass(c.getClassName())) {
                    servletClasses.add(c.getClassName());
                }
            }
            for (ClassView c : engine.getAllFilters()) {
                if (!CommonFilterUtil.isFilteredClass(c.getClassName())) {
                    filterClasses.add(c.getClassName());
                }
            }
            for (ClassView c : engine.getAllListeners()) {
                if (!CommonFilterUtil.isFilteredClass(c.getClassName())) {
                    listenerClasses.add(c.getClassName());
                }
            }
        }

        List<ConfigUsageResult.EntryPoint> resolve(MethodView method, int maxDepth, int maxEntrypoints) {
            List<ConfigUsageResult.EntryPoint> out = new ArrayList<>();
            if (method == null) {
                return out;
            }
            MethodKey seed = new MethodKey(method.getClassName(), method.getMethodName(), method.getMethodDesc(), method.getJarId());
            addIfEntryPoint(method, seed, 0, Collections.singletonList(toTrace(method)), out);
            if (!out.isEmpty() || maxDepth <= 0) {
                return out;
            }
            Deque<TraceNode> queue = new ArrayDeque<>();
            queue.add(new TraceNode(seed, Collections.singletonList(toTrace(method))));
            Set<String> visited = new HashSet<>();
            visited.add(seed.key());
            int depth = 0;
            while (!queue.isEmpty() && depth < maxDepth && out.size() < maxEntrypoints) {
                int size = queue.size();
                depth++;
                for (int i = 0; i < size; i++) {
                    TraceNode node = queue.poll();
                    if (node == null) {
                        continue;
                    }
                    List<CallEdgeView> edges = loadCallerEdges(node.method);
                    for (CallEdgeView edge : edges) {
                        MethodKey caller = new MethodKey(edge.getCallerClassName(),
                                edge.getCallerMethodName(),
                                edge.getCallerMethodDesc(),
                                edge.getCallerJarId() == null ? 0 : edge.getCallerJarId());
                        if (!visited.add(caller.key())) {
                            continue;
                        }
                        if (CommonFilterUtil.isFilteredClass(caller.className)
                                || CommonFilterUtil.isFilteredJar(edge.getCallerJarName())) {
                            continue;
                        }
                        List<ConfigUsageResult.MethodTrace> trace = new ArrayList<>();
                        trace.add(new ConfigUsageResult.MethodTrace(
                                caller.className, caller.methodName, caller.methodDesc));
                        trace.addAll(node.trace);
                        MethodView callerMethod = toMethod(edge);
                        if (addIfEntryPoint(callerMethod, caller, depth, trace, out)) {
                            if (out.size() >= maxEntrypoints) {
                                break;
                            }
                        } else if (depth < maxDepth) {
                            queue.add(new TraceNode(caller, trace));
                        }
                    }
                    if (out.size() >= maxEntrypoints) {
                        break;
                    }
                }
            }
            return out;
        }

        private List<CallEdgeView> loadCallerEdges(MethodKey method) {
            List<CallEdgeView> out = new ArrayList<>();
            if (method == null) {
                return out;
            }
            int offset = 0;
            while (true) {
                List<CallEdgeView> batch = engine.getCallEdgesByCallee(
                        method.className,
                        method.methodName,
                        method.methodDesc,
                        method.jarId,
                        offset,
                        CALLER_PAGE_SIZE
                );
                if (batch == null || batch.isEmpty()) {
                    break;
                }
                out.addAll(batch);
                if (batch.size() < CALLER_PAGE_SIZE) {
                    break;
                }
                offset += batch.size();
            }
            return out;
        }

        private boolean addIfEntryPoint(MethodView method,
                                        MethodKey key,
                                        int depth,
                                        List<ConfigUsageResult.MethodTrace> trace,
                                        List<ConfigUsageResult.EntryPoint> out) {
            String type = entryType(method);
            if (type == null) {
                return false;
            }
            ConfigUsageResult.EntryPoint ep = new ConfigUsageResult.EntryPoint();
            ep.setType(type);
            ep.setClassName(method.getClassName());
            ep.setMethodName(method.getMethodName());
            ep.setMethodDesc(method.getMethodDesc());
            ep.setJarId(method.getJarId());
            ep.setJarName(method.getJarName());
            if ("spring".equals(type)) {
                MethodView mapping = springMap.get(key.key());
                if (mapping != null) {
                    ep.setPath(mapping.getActualPath());
                    ep.setRestfulType(mapping.getRestfulType());
                    ep.setJarId(mapping.getJarId());
                    ep.setJarName(mapping.getJarName());
                }
            }
            ep.setDepth(depth);
            ep.setTrace(trace == null ? new ArrayList<>() : trace);
            out.add(ep);
            return true;
        }

        private String entryType(MethodView method) {
            if (method == null) {
                return null;
            }
            String key = methodKey(method.getClassName(), method.getMethodName(), method.getMethodDesc(), method.getJarId());
            if (springMap.containsKey(key)) {
                return "spring";
            }
            if (servletClasses.contains(method.getClassName())
                    && SERVLET_METHODS.contains(method.getMethodName())) {
                return "servlet";
            }
            if (filterClasses.contains(method.getClassName())
                    && FILTER_METHODS.contains(method.getMethodName())) {
                return "filter";
            }
            if (listenerClasses.contains(method.getClassName())
                    && LISTENER_METHODS.contains(method.getMethodName())) {
                return "listener";
            }
            return null;
        }

        private MethodView toMethod(CallEdgeView edge) {
            MethodView m = new MethodView();
            m.setClassName(edge.getCallerClassName());
            m.setMethodName(edge.getCallerMethodName());
            m.setMethodDesc(edge.getCallerMethodDesc());
            m.setJarId(edge.getCallerJarId() == null ? 0 : edge.getCallerJarId());
            m.setJarName(edge.getCallerJarName());
            return m;
        }

        private ConfigUsageResult.MethodTrace toTrace(MethodView m) {
            return new ConfigUsageResult.MethodTrace(m.getClassName(), m.getMethodName(), m.getMethodDesc());
        }

        private String methodKey(String cls, String name, String desc, int jarId) {
            return (cls == null ? "" : cls) + "#" +
                    (name == null ? "" : name) + "#" +
                    (desc == null ? "" : desc) + "#" + jarId;
        }
    }

    private static class MethodKey {
        private final String className;
        private final String methodName;
        private final String methodDesc;
        private final int jarId;

        MethodKey(String className, String methodName, String methodDesc, int jarId) {
            this.className = className;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
            this.jarId = jarId;
        }

        String key() {
            return (className == null ? "" : className) + "#" +
                    (methodName == null ? "" : methodName) + "#" +
                    (methodDesc == null ? "" : methodDesc) + "#" + jarId;
        }
    }

    private static class TraceNode {
        private final MethodKey method;
        private final List<ConfigUsageResult.MethodTrace> trace;

        TraceNode(MethodKey method, List<ConfigUsageResult.MethodTrace> trace) {
            this.method = method;
            this.trace = trace;
        }
    }
}
