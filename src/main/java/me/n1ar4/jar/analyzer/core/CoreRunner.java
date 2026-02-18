/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.asm.FixClassVisitor;
import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.edge.EdgeInferencePipeline;
import me.n1ar4.jar.analyzer.core.pta.ContextSensitivePtaEngine;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.DecompileEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.index.IndexEngine;
import me.n1ar4.jar.analyzer.graph.build.GraphProjectionBuilder;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.BytecodeCache;
import me.n1ar4.jar.analyzer.utils.ClasspathResolver;
import me.n1ar4.jar.analyzer.utils.CoreUtil;
import me.n1ar4.jar.analyzer.utils.DeferredFileWriter;
import me.n1ar4.jar.analyzer.utils.IOUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class CoreRunner {
    private static final Logger logger = LogManager.getLogger();
    private static final IntConsumer NOOP_PROGRESS = p -> {
    };
    private static final String CALL_GRAPH_MODE_PROP = "jar.analyzer.callgraph.mode";
    private static final String PTA_BASELINE_DISPATCH_PROP = "jar.analyzer.pta.baseline.dispatch";

    private static void refreshCachesAfterBuild() {
        try {
            IndexEngine.closeAll();
        } catch (Exception ex) {
            logger.debug("close index fail: {}", ex.toString());
        }
        try {
            DecompileEngine.cleanCache();
        } catch (Exception ex) {
            logger.debug("clean fern cache fail: {}", ex.toString());
        }
        try {
            CFRDecompileEngine.cleanCache();
        } catch (Exception ex) {
            logger.debug("clean cfr cache fail: {}", ex.toString());
        }
        try {
            me.n1ar4.jar.analyzer.utils.ClassIndex.refresh();
        } catch (Exception ex) {
            logger.debug("refresh class index fail: {}", ex.toString());
        }
    }

    public static BuildResult run(Path jarPath, Path rtJarPath, boolean fixClass) {
        return run(jarPath, rtJarPath, fixClass, false, true, null, false);
    }

    public static BuildResult run(Path jarPath,
                                  Path rtJarPath,
                                  boolean fixClass,
                                  boolean quickMode,
                                  boolean enableFixMethodImpl,
                                  IntConsumer progressConsumer) {
        return run(jarPath, rtJarPath, fixClass, quickMode, enableFixMethodImpl, progressConsumer, false);
    }

    /**
     * Build database for the given input.
     *
     * @param clearExistingDbData if true and {@code db/jar-analyzer.db} exists, clear all tables before building
     *                            (CLI-friendly behavior; GUI typically passes false).
     */
    public static BuildResult run(Path jarPath,
                                  Path rtJarPath,
                                  boolean fixClass,
                                  boolean quickMode,
                                  boolean enableFixMethodImpl,
                                  IntConsumer progressConsumer,
                                  boolean clearExistingDbData) {
        IntConsumer progress = progressConsumer == null ? NOOP_PROGRESS : progressConsumer;

        if (clearExistingDbData) {
            try {
                Path dbPath = Paths.get(Const.dbFile);
                if (Files.exists(dbPath)) {
                    DatabaseManager.clearAllData();
                }
            } catch (Exception e) {
                logger.warn("clear cli db fail: {}", e.toString());
            }
        }
        DatabaseManager.setBuilding(true);
        try {
            DatabaseManager.prepareBuild();
        } catch (Throwable t) {
            DatabaseManager.setBuilding(false);
            throw t;
        }
        BuildContext context = new BuildContext();
        CallGraphMode callGraphMode = resolveCallGraphMode();
        ContextSensitivePtaEngine.Result ptaResultSummary = null;
        DispatchMetrics dispatchMetrics = DispatchMetrics.empty();
        final long buildStartNs = System.nanoTime();
        long stageStartNs = buildStartNs;
        BuildDbWriter dbWriter = new BuildDbWriter();
        boolean finalizePending = true;
        boolean cleaned = false;
        try {
            Map<String, Integer> jarIdMap = new HashMap<>();

            List<ClassFileEntity> cfs;
            progress.accept(10);
            // Nested jars are handled via WorkspaceContext.resolveInnerJars (JarUtil).
            boolean includeNested = false;
            List<String> jarList = ClasspathResolver.resolveInputArchives(
                    jarPath, rtJarPath, !quickMode, includeNested);
            if (Files.isDirectory(jarPath)) {
                logger.info("input is a dir");
            } else {
                logger.info("input is a jar file");
            }
            for (String s : jarList) {
                if (s == null || s.trim().isEmpty()) {
                    continue;
                }
                String lower = s.toLowerCase();
                if (lower.endsWith(".jar") || lower.endsWith(".war")) {
                    DatabaseManager.saveJar(s);
                    jarIdMap.put(s, DatabaseManager.getJarId(s).getJid());
                }
            }
            cfs = CoreUtil.getAllClassesFromJars(jarList, jarIdMap, context.resources);
            // BUG CLASS NAME
            for (ClassFileEntity cf : cfs) {
                String className = cf.getClassName();
                if (!fixClass) {
                    int i = className.indexOf("classes");
                    if (className.contains("BOOT-INF") || className.contains("WEB-INF")) {
                        // 从 BOOT-INF/classes 开始取
                        // 从 WEB-INF/classes 开始取
                        className = className.substring(i + 8);
                    }
                    // 如果 i 小于 0 (不包含 classes 目录) 直接设置
                    cf.setClassName(className);
                } else {
                    // fix class name
                    Path parPath = resolveJarRoot(cf.getPath());
                    FixClassVisitor cv = new FixClassVisitor();
                    byte[] classBytes = cf.getFile();
                    if (classBytes == null || classBytes.length == 0) {
                        continue;
                    }
                    ClassReader cr = new ClassReader(classBytes);
                    cr.accept(cv, Const.HeaderASMOptions);
                    // get actual class name
                    String actualName = cv.getName();
                    Path path = parPath.resolve(Paths.get(actualName));
                    File file = path.toFile();
                    // write file
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        logger.error("fix class mkdirs error");
                    }
                    className = file.getPath() + ".class";
                    try {
                        Path fixedPath = Paths.get(className);
                        Files.write(fixedPath, classBytes);
                        BytecodeCache.preload(fixedPath, classBytes);
                    } catch (Exception ex) {
                        logger.error("fix path copy bytes error: " + ex.getMessage(), ex);
                    }
                    cf.setClassName(actualName + ".class");
                    cf.setPath(Paths.get(className));
                }
            }

            progress.accept(15);
            context.classFileList.addAll(cfs);
            logger.info("get all class");
            dbWriter.submit(() -> DatabaseManager.saveClassFiles(context.classFileList));
            dbWriter.submit(() -> DatabaseManager.saveResources(context.resources));
            progress.accept(20);
            DiscoveryRunner.start(context.classFileList, context.discoveredClasses,
                    context.discoveredMethods, context.classMap,
                    context.methodMap, context.stringAnnoMap);
            logger.info("build stage discovery: {} ms (classFiles={}, classes={}, methods={})",
                    msSince(stageStartNs),
                    context.classFileList.size(),
                    context.discoveredClasses.size(),
                    context.discoveredMethods.size());
            stageStartNs = System.nanoTime();
            dbWriter.submit(() -> DatabaseManager.saveClassInfo(context.discoveredClasses));
            progress.accept(25);
            dbWriter.submit(() -> DatabaseManager.saveMethods(context.discoveredMethods));
            progress.accept(30);
            logger.info("analyze class finish");
            for (MethodReference mr : context.discoveredMethods) {
                ClassReference.Handle ch = mr.getClassReference();
                context.methodsInClassMap
                        .computeIfAbsent(ch, k -> new ArrayList<>())
                        .add(mr);
            }
            progress.accept(35);
            ClassAnalysisRunner.start(context.classFileList,
                    context.methodCalls,
                    context.methodMap,
                    context.methodCallMeta,
                    context.instantiatedClasses,
                    context.strMap,
                    context.classMap,
                    context.controllers,
                    context.interceptors,
                    context.servlets,
                    context.filters,
                    context.listeners,
                    !quickMode,
                    !quickMode,
                    !quickMode);
            logger.info("build stage class-analysis: {} ms (edges={})",
                    msSince(stageStartNs),
                    countEdges(context.methodCalls));
            stageStartNs = System.nanoTime();
            progress.accept(40);

            BytecodeSymbolRunner.Result symbolResult = null;
            if (!quickMode && BytecodeSymbolRunner.isEnabled()) {
                symbolResult = BytecodeSymbolRunner.start(context.classFileList);
                List<CallSiteEntity> callSites = symbolResult.getCallSites();
                List<LocalVarEntity> localVars = symbolResult.getLocalVars();
                context.callSites.clear();
                if (callSites != null && !callSites.isEmpty()) {
                    context.callSites.addAll(callSites);
                }
                dbWriter.submit(() -> DatabaseManager.saveCallSites(callSites));
                dbWriter.submit(() -> DatabaseManager.saveLocalVars(localVars));
                logger.info("build stage symbol: {} ms (callSites={}, localVars={})",
                        msSince(stageStartNs),
                        callSites == null ? 0 : callSites.size(),
                        localVars == null ? 0 : localVars.size());
                stageStartNs = System.nanoTime();
            }

            if (!quickMode) {
                context.inheritanceMap = InheritanceRunner.derive(context.classMap);
                progress.accept(50);
                logger.info("build inheritance");
                Map<MethodReference.Handle, Set<MethodReference.Handle>> implMap =
                        InheritanceRunner.getAllMethodImplementations(context.inheritanceMap, context.methodMap);
                dbWriter.submit(() -> DatabaseManager.saveImpls(implMap, context.classMap));
                progress.accept(60);

                // 2024/09/02
                // 自动处理方法实现是可选的
                // 具体参考 doc/README-others.md
                if (enableFixMethodImpl) {
                    // 方法 -> [所有子类 override 方法列表]
                    for (Map.Entry<MethodReference.Handle, Set<MethodReference.Handle>> entry :
                            implMap.entrySet()) {
                        MethodReference.Handle k = entry.getKey();
                        Set<MethodReference.Handle> v = entry.getValue();
                        // 当前方法的所有 callee 列表
                        HashSet<MethodReference.Handle> calls =
                                context.methodCalls.computeIfAbsent(k, kk -> new HashSet<>());
                        // 增加所有的 override 方法
                        for (MethodReference.Handle impl : v) {
                            MethodCallUtils.addCallee(calls, impl);
                            String reason = resolveOverrideReason(context.classMap, k);
                            MethodCallMeta.record(context.methodCallMeta, MethodCallKey.of(k, impl),
                                    MethodCallMeta.TYPE_OVERRIDE, MethodCallMeta.CONF_LOW, reason);
                        }
                    }
                    Set<ClassReference.Handle> instantiated = context.instantiatedClasses;
                    if (instantiated == null || instantiated.isEmpty()) {
                        // Fallback for legacy builds / failures: full scan.
                        instantiated = DispatchCallResolver.collectInstantiatedClasses(context.classFileList);
                    }
                    List<CallSiteEntity> dispatchCallSites =
                            symbolResult == null || symbolResult.getCallSites() == null
                                    ? Collections.emptyList() : symbolResult.getCallSites();
                    if (callGraphMode == CallGraphMode.PTA) {
                        if (isPtaBaselineDispatchEnabled()) {
                            dispatchMetrics = expandDispatchEdges(
                                    context,
                                    CallGraphMode.RTA,
                                    instantiated,
                                    dispatchCallSites
                            );
                            logger.info("pta baseline dispatch edges added: {} (typed={})",
                                    dispatchMetrics.dispatchEdgesAdded,
                                    dispatchMetrics.typedDispatchEdgesAdded);
                        }
                        List<CallSiteEntity> ptaCallSites = context.callSites;
                        if (symbolResult != null && symbolResult.getCallSites() != null) {
                            ptaCallSites = symbolResult.getCallSites();
                        }
                        ptaResultSummary = ContextSensitivePtaEngine.run(context, ptaCallSites);
                        logger.info("pta stage result: {}", ptaResultSummary);
                    } else {
                        dispatchMetrics = expandDispatchEdges(
                                context,
                                callGraphMode,
                                instantiated,
                                dispatchCallSites
                        );
                        logger.info("dispatch edges added: {} (mode={})",
                                dispatchMetrics.dispatchEdgesAdded,
                                callGraphMode.getConfigValue());
                        if (callGraphMode == CallGraphMode.HYBRID) {
                            logger.info("dispatch edges detail: rta={} cha={}",
                                    dispatchMetrics.rtaDispatchAdded,
                                    dispatchMetrics.chaDispatchAdded);
                        }
                        logger.info("typed dispatch edges added: {}", dispatchMetrics.typedDispatchEdgesAdded);
                    }

                    int inferred = EdgeInferencePipeline.infer(context);
                    if (inferred > 0) {
                        logger.info("semantic inference edges added: {}", inferred);
                    }
                } else {
                    logger.warn("enable fix method impl/override is recommend");
                }
                logger.info("build stage inheritance/dispatch: {} ms (implEdges={}, edges={})",
                        msSince(stageStartNs),
                        implMap == null ? 0 : implMap.size(),
                        countEdges(context.methodCalls));
                stageStartNs = System.nanoTime();

                clearCachedBytes(context.classFileList);
                dbWriter.submit(() -> DatabaseManager.saveMethodCalls(
                        context.methodCalls, context.classMap, context.methodCallMeta, context.callSites));
                progress.accept(70);
                logger.info("build extra inheritance");
                progress.accept(80);
                dbWriter.submit(() -> DatabaseManager.saveStrMap(context.strMap, context.stringAnnoMap, context.methodMap, context.classMap));
                dbWriter.submit(() -> DatabaseManager.saveSpringController(context.controllers));
                dbWriter.submit(() -> DatabaseManager.saveSpringInterceptor(context.interceptors, context.classMap));
                dbWriter.submit(() -> DatabaseManager.saveServlets(context.servlets, context.classMap));
                dbWriter.submit(() -> DatabaseManager.saveFilters(context.filters, context.classMap));
                dbWriter.submit(() -> DatabaseManager.saveListeners(context.listeners, context.classMap));

                progress.accept(90);
            } else {
                progress.accept(70);
                clearCachedBytes(context.classFileList);
                dbWriter.submit(() -> DatabaseManager.saveMethodCalls(
                        context.methodCalls, context.classMap, context.methodCallMeta, context.callSites));
            }

            DeferredFileWriter.awaitAndStop();
            CoreUtil.cleanupEmptyTempDirs();
            long dbWriteStartNs = stageStartNs;
            dbWriter.await();
            GraphProjectionBuilder.projectCurrentBuild(DatabaseManager.getBuildSeq(), quickMode, callGraphMode.getConfigValue());
            DatabaseManager.finalizeBuild();
            finalizePending = false;
            refreshCachesAfterBuild();
            logger.info("build database finish");
            logger.info("build stage db-write/finalize: {} ms (dbSize={})",
                    msSince(dbWriteStartNs),
                    formatSizeInMB(getFileSize()));
            long edgeCount = countEdges(context.methodCalls);
            long fileSizeBytes = getFileSize();
            String fileSizeMB = formatSizeInMB(fileSizeBytes);
            progress.accept(100);

            ConfigFile config = new ConfigFile();
            config.setTempPath(Const.tempDir);
            config.setDbPath(Const.dbFile);
            config.setJarPath(jarPath == null ? "" : jarPath.toAbsolutePath().toString());
            config.setDbSize(fileSizeMB);
            config.setLang("en");
            config.setDecompileCacheSize(String.valueOf(DecompileEngine.getCacheCapacity()));
            EngineContext.setEngine(new CoreEngine(config));

            BuildResult result = new BuildResult(
                    DatabaseManager.getBuildSeq(),
                    jarList.size(),
                    context.classFileList.size(),
                    context.discoveredClasses.size(),
                    context.discoveredMethods.size(),
                    edgeCount,
                    fileSizeBytes,
                    fileSizeMB,
                    quickMode,
                    enableFixMethodImpl,
                    callGraphMode.getConfigValue(),
                    dispatchMetrics.dispatchEdgesAdded,
                    dispatchMetrics.typedDispatchEdgesAdded,
                    ptaResultSummary == null ? -1 : ptaResultSummary.getEdgesAdded(),
                    ptaResultSummary == null ? -1 : ptaResultSummary.getContextMethodsProcessed(),
                    ptaResultSummary == null ? -1 : ptaResultSummary.getInvokeSitesProcessed(),
                    ptaResultSummary == null ? -1 : ptaResultSummary.getIncrementalSkips()
            );

            clearBuildContext(context, quickMode);
            cleaned = true;
            return result;
        } finally {
            DeferredFileWriter.awaitAndStop();
            CoreUtil.cleanupEmptyTempDirs();
            dbWriter.close();
            if (!cleaned) {
                clearBuildContext(context, quickMode);
            }
            if (finalizePending) {
                DatabaseManager.finalizeBuild();
            }
            DatabaseManager.setBuilding(false);
        }
    }

    private static long msSince(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }

    private static void clearBuildContext(BuildContext ctx, boolean quickMode) {
        if (ctx == null) {
            BytecodeCache.clear();
            return;
        }
        clearCachedBytes(ctx.classFileList);
        ctx.classFileList.clear();
        ctx.discoveredClasses.clear();
        ctx.discoveredMethods.clear();
        ctx.methodsInClassMap.clear();
        ctx.classMap.clear();
        ctx.methodMap.clear();
        ctx.methodCalls.clear();
        ctx.methodCallMeta.clear();
        ctx.strMap.clear();
        ctx.resources.clear();
        BytecodeCache.clear();
        if (!quickMode && ctx.inheritanceMap != null) {
            ctx.inheritanceMap.getInheritanceMap().clear();
            ctx.inheritanceMap.getSubClassMap().clear();
        }
        ctx.inheritanceMap = null;
        ctx.controllers.clear();
        ctx.interceptors.clear();
        ctx.servlets.clear();
        ctx.filters.clear();
        ctx.listeners.clear();
        ctx.stringAnnoMap.clear();
        ctx.instantiatedClasses.clear();
        ctx.callSites.clear();
        // Avoid forcing GC after every build; it can hurt throughput and also makes failures
        // harder to reason about in tests. Enable explicitly if needed.
        if (Boolean.getBoolean("jar.analyzer.build.forceGc")) {
            System.gc();
        }
    }

    private static void clearCachedBytes(Set<ClassFileEntity> classFileList) {
        if (classFileList == null || classFileList.isEmpty()) {
            return;
        }
        for (ClassFileEntity cf : classFileList) {
            if (cf != null) {
                cf.clearCachedBytes();
            }
        }
    }

    private static Path resolveJarRoot(Path classPath) {
        Path tempRoot = Paths.get(Const.tempDir).toAbsolutePath().normalize();
        if (classPath == null) {
            return tempRoot;
        }
        Path current = classPath.toAbsolutePath().normalize();
        while (current != null && current.getParent() != null && !current.getParent().equals(tempRoot)) {
            current = current.getParent();
        }
        if (current != null && current.getParent() != null && current.getParent().equals(tempRoot)) {
            return current;
        }
        return tempRoot;
    }

    private static long countEdges(Map<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls) {
        if (methodCalls == null || methodCalls.isEmpty()) {
            return 0L;
        }
        long count = 0L;
        for (Set<MethodReference.Handle> callees : methodCalls.values()) {
            if (callees != null) {
                count += callees.size();
            }
        }
        return count;
    }

    private static long getFileSize() {
        File file = new File(Const.dbFile);
        return file.length();
    }

    private static String formatSizeInMB(long fileSizeBytes) {
        double fileSizeMB = (double) fileSizeBytes / (1024 * 1024);
        return String.format("%.2f MB", fileSizeMB);
    }

    private static String resolveOverrideReason(Map<ClassReference.Handle, ClassReference> classMap,
                                                MethodReference.Handle base) {
        if (base == null) {
            return "override";
        }
        ClassReference baseClass = classMap == null ? null : classMap.get(base.getClassReference());
        if (baseClass == null) {
            return "override";
        }
        if (baseClass.isInterface()) {
            return "interface";
        }
        String name = baseClass.getName();
        if (name == null || name.isEmpty()) {
            return "inheritance";
        }
        return switch (name) {
            case "java/lang/Object" -> "object";
            case "java/io/Serializable" -> "serializable";
            case "java/lang/Iterable" -> "iterable";
            case "java/lang/Cloneable" -> "cloneable";
            default -> "inheritance";
        };
    }

    private static CallGraphMode resolveCallGraphMode() {
        String raw = System.getProperty(CALL_GRAPH_MODE_PROP);
        if (raw == null || raw.trim().isEmpty()) {
            return CallGraphMode.RTA;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "cha" -> CallGraphMode.CHA;
            case "hybrid", "cha+rta", "rta+cha", "mix" -> CallGraphMode.HYBRID;
            case "pta", "points-to", "points_to", "cspta" -> CallGraphMode.PTA;
            default -> CallGraphMode.RTA;
        };
    }

    private static boolean isPtaBaselineDispatchEnabled() {
        String raw = System.getProperty(PTA_BASELINE_DISPATCH_PROP);
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        return !"false".equalsIgnoreCase(raw.trim());
    }

    private static DispatchMetrics expandDispatchEdges(
            BuildContext context,
            CallGraphMode mode,
            Set<ClassReference.Handle> instantiated,
            List<CallSiteEntity> callSites) {
        if (context == null || context.methodCalls == null || context.methodCalls.isEmpty()
                || context.methodMap == null || context.classMap == null || context.inheritanceMap == null) {
            return DispatchMetrics.empty();
        }
        int dispatchAdded = 0;
        int typedAdded = 0;
        int rtaAdded = 0;
        int chaAdded = 0;

        if (mode == CallGraphMode.CHA) {
            dispatchAdded = DispatchCallResolver.expandVirtualCalls(
                    context.methodCalls,
                    context.methodCallMeta,
                    context.methodMap,
                    context.classMap,
                    context.inheritanceMap,
                    null);
        } else if (mode == CallGraphMode.HYBRID) {
            rtaAdded = DispatchCallResolver.expandVirtualCalls(
                    context.methodCalls,
                    context.methodCallMeta,
                    context.methodMap,
                    context.classMap,
                    context.inheritanceMap,
                    instantiated);
            chaAdded = DispatchCallResolver.expandVirtualCalls(
                    context.methodCalls,
                    context.methodCallMeta,
                    context.methodMap,
                    context.classMap,
                    context.inheritanceMap,
                    null);
            dispatchAdded = rtaAdded + chaAdded;
        } else {
            // RTA + PTA-baseline both share instantiated-set dispatch.
            dispatchAdded = DispatchCallResolver.expandVirtualCalls(
                    context.methodCalls,
                    context.methodCallMeta,
                    context.methodMap,
                    context.classMap,
                    context.inheritanceMap,
                    instantiated);
        }

        if (callSites != null && !callSites.isEmpty()) {
            typedAdded = TypedDispatchResolver.expandWithTypes(
                    context.methodCalls,
                    context.methodCallMeta,
                    context.methodMap,
                    context.classMap,
                    context.inheritanceMap,
                    callSites);
        }
        return new DispatchMetrics(dispatchAdded, typedAdded, rtaAdded, chaAdded);
    }

    private enum CallGraphMode {
        RTA("rta"),
        CHA("cha"),
        HYBRID("hybrid"),
        PTA("pta");

        private final String configValue;

        CallGraphMode(String configValue) {
            this.configValue = configValue;
        }

        public String getConfigValue() {
            return configValue;
        }
    }

    private static final class DispatchMetrics {
        private final int dispatchEdgesAdded;
        private final int typedDispatchEdgesAdded;
        private final int rtaDispatchAdded;
        private final int chaDispatchAdded;

        private DispatchMetrics(int dispatchEdgesAdded,
                                int typedDispatchEdgesAdded,
                                int rtaDispatchAdded,
                                int chaDispatchAdded) {
            this.dispatchEdgesAdded = dispatchEdgesAdded;
            this.typedDispatchEdgesAdded = typedDispatchEdgesAdded;
            this.rtaDispatchAdded = rtaDispatchAdded;
            this.chaDispatchAdded = chaDispatchAdded;
        }

        private static DispatchMetrics empty() {
            return new DispatchMetrics(0, 0, 0, 0);
        }
    }

    public static final class BuildResult {
        private final long buildSeq;
        private final int jarCount;
        private final int classFileCount;
        private final int classCount;
        private final int methodCount;
        private final long edgeCount;
        private final long dbSizeBytes;
        private final String dbSizeLabel;
        private final boolean quickMode;
        private final boolean fixMethodImplEnabled;
        private final String callGraphMode;
        private final int dispatchEdgesAdded;
        private final int typedDispatchEdgesAdded;
        private final int ptaEdgesAdded;
        private final int ptaContextMethodsProcessed;
        private final int ptaInvokeSitesProcessed;
        private final int ptaIncrementalSkips;

        public BuildResult(long buildSeq,
                           int jarCount,
                           int classFileCount,
                           int classCount,
                           int methodCount,
                           long edgeCount,
                           long dbSizeBytes,
                           String dbSizeLabel,
                           boolean quickMode,
                           boolean fixMethodImplEnabled,
                           String callGraphMode) {
            this(buildSeq,
                    jarCount,
                    classFileCount,
                    classCount,
                    methodCount,
                    edgeCount,
                    dbSizeBytes,
                    dbSizeLabel,
                    quickMode,
                    fixMethodImplEnabled,
                    callGraphMode,
                    0,
                    0,
                    -1,
                    -1,
                    -1,
                    -1);
        }

        public BuildResult(long buildSeq,
                           int jarCount,
                           int classFileCount,
                           int classCount,
                           int methodCount,
                           long edgeCount,
                           long dbSizeBytes,
                           String dbSizeLabel,
                           boolean quickMode,
                           boolean fixMethodImplEnabled,
                           String callGraphMode,
                           int dispatchEdgesAdded,
                           int typedDispatchEdgesAdded,
                           int ptaEdgesAdded,
                           int ptaContextMethodsProcessed,
                           int ptaInvokeSitesProcessed,
                           int ptaIncrementalSkips) {
            this.buildSeq = buildSeq;
            this.jarCount = jarCount;
            this.classFileCount = classFileCount;
            this.classCount = classCount;
            this.methodCount = methodCount;
            this.edgeCount = edgeCount;
            this.dbSizeBytes = dbSizeBytes;
            this.dbSizeLabel = dbSizeLabel;
            this.quickMode = quickMode;
            this.fixMethodImplEnabled = fixMethodImplEnabled;
            this.callGraphMode = callGraphMode;
            this.dispatchEdgesAdded = dispatchEdgesAdded;
            this.typedDispatchEdgesAdded = typedDispatchEdgesAdded;
            this.ptaEdgesAdded = ptaEdgesAdded;
            this.ptaContextMethodsProcessed = ptaContextMethodsProcessed;
            this.ptaInvokeSitesProcessed = ptaInvokeSitesProcessed;
            this.ptaIncrementalSkips = ptaIncrementalSkips;
        }

        public long getBuildSeq() {
            return buildSeq;
        }

        public int getJarCount() {
            return jarCount;
        }

        public int getClassFileCount() {
            return classFileCount;
        }

        public int getClassCount() {
            return classCount;
        }

        public int getMethodCount() {
            return methodCount;
        }

        public long getEdgeCount() {
            return edgeCount;
        }

        public long getDbSizeBytes() {
            return dbSizeBytes;
        }

        public String getDbSizeLabel() {
            return dbSizeLabel;
        }

        public boolean isQuickMode() {
            return quickMode;
        }

        public boolean isFixMethodImplEnabled() {
            return fixMethodImplEnabled;
        }

        public String getCallGraphMode() {
            return callGraphMode;
        }

        public int getDispatchEdgesAdded() {
            return dispatchEdgesAdded;
        }

        public int getTypedDispatchEdgesAdded() {
            return typedDispatchEdgesAdded;
        }

        public int getPtaEdgesAdded() {
            return ptaEdgesAdded;
        }

        public int getPtaContextMethodsProcessed() {
            return ptaContextMethodsProcessed;
        }

        public int getPtaInvokeSitesProcessed() {
            return ptaInvokeSitesProcessed;
        }

        public int getPtaIncrementalSkips() {
            return ptaIncrementalSkips;
        }
    }

}
