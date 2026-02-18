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

import me.n1ar4.jar.analyzer.engine.project.ProjectBuildMode;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.project.ProjectRoot;
import me.n1ar4.jar.analyzer.engine.project.ProjectRootKind;
import me.n1ar4.jar.analyzer.meta.CompatibilityCode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds workspace paths for headless components that used to read Swing text fields.
 */
public final class WorkspaceContext {
    private static final AtomicReference<Path> INPUT_PATH = new AtomicReference<>();
    private static final AtomicReference<Path> RUNTIME_JAR_PATH = new AtomicReference<>();
    private static final AtomicReference<List<Path>> ANALYZED_ARCHIVES =
            new AtomicReference<>(Collections.emptyList());
    private static final AtomicBoolean RESOLVE_INNER_JARS = new AtomicBoolean(false);
    private static final AtomicReference<ProjectModel> PROJECT_MODEL =
            new AtomicReference<>(ProjectModel.empty());
    private static final AtomicBoolean EXPLICIT_PROJECT_MODEL = new AtomicBoolean(false);

    private WorkspaceContext() {
    }

    public static void setProjectModel(ProjectModel model) {
        ProjectModel safeModel = model == null ? ProjectModel.empty() : model;
        PROJECT_MODEL.set(safeModel);
        EXPLICIT_PROJECT_MODEL.set(true);
        syncLegacyFieldsFromModel(safeModel);
    }

    public static ProjectModel getProjectModel() {
        ProjectModel model = PROJECT_MODEL.get();
        return model == null ? ProjectModel.empty() : model;
    }

    public static void ensureArtifactProjectModel(Path inputPath, Path rtJarPath, boolean resolveInnerJars) {
        INPUT_PATH.set(normalizePath(inputPath));
        RUNTIME_JAR_PATH.set(normalizePath(rtJarPath));
        RESOLVE_INNER_JARS.set(resolveInnerJars);
        EXPLICIT_PROJECT_MODEL.set(false);
        refreshDerivedProjectModel();
    }

    @CompatibilityCode(
            primary = "WorkspaceContext#setProjectModel",
            reason = "Legacy callers still set input/runtime paths directly"
    )
    public static void setInputPath(Path inputPath) {
        INPUT_PATH.set(normalizePath(inputPath));
        refreshDerivedProjectModel();
    }

    @CompatibilityCode(
            primary = "WorkspaceContext#getProjectModel",
            reason = "Legacy analysis path still reads a single input path"
    )
    public static Path getInputPath() {
        return INPUT_PATH.get();
    }

    @CompatibilityCode(
            primary = "WorkspaceContext#setProjectModel",
            reason = "Legacy callers still set runtime path directly"
    )
    public static void setRuntimeJarPath(Path rtJarPath) {
        RUNTIME_JAR_PATH.set(normalizePath(rtJarPath));
        refreshDerivedProjectModel();
    }

    @CompatibilityCode(
            primary = "WorkspaceContext#getProjectModel",
            reason = "Legacy runtime resolver still expects a single runtime path"
    )
    public static Path getRuntimeJarPath() {
        return RUNTIME_JAR_PATH.get();
    }

    @CompatibilityCode(
            primary = "WorkspaceContext#setProjectModel",
            reason = "Legacy pipeline writes analyzed archive list independently"
    )
    public static void setAnalyzedArchives(List<Path> archives) {
        List<Path> normalized = toImmutablePathList(archives);
        ANALYZED_ARCHIVES.set(normalized);
        ProjectModel current = PROJECT_MODEL.get();
        if (EXPLICIT_PROJECT_MODEL.get()
                && current != null
                && current.buildMode() == ProjectBuildMode.PROJECT) {
            PROJECT_MODEL.set(new ProjectModel(
                    current.buildMode(),
                    current.primaryInputPath(),
                    current.runtimePath(),
                    current.roots(),
                    normalized,
                    current.resolveInnerJars()
            ));
            return;
        }
        refreshDerivedProjectModel();
    }

    @CompatibilityCode(
            primary = "WorkspaceContext#getProjectModel",
            reason = "Legacy classpath resolver reads archive list directly"
    )
    public static List<Path> getAnalyzedArchives() {
        List<Path> value = ANALYZED_ARCHIVES.get();
        return value == null ? Collections.emptyList() : value;
    }

    @CompatibilityCode(
            primary = "WorkspaceContext#setProjectModel",
            reason = "Legacy nested-jar option is still wired as a standalone flag"
    )
    public static void setResolveInnerJars(boolean enabled) {
        RESOLVE_INNER_JARS.set(enabled);
        ProjectModel current = PROJECT_MODEL.get();
        if (EXPLICIT_PROJECT_MODEL.get()
                && current != null
                && current.buildMode() == ProjectBuildMode.PROJECT) {
            PROJECT_MODEL.set(new ProjectModel(
                    current.buildMode(),
                    current.primaryInputPath(),
                    current.runtimePath(),
                    current.roots(),
                    current.analyzedArchives(),
                    enabled
            ));
            return;
        }
        refreshDerivedProjectModel();
    }

    @CompatibilityCode(
            primary = "WorkspaceContext#getProjectModel",
            reason = "Legacy nested-jar resolver reads this flag directly"
    )
    public static boolean isResolveInnerJars() {
        return RESOLVE_INNER_JARS.get();
    }

    public static void clear() {
        INPUT_PATH.set(null);
        RUNTIME_JAR_PATH.set(null);
        ANALYZED_ARCHIVES.set(Collections.emptyList());
        RESOLVE_INNER_JARS.set(false);
        PROJECT_MODEL.set(ProjectModel.empty());
        EXPLICIT_PROJECT_MODEL.set(false);
    }

    private static void syncLegacyFieldsFromModel(ProjectModel model) {
        INPUT_PATH.set(resolvePrimaryInput(model));
        RUNTIME_JAR_PATH.set(resolveRuntimePath(model));
        ANALYZED_ARCHIVES.set(toImmutablePathList(model == null ? null : model.analyzedArchives()));
        RESOLVE_INNER_JARS.set(model != null && model.resolveInnerJars());
    }

    private static Path resolvePrimaryInput(ProjectModel model) {
        if (model == null) {
            return null;
        }
        if (model.primaryInputPath() != null) {
            return model.primaryInputPath();
        }
        for (ProjectRoot root : model.roots()) {
            if (root == null) {
                continue;
            }
            ProjectRootKind kind = root.kind();
            if (kind == ProjectRootKind.CONTENT_ROOT || kind == ProjectRootKind.SOURCE_ROOT) {
                return root.path();
            }
        }
        return null;
    }

    private static Path resolveRuntimePath(ProjectModel model) {
        if (model == null) {
            return null;
        }
        if (model.runtimePath() != null) {
            return model.runtimePath();
        }
        for (ProjectRoot root : model.roots()) {
            if (root != null && root.kind() == ProjectRootKind.SDK) {
                return root.path();
            }
        }
        return null;
    }

    private static void refreshDerivedProjectModel() {
        ProjectModel current = PROJECT_MODEL.get();
        if (EXPLICIT_PROJECT_MODEL.get()
                && current != null
                && current.buildMode() == ProjectBuildMode.PROJECT) {
            return;
        }
        PROJECT_MODEL.set(ProjectModel.artifact(
                INPUT_PATH.get(),
                RUNTIME_JAR_PATH.get(),
                ANALYZED_ARCHIVES.get(),
                RESOLVE_INNER_JARS.get()
        ));
    }

    private static List<Path> toImmutablePathList(List<Path> archives) {
        if (archives == null || archives.isEmpty()) {
            return Collections.emptyList();
        }
        List<Path> out = new ArrayList<>(archives.size());
        for (Path archive : archives) {
            Path normalized = normalizePath(archive);
            if (normalized != null) {
                out.add(normalized);
            }
        }
        return out.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(out);
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
}
