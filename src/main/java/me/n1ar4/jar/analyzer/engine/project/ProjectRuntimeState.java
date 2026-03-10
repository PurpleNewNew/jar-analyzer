/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.engine.project;

import me.n1ar4.jar.analyzer.utils.ProjectPathNormalizer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable runtime state for the active project.
 */
public record ProjectRuntimeState(
        String projectKey,
        long buildSeq,
        ProjectModel projectModel
) {
    public ProjectRuntimeState {
        projectKey = projectKey == null ? "" : projectKey.trim();
        buildSeq = Math.max(0L, buildSeq);
        projectModel = normalizeModel(projectModel);
    }

    public static ProjectRuntimeState empty() {
        return new ProjectRuntimeState("", 0L, ProjectModel.empty());
    }

    public Path primaryInputPath() {
        return normalizePath(projectModel.primaryInputPath());
    }

    public Path runtimePath() {
        return normalizePath(projectModel.runtimePath());
    }

    public List<Path> analyzedArchives() {
        return toImmutablePathList(projectModel.analyzedArchives());
    }

    public boolean resolveInnerJars() {
        return projectModel.resolveInnerJars();
    }

    public String cacheKey() {
        return projectKey + "|" + buildSeq + "|" + projectModel;
    }

    public ProjectRuntimeState withProjectModel(ProjectModel model) {
        return new ProjectRuntimeState(projectKey, buildSeq, model);
    }

    public ProjectRuntimeState withAnalyzedArchives(List<Path> archives) {
        ProjectModel current = projectModel == null ? ProjectModel.empty() : projectModel;
        return new ProjectRuntimeState(projectKey, buildSeq, new ProjectModel(
                current.buildMode(),
                normalizePath(current.primaryInputPath()),
                normalizePath(current.runtimePath()),
                current.roots(),
                toImmutablePathList(archives),
                current.resolveInnerJars()
        ));
    }

    public ProjectRuntimeState withResolveInnerJars(boolean enabled) {
        ProjectModel current = projectModel == null ? ProjectModel.empty() : projectModel;
        return new ProjectRuntimeState(projectKey, buildSeq, new ProjectModel(
                current.buildMode(),
                normalizePath(current.primaryInputPath()),
                normalizePath(current.runtimePath()),
                current.roots(),
                toImmutablePathList(current.analyzedArchives()),
                enabled
        ));
    }

    public ProjectRuntimeState ensureArtifactProjectModel(Path inputPath, Path rtJarPath, boolean resolveInnerJars) {
        ProjectModel current = projectModel == null ? ProjectModel.empty() : projectModel;
        ProjectModel artifact = ProjectModel.artifact(
                normalizePath(inputPath),
                normalizePath(rtJarPath),
                current.analyzedArchives(),
                resolveInnerJars
        );
        return new ProjectRuntimeState(projectKey, buildSeq, artifact);
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
        return ProjectPathNormalizer.normalizeNullablePath(path);
    }
}
