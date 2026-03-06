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
    private final Map<String, ArrayList<MethodCallResult>> callEdgesByCallerScoped;
    private final Map<String, ArrayList<MethodCallResult>> callEdgesByCalleeScoped;
    private final Map<String, ArrayList<MethodCallResult>> callEdgesByCaller;
    private final Map<String, ArrayList<MethodCallResult>> callEdgesByCallee;
    private final Map<String, ArrayList<MethodCallResult>> callEdgesByCallerSimple;
    private final Map<String, ArrayList<MethodCallResult>> callEdgesByCalleeSimple;
    private final ArrayList<MethodCallResult> allCallEdges;
    private final Map<MethodCallKey, MethodCallMeta> edgeMeta;
    private final int edgeCount;
    private final int methodCount;

    private CallGraphCache(Map<String, ArrayList<MethodResult>> callersByCalleeScoped,
                           Map<String, ArrayList<MethodResult>> calleesByCallerScoped,
                           Map<String, ArrayList<MethodResult>> callersByCallee,
                           Map<String, ArrayList<MethodResult>> calleesByCaller,
                           Map<String, ArrayList<MethodResult>> callersByCalleeSimple,
                           Map<String, ArrayList<MethodResult>> calleesByCallerSimple,
                           Map<String, ArrayList<MethodCallResult>> callEdgesByCallerScoped,
                           Map<String, ArrayList<MethodCallResult>> callEdgesByCalleeScoped,
                           Map<String, ArrayList<MethodCallResult>> callEdgesByCaller,
                           Map<String, ArrayList<MethodCallResult>> callEdgesByCallee,
                           Map<String, ArrayList<MethodCallResult>> callEdgesByCallerSimple,
                           Map<String, ArrayList<MethodCallResult>> callEdgesByCalleeSimple,
                           ArrayList<MethodCallResult> allCallEdges,
                           Map<MethodCallKey, MethodCallMeta> edgeMeta,
                           int edgeCount,
                           int methodCount) {
        this.callersByCalleeScoped = callersByCalleeScoped;
        this.calleesByCallerScoped = calleesByCallerScoped;
        this.callersByCallee = callersByCallee;
        this.calleesByCaller = calleesByCaller;
        this.callersByCalleeSimple = callersByCalleeSimple;
        this.calleesByCallerSimple = calleesByCallerSimple;
        this.callEdgesByCallerScoped = callEdgesByCallerScoped;
        this.callEdgesByCalleeScoped = callEdgesByCalleeScoped;
        this.callEdgesByCaller = callEdgesByCaller;
        this.callEdgesByCallee = callEdgesByCallee;
        this.callEdgesByCallerSimple = callEdgesByCallerSimple;
        this.callEdgesByCalleeSimple = callEdgesByCalleeSimple;
        this.allCallEdges = allCallEdges;
        this.edgeMeta = edgeMeta;
        this.edgeCount = edgeCount;
        this.methodCount = methodCount;
    }

    public static CallGraphCache build(List<MethodCallResult> edges) {
        if (edges == null || edges.isEmpty()) {
            return new CallGraphCache(Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyMap(),
                    new ArrayList<>(),
                    Collections.emptyMap(), 0, 0);
        }
        Map<String, MethodResult> methodCache = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodResult>> callersScopedTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodResult>> calleesScopedTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodResult>> callersTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodResult>> calleesTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodResult>> callersSimpleTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodResult>> calleesSimpleTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodCallResult>> callEdgesByCallerScopedTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodCallResult>> callEdgesByCalleeScopedTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodCallResult>> callEdgesByCallerTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodCallResult>> callEdgesByCalleeTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodCallResult>> callEdgesByCallerSimpleTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodCallResult>> callEdgesByCalleeSimpleTmp = new HashMap<>();
        Map<String, MethodCallResult> callEdgeCache = new LinkedHashMap<>();
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
            String scopedEdgeKey = callerScopedKey + "->" + calleeScopedKey;
            scopedEdgeKeys.add(scopedEdgeKey);

            MethodCallResult cachedEdge = callEdgeCache.get(scopedEdgeKey);
            if (cachedEdge == null) {
                cachedEdge = copyEdge(edge);
                callEdgeCache.put(scopedEdgeKey, cachedEdge);
            } else {
                mergeEdge(cachedEdge, edge);
            }
            callEdgesByCallerScopedTmp.computeIfAbsent(callerScopedKey, k -> new LinkedHashMap<>())
                    .putIfAbsent(scopedEdgeKey, cachedEdge);
            callEdgesByCalleeScopedTmp.computeIfAbsent(calleeScopedKey, k -> new LinkedHashMap<>())
                    .putIfAbsent(scopedEdgeKey, cachedEdge);
            callEdgesByCallerTmp.computeIfAbsent(callerKey, k -> new LinkedHashMap<>())
                    .putIfAbsent(scopedEdgeKey, cachedEdge);
            callEdgesByCalleeTmp.computeIfAbsent(calleeKey, k -> new LinkedHashMap<>())
                    .putIfAbsent(scopedEdgeKey, cachedEdge);
            callEdgesByCallerSimpleTmp.computeIfAbsent(callerSimpleKey, k -> new LinkedHashMap<>())
                    .putIfAbsent(scopedEdgeKey, cachedEdge);
            callEdgesByCalleeSimpleTmp.computeIfAbsent(calleeSimpleKey, k -> new LinkedHashMap<>())
                    .putIfAbsent(scopedEdgeKey, cachedEdge);

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
        Map<String, ArrayList<MethodCallResult>> edgesByCallerScoped = finalizeEdgeMap(callEdgesByCallerScopedTmp);
        Map<String, ArrayList<MethodCallResult>> edgesByCalleeScoped = finalizeEdgeMap(callEdgesByCalleeScopedTmp);
        Map<String, ArrayList<MethodCallResult>> edgesByCaller = finalizeEdgeMap(callEdgesByCallerTmp);
        Map<String, ArrayList<MethodCallResult>> edgesByCallee = finalizeEdgeMap(callEdgesByCalleeTmp);
        Map<String, ArrayList<MethodCallResult>> edgesByCallerSimple = finalizeEdgeMap(callEdgesByCallerSimpleTmp);
        Map<String, ArrayList<MethodCallResult>> edgesByCalleeSimple = finalizeEdgeMap(callEdgesByCalleeSimpleTmp);
        ArrayList<MethodCallResult> allCallEdges = new ArrayList<>(callEdgeCache.values());
        allCallEdges.sort(EDGE_COMPARATOR);

        int edgeCounter = scopedEdgeKeys.size();
        return new CallGraphCache(callersScoped, calleesScoped, callers, callees, callersSimple, calleesSimple,
                edgesByCallerScoped, edgesByCalleeScoped, edgesByCaller, edgesByCallee,
                edgesByCallerSimple, edgesByCalleeSimple, allCallEdges,
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

    public ArrayList<MethodCallResult> getCallEdgesByCaller(String className,
                                                            String methodName,
                                                            String methodDesc,
                                                            Integer jarId) {
        return getCallEdges(true, className, methodName, methodDesc, jarId);
    }

    public ArrayList<MethodCallResult> getCallEdgesByCallee(String className,
                                                            String methodName,
                                                            String methodDesc,
                                                            Integer jarId) {
        return getCallEdges(false, className, methodName, methodDesc, jarId);
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

    private ArrayList<MethodCallResult> getCallEdges(boolean byCaller,
                                                     String className,
                                                     String methodName,
                                                     String methodDesc,
                                                     Integer jarId) {
        String cls = safe(className);
        String method = safe(methodName);
        String desc = safe(methodDesc);
        int normalizedJarId = jarId == null ? -1 : jarId;
        if (cls.isEmpty() && method.isEmpty() && isAnyDesc(desc) && normalizedJarId < 0) {
            return new ArrayList<>(allCallEdges);
        }
        if (!cls.isEmpty() && !method.isEmpty()) {
            ArrayList<MethodCallResult> indexed = indexedCallEdges(byCaller, cls, method, desc, normalizedJarId);
            if (indexed != null) {
                return new ArrayList<>(indexed);
            }
        }
        ArrayList<MethodCallResult> out = new ArrayList<>();
        for (MethodCallResult edge : allCallEdges) {
            if (!matchesEdgeSide(edge, byCaller, cls, method, desc, normalizedJarId)) {
                continue;
            }
            out.add(edge);
        }
        return out;
    }

    private ArrayList<MethodCallResult> indexedCallEdges(boolean byCaller,
                                                         String className,
                                                         String methodName,
                                                         String methodDesc,
                                                         int jarId) {
        if (jarId >= 0 && !isAnyDesc(methodDesc)) {
            String scoped = methodScopedKey(className, methodName, methodDesc, jarId);
            ArrayList<MethodCallResult> scopedResults = byCaller
                    ? callEdgesByCallerScoped.get(scoped)
                    : callEdgesByCalleeScoped.get(scoped);
            if (scopedResults != null) {
                return scopedResults;
            }
        }
        if (isAnyDesc(methodDesc)) {
            String simple = methodSimpleKey(className, methodName);
            return byCaller ? callEdgesByCallerSimple.get(simple) : callEdgesByCalleeSimple.get(simple);
        }
        String exact = methodKey(className, methodName, methodDesc);
        return byCaller ? callEdgesByCaller.get(exact) : callEdgesByCallee.get(exact);
    }

    private static boolean matchesEdgeSide(MethodCallResult edge,
                                           boolean byCaller,
                                           String className,
                                           String methodName,
                                           String methodDesc,
                                           int jarId) {
        if (edge == null) {
            return false;
        }
        String edgeClass = byCaller ? safe(edge.getCallerClassName()) : safe(edge.getCalleeClassName());
        String edgeMethod = byCaller ? safe(edge.getCallerMethodName()) : safe(edge.getCalleeMethodName());
        String edgeDesc = byCaller ? safe(edge.getCallerMethodDesc()) : safe(edge.getCalleeMethodDesc());
        Integer edgeJarId = byCaller ? edge.getCallerJarId() : edge.getCalleeJarId();
        if (!className.isEmpty() && !className.equals(edgeClass)) {
            return false;
        }
        if (!methodName.isEmpty() && !methodName.equals(edgeMethod)) {
            return false;
        }
        if (!isAnyDesc(methodDesc) && !methodDesc.equals(edgeDesc)) {
            return false;
        }
        return jarId < 0 || (edgeJarId != null && edgeJarId == jarId);
    }

    private static Map<String, ArrayList<MethodCallResult>> finalizeEdgeMap(
            Map<String, LinkedHashMap<String, MethodCallResult>> tmp) {
        if (tmp == null || tmp.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, ArrayList<MethodCallResult>> out = new HashMap<>(tmp.size() * 2);
        for (Map.Entry<String, LinkedHashMap<String, MethodCallResult>> entry : tmp.entrySet()) {
            ArrayList<MethodCallResult> values = new ArrayList<>(entry.getValue().values());
            values.sort(EDGE_COMPARATOR);
            out.put(entry.getKey(), values);
        }
        return out;
    }

    private static MethodCallResult copyEdge(MethodCallResult edge) {
        MethodCallResult copy = new MethodCallResult();
        copy.setCallerClassName(edge.getCallerClassName());
        copy.setCallerMethodName(edge.getCallerMethodName());
        copy.setCallerMethodDesc(edge.getCallerMethodDesc());
        copy.setCallerJarId(edge.getCallerJarId());
        copy.setCallerJarName(edge.getCallerJarName());
        copy.setCalleeClassName(edge.getCalleeClassName());
        copy.setCalleeMethodName(edge.getCalleeMethodName());
        copy.setCalleeMethodDesc(edge.getCalleeMethodDesc());
        copy.setCalleeJarId(edge.getCalleeJarId());
        copy.setCalleeJarName(edge.getCalleeJarName());
        copy.setOpCode(edge.getOpCode());
        copy.setEdgeType(edge.getEdgeType());
        copy.setEdgeConfidence(edge.getEdgeConfidence());
        copy.setEdgeEvidence(edge.getEdgeEvidence());
        copy.setCallSiteKey(edge.getCallSiteKey());
        return copy;
    }

    private static void mergeEdge(MethodCallResult existing, MethodCallResult incoming) {
        if (existing == null || incoming == null) {
            return;
        }
        MethodCallMeta merged = mergeMeta(
                new MethodCallMeta(existing.getEdgeType(), existing.getEdgeConfidence(), existing.getEdgeEvidence()),
                new MethodCallMeta(incoming.getEdgeType(), incoming.getEdgeConfidence(), incoming.getEdgeEvidence())
        );
        if (merged != null) {
            existing.setEdgeType(merged.getType());
            existing.setEdgeConfidence(merged.getConfidence());
            existing.setEdgeEvidence(merged.getEvidence());
        }
        if (existing.getOpCode() == null && incoming.getOpCode() != null) {
            existing.setOpCode(incoming.getOpCode());
        }
        if ((existing.getCallSiteKey() == null || existing.getCallSiteKey().isBlank())
                && incoming.getCallSiteKey() != null
                && !incoming.getCallSiteKey().isBlank()) {
            existing.setCallSiteKey(incoming.getCallSiteKey());
        }
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

    private static String safe(String value) {
        return value == null ? "" : value.trim();
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

    private static final java.util.Comparator<MethodCallResult> EDGE_COMPARATOR =
            java.util.Comparator.comparing((MethodCallResult row) -> safe(row == null ? null : row.getCallerClassName()))
                    .thenComparing(row -> safe(row == null ? null : row.getCallerMethodName()))
                    .thenComparing(row -> safe(row == null ? null : row.getCallerMethodDesc()))
                    .thenComparingInt(row -> row == null || row.getCallerJarId() == null ? Integer.MAX_VALUE : row.getCallerJarId())
                    .thenComparing(row -> safe(row == null ? null : row.getCalleeClassName()))
                    .thenComparing(row -> safe(row == null ? null : row.getCalleeMethodName()))
                    .thenComparing(row -> safe(row == null ? null : row.getCalleeMethodDesc()))
                    .thenComparingInt(row -> row == null || row.getCalleeJarId() == null ? Integer.MAX_VALUE : row.getCalleeJarId());
}
