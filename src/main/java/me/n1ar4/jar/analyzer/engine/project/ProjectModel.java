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
        String projectId,
        String projectName,
        ProjectBuildMode buildMode,
        Path primaryInputPath,
        Path runtimePath,
        List<ProjectRoot> roots,
        List<Path> analyzedArchives,
        boolean resolveInnerJars,
        int artifactCatalogVersion,
        String selectedRuntimeProfileId,
        List<ArtifactEntry> artifactEntries
) {
    public ProjectModel {
        projectId = normalizeText(projectId);
        if (projectId.isEmpty()) {
            projectId = "default";
        }
        projectName = normalizeText(projectName);
        if (projectName.isEmpty()) {
            projectName = projectId;
        }
        buildMode = buildMode == null ? ProjectBuildMode.ARTIFACT : buildMode;
        primaryInputPath = normalizeNullablePath(primaryInputPath);
        runtimePath = normalizeNullablePath(runtimePath);
        roots = immutableRoots(roots);
        analyzedArchives = immutablePaths(analyzedArchives);
        artifactCatalogVersion = artifactCatalogVersion <= 0 ? 1 : artifactCatalogVersion;
        selectedRuntimeProfileId = normalizeText(selectedRuntimeProfileId);
        if (selectedRuntimeProfileId.isEmpty()) {
            selectedRuntimeProfileId = "default-runtime";
        }
        artifactEntries = immutableArtifactEntries(artifactEntries);
    }

    public static ProjectModel empty() {
        return new ProjectModel(
                "default",
                "default",
                ProjectBuildMode.ARTIFACT,
                null,
                null,
                List.of(),
                List.of(),
                false,
                1,
                "default-runtime",
                List.of()
        );
    }

    public static ProjectModel artifact(Path inputPath,
                                        Path runtimePath,
                                        List<Path> analyzedArchives,
                                        boolean resolveInnerJars) {
        return artifact(
                "default",
                "default",
                inputPath,
                runtimePath,
                analyzedArchives,
                resolveInnerJars,
                "default-runtime"
        );
    }

    public static ProjectModel artifact(String projectId,
                                        String projectName,
                                        Path inputPath,
                                        Path runtimePath,
                                        List<Path> analyzedArchives,
                                        boolean resolveInnerJars,
                                        String selectedRuntimeProfileId) {
        Builder builder = builder()
                .projectId(projectId)
                .projectName(projectName)
                .buildMode(ProjectBuildMode.ARTIFACT)
                .primaryInputPath(inputPath)
                .runtimePath(runtimePath)
                .resolveInnerJars(resolveInnerJars)
                .analyzedArchives(analyzedArchives)
                .selectedRuntimeProfileId(selectedRuntimeProfileId);
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
            builder.addArtifactEntry(new ArtifactEntry(
                    ArtifactRole.INPUT,
                    ArtifactIndexPolicy.INDEX_FULL,
                    ProjectOrigin.APP,
                    inputPath.getFileName() == null ? inputPath.toString() : inputPath.getFileName().toString(),
                    "",
                    inputPath
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
            builder.addArtifactEntry(new ArtifactEntry(
                    ArtifactRole.DEPENDENCY,
                    ArtifactIndexPolicy.BRIDGE_ONLY,
                    ProjectOrigin.SDK,
                    runtimePath.getFileName() == null ? runtimePath.toString() : runtimePath.getFileName().toString(),
                    "",
                    runtimePath
            ));
        }
        if (analyzedArchives != null && !analyzedArchives.isEmpty()) {
            for (Path archive : analyzedArchives) {
                Path normalized = normalizeNullablePath(archive);
                if (normalized == null || Objects.equals(normalized, normalizeNullablePath(inputPath))) {
                    continue;
                }
                builder.addArtifactEntry(new ArtifactEntry(
                        ArtifactRole.DEPENDENCY,
                        ArtifactIndexPolicy.BRIDGE_ONLY,
                        ProjectOrigin.LIBRARY,
                        normalized.getFileName() == null ? normalized.toString() : normalized.getFileName().toString(),
                        "",
                        normalized
                ));
            }
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

    private static List<ArtifactEntry> immutableArtifactEntries(List<ArtifactEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        List<ArtifactEntry> out = new ArrayList<>();
        for (ArtifactEntry entry : entries) {
            if (entry != null) {
                out.add(entry);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
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

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Builder {
        private String projectId = "default";
        private String projectName = "default";
        private ProjectBuildMode buildMode = ProjectBuildMode.ARTIFACT;
        private Path primaryInputPath;
        private Path runtimePath;
        private final List<ProjectRoot> roots = new ArrayList<>();
        private final List<Path> analyzedArchives = new ArrayList<>();
        private boolean resolveInnerJars;
        private int artifactCatalogVersion = 1;
        private String selectedRuntimeProfileId = "default-runtime";
        private final List<ArtifactEntry> artifactEntries = new ArrayList<>();

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

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

        public Builder artifactCatalogVersion(int version) {
            this.artifactCatalogVersion = version;
            return this;
        }

        public Builder selectedRuntimeProfileId(String selectedRuntimeProfileId) {
            this.selectedRuntimeProfileId = selectedRuntimeProfileId;
            return this;
        }

        public Builder addArtifactEntry(ArtifactEntry entry) {
            if (entry != null) {
                artifactEntries.add(entry);
            }
            return this;
        }

        public Builder artifactEntries(List<ArtifactEntry> entries) {
            artifactEntries.clear();
            if (entries != null && !entries.isEmpty()) {
                for (ArtifactEntry entry : entries) {
                    addArtifactEntry(entry);
                }
            }
            return this;
        }

        public ProjectModel build() {
            return new ProjectModel(
                    projectId,
                    projectName,
                    Objects.requireNonNullElse(buildMode, ProjectBuildMode.ARTIFACT),
                    primaryInputPath,
                    runtimePath,
                    roots,
                    analyzedArchives,
                    resolveInnerJars,
                    artifactCatalogVersion,
                    selectedRuntimeProfileId,
                    artifactEntries
            );
        }
    }
}
