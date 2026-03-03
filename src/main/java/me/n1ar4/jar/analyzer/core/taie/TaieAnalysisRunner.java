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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class TaieAnalysisRunner {
    private static final Logger logger = LogManager.getLogger();

    private static final String ANALYSIS_PROFILE_PROP = "jar.analyzer.analysis.profile";
    private static final String DEFAULT_ANALYSIS_PROFILE = "balanced";
    private static final String INVOKEDYNAMIC_MODE_PROP = "jar.analyzer.taie.invokedynamic";
    private static final String INVOKEDYNAMIC_ERR = "InvokeDynamic.getMethodRef() is unavailable";

    private TaieAnalysisRunner() {
    }

    public static TaieRunResult run(List<Path> appArchives,
                                    List<Path> classpathArchives,
                                    AnalysisProfile profile) {
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

        InvokeDynamicMode invokeDynamicMode = InvokeDynamicMode.fromSystemProperty();
        List<RunAttempt> attempts = buildAttempts(invokeDynamicMode);
        Throwable lastError = null;
        String lastReason = "";
        for (int i = 0; i < attempts.size(); i++) {
            RunAttempt attempt = attempts.get(i);
            List<String> args = buildArgs(app, cp, resolved, attempt.handleInvokedynamic(), attempt.disableReflectionInference());
            logger.info("run Tai-e attempt={}/{} profile={} appArchives={} classpathArchives={} invokedynamic={} reflection={}",
                    i + 1,
                    attempts.size(),
                    resolved.value,
                    app.size(),
                    cp.size(),
                    attempt.handleInvokedynamic() ? "on" : "off",
                    attempt.disableReflectionInference() ? "off" : "string-constant");

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
                            if (i > 0) {
                                logger.warn("tai-e fallback activated: attempt={} invokedynamic={} reflection={}",
                                        i + 1,
                                        attempt.handleInvokedynamic() ? "on" : "off",
                                        attempt.disableReflectionInference() ? "off" : "string-constant");
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

            if (!canRetry(attempts, i, lastError, lastReason)) {
                break;
            }
        }
        if (lastError != null) {
            logger.error("run Tai-e failed: {}", lastError.toString(), lastError);
        }
        return TaieRunResult.failed(resolved, lastReason);
    }

    public static AnalysisProfile resolveProfile() {
        return AnalysisProfile.fromSystemProperty();
    }

    public static InvokeDynamicMode resolveInvokeDynamicMode() {
        return InvokeDynamicMode.fromSystemProperty();
    }

    private static List<String> buildArgs(List<Path> app,
                                          List<Path> cp,
                                          AnalysisProfile profile,
                                          boolean handleInvokedynamic,
                                          boolean disableReflectionInference) {
        String appClasspath = joinClasspath(app);
        String fullClasspath = joinClasspath(cp);
        Path outputDir = Path.of(Const.tempDir, "taie-output").toAbsolutePath().normalize();

        List<String> args = new ArrayList<>();
        args.add("-acp");
        args.add(appClasspath);
        args.add("-cp");
        args.add(fullClasspath);
        args.add("-pp");
        args.add("-ap");
        args.add("-scope");
        args.add("APP");
        args.add("--output-dir");
        args.add(outputDir.toString());
        args.add("-a");
        args.add("pta=" + buildPtaOptions(profile, handleInvokedynamic, disableReflectionInference));
        return args;
    }

    private static String buildPtaOptions(AnalysisProfile profile,
                                          boolean handleInvokedynamic,
                                          boolean disableReflectionInference) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("cs:").append(profile.contextSelector)
                .append(";only-app:false;implicit-entries:true;")
                .append("handle-invokedynamic:").append(handleInvokedynamic).append(";");
        if (disableReflectionInference) {
            sb.append("reflection-inference:null;");
        } else {
            sb.append("reflection-inference:string-constant;");
        }
        return sb.toString();
    }

    private static List<RunAttempt> buildAttempts(InvokeDynamicMode mode) {
        if (mode == InvokeDynamicMode.ON) {
            return List.of(new RunAttempt(true, false));
        }
        if (mode == InvokeDynamicMode.OFF) {
            return List.of(new RunAttempt(false, false));
        }
        return List.of(
                new RunAttempt(true, false),
                new RunAttempt(true, true),
                new RunAttempt(false, false)
        );
    }

    private static boolean canRetry(List<RunAttempt> attempts,
                                    int attemptIndex,
                                    Throwable error,
                                    String reason) {
        if (attempts == null || attempts.isEmpty()) {
            return false;
        }
        if (attemptIndex >= attempts.size() - 1) {
            return false;
        }
        if (attemptIndex == 0) {
            return isInvokeDynamicFailure(error, reason);
        }
        return true;
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

    public enum InvokeDynamicMode {
        AUTO("auto"),
        ON("on"),
        OFF("off");

        private final String value;

        InvokeDynamicMode(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static InvokeDynamicMode fromSystemProperty() {
            return fromValue(System.getProperty(INVOKEDYNAMIC_MODE_PROP, AUTO.value));
        }

        public static InvokeDynamicMode fromValue(String raw) {
            if (raw == null || raw.isBlank()) {
                return AUTO;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "on", "true", "enable", "enabled" -> ON;
                case "off", "false", "disable", "disabled" -> OFF;
                default -> AUTO;
            };
        }
    }

    private record RunAttempt(boolean handleInvokedynamic,
                              boolean disableReflectionInference) {
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
