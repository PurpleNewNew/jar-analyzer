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

import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.core.WebEntryMethodSpec;
import me.n1ar4.jar.analyzer.core.WebEntryMethods;
import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.graph.model.GraphRelationType;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.rules.SourceRuleSupport;
import me.n1ar4.jar.analyzer.taint.AliasRuleSupport;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.neo4j.cli.ExecutionContext;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.importer.ImportCommand;
import org.neo4j.io.fs.DefaultFileSystemAbstraction;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class Neo4jBulkImportService {
    private static final Logger logger = LogManager.getLogger();
    private static final String KEEP_STAGING_PROP = "jar.analyzer.neo4j.bulk.keepStaging";
    private static final String MAX_OFF_HEAP_PROP = "jar.analyzer.neo4j.bulk.maxOffHeapMemory";
    private static final String THREADS_PROP = "jar.analyzer.neo4j.bulk.threads";
    private static final int IMPORT_LOG_TAIL_BYTES = 8192;
    private static final int IMPORT_ERR_SUMMARY_LIMIT = 480;
    private static final ProjectGraphStoreFacade PROJECT_STORE = ProjectGraphStoreFacade.getInstance();
    private static final String BUILD_META_KEY = "build_meta";
    private static final ExecutorService CLEANUP_EXECUTOR = Executors.newSingleThreadExecutor(
            Thread.ofPlatform()
                    .name("neo4j-bulk-cleanup-0")
                    .daemon(true)
                    .factory()
    );

    public interface StageObserver {
        void onStage(String stageKey, long durationMs, Map<String, Object> details);
    }

    public ProjectRuntimeSnapshot replaceFromAnalysis(String projectKey,
                                                     long buildSeq,
                                                     boolean quickMode,
                                                     String callGraphMode,
                                                     Set<MethodReference> methods,
                                                     Map<MethodReference.Handle, ? extends Set<MethodReference.Handle>> methodCalls,
                                                     Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                                     GraphPayloadData graphPayloadData,
                                                     Supplier<ProjectRuntimeSnapshot> runtimeSnapshotSupplier,
                                                     Map<String, Object> buildMeta,
                                                     StageObserver stageObserver) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        Path projectHome = PROJECT_STORE.resolveProjectHome(normalized);
        Path stagingHome = resolveStagingHome(projectHome, buildSeq);
        Path stagingDir = stagingHome
                .resolve("import-staging")
                .resolve("build-" + buildSeq + "-" + System.nanoTime());
        boolean keepStaging = keepStagingFiles();
        boolean buildSucceeded = false;
        boolean importLockHeld = false;
        long startNs = System.nanoTime();
        CsvBuildResult csvResult = null;
        Path backupHome = null;
        Path cleanupBuildArtifacts = null;
        ProjectRuntimeSnapshot runtimeSnapshot = null;
        try {
            Files.createDirectories(stagingDir);
            Path nodeFile = stagingDir.resolve("nodes.csv");
            Path relFile = stagingDir.resolve("rels.csv");
            long csvStartNs = System.nanoTime();
            csvResult = writeCsvPayload(
                    nodeFile,
                    relFile,
                    buildSeq,
                    quickMode,
                    callGraphMode,
                    methods,
                    methodCalls,
                    methodCallMeta,
                    graphPayloadData,
                    buildMeta
            );
            notifyStage(stageObserver, "neo4j_csv_payload", csvStartNs, detailMap(
                    "class_nodes", csvResult.classNodes(),
                    "method_nodes", csvResult.methodNodes(),
                    "alias_edges", csvResult.aliasEdges(),
                    "call_site_edges", csvResult.callSiteEdges(),
                    "edges", csvResult.edgeCount()
            ));
            logger.info("neo4j bulk csv payload written: key={} buildSeq={} classNodes={} methodNodes={} aliasEdges={} edges={} callSiteEdges={} heap={}",
                    normalized,
                    buildSeq,
                    csvResult.classNodes(),
                    csvResult.methodNodes(),
                    csvResult.aliasEdges(),
                    csvResult.edgeCount(),
                    csvResult.callSiteEdges(),
                    heapUsage());

            PROJECT_STORE.beginProjectImport(normalized);
            importLockHeld = true;
            long importStartNs = System.nanoTime();
            runFullImport(stagingHome, csvResult.nodesFile(), csvResult.relationshipsFile(), stagingDir.resolve("import.report"));
            notifyStage(stageObserver, "neo4j_bulk_import", importStartNs, detailMap(
                    "nodes", csvResult.totalNodes(),
                    "edges", csvResult.edgeCount(),
                    "call_site_edges", csvResult.callSiteEdges()
            ));
            logger.info("neo4j bulk import finished: key={} buildSeq={} heap={}",
                    normalized,
                    buildSeq,
                    heapUsage());

            try {
                long buildMetaStartNs = System.nanoTime();
                notifyStage(stageObserver, "neo4j_write_build_meta", buildMetaStartNs, detailMap(
                        "nodes", csvResult.totalNodes(),
                        "edges", csvResult.edgeCount(),
                        "strategy", "csv_import"
                ));
                runtimeSnapshot = requireRuntimeSnapshot(runtimeSnapshotSupplier);
                long persistSnapshotStartNs = System.nanoTime();
                ProjectMetadataSnapshotStore.getInstance().writeToHome(stagingHome, projectHome, runtimeSnapshot);
                notifyStage(stageObserver, "neo4j_persist_runtime_snapshot", persistSnapshotStartNs, detailMap(
                        "build_seq", buildSeq,
                        "runtime_jars", runtimeSnapshot.jars() == null ? 0 : runtimeSnapshot.jars().size(),
                        "class_files", runtimeSnapshot.classFiles() == null ? 0 : runtimeSnapshot.classFiles().size(),
                        "methods", runtimeSnapshot.methodReferences() == null ? 0 : runtimeSnapshot.methodReferences().size()
                ));
                logger.info("neo4j bulk metadata snapshot written: key={} buildSeq={} heap={}",
                        normalized,
                        buildSeq,
                        heapUsage());
                long swapStartNs = System.nanoTime();
                backupHome = replaceProjectHome(projectHome, stagingHome, buildSeq);
                cleanupBuildArtifacts = resolvePromotedBuildArtifacts(projectHome, stagingDir);
                notifyStage(stageObserver, "neo4j_swap_home", swapStartNs, detailMap(
                        "backup_created", backupHome != null
                ));
            } finally {
                Neo4jGraphSnapshotLoader.invalidate(normalized);
            }

            PROJECT_STORE.endProjectImport(normalized);
            importLockHeld = false;

            if (csvResult.edgeCount() > Integer.MAX_VALUE) {
                logger.warn("neo4j bulk edge count overflow int: key={} edgeCount={}", normalized, csvResult.edgeCount());
            }
            logger.info("neo4j bulk build finish: key={} buildSeq={} classNodes={} methodNodes={} aliasEdges={} callSiteEdges={} edges={} elapsedMs={}",
                    normalized,
                    buildSeq,
                    csvResult.classNodes(),
                    csvResult.methodNodes(),
                    csvResult.aliasEdges(),
                    csvResult.callSiteEdges(),
                    csvResult.edgeCount(),
                    msSince(startNs));
            buildSucceeded = true;
            return runtimeSnapshot;
        } catch (Exception ex) {
            logger.error("neo4j bulk build fail: key={} buildSeq={} err={}", normalized, buildSeq, ex.toString(), ex);
            throw ex instanceof RuntimeException
                    ? (RuntimeException) ex
                    : new IllegalStateException("neo4j_bulk_import_failed", ex);
        } finally {
            if (importLockHeld) {
                PROJECT_STORE.endProjectImport(normalized);
            }
            if (!keepStaging && buildSucceeded) {
                scheduleCleanup("neo4j backup home", backupHome);
                scheduleCleanup("neo4j import staging", cleanupBuildArtifacts);
            } else {
                deleteRecursively(backupHome);
                Path keptPath = buildSucceeded ? cleanupBuildArtifacts : stagingDir;
                logger.info("keep neo4j bulk staging files: {}", keptPath);
            }
            if (!buildSucceeded && !keepStaging) {
                deleteRecursively(stagingHome);
            }
        }
    }

    public void updateBuildMeta(String projectKey,
                                long buildSeq,
                                Map<String, Object> extraMeta) {
        if (extraMeta == null || extraMeta.isEmpty()) {
            return;
        }
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        try {
            PROJECT_STORE.write(normalized, 30_000L, tx -> {
                Node meta;
                try (var it = tx.findNodes(Label.label("JAMeta"), "key", BUILD_META_KEY)) {
                    if (it.hasNext()) {
                        meta = it.next();
                    } else {
                        meta = tx.createNode(Label.label("JAMeta"));
                        meta.setProperty("key", BUILD_META_KEY);
                        meta.setProperty("build_seq", buildSeq);
                    }
                }
                meta.setProperty("updated_at", System.currentTimeMillis());
                applyExtraBuildMeta(meta, extraMeta);
                return null;
            });
        } catch (Exception ex) {
            throw new IllegalStateException("neo4j_build_meta_update_failed", ex);
        }
    }

    private CsvBuildResult writeCsvPayload(
            Path nodeFile,
            Path relFile,
            long buildSeq,
            boolean quickMode,
            String callGraphMode,
            Set<MethodReference> methods,
            Map<MethodReference.Handle, ? extends Set<MethodReference.Handle>> methodCalls,
            Map<MethodCallKey, MethodCallMeta> methodCallMeta,
            GraphPayloadData graphPayloadData,
            Map<String, Object> buildMeta) throws IOException {
        Map<MethodKey, Long> methodNodeByKey = new LinkedHashMap<>();
        Map<MethodLooseKey, Long> methodNodeByLooseKey = new LinkedHashMap<>();
        Set<MethodLooseKey> ambiguousMethodLooseKeys = new HashSet<>();
        Map<CallSiteLookupKey, List<CallSiteProjection>> callSiteIndex = buildCallSiteIndex(
                graphPayloadData == null ? null : graphPayloadData.callSites()
        );
        long nextNodeId = 1L;
        long nextEdgeId = 1L;
        int classNodeCount = 0;
        int methodNodeCount = 0;
        long edgeCount = 0L;
        long aliasEdgeCount = 0L;
        long callSiteEdgeCount = 0L;
        Map<ClassKey, Long> classNodeByKey = new LinkedHashMap<>();
        Map<String, Long> classNodeByName = new LinkedHashMap<>();
        Set<String> ambiguousClassNames = new HashSet<>();

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
                        "method_semantic_flags:int",
                        "is_interface:boolean",
                        "is_abstract:boolean",
                        "key",
                        "build_seq:long",
                        "quick_mode:boolean",
                        "call_graph_mode",
                        "node_count:int",
                        "edge_count:long",
                        "updated_at:long",
                        "call_graph_engine",
                        "analysis_profile",
                        "target_jar_count:int",
                        "library_jar_count:int",
                        "sdk_entry_count:int",
                        "explicit_entry_method_count:int",
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
                        "alias_kind",
                        "op_code:int",
                        "edge_semantic_flags:int",
                        "call_site_key",
                        "line_number:int",
                        "call_index:int"
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
            SourceMarkerIndex sourceMarkerIndex = buildSourceMarkerIndex(graphPayloadData);
            List<ClassReference> sortedClasses = sortedClasses(graphPayloadData == null ? null : graphPayloadData.classReferences());
            for (ClassReference classReference : sortedClasses) {
                ClassKey key = toClassKey(classReference);
                if (classNodeByKey.containsKey(key)) {
                    continue;
                }
                long nodeId = nextNodeId++;
                nodePrinter.printRecord(
                        nodeId,
                        "class",
                        key.jarId,
                        key.className,
                        "",
                        "",
                        "",
                        -1,
                        -1,
                        -1,
                        0,
                        classReference != null && classReference.isInterface(),
                        isAbstractClass(classReference),
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        classLabels()
                );
                classNodeByKey.put(key, nodeId);
                registerLooseClassNode(classNodeByName, ambiguousClassNames, key.className, nodeId);
                classNodeCount++;
            }
            for (ClassReference classReference : sortedClasses) {
                Long srcNode = resolveClassNode(
                        classNodeByKey,
                        classNodeByName,
                        ambiguousClassNames,
                        classReference == null ? null : classReference.getName(),
                        classReference == null ? null : classReference.getJarId()
                );
                if (srcNode == null || srcNode <= 0L) {
                    continue;
                }
                String superClass = classReference == null ? "" : safe(classReference.getSuperClass());
                if (!superClass.isBlank()) {
                    Long dstNode = resolveClassNode(
                            classNodeByKey,
                            classNodeByName,
                            ambiguousClassNames,
                            superClass,
                            classReference == null ? null : classReference.getJarId()
                    );
                    if (dstNode != null && dstNode > 0L) {
                        relPrinter.printRecord(
                                srcNode,
                                dstNode,
                                "EXTEND",
                                nextEdgeId++,
                                "EXTEND",
                                "",
                                "class_structure",
                                "",
                                -1,
                                0,
                                "",
                                -1,
                                -1
                        );
                        edgeCount++;
                    }
                }
                if (classReference == null || classReference.getInterfaces() == null) {
                    continue;
                }
                for (String interfaceName : classReference.getInterfaces()) {
                    Long dstNode = resolveClassNode(
                            classNodeByKey,
                            classNodeByName,
                            ambiguousClassNames,
                            interfaceName,
                            classReference.getJarId()
                    );
                    if (dstNode == null || dstNode <= 0L) {
                        continue;
                    }
                    relPrinter.printRecord(
                            srcNode,
                            dstNode,
                            "INTERFACES",
                            nextEdgeId++,
                            "INTERFACES",
                            "",
                            "class_structure",
                            "",
                            -1,
                            0,
                            "",
                            -1,
                            -1
                    );
                    edgeCount++;
                }
            }
            List<MethodReference> sortedMethods = sortedMethods(methods);
            for (MethodReference method : sortedMethods) {
                MethodKey key = toMethodKey(method);
                if (methodNodeByKey.containsKey(key)) {
                    continue;
                }
                long nodeId = nextNodeId++;
                int sourceFlags = resolveSourceFlags(method, sourceMarkerIndex);
                int methodSemanticFlags = Math.max(0, method.getSemanticFlags());
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
                        methodSemanticFlags,
                        false,
                        false,
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        methodLabels()
                );
                methodNodeByKey.put(key, nodeId);
                registerLooseMethodNode(
                        methodNodeByLooseKey,
                        ambiguousMethodLooseKeys,
                        new MethodLooseKey(key.className, key.methodName, key.methodDesc),
                        nodeId
                );
                methodNodeCount++;
                Long ownerNode = resolveClassNode(
                        classNodeByKey,
                        classNodeByName,
                        ambiguousClassNames,
                        key.className,
                        key.jarId
                );
                if (ownerNode != null && ownerNode > 0L) {
                    relPrinter.printRecord(
                            ownerNode,
                            nodeId,
                            "HAS",
                            nextEdgeId++,
                        "HAS",
                        "",
                                "class_structure",
                                "",
                                -1,
                                0,
                                "",
                                -1,
                                -1
                    );
                    edgeCount++;
                }
            }
            List<AliasRuleSupport.AliasEdge> aliasEdges = AliasRuleSupport.resolveAliasEdges(
                    methodCalls,
                    graphPayloadData == null ? List.of() : graphPayloadData.classReferences()
            );
            for (AliasRuleSupport.AliasEdge aliasEdge : aliasEdges) {
                if (aliasEdge == null || aliasEdge.source() == null || aliasEdge.target() == null) {
                    continue;
                }
                Long srcNode = resolveMethodNode(
                        methodNodeByKey,
                        methodNodeByLooseKey,
                        ambiguousMethodLooseKeys,
                        aliasEdge.source(),
                        normalizeJarId(aliasEdge.source().getJarId())
                );
                Long dstNode = resolveMethodNode(
                        methodNodeByKey,
                        methodNodeByLooseKey,
                        ambiguousMethodLooseKeys,
                        aliasEdge.target(),
                        normalizeJarId(aliasEdge.target().getJarId())
                );
                if (srcNode == null || srcNode <= 0L || dstNode == null || dstNode <= 0L || srcNode.equals(dstNode)) {
                    continue;
                }
                relPrinter.printRecord(
                        srcNode,
                        dstNode,
                        GraphRelationType.ALIAS.name(),
                        nextEdgeId++,
                        GraphRelationType.ALIAS.name(),
                        safe(aliasEdge.confidence()),
                        safe(aliasEdge.evidence()),
                        safe(aliasEdge.aliasKind()),
                        -1,
                        0,
                        "",
                        -1,
                        -1
                );
                edgeCount++;
                aliasEdgeCount++;
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
                            ambiguousMethodLooseKeys,
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
                                ambiguousMethodLooseKeys,
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
                        int edgeSemanticFlags = meta == null ? 0 : meta.getEvidenceBits();
                        int opCode = resolveOpcode(meta, caller, callee);
                        List<CallSiteProjection> projections = resolveCallSiteProjections(callSiteIndex, caller, callee, meta);
                        if (projections.isEmpty()) {
                            relPrinter.printRecord(
                                    srcNode,
                                    dstNode,
                                    sanitizeRelationshipType(relation),
                                    nextEdgeId++,
                                    relation,
                                    confidence,
                                    evidence,
                                    "",
                                    opCode,
                                    edgeSemanticFlags,
                                    "",
                                    -1,
                                    -1
                            );
                            edgeCount++;
                            continue;
                        }
                        for (CallSiteProjection projection : projections) {
                            relPrinter.printRecord(
                                    srcNode,
                                    dstNode,
                                    sanitizeRelationshipType(relation),
                                    nextEdgeId++,
                                    relation,
                                    confidence,
                                    evidence,
                                    "",
                                    projection.opCode() > 0 ? projection.opCode() : opCode,
                                    edgeSemanticFlags,
                                    projection.callSiteKey(),
                                    projection.lineNumber(),
                                    projection.callIndex()
                            );
                            edgeCount++;
                            callSiteEdgeCount++;
                        }
                    }
                }
            }
            BuildMetaCsvRow buildMetaRow = BuildMetaCsvRow.from(
                    buildSeq,
                    quickMode,
                    callGraphMode,
                    classNodeCount + methodNodeCount,
                    edgeCount,
                    buildMeta
            );
            nodePrinter.printRecord(
                    nextNodeId,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    BUILD_META_KEY,
                    buildMetaRow.buildSeq(),
                    buildMetaRow.quickMode(),
                    buildMetaRow.callGraphMode(),
                    buildMetaRow.nodeCount(),
                    buildMetaRow.edgeCount(),
                    buildMetaRow.updatedAt(),
                    buildMetaRow.callGraphEngine(),
                    buildMetaRow.analysisProfile(),
                    buildMetaRow.targetJarCount(),
                    buildMetaRow.libraryJarCount(),
                    buildMetaRow.sdkEntryCount(),
                    buildMetaRow.explicitEntryMethodCount(),
                    "JAMeta"
            );
            nodePrinter.flush();
            relPrinter.flush();
        }
        return new CsvBuildResult(nodeFile, relFile, classNodeCount, methodNodeCount, aliasEdgeCount, edgeCount, callSiteEdgeCount);
    }

    private void runFullImport(Path projectHome, Path nodeFile, Path relFile, Path reportFile) {
        Path confDir = ensureImportConf(projectHome, reportFile);
        Path logDir = resolveImportLogDir(projectHome, reportFile);
        Path stdoutLog = logDir.resolve("import.stdout.log");
        Path stderrLog = logDir.resolve("import.stderr.log");
        List<String> args = new ArrayList<>();
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

        int exitCode;
        AtomicReference<Throwable> executionError = new AtomicReference<>();
        try (OutputStream outRaw = Files.newOutputStream(
                stdoutLog,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
             OutputStream errRaw = Files.newOutputStream(
                     stderrLog,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING,
                     StandardOpenOption.WRITE);
             PrintStream out = new PrintStream(outRaw, true, StandardCharsets.UTF_8);
             PrintStream err = new PrintStream(errRaw, true, StandardCharsets.UTF_8);
             DefaultFileSystemAbstraction fs = new DefaultFileSystemAbstraction()) {
            ExecutionContext context = new ExecutionContext(projectHome, confDir, out, err, fs);
            ImportCommand.Full command = new ImportCommand.Full(context);
            CommandLine cli = new CommandLine(command);
            cli.setExecutionExceptionHandler((ex, cmd, parseResult) -> {
                executionError.set(ex);
                try {
                    ex.printStackTrace(err);
                    err.flush();
                } catch (Exception ignored) {
                    logger.debug("write neo4j import execution error to stderr log failed: {}", ignored.toString());
                }
                return cmd.getCommandSpec().exitCodeOnExecutionException();
            });
            exitCode = cli.execute(args.toArray(new String[0]));
        } catch (Exception ex) {
            throw new IllegalStateException("neo4j_admin_import_launch_failed", ex);
        }
        if (exitCode != 0) {
            Throwable commandError = executionError.get();
            String stderrTail = readTail(stderrLog, IMPORT_LOG_TAIL_BYTES);
            String stdoutTail = readTail(stdoutLog, IMPORT_LOG_TAIL_BYTES);
            String detail = stderrTail.isBlank() ? stdoutTail : stderrTail;
            String brief = summarizeOneLine(detail, IMPORT_ERR_SUMMARY_LIMIT);
            String throwableBrief = summarizeOneLine(summarizeThrowable(commandError), IMPORT_ERR_SUMMARY_LIMIT);
            if (brief.isBlank() || isLikelyPreambleOnly(brief)) {
                brief = throwableBrief;
            } else if (!throwableBrief.isBlank() && !brief.contains(throwableBrief)) {
                brief = brief + " | " + throwableBrief;
            }
            if (brief.isBlank()) {
                logger.error("neo4j admin import failed: exitCode={} (no output captured) stdoutLog={} stderrLog={}",
                        exitCode,
                        stdoutLog,
                        stderrLog);
                throw new IllegalStateException(
                        "neo4j_admin_import_failed_exit_" + exitCode
                                + " (no output captured, see logs: "
                                + stderrLog.toAbsolutePath().normalize()
                                + ")",
                        commandError
                );
            }
            logger.error("neo4j admin import failed: exitCode={} detail={} stdoutLog={} stderrLog={}",
                    exitCode,
                    brief,
                    stdoutLog,
                    stderrLog);
            throw new IllegalStateException(
                    "neo4j_admin_import_failed_exit_" + exitCode
                            + ": " + brief
                            + " (stderrLog=" + stderrLog.toAbsolutePath().normalize() + ")",
                    commandError
            );
        }
    }

    private static Path resolveImportLogDir(Path projectHome, Path reportFile) {
        Path stagingRoot = reportFile == null ? null : reportFile.toAbsolutePath().normalize().getParent();
        return stagingRoot == null ? projectHome : stagingRoot;
    }

    private static Path ensureImportConf(Path projectHome, Path reportFile) {
        if (projectHome == null) {
            throw new IllegalStateException("neo4j_project_home_missing");
        }
        Path stagingRoot = reportFile == null ? null : reportFile.toAbsolutePath().normalize().getParent();
        Path conf = stagingRoot == null ? projectHome.resolve("conf") : stagingRoot.resolve("neo4j-conf");
        try {
            Files.createDirectories(projectHome);
            Files.createDirectories(conf);
            Path confFile = conf.resolve("neo4j.conf");
            Files.writeString(
                    confFile,
                    "# generated by jar-analyzer for neo4j import\n"
                            + "server.config.strict_validation.enabled=false\n"
                            + "server.bolt.enabled=false\n"
                            + "server.http.enabled=false\n"
                            + "server.https.enabled=false\n",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            return conf;
        } catch (Exception ex) {
            throw new IllegalStateException("neo4j_project_conf_prepare_failed", ex);
        }
    }

    private static Path resolveStagingHome(Path projectHome, long buildSeq) {
        if (projectHome == null) {
            throw new IllegalStateException("neo4j_project_home_missing");
        }
        Path parent = projectHome.toAbsolutePath().normalize().getParent();
        if (parent == null) {
            throw new IllegalStateException("neo4j_project_parent_missing");
        }
        String name = projectHome.getFileName() == null ? "project" : projectHome.getFileName().toString();
        return parent.resolve(name + ".staging-" + buildSeq + "-" + System.nanoTime());
    }

    private static Path replaceProjectHome(Path projectHome, Path stagingHome, long buildSeq) {
        if (projectHome == null || stagingHome == null) {
            throw new IllegalArgumentException("neo4j_project_swap_path_missing");
        }
        Path normalizedProjectHome = projectHome.toAbsolutePath().normalize();
        Path normalizedStagingHome = stagingHome.toAbsolutePath().normalize();
        Path parent = normalizedProjectHome.getParent();
        if (parent == null) {
            throw new IllegalStateException("neo4j_project_parent_missing");
        }
        String baseName = normalizedProjectHome.getFileName() == null
                ? "project"
                : normalizedProjectHome.getFileName().toString();
        Path backupHome = parent.resolve(baseName + ".backup-" + buildSeq + "-" + System.nanoTime());
        boolean originalMoved = false;
        try {
            if (Files.exists(normalizedProjectHome)) {
                movePath(normalizedProjectHome, backupHome);
                originalMoved = true;
            }
            movePath(normalizedStagingHome, normalizedProjectHome);
            return originalMoved ? backupHome : null;
        } catch (Exception ex) {
            try {
                if (!originalMoved) {
                    deleteRecursively(normalizedProjectHome);
                } else {
                    deleteRecursively(normalizedProjectHome);
                    movePath(backupHome, normalizedProjectHome);
                }
            } catch (Exception rollbackEx) {
                logger.error("neo4j project home rollback failed: live={} backup={} err={}",
                        normalizedProjectHome,
                        backupHome,
                        rollbackEx.toString(),
                        rollbackEx);
            }
            throw new IllegalStateException("neo4j_project_swap_failed", ex);
        }
    }

    private static Path resolvePromotedBuildArtifacts(Path projectHome, Path stagingDir) {
        if (projectHome == null || stagingDir == null) {
            return null;
        }
        try {
            Path relative = stagingDir.getFileName();
            if (relative == null) {
                return projectHome.resolve("import-staging");
            }
            return projectHome.resolve("import-staging").resolve(relative.toString());
        } catch (Exception ex) {
            logger.debug("resolve promoted import staging path fail: {}", ex.toString());
            return projectHome.resolve("import-staging");
        }
    }

    private static void scheduleCleanup(String label, Path path) {
        if (path == null) {
            return;
        }
        CLEANUP_EXECUTOR.execute(() -> {
            try {
                deleteRecursively(path);
                deleteIfEmpty(path.getParent());
            } catch (Exception ex) {
                logger.debug("{} cleanup fail: {} ({})", label, path, ex.toString());
            }
        });
    }

    private static void deleteIfEmpty(Path path) {
        if (path == null || !Files.isDirectory(path)) {
            return;
        }
        try (var children = Files.list(path)) {
            if (children.findAny().isPresent()) {
                return;
            }
        } catch (Exception ex) {
            logger.debug("check empty directory fail: {} ({})", path, ex.toString());
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (Exception ex) {
            logger.debug("delete empty directory fail: {} ({})", path, ex.toString());
        }
    }

    private static void movePath(Path source, Path target) throws IOException {
        if (source == null || target == null) {
            throw new IllegalArgumentException("neo4j_move_path_missing");
        }
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void applyExtraBuildMeta(Node meta, Map<String, Object> extraMeta) {
        if (meta == null || extraMeta == null || extraMeta.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : extraMeta.entrySet()) {
            if (entry == null) {
                continue;
            }
            String key = safe(entry.getKey()).trim();
            if (key.isBlank()
                    || "key".equals(key)
                    || "build_seq".equals(key)
                    || "quick_mode".equals(key)
                    || "call_graph_mode".equals(key)
                    || "node_count".equals(key)
                    || "edge_count".equals(key)
                    || "updated_at".equals(key)) {
                continue;
            }
            Object value = normalizeMetaValue(entry.getValue());
            if (value == null) {
                continue;
            }
            try {
                meta.setProperty(key, value);
            } catch (Exception ignored) {
                logger.debug("skip unsupported build meta value: key={} value={}", key, value);
            }
        }
    }

    private static Object normalizeMetaValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Double d) {
            return d;
        }
        if (value instanceof Float f) {
            return f;
        }
        if (value instanceof Short s) {
            return (int) s;
        }
        if (value instanceof Byte b) {
            return (int) b;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return String.valueOf(value);
    }

    private static String methodLabels() {
        return "JANode;Method";
    }

    private static String classLabels() {
        return "JANode;Class";
    }

    private record BuildMetaCsvRow(long buildSeq,
                                   boolean quickMode,
                                   String callGraphMode,
                                   int nodeCount,
                                   long edgeCount,
                                   long updatedAt,
                                   String callGraphEngine,
                                   String analysisProfile,
                                   int targetJarCount,
                                   int libraryJarCount,
                                   int sdkEntryCount,
                                   int explicitEntryMethodCount) {
        private static BuildMetaCsvRow from(long buildSeq,
                                            boolean quickMode,
                                            String callGraphMode,
                                            int nodeCount,
                                            long edgeCount,
                                            Map<String, Object> extraMeta) {
            long now = System.currentTimeMillis();
            return new BuildMetaCsvRow(
                    Math.max(0L, buildSeq),
                    quickMode,
                    safe(callGraphMode),
                    Math.max(0, nodeCount),
                    Math.max(0L, edgeCount),
                    now,
                    stringValue(extraMeta, "call_graph_engine"),
                    stringValue(extraMeta, "analysis_profile"),
                    Math.max(0, intValue(extraMeta, "target_jar_count")),
                    Math.max(0, intValue(extraMeta, "library_jar_count")),
                    Math.max(0, intValue(extraMeta, "sdk_entry_count")),
                    Math.max(0, intValue(extraMeta, "explicit_entry_method_count"))
            );
        }

        private static String stringValue(Map<String, Object> values, String key) {
            Object normalized = normalizeMetaValue(values == null ? null : values.get(key));
            return normalized == null ? "" : safe(String.valueOf(normalized));
        }

        private static int intValue(Map<String, Object> values, String key) {
            Object normalized = normalizeMetaValue(values == null ? null : values.get(key));
            if (normalized instanceof Number number) {
                return number.intValue();
            }
            if (normalized instanceof String string && !string.isBlank()) {
                try {
                    return Integer.parseInt(string.trim());
                } catch (Exception ignored) {
                    return 0;
                }
            }
            return 0;
        }
    }

    private static SourceMarkerIndex buildSourceMarkerIndex(GraphPayloadData graphPayloadData) {
        SourceRuleSupport.RuleSnapshot ruleSnapshot = SourceRuleSupport.snapshotCurrentRules();
        Map<MethodLooseKey, Integer> explicitSourceFlags = new HashMap<>();
        mergeExplicitWebEntryFlags(explicitSourceFlags, graphPayloadData);
        Map<ClassKey, ClassReference> classReferences = new HashMap<>();
        Map<String, ClassReference> classReferencesByName = new HashMap<>();
        if (graphPayloadData != null && graphPayloadData.classReferences() != null) {
            for (ClassReference classReference : graphPayloadData.classReferences()) {
                if (classReference == null) {
                    continue;
                }
                classReferences.put(toClassKey(classReference), classReference);
                classReferencesByName.putIfAbsent(safe(classReference.getName()), classReference);
            }
        }
        return new SourceMarkerIndex(ruleSnapshot, explicitSourceFlags, classReferences, classReferencesByName);
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

        int flags = markerIndex == null ? 0 : SourceRuleSupport.resolveRuleFlags(method, markerIndex.ruleSnapshot());
        if (markerIndex != null) {
            ClassReference ownerClass = resolveMarkerClassReference(markerIndex, cls, resolveMethodJar(method));
            flags |= SourceRuleSupport.resolveFrameworkFlags(
                    method,
                    ownerClass,
                    (ownerName, ownerJarId) -> resolveMarkerClassReference(markerIndex, ownerName, ownerJarId)
            );
        }
        if (markerIndex != null) {
            Map<MethodLooseKey, Integer> explicitSourceFlags = markerIndex.explicitSourceFlags();
            if (explicitSourceFlags != null && !explicitSourceFlags.isEmpty()) {
                Integer explicit = explicitSourceFlags.get(new MethodLooseKey(cls, name, desc));
                if (explicit != null) {
                    flags |= explicit;
                }
            }
        }
        if (SourceRuleSupport.isServletEntry(name, desc)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        if (flags != 0) {
            flags |= GraphNode.SOURCE_FLAG_ANY;
        }
        return flags;
    }

    private static void mergeExplicitWebEntryFlags(Map<MethodLooseKey, Integer> explicitWebFlags,
                                                   GraphPayloadData graphPayloadData) {
        if (explicitWebFlags == null || graphPayloadData == null) {
            return;
        }
        if (graphPayloadData.springControllers() != null) {
            for (SpringController controller : graphPayloadData.springControllers()) {
                if (controller == null || controller.getMappings() == null) {
                    continue;
                }
                for (me.n1ar4.jar.analyzer.analyze.spring.SpringMapping mapping : controller.getMappings()) {
                    if (mapping == null) {
                        continue;
                    }
                    addExplicitWebEntry(
                            explicitWebFlags,
                            mapping.getMethodName() == null
                                    || mapping.getMethodName().getClassReference() == null
                                    ? null
                                    : mapping.getMethodName().getClassReference().getName(),
                            mapping.getMethodName() == null ? null : mapping.getMethodName().getName(),
                            mapping.getMethodName() == null ? null : mapping.getMethodName().getDesc()
                    );
                }
            }
        }
        addExplicitEntriesByClass(explicitWebFlags, graphPayloadData.servlets(), WebEntryMethods.SERVLET_ENTRY_METHODS);
        addExplicitEntriesByClass(explicitWebFlags, graphPayloadData.filters(), WebEntryMethods.FILTER_ENTRY_METHODS);
        addExplicitEntriesByClass(explicitWebFlags, graphPayloadData.springInterceptors(), WebEntryMethods.INTERCEPTOR_ENTRY_METHODS);
        addExplicitEntriesByClass(explicitWebFlags, graphPayloadData.listeners(), WebEntryMethods.LISTENER_ENTRY_METHODS);
        if (graphPayloadData.explicitSourceMethodFlags() != null) {
            for (Map.Entry<MethodReference.Handle, Integer> entry : graphPayloadData.explicitSourceMethodFlags().entrySet()) {
                MethodReference.Handle handle = entry.getKey();
                if (handle == null || handle.getClassReference() == null) {
                    continue;
                }
                String owner = handle.getClassReference().getName();
                String methodName = handle.getName();
                String methodDesc = handle.getDesc();
                int flags = entry.getValue() == null ? 0 : entry.getValue();
                addExplicitWebEntry(explicitWebFlags, owner, methodName, methodDesc, flags);
            }
        }
    }

    private static void addExplicitEntriesByClass(Map<MethodLooseKey, Integer> explicitWebFlags,
                                                  Collection<String> classes,
                                                  Set<WebEntryMethodSpec> methods) {
        if (explicitWebFlags == null || classes == null || classes.isEmpty() || methods == null || methods.isEmpty()) {
            return;
        }
        for (String className : classes) {
            for (WebEntryMethodSpec method : methods) {
                if (method == null) {
                    continue;
                }
                addExplicitWebEntry(explicitWebFlags, className, method.name(), method.desc());
            }
        }
    }

    private static void addExplicitWebEntry(Map<MethodLooseKey, Integer> explicitWebFlags,
                                            String className,
                                            String methodName,
                                            String methodDesc) {
        addExplicitWebEntry(explicitWebFlags, className, methodName, methodDesc, GraphNode.SOURCE_FLAG_WEB);
    }

    private static void addExplicitWebEntry(Map<MethodLooseKey, Integer> explicitWebFlags,
                                            String className,
                                            String methodName,
                                            String methodDesc,
                                            int flags) {
        String owner = normalizeInternalName(className);
        String method = safe(methodName);
        String desc = safe(methodDesc);
        if (owner.isBlank() || method.isBlank() || desc.isBlank() || flags == 0) {
            return;
        }
        explicitWebFlags.merge(
                new MethodLooseKey(owner, method, desc),
                flags,
                (left, right) -> left | right
        );
    }

    private static ClassReference resolveMarkerClassReference(SourceMarkerIndex markerIndex,
                                                              String className,
                                                              Integer jarId) {
        if (markerIndex == null) {
            return null;
        }
        String normalized = safe(className);
        if (normalized.isBlank()) {
            return null;
        }
        Map<ClassKey, ClassReference> classes = markerIndex.classReferences();
        if (classes != null) {
            ClassReference direct = classes.get(new ClassKey(normalized, normalizeJarId(jarId)));
            if (direct != null) {
                return direct;
            }
        }
        Map<String, ClassReference> classReferencesByName = markerIndex.classReferencesByName();
        return classReferencesByName == null ? null : classReferencesByName.get(normalized);
    }

    private static String normalizeInternalName(String className) {
        String value = safe(className);
        if (value.isBlank()) {
            return "";
        }
        return value.replace('.', '/');
    }

    private static void registerLooseMethodNode(Map<MethodLooseKey, Long> methodNodeByLooseKey,
                                                Set<MethodLooseKey> ambiguousMethodLooseKeys,
                                                MethodLooseKey key,
                                                long nodeId) {
        if (methodNodeByLooseKey == null || ambiguousMethodLooseKeys == null || key == null || nodeId <= 0L) {
            return;
        }
        if (ambiguousMethodLooseKeys.contains(key)) {
            return;
        }
        Long existing = methodNodeByLooseKey.putIfAbsent(key, nodeId);
        if (existing != null && existing.longValue() != nodeId) {
            methodNodeByLooseKey.remove(key);
            ambiguousMethodLooseKeys.add(key);
        }
    }

    private static void registerLooseClassNode(Map<String, Long> classNodeByName,
                                               Set<String> ambiguousClassNames,
                                               String className,
                                               long nodeId) {
        String normalized = safe(className);
        if (classNodeByName == null || ambiguousClassNames == null || normalized.isBlank() || nodeId <= 0L) {
            return;
        }
        if (ambiguousClassNames.contains(normalized)) {
            return;
        }
        Long existing = classNodeByName.putIfAbsent(normalized, nodeId);
        if (existing != null && existing.longValue() != nodeId) {
            classNodeByName.remove(normalized);
            ambiguousClassNames.add(normalized);
        }
    }

    private static Long resolveMethodNode(Map<MethodKey, Long> methodNodeByKey,
                                          Map<MethodLooseKey, Long> methodNodeByLooseKey,
                                          Set<MethodLooseKey> ambiguousMethodLooseKeys,
                                          MethodReference.Handle handle,
                                          int jarId) {
        if (handle == null) {
            return null;
        }
        return resolveMethodNode(
                methodNodeByKey,
                methodNodeByLooseKey,
                ambiguousMethodLooseKeys,
                safe(handle.getClassReference() == null ? null : handle.getClassReference().getName()),
                safe(handle.getName()),
                safe(handle.getDesc()),
                jarId
        );
    }

    private static Long resolveMethodNode(Map<MethodKey, Long> methodNodeByKey,
                                          Map<MethodLooseKey, Long> methodNodeByLooseKey,
                                          Set<MethodLooseKey> ambiguousMethodLooseKeys,
                                          String className,
                                          String methodName,
                                          String methodDesc,
                                          int jarId) {
        MethodKey exact = new MethodKey(safe(className), safe(methodName), safe(methodDesc), normalizeJarId(jarId));
        Long node = methodNodeByKey.get(exact);
        if (node != null) {
            return node;
        }
        MethodLooseKey looseKey = new MethodLooseKey(exact.className, exact.methodName, exact.methodDesc);
        if (ambiguousMethodLooseKeys != null && ambiguousMethodLooseKeys.contains(looseKey)) {
            return null;
        }
        return methodNodeByLooseKey.get(looseKey);
    }

    private static Long resolveClassNode(Map<ClassKey, Long> classNodeByKey,
                                         Map<String, Long> classNodeByName,
                                         Set<String> ambiguousClassNames,
                                         String className,
                                         Integer jarId) {
        String normalized = safe(className);
        if (normalized.isBlank()) {
            return null;
        }
        Long exact = classNodeByKey.get(new ClassKey(normalized, normalizeJarId(jarId)));
        if (exact != null) {
            return exact;
        }
        if (ambiguousClassNames.contains(normalized)) {
            return null;
        }
        return classNodeByName.get(normalized);
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

    private static MethodKey toMethodKey(MethodReference.Handle handle) {
        if (handle == null) {
            return new MethodKey("", "", "", -1);
        }
        return new MethodKey(
                safe(handle.getClassReference() == null ? null : handle.getClassReference().getName()),
                safe(handle.getName()),
                safe(handle.getDesc()),
                normalizeJarId(handle.getJarId())
        );
    }

    private static ClassKey toClassKey(ClassReference classReference) {
        if (classReference == null) {
            return new ClassKey("", -1);
        }
        return new ClassKey(
                safe(classReference.getName()),
                normalizeJarId(classReference.getJarId())
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

    private static List<ClassReference> sortedClasses(List<ClassReference> classReferences) {
        if (classReferences == null || classReferences.isEmpty()) {
            return List.of();
        }
        List<ClassReference> out = new ArrayList<>(classReferences);
        out.sort(CLASS_REFERENCE_COMPARATOR);
        return out;
    }

    private static boolean isAbstractClass(ClassReference classReference) {
        if (classReference == null || classReference.getAccess() == null) {
            return false;
        }
        return (classReference.getAccess() & 0x0400) != 0;
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
            logger.debug("parse positive int property fail: {} ({})", raw, ignored.toString());
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

    private static long msSince(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }

    private static void notifyStage(StageObserver stageObserver,
                                    String stageKey,
                                    long startNs,
                                    Map<String, Object> details) {
        if (stageObserver == null) {
            return;
        }
        stageObserver.onStage(stageKey, msSince(startNs), details);
    }

    private static Map<String, Object> detailMap(Object... items) {
        if (items == null || items.length == 0) {
            return Map.of();
        }
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        for (int i = 0; i + 1 < items.length; i += 2) {
            Object key = items[i];
            Object value = items[i + 1];
            if (key == null || value == null) {
                continue;
            }
            String normalizedKey = safe(String.valueOf(key)).trim().toLowerCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');
            if (normalizedKey.isBlank()) {
                continue;
            }
            details.put(normalizedKey, value);
        }
        return details.isEmpty() ? Map.of() : Map.copyOf(details);
    }

    private static String heapUsage() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long committed = runtime.totalMemory();
        long used = committed - runtime.freeMemory();
        return "used=" + formatMemory(used)
                + ", committed=" + formatMemory(committed)
                + ", max=" + formatMemory(max);
    }

    private static String formatMemory(long bytes) {
        if (bytes <= 0L) {
            return "0 MiB";
        }
        double gib = bytes / (1024.0 * 1024.0 * 1024.0);
        if (gib >= 1.0) {
            return String.format(Locale.ROOT, "%.1f GiB", gib);
        }
        double mib = bytes / (1024.0 * 1024.0);
        return String.format(Locale.ROOT, "%.0f MiB", mib);
    }

    private static String readTail(Path file, int maxBytes) {
        if (file == null || maxBytes <= 0) {
            return "";
        }
        try {
            if (!Files.exists(file) || !Files.isRegularFile(file)) {
                return "";
            }
            try (SeekableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ)) {
                long size = channel.size();
                if (size <= 0L) {
                    return "";
                }
                int len = (int) Math.min(size, maxBytes);
                long start = Math.max(0L, size - len);
                channel.position(start);
                ByteBuffer buffer = ByteBuffer.allocate(len);
                while (buffer.hasRemaining()) {
                    int read = channel.read(buffer);
                    if (read <= 0) {
                        break;
                    }
                }
                buffer.flip();
                return StandardCharsets.UTF_8.decode(buffer).toString().trim();
            }
        } catch (Exception ex) {
            logger.debug("read import log tail failed: {} ({})", file, ex.toString());
            return "";
        }
    }

    private static String summarizeOneLine(String value, int maxLen) {
        String raw = safe(value);
        if (raw.isBlank()) {
            return "";
        }
        String compact = raw.replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .replaceAll("\\s{2,}", " ")
                .trim();
        if (compact.length() <= maxLen) {
            return compact;
        }
        return compact.substring(0, maxLen) + "...";
    }

    private static boolean isLikelyPreambleOnly(String value) {
        String text = safe(value).toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            return true;
        }
        return text.contains("vm name:")
                && text.contains("configuration files used")
                && !text.contains("exception")
                && !text.contains("error:")
                && !text.contains("failed");
    }

    private static String summarizeThrowable(Throwable error) {
        if (error == null) {
            return "";
        }
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String rootMsg = safe(root.getMessage());
        if (!rootMsg.isBlank()) {
            return root.getClass().getSimpleName() + ": " + rootMsg;
        }
        String topMsg = safe(error.getMessage());
        if (!topMsg.isBlank()) {
            return error.getClass().getSimpleName() + ": " + topMsg;
        }
        return error.getClass().getName();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static Map<CallSiteLookupKey, List<CallSiteProjection>> buildCallSiteIndex(List<CallSiteEntity> callSites) {
        if (callSites == null || callSites.isEmpty()) {
            return Map.of();
        }
        Map<CallSiteLookupKey, List<CallSiteProjection>> index = new HashMap<>();
        for (CallSiteEntity row : callSites) {
            if (row == null) {
                continue;
            }
            CallSiteLookupKey key = new CallSiteLookupKey(
                    safe(row.getCallerClassName()),
                    safe(row.getCallerMethodName()),
                    safe(row.getCallerMethodDesc()),
                    safe(row.getCalleeOwner()),
                    safe(row.getCalleeMethodName()),
                    safe(row.getCalleeMethodDesc()),
                    normalizeJarId(row.getJarId())
            );
            if (key.isBlank()) {
                continue;
            }
            index.computeIfAbsent(key, ignore -> new ArrayList<>()).add(new CallSiteProjection(
                    safe(row.getCallSiteKey()),
                    row.getLineNumber() == null ? -1 : row.getLineNumber(),
                    row.getCallIndex() == null ? -1 : row.getCallIndex(),
                    row.getOpCode() == null ? -1 : row.getOpCode()
            ));
        }
        if (index.isEmpty()) {
            return Map.of();
        }
        for (List<CallSiteProjection> projections : index.values()) {
            projections.sort(Comparator
                    .comparingInt(CallSiteProjection::lineNumber)
                    .thenComparingInt(CallSiteProjection::callIndex)
                    .thenComparing(CallSiteProjection::callSiteKey));
        }
        return index;
    }

    private static List<CallSiteProjection> resolveCallSiteProjections(
            Map<CallSiteLookupKey, List<CallSiteProjection>> callSiteIndex,
            MethodReference.Handle caller,
            MethodReference.Handle callee,
            MethodCallMeta meta) {
        if (callSiteIndex == null || callSiteIndex.isEmpty() || caller == null || callee == null) {
            return List.of();
        }
        CallSiteLookupKey key = new CallSiteLookupKey(
                safe(caller.getClassReference() == null ? null : caller.getClassReference().getName()),
                safe(caller.getName()),
                safe(caller.getDesc()),
                safe(callee.getClassReference() == null ? null : callee.getClassReference().getName()),
                safe(callee.getName()),
                safe(callee.getDesc()),
                normalizeJarId(caller.getJarId())
        );
        List<CallSiteProjection> projections = callSiteIndex.get(key);
        if (projections != null && !projections.isEmpty()) {
            return projections;
        }
        if (meta == null || !MethodCallMeta.TYPE_PTA.equals(meta.getType())) {
            return List.of();
        }
        int expectedOpcode = resolveOpcode(meta, caller, callee);
        LinkedHashMap<String, CallSiteProjection> fallback = new LinkedHashMap<>();
        String callerClass = safe(caller.getClassReference() == null ? null : caller.getClassReference().getName());
        String callerMethod = safe(caller.getName());
        String callerDesc = safe(caller.getDesc());
        String calleeMethod = safe(callee.getName());
        String calleeDesc = safe(callee.getDesc());
        int callerJarId = normalizeJarId(caller.getJarId());
        for (Map.Entry<CallSiteLookupKey, List<CallSiteProjection>> entry : callSiteIndex.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            CallSiteLookupKey candidateKey = entry.getKey();
            if (!callerClass.equals(candidateKey.callerClassName())
                    || !callerMethod.equals(candidateKey.callerMethodName())
                    || !callerDesc.equals(candidateKey.callerMethodDesc())
                    || !calleeMethod.equals(candidateKey.calleeMethodName())
                    || !calleeDesc.equals(candidateKey.calleeMethodDesc())
                    || callerJarId != candidateKey.jarId()) {
                continue;
            }
            for (CallSiteProjection projection : entry.getValue()) {
                if (projection == null) {
                    continue;
                }
                if (expectedOpcode > 0
                        && projection.opCode() > 0
                        && projection.opCode() != expectedOpcode) {
                    continue;
                }
                fallback.putIfAbsent(projection.callSiteKey(), projection);
            }
        }
        if (fallback.isEmpty()) {
            return List.of();
        }
        ArrayList<CallSiteProjection> out = new ArrayList<>(fallback.values());
        out.sort(Comparator
                .comparingInt(CallSiteProjection::lineNumber)
                .thenComparingInt(CallSiteProjection::callIndex)
                .thenComparing(CallSiteProjection::callSiteKey));
        return List.copyOf(out);
    }

    private static ProjectRuntimeSnapshot requireRuntimeSnapshot(Supplier<ProjectRuntimeSnapshot> runtimeSnapshotSupplier) {
        if (runtimeSnapshotSupplier == null) {
            throw new IllegalStateException("project_runtime_snapshot_supplier_missing");
        }
        ProjectRuntimeSnapshot runtimeSnapshot = runtimeSnapshotSupplier.get();
        if (runtimeSnapshot == null) {
            throw new IllegalStateException("project_runtime_snapshot_missing");
        }
        return runtimeSnapshot;
    }

    private record CsvBuildResult(Path nodesFile,
                                  Path relationshipsFile,
                                  int classNodes,
                                  int methodNodes,
                                  long aliasEdges,
                                  long edgeCount,
                                  long callSiteEdges) {
        private int totalNodes() {
            return Math.max(0, classNodes) + Math.max(0, methodNodes);
        }
    }

    public record GraphPayloadData(List<CallSiteEntity> callSites,
                                   List<ClassReference> classReferences,
                                   List<SpringController> springControllers,
                                   List<String> springInterceptors,
                                   List<String> servlets,
                                   List<String> filters,
                                   List<String> listeners,
                                   Map<MethodReference.Handle, Integer> explicitSourceMethodFlags) {
        public GraphPayloadData {
            callSites = callSites == null ? List.of() : callSites;
            classReferences = classReferences == null ? List.of() : classReferences;
            springControllers = springControllers == null ? List.of() : springControllers;
            springInterceptors = springInterceptors == null ? List.of() : springInterceptors;
            servlets = servlets == null ? List.of() : servlets;
            filters = filters == null ? List.of() : filters;
            listeners = listeners == null ? List.of() : listeners;
            explicitSourceMethodFlags = explicitSourceMethodFlags == null ? Map.of() : Map.copyOf(explicitSourceMethodFlags);
        }
    }

    private record CallSiteLookupKey(String callerClassName,
                                     String callerMethodName,
                                     String callerMethodDesc,
                                     String calleeClassName,
                                     String calleeMethodName,
                                     String calleeMethodDesc,
                                     int jarId) {
        private boolean isBlank() {
            return callerClassName.isBlank()
                    || callerMethodName.isBlank()
                    || callerMethodDesc.isBlank()
                    || calleeClassName.isBlank()
                    || calleeMethodName.isBlank()
                    || calleeMethodDesc.isBlank();
        }
    }

    private record CallSiteProjection(String callSiteKey,
                                      int lineNumber,
                                      int callIndex,
                                      int opCode) {
        private CallSiteProjection {
            callSiteKey = safe(callSiteKey);
        }
    }

    private record MethodKey(String className, String methodName, String methodDesc, int jarId) {
    }

    private record ClassKey(String className, int jarId) {
    }

    private record MethodLooseKey(String className, String methodName, String methodDesc) {
    }

    private record SourceMarkerIndex(SourceRuleSupport.RuleSnapshot ruleSnapshot,
                                     Map<MethodLooseKey, Integer> explicitSourceFlags,
                                     Map<ClassKey, ClassReference> classReferences,
                                     Map<String, ClassReference> classReferencesByName) {
    }

    private static final Comparator<ClassReference> CLASS_REFERENCE_COMPARATOR = Comparator
            .comparing((ClassReference row) -> safe(row == null ? null : row.getName()))
            .thenComparingInt(row -> normalizeJarId(row == null ? null : row.getJarId()));

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
