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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Unified workspace/project model for build, tree and search.
 */
public record ProjectModel(
        ProjectBuildMode buildMode,
        Path primaryInputPath,
        Path runtimePath,
        List<ProjectRoot> roots,
        List<Path> analyzedArchives,
        boolean resolveInnerJars
) {
    public ProjectModel {
        buildMode = buildMode == null ? ProjectBuildMode.ARTIFACT : buildMode;
        primaryInputPath = normalizeNullablePath(primaryInputPath);
        runtimePath = normalizeNullablePath(runtimePath);
        roots = immutableRoots(roots);
        analyzedArchives = immutablePaths(analyzedArchives);
    }

    public static ProjectModel empty() {
        return new ProjectModel(
                ProjectBuildMode.ARTIFACT,
                null,
                null,
                List.of(),
                List.of(),
                false
        );
    }

    public static ProjectModel artifact(Path inputPath,
                                        Path runtimePath,
                                        List<Path> analyzedArchives,
                                        boolean resolveInnerJars) {
        Builder builder = builder()
                .buildMode(ProjectBuildMode.ARTIFACT)
                .primaryInputPath(inputPath)
                .runtimePath(runtimePath)
                .resolveInnerJars(resolveInnerJars)
                .analyzedArchives(analyzedArchives);
        if (inputPath != null) {
            builder.addRoot(new ProjectRoot(
                    ProjectRootKind.CONTENT_ROOT,
                    ProjectOrigin.APP,
                    inputPath,
                    "",
                    isArchiveLike(inputPath),
                    false,
                    10
            ));
        }
        if (runtimePath != null) {
            builder.addRoot(new ProjectRoot(
                    ProjectRootKind.SDK,
                    ProjectOrigin.SDK,
                    runtimePath,
                    "",
                    isArchiveLike(runtimePath),
                    false,
                    20
            ));
        }
        return builder.build();
    }

    public List<ProjectRoot> rootsByKind(ProjectRootKind kind) {
        if (kind == null || roots.isEmpty()) {
            return List.of();
        }
        List<ProjectRoot> out = new ArrayList<>();
        for (ProjectRoot root : roots) {
            if (root != null && root.kind() == kind) {
                out.add(root);
            }
        }
        return out;
    }

    public List<ProjectRoot> rootsByOrigin(ProjectOrigin origin) {
        if (origin == null || roots.isEmpty()) {
            return List.of();
        }
        List<ProjectRoot> out = new ArrayList<>();
        for (ProjectRoot root : roots) {
            if (root != null && root.origin() == origin) {
                out.add(root);
            }
        }
        return out;
    }

    public boolean hasAnyKind(ProjectRootKind... kinds) {
        if (roots.isEmpty() || kinds == null || kinds.length == 0) {
            return false;
        }
        EnumSet<ProjectRootKind> wanted = EnumSet.noneOf(ProjectRootKind.class);
        Collections.addAll(wanted, kinds);
        for (ProjectRoot root : roots) {
            if (root != null && wanted.contains(root.kind())) {
                return true;
            }
        }
        return false;
    }

    public static Builder builder() {
        return new Builder();
    }

    private static boolean isArchiveLike(Path path) {
        if (path == null) {
            return false;
        }
        String value = path.getFileName() == null ? path.toString() : path.getFileName().toString();
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jar")
                || lower.endsWith(".war")
                || lower.endsWith(".zip")
                || lower.endsWith(".jmod");
    }

    private static List<ProjectRoot> immutableRoots(List<ProjectRoot> roots) {
        if (roots == null || roots.isEmpty()) {
            return List.of();
        }
        List<ProjectRoot> out = new ArrayList<>();
        for (ProjectRoot root : roots) {
            if (root != null) {
                out.add(root);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static List<Path> immutablePaths(List<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            return List.of();
        }
        List<Path> out = new ArrayList<>();
        for (Path path : paths) {
            Path normalized = normalizeNullablePath(path);
            if (normalized != null) {
                out.add(normalized);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static Path normalizeNullablePath(Path path) {
        if (path == null) {
            return null;
        }
        try {
            return path.toAbsolutePath().normalize();
        } catch (Exception ex) {
            return path.normalize();
        }
    }

    public static final class Builder {
        private ProjectBuildMode buildMode = ProjectBuildMode.ARTIFACT;
        private Path primaryInputPath;
        private Path runtimePath;
        private final List<ProjectRoot> roots = new ArrayList<>();
        private final List<Path> analyzedArchives = new ArrayList<>();
        private boolean resolveInnerJars;

        public Builder buildMode(ProjectBuildMode buildMode) {
            this.buildMode = buildMode == null ? ProjectBuildMode.ARTIFACT : buildMode;
            return this;
        }

        public Builder primaryInputPath(Path primaryInputPath) {
            this.primaryInputPath = primaryInputPath;
            return this;
        }

        public Builder runtimePath(Path runtimePath) {
            this.runtimePath = runtimePath;
            return this;
        }

        public Builder addRoot(ProjectRoot root) {
            if (root != null) {
                roots.add(root);
            }
            return this;
        }

        public Builder roots(List<ProjectRoot> roots) {
            this.roots.clear();
            if (roots != null && !roots.isEmpty()) {
                for (ProjectRoot root : roots) {
                    addRoot(root);
                }
            }
            return this;
        }

        public Builder addAnalyzedArchive(Path archive) {
            if (archive != null) {
                analyzedArchives.add(archive);
            }
            return this;
        }

        public Builder analyzedArchives(List<Path> archives) {
            this.analyzedArchives.clear();
            if (archives != null && !archives.isEmpty()) {
                for (Path archive : archives) {
                    addAnalyzedArchive(archive);
                }
            }
            return this;
        }

        public Builder resolveInnerJars(boolean resolveInnerJars) {
            this.resolveInnerJars = resolveInnerJars;
            return this;
        }

        public ProjectModel build() {
            return new ProjectModel(
                    Objects.requireNonNullElse(buildMode, ProjectBuildMode.ARTIFACT),
                    primaryInputPath,
                    runtimePath,
                    roots,
                    analyzedArchives,
                    resolveInnerJars
            );
        }
    }
}
