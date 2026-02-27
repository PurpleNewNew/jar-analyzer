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
import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.graph.model.GraphRelationType;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.rules.SourceModel;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class Neo4jGraphBuildService {
    private static final Logger logger = LogManager.getLogger();
    private static final String WRITE_MODE_PROP = "jar.analyzer.neo4j.write.mode";
    private static final String WRITE_MODE_BULK = "bulk";
    private static final String WRITE_MODE_DIRECT = "direct";
    private static final String LABEL_METHOD = "method";
    private static final String LABEL_CALLSITE = "callsite";
    private static final String LABEL_SOURCE = "source";
    private static final String LABEL_SOURCE_WEB = "source_web";
    private static final String LABEL_SOURCE_MODEL = "source_model";
    private static final String LABEL_SOURCE_ANNO = "source_annotation";
    private static final String LABEL_SOURCE_RPC = "source_rpc";
    private final Neo4jGraphWriteService graphWriteService = new Neo4jGraphWriteService();
    private final Neo4jBulkImportService bulkImportService = new Neo4jBulkImportService();

    public GraphBuildStats replaceFromAnalysis(long buildSeq,
                                               boolean quickMode,
                                               String callGraphMode,
                                               Set<MethodReference> methods,
                                               Map<MethodReference.Handle, ? extends Set<MethodReference.Handle>> methodCalls,
                                               Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                               List<CallSiteEntity> callSites) {
        String writeMode = resolveWriteMode();
        if (WRITE_MODE_BULK.equals(writeMode)) {
            return bulkImportService.replaceFromAnalysis(
                    buildSeq,
                    quickMode,
                    callGraphMode,
                    methods,
                    methodCalls,
                    methodCallMeta,
                    callSites
            );
        }
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

    private static String resolveWriteMode() {
        String raw = safe(System.getProperty(WRITE_MODE_PROP)).toLowerCase(Locale.ROOT);
        if (WRITE_MODE_DIRECT.equals(raw)) {
            return WRITE_MODE_DIRECT;
        }
        if (!raw.isBlank() && !WRITE_MODE_BULK.equals(raw)) {
            logger.warn("unknown neo4j write mode: {}, fallback bulk", raw);
        }
        return WRITE_MODE_BULK;
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
        SourceMarkerIndex sourceMarkerIndex = buildSourceMarkerIndex();
        for (MethodReference method : sortedMethods) {
            MethodKey key = toMethodKey(method);
            if (methodNodeByKey.containsKey(key)) {
                continue;
            }
            long nodeId = nextNodeId++;
            int sourceFlags = resolveSourceFlags(method, sourceMarkerIndex);
            GraphNode node = new GraphNode(
                    nodeId,
                    "method",
                    key.jarId,
                    key.className,
                    key.methodName,
                    key.methodDesc,
                    "",
                    -1,
                    -1,
                    sourceFlags
            );
            nodeMap.put(nodeId, node);
            methodNodeByKey.put(key, nodeId);
            methodNodeByLooseKey.putIfAbsent(new MethodLooseKey(key.className, key.methodName, key.methodDesc), nodeId);
            addLabel(labelIndex, LABEL_METHOD, node);
            addSourceLabels(labelIndex, node);
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

    private static void addSourceLabels(Map<String, List<GraphNode>> labelIndex, GraphNode node) {
        if (labelIndex == null || node == null || !node.hasSourceFlag(GraphNode.SOURCE_FLAG_ANY)) {
            return;
        }
        addLabel(labelIndex, LABEL_SOURCE, node);
        if (node.hasSourceFlag(GraphNode.SOURCE_FLAG_WEB)) {
            addLabel(labelIndex, LABEL_SOURCE_WEB, node);
        }
        if (node.hasSourceFlag(GraphNode.SOURCE_FLAG_MODEL)) {
            addLabel(labelIndex, LABEL_SOURCE_MODEL, node);
        }
        if (node.hasSourceFlag(GraphNode.SOURCE_FLAG_ANNOTATION)) {
            addLabel(labelIndex, LABEL_SOURCE_ANNO, node);
        }
        if (node.hasSourceFlag(GraphNode.SOURCE_FLAG_RPC)) {
            addLabel(labelIndex, LABEL_SOURCE_RPC, node);
        }
    }

    private static SourceMarkerIndex buildSourceMarkerIndex() {
        Map<MethodLooseKey, Integer> modelFlags = new HashMap<>();
        List<SourceModel> sourceModels = ModelRegistry.getSourceModels();
        if (sourceModels != null) {
            for (SourceModel model : sourceModels) {
                if (model == null) {
                    continue;
                }
                String cls = safe(model.getClassName()).replace('.', '/');
                String method = safe(model.getMethodName());
                String desc = normalizeSourceModelDesc(model.getMethodDesc());
                if (cls.isBlank() || method.isBlank()) {
                    continue;
                }
                int flags = GraphNode.SOURCE_FLAG_MODEL;
                if (isWebSourceKind(model.getKind())) {
                    flags |= GraphNode.SOURCE_FLAG_WEB;
                }
                if (isRpcSourceKind(model.getKind())) {
                    flags |= GraphNode.SOURCE_FLAG_RPC;
                }
                if (desc.equals("*")) {
                    MethodLooseKey wildcard = new MethodLooseKey(cls, method, "*");
                    modelFlags.merge(wildcard, flags, (left, right) -> left | right);
                } else {
                    MethodLooseKey exact = new MethodLooseKey(cls, method, desc);
                    modelFlags.merge(exact, flags, (left, right) -> left | right);
                }
            }
        }

        Set<String> sourceAnnotations = new HashSet<>();
        List<String> sourceAnnoList = ModelRegistry.getSourceAnnotations();
        if (sourceAnnoList != null) {
            for (String raw : sourceAnnoList) {
                String normalized = normalizeAnnotationName(raw);
                if (!normalized.isBlank()) {
                    sourceAnnotations.add(normalized);
                }
            }
        }
        return new SourceMarkerIndex(modelFlags, sourceAnnotations);
    }

    private static int resolveSourceFlags(MethodReference method, SourceMarkerIndex markerIndex) {
        if (method == null) {
            return 0;
        }
        String cls = safe(method.getClassReference() == null ? null : method.getClassReference().getName()).replace('.', '/');
        String name = safe(method.getName());
        String desc = safe(method.getDesc());
        if (cls.isBlank() || name.isBlank() || desc.isBlank()) {
            return 0;
        }

        int flags = 0;
        if (markerIndex != null) {
            Map<MethodLooseKey, Integer> modelFlags = markerIndex.modelFlags();
            if (modelFlags != null && !modelFlags.isEmpty()) {
                Integer exact = modelFlags.get(new MethodLooseKey(cls, name, desc));
                if (exact != null) {
                    flags |= exact;
                }
                Integer wildcard = modelFlags.get(new MethodLooseKey(cls, name, "*"));
                if (wildcard != null) {
                    flags |= wildcard;
                }
            }
            Set<String> sourceAnnotations = markerIndex.sourceAnnotations();
            Set<AnnoReference> annos = method.getAnnotations();
            if (annos != null && sourceAnnotations != null && !sourceAnnotations.isEmpty()) {
                for (AnnoReference anno : annos) {
                    String normalized = normalizeAnnotationName(anno == null ? null : anno.getAnnoName());
                    if (normalized.isBlank() || !sourceAnnotations.contains(normalized)) {
                        continue;
                    }
                    flags |= GraphNode.SOURCE_FLAG_ANNOTATION;
                    if (isWebAnnotation(normalized)) {
                        flags |= GraphNode.SOURCE_FLAG_WEB;
                    }
                    if (isRpcAnnotation(normalized)) {
                        flags |= GraphNode.SOURCE_FLAG_RPC;
                    }
                }
            }
        }
        if (isServletEntry(name, desc)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        if (flags != 0) {
            flags |= GraphNode.SOURCE_FLAG_ANY;
        }
        return flags;
    }

    private static String normalizeSourceModelDesc(String desc) {
        String value = safe(desc);
        if (value.isBlank() || "null".equalsIgnoreCase(value)) {
            return "*";
        }
        return value;
    }

    private static String normalizeAnnotationName(String raw) {
        String value = safe(raw);
        if (value.isBlank()) {
            return "";
        }
        if (value.charAt(0) == '@') {
            value = value.substring(1);
        }
        value = value.replace('.', '/');
        if (!value.startsWith("L")) {
            value = "L" + value;
        }
        if (!value.endsWith(";")) {
            value = value + ";";
        }
        return value;
    }

    private static boolean isWebSourceKind(String kind) {
        String value = safe(kind).toLowerCase(Locale.ROOT);
        return value.contains("web")
                || value.contains("http")
                || value.contains("rest")
                || value.contains("servlet")
                || value.contains("controller");
    }

    private static boolean isRpcSourceKind(String kind) {
        String value = safe(kind).toLowerCase(Locale.ROOT);
        return value.contains("rpc")
                || value.contains("grpc")
                || value.contains("dubbo")
                || value.contains("webservice");
    }

    private static boolean isWebAnnotation(String annotation) {
        String value = safe(annotation).toLowerCase(Locale.ROOT);
        return value.contains("/springframework/web/")
                || value.contains("/ws/rs/")
                || value.contains("/servlet/");
    }

    private static boolean isRpcAnnotation(String annotation) {
        String value = safe(annotation).toLowerCase(Locale.ROOT);
        return value.contains("/dubbo/")
                || value.contains("/grpc/")
                || value.contains("/jws/webservice;");
    }

    private static boolean isServletEntry(String methodName, String methodDesc) {
        String name = safe(methodName);
        String desc = safe(methodDesc);
        if (name.isBlank() || desc.isBlank()) {
            return false;
        }
        if (!("doGet".equals(name) || "doPost".equals(name) || "doPut".equals(name)
                || "doDelete".equals(name) || "doHead".equals(name)
                || "doOptions".equals(name) || "doTrace".equals(name)
                || "service".equals(name))) {
            return false;
        }
        return "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V".equals(desc)
                || "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V".equals(desc)
                || "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)V".equals(desc)
                || "(Ljakarta/servlet/ServletRequest;Ljakarta/servlet/ServletResponse;)V".equals(desc);
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

    private record SourceMarkerIndex(Map<MethodLooseKey, Integer> modelFlags,
                                     Set<String> sourceAnnotations) {
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
