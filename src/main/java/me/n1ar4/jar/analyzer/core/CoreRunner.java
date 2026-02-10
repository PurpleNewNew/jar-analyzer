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
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.DecompileEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.index.IndexEngine;
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
        return run(jarPath, rtJarPath, fixClass, false, true, null);
    }

    public static BuildResult run(Path jarPath,
                                  Path rtJarPath,
                                  boolean fixClass,
                                  boolean quickMode,
                                  boolean enableFixMethodImpl,
                                  IntConsumer progressConsumer) {
        IntConsumer progress = progressConsumer == null ? NOOP_PROGRESS : progressConsumer;

        if (AnalyzeEnv.isCli) {
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
        AnalyzeEnv.use(context);
        final long buildStartNs = System.nanoTime();
        long stageStartNs = buildStartNs;
        BuildDbWriter dbWriter = new BuildDbWriter();
        boolean finalizePending = true;
        boolean cleaned = false;
        try {
            Map<String, Integer> jarIdMap = new HashMap<>();

            List<ClassFileEntity> cfs;
            progress.accept(10);
            // Nested jars are handled only via AnalyzeEnv.jarsInJar (JarUtil).
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
            cfs = CoreUtil.getAllClassesFromJars(jarList, jarIdMap, AnalyzeEnv.resources);
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
            AnalyzeEnv.classFileList.addAll(cfs);
            logger.info("get all class");
            dbWriter.submit(() -> DatabaseManager.saveClassFiles(AnalyzeEnv.classFileList));
            dbWriter.submit(() -> DatabaseManager.saveResources(AnalyzeEnv.resources));
            progress.accept(20);
            DiscoveryRunner.start(AnalyzeEnv.classFileList, AnalyzeEnv.discoveredClasses,
                    AnalyzeEnv.discoveredMethods, AnalyzeEnv.classMap,
                    AnalyzeEnv.methodMap, AnalyzeEnv.stringAnnoMap);
            logger.info("build stage discovery: {} ms (classFiles={}, classes={}, methods={})",
                    msSince(stageStartNs),
                    AnalyzeEnv.classFileList.size(),
                    AnalyzeEnv.discoveredClasses.size(),
                    AnalyzeEnv.discoveredMethods.size());
            stageStartNs = System.nanoTime();
            dbWriter.submit(() -> DatabaseManager.saveClassInfo(AnalyzeEnv.discoveredClasses));
            progress.accept(25);
            dbWriter.submit(() -> DatabaseManager.saveMethods(AnalyzeEnv.discoveredMethods));
            progress.accept(30);
            logger.info("analyze class finish");
            for (MethodReference mr : AnalyzeEnv.discoveredMethods) {
                ClassReference.Handle ch = mr.getClassReference();
                AnalyzeEnv.methodsInClassMap
                        .computeIfAbsent(ch, k -> new ArrayList<>())
                        .add(mr);
            }
            progress.accept(35);
            ClassAnalysisRunner.start(AnalyzeEnv.classFileList,
                    AnalyzeEnv.methodCalls,
                    AnalyzeEnv.methodMap,
                    AnalyzeEnv.methodCallMeta,
                    AnalyzeEnv.instantiatedClasses,
                    AnalyzeEnv.strMap,
                    AnalyzeEnv.classMap,
                    AnalyzeEnv.controllers,
                    AnalyzeEnv.interceptors,
                    AnalyzeEnv.servlets,
                    AnalyzeEnv.filters,
                    AnalyzeEnv.listeners,
                    !quickMode,
                    !quickMode,
                    !quickMode);
            logger.info("build stage class-analysis: {} ms (edges={})",
                    msSince(stageStartNs),
                    countEdges(AnalyzeEnv.methodCalls));
            stageStartNs = System.nanoTime();
            progress.accept(40);

            BytecodeSymbolRunner.Result symbolResult = null;
            if (!quickMode && BytecodeSymbolRunner.isEnabled()) {
                symbolResult = BytecodeSymbolRunner.start(AnalyzeEnv.classFileList);
                List<CallSiteEntity> callSites = symbolResult.getCallSites();
                List<LocalVarEntity> localVars = symbolResult.getLocalVars();
                dbWriter.submit(() -> DatabaseManager.saveCallSites(callSites));
                dbWriter.submit(() -> DatabaseManager.saveLocalVars(localVars));
                logger.info("build stage symbol: {} ms (callSites={}, localVars={})",
                        msSince(stageStartNs),
                        callSites == null ? 0 : callSites.size(),
                        localVars == null ? 0 : localVars.size());
                stageStartNs = System.nanoTime();
            }

            if (!quickMode) {
                context.inheritanceMap = InheritanceRunner.derive(AnalyzeEnv.classMap);
                AnalyzeEnv.inheritanceMap = context.inheritanceMap;
                progress.accept(50);
                logger.info("build inheritance");
                Map<MethodReference.Handle, Set<MethodReference.Handle>> implMap =
                        InheritanceRunner.getAllMethodImplementations(AnalyzeEnv.inheritanceMap, AnalyzeEnv.methodMap);
                dbWriter.submit(() -> DatabaseManager.saveImpls(implMap));
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
                                AnalyzeEnv.methodCalls.computeIfAbsent(k, kk -> new HashSet<>());
                        // 增加所有的 override 方法
                        for (MethodReference.Handle impl : v) {
                            MethodCallUtils.addCallee(calls, impl);
                            String reason = resolveOverrideReason(k);
                            MethodCallMeta.record(AnalyzeEnv.methodCallMeta, MethodCallKey.of(k, impl),
                                    MethodCallMeta.TYPE_OVERRIDE, MethodCallMeta.CONF_LOW, reason);
                        }
                    }
                    Set<ClassReference.Handle> instantiated = AnalyzeEnv.instantiatedClasses;
                    if (instantiated == null || instantiated.isEmpty()) {
                        // Fallback for legacy builds / failures: full scan.
                        instantiated = DispatchCallResolver.collectInstantiatedClasses(AnalyzeEnv.classFileList);
                    }
                    int dispatchAdded = DispatchCallResolver.expandVirtualCalls(
                            AnalyzeEnv.methodCalls,
                            AnalyzeEnv.methodCallMeta,
                            AnalyzeEnv.methodMap,
                            AnalyzeEnv.classMap,
                            AnalyzeEnv.inheritanceMap,
                            instantiated);
                    logger.info("dispatch edges added: {}", dispatchAdded);
                    if (symbolResult != null && !symbolResult.getCallSites().isEmpty()) {
                        int typedAdded = TypedDispatchResolver.expandWithTypes(
                                AnalyzeEnv.methodCalls,
                                AnalyzeEnv.methodCallMeta,
                                AnalyzeEnv.methodMap,
                                AnalyzeEnv.classMap,
                                AnalyzeEnv.inheritanceMap,
                                symbolResult.getCallSites());
                        logger.info("typed dispatch edges added: {}", typedAdded);
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
                        countEdges(AnalyzeEnv.methodCalls));
                stageStartNs = System.nanoTime();

                clearCachedBytes(AnalyzeEnv.classFileList);
                dbWriter.submit(() -> DatabaseManager.saveMethodCalls(AnalyzeEnv.methodCalls));
                progress.accept(70);
                logger.info("build extra inheritance");
                progress.accept(80);
                dbWriter.submit(() -> DatabaseManager.saveStrMap(AnalyzeEnv.strMap, AnalyzeEnv.stringAnnoMap));
                dbWriter.submit(() -> DatabaseManager.saveSpringController(AnalyzeEnv.controllers));
                dbWriter.submit(() -> DatabaseManager.saveSpringInterceptor(AnalyzeEnv.interceptors));
                dbWriter.submit(() -> DatabaseManager.saveServlets(AnalyzeEnv.servlets));
                dbWriter.submit(() -> DatabaseManager.saveFilters(AnalyzeEnv.filters));
                dbWriter.submit(() -> DatabaseManager.saveListeners(AnalyzeEnv.listeners));

                progress.accept(90);
            } else {
                progress.accept(70);
                clearCachedBytes(AnalyzeEnv.classFileList);
                dbWriter.submit(() -> DatabaseManager.saveMethodCalls(AnalyzeEnv.methodCalls));
            }

            DeferredFileWriter.awaitAndStop();
            CoreUtil.cleanupEmptyTempDirs();
            long dbWriteStartNs = stageStartNs;
            dbWriter.await();
            DatabaseManager.finalizeBuild();
            finalizePending = false;
            refreshCachesAfterBuild();
            logger.info("build database finish");
            logger.info("build stage db-write/finalize: {} ms (dbSize={})",
                    msSince(dbWriteStartNs),
                    formatSizeInMB(getFileSize()));
            long edgeCount = countEdges(AnalyzeEnv.methodCalls);
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
                    AnalyzeEnv.classFileList.size(),
                    AnalyzeEnv.discoveredClasses.size(),
                    AnalyzeEnv.discoveredMethods.size(),
                    edgeCount,
                    fileSizeBytes,
                    fileSizeMB,
                    quickMode,
                    enableFixMethodImpl
            );

            clearAnalyzeEnv(quickMode);
            cleaned = true;
            return result;
        } finally {
            DeferredFileWriter.awaitAndStop();
            CoreUtil.cleanupEmptyTempDirs();
            dbWriter.close();
            if (!cleaned) {
                clearAnalyzeEnv(quickMode);
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

    private static void clearAnalyzeEnv(boolean quickMode) {
        clearCachedBytes(AnalyzeEnv.classFileList);
        AnalyzeEnv.classFileList.clear();
        AnalyzeEnv.discoveredClasses.clear();
        AnalyzeEnv.discoveredMethods.clear();
        AnalyzeEnv.methodsInClassMap.clear();
        AnalyzeEnv.classMap.clear();
        AnalyzeEnv.methodMap.clear();
        AnalyzeEnv.methodCalls.clear();
        AnalyzeEnv.methodCallMeta.clear();
        AnalyzeEnv.strMap.clear();
        AnalyzeEnv.resources.clear();
        BytecodeCache.clear();
        if (!quickMode && AnalyzeEnv.inheritanceMap != null) {
            AnalyzeEnv.inheritanceMap.getInheritanceMap().clear();
            AnalyzeEnv.inheritanceMap.getSubClassMap().clear();
        }
        AnalyzeEnv.controllers.clear();
        AnalyzeEnv.interceptors.clear();
        AnalyzeEnv.servlets.clear();
        AnalyzeEnv.filters.clear();
        AnalyzeEnv.listeners.clear();
        AnalyzeEnv.stringAnnoMap.clear();
        AnalyzeEnv.instantiatedClasses.clear();
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

    private static String resolveOverrideReason(MethodReference.Handle base) {
        if (base == null) {
            return "override";
        }
        ClassReference baseClass = AnalyzeEnv.classMap.get(base.getClassReference());
        if (baseClass == null) {
            return "override";
        }
        String name = baseClass.getName();
        if (baseClass.isInterface()) {
            return "interface";
        }
        if ("java/lang/Object".equals(name)) {
            return "object";
        }
        if ("java/io/Serializable".equals(name)) {
            return "serializable";
        }
        if ("java/lang/Iterable".equals(name)) {
            return "iterable";
        }
        if ("java/lang/Cloneable".equals(name)) {
            return "cloneable";
        }
        if (name != null && name.startsWith("java/util/") && baseClass.isInterface()) {
            return "util_interface";
        }
        return "inheritance";
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

        public BuildResult(long buildSeq,
                           int jarCount,
                           int classFileCount,
                           int classCount,
                           int methodCount,
                           long edgeCount,
                           long dbSizeBytes,
                           String dbSizeLabel,
                           boolean quickMode,
                           boolean fixMethodImplEnabled) {
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
    }

}
