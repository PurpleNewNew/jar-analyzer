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

import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.analyze.spring.SpringMapping;
import me.n1ar4.jar.analyzer.core.asm.FixClassVisitor;
import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.bytecode.BuildBytecodeWorkspace;
import me.n1ar4.jar.analyzer.core.facts.BytecodeFactRunner;
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
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectBuildMode;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.jar.analyzer.engine.index.IndexEngine;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;
import me.n1ar4.jar.analyzer.rules.MethodSemanticSupport;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jBulkImportService;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistryService;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.BytecodeCache;
import me.n1ar4.jar.analyzer.utils.ClasspathResolver;
import me.n1ar4.jar.analyzer.utils.CoreUtil;
import me.n1ar4.jar.analyzer.utils.DeferredFileWriter;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

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
    private static final Neo4jBulkImportService GRAPH_BUILD_SERVICE = new Neo4jBulkImportService();
    private static final IntConsumer NOOP_PROGRESS = p -> {
    };
    private static final ThreadLocal<String> BUILD_STAGE = ThreadLocal.withInitial(() -> "idle");
    private static final ThreadLocal<String> LAST_BUILD_STAGE = new ThreadLocal<>();

    private static final String ALL_COMMON_POLICY_PROP = "jar.analyzer.all-common.policy";
    private static final String ALL_COMMON_POLICY_CONTINUE = "continue-no-callgraph";
    private static final String CALL_GRAPH_ENGINE_PROP = "jar.analyzer.callgraph.engine";
    private static final String CALL_GRAPH_ENGINE_DISABLED = "disabled-no-target";
    private static final String CALLGRAPH_STAGE_KEY = "callgraph";
    private static final String BOOT_INF_CLASSES_PREFIX = "BOOT-INF/classes/";
    private static final String WEB_INF_CLASSES_PREFIX = "WEB-INF/classes/";
    private static final boolean DEFAULT_COLLECT_ENDPOINT_ALIAS_STATS = false;

    private static void refreshCachesAfterBuild() {
        try {
            IndexEngine.closeAll();
        } catch (Exception ex) {
            logger.warn("close index fail: {}", ex.toString());
        }
    }

    public static BuildResult run(Path jarPath,
                                  Path rtJarPath,
                                  boolean fixClass,
                                  boolean quickMode,
                                  IntConsumer progressConsumer) {
        return run(jarPath, rtJarPath, fixClass, quickMode, progressConsumer, false);
    }

    /**
     * Build database for the given bytecode input.
     *
     * @param includeNested if true, include nested jars when resolving classpath.
     */
    public static BuildResult run(Path jarPath,
                                  Path rtJarPath,
                                  boolean fixClass,
                                  boolean quickMode,
                                  IntConsumer progressConsumer,
                                  boolean includeNested) {
        IntConsumer progress = progressConsumer == null ? NOOP_PROGRESS : progressConsumer;
        BuildContext context = new BuildContext();
        BuildMetricsCollector metrics = new BuildMetricsCollector();
        long buildStartNs = System.nanoTime();
        boolean cleaned = false;
        boolean buildCommitted = false;
        BuildSession session = null;
        String targetProjectKey = "";
        long buildSeq = 0L;
        try {
            session = beginBuildSession();
            targetProjectKey = session.targetProjectKey();
            buildSeq = session.buildSeq();
            LAST_BUILD_STAGE.remove();
            markBuildStage("prepare-project-model");
            long stageStartNs = System.nanoTime();
            prepareProjectModelForBuild(jarPath, rtJarPath, includeNested);
            metrics.record("prepare_project_model", msSince(stageStartNs), metricMap(
                    "include_nested", includeNested,
                    "project_mode", ProjectRuntimeContext.getProjectModel() != null
                            && ProjectRuntimeContext.getProjectModel().buildMode() == ProjectBuildMode.PROJECT,
                    "runtime_hint_present", rtJarPath != null || ProjectRuntimeContext.runtimePath() != null
            ));
            markBuildStage("resolve-inputs");
            BuildInputs inputs = resolveBuildInputs(jarPath, rtJarPath, quickMode, includeNested, progress, metrics);
            markBuildStage("prepare-class-files");
            prepareClassFiles(context, inputs.userArchives(), inputs.jarIdMap(), fixClass, includeNested, progress, metrics);
            BytecodeFrontEndStageResult frontEndStage = runBytecodeStages(context, quickMode, progress, metrics);
            BytecodeSymbolRunner.Result symbolResult = frontEndStage.symbolResult();
            List<LocalVarEntity> localVars = symbolResult == null || symbolResult.getLocalVars() == null
                    ? Collections.emptyList()
                    : symbolResult.getLocalVars();
            CallGraphStageResult callGraphStage = runCallGraphStage(
                    jarPath,
                    quickMode,
                    context,
                    inputs.scopeSummary(),
                    inputs.runtimeHint(),
                    inputs.jarOriginsById(),
                    frontEndStage.workspace(),
                    localVars,
                    progress,
                    metrics
            );
            BuildResult result = commitBuild(
                    targetProjectKey,
                    buildSeq,
                    quickMode,
                    context,
                    inputs,
                    symbolResult,
                    callGraphStage,
                    progress,
                    metrics,
                    buildStartNs
            );
            buildCommitted = true;
            markBuildStage("cleanup");
            clearBuildContext(context);
            cleaned = true;
            return result;
        } finally {
            markBuildStage("cleanup", false);
            DeferredFileWriter.awaitAndStop();
            CoreUtil.cleanupEmptyTempDirs();
            if (!cleaned) {
                clearBuildContext(context);
            }
            if (session != null || DatabaseManager.isBuilding()) {
                DatabaseManager.finishBuild(buildCommitted);
            }
            BUILD_STAGE.remove();
            if (buildCommitted) {
                LAST_BUILD_STAGE.remove();
            }
        }
    }

    public static String currentBuildStage() {
        String stage = BUILD_STAGE.get();
        if (stage != null && !stage.isBlank() && !"idle".equalsIgnoreCase(stage)) {
            return stage;
        }
        String lastStage = LAST_BUILD_STAGE.get();
        if (lastStage != null && !lastStage.isBlank()) {
            return lastStage;
        }
        return "idle";
    }

    private static BuildSession beginBuildSession() {
        synchronized (ActiveProjectContext.mutationLock()) {
            String targetProjectKey = ActiveProjectContext.getActiveProjectKey();
            try {
                long buildSeq = DatabaseManager.beginBuild(targetProjectKey);
                return new BuildSession(targetProjectKey, buildSeq);
            } catch (Throwable ex) {
                if (DatabaseManager.isBuilding(targetProjectKey)) {
                    DatabaseManager.finishBuild(false);
                }
                throw ex;
            }
        }
    }

    private static void prepareProjectModelForBuild(Path jarPath,
                                                    Path rtJarPath,
                                                    boolean includeNested) {
        try {
            ProjectModel currentModel = ProjectRuntimeContext.getProjectModel();
            boolean explicitProjectMode = currentModel != null
                    && currentModel.buildMode() == ProjectBuildMode.PROJECT;
            Path runtimeForModel = rtJarPath;
            if (!explicitProjectMode && runtimeForModel == null) {
                runtimeForModel = ProjectRuntimeContext.runtimePath();
            }
            ProjectRuntimeContext.prepareArtifactBuild(
                    explicitProjectMode ? null : jarPath,
                    runtimeForModel,
                    null,
                    includeNested
            );
        } catch (Exception ex) {
            logger.warn("prepare artifact project model fail, continue with possible degraded archive classification: {}",
                    ex.toString());
        }
    }

    private static BuildInputs resolveBuildInputs(Path jarPath,
                                                  Path rtJarPath,
                                                  boolean quickMode,
                                                  boolean includeNested,
                                                  IntConsumer progress,
                                                  BuildMetricsCollector metrics) {
        long stageStartNs = System.nanoTime();
        Map<String, Integer> jarIdMap = new LinkedHashMap<>();
        int[] nextJarId = {1};

        progress.accept(10);
        List<String> userArchives = ClasspathResolver.resolveInputArchives(
                jarPath, null, !quickMode, includeNested);
        List<Path> normalizedArchives = normalizePaths(userArchives);
        try {
            ProjectRuntimeContext.prepareArtifactBuild(
                    null,
                    null,
                    normalizedArchives,
                    includeNested
            );
        } catch (Exception ex) {
            logger.debug("update analyzed archives fail: {}", ex.toString());
        }

        Path runtimeHint = rtJarPath == null ? ProjectRuntimeContext.runtimePath() : rtJarPath;
        ScopeSummary scopeSummary = ArchiveScopeClassifier.classifyArchives(
                normalizedArchives, jarPath, runtimeHint);
        Map<Path, ProjectOrigin> archiveOrigins = applyProjectModePrimaryInputOriginOverride(
                scopeSummary.originsByArchive(),
                normalizedArchives
        );
        scopeSummary = rebuildScopeSummary(archiveOrigins);
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
        logger.info("build stage resolve-inputs: {} ms (archives={}, targetArchives={}, libraryArchives={}, sdkArchives={}, heap={})",
                msSince(stageStartNs),
                userArchives.size(),
                scopeSummary.targetArchiveCount(),
                scopeSummary.libraryArchiveCount(),
                scopeSummary.sdkArchiveCount(),
                heapUsage());
        if (metrics != null) {
            metrics.record("resolve_inputs", msSince(stageStartNs), metricMap(
                    "archives", userArchives.size(),
                    "target_archives", scopeSummary.targetArchiveCount(),
                    "library_archives", scopeSummary.libraryArchiveCount(),
                    "sdk_archives", scopeSummary.sdkArchiveCount(),
                    "quick_mode", quickMode,
                    "include_nested", includeNested
            ));
        }
        return new BuildInputs(userArchives, jarIdMap, jarOriginsById, scopeSummary, runtimeHint);
    }

    private static Map<Path, ProjectOrigin> applyProjectModePrimaryInputOriginOverride(Map<Path, ProjectOrigin> originsByArchive,
                                                                                       List<Path> normalizedArchives) {
        Map<Path, ProjectOrigin> current = originsByArchive == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(originsByArchive);
        ProjectModel projectModel = ProjectRuntimeContext.getProjectModel();
        if (projectModel == null || projectModel.buildMode() != ProjectBuildMode.PROJECT) {
            return current;
        }
        Path primaryInput = normalizePath(projectModel.primaryInputPath());
        if (primaryInput == null || normalizedArchives == null || normalizedArchives.isEmpty()) {
            return current;
        }
        for (Path archive : normalizedArchives) {
            Path normalized = normalizePath(archive);
            if (normalized == null || !normalized.equals(primaryInput)) {
                continue;
            }
            current.put(normalized, ProjectOrigin.APP);
        }
        return current;
    }

    private static ScopeSummary rebuildScopeSummary(Map<Path, ProjectOrigin> originsByArchive) {
        if (originsByArchive == null || originsByArchive.isEmpty()) {
            return new ScopeSummary(Map.of(), 0, 0, 0);
        }
        int target = 0;
        int library = 0;
        int sdk = 0;
        for (ProjectOrigin origin : originsByArchive.values()) {
            if (origin == ProjectOrigin.SDK) {
                sdk++;
            } else if (origin == ProjectOrigin.LIBRARY) {
                library++;
            } else {
                target++;
            }
        }
        return new ScopeSummary(originsByArchive, target, library, sdk);
    }

    private static Path normalizePath(Path path) {
        if (path == null) {
            return null;
        }
        try {
            return path.toAbsolutePath().normalize();
        } catch (Exception ex) {
            return path.normalize();
        }
    }

    private static void prepareClassFiles(BuildContext context,
                                          List<String> userArchives,
                                          Map<String, Integer> jarIdMap,
                                          boolean fixClass,
                                          boolean includeNested,
                                          IntConsumer progress,
                                          BuildMetricsCollector metrics) {
        long stageStartNs = System.nanoTime();
        List<ClassFileEntity> classFiles = CoreUtil.getAllClassesFromJars(
                userArchives, jarIdMap, context.resources, includeNested);
        List<ClassFileEntity> jspClassFiles = JspCompileRunner.compile(
                ProjectRuntimeContext.getProjectModel(),
                context.resources,
                userArchives
        );
        if (!jspClassFiles.isEmpty()) {
            classFiles.addAll(jspClassFiles);
        }
        for (ClassFileEntity cf : classFiles) {
            String className = cf.getClassName();
            if (!fixClass) {
                cf.setClassName(normalizeDiscoveredClassName(className));
                continue;
            }
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
        progress.accept(15);
        context.classFileList.addAll(classFiles);
        progress.accept(20);
        logger.info("build stage prepare-class-files: {} ms (classFiles={}, resources={}, fixClass={}, heap={})",
                msSince(stageStartNs),
                classFiles.size(),
                context.resources.size(),
                fixClass,
                heapUsage());
        if (metrics != null) {
            metrics.record("prepare_class_files", msSince(stageStartNs), metricMap(
                    "class_files", classFiles.size(),
                    "resources", context.resources.size(),
                    "jsp_class_files", jspClassFiles.size(),
                    "fix_class", fixClass,
                    "include_nested", includeNested
            ));
        }
    }

    private static BytecodeFrontEndStageResult runBytecodeStages(BuildContext context,
                                                                 boolean quickMode,
                                                                 IntConsumer progress,
                                                                 BuildMetricsCollector metrics) {
        markBuildStage("discovery");
        long stageStartNs = System.nanoTime();
        BuildBytecodeWorkspace workspace = BuildBytecodeWorkspace.parse(context.classFileList);
        DiscoveryRunner.start(
                workspace,
                context.discoveredClasses,
                context.discoveredMethods,
                context.classMap,
                context.methodMap,
                context.stringAnnoMap
        );
        logger.info("build stage discovery: {} ms (classFiles={}, classes={}, methods={}, heap={})",
                msSince(stageStartNs),
                context.classFileList.size(),
                context.discoveredClasses.size(),
                context.discoveredMethods.size(),
                heapUsage());
        if (metrics != null) {
            metrics.record("discovery", msSince(stageStartNs), metricMap(
                    "class_files", context.classFileList.size(),
                    "classes", context.discoveredClasses.size(),
                    "methods", context.discoveredMethods.size()
            ));
        }
        progress.accept(30);

        markBuildStage("class-analysis");
        stageStartNs = System.nanoTime();
        progress.accept(35);
        ClassAnalysisRunner.start(
                workspace,
                context.methodMap,
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
        logger.info("build stage class-analysis: {} ms (strings={}, controllers={}, servlets={}, filters={}, listeners={}, heap={})",
                msSince(stageStartNs),
                context.strMap.size(),
                context.controllers.size(),
                context.servlets.size(),
                context.filters.size(),
                context.listeners.size(),
                heapUsage());
        if (metrics != null) {
            metrics.record("class_analysis", msSince(stageStartNs), metricMap(
                    "strings", context.strMap.size(),
                    "controllers", context.controllers.size(),
                    "interceptors", context.interceptors.size(),
                    "servlets", context.servlets.size(),
                    "filters", context.filters.size(),
                    "listeners", context.listeners.size()
            ));
        }

        markBuildStage("framework-entry");
        stageStartNs = System.nanoTime();
        FrameworkEntryDiscovery.Result frameworkEntries = FrameworkEntryDiscovery.discover(
                context.resources,
                context.classMap,
                context.methodMap
        );
        mergeUnique(context.servlets, frameworkEntries.servlets());
        mergeUnique(context.filters, frameworkEntries.filters());
        mergeUnique(context.listeners, frameworkEntries.listeners());
        if (frameworkEntries.explicitSourceMethodFlags() != null
                && !frameworkEntries.explicitSourceMethodFlags().isEmpty()) {
            context.explicitSourceMethodFlags.putAll(frameworkEntries.explicitSourceMethodFlags());
        }
        logger.info("build stage framework-entry: {} ms (explicitMethods={}, servlets={}, filters={}, listeners={}, heap={})",
                msSince(stageStartNs),
                context.explicitSourceMethodFlags.size(),
                context.servlets.size(),
                context.filters.size(),
                context.listeners.size(),
                heapUsage());
        if (metrics != null) {
            metrics.record("framework_entry", msSince(stageStartNs), metricMap(
                    "explicit_methods", context.explicitSourceMethodFlags.size(),
                    "servlets", context.servlets.size(),
                    "filters", context.filters.size(),
                    "listeners", context.listeners.size()
            ));
        }

        markBuildStage("method-semantic");
        stageStartNs = System.nanoTime();
        applyMethodSemanticFlags(context);
        logger.info("build stage method-semantic: {} ms (taggedMethods={}, heap={})",
                msSince(stageStartNs),
                countSemanticMethods(context),
                heapUsage());
        if (metrics != null) {
            metrics.record("method_semantic", msSince(stageStartNs), metricMap(
                    "tagged_methods", countSemanticMethods(context)
            ));
        }

        markBuildStage("bytecode-symbol");
        stageStartNs = System.nanoTime();
        progress.accept(40);
        if (quickMode) {
            logger.info("build stage bytecode-symbol: skipped in quick mode (heap={})", heapUsage());
            if (metrics != null) {
                metrics.record("bytecode_symbol", msSince(stageStartNs), metricMap(
                        "call_sites", 0,
                        "local_vars", 0,
                        "skipped", true
                ));
            }
            return new BytecodeFrontEndStageResult(workspace, null);
        }
        BytecodeSymbolRunner.Result symbolResult = BytecodeSymbolRunner.start(workspace);
        List<CallSiteEntity> callSites = symbolResult.getCallSites();
        context.callSites.clear();
        if (callSites != null && !callSites.isEmpty()) {
            context.callSites.addAll(callSites);
        }
        logger.info("build stage symbol: {} ms (callSites={}, localVars={}, heap={})",
                msSince(stageStartNs),
                callSites == null ? 0 : callSites.size(),
                symbolResult.getLocalVars() == null ? 0 : symbolResult.getLocalVars().size(),
                heapUsage());
        if (metrics != null) {
            metrics.record("bytecode_symbol", msSince(stageStartNs), metricMap(
                    "call_sites", callSites == null ? 0 : callSites.size(),
                    "local_vars", symbolResult.getLocalVars() == null ? 0 : symbolResult.getLocalVars().size(),
                    "skipped", false
            ));
        }
        return new BytecodeFrontEndStageResult(workspace, symbolResult);
    }

    private static CallGraphStageResult runCallGraphStage(Path jarPath,
                                                          boolean quickMode,
                                                          BuildContext context,
                                                          ScopeSummary scopeSummary,
                                                          Path runtimeHint,
                                                          Map<Integer, ProjectOrigin> jarOriginsById,
                                                          BuildBytecodeWorkspace workspace,
                                                          List<LocalVarEntity> localVars,
                                                          IntConsumer progress,
                                                          BuildMetricsCollector metrics) {
        markBuildStage("callgraph");
        progress.accept(55);
        long stageStartNs = System.nanoTime();
        JdkResolution jdkResolution = JdkArchiveResolver.resolve(runtimeHint);
        AnalysisProfile taieProfile = TaieAnalysisRunner.resolveProfile();
        CallGraphPlan callGraphPlan = CallGraphPlan.resolve(
                System.getProperty(CALL_GRAPH_ENGINE_PROP),
                System.getProperty(CallGraphPlan.CALL_GRAPH_PROFILE_PROP),
                taieProfile
        );
        TaieEdgeMapper.EdgePolicy edgePolicy = TaieEdgeMapper.resolveEdgePolicy();
        List<Path> appArchives = ArchiveScopeClassifier.pickAppArchives(scopeSummary);
        List<Path> libraryArchives = ArchiveScopeClassifier.pickLibraryArchives(scopeSummary);

        LinkedHashSet<Path> taieClasspath = new LinkedHashSet<>();
        taieClasspath.addAll(appArchives);
        taieClasspath.addAll(libraryArchives);
        taieClasspath.addAll(jdkResolution.archives());

        String callGraphEngine = callGraphPlan.callGraphEngine();
        String callGraphModeMeta = callGraphPlan.callGraphModeMeta();
        String analysisProfile = callGraphPlan.analysisProfile();
        int oracleEdgeCount = 0;
        int oracleEntryMethodCount = 0;
        int oracleReachableMethodCount = 0;
        int oraclePointsToVarCount = 0;
        int oraclePointsToObjectCount = 0;
        int oracleEndpointThisVarCount = 0;
        long oracleEndpointAliasPairs = 0L;
        String oracleReflectionInference = "";
        String oracleReflectionLog = "";
        BytecodeMainlineCallGraphRunner.Result bytecodeResult = BytecodeMainlineCallGraphRunner.Result.empty();
        List<String> explicitEntryMethods = collectExplicitEntryMethods(context, jarOriginsById);
        context.methodCalls.clear();
        context.methodCallMeta.clear();

        if (scopeSummary.targetArchiveCount() <= 0) {
            String policy = System.getProperty(ALL_COMMON_POLICY_PROP, ALL_COMMON_POLICY_CONTINUE);
            if (policy != null && !policy.isBlank()
                    && "fail".equals(policy.trim().toLowerCase(Locale.ROOT))) {
                throw new IllegalStateException("no target archives found and all-common policy forbids continue");
            }
            callGraphEngine = CALL_GRAPH_ENGINE_DISABLED;
            callGraphModeMeta = CALL_GRAPH_ENGINE_DISABLED;
            logger.info("all archives are common/sdk, continue without call graph (policy={})",
                    policy == null || policy.isBlank() ? ALL_COMMON_POLICY_CONTINUE : policy);
        } else {
            if (callGraphPlan.bytecodeMainline()) {
                BytecodeFactRunner.Result facts = BytecodeFactRunner.collect(
                        context,
                        scopeSummary,
                        jarOriginsById,
                        localVars,
                        workspace
                );
                bytecodeResult = BytecodeMainlineCallGraphRunner.run(
                        facts.snapshot(),
                        facts.edges(),
                        callGraphPlan.bytecodeSettings()
                );
                facts.edges().copyInto(context);
                logger.info("build stage bytecode-mainline: {} ms (mode={}, directEdges={}, declaredDispatchEdges={}, invokeDynamicEdges={}, typedDispatchEdges={}, dispatchExpansionEdges={}, reflectionEdges={}, methodHandleEdges={}, callbackEdges={}, frameworkEdges={}, triggerBridgeEdges={}, ptaEdges={}, ptaRefinedCallSites={}, ptaHotspotCallSites={}, ptaFieldSites={}, ptaArraySites={}, ptaArrayCopySites={}, instantiatedClasses={}, unresolvedCallers={}, unresolvedDeclaredTargets={}, totalEdges={}, heap={})",
                        msSince(stageStartNs),
                        callGraphModeMeta,
                        bytecodeResult.directEdges(),
                        bytecodeResult.declaredDispatchEdges(),
                        bytecodeResult.invokeDynamicEdges(),
                        bytecodeResult.typedDispatchEdges(),
                        bytecodeResult.dispatchExpansionEdges(),
                        bytecodeResult.reflectionEdges(),
                        bytecodeResult.methodHandleEdges(),
                        bytecodeResult.callbackEdges(),
                        bytecodeResult.frameworkEdges(),
                        bytecodeResult.triggerBridgeEdges(),
                        bytecodeResult.ptaEdges(),
                        bytecodeResult.refinedPtaCallSites(),
                        bytecodeResult.ptaHotspotCallSites(),
                        bytecodeResult.ptaFieldSites(),
                        bytecodeResult.ptaArraySites(),
                        bytecodeResult.ptaArrayCopySites(),
                        bytecodeResult.instantiatedClassCount(),
                        bytecodeResult.unresolvedCallerCount(),
                        bytecodeResult.unresolvedDeclaredTargetCount(),
                        bytecodeResult.totalEdges(),
                        heapUsage());
            } else {
                AnalysisProfile resolvedTaieProfile = callGraphPlan.taieProfile() == null
                        ? taieProfile
                        : callGraphPlan.taieProfile();
                String mainClass = resolveMainClass(jarPath, appArchives, context.discoveredMethods);
                TaieRunResult taieResult = TaieAnalysisRunner.run(
                        appArchives,
                        new ArrayList<>(taieClasspath),
                        resolvedTaieProfile,
                        mainClass,
                        explicitEntryMethods,
                        DEFAULT_COLLECT_ENDPOINT_ALIAS_STATS
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
                oracleEdgeCount = mapped.keptEdges();
                oracleEntryMethodCount = taieResult.entryMethodCount();
                oracleReachableMethodCount = taieResult.reachableMethodCount();
                oraclePointsToVarCount = taieResult.pointsToVarCount();
                oraclePointsToObjectCount = taieResult.pointsToObjectCount();
                oracleEndpointThisVarCount = taieResult.endpointThisVarCount();
                oracleEndpointAliasPairs = taieResult.endpointMayAliasPairs();
                oracleReflectionInference = safe(taieResult.reflectionInference());
                oracleReflectionLog = safe(taieResult.reflectionLog());
                logger.info("build stage oracle-taie: {} ms (profile={}, edgePolicy={}, totalEdges={}, keptEdges={}, skippedByPolicy={}, unresolvedCaller={}, unresolvedCallee={}, explicitEntries={}, entryMethods={}, reachableMethods={}, pointsToVars={}, pointsToObjects={}, endpointThisVars={}, endpointAliasPairs={}, reflection={}, reflectionLog={}, heap={})",
                        taieResult.elapsedMs(),
                        resolvedTaieProfile.value(),
                        mapped.edgePolicy(),
                        mapped.totalEdges(),
                        mapped.keptEdges(),
                        mapped.skippedByPolicy(),
                        mapped.unresolvedCaller(),
                        mapped.unresolvedCallee(),
                        taieResult.explicitEntryCount(),
                        oracleEntryMethodCount,
                        oracleReachableMethodCount,
                        oraclePointsToVarCount,
                        oraclePointsToObjectCount,
                        oracleEndpointThisVarCount,
                        oracleEndpointAliasPairs,
                        oracleReflectionInference.isBlank() ? "<default>" : oracleReflectionInference,
                        oracleReflectionLog.isBlank() ? "<none>" : oracleReflectionLog,
                        heapUsage());
            }
        }
        logger.info("build stage callgraph total: {} ms (engine={}, heap={})",
                msSince(stageStartNs), callGraphEngine, heapUsage());
        if (metrics != null) {
            Map<String, Object> callGraphMetrics = new LinkedHashMap<>(metricMap(
                    "engine", callGraphEngine,
                    "analysis_profile", safe(analysisProfile),
                    "quick_mode", quickMode,
                    "app_archives", appArchives.size(),
                    "classpath_archives", taieClasspath.size(),
                    "explicit_entries", explicitEntryMethods.size(),
                    "target_archives", scopeSummary == null ? 0 : scopeSummary.targetArchiveCount(),
                    "library_archives", scopeSummary == null ? 0 : scopeSummary.libraryArchiveCount(),
                    "oracle_edges", oracleEdgeCount,
                    "oracle_entry_methods", oracleEntryMethodCount,
                    "entry_methods", oracleEntryMethodCount,
                    "oracle_reachable_methods", oracleReachableMethodCount,
                    "reachable_methods", oracleReachableMethodCount,
                    "oracle_points_to_vars", oraclePointsToVarCount,
                    "points_to_vars", oraclePointsToVarCount,
                    "oracle_points_to_objects", oraclePointsToObjectCount,
                    "points_to_objects", oraclePointsToObjectCount,
                    "oracle_endpoint_this_vars", oracleEndpointThisVarCount,
                    "endpoint_this_vars", oracleEndpointThisVarCount,
                    "oracle_endpoint_alias_pairs", oracleEndpointAliasPairs,
                    "endpoint_alias_pairs", oracleEndpointAliasPairs
            ));
            if (callGraphPlan.bytecodeMainline()) {
                callGraphMetrics.put("direct_edges", bytecodeResult.directEdges());
                callGraphMetrics.put("declared_dispatch_edges", bytecodeResult.declaredDispatchEdges());
                callGraphMetrics.put("invoke_dynamic_edges", bytecodeResult.invokeDynamicEdges());
                callGraphMetrics.put("typed_dispatch_edges", bytecodeResult.typedDispatchEdges());
                callGraphMetrics.put("dispatch_expansion_edges", bytecodeResult.dispatchExpansionEdges());
                callGraphMetrics.put("reflection_edges", bytecodeResult.reflectionEdges());
                callGraphMetrics.put("method_handle_edges", bytecodeResult.methodHandleEdges());
                callGraphMetrics.put("callback_edges", bytecodeResult.callbackEdges());
                callGraphMetrics.put("framework_edges", bytecodeResult.frameworkEdges());
                callGraphMetrics.put("trigger_bridge_edges", bytecodeResult.triggerBridgeEdges());
                callGraphMetrics.put("thread_start_edges", bytecodeResult.threadStartEdges());
                callGraphMetrics.put("executor_edges", bytecodeResult.executorEdges());
                callGraphMetrics.put("completable_future_edges", bytecodeResult.completableFutureEdges());
                callGraphMetrics.put("do_privileged_edges", bytecodeResult.doPrivilegedEdges());
                callGraphMetrics.put("dynamic_proxy_edges", bytecodeResult.dynamicProxyEdges());
                callGraphMetrics.put("pta_edges", bytecodeResult.ptaEdges());
                callGraphMetrics.put("pta_refined_call_sites", bytecodeResult.refinedPtaCallSites());
                callGraphMetrics.put("pta_hotspot_call_sites", bytecodeResult.ptaHotspotCallSites());
                callGraphMetrics.put("pta_field_sites", bytecodeResult.ptaFieldSites());
                callGraphMetrics.put("pta_array_sites", bytecodeResult.ptaArraySites());
                callGraphMetrics.put("pta_array_copy_sites", bytecodeResult.ptaArrayCopySites());
                callGraphMetrics.put("instantiated_classes", bytecodeResult.instantiatedClassCount());
                callGraphMetrics.put("unresolved_callers", bytecodeResult.unresolvedCallerCount());
                callGraphMetrics.put("unresolved_declared_targets", bytecodeResult.unresolvedDeclaredTargetCount());
                callGraphMetrics.put("total_edges", bytecodeResult.totalEdges());
            }
            metrics.record(CALLGRAPH_STAGE_KEY, msSince(stageStartNs), callGraphMetrics);
        }
        return new CallGraphStageResult(
                scopeSummary,
                jdkResolution,
                analysisProfile,
                callGraphEngine,
                callGraphModeMeta,
                oracleEdgeCount,
                oracleEntryMethodCount,
                oracleReachableMethodCount,
                oraclePointsToVarCount,
                oraclePointsToObjectCount,
                oracleEndpointThisVarCount,
                oracleEndpointAliasPairs,
                oracleReflectionInference,
                oracleReflectionLog,
                explicitEntryMethods.size()
        );
    }

    private static BuildResult commitBuild(String targetProjectKey,
                                           long buildSeq,
                                           boolean quickMode,
                                           BuildContext context,
                                           BuildInputs inputs,
                                           BytecodeSymbolRunner.Result symbolResult,
                                           CallGraphStageResult callGraphStage,
                                           IntConsumer progress,
                                           BuildMetricsCollector metrics,
                                           long buildStartNs) {
        clearCachedBytes(context.classFileList);
        progress.accept(70);
        progress.accept(90);

        DeferredFileWriter.awaitAndStop();
        CoreUtil.cleanupEmptyTempDirs();
        List<LocalVarEntity> localVars = symbolResult == null ? Collections.emptyList() : symbolResult.getLocalVars();
        int localVarCount = localVars == null ? 0 : localVars.size();
        int jarCount = inputs.userArchives().size();
        int classFileCount = context.classFileList.size();
        int classCount = context.discoveredClasses.size();
        int methodCount = context.discoveredMethods.size();
        int callSiteCount = context.callSites.size();
        long edgeCount = countEdges(context.methodCalls);
        ProjectModel projectModel = ProjectRuntimeContext.getProjectModel();
        List<String> runtimeSnapshotJarPaths = new ArrayList<>(inputs.jarIdMap().keySet());

        markBuildStage("neo4j-import");
        long commitStartNs = System.nanoTime();
        ProjectRuntimeSnapshot runtimeSnapshot = GRAPH_BUILD_SERVICE.replaceFromAnalysis(
                targetProjectKey,
                buildSeq,
                quickMode,
                callGraphStage.callGraphModeMeta(),
                context.discoveredMethods,
                context.methodCalls,
                context.methodCallMeta,
                new Neo4jBulkImportService.GraphPayloadData(
                        context.callSites,
                        new ArrayList<>(context.discoveredClasses),
                        context.controllers,
                        context.interceptors,
                        context.servlets,
                        context.filters,
                        context.listeners,
                        context.explicitSourceMethodFlags
                ),
                () -> {
                    markBuildStage("build-runtime-snapshot");
                    long snapshotStartNs = System.nanoTime();
                    ProjectRuntimeSnapshot snapshot = DatabaseManager.buildProjectRuntimeSnapshot(
                            buildSeq,
                            projectModel,
                            runtimeSnapshotJarPaths,
                            context.classFileList,
                            context.discoveredClasses,
                            context.discoveredMethods,
                            context.strMap,
                            context.stringAnnoMap,
                            context.resources,
                            context.callSites,
                            localVars,
                            context.controllers,
                            context.interceptors,
                            context.servlets,
                            context.filters,
                            context.listeners
                    );
                    logger.info("build stage build-runtime-snapshot: {} ms (classFiles={}, methods={}, callSites={}, localVars={}, heap={})",
                            msSince(snapshotStartNs),
                            classFileCount,
                            methodCount,
                            callSiteCount,
                            localVarCount,
                            heapUsage());
                    if (metrics != null) {
                        metrics.record("build_runtime_snapshot", msSince(snapshotStartNs), metricMap(
                                "class_files", classFileCount,
                                "methods", methodCount,
                                "call_sites", callSiteCount,
                                "local_vars", localVarCount
                        ));
                    }
                    releaseSnapshotBackedBuildData(context, localVars);
                    return snapshot;
                },
                buildMeta(
                        callGraphStage.callGraphEngine(),
                        callGraphStage.analysisProfile(),
                        callGraphStage.scopeSummary(),
                        callGraphStage.jdkResolution(),
                        callGraphStage.oracleEdgeCount(),
                        callGraphStage.oracleEntryMethodCount(),
                        callGraphStage.oracleReachableMethodCount(),
                        callGraphStage.oraclePointsToVarCount(),
                        callGraphStage.oraclePointsToObjectCount(),
                        callGraphStage.oracleEndpointThisVarCount(),
                        callGraphStage.oracleEndpointAliasPairs(),
                        callGraphStage.oracleReflectionInference(),
                        callGraphStage.oracleReflectionLog(),
                        callGraphStage.explicitEntryCount()
                )
        );
        markBuildStage("publish-runtime");
        long publishStartNs = System.nanoTime();
        releaseCommittedBuildData(context, localVars);
        ProjectRegistryService.getInstance().publishBuiltActiveProjectRuntime(
                targetProjectKey,
                runtimeSnapshot,
                projectModel
        );
        if (metrics != null) {
            metrics.record("publish_runtime", msSince(publishStartNs), metricMap(
                    "project_key_present", targetProjectKey != null && !targetProjectKey.isBlank(),
                    "build_seq", buildSeq
            ));
        }
        markBuildStage("refresh-caches");
        long refreshStartNs = System.nanoTime();
        refreshCachesAfterBuild();
        if (metrics != null) {
            metrics.record("refresh_caches", msSince(refreshStartNs), metricMap(
                    "cache_refresh", true
            ));
        }
        logger.info("build stage neo4j+metadata commit: {} ms (dbSize={}, heap={})",
                msSince(commitStartNs),
                formatSizeInMB(getFileSize(targetProjectKey)),
                heapUsage());
        if (metrics != null) {
            metrics.record("neo4j_commit", msSince(commitStartNs), metricMap(
                    "class_files", classFileCount,
                    "methods", methodCount,
                    "call_sites", callSiteCount,
                    "local_vars", localVarCount,
                    "edges", edgeCount
            ));
        }

        long fileSizeBytes = getFileSize(targetProjectKey);
        String fileSizeMB = formatSizeInMB(fileSizeBytes);
        long buildWallMs = msSince(buildStartNs);
        Map<String, BuildStageMetric> stageMetrics = metrics == null
                ? Collections.emptyMap()
                : metrics.snapshot();
        long peakHeapUsedBytes = metrics == null ? 0L : metrics.peakHeapUsedBytes();
        long peakHeapCommittedBytes = metrics == null ? 0L : metrics.peakHeapCommittedBytes();
        long heapMaxBytes = metrics == null ? 0L : metrics.heapMaxBytes();
        try {
            GRAPH_BUILD_SERVICE.updateBuildMeta(
                    targetProjectKey,
                    buildSeq,
                    flattenBuildMetrics(stageMetrics, buildWallMs, peakHeapUsedBytes, peakHeapCommittedBytes, heapMaxBytes)
            );
        } catch (Exception ex) {
            logger.warn("update structured build meta fail: {}", ex.toString());
        }
        progress.accept(100);

        return new BuildResult(
                buildSeq,
                jarCount,
                classFileCount,
                classCount,
                methodCount,
                callSiteCount,
                edgeCount,
                fileSizeBytes,
                fileSizeMB,
                quickMode,
                callGraphStage.callGraphEngine(),
                callGraphStage.callGraphModeMeta(),
                callGraphStage.analysisProfile(),
                callGraphStage.scopeSummary().targetArchiveCount(),
                callGraphStage.scopeSummary().libraryArchiveCount(),
                callGraphStage.jdkResolution().sdkEntryCount(),
                callGraphStage.oracleEdgeCount(),
                callGraphStage.oracleEntryMethodCount(),
                callGraphStage.oracleReachableMethodCount(),
                callGraphStage.oraclePointsToVarCount(),
                callGraphStage.oraclePointsToObjectCount(),
                callGraphStage.oracleEndpointThisVarCount(),
                callGraphStage.oracleEndpointAliasPairs(),
                peakHeapUsedBytes,
                peakHeapCommittedBytes,
                heapMaxBytes,
                buildWallMs,
                stageMetrics
        );
    }

    private record BuildSession(String targetProjectKey, long buildSeq) {
    }

    private record BuildInputs(List<String> userArchives,
                               Map<String, Integer> jarIdMap,
                               Map<Integer, ProjectOrigin> jarOriginsById,
                               ScopeSummary scopeSummary,
                               Path runtimeHint) {
    }

    private record BytecodeFrontEndStageResult(BuildBytecodeWorkspace workspace,
                                               BytecodeSymbolRunner.Result symbolResult) {
        private BytecodeFrontEndStageResult {
            workspace = workspace == null ? BuildBytecodeWorkspace.empty() : workspace;
        }
    }

    private record CallGraphStageResult(ScopeSummary scopeSummary,
                                        JdkResolution jdkResolution,
                                        String analysisProfile,
                                        String callGraphEngine,
                                        String callGraphModeMeta,
                                        int oracleEdgeCount,
                                        int oracleEntryMethodCount,
                                        int oracleReachableMethodCount,
                                        int oraclePointsToVarCount,
                                        int oraclePointsToObjectCount,
                                        int oracleEndpointThisVarCount,
                                        long oracleEndpointAliasPairs,
                                        String oracleReflectionInference,
                                        String oracleReflectionLog,
                                        int explicitEntryCount) {
    }

    private record HeapStats(long usedBytes, long committedBytes, long maxBytes) {
    }

    private static long msSince(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }

    private static void markBuildStage(String stage) {
        markBuildStage(stage, true);
    }

    private static void markBuildStage(String stage, boolean updateLastStage) {
        String normalized = safe(stage);
        if (normalized.isBlank()) {
            normalized = "unknown";
        }
        BUILD_STAGE.set(normalized);
        if (updateLastStage) {
            LAST_BUILD_STAGE.set(normalized);
        }
        logger.info("build stage enter: {} (heap={})", normalized, heapUsage());
    }

    private static void releaseCommittedBuildData(BuildContext context, List<LocalVarEntity> localVars) {
        clearBuildContext(context);
        if (localVars != null && !localVars.isEmpty()) {
            try {
                localVars.clear();
            } catch (UnsupportedOperationException ex) {
                logger.debug("release local vars after commit skipped: {}", ex.toString());
            }
        }
    }

    private static void releaseSnapshotBackedBuildData(BuildContext context, List<LocalVarEntity> localVars) {
        if (context != null) {
            clearCachedBytes(context.classFileList);
            context.classFileList.clear();
            context.discoveredClasses.clear();
            context.classMap.clear();
            context.methodMap.clear();
            context.strMap.clear();
            context.resources.clear();
            context.controllers.clear();
            context.interceptors.clear();
            context.servlets.clear();
            context.filters.clear();
            context.listeners.clear();
            context.stringAnnoMap.clear();
            context.callSites.clear();
        } else {
            BytecodeCache.clear();
        }
        if (localVars != null && !localVars.isEmpty()) {
            try {
                localVars.clear();
            } catch (UnsupportedOperationException ex) {
                logger.debug("release local vars before neo4j commit skipped: {}", ex.toString());
            }
        }
        BytecodeCache.clear();
        logger.info("build stage build-runtime-snapshot release: heap={}", heapUsage());
    }

    private static String heapUsage() {
        HeapStats heap = captureHeapStats();
        return "used=" + formatMemory(heap.usedBytes())
                + ", committed=" + formatMemory(heap.committedBytes())
                + ", max=" + formatMemory(heap.maxBytes());
    }

    private static HeapStats captureHeapStats() {
        Runtime runtime = Runtime.getRuntime();
        long max = Math.max(0L, runtime.maxMemory());
        long committed = Math.max(0L, runtime.totalMemory());
        long used = Math.max(0L, committed - runtime.freeMemory());
        return new HeapStats(used, committed, max);
    }

    private static long toMiB(long bytes) {
        if (bytes <= 0L) {
            return 0L;
        }
        return bytes / (1024L * 1024L);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
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

    static String normalizeDiscoveredClassName(String className) {
        if (className == null || className.isBlank()) {
            return className;
        }
        if (className.startsWith(BOOT_INF_CLASSES_PREFIX)) {
            return className.substring(BOOT_INF_CLASSES_PREFIX.length());
        }
        if (className.startsWith(WEB_INF_CLASSES_PREFIX)) {
            return className.substring(WEB_INF_CLASSES_PREFIX.length());
        }
        return className;
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

    private static long getFileSize(String projectKey) {
        Path root = resolveStorePath(projectKey);
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

    private static Path resolveStorePath(String projectKey) {
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

    private static Map<String, Object> flattenBuildMetrics(Map<String, BuildStageMetric> stageMetrics,
                                                           long buildWallMs,
                                                           long peakHeapUsedBytes,
                                                           long peakHeapCommittedBytes,
                                                           long heapMaxBytes) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("build_wall_ms", Math.max(0L, buildWallMs));
        out.put("build_peak_heap_used_bytes", Math.max(0L, peakHeapUsedBytes));
        out.put("build_peak_heap_used_mb", toMiB(peakHeapUsedBytes));
        out.put("build_peak_heap_committed_bytes", Math.max(0L, peakHeapCommittedBytes));
        out.put("build_peak_heap_committed_mb", toMiB(peakHeapCommittedBytes));
        out.put("build_heap_max_bytes", Math.max(0L, heapMaxBytes));
        out.put("build_heap_max_mb", toMiB(heapMaxBytes));
        if (stageMetrics == null || stageMetrics.isEmpty()) {
            return out;
        }
        for (Map.Entry<String, BuildStageMetric> entry : stageMetrics.entrySet()) {
            if (entry == null || entry.getValue() == null) {
                continue;
            }
            String stageKey = normalizeMetricKey(entry.getKey());
            if (stageKey.isBlank()) {
                continue;
            }
            BuildStageMetric metric = entry.getValue();
            out.put("build_stage_" + stageKey + "_ms", Math.max(0L, metric.getDurationMs()));
            Map<String, Object> details = metric.getDetails();
            if (details == null || details.isEmpty()) {
                continue;
            }
            for (Map.Entry<String, Object> detailEntry : details.entrySet()) {
                if (detailEntry == null) {
                    continue;
                }
                String detailKey = normalizeMetricKey(detailEntry.getKey());
                if (detailKey.isBlank()) {
                    continue;
                }
                Object value = detailEntry.getValue();
                if (value == null) {
                    continue;
                }
                out.put("build_stage_" + stageKey + "_" + detailKey, value);
            }
        }
        return out;
    }

    private static String normalizeMetricKey(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        StringBuilder out = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_') {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static Map<String, Object> metricMap(Object... items) {
        if (items == null || items.length == 0) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i + 1 < items.length; i += 2) {
            Object rawKey = items[i];
            Object rawValue = items[i + 1];
            String key = normalizeMetricKey(rawKey == null ? "" : String.valueOf(rawKey));
            if (key.isBlank() || rawValue == null) {
                continue;
            }
            out.put(key, rawValue);
        }
        return out.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(out);
    }

    private static List<String> collectExplicitEntryMethods(BuildContext context,
                                                            Map<Integer, ProjectOrigin> jarOriginsById) {
        if (context == null || context.methodMap == null || context.methodMap.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> signatures = new LinkedHashSet<>();
        LinkedHashSet<String> entryOwners = new LinkedHashSet<>();

        if (context.controllers != null && !context.controllers.isEmpty()) {
            for (SpringController controller : context.controllers) {
                if (controller == null || controller.getMappings() == null || controller.getMappings().isEmpty()) {
                    continue;
                }
                for (SpringMapping mapping : controller.getMappings()) {
                    if (mapping == null) {
                        continue;
                    }
                    MethodReference method = mapping.getMethodReference();
                    if (method == null) {
                        method = resolveMethodByHandle(mapping.getMethodName(), context.methodMap);
                    }
                    addEntrySignature(signatures, entryOwners, method, jarOriginsById);
                }
            }
        }

        addEntryMethodsByClass(
                signatures,
                entryOwners,
                context.servlets,
                WebEntryMethods.SERVLET_ENTRY_METHODS,
                context.methodMap,
                jarOriginsById
        );
        addEntryMethodsByClass(
                signatures,
                entryOwners,
                context.filters,
                WebEntryMethods.FILTER_ENTRY_METHODS,
                context.methodMap,
                jarOriginsById
        );
        addEntryMethodsByClass(
                signatures,
                entryOwners,
                context.interceptors,
                WebEntryMethods.INTERCEPTOR_ENTRY_METHODS,
                context.methodMap,
                jarOriginsById
        );
        addEntryMethodsByClass(
                signatures,
                entryOwners,
                context.listeners,
                WebEntryMethods.LISTENER_ENTRY_METHODS,
                context.methodMap,
                jarOriginsById
        );
        if (context.explicitSourceMethodFlags != null && !context.explicitSourceMethodFlags.isEmpty()) {
            for (MethodReference.Handle handle : context.explicitSourceMethodFlags.keySet()) {
                addEntrySignature(signatures, entryOwners, resolveMethodByHandle(handle, context.methodMap), jarOriginsById);
            }
        }
        addEntryConstructors(signatures, entryOwners, context.methodMap, jarOriginsById);

        if (!signatures.isEmpty()) {
            logger.info("tai-e explicit entry methods resolved: {}", signatures.size());
        }
        return signatures.isEmpty() ? List.of() : List.copyOf(signatures);
    }

    private static void applyMethodSemanticFlags(BuildContext context) {
        if (context == null || context.discoveredMethods.isEmpty()) {
            return;
        }
        Map<MethodReference.Handle, Integer> flags = MethodSemanticSupport.derive(
                context.discoveredMethods,
                context.discoveredClasses,
                context.controllers,
                context.interceptors,
                context.servlets,
                context.filters,
                context.listeners,
                context.resources,
                context.explicitSourceMethodFlags
        );
        if (flags.isEmpty()) {
            return;
        }
        for (MethodReference method : context.discoveredMethods) {
            if (method != null) {
                method.mergeSemanticFlags(flags.getOrDefault(method.getHandle(), 0));
            }
        }
        for (MethodReference method : context.methodMap.values()) {
            if (method != null) {
                method.mergeSemanticFlags(flags.getOrDefault(method.getHandle(), 0));
            }
        }
    }

    private static int countSemanticMethods(BuildContext context) {
        if (context == null || context.discoveredMethods.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (MethodReference method : context.discoveredMethods) {
            if (method != null && method.getSemanticFlags() != 0) {
                count++;
            }
        }
        return count;
    }

    private static void addEntryMethodsByClass(Set<String> out,
                                               Set<String> ownerOut,
                                               List<String> classNames,
                                               Set<WebEntryMethodSpec> methodSpecs,
                                               Map<MethodReference.Handle, MethodReference> methodMap,
                                               Map<Integer, ProjectOrigin> jarOriginsById) {
        if (out == null || classNames == null || classNames.isEmpty()
                || methodSpecs == null || methodSpecs.isEmpty()
                || methodMap == null || methodMap.isEmpty()) {
            return;
        }
        Set<String> normalizedClasses = new HashSet<>();
        for (String className : classNames) {
            String normalized = normalizeInternalClassName(className);
            if (!normalized.isBlank()) {
                normalizedClasses.add(normalized);
            }
        }
        if (normalizedClasses.isEmpty()) {
            return;
        }
        for (MethodReference method : methodMap.values()) {
            if (method == null || method.getClassReference() == null) {
                continue;
            }
            String owner = normalizeInternalClassName(method.getClassReference().getName());
            if (!normalizedClasses.contains(owner)) {
                continue;
            }
            WebEntryMethodSpec spec = new WebEntryMethodSpec(safe(method.getName()), safe(method.getDesc()));
            if (!methodSpecs.contains(spec)) {
                continue;
            }
            addEntrySignature(out, ownerOut, method, jarOriginsById);
        }
    }

    private static MethodReference resolveMethodByHandle(MethodReference.Handle handle,
                                                         Map<MethodReference.Handle, MethodReference> methodMap) {
        if (handle == null || methodMap == null || methodMap.isEmpty()) {
            return null;
        }
        MethodReference direct = methodMap.get(handle);
        if (direct != null) {
            return direct;
        }
        String targetOwner = normalizeInternalClassName(
                handle.getClassReference() == null ? "" : handle.getClassReference().getName()
        );
        String targetName = safe(handle.getName());
        String targetDesc = safe(handle.getDesc());
        if (targetOwner.isBlank() || targetName.isBlank() || targetDesc.isBlank()) {
            return null;
        }
        for (MethodReference method : methodMap.values()) {
            if (method == null || method.getClassReference() == null) {
                continue;
            }
            if (!targetName.equals(method.getName()) || !targetDesc.equals(method.getDesc())) {
                continue;
            }
            String owner = normalizeInternalClassName(method.getClassReference().getName());
            if (targetOwner.equals(owner)) {
                return method;
            }
        }
        return null;
    }

    private static void mergeUnique(List<String> target, List<String> source) {
        if (target == null || source == null || source.isEmpty()) {
            return;
        }
        Set<String> seen = new HashSet<>(target);
        for (String value : source) {
            if (value != null && seen.add(value)) {
                target.add(value);
            }
        }
    }

    private static void addEntrySignature(Set<String> out,
                                          Set<String> ownerOut,
                                          MethodReference method,
                                          Map<Integer, ProjectOrigin> jarOriginsById) {
        if (out == null || method == null || !isAppMethod(method, jarOriginsById)) {
            return;
        }
        String signature = toTaieMethodSignature(method);
        if (!signature.isBlank()) {
            out.add(signature);
        }
        if (ownerOut == null || method.getClassReference() == null) {
            return;
        }
        String owner = normalizeInternalClassName(method.getClassReference().getName());
        if (!owner.isBlank()) {
            ownerOut.add(owner);
        }
    }

    private static void addEntryConstructors(Set<String> out,
                                             Set<String> owners,
                                             Map<MethodReference.Handle, MethodReference> methodMap,
                                             Map<Integer, ProjectOrigin> jarOriginsById) {
        if (out == null || owners == null || owners.isEmpty()
                || methodMap == null || methodMap.isEmpty()) {
            return;
        }
        Map<String, List<MethodReference>> constructorsByOwner = new LinkedHashMap<>();
        for (MethodReference method : methodMap.values()) {
            if (method == null || method.getClassReference() == null || !isAppMethod(method, jarOriginsById)) {
                continue;
            }
            if (!"<init>".equals(method.getName())) {
                continue;
            }
            String owner = normalizeInternalClassName(method.getClassReference().getName());
            if (owner.isBlank() || !owners.contains(owner)) {
                continue;
            }
            constructorsByOwner.computeIfAbsent(owner, ignored -> new ArrayList<>()).add(method);
        }
        for (String owner : owners) {
            List<MethodReference> constructors = constructorsByOwner.get(owner);
            if (constructors == null || constructors.isEmpty()) {
                continue;
            }
            boolean addedDefault = false;
            for (MethodReference constructor : constructors) {
                if (!"()V".equals(constructor.getDesc())) {
                    continue;
                }
                String signature = toTaieMethodSignature(constructor);
                if (!signature.isBlank()) {
                    out.add(signature);
                    addedDefault = true;
                }
            }
            if (addedDefault) {
                continue;
            }
            for (MethodReference constructor : constructors) {
                String signature = toTaieMethodSignature(constructor);
                if (!signature.isBlank()) {
                    out.add(signature);
                }
            }
        }
    }

    private static boolean isAppMethod(MethodReference method,
                                       Map<Integer, ProjectOrigin> jarOriginsById) {
        if (method == null || method.getJarId() == null) {
            return false;
        }
        Integer jarId = method.getJarId();
        if (jarId <= 0) {
            return false;
        }
        ProjectOrigin origin = jarOriginsById == null ? null : jarOriginsById.get(jarId);
        return origin == ProjectOrigin.APP;
    }

    private static String toTaieMethodSignature(MethodReference method) {
        if (method == null || method.getClassReference() == null) {
            return "";
        }
        String owner = normalizeMainClassName(method.getClassReference().getName());
        String methodName = safe(method.getName());
        String desc = safe(method.getDesc());
        if (owner.isBlank() || methodName.isBlank() || desc.isBlank()) {
            return "";
        }
        try {
            Type mType = Type.getMethodType(desc);
            String returnType = toTaieTypeName(mType.getReturnType());
            Type[] args = mType.getArgumentTypes();
            StringBuilder params = new StringBuilder();
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) {
                        params.append(',');
                    }
                    params.append(toTaieTypeName(args[i]));
                }
            }
            return "<" + owner + ": " + returnType + " " + methodName + "(" + params + ")>";
        } catch (Exception ex) {
            logger.debug("build Tai-e method signature failed: {}#{}{} ({})",
                    owner, methodName, desc, ex.toString());
            return "";
        }
    }

    private static String toTaieTypeName(Type type) {
        if (type == null) {
            return "java.lang.Object";
        }
        return switch (type.getSort()) {
            case Type.VOID -> "void";
            case Type.BOOLEAN -> "boolean";
            case Type.CHAR -> "char";
            case Type.BYTE -> "byte";
            case Type.SHORT -> "short";
            case Type.INT -> "int";
            case Type.FLOAT -> "float";
            case Type.LONG -> "long";
            case Type.DOUBLE -> "double";
            default -> type.getClassName();
        };
    }

    private static String normalizeInternalClassName(String className) {
        if (className == null || className.isBlank()) {
            return "";
        }
        String normalized = className.trim().replace('.', '/').replace('\\', '/');
        if (normalized.startsWith("L") && normalized.endsWith(";") && normalized.length() > 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - ".class".length());
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static Map<String, Object> buildMeta(String callGraphEngine,
                                                 String analysisProfile,
                                                 ScopeSummary scopeSummary,
                                                 JdkResolution jdkResolution,
                                                 int oracleEdgeCount,
                                                 int oracleEntryMethodCount,
                                                 int oracleReachableMethodCount,
                                                 int oraclePointsToVarCount,
                                                 int oraclePointsToObjectCount,
                                                 int oracleEndpointThisVarCount,
                                                 long oracleEndpointAliasPairs,
                                                 String oracleReflectionInference,
                                                 String oracleReflectionLog,
                                                 int explicitEntryMethodCount) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("call_graph_engine", safe(callGraphEngine));
        meta.put("analysis_profile", safe(analysisProfile));
        meta.put("target_jar_count", scopeSummary == null ? 0 : Math.max(0, scopeSummary.targetArchiveCount()));
        meta.put("library_jar_count", scopeSummary == null ? 0 : Math.max(0, scopeSummary.libraryArchiveCount()));
        meta.put("sdk_entry_count", jdkResolution == null ? 0 : Math.max(0, jdkResolution.sdkEntryCount()));
        meta.put("oracle_edge_count", Math.max(0, oracleEdgeCount));
        meta.put("explicit_entry_method_count", Math.max(0, explicitEntryMethodCount));
        meta.put("oracle_entry_method_count", Math.max(0, oracleEntryMethodCount));
        meta.put("oracle_reachable_method_count", Math.max(0, oracleReachableMethodCount));
        meta.put("oracle_points_to_var_count", Math.max(0, oraclePointsToVarCount));
        meta.put("oracle_points_to_object_count", Math.max(0, oraclePointsToObjectCount));
        meta.put("oracle_endpoint_this_var_count", Math.max(0, oracleEndpointThisVarCount));
        meta.put("oracle_endpoint_alias_pair_count", Math.max(0L, oracleEndpointAliasPairs));
        if (oracleReflectionInference != null && !oracleReflectionInference.isBlank()) {
            meta.put("oracle_reflection_inference", oracleReflectionInference);
        }
        if (oracleReflectionLog != null && !oracleReflectionLog.isBlank()) {
            meta.put("oracle_reflection_log", oracleReflectionLog);
        }
        return meta;
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
                logger.debug("normalize input path fail: {} ({})", value, ignored.toString());
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
        private final int callSiteCount;
        private final long edgeCount;
        private final long dbSizeBytes;
        private final String dbSizeLabel;
        private final boolean quickMode;
        private final String callGraphEngine;
        private final String callGraphMode;
        private final String analysisProfile;
        private final int targetJarCount;
        private final int libraryJarCount;
        private final int sdkEntryCount;
        private final int oracleEdgeCount;
        private final int oracleEntryMethodCount;
        private final int oracleReachableMethodCount;
        private final int oraclePointsToVarCount;
        private final int oraclePointsToObjectCount;
        private final int oracleEndpointThisVarCount;
        private final long oracleEndpointAliasPairCount;
        private final long peakHeapUsedBytes;
        private final long peakHeapCommittedBytes;
        private final long heapMaxBytes;
        private final long buildWallMs;
        private final Map<String, BuildStageMetric> stageMetrics;

        public BuildResult(long buildSeq,
                           int jarCount,
                           int classFileCount,
                           int classCount,
                           int methodCount,
                           int callSiteCount,
                           long edgeCount,
                           long dbSizeBytes,
                           String dbSizeLabel,
                           boolean quickMode,
                           String callGraphEngine,
                           String callGraphMode,
                           String analysisProfile,
                           int targetJarCount,
                           int libraryJarCount,
                           int sdkEntryCount,
                           int oracleEdgeCount,
                           int oracleEntryMethodCount,
                           int oracleReachableMethodCount,
                           int oraclePointsToVarCount,
                           int oraclePointsToObjectCount,
                           int oracleEndpointThisVarCount,
                           long oracleEndpointAliasPairCount,
                           long peakHeapUsedBytes,
                           long peakHeapCommittedBytes,
                           long heapMaxBytes,
                           long buildWallMs,
                           Map<String, BuildStageMetric> stageMetrics) {
            this.buildSeq = buildSeq;
            this.jarCount = jarCount;
            this.classFileCount = classFileCount;
            this.classCount = classCount;
            this.methodCount = methodCount;
            this.callSiteCount = Math.max(0, callSiteCount);
            this.edgeCount = edgeCount;
            this.dbSizeBytes = dbSizeBytes;
            this.dbSizeLabel = dbSizeLabel;
            this.quickMode = quickMode;
            this.callGraphEngine = callGraphEngine;
            this.callGraphMode = callGraphMode;
            this.analysisProfile = analysisProfile;
            this.targetJarCount = Math.max(0, targetJarCount);
            this.libraryJarCount = Math.max(0, libraryJarCount);
            this.sdkEntryCount = Math.max(0, sdkEntryCount);
            this.oracleEdgeCount = Math.max(0, oracleEdgeCount);
            this.oracleEntryMethodCount = Math.max(0, oracleEntryMethodCount);
            this.oracleReachableMethodCount = Math.max(0, oracleReachableMethodCount);
            this.oraclePointsToVarCount = Math.max(0, oraclePointsToVarCount);
            this.oraclePointsToObjectCount = Math.max(0, oraclePointsToObjectCount);
            this.oracleEndpointThisVarCount = Math.max(0, oracleEndpointThisVarCount);
            this.oracleEndpointAliasPairCount = Math.max(0L, oracleEndpointAliasPairCount);
            this.peakHeapUsedBytes = Math.max(0L, peakHeapUsedBytes);
            this.peakHeapCommittedBytes = Math.max(0L, peakHeapCommittedBytes);
            this.heapMaxBytes = Math.max(0L, heapMaxBytes);
            this.buildWallMs = Math.max(0L, buildWallMs);
            this.stageMetrics = stageMetrics == null || stageMetrics.isEmpty()
                    ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(stageMetrics));
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

        public int getCallSiteCount() {
            return callSiteCount;
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

        public String getCallGraphMode() {
            return callGraphMode;
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

        public int getOracleEdgeCount() {
            return oracleEdgeCount;
        }

        public int getOracleEntryMethodCount() {
            return oracleEntryMethodCount;
        }

        public int getOracleReachableMethodCount() {
            return oracleReachableMethodCount;
        }

        public int getOraclePointsToVarCount() {
            return oraclePointsToVarCount;
        }

        public int getOraclePointsToObjectCount() {
            return oraclePointsToObjectCount;
        }

        public int getOracleEndpointThisVarCount() {
            return oracleEndpointThisVarCount;
        }

        public long getOracleEndpointAliasPairCount() {
            return oracleEndpointAliasPairCount;
        }

        public long getPeakHeapUsedBytes() {
            return peakHeapUsedBytes;
        }

        public long getPeakHeapCommittedBytes() {
            return peakHeapCommittedBytes;
        }

        public long getHeapMaxBytes() {
            return heapMaxBytes;
        }

        public long getBuildWallMs() {
            return buildWallMs;
        }

        public Map<String, BuildStageMetric> getStageMetrics() {
            return stageMetrics;
        }

        public BuildStageMetric getStageMetric(String stageKey) {
            if (stageKey == null || stageKey.isBlank() || stageMetrics.isEmpty()) {
                return null;
            }
            return stageMetrics.get(normalizeMetricKey(stageKey));
        }
    }

    public static final class BuildStageMetric {
        private final String stageKey;
        private final long durationMs;
        private final Map<String, Object> details;

        private BuildStageMetric(String stageKey, long durationMs, Map<String, Object> details) {
            this.stageKey = normalizeMetricKey(stageKey);
            this.durationMs = Math.max(0L, durationMs);
            this.details = details == null || details.isEmpty()
                    ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(details));
        }

        public String getStageKey() {
            return stageKey;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public Map<String, Object> getDetails() {
            return details;
        }
    }

    private static final class BuildMetricsCollector {
        private final LinkedHashMap<String, BuildStageMetric> stages = new LinkedHashMap<>();
        private long peakHeapUsedBytes;
        private long peakHeapCommittedBytes;
        private long heapMaxBytes;

        private void record(String stageKey, long durationMs, Map<String, Object> details) {
            String normalized = normalizeMetricKey(stageKey);
            if (normalized.isBlank()) {
                return;
            }
            HeapStats heap = captureHeapStats();
            peakHeapUsedBytes = Math.max(peakHeapUsedBytes, heap.usedBytes());
            peakHeapCommittedBytes = Math.max(peakHeapCommittedBytes, heap.committedBytes());
            heapMaxBytes = Math.max(heapMaxBytes, heap.maxBytes());
            LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
            if (details != null && !details.isEmpty()) {
                merged.putAll(details);
            }
            merged.putIfAbsent("heap_used_bytes", heap.usedBytes());
            merged.putIfAbsent("heap_committed_bytes", heap.committedBytes());
            merged.putIfAbsent("heap_max_bytes", heap.maxBytes());
            merged.putIfAbsent("heap_used_mb", toMiB(heap.usedBytes()));
            merged.putIfAbsent("heap_committed_mb", toMiB(heap.committedBytes()));
            merged.putIfAbsent("heap_max_mb", toMiB(heap.maxBytes()));
            stages.put(normalized, new BuildStageMetric(normalized, durationMs, merged));
        }

        private Map<String, BuildStageMetric> snapshot() {
            if (stages.isEmpty()) {
                return Collections.emptyMap();
            }
            return Collections.unmodifiableMap(new LinkedHashMap<>(stages));
        }

        private long peakHeapUsedBytes() {
            return Math.max(0L, peakHeapUsedBytes);
        }

        private long peakHeapCommittedBytes() {
            return Math.max(0L, peakHeapCommittedBytes);
        }

        private long heapMaxBytes() {
            return Math.max(0L, heapMaxBytes);
        }
    }
}
