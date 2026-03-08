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
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class ProjectMetadataSnapshotStore {
    private static final Logger logger = LogManager.getLogger();
    private static final ProjectMetadataSnapshotStore INSTANCE = new ProjectMetadataSnapshotStore();
    private static final String SNAPSHOT_FILE = "runtime-metadata.json";
    private static final String UNAVAILABLE_FILE = "runtime-metadata.unavailable";
    private static final String ASSET_DIR = "runtime-assets";
    private static final String CLASS_ASSET_DIR = "classes";
    private static final String RESOURCE_ASSET_DIR = "resources";

    private final Map<String, CachedSnapshot> snapshotCache = new ConcurrentHashMap<>();

    private ProjectMetadataSnapshotStore() {
    }

    public static ProjectMetadataSnapshotStore getInstance() {
        return INSTANCE;
    }

    void writeToHome(Path assetHome, Path persistedHome, ProjectRuntimeSnapshot snapshot) {
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
            clearUnavailableMarker(assetHome);
            snapshotCache.clear();
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

    public void markUnavailable(String projectKey, long buildSeq, String reason) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        Path marker = resolveUnavailableFile(normalized);
        Path temp = marker.resolveSibling(marker.getFileName() + ".tmp");
        try {
            Files.createDirectories(marker.getParent());
            Map<String, Object> payload = new HashMap<>();
            payload.put("projectKey", normalized);
            payload.put("buildSeq", Math.max(0L, buildSeq));
            payload.put("reason", safe(reason));
            payload.put("updatedAt", System.currentTimeMillis());
            Files.writeString(temp, JSON.toJSONString(payload), StandardCharsets.UTF_8);
            try {
                Files.move(temp, marker,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception ex) {
                Files.move(temp, marker, StandardCopyOption.REPLACE_EXISTING);
            }
            snapshotCache.remove(normalized);
        } catch (Exception ex) {
            try {
                Files.deleteIfExists(temp);
            } catch (Exception ignored) {
                // best effort
            }
            logger.error("mark project runtime unavailable fail: key={} err={}", normalized, ex.toString(), ex);
            throw new IllegalStateException("project_runtime_snapshot_unavailable_mark_failed", ex);
        }
    }

    public boolean isUnavailable(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        Path marker = resolveUnavailableFile(normalized);
        if (!Files.exists(marker)) {
            return false;
        }
        return true;
    }

    public void clearUnavailable(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        Path marker = resolveUnavailableFile(normalized);
        try {
            Files.deleteIfExists(marker);
        } catch (Exception ex) {
            logger.debug("clear project runtime unavailable marker fail: key={} err={}", normalized, ex.toString());
        } finally {
            snapshotCache.remove(normalized);
        }
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
        CachedSnapshot cached = loadCachedSnapshot(normalized);
        return cached == null ? null : cached.snapshot();
    }

    public long readBuildSeq(String projectKey) {
        ProjectRuntimeSnapshot snapshot = read(projectKey);
        return snapshot == null ? 0L : Math.max(0L, snapshot.buildSeq());
    }

    public ProjectRuntimeSnapshot.ProjectModelData readProjectModelRegardlessOfAvailability(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        ProjectRuntimeSnapshot snapshot = readDirectSnapshot(normalized);
        return snapshot == null ? null : snapshot.projectModel();
    }

    public ProjectRuntimeSnapshot.ClassFileData findClassFile(String projectKey,
                                                              String className,
                                                              Integer jarId) {
        CachedSnapshot cached = loadCachedSnapshot(ActiveProjectContext.resolveRequestedOrActive(projectKey));
        if (cached == null) {
            return null;
        }
        return cached.index().findClassFile(className, jarId);
    }

    public List<ProjectRuntimeSnapshot.ClassReferenceData> findClassReferences(String projectKey, String className) {
        CachedSnapshot cached = loadCachedSnapshot(ActiveProjectContext.resolveRequestedOrActive(projectKey));
        if (cached == null) {
            return List.of();
        }
        return cached.index().findClassReferences(className);
    }

    public ProjectRuntimeSnapshot.ClassReferenceData findClassReference(String projectKey,
                                                                        String className,
                                                                        Integer jarId) {
        CachedSnapshot cached = loadCachedSnapshot(ActiveProjectContext.resolveRequestedOrActive(projectKey));
        if (cached == null) {
            return null;
        }
        return cached.index().findClassReference(className, jarId);
    }

    public List<ProjectRuntimeSnapshot.MethodReferenceData> findMethodReferencesByClass(String projectKey,
                                                                                        String className) {
        CachedSnapshot cached = loadCachedSnapshot(ActiveProjectContext.resolveRequestedOrActive(projectKey));
        if (cached == null) {
            return List.of();
        }
        return cached.index().findMethodReferencesByClass(className);
    }

    Path resolveSnapshotFile(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        return resolveSnapshotFile(Neo4jProjectStore.getInstance().resolveProjectHome(normalized));
    }

    Path resolveUnavailableFile(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        return resolveUnavailableFile(Neo4jProjectStore.getInstance().resolveProjectHome(normalized));
    }

    Path resolveSnapshotFile(Path projectHome) {
        if (projectHome == null) {
            throw new IllegalArgumentException("project_home_missing");
        }
        return projectHome.resolve(SNAPSHOT_FILE);
    }

    Path resolveUnavailableFile(Path projectHome) {
        if (projectHome == null) {
            throw new IllegalArgumentException("project_home_missing");
        }
        return projectHome.resolve(UNAVAILABLE_FILE);
    }

    private CachedSnapshot loadCachedSnapshot(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        if (isUnavailable(normalized)) {
            snapshotCache.remove(normalized);
            return null;
        }
        Path target = resolveSnapshotFile(normalized);
        SnapshotFileStamp stamp = SnapshotFileStamp.of(target);
        if (stamp == null) {
            snapshotCache.remove(normalized);
            return null;
        }
        CachedSnapshot cached = snapshotCache.get(normalized);
        if (cached != null && cached.stamp().equals(stamp)) {
            return cached;
        }
        try {
            ProjectRuntimeSnapshot snapshot = parseSnapshotFile(target, normalized);
            if (snapshot == null) {
                snapshotCache.remove(normalized);
                return null;
            }
            CachedSnapshot refreshed = new CachedSnapshot(stamp, snapshot, SnapshotIndex.of(snapshot));
            snapshotCache.put(normalized, refreshed);
            return refreshed;
        } catch (Exception ex) {
            logger.warn("read project runtime snapshot fail: key={} err={}", normalized, ex.toString());
            snapshotCache.remove(normalized);
            return null;
        }
    }

    private ProjectRuntimeSnapshot readDirectSnapshot(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        Path target = resolveSnapshotFile(normalized);
        if (target == null || !Files.exists(target)) {
            return null;
        }
        try {
            return parseSnapshotFile(target, normalized);
        } catch (Exception ex) {
            logger.warn("read project runtime snapshot direct fail: key={} err={}", normalized, ex.toString());
            return null;
        }
    }

    private ProjectRuntimeSnapshot parseSnapshotFile(Path target, String projectKey) throws Exception {
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
                    projectKey, snapshot.schemaVersion());
            return null;
        }
        return snapshot;
    }

    private record CachedSnapshot(SnapshotFileStamp stamp,
                                  ProjectRuntimeSnapshot snapshot,
                                  SnapshotIndex index) {
    }

    private record SnapshotFileStamp(long size, long lastModifiedNs, String fileKey) {
        private static SnapshotFileStamp of(Path target) {
            if (target == null || !Files.exists(target)) {
                return null;
            }
            try {
                BasicFileAttributes attrs = Files.readAttributes(target, BasicFileAttributes.class);
                Object fileKey = attrs.fileKey();
                return new SnapshotFileStamp(
                        attrs.size(),
                        attrs.lastModifiedTime().to(TimeUnit.NANOSECONDS),
                        fileKey == null ? "" : fileKey.toString()
                );
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private void clearUnavailableMarker(Path projectHome) {
        if (projectHome == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolveUnavailableFile(projectHome));
        } catch (Exception ex) {
            logger.debug("clear project runtime unavailable marker fail: {} ({})", projectHome, ex.toString());
        }
    }

    private static final class SnapshotIndex {
        private final Map<String, List<ProjectRuntimeSnapshot.ClassFileData>> classFilesByName;
        private final Map<String, ProjectRuntimeSnapshot.ClassFileData> primaryClassFileByName;
        private final Map<String, List<ProjectRuntimeSnapshot.ClassReferenceData>> classRefsByName;
        private final Map<String, List<ProjectRuntimeSnapshot.MethodReferenceData>> methodsByClass;

        private SnapshotIndex(Map<String, List<ProjectRuntimeSnapshot.ClassFileData>> classFilesByName,
                              Map<String, ProjectRuntimeSnapshot.ClassFileData> primaryClassFileByName,
                              Map<String, List<ProjectRuntimeSnapshot.ClassReferenceData>> classRefsByName,
                              Map<String, List<ProjectRuntimeSnapshot.MethodReferenceData>> methodsByClass) {
            this.classFilesByName = classFilesByName;
            this.primaryClassFileByName = primaryClassFileByName;
            this.classRefsByName = classRefsByName;
            this.methodsByClass = methodsByClass;
        }

        private static SnapshotIndex of(ProjectRuntimeSnapshot snapshot) {
            if (snapshot == null) {
                return new SnapshotIndex(Map.of(), Map.of(), Map.of(), Map.of());
            }
            Map<String, List<ProjectRuntimeSnapshot.ClassFileData>> classFilesByName = new HashMap<>();
            Map<String, ProjectRuntimeSnapshot.ClassFileData> primaryClassFileByName = new HashMap<>();
            for (ProjectRuntimeSnapshot.ClassFileData row : snapshot.classFiles()) {
                if (row == null) {
                    continue;
                }
                String className = normalizeClassName(row.className());
                if (className == null) {
                    continue;
                }
                classFilesByName.computeIfAbsent(className, ignore -> new ArrayList<>()).add(row);
                primaryClassFileByName.merge(className, row, SnapshotIndex::pickPreferredClassFile);
            }

            Map<String, List<ProjectRuntimeSnapshot.ClassReferenceData>> classRefsByName = new HashMap<>();
            for (ProjectRuntimeSnapshot.ClassReferenceData row : snapshot.classReferences()) {
                if (row == null) {
                    continue;
                }
                String className = normalizeClassName(row.name());
                if (className == null) {
                    continue;
                }
                classRefsByName.computeIfAbsent(className, ignore -> new ArrayList<>()).add(row);
            }

            Map<String, List<ProjectRuntimeSnapshot.MethodReferenceData>> methodsByClass = new HashMap<>();
            for (ProjectRuntimeSnapshot.MethodReferenceData row : snapshot.methodReferences()) {
                if (row == null || row.classReference() == null) {
                    continue;
                }
                String className = normalizeClassName(row.classReference().name());
                if (className == null) {
                    continue;
                }
                methodsByClass.computeIfAbsent(className, ignore -> new ArrayList<>()).add(row);
            }
            return new SnapshotIndex(classFilesByName, primaryClassFileByName, classRefsByName, methodsByClass);
        }

        private ProjectRuntimeSnapshot.ClassFileData findClassFile(String className, Integer jarId) {
            String normalized = normalizeClassName(className);
            if (normalized == null) {
                return null;
            }
            if (jarId != null && jarId >= 0) {
                List<ProjectRuntimeSnapshot.ClassFileData> rows = classFilesByName.get(normalized);
                if (rows != null) {
                    for (ProjectRuntimeSnapshot.ClassFileData row : rows) {
                        if (row != null && jarId.equals(row.jarId())) {
                            return row;
                        }
                    }
                }
            }
            return primaryClassFileByName.get(normalized);
        }

        private List<ProjectRuntimeSnapshot.ClassReferenceData> findClassReferences(String className) {
            String normalized = normalizeClassName(className);
            if (normalized == null) {
                return List.of();
            }
            List<ProjectRuntimeSnapshot.ClassReferenceData> rows = classRefsByName.get(normalized);
            return rows == null || rows.isEmpty() ? List.of() : List.copyOf(rows);
        }

        private ProjectRuntimeSnapshot.ClassReferenceData findClassReference(String className, Integer jarId) {
            List<ProjectRuntimeSnapshot.ClassReferenceData> rows = findClassReferences(className);
            if (rows.isEmpty()) {
                return null;
            }
            if (jarId != null && jarId >= 0) {
                for (ProjectRuntimeSnapshot.ClassReferenceData row : rows) {
                    if (row != null && jarId.equals(row.jarId())) {
                        return row;
                    }
                }
            }
            return rows.get(0);
        }

        private List<ProjectRuntimeSnapshot.MethodReferenceData> findMethodReferencesByClass(String className) {
            String normalized = normalizeClassName(className);
            if (normalized == null) {
                return List.of();
            }
            List<ProjectRuntimeSnapshot.MethodReferenceData> rows = methodsByClass.get(normalized);
            return rows == null || rows.isEmpty() ? List.of() : List.copyOf(rows);
        }

        private static ProjectRuntimeSnapshot.ClassFileData pickPreferredClassFile(ProjectRuntimeSnapshot.ClassFileData a,
                                                                                   ProjectRuntimeSnapshot.ClassFileData b) {
            if (a == null) {
                return b;
            }
            if (b == null) {
                return a;
            }
            int aj = normalizeJarId(a.jarId());
            int bj = normalizeJarId(b.jarId());
            if (bj < aj) {
                return b;
            }
            if (bj > aj) {
                return a;
            }
            return safe(b.pathStr()).compareTo(safe(a.pathStr())) < 0 ? b : a;
        }
    }

    private static int normalizeJarId(Integer jarId) {
        if (jarId == null || jarId < 0) {
            return Integer.MAX_VALUE;
        }
        return jarId;
    }

    private static String normalizeClassName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        normalized = normalized.replace('\\', '/');
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - ".class".length());
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.indexOf('/') < 0 && normalized.indexOf('.') >= 0) {
            normalized = normalized.replace('.', '/');
        }
        return normalized.isEmpty() ? null : normalized;
    }
}
