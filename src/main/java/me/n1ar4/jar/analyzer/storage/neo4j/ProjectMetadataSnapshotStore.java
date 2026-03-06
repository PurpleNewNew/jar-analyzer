/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.storage.neo4j;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class ProjectMetadataSnapshotStore {
    private static final Logger logger = LogManager.getLogger();
    private static final ProjectMetadataSnapshotStore INSTANCE = new ProjectMetadataSnapshotStore();
    private static final String SNAPSHOT_FILE = "runtime-metadata.json";
    private static final String ASSET_DIR = "runtime-assets";
    private static final String CLASS_ASSET_DIR = "classes";
    private static final String RESOURCE_ASSET_DIR = "resources";

    private ProjectMetadataSnapshotStore() {
    }

    public static ProjectMetadataSnapshotStore getInstance() {
        return INSTANCE;
    }

    void write(String projectKey, ProjectRuntimeSnapshot snapshot) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        Path home = Neo4jProjectStore.getInstance().resolveProjectHome(normalized);
        writeToHome(home, home, snapshot);
    }

    public void writeToHome(Path projectHome, ProjectRuntimeSnapshot snapshot) {
        writeToHome(projectHome, projectHome, snapshot);
    }

    public void writeToHome(Path assetHome, Path persistedHome, ProjectRuntimeSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("project_runtime_snapshot_missing");
        }
        Path target = resolveSnapshotFile(assetHome);
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.createDirectories(target.getParent());
            ProjectRuntimeSnapshot persisted = materializeSnapshotAssets(assetHome, persistedHome, snapshot);
            String json = JSON.toJSONString(persisted);
            Files.writeString(temp, json, StandardCharsets.UTF_8);
            try {
                Files.move(temp, target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception ex) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ex) {
            try {
                Files.deleteIfExists(temp);
            } catch (Exception ignored) {
                // best effort
            }
            logger.error("persist project runtime snapshot fail: home={} err={}", assetHome, ex.toString(), ex);
            throw new IllegalStateException("project_runtime_snapshot_write_failed", ex);
        }
    }

    public boolean restoreIntoRuntime(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        ProjectRuntimeSnapshot snapshot = read(normalized);
        if (snapshot == null) {
            DatabaseManager.clearAllData();
            WorkspaceContext.clear();
            return false;
        }
        DatabaseManager.restoreProjectRuntime(snapshot);
        ProjectModel model = DatabaseManager.getProjectModel();
        WorkspaceContext.setProjectModel(model == null ? ProjectModel.empty() : model);
        return true;
    }

    private ProjectRuntimeSnapshot materializeSnapshotAssets(Path assetHome,
                                                            Path persistedHome,
                                                            ProjectRuntimeSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        Path effectiveAssetHome = requireHome(assetHome);
        Path effectivePersistedHome = requireHome(persistedHome);
        List<ProjectRuntimeSnapshot.ClassFileData> classFiles =
                materializeClassFiles(snapshot.classFiles(), effectiveAssetHome, effectivePersistedHome);
        List<ProjectRuntimeSnapshot.ResourceData> resources =
                materializeResources(snapshot.resources(), effectiveAssetHome, effectivePersistedHome);
        return new ProjectRuntimeSnapshot(
                snapshot.schemaVersion(),
                snapshot.buildSeq(),
                snapshot.projectModel(),
                snapshot.jars(),
                classFiles,
                snapshot.classReferences(),
                snapshot.methodReferences(),
                snapshot.methodStrings(),
                snapshot.methodAnnoStrings(),
                resources,
                snapshot.callSites(),
                snapshot.localVars(),
                snapshot.springControllers(),
                snapshot.springInterceptors(),
                snapshot.servlets(),
                snapshot.filters(),
                snapshot.listeners()
        );
    }

    private List<ProjectRuntimeSnapshot.ClassFileData> materializeClassFiles(
            List<ProjectRuntimeSnapshot.ClassFileData> rows,
            Path assetHome,
            Path persistedHome) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<ProjectRuntimeSnapshot.ClassFileData> out = new ArrayList<>(rows.size());
        for (ProjectRuntimeSnapshot.ClassFileData row : rows) {
            if (row == null) {
                continue;
            }
            String path = materializePath(
                    row.pathStr(),
                    assetHome.resolve(ASSET_DIR).resolve(CLASS_ASSET_DIR),
                    persistedHome.resolve(ASSET_DIR).resolve(CLASS_ASSET_DIR),
                    buildClassAssetRelativePath(row)
            );
            out.add(new ProjectRuntimeSnapshot.ClassFileData(
                    row.cfId(),
                    row.className(),
                    path,
                    row.jarName(),
                    row.jarId()
            ));
        }
        return out;
    }

    private List<ProjectRuntimeSnapshot.ResourceData> materializeResources(
            List<ProjectRuntimeSnapshot.ResourceData> rows,
            Path assetHome,
            Path persistedHome) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<ProjectRuntimeSnapshot.ResourceData> out = new ArrayList<>(rows.size());
        for (ProjectRuntimeSnapshot.ResourceData row : rows) {
            if (row == null) {
                continue;
            }
            String path = materializePath(
                    row.pathStr(),
                    assetHome.resolve(ASSET_DIR).resolve(RESOURCE_ASSET_DIR),
                    persistedHome.resolve(ASSET_DIR).resolve(RESOURCE_ASSET_DIR),
                    buildResourceAssetRelativePath(row)
            );
            out.add(new ProjectRuntimeSnapshot.ResourceData(
                    row.rid(),
                    row.resourcePath(),
                    path,
                    row.jarName(),
                    row.jarId(),
                    row.fileSize(),
                    row.isText()
            ));
        }
        return out;
    }

    private String materializePath(String rawSourcePath,
                                   Path assetRoot,
                                   Path persistedRoot,
                                   Path relativeTarget) {
        String sourcePath = safe(rawSourcePath);
        if (sourcePath.isBlank() || !shouldMaterialize(sourcePath)) {
            return sourcePath;
        }
        try {
            Path source = Paths.get(sourcePath).toAbsolutePath().normalize();
            if (!Files.exists(source) || !Files.isRegularFile(source)) {
                logger.warn("skip missing runtime asset during snapshot persist: {}", source);
                return sourcePath;
            }
            Path target = safeResolve(assetRoot, relativeTarget);
            Files.createDirectories(target.getParent());
            Files.copy(source, target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
            Path persisted = safeResolve(persistedRoot, relativeTarget);
            return persisted.toAbsolutePath().normalize().toString();
        } catch (Exception ex) {
            logger.warn("materialize runtime asset fail: {} ({})", sourcePath, ex.toString());
            return sourcePath;
        }
    }

    private static Path buildClassAssetRelativePath(ProjectRuntimeSnapshot.ClassFileData row) {
        List<String> segments = new ArrayList<>();
        segments.add("jar-" + normalizeId(row == null ? null : row.jarId()));
        segments.add("cf-" + Math.max(0, row == null ? 0 : row.cfId()));
        segments.addAll(sanitizePathSegments(classFilePath(row)));
        return buildRelativePath(segments, "class.bin");
    }

    private static Path buildResourceAssetRelativePath(ProjectRuntimeSnapshot.ResourceData row) {
        List<String> segments = new ArrayList<>();
        segments.add("jar-" + normalizeId(row == null ? null : row.jarId()));
        segments.add("rid-" + Math.max(0, row == null ? 0 : row.rid()));
        segments.addAll(sanitizePathSegments(row == null ? null : row.resourcePath()));
        return buildRelativePath(segments, "resource.bin");
    }

    private static Path buildRelativePath(List<String> segments, String fallbackFileName) {
        Path relative = Paths.get("");
        boolean hasFileName = false;
        for (String segment : segments) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            relative = relative.resolve(segment);
            hasFileName = true;
        }
        if (!hasFileName) {
            return Paths.get(fallbackFileName);
        }
        return relative.normalize();
    }

    private static List<String> sanitizePathSegments(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return List.of();
        }
        String normalized = rawPath.replace('\\', '/');
        String[] parts = normalized.split("/");
        List<String> out = new ArrayList<>();
        for (String part : parts) {
            String cleaned = safeSegment(part);
            if (!cleaned.isBlank()) {
                out.add(cleaned);
            }
        }
        return out;
    }

    private static String classFilePath(ProjectRuntimeSnapshot.ClassFileData row) {
        String className = row == null ? "" : safe(row.className()).replace('.', '/');
        if (className.isBlank()) {
            return "";
        }
        return className.endsWith(".class") ? className : className + ".class";
    }

    private static String safeSegment(String value) {
        String trimmed = safe(value);
        if (trimmed.isBlank() || ".".equals(trimmed) || "..".equals(trimmed)) {
            return "";
        }
        return trimmed.replace(':', '_');
    }

    private static Path safeResolve(Path root, Path relative) {
        Path normalizedRoot = requireHome(root);
        Path normalizedRelative = relative == null ? Paths.get("") : relative.normalize();
        Path resolved = normalizedRoot.resolve(normalizedRelative).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new IllegalStateException("runtime_asset_path_escape");
        }
        return resolved;
    }

    private static boolean shouldMaterialize(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return false;
        }
        try {
            Path path = Paths.get(rawPath).toAbsolutePath().normalize();
            Path tempRoot = Paths.get(Const.tempDir).toAbsolutePath().normalize();
            return path.startsWith(tempRoot);
        } catch (Exception ex) {
            return false;
        }
    }

    private static Path requireHome(Path home) {
        if (home == null) {
            throw new IllegalArgumentException("project_home_missing");
        }
        return home.toAbsolutePath().normalize();
    }

    private static int normalizeId(Integer value) {
        return value == null || value < 0 ? -1 : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public ProjectRuntimeSnapshot read(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        Path target = resolveSnapshotFile(normalized);
        if (!Files.exists(target)) {
            return null;
        }
        try {
            String raw = Files.readString(target, StandardCharsets.UTF_8);
            if (raw == null || raw.isBlank()) {
                return null;
            }
            ProjectRuntimeSnapshot snapshot = JSON.parseObject(
                    raw,
                    new TypeReference<ProjectRuntimeSnapshot>() {
                    }
            );
            if (snapshot == null) {
                return null;
            }
            if (snapshot.schemaVersion() > ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION) {
                logger.warn("skip project runtime snapshot with newer schema: key={} version={}",
                        normalized, snapshot.schemaVersion());
                return null;
            }
            return snapshot;
        } catch (Exception ex) {
            logger.warn("read project runtime snapshot fail: key={} err={}", normalized, ex.toString());
            return null;
        }
    }

    Path resolveSnapshotFile(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        return resolveSnapshotFile(Neo4jProjectStore.getInstance().resolveProjectHome(normalized));
    }

    Path resolveSnapshotFile(Path projectHome) {
        if (projectHome == null) {
            throw new IllegalArgumentException("project_home_missing");
        }
        return projectHome.resolve(SNAPSHOT_FILE);
    }
}
