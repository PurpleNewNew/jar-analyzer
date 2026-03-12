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
import me.n1ar4.jar.analyzer.engine.model.CallEdgeView;
import me.n1ar4.jar.analyzer.engine.model.MethodView;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CallGraphCache {
    private final Map<String, ArrayList<MethodView>> callersByCalleeScoped;
    private final Map<String, ArrayList<MethodView>> calleesByCallerScoped;
    private final Map<String, ArrayList<MethodView>> callersByCallee;
    private final Map<String, ArrayList<MethodView>> calleesByCaller;
    private final Map<String, ArrayList<MethodView>> callersByCalleeSimple;
    private final Map<String, ArrayList<MethodView>> calleesByCallerSimple;
    private final Map<String, ArrayList<CallEdgeView>> callEdgesByCallerScoped;
    private final Map<String, ArrayList<CallEdgeView>> callEdgesByCalleeScoped;
    private final Map<String, ArrayList<CallEdgeView>> callEdgesByCaller;
    private final Map<String, ArrayList<CallEdgeView>> callEdgesByCallee;
    private final Map<String, ArrayList<CallEdgeView>> callEdgesByCallerSimple;
    private final Map<String, ArrayList<CallEdgeView>> callEdgesByCalleeSimple;
    private final ArrayList<CallEdgeView> allCallEdges;
    private final Map<MethodCallKey, MethodCallMeta> edgeMeta;
    private final int edgeCount;
    private final int methodCount;

    private CallGraphCache(Map<String, ArrayList<MethodView>> callersByCalleeScoped,
                           Map<String, ArrayList<MethodView>> calleesByCallerScoped,
                           Map<String, ArrayList<MethodView>> callersByCallee,
                           Map<String, ArrayList<MethodView>> calleesByCaller,
                           Map<String, ArrayList<MethodView>> callersByCalleeSimple,
                           Map<String, ArrayList<MethodView>> calleesByCallerSimple,
                           Map<String, ArrayList<CallEdgeView>> callEdgesByCallerScoped,
                           Map<String, ArrayList<CallEdgeView>> callEdgesByCalleeScoped,
                           Map<String, ArrayList<CallEdgeView>> callEdgesByCaller,
                           Map<String, ArrayList<CallEdgeView>> callEdgesByCallee,
                           Map<String, ArrayList<CallEdgeView>> callEdgesByCallerSimple,
                           Map<String, ArrayList<CallEdgeView>> callEdgesByCalleeSimple,
                           ArrayList<CallEdgeView> allCallEdges,
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

    public static CallGraphCache build(List<CallEdgeView> edges) {
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
        Map<String, MethodView> methodCache = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodView>> callersScopedTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodView>> calleesScopedTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodView>> callersTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodView>> calleesTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodView>> callersSimpleTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodView>> calleesSimpleTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, CallEdgeView>> callEdgesByCallerScopedTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, CallEdgeView>> callEdgesByCalleeScopedTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, CallEdgeView>> callEdgesByCallerTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, CallEdgeView>> callEdgesByCalleeTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, CallEdgeView>> callEdgesByCallerSimpleTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, CallEdgeView>> callEdgesByCalleeSimpleTmp = new HashMap<>();
        Map<String, CallEdgeView> callEdgeCache = new LinkedHashMap<>();
        Map<MethodCallKey, MethodCallMeta> metaMap = new HashMap<>(edges.size() * 2);
        Set<String> scopedEdgeKeys = new HashSet<>(edges.size() * 2);

        for (CallEdgeView edge : edges) {
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
            MethodView caller = getOrCreateMethod(methodCache, callerClass, callerMethod, callerDesc,
                    edge.getCallerJarName(), edge.getCallerJarId());
            MethodView callee = getOrCreateMethod(methodCache, calleeClass, calleeMethod, calleeDesc,
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

            CallEdgeView cachedEdge = callEdgeCache.get(scopedEdgeKey);
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

        Map<String, ArrayList<MethodView>> callersScoped = finalizeMap(callersScopedTmp);
        Map<String, ArrayList<MethodView>> calleesScoped = finalizeMap(calleesScopedTmp);
        Map<String, ArrayList<MethodView>> callers = finalizeMap(callersTmp);
        Map<String, ArrayList<MethodView>> callees = finalizeMap(calleesTmp);
        Map<String, ArrayList<MethodView>> callersSimple = finalizeMap(callersSimpleTmp);
        Map<String, ArrayList<MethodView>> calleesSimple = finalizeMap(calleesSimpleTmp);
        Map<String, ArrayList<CallEdgeView>> edgesByCallerScoped = finalizeEdgeMap(callEdgesByCallerScopedTmp);
        Map<String, ArrayList<CallEdgeView>> edgesByCalleeScoped = finalizeEdgeMap(callEdgesByCalleeScopedTmp);
        Map<String, ArrayList<CallEdgeView>> edgesByCaller = finalizeEdgeMap(callEdgesByCallerTmp);
        Map<String, ArrayList<CallEdgeView>> edgesByCallee = finalizeEdgeMap(callEdgesByCalleeTmp);
        Map<String, ArrayList<CallEdgeView>> edgesByCallerSimple = finalizeEdgeMap(callEdgesByCallerSimpleTmp);
        Map<String, ArrayList<CallEdgeView>> edgesByCalleeSimple = finalizeEdgeMap(callEdgesByCalleeSimpleTmp);
        ArrayList<CallEdgeView> allCallEdges = new ArrayList<>(callEdgeCache.values());
        allCallEdges.sort(EDGE_COMPARATOR);

        int edgeCounter = scopedEdgeKeys.size();
        return new CallGraphCache(callersScoped, calleesScoped, callers, callees, callersSimple, calleesSimple,
                edgesByCallerScoped, edgesByCalleeScoped, edgesByCaller, edgesByCallee,
                edgesByCallerSimple, edgesByCalleeSimple, allCallEdges,
                metaMap, edgeCounter, methodCache.size());
    }

    public static CallGraphCache build(GraphSnapshot snapshot, Map<Integer, String> jarNames) {
        if (snapshot == null || snapshot.getNodeCount() <= 0 || snapshot.getEdgeCount() <= 0) {
            return new CallGraphCache(Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyMap(),
                    new ArrayList<>(),
                    Collections.emptyMap(), 0, 0);
        }
        Map<Long, MethodView> methodCache = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodView>> callersScopedTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodView>> calleesScopedTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodView>> callersTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodView>> calleesTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodView>> callersSimpleTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, MethodView>> calleesSimpleTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, CallEdgeView>> callEdgesByCallerScopedTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, CallEdgeView>> callEdgesByCalleeScopedTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, CallEdgeView>> callEdgesByCallerTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, CallEdgeView>> callEdgesByCalleeTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, CallEdgeView>> callEdgesByCallerSimpleTmp = new HashMap<>();
        Map<String, LinkedHashMap<String, CallEdgeView>> callEdgesByCalleeSimpleTmp = new HashMap<>();
        Map<String, CallEdgeView> callEdgeCache = new LinkedHashMap<>();
        Map<MethodCallKey, MethodCallMeta> metaMap = new HashMap<>(Math.max(16, snapshot.getEdgeCount() * 2));
        Set<String> scopedEdgeKeys = new HashSet<>(Math.max(16, snapshot.getEdgeCount() * 2));

        for (GraphNode callerNode : snapshot.getNodesByKindView("method")) {
            if (!isMethodNode(callerNode)) {
                continue;
            }
            MethodView caller = getOrCreateMethod(methodCache, callerNode, jarNames);
            String callerClass = caller.getClassName();
            String callerMethod = caller.getMethodName();
            String callerDesc = caller.getMethodDesc();
            String callerKey = methodKey(callerClass, callerMethod, callerDesc);
            String callerSimpleKey = methodSimpleKey(callerClass, callerMethod);
            String callerScopedKey = methodScopedKey(callerClass, callerMethod, callerDesc, caller.getJarId());

            for (GraphEdge edge : snapshot.getOutgoingView(callerNode.getNodeId())) {
                if (edge == null || !isCallEdge(edge.getRelType())) {
                    continue;
                }
                GraphNode calleeNode = snapshot.getNode(edge.getDstId());
                if (!isMethodNode(calleeNode)) {
                    continue;
                }
                MethodView callee = getOrCreateMethod(methodCache, calleeNode, jarNames);
                String calleeClass = callee.getClassName();
                String calleeMethod = callee.getMethodName();
                String calleeDesc = callee.getMethodDesc();
                String calleeKey = methodKey(calleeClass, calleeMethod, calleeDesc);
                String calleeSimpleKey = methodSimpleKey(calleeClass, calleeMethod);
                String calleeScopedKey = methodScopedKey(calleeClass, calleeMethod, calleeDesc, callee.getJarId());

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
                CallEdgeView cachedEdge = callEdgeCache.get(scopedEdgeKey);
                if (cachedEdge == null) {
                    cachedEdge = toEdge(caller, callee, edge);
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
                        callerClass, callerMethod, callerDesc, caller.getJarId(),
                        calleeClass, calleeMethod, calleeDesc, callee.getJarId()
                );
                MethodCallMeta incoming = new MethodCallMeta(
                        resolveEdgeType(edge),
                        safe(edge.getConfidence()),
                        safe(edge.getEvidence())
                );
                MethodCallMeta existing = metaMap.get(key);
                metaMap.put(key, mergeMeta(existing, incoming));
            }
        }

        Map<String, ArrayList<MethodView>> callersScoped = finalizeMap(callersScopedTmp);
        Map<String, ArrayList<MethodView>> calleesScoped = finalizeMap(calleesScopedTmp);
        Map<String, ArrayList<MethodView>> callers = finalizeMap(callersTmp);
        Map<String, ArrayList<MethodView>> callees = finalizeMap(calleesTmp);
        Map<String, ArrayList<MethodView>> callersSimple = finalizeMap(callersSimpleTmp);
        Map<String, ArrayList<MethodView>> calleesSimple = finalizeMap(calleesSimpleTmp);
        Map<String, ArrayList<CallEdgeView>> edgesByCallerScoped = finalizeEdgeMap(callEdgesByCallerScopedTmp);
        Map<String, ArrayList<CallEdgeView>> edgesByCalleeScoped = finalizeEdgeMap(callEdgesByCalleeScopedTmp);
        Map<String, ArrayList<CallEdgeView>> edgesByCaller = finalizeEdgeMap(callEdgesByCallerTmp);
        Map<String, ArrayList<CallEdgeView>> edgesByCallee = finalizeEdgeMap(callEdgesByCalleeTmp);
        Map<String, ArrayList<CallEdgeView>> edgesByCallerSimple = finalizeEdgeMap(callEdgesByCallerSimpleTmp);
        Map<String, ArrayList<CallEdgeView>> edgesByCalleeSimple = finalizeEdgeMap(callEdgesByCalleeSimpleTmp);
        ArrayList<CallEdgeView> allCallEdges = new ArrayList<>(callEdgeCache.values());
        allCallEdges.sort(EDGE_COMPARATOR);

        return new CallGraphCache(callersScoped, calleesScoped, callers, callees, callersSimple, calleesSimple,
                edgesByCallerScoped, edgesByCalleeScoped, edgesByCaller, edgesByCallee,
                edgesByCallerSimple, edgesByCalleeSimple, allCallEdges,
                metaMap, scopedEdgeKeys.size(), methodCache.size());
    }

    public ArrayList<MethodView> getCallers(MethodView callee) {
        if (callee == null) {
            return new ArrayList<>();
        }
        return getCallers(callee.getClassName(), callee.getMethodName(), callee.getMethodDesc(), callee.getJarId());
    }

    public ArrayList<MethodView> getCallers(String className, String methodName, String methodDesc) {
        return getCallers(className, methodName, methodDesc, -1);
    }

    public ArrayList<MethodView> getCallers(String className, String methodName, String methodDesc, Integer jarId) {
        if (jarId != null && jarId >= 0 && !isAnyDesc(methodDesc)) {
            String scoped = methodScopedKey(className, methodName, methodDesc, jarId);
            ArrayList<MethodView> scopedResults = callersByCalleeScoped.get(scoped);
            if (scopedResults != null) {
                return scopedResults;
            }
        }
        String key = isAnyDesc(methodDesc)
                ? methodSimpleKey(className, methodName)
                : methodKey(className, methodName, methodDesc);
        ArrayList<MethodView> results = isAnyDesc(methodDesc)
                ? callersByCalleeSimple.get(key)
                : callersByCallee.get(key);
        return results == null ? new ArrayList<>() : results;
    }

    public ArrayList<MethodView> getCallees(MethodView caller) {
        if (caller == null) {
            return new ArrayList<>();
        }
        return getCallees(caller.getClassName(), caller.getMethodName(), caller.getMethodDesc(), caller.getJarId());
    }

    public ArrayList<MethodView> getCallees(String className, String methodName, String methodDesc) {
        return getCallees(className, methodName, methodDesc, -1);
    }

    public ArrayList<MethodView> getCallees(String className, String methodName, String methodDesc, Integer jarId) {
        if (jarId != null && jarId >= 0 && !isAnyDesc(methodDesc)) {
            String scoped = methodScopedKey(className, methodName, methodDesc, jarId);
            ArrayList<MethodView> scopedResults = calleesByCallerScoped.get(scoped);
            if (scopedResults != null) {
                return scopedResults;
            }
        }
        String key = isAnyDesc(methodDesc)
                ? methodSimpleKey(className, methodName)
                : methodKey(className, methodName, methodDesc);
        ArrayList<MethodView> results = isAnyDesc(methodDesc)
                ? calleesByCallerSimple.get(key)
                : calleesByCaller.get(key);
        return results == null ? new ArrayList<>() : results;
    }

    public ArrayList<CallEdgeView> getCallEdgesByCaller(String className,
                                                            String methodName,
                                                            String methodDesc,
                                                            Integer jarId) {
        return new ArrayList<>(viewCallEdges(true, className, methodName, methodDesc, jarId));
    }

    public ArrayList<CallEdgeView> getCallEdgesByCallee(String className,
                                                            String methodName,
                                                            String methodDesc,
                                                            Integer jarId) {
        return new ArrayList<>(viewCallEdges(false, className, methodName, methodDesc, jarId));
    }

    List<CallEdgeView> viewCallEdgesByCaller(String className,
                                                 String methodName,
                                                 String methodDesc,
                                                 Integer jarId) {
        return viewCallEdges(true, className, methodName, methodDesc, jarId);
    }

    List<CallEdgeView> viewCallEdgesByCallee(String className,
                                                 String methodName,
                                                 String methodDesc,
                                                 Integer jarId) {
        return viewCallEdges(false, className, methodName, methodDesc, jarId);
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

    private static Map<String, ArrayList<MethodView>> finalizeMap(
            Map<String, LinkedHashMap<String, MethodView>> tmp) {
        if (tmp == null || tmp.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, ArrayList<MethodView>> out = new HashMap<>(tmp.size() * 2);
        for (Map.Entry<String, LinkedHashMap<String, MethodView>> entry : tmp.entrySet()) {
            out.put(entry.getKey(), new ArrayList<>(entry.getValue().values()));
        }
        return out;
    }

    private List<CallEdgeView> viewCallEdges(boolean byCaller,
                                                 String className,
                                                 String methodName,
                                                 String methodDesc,
                                                 Integer jarId) {
        String cls = safe(className);
        String method = safe(methodName);
        String desc = safe(methodDesc);
        int normalizedJarId = jarId == null ? -1 : jarId;
        if (cls.isEmpty() && method.isEmpty() && isAnyDesc(desc) && normalizedJarId < 0) {
            return allCallEdges;
        }
        if (!cls.isEmpty() && !method.isEmpty()) {
            ArrayList<CallEdgeView> indexed = indexedCallEdges(byCaller, cls, method, desc, normalizedJarId);
            if (indexed != null) {
                return indexed;
            }
        }
        ArrayList<CallEdgeView> out = new ArrayList<>();
        for (CallEdgeView edge : allCallEdges) {
            if (!matchesEdgeSide(edge, byCaller, cls, method, desc, normalizedJarId)) {
                continue;
            }
            out.add(edge);
        }
        return out;
    }

    private ArrayList<CallEdgeView> indexedCallEdges(boolean byCaller,
                                                         String className,
                                                         String methodName,
                                                         String methodDesc,
                                                         int jarId) {
        if (jarId >= 0 && !isAnyDesc(methodDesc)) {
            String scoped = methodScopedKey(className, methodName, methodDesc, jarId);
            ArrayList<CallEdgeView> scopedResults = byCaller
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

    private static boolean matchesEdgeSide(CallEdgeView edge,
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

    private static Map<String, ArrayList<CallEdgeView>> finalizeEdgeMap(
            Map<String, LinkedHashMap<String, CallEdgeView>> tmp) {
        if (tmp == null || tmp.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, ArrayList<CallEdgeView>> out = new HashMap<>(tmp.size() * 2);
        for (Map.Entry<String, LinkedHashMap<String, CallEdgeView>> entry : tmp.entrySet()) {
            ArrayList<CallEdgeView> values = new ArrayList<>(entry.getValue().values());
            values.sort(EDGE_COMPARATOR);
            out.put(entry.getKey(), values);
        }
        return out;
    }

    private static CallEdgeView copyEdge(CallEdgeView edge) {
        CallEdgeView copy = new CallEdgeView();
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

    private static CallEdgeView toEdge(MethodView caller, MethodView callee, GraphEdge edge) {
        CallEdgeView row = new CallEdgeView();
        row.setCallerClassName(caller == null ? "" : caller.getClassName());
        row.setCallerMethodName(caller == null ? "" : caller.getMethodName());
        row.setCallerMethodDesc(caller == null ? "" : caller.getMethodDesc());
        row.setCallerJarId(caller == null ? -1 : caller.getJarId());
        row.setCallerJarName(caller == null ? "" : caller.getJarName());
        row.setCalleeClassName(callee == null ? "" : callee.getClassName());
        row.setCalleeMethodName(callee == null ? "" : callee.getMethodName());
        row.setCalleeMethodDesc(callee == null ? "" : callee.getMethodDesc());
        row.setCalleeJarId(callee == null ? -1 : callee.getJarId());
        row.setCalleeJarName(callee == null ? "" : callee.getJarName());
        row.setOpCode(edge == null ? null : edge.getOpCode());
        row.setEdgeType(resolveEdgeType(edge));
        row.setEdgeConfidence(edge == null ? "" : safe(edge.getConfidence()));
        row.setEdgeEvidence(edge == null ? "" : safe(edge.getEvidence()));
        row.setCallSiteKey(edge == null ? "" : safe(edge.getCallSiteKey()));
        return row;
    }

    private static void mergeEdge(CallEdgeView existing, CallEdgeView incoming) {
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

    private static void mergeEdge(CallEdgeView existing, GraphEdge incoming) {
        if (existing == null || incoming == null) {
            return;
        }
        MethodCallMeta merged = mergeMeta(
                new MethodCallMeta(existing.getEdgeType(), existing.getEdgeConfidence(), existing.getEdgeEvidence()),
                new MethodCallMeta(resolveEdgeType(incoming), safe(incoming.getConfidence()), safe(incoming.getEvidence()))
        );
        if (merged != null) {
            existing.setEdgeType(merged.getType());
            existing.setEdgeConfidence(merged.getConfidence());
            existing.setEdgeEvidence(merged.getEvidence());
        }
        if (existing.getOpCode() == null) {
            existing.setOpCode(incoming.getOpCode());
        }
        if ((existing.getCallSiteKey() == null || existing.getCallSiteKey().isBlank())
                && !safe(incoming.getCallSiteKey()).isBlank()) {
            existing.setCallSiteKey(incoming.getCallSiteKey());
        }
    }

    private static MethodView getOrCreateMethod(Map<String, MethodView> cache,
                                                  String className,
                                                  String methodName,
                                                  String methodDesc,
                                                  String jarName,
                                                  Integer jarId) {
        String key = methodScopedKey(className, methodName, methodDesc, jarId);
        MethodView cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        MethodView result = new MethodView();
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

    private static MethodView getOrCreateMethod(Map<Long, MethodView> cache,
                                                  GraphNode node,
                                                  Map<Integer, String> jarNames) {
        if (node == null) {
            return null;
        }
        MethodView cached = cache.get(node.getNodeId());
        if (cached != null) {
            return cached;
        }
        MethodView result = new MethodView();
        result.setClassName(safe(node.getClassName()));
        result.setMethodName(safe(node.getMethodName()));
        result.setMethodDesc(safe(node.getMethodDesc()));
        result.setJarId(node.getJarId());
        result.setJarName(resolveJarName(node.getJarId(), jarNames));
        cache.put(node.getNodeId(), result);
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

    private static boolean isMethodNode(GraphNode node) {
        return node != null && "method".equalsIgnoreCase(node.getKind());
    }

    private static boolean isCallEdge(String relType) {
        String value = safe(relType);
        return !value.isEmpty() && value.startsWith("CALLS_");
    }

    private static String resolveJarName(int jarId, Map<Integer, String> jarNames) {
        if (jarId < 0 || jarNames == null || jarNames.isEmpty()) {
            return "";
        }
        return safe(jarNames.get(jarId));
    }

    private static String resolveEdgeType(GraphEdge edge) {
        if (edge == null) {
            return MethodCallMeta.TYPE_DIRECT;
        }
        String relation = safe(edge.getRelType());
        return switch (relation) {
            case "CALLS_DISPATCH" -> MethodCallMeta.TYPE_DISPATCH;
            case "CALLS_REFLECTION" -> MethodCallMeta.TYPE_REFLECTION;
            case "CALLS_CALLBACK" -> MethodCallMeta.TYPE_CALLBACK;
            case "CALLS_OVERRIDE" -> MethodCallMeta.TYPE_OVERRIDE;
            case "CALLS_FRAMEWORK" -> MethodCallMeta.TYPE_FRAMEWORK;
            case "CALLS_METHOD_HANDLE" -> MethodCallMeta.TYPE_METHOD_HANDLE;
            case "CALLS_PTA" -> MethodCallMeta.TYPE_PTA;
            case "CALLS_INDY" -> MethodCallMeta.TYPE_INDY;
            default -> resolveEdgeTypeFromEvidence(edge.getEvidence());
        };
    }

    private static String resolveEdgeTypeFromEvidence(String evidence) {
        String ev = safe(evidence).toLowerCase();
        if (ev.contains("pta:") || ev.contains("pta_ctx=") || ev.contains("|pta:")) {
            return MethodCallMeta.TYPE_PTA;
        }
        if (ev.contains("method_handle") || ev.contains("mh:")) {
            return MethodCallMeta.TYPE_METHOD_HANDLE;
        }
        if (ev.contains("framework") || ev.contains("spring_web_entry")) {
            return MethodCallMeta.TYPE_FRAMEWORK;
        }
        if (ev.contains("callback") || ev.contains("dynamic_proxy")) {
            return MethodCallMeta.TYPE_CALLBACK;
        }
        if (ev.contains("reflection") || ev.contains("tamiflex") || ev.contains("reflect")) {
            return MethodCallMeta.TYPE_REFLECTION;
        }
        return MethodCallMeta.TYPE_DIRECT;
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

    private static final java.util.Comparator<CallEdgeView> EDGE_COMPARATOR =
            java.util.Comparator.comparing((CallEdgeView row) -> safe(row == null ? null : row.getCallerClassName()))
                    .thenComparing(row -> safe(row == null ? null : row.getCallerMethodName()))
                    .thenComparing(row -> safe(row == null ? null : row.getCallerMethodDesc()))
                    .thenComparingInt(row -> row == null || row.getCallerJarId() == null ? Integer.MAX_VALUE : row.getCallerJarId())
                    .thenComparing(row -> safe(row == null ? null : row.getCalleeClassName()))
                    .thenComparing(row -> safe(row == null ? null : row.getCalleeMethodName()))
                    .thenComparing(row -> safe(row == null ? null : row.getCalleeMethodDesc()))
                    .thenComparingInt(row -> row == null || row.getCalleeJarId() == null ? Integer.MAX_VALUE : row.getCalleeJarId());
}
