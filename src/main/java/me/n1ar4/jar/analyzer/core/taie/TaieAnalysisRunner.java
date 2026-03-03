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
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class TaieAnalysisRunner {
    private static final Logger logger = LogManager.getLogger();

    private static final String ANALYSIS_PROFILE_PROP = "jar.analyzer.analysis.profile";
    private static final String DEFAULT_ANALYSIS_PROFILE = "balanced";
    private static final String INVOKEDYNAMIC_ERR = "InvokeDynamic.getMethodRef() is unavailable";
    private static final String MAIN_CLASS_MISSING_ERR = "Main-class has no main method!";
    private static final String FAT_ARCHIVE_DIR = "taie-fat-archives";
    private static final String BOOT_INF_CLASSES_PREFIX = "BOOT-INF/classes/";
    private static final String WEB_INF_CLASSES_PREFIX = "WEB-INF/classes/";
    private static final String BOOT_INF_LIB_PREFIX = "BOOT-INF/lib/";
    private static final String WEB_INF_LIB_PREFIX = "WEB-INF/lib/";

    private TaieAnalysisRunner() {
    }

    public static TaieRunResult run(List<Path> appArchives,
                                    List<Path> classpathArchives,
                                    AnalysisProfile profile) {
        return run(appArchives, classpathArchives, profile, null);
    }

    public static TaieRunResult run(List<Path> appArchives,
                                    List<Path> classpathArchives,
                                    AnalysisProfile profile,
                                    String mainClass) {
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
        PreparedInput preparedInput = prepareInputArchives(app, cp);
        app = preparedInput.appArchives();
        cp = preparedInput.classpathArchives();
        if (preparedInput.expandedAppArchiveCount() > 0 || preparedInput.addedNestedLibCount() > 0) {
            logger.info("normalize fat archives for Tai-e: expandedAppArchives={} addedNestedLibs={}",
                    preparedInput.expandedAppArchiveCount(),
                    preparedInput.addedNestedLibCount());
        }

        Throwable lastError = null;
        String lastReason = "";
        String activeMainClass = mainClass == null ? "" : mainClass.trim();
        boolean retriedWithoutMainClass = false;
        boolean[] invokeDynamicAttempts = {true, false};
        while (true) {
            for (int i = 0; i < invokeDynamicAttempts.length; i++) {
                boolean handleInvokedynamic = invokeDynamicAttempts[i];
                List<String> args = buildArgs(app, cp, resolved, handleInvokedynamic, activeMainClass);
                logger.info("run Tai-e attempt={}/{} profile={} appArchives={} classpathArchives={} mainClass={} invokedynamic={}",
                        i + 1,
                        invokeDynamicAttempts.length,
                        resolved.value,
                        app.size(),
                        cp.size(),
                        activeMainClass.isBlank() ? "<none>" : activeMainClass,
                        handleInvokedynamic ? "on" : "off");

                long start = System.nanoTime();
                try {
                    World.reset();
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
                                long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
                                if (i > 0 || retriedWithoutMainClass) {
                                    logger.warn("tai-e fallback activated: attempt={} invokedynamic={} mainClass={}",
                                            i + 1,
                                            handleInvokedynamic ? "on" : "off",
                                            activeMainClass.isBlank() ? "<none>" : activeMainClass);
                                }
                                return TaieRunResult.success(resolved, callGraph, elapsedMs);
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
                    }
                }

                if (i >= invokeDynamicAttempts.length - 1 || !isInvokeDynamicFailure(lastError, lastReason)) {
                    break;
                }
                logger.warn("tai-e invokedynamic unsupported, retry with invokedynamic=off");
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
        return TaieRunResult.failed(resolved, lastReason);
    }

    public static AnalysisProfile resolveProfile() {
        return AnalysisProfile.fromSystemProperty();
    }

    private static List<String> buildArgs(List<Path> app,
                                          List<Path> cp,
                                          AnalysisProfile profile,
                                          boolean handleInvokedynamic,
                                          String mainClass) {
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
        args.add("pta=" + buildPtaOptions(profile, handleInvokedynamic));
        return args;
    }

    private static String buildPtaOptions(AnalysisProfile profile,
                                          boolean handleInvokedynamic) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("cs:").append(profile.contextSelector)
                .append(";only-app:true;implicit-entries:true;")
                .append("handle-invokedynamic:").append(handleInvokedynamic).append(";");
        sb.append("reflection-inference:string-constant;");
        return sb.toString();
    }

    private static boolean isInvokeDynamicFailure(Throwable error, String reason) {
        if (containsInvokeDynamicError(reason)) {
            return true;
        }
        Throwable cursor = error;
        while (cursor != null) {
            if (containsInvokeDynamicError(cursor.getMessage())) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return error instanceof UnsupportedOperationException;
    }

    private static boolean containsInvokeDynamicError(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.contains(INVOKEDYNAMIC_ERR) || value.contains("InvokeDynamic.getMethodRef()");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static PreparedInput prepareInputArchives(List<Path> appArchives,
                                                      List<Path> classpathArchives) {
        if (appArchives == null || appArchives.isEmpty()) {
            return new PreparedInput(List.of(), List.of(), 0, 0);
        }
        LinkedHashSet<Path> nestedLibs = new LinkedHashSet<>();
        List<Path> preparedApp = new ArrayList<>();
        int expandedAppArchiveCount = 0;
        for (Path appArchive : appArchives) {
            PreparedFatArchive prepared = prepareFatArchive(appArchive);
            if (prepared == null) {
                preparedApp.add(appArchive);
                continue;
            }
            preparedApp.add(appArchive);
            preparedApp.add(prepared.classesRoot());
            expandedAppArchiveCount++;
            if (prepared.nestedLibs() != null && !prepared.nestedLibs().isEmpty()) {
                nestedLibs.addAll(prepared.nestedLibs());
            }
        }

        LinkedHashSet<Path> preparedClasspath = new LinkedHashSet<>();
        if (classpathArchives != null && !classpathArchives.isEmpty()) {
            preparedClasspath.addAll(classpathArchives);
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
                }
            });
        } catch (Exception ignored) {
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

    public record TaieRunResult(boolean enabled,
                                boolean success,
                                AnalysisProfile profile,
                                String reason,
                                long elapsedMs,
                                CallGraph<Invoke, JMethod> callGraph) {
        private static TaieRunResult success(AnalysisProfile profile,
                                             CallGraph<Invoke, JMethod> callGraph,
                                             long elapsedMs) {
            return new TaieRunResult(true, true, profile, "", elapsedMs, callGraph);
        }

        private static TaieRunResult failed(AnalysisProfile profile, String reason) {
            return new TaieRunResult(true, false, profile, safe(reason), 0L, null);
        }

        private static TaieRunResult disabled(AnalysisProfile profile, String reason) {
            return new TaieRunResult(false, false, profile, safe(reason), 0L, null);
        }

    }
}
