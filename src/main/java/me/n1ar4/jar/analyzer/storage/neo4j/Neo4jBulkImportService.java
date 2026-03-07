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
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.core.WebEntryMethodSpec;
import me.n1ar4.jar.analyzer.core.WebEntryMethods;
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
import org.neo4j.cli.ExecutionContext;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
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
import java.util.concurrent.atomic.AtomicReference;

public final class Neo4jBulkImportService {
    private static final Logger logger = LogManager.getLogger();
    private static final String KEEP_STAGING_PROP = "jar.analyzer.neo4j.bulk.keepStaging";
    private static final String MAX_OFF_HEAP_PROP = "jar.analyzer.neo4j.bulk.maxOffHeapMemory";
    private static final String THREADS_PROP = "jar.analyzer.neo4j.bulk.threads";
    private static final int IMPORT_LOG_TAIL_BYTES = 8192;
    private static final int IMPORT_ERR_SUMMARY_LIMIT = 480;
    public void replaceFromAnalysis(String projectKey,
                                    long buildSeq,
                                    boolean quickMode,
                                    String callGraphMode,
                                    Set<MethodReference> methods,
                                    Map<MethodReference.Handle, ? extends Set<MethodReference.Handle>> methodCalls,
                                    Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                    List<CallSiteEntity> callSites,
                                    ProjectRuntimeSnapshot runtimeSnapshot,
                                    Map<String, Object> buildMeta) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        Neo4jProjectStore store = Neo4jProjectStore.getInstance();
        Path projectHome = store.resolveProjectHome(normalized);
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
        try {
            Files.createDirectories(stagingDir);
            Path nodeFile = stagingDir.resolve("nodes.csv");
            Path relFile = stagingDir.resolve("rels.csv");
            csvResult = writeCsvPayload(nodeFile, relFile, methods, methodCalls, methodCallMeta, callSites, runtimeSnapshot);

            store.beginProjectImport(normalized);
            importLockHeld = true;
            runFullImport(stagingHome, csvResult.nodesFile(), csvResult.relationshipsFile(), stagingDir.resolve("import.report"));

            try {
                writeBuildMeta(
                        stagingHome,
                        buildSeq,
                        quickMode,
                        callGraphMode,
                        csvResult.methodNodes() + csvResult.callSiteNodes(),
                        csvResult.edgeCount(),
                        buildMeta
                );
                if (runtimeSnapshot != null) {
                    ProjectMetadataSnapshotStore.getInstance().writeToHome(stagingHome, projectHome, runtimeSnapshot);
                }
                backupHome = replaceProjectHome(projectHome, stagingHome, buildSeq);
            } finally {
                Neo4jGraphSnapshotLoader.invalidate(normalized);
            }

            store.endProjectImport(normalized);
            importLockHeld = false;

            if (csvResult.edgeCount() > Integer.MAX_VALUE) {
                logger.warn("neo4j bulk edge count overflow int: key={} edgeCount={}", normalized, csvResult.edgeCount());
            }
            logger.info("neo4j bulk build finish: key={} buildSeq={} methodNodes={} callSiteNodes={} edges={} elapsedMs={}",
                    normalized,
                    buildSeq,
                    csvResult.methodNodes(),
                    csvResult.callSiteNodes(),
                    csvResult.edgeCount(),
                    msSince(startNs));
            buildSucceeded = true;
            return;
        } catch (Exception ex) {
            logger.error("neo4j bulk build fail: key={} buildSeq={} err={}", normalized, buildSeq, ex.toString(), ex);
            throw ex instanceof RuntimeException
                    ? (RuntimeException) ex
                    : new IllegalStateException("neo4j_bulk_import_failed", ex);
        } finally {
            if (importLockHeld) {
                store.endProjectImport(normalized);
            }
            deleteRecursively(backupHome);
            if (!keepStaging && buildSucceeded) {
                deleteRecursively(projectHome.resolve("import-staging"));
            } else {
                Path keptPath = buildSucceeded ? projectHome.resolve("import-staging") : stagingDir;
                logger.info("keep neo4j bulk staging files: {}", keptPath);
            }
            if (!buildSucceeded && !keepStaging) {
                deleteRecursively(stagingHome);
            }
        }
    }

    private CsvBuildResult writeCsvPayload(
            Path nodeFile,
            Path relFile,
            Set<MethodReference> methods,
            Map<MethodReference.Handle, ? extends Set<MethodReference.Handle>> methodCalls,
            Map<MethodCallKey, MethodCallMeta> methodCallMeta,
            List<CallSiteEntity> callSites,
            ProjectRuntimeSnapshot runtimeSnapshot) throws IOException {
        Map<MethodKey, Long> methodNodeByKey = new LinkedHashMap<>();
        Map<MethodLooseKey, Long> methodNodeByLooseKey = new LinkedHashMap<>();
        Map<MethodLooseKey, List<MethodKey>> methodCandidatesByLooseKey = new LinkedHashMap<>();
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
            SourceMarkerIndex sourceMarkerIndex = buildSourceMarkerIndex(runtimeSnapshot);
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
                methodCandidatesByLooseKey
                        .computeIfAbsent(new MethodLooseKey(key.className, key.methodName, key.methodDesc),
                                ignore -> new ArrayList<>())
                        .add(key);
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
            CallSiteCalleeResolver callSiteCalleeResolver = new CallSiteCalleeResolver(
                    methodNodeByKey,
                    methodCandidatesByLooseKey,
                    methodCalls
            );
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

                Long calleeNode = callSiteCalleeResolver.resolveCalleeNode(row);
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

    private static void writeBuildMeta(Path projectHome,
                                       long buildSeq,
                                       boolean quickMode,
                                       String callGraphMode,
                                       int nodeCount,
                                       long edgeCount,
                                       Map<String, Object> extraMeta) {
        if (projectHome == null) {
            throw new IllegalStateException("neo4j_project_home_missing");
        }
        DatabaseManagementService managementService = null;
        try {
            managementService = new DatabaseManagementServiceBuilder(projectHome).build();
            GraphDatabaseService database = managementService.database(GraphDatabaseSettings.DEFAULT_DATABASE_NAME);
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
                applyExtraBuildMeta(meta, extraMeta);
                tx.commit();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("neo4j_build_meta_write_failed", ex);
        } finally {
            if (managementService != null) {
                try {
                    managementService.shutdown();
                } catch (Exception ex) {
                    logger.debug("shutdown staging neo4j metadata writer fail: {}", ex.toString());
                }
            }
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

    private static SourceMarkerIndex buildSourceMarkerIndex(ProjectRuntimeSnapshot runtimeSnapshot) {
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

        Map<MethodLooseKey, Integer> explicitWebFlags = new HashMap<>();
        mergeExplicitWebEntryFlags(explicitWebFlags, runtimeSnapshot);

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
        return new SourceMarkerIndex(modelFlags, explicitWebFlags, sourceAnnotations);
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
            Map<MethodLooseKey, Integer> explicitWebFlags = markerIndex.explicitWebFlags();
            if (explicitWebFlags != null && !explicitWebFlags.isEmpty()) {
                Integer explicit = explicitWebFlags.get(new MethodLooseKey(cls, name, desc));
                if (explicit != null) {
                    flags |= explicit;
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

    private static void mergeExplicitWebEntryFlags(Map<MethodLooseKey, Integer> explicitWebFlags,
                                                   ProjectRuntimeSnapshot runtimeSnapshot) {
        if (explicitWebFlags == null || runtimeSnapshot == null) {
            return;
        }
        if (runtimeSnapshot.springControllers() != null) {
            for (ProjectRuntimeSnapshot.SpringControllerData controller : runtimeSnapshot.springControllers()) {
                if (controller == null || controller.mappings() == null) {
                    continue;
                }
                for (ProjectRuntimeSnapshot.SpringMappingData mapping : controller.mappings()) {
                    if (mapping == null) {
                        continue;
                    }
                    addExplicitWebEntry(
                            explicitWebFlags,
                            mapping.methodOwner() == null ? null : mapping.methodOwner().name(),
                            mapping.methodName(),
                            mapping.methodDesc()
                    );
                }
            }
        }
        addExplicitEntriesByClass(explicitWebFlags, runtimeSnapshot.servlets(), WebEntryMethods.SERVLET_ENTRY_METHODS);
        addExplicitEntriesByClass(explicitWebFlags, runtimeSnapshot.filters(), WebEntryMethods.FILTER_ENTRY_METHODS);
        addExplicitEntriesByClass(explicitWebFlags, runtimeSnapshot.springInterceptors(), WebEntryMethods.INTERCEPTOR_ENTRY_METHODS);
        addExplicitEntriesByClass(explicitWebFlags, runtimeSnapshot.listeners(), WebEntryMethods.LISTENER_ENTRY_METHODS);
    }

    private static void addExplicitEntriesByClass(Map<MethodLooseKey, Integer> explicitWebFlags,
                                                  Set<String> classes,
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
        String owner = normalizeInternalName(className);
        String method = safe(methodName);
        String desc = safe(methodDesc);
        if (owner.isBlank() || method.isBlank() || desc.isBlank()) {
            return;
        }
        explicitWebFlags.merge(
                new MethodLooseKey(owner, method, desc),
                GraphNode.SOURCE_FLAG_WEB,
                (left, right) -> left | right
        );
    }

    private static String normalizeInternalName(String className) {
        String value = safe(className);
        if (value.isBlank()) {
            return "";
        }
        return value.replace('.', '/');
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

    private static final class CallSiteCalleeResolver {
        private final Map<MethodKey, Long> methodNodeByKey;
        private final Map<MethodLooseKey, List<MethodKey>> candidatesByLooseKey;
        private final Map<MethodKey, Map<MethodLooseKey, List<MethodKey>>> targetsByCaller;

        private CallSiteCalleeResolver(
                Map<MethodKey, Long> methodNodeByKey,
                Map<MethodLooseKey, List<MethodKey>> candidatesByLooseKey,
                Map<MethodReference.Handle, ? extends Collection<MethodReference.Handle>> methodCalls) {
            this.methodNodeByKey = methodNodeByKey == null ? Map.of() : methodNodeByKey;
            this.candidatesByLooseKey = candidatesByLooseKey == null ? Map.of() : candidatesByLooseKey;
            this.targetsByCaller = buildTargetsByCaller(methodCalls);
        }

        private Long resolveCalleeNode(CallSiteRow row) {
            if (row == null) {
                return null;
            }
            MethodKey callerKey = new MethodKey(row.callerClass(), row.callerMethod(), row.callerDesc(), row.jarId());
            MethodLooseKey calleeLoose = new MethodLooseKey(row.calleeClass(), row.calleeMethod(), row.calleeDesc());
            Long hinted = resolveHintedTarget(callerKey, calleeLoose, row.jarId());
            if (hinted != null) {
                return hinted;
            }
            if (row.jarId() >= 0) {
                Long sameJar = methodNodeByKey.get(new MethodKey(
                        row.calleeClass(),
                        row.calleeMethod(),
                        row.calleeDesc(),
                        row.jarId()
                ));
                if (sameJar != null) {
                    return sameJar;
                }
            }
            return selectCandidate(candidatesByLooseKey.get(calleeLoose), row.jarId());
        }

        private Long resolveHintedTarget(MethodKey callerKey, MethodLooseKey calleeLoose, int preferredJarId) {
            Map<MethodLooseKey, List<MethodKey>> byCallee = targetsByCaller.get(callerKey);
            if (byCallee == null || byCallee.isEmpty()) {
                return null;
            }
            return selectCandidate(byCallee.get(calleeLoose), preferredJarId);
        }

        private Long selectCandidate(List<MethodKey> candidates, int preferredJarId) {
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
            if (candidates.size() == 1) {
                return methodNodeByKey.get(candidates.get(0));
            }
            if (preferredJarId >= 0) {
                MethodKey preferred = null;
                for (MethodKey candidate : candidates) {
                    if (candidate == null || candidate.jarId() != preferredJarId) {
                        continue;
                    }
                    if (preferred != null) {
                        return null;
                    }
                    preferred = candidate;
                }
                if (preferred != null) {
                    return methodNodeByKey.get(preferred);
                }
            }
            return null;
        }

        private Map<MethodKey, Map<MethodLooseKey, List<MethodKey>>> buildTargetsByCaller(
                Map<MethodReference.Handle, ? extends Collection<MethodReference.Handle>> methodCalls) {
            if (methodCalls == null || methodCalls.isEmpty()) {
                return Map.of();
            }
            Map<MethodKey, Map<MethodLooseKey, List<MethodKey>>> out = new LinkedHashMap<>();
            for (Map.Entry<MethodReference.Handle, ? extends Collection<MethodReference.Handle>> entry : methodCalls.entrySet()) {
                MethodKey callerKey = toMethodKey(entry == null ? null : entry.getKey());
                if (!methodNodeByKey.containsKey(callerKey)) {
                    continue;
                }
                Collection<MethodReference.Handle> callees = entry.getValue();
                if (callees == null || callees.isEmpty()) {
                    continue;
                }
                Map<MethodLooseKey, List<MethodKey>> byCallee =
                        out.computeIfAbsent(callerKey, ignore -> new LinkedHashMap<>());
                for (MethodReference.Handle callee : callees) {
                    MethodKey calleeKey = toMethodKey(callee);
                    if (!methodNodeByKey.containsKey(calleeKey)) {
                        continue;
                    }
                    MethodLooseKey loose = new MethodLooseKey(
                            calleeKey.className(),
                            calleeKey.methodName(),
                            calleeKey.methodDesc()
                    );
                    List<MethodKey> targets = byCallee.computeIfAbsent(loose, ignore -> new ArrayList<>());
                    if (!targets.contains(calleeKey)) {
                        targets.add(calleeKey);
                    }
                }
            }
            return out.isEmpty() ? Map.of() : out;
        }
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
                                     Map<MethodLooseKey, Integer> explicitWebFlags,
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
