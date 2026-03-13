/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.engine;

import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.project.ProjectBuildMode;
import me.n1ar4.jar.analyzer.engine.project.ProjectRuntimeState;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Single runtime state source for the active project.
 */
public final class ProjectRuntimeContext {
    private static final AtomicReference<ProjectRuntimeState> STATE =
            new AtomicReference<>(ProjectRuntimeState.empty());
    private static final ThreadLocal<ProjectRuntimeState> OVERRIDE_STATE = new ThreadLocal<>();
    private static final AtomicLong STATE_VERSION = new AtomicLong(0L);

    private ProjectRuntimeContext() {
    }

    public static ProjectRuntimeState getState() {
        ProjectRuntimeState override = OVERRIDE_STATE.get();
        return override == null ? STATE.get() : override;
    }

    private static void setState(ProjectRuntimeState state) {
        ProjectRuntimeState next = state == null ? ProjectRuntimeState.empty() : state;
        ProjectRuntimeState previous = STATE.getAndSet(next);
        if (!next.equals(previous)) {
            STATE_VERSION.incrementAndGet();
        }
    }

    public static void restoreProjectRuntime(String projectKey, long buildSeq, ProjectModel projectModel) {
        setState(new ProjectRuntimeState(projectKey, buildSeq, projectModel));
    }

    public static void replaceProjectModel(ProjectModel projectModel) {
        updateState(currentState -> currentState.withProjectModel(projectModel));
    }

    public static ProjectModel getProjectModel() {
        return getState().projectModel();
    }

    public static void prepareArtifactBuild(Path inputPath,
                                            Path rtJarPath,
                                            List<Path> archives,
                                            boolean resolveInnerJars,
                                            String jdkModules) {
        updateState(currentState -> {
            ProjectRuntimeState next = currentState;
            ProjectModel currentModel = currentState == null ? null : currentState.projectModel();
            boolean explicitProjectMode = currentModel != null
                    && currentModel.buildMode() == ProjectBuildMode.PROJECT;
            if (!explicitProjectMode) {
                Path effectiveInput = inputPath == null ? next.primaryInputPath() : inputPath;
                Path effectiveRuntime = rtJarPath == null ? next.runtimePath() : rtJarPath;
                String effectiveModules = jdkModules;
                if ((effectiveModules == null || effectiveModules.isBlank()) && currentModel != null) {
                    effectiveModules = currentModel.jdkModules();
                }
                next = next.ensureArtifactProjectModel(effectiveInput, effectiveRuntime, resolveInnerJars, effectiveModules);
            }
            if (archives == null) {
                return next;
            }
            return next.withAnalyzedArchives(archives);
        });
    }

    public static void updateResolveInnerJars(boolean enabled) {
        updateState(currentState -> currentState.withResolveInnerJars(enabled));
    }

    public static String projectKey() {
        return getState().projectKey();
    }

    public static long buildSeq() {
        return getState().buildSeq();
    }

    public static Path primaryInputPath() {
        return getState().primaryInputPath();
    }

    public static Path runtimePath() {
        return getState().runtimePath();
    }

    public static List<Path> analyzedArchives() {
        return getState().analyzedArchives();
    }

    public static boolean resolveInnerJars() {
        return getState().resolveInnerJars();
    }

    public static String jdkModules() {
        ProjectModel model = getProjectModel();
        return model == null ? "" : model.jdkModules();
    }

    public static void clear() {
        setState(ProjectRuntimeState.empty());
    }

    public static long stateVersion() {
        return STATE_VERSION.get();
    }

    private static void updateState(UnaryOperator<ProjectRuntimeState> updater) {
        if (updater == null) {
            return;
        }
        while (true) {
            ProjectRuntimeState current = STATE.get();
            ProjectRuntimeState next = updater.apply(current);
            if (next == null) {
                next = ProjectRuntimeState.empty();
            }
            if (STATE.compareAndSet(current, next)) {
                if (!next.equals(current)) {
                    STATE_VERSION.incrementAndGet();
                }
                return;
            }
        }
    }

    public static <T> T withState(ProjectRuntimeState state, Supplier<T> supplier) {
        if (supplier == null) {
            return null;
        }
        ProjectRuntimeState previous = OVERRIDE_STATE.get();
        if (state == null) {
            OVERRIDE_STATE.remove();
        } else {
            OVERRIDE_STATE.set(state);
        }
        try {
            return supplier.get();
        } finally {
            if (previous == null) {
                OVERRIDE_STATE.remove();
            } else {
                OVERRIDE_STATE.set(previous);
            }
        }
    }
}
