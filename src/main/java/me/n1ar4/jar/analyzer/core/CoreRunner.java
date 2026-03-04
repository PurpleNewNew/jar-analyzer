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
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.core.runtime.JdkArchiveResolver;
import me.n1ar4.jar.analyzer.core.runtime.JdkArchiveResolver.JdkResolution;
import me.n1ar4.jar.analyzer.core.scope.ArchiveScopeClassifier;
import me.n1ar4.jar.analyzer.core.scope.ArchiveScopeClassifier.ScopeSummary;
import me.n1ar4.jar.analyzer.core.taie.TaieAnalysisRunner;
import me.n1ar4.jar.analyzer.core.taie.TaieAnalysisRunner.AnalysisProfile;
import me.n1ar4.jar.analyzer.core.taie.TaieAnalysisRunner.TaieRunResult;
import me.n1ar4.jar.analyzer.core.taie.TaieEdgeMapper;
import me.n1ar4.jar.analyzer.core.taie.TaieEdgeMapper.MappingResult;
import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectBuildMode;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.jar.analyzer.engine.index.IndexEngine;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jGraphBuildService;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.BytecodeCache;
import me.n1ar4.jar.analyzer.utils.ClasspathResolver;
import me.n1ar4.jar.analyzer.utils.CoreUtil;
import me.n1ar4.jar.analyzer.utils.DeferredFileWriter;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public class CoreRunner {
    private static final Logger logger = LogManager.getLogger();
    private static final Neo4jGraphBuildService GRAPH_BUILD_SERVICE = new Neo4jGraphBuildService();
    private static final IntConsumer NOOP_PROGRESS = p -> {
    };

    private static final String ALL_COMMON_POLICY_PROP = "jar.analyzer.all-common.policy";
    private static final String ALL_COMMON_POLICY_CONTINUE = "continue-no-callgraph";
    private static final String CALL_GRAPH_ENGINE_TAIE = "taie";
    private static final String CALL_GRAPH_ENGINE_DISABLED = "disabled-no-target";

    private static void refreshCachesAfterBuild() {
        try {
            IndexEngine.closeAll();
        } catch (Exception ex) {
            logger.debug("close index fail: {}", ex.toString());
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

    public static BuildResult run(Path jarPath,
                                  Path rtJarPath,
                                  boolean fixClass,
                                  boolean quickMode,
                                  IntConsumer progressConsumer,
                                  boolean clearExistingDbData) {
        return run(jarPath, rtJarPath, fixClass, quickMode, progressConsumer, clearExistingDbData, false);
    }

    /**
     * Build database for the given bytecode input.
     *
     * @param clearExistingDbData if true, clear in-memory legacy caches before rebuilding neo4j graph data.
     * @param includeNested if true, include nested jars when resolving classpath.
     */
    public static BuildResult run(Path jarPath,
                                  Path rtJarPath,
                                  boolean fixClass,
                                  boolean quickMode,
                                  IntConsumer progressConsumer,
                                  boolean clearExistingDbData,
                                  boolean includeNested) {
        IntConsumer progress = progressConsumer == null ? NOOP_PROGRESS : progressConsumer;

        if (clearExistingDbData) {
            DatabaseManager.clearAllData();
        }
        DatabaseManager.setBuilding(true);
        try {
            DatabaseManager.prepareBuild();
        } catch (Throwable t) {
            DatabaseManager.setBuilding(false);
            throw t;
        }

        long buildSeq = DatabaseManager.getBuildSeq();
        BuildContext context = new BuildContext();
        long stageStartNs = System.nanoTime();
        boolean finalizePending = true;
        boolean cleaned = false;
        try {
            try {
                ProjectModel currentModel = WorkspaceContext.getProjectModel();
                boolean explicitProjectMode = currentModel != null
                        && currentModel.buildMode() == ProjectBuildMode.PROJECT;
                if (!explicitProjectMode) {
                    Path runtimeForModel = rtJarPath;
                    if (runtimeForModel == null) {
                        runtimeForModel = WorkspaceContext.runtimePath();
                    }
                    WorkspaceContext.ensureArtifactProjectModel(
                            jarPath,
                            runtimeForModel,
                            includeNested
                    );
                }
            } catch (Exception ex) {
                logger.debug("prepare artifact project model fail: {}", ex.toString());
            }

            Map<String, Integer> jarIdMap = new LinkedHashMap<>();
            int[] nextJarId = {1};

            progress.accept(10);
            List<String> userArchives = ClasspathResolver.resolveInputArchives(
                    jarPath, null, !quickMode, includeNested);
            List<Path> normalizedArchives = normalizePaths(userArchives);
            try {
                WorkspaceContext.updateAnalyzedArchives(normalizedArchives);
                DatabaseManager.saveProjectModel(WorkspaceContext.getProjectModel());
            } catch (Exception ex) {
                logger.debug("save project model fail: {}", ex.toString());
            }

            Path runtimeHint = rtJarPath == null ? WorkspaceContext.runtimePath() : rtJarPath;
            ScopeSummary scopeSummary = ArchiveScopeClassifier.classifyArchives(
                    normalizedArchives, jarPath, runtimeHint);
            Map<Path, ProjectOrigin> archiveOrigins = scopeSummary.originsByArchive();
            Map<Integer, ProjectOrigin> jarOriginsById = new HashMap<>();

            if (Files.isDirectory(jarPath)) {
                logger.info("input is a dir");
            } else {
                logger.info("input is a jar file");
            }

            for (String archive : userArchives) {
                if (archive == null || archive.isBlank()) {
                    continue;
                }
                String lower = archive.toLowerCase(Locale.ROOT);
                if (!lower.endsWith(".jar") && !lower.endsWith(".war")) {
                    continue;
                }
                DatabaseManager.saveJar(archive);
                int jarId = jarIdMap.computeIfAbsent(archive, ignore -> nextJarId[0]++);
                Path archivePath;
                try {
                    archivePath = Paths.get(archive).toAbsolutePath().normalize();
                } catch (Exception ex) {
                    archivePath = Paths.get(archive).normalize();
                }
                ProjectOrigin origin = archiveOrigins.get(archivePath);
                if (origin == null) {
                    origin = ProjectOrigin.APP;
                }
                jarOriginsById.put(jarId, origin);
            }

            List<ClassFileEntity> classFiles = CoreUtil.getAllClassesFromJars(
                    userArchives, jarIdMap, context.resources);
            for (ClassFileEntity cf : classFiles) {
                String className = cf.getClassName();
                if (!fixClass) {
                    int i = className.indexOf("classes");
                    if (className.contains("BOOT-INF") || className.contains("WEB-INF")) {
                        className = className.substring(i + 8);
                    }
                    cf.setClassName(className);
                } else {
                    Path parPath = resolveJarRoot(cf.getPath());
                    FixClassVisitor cv = new FixClassVisitor();
                    byte[] classBytes = cf.getFile();
                    if (classBytes == null || classBytes.length == 0) {
                        continue;
                    }
                    ClassReader cr = new ClassReader(classBytes);
                    cr.accept(cv, Const.HeaderASMOptions);
                    String actualName = cv.getName();
                    Path path = parPath.resolve(Paths.get(actualName));
                    File file = path.toFile();
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
                        logger.error("fix path copy bytes error: {}", ex.toString(), ex);
                    }
                    cf.setClassName(actualName + ".class");
                    cf.setPath(Paths.get(className));
                }
            }

            progress.accept(15);
            context.classFileList.addAll(classFiles);
            progress.accept(20);
            DiscoveryRunner.start(
                    context.classFileList,
                    context.discoveredClasses,
                    context.discoveredMethods,
                    context.classMap,
                    context.methodMap,
                    context.stringAnnoMap
            );
            logger.info("build stage discovery: {} ms (classFiles={}, classes={}, methods={})",
                    msSince(stageStartNs),
                    context.classFileList.size(),
                    context.discoveredClasses.size(),
                    context.discoveredMethods.size());
            stageStartNs = System.nanoTime();
            progress.accept(30);

            progress.accept(35);
            ClassAnalysisRunner.start(
                    context.classFileList,
                    context.methodCalls,
                    context.methodMap,
                    context.methodCallMeta,
                    context.strMap,
                    context.classMap,
                    context.controllers,
                    context.interceptors,
                    context.servlets,
                    context.filters,
                    context.listeners,
                    !quickMode,
                    !quickMode,
                    !quickMode
            );
            logger.info("build stage class-analysis: {} ms (bytecodeEdges={})",
                    msSince(stageStartNs),
                    countEdges(context.methodCalls));
            stageStartNs = System.nanoTime();

            progress.accept(40);
            BytecodeSymbolRunner.Result symbolResult = null;
            if (!quickMode) {
                symbolResult = BytecodeSymbolRunner.start(context.classFileList);
                List<CallSiteEntity> callSites = symbolResult.getCallSites();
                context.callSites.clear();
                if (callSites != null && !callSites.isEmpty()) {
                    context.callSites.addAll(callSites);
                }
                logger.info("build stage symbol: {} ms (callSites={}, localVars={})",
                        msSince(stageStartNs),
                        callSites == null ? 0 : callSites.size(),
                        symbolResult.getLocalVars() == null ? 0 : symbolResult.getLocalVars().size());
                stageStartNs = System.nanoTime();
            }

            progress.accept(55);
            JdkResolution jdkResolution = JdkArchiveResolver.resolve(runtimeHint);
            AnalysisProfile profile = TaieAnalysisRunner.resolveProfile();
            TaieEdgeMapper.EdgePolicy edgePolicy = TaieEdgeMapper.resolveEdgePolicy();
            List<Path> appArchives = ArchiveScopeClassifier.pickAppArchives(scopeSummary);
            List<Path> libraryArchives = ArchiveScopeClassifier.pickLibraryArchives(scopeSummary);

            LinkedHashSet<Path> taieClasspath = new LinkedHashSet<>();
            taieClasspath.addAll(appArchives);
            taieClasspath.addAll(libraryArchives);
            taieClasspath.addAll(jdkResolution.archives());

            String callGraphEngine = CALL_GRAPH_ENGINE_TAIE;
            String callGraphModeMeta = "taie:" + profile.value();
            int taieEdgeCount = 0;

            if (scopeSummary.targetArchiveCount() <= 0) {
                String policy = System.getProperty(ALL_COMMON_POLICY_PROP, ALL_COMMON_POLICY_CONTINUE);
                if (policy != null && !policy.isBlank()
                        && "fail".equals(policy.trim().toLowerCase(Locale.ROOT))) {
                    throw new IllegalStateException("no target archives found and all-common policy forbids continue");
                }
                context.methodCalls.clear();
                context.methodCallMeta.clear();
                callGraphEngine = CALL_GRAPH_ENGINE_DISABLED;
                callGraphModeMeta = CALL_GRAPH_ENGINE_DISABLED;
                logger.info("all archives are common/sdk, continue without call graph (policy={})",
                        policy == null || policy.isBlank() ? ALL_COMMON_POLICY_CONTINUE : policy);
            } else {
                // Hard switch: Tai-e is the only call graph source for target builds.
                context.methodCalls.clear();
                context.methodCallMeta.clear();

                String mainClass = resolveMainClass(jarPath, appArchives, context.discoveredMethods);
                TaieRunResult taieResult = TaieAnalysisRunner.run(
                        appArchives,
                        new ArrayList<>(taieClasspath),
                        profile,
                        mainClass
                );
                if (!taieResult.success() || taieResult.callGraph() == null) {
                    String reason = taieResult.reason() == null ? "" : taieResult.reason().trim();
                    throw new IllegalStateException("tai-e call graph failed: " + reason);
                }
                MappingResult mapped = TaieEdgeMapper.map(
                        taieResult.callGraph(),
                        context.methodMap,
                        jarOriginsById,
                        edgePolicy
                );
                if (mapped.syntheticMethods() != null && !mapped.syntheticMethods().isEmpty()) {
                    context.discoveredMethods.addAll(mapped.syntheticMethods());
                }
                if (mapped.methodCalls() != null && !mapped.methodCalls().isEmpty()) {
                    context.methodCalls.putAll(mapped.methodCalls());
                }
                if (mapped.methodCallMeta() != null && !mapped.methodCallMeta().isEmpty()) {
                    context.methodCallMeta.putAll(mapped.methodCallMeta());
                }
                taieEdgeCount = mapped.keptEdges();
                logger.info("build stage taie: {} ms (profile={}, edgePolicy={}, totalEdges={}, keptEdges={}, skippedByPolicy={}, unresolvedCaller={}, unresolvedCallee={})",
                        taieResult.elapsedMs(),
                        profile.value(),
                        mapped.edgePolicy(),
                        mapped.totalEdges(),
                        mapped.keptEdges(),
                        mapped.skippedByPolicy(),
                        mapped.unresolvedCaller(),
                        mapped.unresolvedCallee());
            }
            logger.info("build stage taie total: {} ms", msSince(stageStartNs));
            stageStartNs = System.nanoTime();

            clearCachedBytes(context.classFileList);
            progress.accept(70);
            progress.accept(90);

            DeferredFileWriter.awaitAndStop();
            CoreUtil.cleanupEmptyTempDirs();
            long dbWriteStartNs = stageStartNs;
            List<LocalVarEntity> localVars = symbolResult == null ? Collections.emptyList() : symbolResult.getLocalVars();

            DatabaseManager.runAtomicUpdate(() -> {
                DatabaseManager.saveClassFiles(context.classFileList);
                DatabaseManager.saveClassInfo(context.discoveredClasses);
                DatabaseManager.saveMethods(context.discoveredMethods);
                DatabaseManager.saveStrMap(
                        context.strMap,
                        context.stringAnnoMap
                );
                DatabaseManager.saveResources(context.resources);
                DatabaseManager.saveCallSites(context.callSites);
                DatabaseManager.saveLocalVars(localVars);
                DatabaseManager.saveSpringController(context.controllers);
                DatabaseManager.saveSpringInterceptor(context.interceptors);
                DatabaseManager.saveServlets(context.servlets);
                DatabaseManager.saveFilters(context.filters);
                DatabaseManager.saveListeners(context.listeners);
            });

            GRAPH_BUILD_SERVICE.replaceFromAnalysis(
                    buildSeq,
                    quickMode,
                    callGraphModeMeta,
                    context.discoveredMethods,
                    context.methodCalls,
                    context.methodCallMeta,
                    context.callSites
            );

            DatabaseManager.finalizeBuild();
            finalizePending = false;
            refreshCachesAfterBuild();
            logger.info("build stage neo4j-write/finalize: {} ms (dbSize={})",
                    msSince(dbWriteStartNs),
                    formatSizeInMB(getFileSize()));

            long edgeCount = countEdges(context.methodCalls);
            long fileSizeBytes = getFileSize();
            String fileSizeMB = formatSizeInMB(fileSizeBytes);
            progress.accept(100);

            ConfigFile config = new ConfigFile();
            config.setTempPath(Const.tempDir);
            config.setDbPath(resolveActiveStorePath().toString());
            config.setJarPath(jarPath == null ? "" : jarPath.toAbsolutePath().toString());
            config.setDbSize(fileSizeMB);
            config.setLang("en");
            config.setDecompileCacheSize(String.valueOf(CFRDecompileEngine.getCacheCapacity()));
            try {
                EngineContext.setEngine(new CoreEngine(config));
            } catch (Exception ex) {
                logger.warn("init legacy core engine skipped in neo4j-only mode: {}", ex.toString());
                EngineContext.setEngine(null);
            }

            BuildResult result = new BuildResult(
                    buildSeq,
                    userArchives.size(),
                    context.classFileList.size(),
                    context.discoveredClasses.size(),
                    context.discoveredMethods.size(),
                    edgeCount,
                    fileSizeBytes,
                    fileSizeMB,
                    quickMode,
                    callGraphEngine,
                    profile.value(),
                    scopeSummary.targetArchiveCount(),
                    scopeSummary.libraryArchiveCount(),
                    jdkResolution.sdkEntryCount(),
                    taieEdgeCount
            );

            clearBuildContext(context);
            cleaned = true;
            return result;
        } finally {
            DeferredFileWriter.awaitAndStop();
            CoreUtil.cleanupEmptyTempDirs();
            if (!cleaned) {
                clearBuildContext(context);
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

    private static void clearBuildContext(BuildContext ctx) {
        if (ctx == null) {
            BytecodeCache.clear();
            return;
        }
        clearCachedBytes(ctx.classFileList);
        ctx.classFileList.clear();
        ctx.discoveredClasses.clear();
        ctx.discoveredMethods.clear();
        ctx.classMap.clear();
        ctx.methodMap.clear();
        ctx.methodCalls.clear();
        ctx.methodCallMeta.clear();
        ctx.strMap.clear();
        ctx.resources.clear();
        BytecodeCache.clear();
        ctx.controllers.clear();
        ctx.interceptors.clear();
        ctx.servlets.clear();
        ctx.filters.clear();
        ctx.listeners.clear();
        ctx.stringAnnoMap.clear();
        ctx.callSites.clear();
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
        Path root = resolveActiveStorePath();
        if (root == null || !Files.exists(root)) {
            return 0L;
        }
        try (var walk = Files.walk(root)) {
            return walk
                    .filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (Exception ex) {
                            return 0L;
                        }
                    })
                    .sum();
        } catch (Exception ex) {
            logger.debug("calculate neo4j store size fail: {}", ex.toString());
            return 0L;
        }
    }

    private static Path resolveActiveStorePath() {
        String projectKey = ActiveProjectContext.getActiveProjectKey();
        return Neo4jProjectStore.getInstance().resolveProjectHome(projectKey);
    }

    private static String formatSizeInMB(long fileSizeBytes) {
        double fileSizeMB = (double) fileSizeBytes / (1024 * 1024);
        return String.format("%.2f MB", fileSizeMB);
    }

    private static String resolveMainClass(Path primaryInput,
                                           List<Path> appArchives,
                                           Set<MethodReference> discoveredMethods) {
        String fromManifest = resolveMainClassFromManifests(primaryInput, appArchives);
        if (!fromManifest.isBlank()) {
            logger.info("resolved Tai-e main class from manifest: {}", fromManifest);
            return fromManifest;
        }
        String fromMethods = selectMainClassFromDiscoveredMethods(discoveredMethods);
        if (!fromMethods.isBlank()) {
            logger.info("resolved Tai-e main class from discovered methods: {}", fromMethods);
            return fromMethods;
        }
        logger.debug("Tai-e main class unresolved; analysis may fallback to implicit entries only");
        return "";
    }

    private static String resolveMainClassFromManifests(Path primaryInput,
                                                        List<Path> appArchives) {
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();
        if (primaryInput != null && Files.isRegularFile(primaryInput)) {
            candidates.add(primaryInput.toAbsolutePath().normalize());
        }
        if (appArchives != null && !appArchives.isEmpty()) {
            for (Path archive : appArchives) {
                if (archive != null && Files.isRegularFile(archive)) {
                    candidates.add(archive.toAbsolutePath().normalize());
                }
            }
        }
        for (Path archive : candidates) {
            String mainClass = readMainClassFromManifest(archive);
            if (!mainClass.isBlank()) {
                return mainClass;
            }
        }
        return "";
    }

    private static String readMainClassFromManifest(Path archive) {
        if (archive == null || Files.notExists(archive)) {
            return "";
        }
        String fileName = archive.getFileName() == null ? "" : archive.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".jar") && !fileName.endsWith(".war")) {
            return "";
        }
        try (JarFile jarFile = new JarFile(archive.toFile())) {
            if (jarFile.getManifest() == null) {
                return "";
            }
            Attributes attrs = jarFile.getManifest().getMainAttributes();
            if (attrs == null) {
                return "";
            }
            String startClass = normalizeMainClassName(attrs.getValue("Start-Class"));
            if (!startClass.isBlank()) {
                return startClass;
            }
            String mainClass = normalizeMainClassName(attrs.getValue(Attributes.Name.MAIN_CLASS));
            if (mainClass.startsWith("org.springframework.boot.loader.")) {
                return "";
            }
            return mainClass;
        } catch (Exception ex) {
            logger.debug("read manifest main class failed for {}: {}", archive, ex.toString());
            return "";
        }
    }

    private static String selectMainClassFromDiscoveredMethods(Set<MethodReference> discoveredMethods) {
        if (discoveredMethods == null || discoveredMethods.isEmpty()) {
            return "";
        }
        String selected = "";
        for (MethodReference method : discoveredMethods) {
            if (method == null || method.getClassReference() == null) {
                continue;
            }
            if (!"main".equals(method.getName())) {
                continue;
            }
            if (!"([Ljava/lang/String;)V".equals(method.getDesc())) {
                continue;
            }
            if ((method.getAccess() & 0x0008) == 0) {
                continue;
            }
            if ((method.getAccess() & 0x0001) == 0) {
                continue;
            }
            String candidate = normalizeMainClassName(method.getClassReference().getName());
            if (candidate.isBlank()) {
                continue;
            }
            if (selected.isBlank() || candidate.compareTo(selected) < 0) {
                selected = candidate;
            }
        }
        return selected;
    }

    private static String normalizeMainClassName(String className) {
        if (className == null) {
            return "";
        }
        String normalized = className.trim();
        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.startsWith("L") && normalized.endsWith(";") && normalized.length() > 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        normalized = normalized.replace('/', '.').replace('\\', '.');
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - ".class".length());
        }
        while (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static List<Path> normalizePaths(List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return List.of();
        }
        List<Path> out = new ArrayList<>();
        for (String value : paths) {
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                out.add(Paths.get(value).toAbsolutePath().normalize());
            } catch (Exception ignored) {
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
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
        private final String callGraphEngine;
        private final String analysisProfile;
        private final int targetJarCount;
        private final int libraryJarCount;
        private final int sdkEntryCount;
        private final int taieEdgeCount;

        public BuildResult(long buildSeq,
                           int jarCount,
                           int classFileCount,
                           int classCount,
                           int methodCount,
                           long edgeCount,
                           long dbSizeBytes,
                           String dbSizeLabel,
                           boolean quickMode,
                           String callGraphEngine,
                           String analysisProfile,
                           int targetJarCount,
                           int libraryJarCount,
                           int sdkEntryCount,
                           int taieEdgeCount) {
            this.buildSeq = buildSeq;
            this.jarCount = jarCount;
            this.classFileCount = classFileCount;
            this.classCount = classCount;
            this.methodCount = methodCount;
            this.edgeCount = edgeCount;
            this.dbSizeBytes = dbSizeBytes;
            this.dbSizeLabel = dbSizeLabel;
            this.quickMode = quickMode;
            this.callGraphEngine = callGraphEngine;
            this.analysisProfile = analysisProfile;
            this.targetJarCount = Math.max(0, targetJarCount);
            this.libraryJarCount = Math.max(0, libraryJarCount);
            this.sdkEntryCount = Math.max(0, sdkEntryCount);
            this.taieEdgeCount = Math.max(0, taieEdgeCount);
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

        public String getCallGraphEngine() {
            return callGraphEngine;
        }

        public String getAnalysisProfile() {
            return analysisProfile;
        }

        public int getTargetJarCount() {
            return targetJarCount;
        }

        public int getLibraryJarCount() {
            return libraryJarCount;
        }

        public int getSdkEntryCount() {
            return sdkEntryCount;
        }

        public int getTaieEdgeCount() {
            return taieEdgeCount;
        }
    }
}
