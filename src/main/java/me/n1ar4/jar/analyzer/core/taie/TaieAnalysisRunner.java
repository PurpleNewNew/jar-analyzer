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

        List<String> args = buildArgs(app, cp, resolved);
        logger.info("run Tai-e with profile={} appArchives={} classpathArchives={}",
                resolved.value,
                app.size(),
                cp.size());

        long start = System.nanoTime();
        try {
            World.reset();
            Main.main(args.toArray(new String[0]));
            World world = World.get();
            if (world == null) {
                return TaieRunResult.failed(resolved, "tai-e world is null");
            }
            PointerAnalysisResult pointer = world.getResult(PointerAnalysis.ID);
            if (pointer == null) {
                return TaieRunResult.failed(resolved, "pta result is null");
            }
            CallGraph<Invoke, JMethod> callGraph = pointer.getCallGraph();
            if (callGraph == null) {
                return TaieRunResult.failed(resolved, "pta call graph is null");
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            return TaieRunResult.success(resolved, callGraph, elapsedMs);
        } catch (Throwable ex) {
            logger.error("run Tai-e failed: {}", ex.toString(), ex);
            return TaieRunResult.failed(resolved, ex.getMessage());
        } finally {
            try {
                World.reset();
            } catch (Throwable ignored) {
            }
        }
    }

    public static AnalysisProfile resolveProfile() {
        return AnalysisProfile.fromSystemProperty();
    }

    private static List<String> buildArgs(List<Path> app,
                                          List<Path> cp,
                                          AnalysisProfile profile) {
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
        args.add("pta=cs:" + profile.contextSelector + ";only-app:false;implicit-entries:true;handle-invokedynamic:true;");
        return args;
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

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
