/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.engine;

import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.MethodCallResult;
import me.n1ar4.jar.analyzer.entity.MethodResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CallGraphCache {
    private final Map<String, ArrayList<MethodResult>> callersByCalleeScoped;
    private final Map<String, ArrayList<MethodResult>> calleesByCallerScoped;
    private final Map<String, ArrayList<MethodResult>> callersByCallee;
    private final Map<String, ArrayList<MethodResult>> calleesByCaller;
    private final Map<String, ArrayList<MethodResult>> callersByCalleeSimple;
    private final Map<String, ArrayList<MethodResult>> calleesByCallerSimple;
    private final Map<MethodCallKey, MethodCallMeta> edgeMeta;
    private final int edgeCount;
    private final int methodCount;

    private CallGraphCache(Map<String, ArrayList<MethodResult>> callersByCalleeScoped,
                           Map<String, ArrayList<MethodResult>> calleesByCallerScoped,
                           Map<String, ArrayList<MethodResult>> callersByCallee,
                           Map<String, ArrayList<MethodResult>> calleesByCaller,
                           Map<String, ArrayList<MethodResult>> callersByCalleeSimple,
                           Map<String, ArrayList<MethodResult>> calleesByCallerSimple,
                           Map<MethodCallKey, MethodCallMeta> edgeMeta,
                           int edgeCount,
                           int methodCount) {
        this.callersByCalleeScoped = callersByCalleeScoped;
        this.calleesByCallerScoped = calleesByCallerScoped;
        this.callersByCallee = callersByCallee;
        this.calleesByCaller = calleesByCaller;
        this.callersByCalleeSimple = callersByCalleeSimple;
        this.calleesByCallerSimple = calleesByCallerSimple;
        this.edgeMeta = edgeMeta;
        this.edgeCount = edgeCount;
        this.methodCount = methodCount;
    }

    public static CallGraphCache build(List<MethodCallResult> edges) {
        if (edges == null || edges.isEmpty()) {
            return new CallGraphCache(Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap(), 0, 0);
        }
        Map<String, MethodResult> methodCache = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodResult>> callersScopedTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodResult>> calleesScopedTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodResult>> callersTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodResult>> calleesTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodResult>> callersSimpleTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodResult>> calleesSimpleTmp = new HashMap<>();
        Map<MethodCallKey, MethodCallMeta> metaMap = new HashMap<>(edges.size() * 2);
        Set<String> scopedEdgeKeys = new HashSet<>(edges.size() * 2);

        for (MethodCallResult edge : edges) {
            if (edge == null) {
                continue;
            }
            String callerClass = edge.getCallerClassName();
            String callerMethod = edge.getCallerMethodName();
            String callerDesc = edge.getCallerMethodDesc();
            String calleeClass = edge.getCalleeClassName();
            String calleeMethod = edge.getCalleeMethodName();
            String calleeDesc = edge.getCalleeMethodDesc();
            if (callerClass == null || callerMethod == null || callerDesc == null
                    || calleeClass == null || calleeMethod == null || calleeDesc == null) {
                continue;
            }
            MethodResult caller = getOrCreateMethod(methodCache, callerClass, callerMethod, callerDesc,
                    edge.getCallerJarName(), edge.getCallerJarId());
            MethodResult callee = getOrCreateMethod(methodCache, calleeClass, calleeMethod, calleeDesc,
                    edge.getCalleeJarName(), edge.getCalleeJarId());
            String callerKey = methodKey(callerClass, callerMethod, callerDesc);
            String calleeKey = methodKey(calleeClass, calleeMethod, calleeDesc);
            String callerSimpleKey = methodSimpleKey(callerClass, callerMethod);
            String calleeSimpleKey = methodSimpleKey(calleeClass, calleeMethod);
            String callerScopedKey = methodScopedKey(callerClass, callerMethod, callerDesc, edge.getCallerJarId());
            String calleeScopedKey = methodScopedKey(calleeClass, calleeMethod, calleeDesc, edge.getCalleeJarId());

            callersTmp.computeIfAbsent(calleeKey, k -> new LinkedHashMap<>())
                    .putIfAbsent(callerScopedKey, caller);
            calleesTmp.computeIfAbsent(callerKey, k -> new LinkedHashMap<>())
                    .putIfAbsent(calleeScopedKey, callee);
            callersScopedTmp.computeIfAbsent(calleeScopedKey, k -> new LinkedHashMap<>())
                    .putIfAbsent(callerScopedKey, caller);
            calleesScopedTmp.computeIfAbsent(callerScopedKey, k -> new LinkedHashMap<>())
                    .putIfAbsent(calleeScopedKey, callee);
            callersSimpleTmp.computeIfAbsent(calleeSimpleKey, k -> new LinkedHashMap<>())
                    .putIfAbsent(callerScopedKey, caller);
            calleesSimpleTmp.computeIfAbsent(callerSimpleKey, k -> new LinkedHashMap<>())
                    .putIfAbsent(calleeScopedKey, callee);
            scopedEdgeKeys.add(callerScopedKey + "->" + calleeScopedKey);

            MethodCallKey key = new MethodCallKey(
                    callerClass, callerMethod, callerDesc, edge.getCallerJarId(),
                    calleeClass, calleeMethod, calleeDesc, edge.getCalleeJarId());
            MethodCallMeta incoming = new MethodCallMeta(
                    edge.getEdgeType(),
                    edge.getEdgeConfidence(),
                    edge.getEdgeEvidence());
            MethodCallMeta existing = metaMap.get(key);
            if (existing == null) {
                metaMap.put(key, incoming);
            } else {
                MethodCallMeta merged = mergeMeta(existing, incoming);
                metaMap.put(key, merged);
            }
        }

        Map<String, ArrayList<MethodResult>> callersScoped = finalizeMap(callersScopedTmp);
        Map<String, ArrayList<MethodResult>> calleesScoped = finalizeMap(calleesScopedTmp);
        Map<String, ArrayList<MethodResult>> callers = finalizeMap(callersTmp);
        Map<String, ArrayList<MethodResult>> callees = finalizeMap(calleesTmp);
        Map<String, ArrayList<MethodResult>> callersSimple = finalizeMap(callersSimpleTmp);
        Map<String, ArrayList<MethodResult>> calleesSimple = finalizeMap(calleesSimpleTmp);

        int edgeCounter = scopedEdgeKeys.size();
        return new CallGraphCache(callersScoped, calleesScoped, callers, callees, callersSimple, calleesSimple,
                metaMap, edgeCounter, methodCache.size());
    }

    public ArrayList<MethodResult> getCallers(MethodResult callee) {
        if (callee == null) {
            return new ArrayList<>();
        }
        return getCallers(callee.getClassName(), callee.getMethodName(), callee.getMethodDesc(), callee.getJarId());
    }

    public ArrayList<MethodResult> getCallers(String className, String methodName, String methodDesc) {
        return getCallers(className, methodName, methodDesc, -1);
    }

    public ArrayList<MethodResult> getCallers(String className, String methodName, String methodDesc, Integer jarId) {
        if (jarId != null && jarId >= 0 && !isAnyDesc(methodDesc)) {
            String scoped = methodScopedKey(className, methodName, methodDesc, jarId);
            ArrayList<MethodResult> scopedResults = callersByCalleeScoped.get(scoped);
            if (scopedResults != null) {
                return scopedResults;
            }
        }
        String key = isAnyDesc(methodDesc)
                ? methodSimpleKey(className, methodName)
                : methodKey(className, methodName, methodDesc);
        ArrayList<MethodResult> results = isAnyDesc(methodDesc)
                ? callersByCalleeSimple.get(key)
                : callersByCallee.get(key);
        return results == null ? new ArrayList<>() : results;
    }

    public ArrayList<MethodResult> getCallees(MethodResult caller) {
        if (caller == null) {
            return new ArrayList<>();
        }
        return getCallees(caller.getClassName(), caller.getMethodName(), caller.getMethodDesc(), caller.getJarId());
    }

    public ArrayList<MethodResult> getCallees(String className, String methodName, String methodDesc) {
        return getCallees(className, methodName, methodDesc, -1);
    }

    public ArrayList<MethodResult> getCallees(String className, String methodName, String methodDesc, Integer jarId) {
        if (jarId != null && jarId >= 0 && !isAnyDesc(methodDesc)) {
            String scoped = methodScopedKey(className, methodName, methodDesc, jarId);
            ArrayList<MethodResult> scopedResults = calleesByCallerScoped.get(scoped);
            if (scopedResults != null) {
                return scopedResults;
            }
        }
        String key = isAnyDesc(methodDesc)
                ? methodSimpleKey(className, methodName)
                : methodKey(className, methodName, methodDesc);
        ArrayList<MethodResult> results = isAnyDesc(methodDesc)
                ? calleesByCallerSimple.get(key)
                : calleesByCaller.get(key);
        return results == null ? new ArrayList<>() : results;
    }

    public MethodCallMeta getEdgeMeta(MethodReference.Handle caller, MethodReference.Handle callee) {
        if (caller == null || callee == null) {
            return null;
        }
        MethodCallKey key = MethodCallKey.of(caller, callee);
        if (key == null) {
            return null;
        }
        MethodCallMeta direct = edgeMeta.get(key);
        if (direct != null) {
            return direct;
        }
        if ((caller.getJarId() != null && caller.getJarId() >= 0)
                && (callee.getJarId() != null && callee.getJarId() >= 0)) {
            return null;
        }
        MethodCallMeta merged = null;
        for (Map.Entry<MethodCallKey, MethodCallMeta> entry : edgeMeta.entrySet()) {
            MethodCallKey edgeKey = entry.getKey();
            if (edgeKey == null) {
                continue;
            }
            if (!safeEquals(edgeKey.getCallerClass(), caller.getClassReference().getName())) {
                continue;
            }
            if (!safeEquals(edgeKey.getCallerMethod(), caller.getName())) {
                continue;
            }
            if (!safeEquals(edgeKey.getCallerDesc(), caller.getDesc())) {
                continue;
            }
            if (!safeEquals(edgeKey.getCalleeClass(), callee.getClassReference().getName())) {
                continue;
            }
            if (!safeEquals(edgeKey.getCalleeMethod(), callee.getName())) {
                continue;
            }
            if (!safeEquals(edgeKey.getCalleeDesc(), callee.getDesc())) {
                continue;
            }
            merged = mergeMeta(merged, entry.getValue());
        }
        return merged;
    }

    public int getEdgeCount() {
        return edgeCount;
    }

    public int getMethodCount() {
        return methodCount;
    }

    private static Map<String, ArrayList<MethodResult>> finalizeMap(
            Map<String, LinkedHashMap<String, MethodResult>> tmp) {
        if (tmp == null || tmp.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, ArrayList<MethodResult>> out = new HashMap<>(tmp.size() * 2);
        for (Map.Entry<String, LinkedHashMap<String, MethodResult>> entry : tmp.entrySet()) {
            out.put(entry.getKey(), new ArrayList<>(entry.getValue().values()));
        }
        return out;
    }

    private static MethodResult getOrCreateMethod(Map<String, MethodResult> cache,
                                                  String className,
                                                  String methodName,
                                                  String methodDesc,
                                                  String jarName,
                                                  Integer jarId) {
        String key = methodScopedKey(className, methodName, methodDesc, jarId);
        MethodResult cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        MethodResult result = new MethodResult();
        result.setClassName(className);
        result.setMethodName(methodName);
        result.setMethodDesc(methodDesc);
        if (jarName != null && !jarName.trim().isEmpty()) {
            result.setJarName(jarName);
        }
        if (jarId != null) {
            result.setJarId(jarId);
        }
        cache.put(key, result);
        return result;
    }

    private static String methodKey(String className, String methodName, String methodDesc) {
        return className + "." + methodName + "." + methodDesc;
    }

    private static String methodSimpleKey(String className, String methodName) {
        return className + "." + methodName;
    }

    private static String methodScopedKey(String className,
                                          String methodName,
                                          String methodDesc,
                                          Integer jarId) {
        int scopedJarId = jarId == null ? -1 : jarId;
        return methodKey(className, methodName, methodDesc) + "#" + scopedJarId;
    }

    private static boolean isAnyDesc(String desc) {
        if (desc == null) {
            return true;
        }
        String v = desc.trim();
        if (v.isEmpty()) {
            return true;
        }
        if ("*".equals(v)) {
            return true;
        }
        return "null".equalsIgnoreCase(v);
    }

    private static boolean safeEquals(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    private static MethodCallMeta mergeMeta(MethodCallMeta existing, MethodCallMeta incoming) {
        if (incoming == null) {
            return existing;
        }
        if (existing == null) {
            return incoming;
        }
        String type = pickValue(existing.getType(), incoming.getType());
        String conf = pickConfidence(existing.getConfidence(), incoming.getConfidence());
        String evidence = pickValue(existing.getEvidence(), incoming.getEvidence());
        return new MethodCallMeta(type, conf, evidence);
    }

    private static String pickValue(String primary, String secondary) {
        if (primary != null && !primary.trim().isEmpty()) {
            return primary;
        }
        return secondary;
    }

    private static String pickConfidence(String existing, String incoming) {
        int ex = confidenceScore(existing);
        int in = confidenceScore(incoming);
        return in > ex ? incoming : existing;
    }

    private static int confidenceScore(String confidence) {
        if (MethodCallMeta.CONF_HIGH.equals(confidence)) {
            return 3;
        }
        if (MethodCallMeta.CONF_MEDIUM.equals(confidence)) {
            return 2;
        }
        if (MethodCallMeta.CONF_LOW.equals(confidence)) {
            return 1;
        }
        return 0;
    }
}
