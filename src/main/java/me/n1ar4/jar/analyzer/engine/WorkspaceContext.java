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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds workspace paths for headless components that used to read Swing text fields.
 */
public final class WorkspaceContext {
    private static final AtomicReference<ProjectModel> PROJECT_MODEL =
            new AtomicReference<>(ProjectModel.empty());

    private WorkspaceContext() {
    }

    public static void setProjectModel(ProjectModel model) {
        PROJECT_MODEL.set(normalizeModel(model));
    }

    public static ProjectModel getProjectModel() {
        return normalizeModel(PROJECT_MODEL.get());
    }

    public static void ensureArtifactProjectModel(Path inputPath, Path rtJarPath, boolean resolveInnerJars) {
        ProjectModel current = getProjectModel();
        ProjectModel artifact = ProjectModel.artifact(
                normalizePath(inputPath),
                normalizePath(rtJarPath),
                current.analyzedArchives(),
                resolveInnerJars
        );
        PROJECT_MODEL.set(normalizeModel(artifact));
    }

    public static void updateAnalyzedArchives(List<Path> archives) {
        List<Path> normalized = toImmutablePathList(archives);
        ProjectModel current = getProjectModel();
        PROJECT_MODEL.set(new ProjectModel(
                current.buildMode(),
                normalizePath(current.primaryInputPath()),
                normalizePath(current.runtimePath()),
                current.roots(),
                normalized,
                current.resolveInnerJars()
        ));
    }

    public static void updateResolveInnerJars(boolean enabled) {
        ProjectModel current = getProjectModel();
        PROJECT_MODEL.set(new ProjectModel(
                current.buildMode(),
                normalizePath(current.primaryInputPath()),
                normalizePath(current.runtimePath()),
                current.roots(),
                toImmutablePathList(current.analyzedArchives()),
                enabled
        ));
    }

    public static Path primaryInputPath() {
        return normalizePath(getProjectModel().primaryInputPath());
    }

    public static Path runtimePath() {
        return normalizePath(getProjectModel().runtimePath());
    }

    public static List<Path> analyzedArchives() {
        return toImmutablePathList(getProjectModel().analyzedArchives());
    }

    public static boolean resolveInnerJars() {
        return getProjectModel().resolveInnerJars();
    }

    public static void clear() {
        PROJECT_MODEL.set(ProjectModel.empty());
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

    private static ProjectModel normalizeModel(ProjectModel model) {
        if (model == null) {
            return ProjectModel.empty();
        }
        return new ProjectModel(
                model.buildMode(),
                normalizePath(model.primaryInputPath()),
                normalizePath(model.runtimePath()),
                model.roots(),
                toImmutablePathList(model.analyzedArchives()),
                model.resolveInnerJars()
        );
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
