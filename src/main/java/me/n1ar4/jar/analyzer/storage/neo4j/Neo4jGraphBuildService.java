/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.storage.neo4j;

import me.n1ar4.jar.analyzer.core.CallSiteKeyUtil;
import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.graph.model.GraphRelationType;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Neo4jGraphBuildService {
    private static final Logger logger = LogManager.getLogger();
    private static final String LABEL_METHOD = "method";
    private static final String LABEL_CALLSITE = "callsite";
    private final Neo4jGraphWriteService graphWriteService = new Neo4jGraphWriteService();

    public GraphBuildStats replaceFromAnalysis(long buildSeq,
                                               boolean quickMode,
                                               String callGraphMode,
                                               Set<MethodReference> methods,
                                               Map<MethodReference.Handle, ? extends Set<MethodReference.Handle>> methodCalls,
                                               Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                               List<CallSiteEntity> callSites) {
        GraphAssembly assembly = assemble(buildSeq, methods, methodCalls, methodCallMeta, callSites);
        String projectKey = ActiveProjectContext.getActiveProjectKey();
        graphWriteService.replaceFromSnapshot(
                projectKey,
                assembly.snapshot,
                buildSeq,
                quickMode,
                callGraphMode
        );
        GraphBuildStats stats = new GraphBuildStats(
                assembly.methodNodeCount,
                assembly.callSiteNodeCount,
                assembly.snapshot.getEdgeCount()
        );
        logger.info("neo4j direct build finish: key={} buildSeq={} methodNodes={} callSiteNodes={} edges={}",
                projectKey, buildSeq, stats.methodNodes(), stats.callSiteNodes(), stats.edgeCount());
        return stats;
    }

    private GraphAssembly assemble(long buildSeq,
                                   Set<MethodReference> methods,
                                   Map<MethodReference.Handle, ? extends Set<MethodReference.Handle>> methodCalls,
                                   Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                   List<CallSiteEntity> callSites) {
        Map<Long, GraphNode> nodeMap = new LinkedHashMap<>();
        Map<String, List<GraphNode>> labelIndex = new HashMap<>();
        Map<MethodKey, Long> methodNodeByKey = new LinkedHashMap<>();
        Map<MethodLooseKey, Long> methodNodeByLooseKey = new LinkedHashMap<>();

        long nextNodeId = 1L;
        List<MethodReference> sortedMethods = sortedMethods(methods);
        for (MethodReference method : sortedMethods) {
            MethodKey key = toMethodKey(method);
            if (methodNodeByKey.containsKey(key)) {
                continue;
            }
            long nodeId = nextNodeId++;
            GraphNode node = new GraphNode(
                    nodeId,
                    "method",
                    key.jarId,
                    key.className,
                    key.methodName,
                    key.methodDesc,
                    "",
                    -1,
                    -1
            );
            nodeMap.put(nodeId, node);
            methodNodeByKey.put(key, nodeId);
            methodNodeByLooseKey.putIfAbsent(new MethodLooseKey(key.className, key.methodName, key.methodDesc), nodeId);
            addLabel(labelIndex, LABEL_METHOD, node);
        }

        Map<String, Long> callSiteNodeByKey = new LinkedHashMap<>();
        List<CallSiteRowWithNode> callSiteRows = new ArrayList<>();
        for (CallSiteRow row : sortedCallSites(callSites)) {
            Long nodeId = callSiteNodeByKey.get(row.callSiteKey);
            if (nodeId == null) {
                nodeId = nextNodeId++;
                callSiteNodeByKey.put(row.callSiteKey, nodeId);
                GraphNode node = new GraphNode(
                        nodeId,
                        "callsite",
                        row.jarId,
                        row.callerClass,
                        row.callerMethod,
                        row.callerDesc,
                        row.callSiteKey,
                        row.lineNumber,
                        row.callIndex
                );
                nodeMap.put(nodeId, node);
                addLabel(labelIndex, LABEL_CALLSITE, node);
            }
            callSiteRows.add(new CallSiteRowWithNode(row, nodeId));
        }

        Map<EdgeKey, EdgeKey> edgeMap = new LinkedHashMap<>();
        collectMethodCallEdges(edgeMap, methodCalls, methodCallMeta, methodNodeByKey, methodNodeByLooseKey);
        collectCallSiteEdges(edgeMap, callSiteRows, methodNodeByKey, methodNodeByLooseKey);
        collectNextCallSiteEdges(edgeMap, callSiteRows);

        Map<Long, List<GraphEdge>> outgoing = new LinkedHashMap<>();
        Map<Long, List<GraphEdge>> incoming = new LinkedHashMap<>();
        long edgeId = 1L;
        for (EdgeKey key : edgeMap.keySet()) {
            GraphEdge edge = new GraphEdge(
                    edgeId++,
                    key.srcId,
                    key.dstId,
                    key.relType,
                    key.confidence,
                    key.evidence,
                    key.opCode
            );
            outgoing.computeIfAbsent(edge.getSrcId(), ignore -> new ArrayList<>()).add(edge);
            incoming.computeIfAbsent(edge.getDstId(), ignore -> new ArrayList<>()).add(edge);
        }
        GraphSnapshot snapshot = GraphSnapshot.of(buildSeq, nodeMap, outgoing, incoming, labelIndex);
        return new GraphAssembly(snapshot, methodNodeByKey.size(), callSiteNodeByKey.size());
    }

    private static void collectMethodCallEdges(Map<EdgeKey, EdgeKey> edgeMap,
                                               Map<MethodReference.Handle, ? extends Set<MethodReference.Handle>> methodCalls,
                                               Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                               Map<MethodKey, Long> methodNodeByKey,
                                               Map<MethodLooseKey, Long> methodNodeByLooseKey) {
        if (methodCalls == null || methodCalls.isEmpty()) {
            return;
        }
        List<MethodReference.Handle> callers = new ArrayList<>(methodCalls.keySet());
        callers.sort(METHOD_HANDLE_COMPARATOR);
        for (MethodReference.Handle caller : callers) {
            Collection<MethodReference.Handle> raw = methodCalls.get(caller);
            if (raw == null || raw.isEmpty()) {
                continue;
            }
            List<MethodReference.Handle> callees = new ArrayList<>(raw);
            callees.sort(METHOD_HANDLE_COMPARATOR);
            Long srcNode = resolveMethodNode(methodNodeByKey, methodNodeByLooseKey, caller, normalizeJarId(caller == null ? null : caller.getJarId()));
            if (srcNode == null || srcNode <= 0L) {
                continue;
            }
            for (MethodReference.Handle callee : callees) {
                Long dstNode = resolveMethodNode(methodNodeByKey, methodNodeByLooseKey, callee, normalizeJarId(callee == null ? null : callee.getJarId()));
                if (dstNode == null || dstNode <= 0L) {
                    continue;
                }
                MethodCallMeta meta = methodCallMeta == null ? null : MethodCallMeta.resolve(methodCallMeta, caller, callee);
                String relation = GraphRelationType.fromEdgeType(meta == null ? MethodCallMeta.TYPE_DIRECT : meta.getType()).name();
                String confidence = meta == null ? "low" : safe(meta.getConfidence());
                if (confidence.isBlank()) {
                    confidence = "low";
                }
                String evidence = meta == null ? "" : safe(meta.getEvidence());
                int opCode = resolveOpcode(meta, caller, callee);
                addEdge(edgeMap, srcNode, dstNode, relation, confidence, evidence, opCode);
            }
        }
    }

    private static void collectCallSiteEdges(Map<EdgeKey, EdgeKey> edgeMap,
                                             List<CallSiteRowWithNode> callSites,
                                             Map<MethodKey, Long> methodNodeByKey,
                                             Map<MethodLooseKey, Long> methodNodeByLooseKey) {
        if (callSites == null || callSites.isEmpty()) {
            return;
        }
        for (CallSiteRowWithNode row : callSites) {
            if (row == null || row.row == null || row.nodeId <= 0L) {
                continue;
            }
            Long callerNode = resolveMethodNode(methodNodeByKey, methodNodeByLooseKey,
                    row.row.callerClass, row.row.callerMethod, row.row.callerDesc, row.row.jarId);
            if (callerNode != null && callerNode > 0L) {
                addEdge(edgeMap,
                        callerNode,
                        row.nodeId,
                        GraphRelationType.CONTAINS_CALLSITE.name(),
                        "high",
                        "callsite",
                        row.row.opCode);
            }
            Long calleeNode = resolveMethodNode(methodNodeByKey, methodNodeByLooseKey,
                    row.row.calleeClass, row.row.calleeMethod, row.row.calleeDesc, -1);
            if (calleeNode != null && calleeNode > 0L) {
                addEdge(edgeMap,
                        row.nodeId,
                        calleeNode,
                        GraphRelationType.CALLSITE_TO_CALLEE.name(),
                        "medium",
                        "bytecode",
                        row.row.opCode);
            }
        }
    }

    private static void collectNextCallSiteEdges(Map<EdgeKey, EdgeKey> edgeMap,
                                                 List<CallSiteRowWithNode> callSites) {
        if (callSites == null || callSites.isEmpty()) {
            return;
        }
        Map<String, List<CallSiteRowWithNode>> grouped = new LinkedHashMap<>();
        for (CallSiteRowWithNode row : callSites) {
            if (row == null || row.row == null || row.nodeId <= 0L) {
                continue;
            }
            String key = row.row.callerClass + "#" + row.row.callerMethod + "#" + row.row.callerDesc + "#" + row.row.jarId;
            grouped.computeIfAbsent(key, ignore -> new ArrayList<>()).add(row);
        }
        for (List<CallSiteRowWithNode> rows : grouped.values()) {
            rows.sort((a, b) -> {
                int cmp = Integer.compare(normalizeSortInt(a.row.callIndex), normalizeSortInt(b.row.callIndex));
                if (cmp != 0) {
                    return cmp;
                }
                cmp = Integer.compare(normalizeSortInt(a.row.lineNumber), normalizeSortInt(b.row.lineNumber));
                if (cmp != 0) {
                    return cmp;
                }
                cmp = a.row.callSiteKey.compareTo(b.row.callSiteKey);
                if (cmp != 0) {
                    return cmp;
                }
                return Integer.compare(a.row.ordinal, b.row.ordinal);
            });
            for (int i = 0; i + 1 < rows.size(); i++) {
                CallSiteRowWithNode cur = rows.get(i);
                CallSiteRowWithNode next = rows.get(i + 1);
                if (cur.nodeId == next.nodeId) {
                    continue;
                }
                addEdge(edgeMap,
                        cur.nodeId,
                        next.nodeId,
                        GraphRelationType.NEXT_CALLSITE.name(),
                        "high",
                        "order",
                        -1);
            }
        }
    }

    private static void addEdge(Map<EdgeKey, EdgeKey> edgeMap,
                                long srcId,
                                long dstId,
                                String relType,
                                String confidence,
                                String evidence,
                                int opCode) {
        if (edgeMap == null || srcId <= 0L || dstId <= 0L) {
            return;
        }
        String relation = safe(relType);
        if (relation.isBlank()) {
            relation = GraphRelationType.CALLS_DIRECT.name();
        }
        String conf = safe(confidence);
        if (conf.isBlank()) {
            conf = "low";
        }
        EdgeKey key = new EdgeKey(
                srcId,
                dstId,
                relation,
                conf,
                safe(evidence),
                opCode
        );
        edgeMap.putIfAbsent(key, key);
    }

    private static int resolveOpcode(MethodCallMeta meta,
                                     MethodReference.Handle caller,
                                     MethodReference.Handle callee) {
        if (meta != null && meta.getBestOpcode() > 0) {
            return meta.getBestOpcode();
        }
        Integer calleeOpcode = callee == null ? null : callee.getOpcode();
        if (calleeOpcode != null && calleeOpcode > 0) {
            return calleeOpcode;
        }
        Integer callerOpcode = caller == null ? null : caller.getOpcode();
        if (callerOpcode != null && callerOpcode > 0) {
            return callerOpcode;
        }
        return -1;
    }

    private static List<MethodReference> sortedMethods(Set<MethodReference> methods) {
        if (methods == null || methods.isEmpty()) {
            return List.of();
        }
        List<MethodReference> out = new ArrayList<>(methods);
        out.sort(METHOD_REFERENCE_COMPARATOR);
        return out;
    }

    private static List<CallSiteRow> sortedCallSites(List<CallSiteEntity> callSites) {
        if (callSites == null || callSites.isEmpty()) {
            return List.of();
        }
        List<CallSiteRow> out = new ArrayList<>();
        for (int i = 0; i < callSites.size(); i++) {
            CallSiteEntity site = callSites.get(i);
            if (site == null) {
                continue;
            }
            String callerClass = safe(site.getCallerClassName());
            String callerMethod = safe(site.getCallerMethodName());
            String callerDesc = safe(site.getCallerMethodDesc());
            String calleeClass = safe(site.getCalleeOwner());
            String calleeMethod = safe(site.getCalleeMethodName());
            String calleeDesc = safe(site.getCalleeMethodDesc());
            int opCode = site.getOpCode() == null ? -1 : site.getOpCode();
            int lineNumber = site.getLineNumber() == null ? -1 : site.getLineNumber();
            int callIndex = site.getCallIndex() == null ? -1 : site.getCallIndex();
            int jarId = normalizeJarId(site.getJarId());
            String key = safe(site.getCallSiteKey());
            if (key.isBlank()) {
                key = CallSiteKeyUtil.buildCallSiteKey(site);
            }
            if (key.isBlank()) {
                key = syntheticCallSiteKey(
                        callerClass,
                        callerMethod,
                        callerDesc,
                        calleeClass,
                        calleeMethod,
                        calleeDesc,
                        jarId,
                        opCode,
                        callIndex,
                        lineNumber,
                        i
                );
            }
            out.add(new CallSiteRow(
                    key,
                    callerClass,
                    callerMethod,
                    callerDesc,
                    calleeClass,
                    calleeMethod,
                    calleeDesc,
                    opCode,
                    lineNumber,
                    callIndex,
                    jarId,
                    i
            ));
        }
        out.sort((a, b) -> {
            int cmp = a.callerClass.compareTo(b.callerClass);
            if (cmp != 0) {
                return cmp;
            }
            cmp = a.callerMethod.compareTo(b.callerMethod);
            if (cmp != 0) {
                return cmp;
            }
            cmp = a.callerDesc.compareTo(b.callerDesc);
            if (cmp != 0) {
                return cmp;
            }
            cmp = Integer.compare(normalizeSortInt(a.callIndex), normalizeSortInt(b.callIndex));
            if (cmp != 0) {
                return cmp;
            }
            cmp = Integer.compare(normalizeSortInt(a.lineNumber), normalizeSortInt(b.lineNumber));
            if (cmp != 0) {
                return cmp;
            }
            cmp = a.callSiteKey.compareTo(b.callSiteKey);
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(a.ordinal, b.ordinal);
        });
        return out;
    }

    private static String syntheticCallSiteKey(String callerClass,
                                               String callerMethod,
                                               String callerDesc,
                                               String calleeClass,
                                               String calleeMethod,
                                               String calleeDesc,
                                               int jarId,
                                               int opCode,
                                               int callIndex,
                                               int lineNumber,
                                               int ordinal) {
        return safe(callerClass) + "#" +
                safe(callerMethod) + "#" +
                safe(callerDesc) + "#" +
                safe(calleeClass) + "#" +
                safe(calleeMethod) + "#" +
                safe(calleeDesc) + "#" +
                jarId + "#" +
                opCode + "#" +
                callIndex + "#" +
                lineNumber + "#" +
                ordinal;
    }

    private static MethodKey toMethodKey(MethodReference method) {
        if (method == null) {
            return new MethodKey("", "", "", -1);
        }
        return new MethodKey(
                safe(method.getClassReference() == null ? null : method.getClassReference().getName()),
                safe(method.getName()),
                safe(method.getDesc()),
                normalizeJarId(resolveMethodJar(method))
        );
    }

    private static Integer resolveMethodJar(MethodReference method) {
        if (method == null) {
            return -1;
        }
        Integer jarId = method.getJarId();
        if (jarId != null && jarId >= 0) {
            return jarId;
        }
        if (method.getClassReference() != null) {
            return method.getClassReference().getJarId();
        }
        return -1;
    }

    private static Long resolveMethodNode(Map<MethodKey, Long> methodNodeByKey,
                                          Map<MethodLooseKey, Long> methodNodeByLooseKey,
                                          MethodReference.Handle handle,
                                          int jarId) {
        if (handle == null) {
            return null;
        }
        return resolveMethodNode(
                methodNodeByKey,
                methodNodeByLooseKey,
                safe(handle.getClassReference() == null ? null : handle.getClassReference().getName()),
                safe(handle.getName()),
                safe(handle.getDesc()),
                jarId
        );
    }

    private static Long resolveMethodNode(Map<MethodKey, Long> methodNodeByKey,
                                          Map<MethodLooseKey, Long> methodNodeByLooseKey,
                                          String className,
                                          String methodName,
                                          String methodDesc,
                                          int jarId) {
        MethodKey exact = new MethodKey(safe(className), safe(methodName), safe(methodDesc), normalizeJarId(jarId));
        Long node = methodNodeByKey.get(exact);
        if (node != null) {
            return node;
        }
        return methodNodeByLooseKey.get(new MethodLooseKey(exact.className, exact.methodName, exact.methodDesc));
    }

    private static int normalizeJarId(Integer jarId) {
        if (jarId == null) {
            return -1;
        }
        return jarId;
    }

    private static int normalizeSortInt(int value) {
        return value < 0 ? Integer.MAX_VALUE : value;
    }

    private static void addLabel(Map<String, List<GraphNode>> labelIndex, String label, GraphNode node) {
        if (labelIndex == null || node == null) {
            return;
        }
        String key = safe(label).toLowerCase();
        if (key.isBlank()) {
            return;
        }
        labelIndex.computeIfAbsent(key, ignore -> new ArrayList<>()).add(node);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public record GraphBuildStats(int methodNodes, int callSiteNodes, int edgeCount) {
    }

    private record GraphAssembly(GraphSnapshot snapshot,
                                 int methodNodeCount,
                                 int callSiteNodeCount) {
    }

    private record MethodKey(String className, String methodName, String methodDesc, int jarId) {
    }

    private record MethodLooseKey(String className, String methodName, String methodDesc) {
    }

    private record CallSiteRow(String callSiteKey,
                               String callerClass,
                               String callerMethod,
                               String callerDesc,
                               String calleeClass,
                               String calleeMethod,
                               String calleeDesc,
                               int opCode,
                               int lineNumber,
                               int callIndex,
                               int jarId,
                               int ordinal) {
    }

    private record CallSiteRowWithNode(CallSiteRow row, long nodeId) {
    }

    private record EdgeKey(long srcId,
                           long dstId,
                           String relType,
                           String confidence,
                           String evidence,
                           int opCode) {
    }

    private static final Comparator<MethodReference> METHOD_REFERENCE_COMPARATOR = Comparator
            .comparing((MethodReference method) -> safe(method == null || method.getClassReference() == null ? null : method.getClassReference().getName()))
            .thenComparing(method -> safe(method == null ? null : method.getName()))
            .thenComparing(method -> safe(method == null ? null : method.getDesc()))
            .thenComparingInt(method -> normalizeJarId(resolveMethodJar(method)));

    private static final Comparator<MethodReference.Handle> METHOD_HANDLE_COMPARATOR = Comparator
            .comparing((MethodReference.Handle handle) -> safe(handle == null || handle.getClassReference() == null ? null : handle.getClassReference().getName()))
            .thenComparing(handle -> safe(handle == null ? null : handle.getName()))
            .thenComparing(handle -> safe(handle == null ? null : handle.getDesc()))
            .thenComparingInt(handle -> normalizeJarId(handle == null ? null : handle.getJarId()))
            .thenComparingInt(handle -> {
                if (handle == null || handle.getOpcode() == null) {
                    return -1;
                }
                return handle.getOpcode();
            });
}
