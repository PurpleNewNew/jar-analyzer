/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.taie;

import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class TaieAnalysisRunner {
    private static final Logger logger = LogManager.getLogger();
    private static final String INDY_PLUGIN_PACKAGE = "pascal.taie.analysis.pta.plugin.invokedynamic";

    private static final String ANALYSIS_PROFILE_PROP = "jar.analyzer.analysis.profile";
    private static final String DEFAULT_ANALYSIS_PROFILE = "balanced";
    private static final String INVOKEDYNAMIC_ERR = "InvokeDynamic.getMethodRef() is unavailable";
    private static final String MAIN_CLASS_MISSING_ERR = "Main-class has no main method!";
    private static final String FAT_ARCHIVE_DIR = "taie-fat-archives";
    private static final String REFLECTION_INFERENCE_PROP = "jar.analyzer.taie.reflection.inference";
    private static final String REFLECTION_LOG_PROP = "jar.analyzer.taie.reflection.log";
    private static final String DEFAULT_REFLECTION_INFERENCE = "solar";
    private static final String EXTRA_ENTRY_PLUGIN = "me.n1ar4.jar.analyzer.core.taie.TaieExtraEntryPointPlugin";
    private static final int MAX_ENDPOINT_ALIAS_CHECKS = 200_000;
    private static final String BOOT_INF_CLASSES_PREFIX = "BOOT-INF/classes/";
    private static final String WEB_INF_CLASSES_PREFIX = "WEB-INF/classes/";
    private static final String BOOT_INF_LIB_PREFIX = "BOOT-INF/lib/";
    private static final String WEB_INF_LIB_PREFIX = "WEB-INF/lib/";

    private TaieAnalysisRunner() {
    }

    public static TaieRunResult run(List<Path> appArchives,
                                    List<Path> classpathArchives,
                                    AnalysisProfile profile) {
        return run(appArchives, classpathArchives, profile, null, List.of(), false);
    }

    public static TaieRunResult run(List<Path> appArchives,
                                    List<Path> classpathArchives,
                                    AnalysisProfile profile,
                                    String mainClass) {
        return run(appArchives, classpathArchives, profile, mainClass, List.of(), false);
    }

    public static TaieRunResult run(List<Path> appArchives,
                                    List<Path> classpathArchives,
                                    AnalysisProfile profile,
                                    String mainClass,
                                    List<String> explicitEntryMethods) {
        return run(appArchives, classpathArchives, profile, mainClass, explicitEntryMethods, false);
    }

    public static TaieRunResult run(List<Path> appArchives,
                                    List<Path> classpathArchives,
                                    AnalysisProfile profile,
                                    String mainClass,
                                    List<String> explicitEntryMethods,
                                    boolean collectEndpointAliasStats) {
        AnalysisProfile resolved = profile == null
                ? AnalysisProfile.fromSystemProperty()
                : profile;

        List<Path> app = safePaths(appArchives);
        List<Path> cp = safePaths(classpathArchives);
        if (app.isEmpty()) {
            return TaieRunResult.disabled(resolved, "no-app-archives");
        }
        if (cp.isEmpty()) {
            cp = app;
        }
        List<String> entryMethods = normalizeEntryMethods(explicitEntryMethods);
        String reflectionInference = resolveReflectionInference();
        String reflectionLog = resolveReflectionLog();
        PreparedInput preparedInput = prepareInputArchives(app, cp);
        app = preparedInput.appArchives();
        cp = preparedInput.classpathArchives();
        if (preparedInput.expandedAppArchiveCount() > 0 || preparedInput.addedNestedLibCount() > 0) {
            logger.info("normalize fat archives for Tai-e: expandedAppArchives={} addedNestedLibs={}",
                    preparedInput.expandedAppArchiveCount(),
                    preparedInput.addedNestedLibCount());
        }

        TaieExtraEntryPointPlugin.install(entryMethods);
        try {
            Throwable lastError = null;
            String lastReason = "";
            String activeMainClass = mainClass == null ? "" : mainClass.trim();
            boolean retriedWithoutMainClass = false;
            boolean[] invokeDynamicAttempts = {true, false};
            while (true) {
                for (int i = 0; i < invokeDynamicAttempts.length; i++) {
                    boolean handleInvokedynamic = invokeDynamicAttempts[i];
                    List<String> args = buildArgs(
                            app,
                            cp,
                            resolved,
                            handleInvokedynamic,
                            activeMainClass,
                            reflectionInference,
                            reflectionLog,
                            !entryMethods.isEmpty()
                    );
                    logger.info("run Tai-e attempt={}/{} profile={} appArchives={} classpathArchives={} mainClass={} invokedynamic={} reflection={} reflectionLog={} explicitEntries={}",
                            i + 1,
                            invokeDynamicAttempts.length,
                            resolved.value,
                            app.size(),
                            cp.size(),
                            activeMainClass.isBlank() ? "<none>" : activeMainClass,
                            handleInvokedynamic ? "on" : "off",
                            reflectionInference,
                            reflectionLog.isBlank() ? "<none>" : reflectionLog,
                            entryMethods.size());

                    long start = System.nanoTime();
                    try {
                        World.reset();
                        ensureTaiEStdoutAppender();
                        Main.main(args.toArray(new String[0]));
                        World world = World.get();
                        if (world == null) {
                            lastReason = "tai-e world is null";
                        } else {
                            PointerAnalysisResult pointer = world.getResult(PointerAnalysis.ID);
                            if (pointer == null) {
                                lastReason = "pta result is null";
                            } else {
                                CallGraph<Invoke, JMethod> callGraph = pointer.getCallGraph();
                                if (callGraph == null) {
                                    lastReason = "pta call graph is null";
                                } else {
                                    PtaStats stats = collectPtaStats(
                                            pointer,
                                            callGraph,
                                            entryMethods,
                                            collectEndpointAliasStats
                                    );
                                    long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
                                    if (i > 0 || retriedWithoutMainClass) {
                                        logger.warn("tai-e fallback activated: attempt={} invokedynamic={} mainClass={}",
                                                i + 1,
                                                handleInvokedynamic ? "on" : "off",
                                                activeMainClass.isBlank() ? "<none>" : activeMainClass);
                                    }
                                    return TaieRunResult.success(
                                            resolved,
                                            callGraph,
                                            elapsedMs,
                                            stats,
                                            reflectionInference,
                                            reflectionLog,
                                            entryMethods.size()
                                    );
                                }
                            }
                        }
                    } catch (Throwable ex) {
                        lastError = ex;
                        lastReason = safe(ex.getMessage());
                        logger.warn("run Tai-e attempt {} failed: {}", i + 1, ex.toString());
                    } finally {
                        try {
                            World.reset();
                        } catch (Throwable ignored) {
                            logger.debug("reset Tai-e world in finally fail: {}", ignored.toString());
                        }
                    }

                    InvokeDynamicFallback fallback = resolveInvokeDynamicFallback(lastError, lastReason);
                    if (i >= invokeDynamicAttempts.length - 1 || !fallback.shouldRetryWithIndyOff()) {
                        break;
                    }
                    logger.warn("tai-e invokedynamic fallback={}, retry with invokedynamic=off", fallback.value());
                }
                if (retriedWithoutMainClass || !safe(lastReason).contains(MAIN_CLASS_MISSING_ERR)) {
                    break;
                }
                retriedWithoutMainClass = true;
                activeMainClass = "";
                lastError = null;
                lastReason = "";
                logger.warn("tai-e main class not applicable, retry without explicit main class");
            }
            if (lastError != null) {
                logger.error("run Tai-e failed: {}", lastError.toString(), lastError);
            }
            return TaieRunResult.failed(
                    resolved,
                    lastReason,
                    reflectionInference,
                    reflectionLog,
                    entryMethods.size()
            );
        } finally {
            TaieExtraEntryPointPlugin.clear();
        }
    }

    public static AnalysisProfile resolveProfile() {
        return AnalysisProfile.fromSystemProperty();
    }

    private static List<String> buildArgs(List<Path> app,
                                          List<Path> cp,
                                          AnalysisProfile profile,
                                          boolean handleInvokedynamic,
                                          String mainClass,
                                          String reflectionInference,
                                          String reflectionLog,
                                          boolean hasExtraEntryMethods) {
        String appClasspath = joinClasspath(app);
        String fullClasspath = joinClasspath(cp);
        Path outputDir = Path.of(Const.tempDir, "taie-output").toAbsolutePath().normalize();

        List<String> args = new ArrayList<>();
        args.add("-acp");
        args.add(appClasspath);
        args.add("-cp");
        args.add(fullClasspath);
        if (mainClass != null && !mainClass.isBlank()) {
            args.add("-m");
            args.add(mainClass.trim());
        }
        args.add("-pp");
        args.add("-ap");
        args.add("-scope");
        args.add("APP");
        args.add("--output-dir");
        args.add(outputDir.toString());
        args.add("-a");
        args.add("pta=" + buildPtaOptions(
                profile,
                handleInvokedynamic,
                reflectionInference,
                reflectionLog,
                hasExtraEntryMethods
        ));
        return args;
    }

    private static String buildPtaOptions(AnalysisProfile profile,
                                          boolean handleInvokedynamic,
                                          String reflectionInference,
                                          String reflectionLog,
                                          boolean hasExtraEntryMethods) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("cs:").append(profile.contextSelector)
                .append(";only-app:true;implicit-entries:true;")
                .append("handle-invokedynamic:").append(handleInvokedynamic).append(";");
        sb.append("reflection-inference:")
                .append(resolveReflectionInferenceValue(reflectionInference))
                .append(";");
        if (reflectionLog != null && !reflectionLog.isBlank()) {
            sb.append("reflection-log:").append(reflectionLog.trim()).append(";");
        }
        if (hasExtraEntryMethods) {
            sb.append("plugins:[").append(EXTRA_ENTRY_PLUGIN).append("];");
        }
        return sb.toString();
    }

    private static String resolveReflectionInference() {
        String raw = System.getProperty(REFLECTION_INFERENCE_PROP, DEFAULT_REFLECTION_INFERENCE);
        return resolveReflectionInferenceValue(raw);
    }

    private static String resolveReflectionInferenceValue(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_REFLECTION_INFERENCE;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("string-constant".equals(normalized)
                || "solar".equals(normalized)
                || "null".equals(normalized)) {
            return normalized;
        }
        logger.warn("unknown reflection inference: {}, fallback {}", value, DEFAULT_REFLECTION_INFERENCE);
        return DEFAULT_REFLECTION_INFERENCE;
    }

    private static String resolveReflectionLog() {
        String raw = System.getProperty(REFLECTION_LOG_PROP, "");
        if (raw == null || raw.isBlank()) {
            return "";
        }
        try {
            Path path = Path.of(raw).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path)) {
                logger.warn("reflection log file not found: {}, ignore", path);
                return "";
            }
            return path.toString();
        } catch (Exception ex) {
            logger.warn("invalid reflection log path {}, ignore", raw);
            return "";
        }
    }

    private static List<String> normalizeEntryMethods(List<String> explicitEntryMethods) {
        if (explicitEntryMethods == null || explicitEntryMethods.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String raw : explicitEntryMethods) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String value = raw.trim();
            if (!value.isEmpty()) {
                out.add(value);
            }
        }
        if (out.isEmpty()) {
            return List.of();
        }
        return List.copyOf(out);
    }

    private static PtaStats collectPtaStats(PointerAnalysisResult pointer,
                                            CallGraph<Invoke, JMethod> callGraph,
                                            List<String> explicitEntryMethods,
                                            boolean enableEndpointAliasStats) {
        int entryMethodCount = 0;
        int reachableMethodCount = 0;
        int varCount = 0;
        int objectCount = 0;
        int endpointThisVarCount = 0;
        long endpointAliasChecks = 0L;
        long endpointAliasPairs = 0L;
        try {
            if (callGraph != null) {
                entryMethodCount = safeToInt(callGraph.entryMethods().count());
                reachableMethodCount = safeToInt(callGraph.reachableMethods().count());
            }
        } catch (Exception ex) {
            logger.debug("collect tai-e call graph stats failed: {}", ex.toString());
        }
        try {
            if (pointer != null) {
                varCount = pointer.getVars() == null ? 0 : pointer.getVars().size();
                objectCount = pointer.getObjects() == null ? 0 : pointer.getObjects().size();
                AliasStats aliasStats = enableEndpointAliasStats
                        ? collectEndpointAliasStats(pointer, explicitEntryMethods)
                        : AliasStats.EMPTY;
                endpointThisVarCount = aliasStats.thisVarCount();
                endpointAliasChecks = aliasStats.aliasChecks();
                endpointAliasPairs = aliasStats.aliasPairs();
            }
        } catch (Exception ex) {
            logger.debug("collect tai-e points-to stats failed: {}", ex.toString());
        }
        return new PtaStats(
                entryMethodCount,
                reachableMethodCount,
                varCount,
                objectCount,
                endpointThisVarCount,
                endpointAliasChecks,
                endpointAliasPairs
        );
    }

    private static AliasStats collectEndpointAliasStats(PointerAnalysisResult pointer,
                                                        List<String> explicitEntryMethods) {
        if (pointer == null || explicitEntryMethods == null || explicitEntryMethods.isEmpty()) {
            return AliasStats.EMPTY;
        }
        Set<String> unique = new LinkedHashSet<>(explicitEntryMethods);
        if (unique.isEmpty()) {
            return AliasStats.EMPTY;
        }
        List<Var> thisVars = new ArrayList<>();
        for (String signature : unique) {
            if (signature == null || signature.isBlank()) {
                continue;
            }
            try {
                JMethod method = World.get().getClassHierarchy().getMethod(signature);
                if (method == null || method.isStatic() || method.isAbstract() || method.isNative()) {
                    continue;
                }
                Var thisVar = method.getIR().getThis();
                if (thisVar != null) {
                    thisVars.add(thisVar);
                }
            } catch (Exception ignored) {
                logger.debug("resolve explicit entry for alias stats failed: {}", signature);
            }
        }
        if (thisVars.size() < 2) {
            return new AliasStats(thisVars.size(), 0L, 0L);
        }
        long checks = 0L;
        long pairs = 0L;
        for (int i = 0; i < thisVars.size(); i++) {
            Var left = thisVars.get(i);
            if (left == null) {
                continue;
            }
            for (int j = i + 1; j < thisVars.size(); j++) {
                if (checks >= MAX_ENDPOINT_ALIAS_CHECKS) {
                    return new AliasStats(thisVars.size(), checks, pairs);
                }
                Var right = thisVars.get(j);
                if (right == null) {
                    continue;
                }
                checks++;
                try {
                    if (pointer.mayAlias(left, right)) {
                        pairs++;
                    }
                } catch (Exception ignored) {
                    logger.debug("endpoint alias check failed: {} vs {}", left, right);
                }
            }
        }
        return new AliasStats(thisVars.size(), checks, pairs);
    }

    private static int safeToInt(long value) {
        if (value <= 0L) {
            return 0;
        }
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) value;
    }

    private static InvokeDynamicFallback resolveInvokeDynamicFallback(Throwable error, String reason) {
        if (containsInvokeDynamicFrontendError(reason)) {
            return InvokeDynamicFallback.INDY_FRONTEND_UNSUPPORTED;
        }
        Throwable cursor = error;
        while (cursor != null) {
            if (containsInvokeDynamicFrontendError(cursor.getMessage())) {
                return InvokeDynamicFallback.INDY_FRONTEND_UNSUPPORTED;
            }
            if (isInvokeDynamicPluginFailure(cursor)) {
                return InvokeDynamicFallback.INDY_PLUGIN_FAILURE;
            }
            cursor = cursor.getCause();
        }
        return InvokeDynamicFallback.NONE;
    }

    private static boolean containsInvokeDynamicFrontendError(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains(INVOKEDYNAMIC_ERR.toLowerCase(Locale.ROOT))
                || normalized.contains("invokedynamic.getmethodref()");
    }

    private static boolean isInvokeDynamicPluginFailure(Throwable error) {
        if (error == null) {
            return false;
        }
        if (!(error instanceof UnsupportedOperationException)
                && !(error instanceof IllegalStateException)
                && !(error instanceof NoSuchMethodError)
                && !(error instanceof NullPointerException)) {
            return false;
        }
        if (!stackContains(error, INDY_PLUGIN_PACKAGE)) {
            return false;
        }
        String msg = safe(error.getMessage()).toLowerCase(Locale.ROOT);
        if (msg.isEmpty()) {
            return true;
        }
        return msg.contains("invokedynamic")
                || msg.contains("methodhandle")
                || msg.contains("lambdametafactory")
                || msg.contains("bootstrap method");
    }

    private static boolean stackContains(Throwable error, String token) {
        if (error == null || token == null || token.isBlank()) {
            return false;
        }
        for (StackTraceElement element : error.getStackTrace()) {
            if (element == null) {
                continue;
            }
            String owner = element.getClassName();
            if (owner != null && owner.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static void ensureTaiEStdoutAppender() {
        try {
            org.apache.logging.log4j.core.Logger rootLogger =
                    (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager.getRootLogger();
            LoggerContext context = rootLogger.getContext();
            if (context == null) {
                return;
            }
            Configuration configuration = context.getConfiguration();
            if (configuration == null || configuration.getAppender("STDOUT") != null) {
                return;
            }
            ConsoleAppender appender = ConsoleAppender.newBuilder()
                    .setName("STDOUT")
                    .setLayout(PatternLayout.newBuilder()
                            .withPattern("%m%n")
                            .build())
                    .setTarget(ConsoleAppender.Target.SYSTEM_OUT)
                    .build();
            if (appender == null) {
                return;
            }
            appender.start();
            configuration.addAppender(appender);
            LoggerConfig rootConfig = configuration.getRootLogger();
            if (rootConfig != null) {
                rootConfig.addAppender(appender, rootConfig.getLevel(), rootConfig.getFilter());
            }
            context.updateLoggers();
        } catch (Throwable ex) {
            logger.warn("ensure Tai-e stdout appender failed: {}", ex.toString());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static PreparedInput prepareInputArchives(List<Path> appArchives,
                                                      List<Path> classpathArchives) {
        if (appArchives == null || appArchives.isEmpty()) {
            return new PreparedInput(List.of(), List.of(), 0, 0);
        }
        java.util.Map<Path, PreparedFatArchive> preparedFatArchives = new java.util.LinkedHashMap<>();
        LinkedHashSet<Path> nestedLibs = new LinkedHashSet<>();
        List<Path> preparedApp = new ArrayList<>();
        int expandedAppArchiveCount = 0;
        for (Path appArchive : appArchives) {
            PreparedFatArchive prepared = preparedFatArchives.computeIfAbsent(
                    appArchive,
                    TaieAnalysisRunner::prepareFatArchive
            );
            if (prepared == null) {
                preparedApp.add(appArchive);
                continue;
            }
            preparedApp.add(prepared.classesRoot());
            expandedAppArchiveCount++;
            if (prepared.nestedLibs() != null && !prepared.nestedLibs().isEmpty()) {
                nestedLibs.addAll(prepared.nestedLibs());
            }
        }

        LinkedHashSet<Path> preparedClasspath = new LinkedHashSet<>();
        if (classpathArchives != null && !classpathArchives.isEmpty()) {
            for (Path classpathArchive : classpathArchives) {
                PreparedFatArchive prepared = preparedFatArchives.computeIfAbsent(
                        classpathArchive,
                        TaieAnalysisRunner::prepareFatArchive
                );
                if (prepared == null) {
                    preparedClasspath.add(classpathArchive);
                    continue;
                }
                preparedClasspath.add(prepared.classesRoot());
                if (prepared.nestedLibs() != null && !prepared.nestedLibs().isEmpty()) {
                    nestedLibs.addAll(prepared.nestedLibs());
                }
            }
        }
        preparedClasspath.addAll(preparedApp);
        preparedClasspath.addAll(nestedLibs);

        return new PreparedInput(
                Collections.unmodifiableList(preparedApp),
                Collections.unmodifiableList(new ArrayList<>(preparedClasspath)),
                expandedAppArchiveCount,
                nestedLibs.size()
        );
    }

    private static PreparedFatArchive prepareFatArchive(Path archive) {
        if (archive == null || !Files.isRegularFile(archive)) {
            return null;
        }
        String name = archive.getFileName() == null ? "" : archive.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".jar") && !name.endsWith(".war")) {
            return null;
        }
        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            boolean containsBootClasses = false;
            for (ZipEntry entry : Collections.list(zipFile.entries())) {
                if (entry == null || entry.isDirectory()) {
                    continue;
                }
                String entryName = entry.getName();
                if (entryName.startsWith(BOOT_INF_CLASSES_PREFIX) || entryName.startsWith(WEB_INF_CLASSES_PREFIX)) {
                    containsBootClasses = true;
                    break;
                }
            }
            if (!containsBootClasses) {
                return null;
            }

            Path outputRoot = buildFatArchiveOutputRoot(archive);
            Path classesRoot = outputRoot.resolve("classes");
            Path libsRoot = outputRoot.resolve("libs");
            Path ready = outputRoot.resolve(".ready");
            if (!Files.exists(ready)) {
                if (Files.exists(outputRoot)) {
                    deleteDirectory(outputRoot);
                }
                Files.createDirectories(classesRoot);
                Files.createDirectories(libsRoot);
                int nestedLibIndex = 0;
                for (ZipEntry entry : Collections.list(zipFile.entries())) {
                    if (entry == null || entry.isDirectory()) {
                        continue;
                    }
                    String entryName = entry.getName();
                    String relativeClass = trimClassEntryPrefix(entryName);
                    if (!relativeClass.isBlank()) {
                        Path out = classesRoot.resolve(relativeClass).normalize();
                        if (!out.startsWith(classesRoot)) {
                            continue;
                        }
                        Path parent = out.getParent();
                        if (parent != null) {
                            Files.createDirectories(parent);
                        }
                        try (InputStream in = zipFile.getInputStream(entry)) {
                            Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                        }
                        continue;
                    }
                    if (isNestedLibraryEntry(entryName)) {
                        String fileName = entryName.substring(entryName.lastIndexOf('/') + 1);
                        if (fileName.isBlank()) {
                            fileName = "nested.jar";
                        }
                        String nestedFile = String.format("%04d-%s", nestedLibIndex++, fileName);
                        Path out = libsRoot.resolve(nestedFile).normalize();
                        if (!out.startsWith(libsRoot)) {
                            continue;
                        }
                        try (InputStream in = zipFile.getInputStream(entry)) {
                            Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
                Files.writeString(ready, "ok");
            }

            List<Path> nestedLibs = new ArrayList<>();
            if (Files.isDirectory(libsRoot)) {
                try (var walk = Files.walk(libsRoot)) {
                    walk.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName() != null
                                    && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                            .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                            .forEach(nestedLibs::add);
                }
            }
            return new PreparedFatArchive(classesRoot, nestedLibs);
        } catch (Exception ex) {
            logger.debug("prepare fat archive for Tai-e failed: {}: {}", archive, ex.toString());
            return null;
        }
    }

    private static Path buildFatArchiveOutputRoot(Path archive) {
        String normalized = archive.toAbsolutePath().normalize().toString();
        long size = 0L;
        long mtime = 0L;
        try {
            size = Files.size(archive);
            mtime = Files.getLastModifiedTime(archive).toMillis();
        } catch (Exception ignored) {
            logger.debug("read fat archive metadata fail: {} ({})", archive, ignored.toString());
        }
        String digest = Integer.toUnsignedString((normalized + "#" + size + "#" + mtime).hashCode(), 36);
        return Path.of(Const.tempDir, FAT_ARCHIVE_DIR, digest).toAbsolutePath().normalize();
    }

    private static String trimClassEntryPrefix(String entryName) {
        if (entryName == null || entryName.isBlank()) {
            return "";
        }
        String normalized = entryName.trim();
        if (normalized.startsWith(BOOT_INF_CLASSES_PREFIX)) {
            normalized = normalized.substring(BOOT_INF_CLASSES_PREFIX.length());
        } else if (normalized.startsWith(WEB_INF_CLASSES_PREFIX)) {
            normalized = normalized.substring(WEB_INF_CLASSES_PREFIX.length());
        } else {
            return "";
        }
        if (normalized.isBlank() || !normalized.endsWith(".class")) {
            return "";
        }
        return normalized;
    }

    private static boolean isNestedLibraryEntry(String entryName) {
        if (entryName == null || entryName.isBlank()) {
            return false;
        }
        String normalized = entryName.toLowerCase(Locale.ROOT);
        return (normalized.startsWith(BOOT_INF_LIB_PREFIX.toLowerCase(Locale.ROOT))
                || normalized.startsWith(WEB_INF_LIB_PREFIX.toLowerCase(Locale.ROOT)))
                && normalized.endsWith(".jar");
    }

    private static void deleteDirectory(Path root) {
        if (root == null || Files.notExists(root)) {
            return;
        }
        try (var walk = Files.walk(root)) {
                walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                        logger.debug("delete Tai-e temp path fail: {} ({})", path, ignored.toString());
                    }
                });
        } catch (Exception ignored) {
            logger.debug("delete Tai-e temp directory fail: {} ({})", root, ignored.toString());
        }
    }

    private static String joinClasspath(List<Path> archives) {
        if (archives == null || archives.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Path archive : archives) {
            if (archive == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(File.pathSeparatorChar);
            }
            sb.append(archive.toAbsolutePath().normalize());
        }
        return sb.toString();
    }

    private static List<Path> safePaths(List<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            return List.of();
        }
        List<Path> out = new ArrayList<>();
        for (Path path : paths) {
            if (path == null) {
                continue;
            }
            Path normalized;
            try {
                normalized = path.toAbsolutePath().normalize();
            } catch (Exception ex) {
                normalized = path.normalize();
            }
            if (!out.contains(normalized)) {
                out.add(normalized);
            }
        }
        return out.isEmpty() ? List.of() : Collections.unmodifiableList(out);
    }

    public enum AnalysisProfile {
        FAST("fast", "1-type"),
        BALANCED("balanced", "2-type"),
        HIGH("high", "2-obj");

        private final String value;
        private final String contextSelector;

        AnalysisProfile(String value, String contextSelector) {
            this.value = value;
            this.contextSelector = contextSelector;
        }

        public static AnalysisProfile fromSystemProperty() {
            String raw = System.getProperty(ANALYSIS_PROFILE_PROP, DEFAULT_ANALYSIS_PROFILE);
            return fromValue(raw);
        }

        public static AnalysisProfile fromValue(String raw) {
            if (raw == null || raw.isBlank()) {
                return BALANCED;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "fast" -> FAST;
                case "high" -> HIGH;
                default -> BALANCED;
            };
        }

        public String value() {
            return value;
        }

        public String contextSelector() {
            return contextSelector;
        }
    }

    private record PreparedInput(List<Path> appArchives,
                                 List<Path> classpathArchives,
                                 int expandedAppArchiveCount,
                                 int addedNestedLibCount) {
    }

    private record PreparedFatArchive(Path classesRoot,
                                      List<Path> nestedLibs) {
    }

    private record AliasStats(int thisVarCount,
                              long aliasChecks,
                              long aliasPairs) {
        private static final AliasStats EMPTY = new AliasStats(0, 0L, 0L);
    }

    private record PtaStats(int entryMethodCount,
                            int reachableMethodCount,
                            int pointsToVarCount,
                            int pointsToObjectCount,
                            int endpointThisVarCount,
                            long endpointMayAliasChecks,
                            long endpointMayAliasPairs) {
        private static final PtaStats EMPTY = new PtaStats(0, 0, 0, 0, 0, 0L, 0L);
    }

    private enum InvokeDynamicFallback {
        NONE("none"),
        INDY_FRONTEND_UNSUPPORTED("indy-frontend-unsupported"),
        INDY_PLUGIN_FAILURE("indy-plugin-failure");

        private final String value;

        InvokeDynamicFallback(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public boolean shouldRetryWithIndyOff() {
            return this != NONE;
        }
    }

    public record TaieRunResult(boolean enabled,
                                boolean success,
                                AnalysisProfile profile,
                                String reason,
                                long elapsedMs,
                                CallGraph<Invoke, JMethod> callGraph,
                                int explicitEntryCount,
                                int entryMethodCount,
                                int reachableMethodCount,
                                int pointsToVarCount,
                                int pointsToObjectCount,
                                int endpointThisVarCount,
                                long endpointMayAliasChecks,
                                long endpointMayAliasPairs,
                                String reflectionInference,
                                String reflectionLog) {
        private static TaieRunResult success(AnalysisProfile profile,
                                             CallGraph<Invoke, JMethod> callGraph,
                                             long elapsedMs,
                                             PtaStats stats,
                                             String reflectionInference,
                                             String reflectionLog,
                                             int explicitEntryCount) {
            PtaStats resolvedStats = stats == null ? PtaStats.EMPTY : stats;
            return new TaieRunResult(
                    true,
                    true,
                    profile,
                    "",
                    elapsedMs,
                    callGraph,
                    Math.max(0, explicitEntryCount),
                    Math.max(0, resolvedStats.entryMethodCount()),
                    Math.max(0, resolvedStats.reachableMethodCount()),
                    Math.max(0, resolvedStats.pointsToVarCount()),
                    Math.max(0, resolvedStats.pointsToObjectCount()),
                    Math.max(0, resolvedStats.endpointThisVarCount()),
                    Math.max(0L, resolvedStats.endpointMayAliasChecks()),
                    Math.max(0L, resolvedStats.endpointMayAliasPairs()),
                    safe(reflectionInference),
                    safe(reflectionLog)
            );
        }

        private static TaieRunResult failed(AnalysisProfile profile,
                                            String reason,
                                            String reflectionInference,
                                            String reflectionLog,
                                            int explicitEntryCount) {
            return new TaieRunResult(
                    true,
                    false,
                    profile,
                    safe(reason),
                    0L,
                    null,
                    Math.max(0, explicitEntryCount),
                    0,
                    0,
                    0,
                    0,
                    0,
                    0L,
                    0L,
                    safe(reflectionInference),
                    safe(reflectionLog)
            );
        }

        private static TaieRunResult disabled(AnalysisProfile profile, String reason) {
            return new TaieRunResult(
                    false,
                    false,
                    profile,
                    safe(reason),
                    0L,
                    null,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0L,
                    0L,
                    "",
                    ""
            );
        }

    }
}
