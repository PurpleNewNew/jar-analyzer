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
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.rules.SourceModel;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.neo4j.cli.AdminTool;
import org.neo4j.cli.ExecutionContext;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

public final class Neo4jBulkImportService {
    private static final Logger logger = LogManager.getLogger();
    private static final String KEEP_STAGING_PROP = "jar.analyzer.neo4j.bulk.keepStaging";
    private static final String MAX_OFF_HEAP_PROP = "jar.analyzer.neo4j.bulk.maxOffHeapMemory";
    private static final String THREADS_PROP = "jar.analyzer.neo4j.bulk.threads";

    public Neo4jGraphBuildService.GraphBuildStats replaceFromAnalysis(
            long buildSeq,
            boolean quickMode,
            String callGraphMode,
            Set<MethodReference> methods,
            Map<MethodReference.Handle, ? extends Set<MethodReference.Handle>> methodCalls,
            Map<MethodCallKey, MethodCallMeta> methodCallMeta,
            List<CallSiteEntity> callSites) {
        String projectKey = ActiveProjectContext.getActiveProjectKey();
        String normalized = ActiveProjectContext.normalizeProjectKey(projectKey);
        Neo4jProjectStore store = Neo4jProjectStore.getInstance();
        Path projectHome = store.resolveProjectHome(normalized);
        Path stagingDir = projectHome
                .resolve("import-staging")
                .resolve("build-" + buildSeq + "-" + System.nanoTime());
        boolean keepStaging = keepStagingFiles();
        long startNs = System.nanoTime();
        CsvBuildResult csvResult = null;
        try {
            Files.createDirectories(stagingDir);
            Path nodeFile = stagingDir.resolve("nodes.csv");
            Path relFile = stagingDir.resolve("rels.csv");
            csvResult = writeCsvPayload(nodeFile, relFile, methods, methodCalls, methodCallMeta, callSites);

            store.closeProject(normalized);
            runFullImport(projectHome, csvResult.nodesFile(), csvResult.relationshipsFile(), stagingDir.resolve("import.report"));

            writeBuildMeta(
                    normalized,
                    buildSeq,
                    quickMode,
                    callGraphMode,
                    csvResult.methodNodes() + csvResult.callSiteNodes(),
                    csvResult.edgeCount()
            );
            Neo4jGraphSnapshotLoader.invalidate(normalized);

            int edgeCountInt = safeToInt(csvResult.edgeCount());
            if (csvResult.edgeCount() > Integer.MAX_VALUE) {
                logger.warn("neo4j bulk edge count overflow int: key={} edgeCount={}", normalized, csvResult.edgeCount());
            }
            Neo4jGraphBuildService.GraphBuildStats stats = new Neo4jGraphBuildService.GraphBuildStats(
                    csvResult.methodNodes(),
                    csvResult.callSiteNodes(),
                    edgeCountInt
            );
            logger.info("neo4j bulk build finish: key={} buildSeq={} methodNodes={} callSiteNodes={} edges={} elapsedMs={}",
                    normalized,
                    buildSeq,
                    stats.methodNodes(),
                    stats.callSiteNodes(),
                    csvResult.edgeCount(),
                    msSince(startNs));
            return stats;
        } catch (Exception ex) {
            logger.error("neo4j bulk build fail: key={} buildSeq={} err={}", normalized, buildSeq, ex.toString(), ex);
            throw ex instanceof RuntimeException
                    ? (RuntimeException) ex
                    : new IllegalStateException("neo4j_bulk_import_failed", ex);
        } finally {
            if (!keepStaging) {
                deleteRecursively(stagingDir);
            } else {
                logger.info("keep neo4j bulk staging files: {}", stagingDir);
            }
        }
    }

    private CsvBuildResult writeCsvPayload(
            Path nodeFile,
            Path relFile,
            Set<MethodReference> methods,
            Map<MethodReference.Handle, ? extends Set<MethodReference.Handle>> methodCalls,
            Map<MethodCallKey, MethodCallMeta> methodCallMeta,
            List<CallSiteEntity> callSites) throws IOException {
        Map<MethodKey, Long> methodNodeByKey = new LinkedHashMap<>();
        Map<MethodLooseKey, Long> methodNodeByLooseKey = new LinkedHashMap<>();
        Map<String, Long> callSiteNodeByKey = new LinkedHashMap<>();
        long nextNodeId = 1L;
        long nextEdgeId = 1L;
        int methodNodeCount = 0;
        int callSiteNodeCount = 0;
        long edgeCount = 0L;

        CSVFormat nodeFormat = CSVFormat.DEFAULT.builder()
                .setHeader(
                        "node_id:ID(JANode-ID)",
                        "kind",
                        "jar_id:int",
                        "class_name",
                        "method_name",
                        "method_desc",
                        "call_site_key",
                        "line_number:int",
                        "call_index:int",
                        "source_flags:int",
                        ":LABEL"
                )
                .setRecordSeparator('\n')
                .build();
        CSVFormat relFormat = CSVFormat.DEFAULT.builder()
                .setHeader(
                        "src:START_ID(JANode-ID)",
                        "dst:END_ID(JANode-ID)",
                        ":TYPE",
                        "edge_id:long",
                        "rel_type",
                        "confidence",
                        "evidence",
                        "op_code:int"
                )
                .setRecordSeparator('\n')
                .build();

        try (BufferedWriter nodeWriter = Files.newBufferedWriter(
                nodeFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
             BufferedWriter relWriter = Files.newBufferedWriter(
                     relFile,
                     StandardCharsets.UTF_8,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING,
                     StandardOpenOption.WRITE);
             CSVPrinter nodePrinter = new CSVPrinter(nodeWriter, nodeFormat);
             CSVPrinter relPrinter = new CSVPrinter(relWriter, relFormat)) {
            SourceMarkerIndex sourceMarkerIndex = buildSourceMarkerIndex();
            List<MethodReference> sortedMethods = sortedMethods(methods);
            for (MethodReference method : sortedMethods) {
                MethodKey key = toMethodKey(method);
                if (methodNodeByKey.containsKey(key)) {
                    continue;
                }
                long nodeId = nextNodeId++;
                int sourceFlags = resolveSourceFlags(method, sourceMarkerIndex);
                nodePrinter.printRecord(
                        nodeId,
                        "method",
                        key.jarId,
                        key.className,
                        key.methodName,
                        key.methodDesc,
                        "",
                        -1,
                        -1,
                        sourceFlags,
                        methodLabels(sourceFlags)
                );
                methodNodeByKey.put(key, nodeId);
                methodNodeByLooseKey.putIfAbsent(new MethodLooseKey(key.className, key.methodName, key.methodDesc), nodeId);
                methodNodeCount++;
            }

            if (methodCalls != null && !methodCalls.isEmpty()) {
                List<MethodReference.Handle> callers = new ArrayList<>(methodCalls.keySet());
                callers.sort(METHOD_HANDLE_COMPARATOR);
                for (MethodReference.Handle caller : callers) {
                    Collection<MethodReference.Handle> raw = methodCalls.get(caller);
                    if (raw == null || raw.isEmpty()) {
                        continue;
                    }
                    List<MethodReference.Handle> callees = new ArrayList<>(raw);
                    callees.sort(METHOD_HANDLE_COMPARATOR);
                    Long srcNode = resolveMethodNode(
                            methodNodeByKey,
                            methodNodeByLooseKey,
                            caller,
                            normalizeJarId(caller == null ? null : caller.getJarId())
                    );
                    if (srcNode == null || srcNode <= 0L) {
                        continue;
                    }
                    for (MethodReference.Handle callee : callees) {
                        Long dstNode = resolveMethodNode(
                                methodNodeByKey,
                                methodNodeByLooseKey,
                                callee,
                                normalizeJarId(callee == null ? null : callee.getJarId())
                        );
                        if (dstNode == null || dstNode <= 0L) {
                            continue;
                        }
                        MethodCallMeta meta = methodCallMeta == null
                                ? null
                                : MethodCallMeta.resolve(methodCallMeta, caller, callee);
                        String relation = GraphRelationType.fromEdgeType(
                                meta == null ? MethodCallMeta.TYPE_DIRECT : meta.getType()
                        ).name();
                        String confidence = meta == null ? "low" : safe(meta.getConfidence());
                        if (confidence.isBlank()) {
                            confidence = "low";
                        }
                        String evidence = meta == null ? "" : safe(meta.getEvidence());
                        int opCode = resolveOpcode(meta, caller, callee);
                        relPrinter.printRecord(
                                srcNode,
                                dstNode,
                                sanitizeRelationshipType(relation),
                                nextEdgeId++,
                                relation,
                                confidence,
                                evidence,
                                opCode
                        );
                        edgeCount++;
                    }
                }
            }

            List<CallSiteRow> sortedCallSites = sortedCallSites(callSites);
            String previousGroupKey = "";
            long previousNodeId = -1L;
            for (CallSiteRow row : sortedCallSites) {
                Long callSiteNodeId = callSiteNodeByKey.get(row.callSiteKey);
                if (callSiteNodeId == null) {
                    callSiteNodeId = nextNodeId++;
                    callSiteNodeByKey.put(row.callSiteKey, callSiteNodeId);
                    nodePrinter.printRecord(
                            callSiteNodeId,
                            "callsite",
                            row.jarId,
                            row.callerClass,
                            row.callerMethod,
                            row.callerDesc,
                            row.callSiteKey,
                            row.lineNumber,
                            row.callIndex,
                            0,
                            "JANode;CallSite"
                    );
                    callSiteNodeCount++;
                }

                Long callerNode = resolveMethodNode(
                        methodNodeByKey,
                        methodNodeByLooseKey,
                        row.callerClass,
                        row.callerMethod,
                        row.callerDesc,
                        row.jarId
                );
                if (callerNode != null && callerNode > 0L) {
                    String relation = GraphRelationType.CONTAINS_CALLSITE.name();
                    relPrinter.printRecord(
                            callerNode,
                            callSiteNodeId,
                            sanitizeRelationshipType(relation),
                            nextEdgeId++,
                            relation,
                            "high",
                            "callsite",
                            row.opCode
                    );
                    edgeCount++;
                }

                Long calleeNode = resolveMethodNode(
                        methodNodeByKey,
                        methodNodeByLooseKey,
                        row.calleeClass,
                        row.calleeMethod,
                        row.calleeDesc,
                        -1
                );
                if (calleeNode != null && calleeNode > 0L) {
                    String relation = GraphRelationType.CALLSITE_TO_CALLEE.name();
                    relPrinter.printRecord(
                            callSiteNodeId,
                            calleeNode,
                            sanitizeRelationshipType(relation),
                            nextEdgeId++,
                            relation,
                            "medium",
                            "bytecode",
                            row.opCode
                    );
                    edgeCount++;
                }

                String groupKey = row.callerClass + "#" + row.callerMethod + "#" + row.callerDesc + "#" + row.jarId;
                if (groupKey.equals(previousGroupKey)
                        && previousNodeId > 0L
                        && previousNodeId != callSiteNodeId) {
                    String relation = GraphRelationType.NEXT_CALLSITE.name();
                    relPrinter.printRecord(
                            previousNodeId,
                            callSiteNodeId,
                            sanitizeRelationshipType(relation),
                            nextEdgeId++,
                            relation,
                            "high",
                            "order",
                            -1
                    );
                    edgeCount++;
                }
                previousGroupKey = groupKey;
                previousNodeId = callSiteNodeId;
            }
            nodePrinter.flush();
            relPrinter.flush();
        }
        return new CsvBuildResult(nodeFile, relFile, methodNodeCount, callSiteNodeCount, edgeCount);
    }

    private void runFullImport(Path projectHome, Path nodeFile, Path relFile, Path reportFile) {
        Path confDir = ensureConf(projectHome);
        List<String> args = new ArrayList<>();
        args.add("database");
        args.add("import");
        args.add("full");
        args.add("--overwrite-destination=true");
        args.add("--input-type=csv");
        args.add("--nodes=" + nodeFile.toAbsolutePath().normalize());
        args.add("--relationships=" + relFile.toAbsolutePath().normalize());
        args.add("--report-file=" + reportFile.toAbsolutePath().normalize());
        String maxOffHeap = safe(System.getProperty(MAX_OFF_HEAP_PROP));
        if (!maxOffHeap.isBlank()) {
            args.add("--max-off-heap-memory=" + maxOffHeap);
        }
        Integer threads = parsePositiveInt(System.getProperty(THREADS_PROP));
        if (threads != null) {
            args.add("--threads=" + threads);
        }
        args.add(GraphDatabaseSettings.DEFAULT_DATABASE_NAME);

        ExecutionContext context = new ExecutionContext(projectHome, confDir);
        int exitCode = AdminTool.execute(context, args.toArray(new String[0]));
        if (exitCode != 0) {
            throw new IllegalStateException("neo4j_admin_import_failed_exit_" + exitCode);
        }
    }

    private static Path ensureConf(Path projectHome) {
        if (projectHome == null) {
            throw new IllegalStateException("neo4j_project_home_missing");
        }
        try {
            Files.createDirectories(projectHome);
            Path conf = projectHome.resolve("conf");
            Files.createDirectories(conf);
            return conf;
        } catch (Exception ex) {
            throw new IllegalStateException("neo4j_project_conf_prepare_failed", ex);
        }
    }

    private static void writeBuildMeta(String projectKey,
                                       long buildSeq,
                                       boolean quickMode,
                                       String callGraphMode,
                                       int nodeCount,
                                       long edgeCount) {
        GraphDatabaseService database = Neo4jProjectStore.getInstance().database(projectKey);
        if (database == null) {
            throw new IllegalStateException("neo4j_database_unavailable");
        }
        try (Transaction tx = database.beginTx()) {
            tx.execute("MATCH (m:JAMeta {key:'build_meta'}) DETACH DELETE m");
            Node meta = tx.createNode(Label.label("JAMeta"));
            meta.setProperty("key", "build_meta");
            meta.setProperty("build_seq", buildSeq);
            meta.setProperty("quick_mode", quickMode);
            meta.setProperty("call_graph_mode", safe(callGraphMode));
            meta.setProperty("node_count", nodeCount);
            meta.setProperty("edge_count", edgeCount);
            meta.setProperty("updated_at", System.currentTimeMillis());
            tx.commit();
        }
    }

    private static String methodLabels(int sourceFlags) {
        StringBuilder labels = new StringBuilder("JANode;Method");
        if ((sourceFlags & GraphNode.SOURCE_FLAG_ANY) != 0) {
            labels.append(";Source");
        }
        if ((sourceFlags & GraphNode.SOURCE_FLAG_WEB) != 0) {
            labels.append(";SourceWeb");
        }
        if ((sourceFlags & GraphNode.SOURCE_FLAG_MODEL) != 0) {
            labels.append(";SourceModel");
        }
        if ((sourceFlags & GraphNode.SOURCE_FLAG_ANNOTATION) != 0) {
            labels.append(";SourceAnno");
        }
        if ((sourceFlags & GraphNode.SOURCE_FLAG_RPC) != 0) {
            labels.append(";SourceRpc");
        }
        return labels.toString();
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
                if ("*".equals(desc)) {
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

    private static int normalizeJarId(Integer jarId) {
        if (jarId == null) {
            return -1;
        }
        return jarId;
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

    private static int normalizeSortInt(int value) {
        return value < 0 ? Integer.MAX_VALUE : value;
    }

    private static String sanitizeRelationshipType(String type) {
        String value = safe(type).trim();
        if (value.isBlank()) {
            return "RELATED";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == '_') {
                out.append(Character.toUpperCase(ch));
            } else {
                out.append('_');
            }
        }
        String normalized = out.toString();
        if (normalized.isBlank()) {
            return "RELATED";
        }
        return normalized;
    }

    private static boolean keepStagingFiles() {
        String raw = safe(System.getProperty(KEEP_STAGING_PROP)).toLowerCase(Locale.ROOT);
        return "1".equals(raw) || "true".equals(raw) || "yes".equals(raw) || "on".equals(raw);
    }

    private static Integer parsePositiveInt(String raw) {
        String value = safe(raw);
        if (value.isBlank()) {
            return null;
        }
        try {
            int number = Integer.parseInt(value);
            if (number > 0) {
                return number;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static void deleteRecursively(Path root) {
        if (root == null) {
            return;
        }
        try {
            if (!Files.exists(root)) {
                return;
            }
            try (var walk = Files.walk(root)) {
                walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception ignored) {
                                // best effort cleanup
                            }
                        });
            }
        } catch (Exception ex) {
            logger.debug("delete bulk staging fail: {}", ex.toString());
        }
    }

    private static int safeToInt(long value) {
        if (value <= 0L) {
            return 0;
        }
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) value;
    }

    private static long msSince(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record CsvBuildResult(Path nodesFile,
                                  Path relationshipsFile,
                                  int methodNodes,
                                  int callSiteNodes,
                                  long edgeCount) {
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
