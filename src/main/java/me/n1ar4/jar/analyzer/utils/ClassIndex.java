/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.utils;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.core.facts.ClassFileEntity;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClassIndex {
    private static final Logger logger = LogManager.getLogger();
    private static final Object LOCK = new Object();
    private static volatile IndexSnapshot SNAPSHOT = IndexSnapshot.empty();

    private ClassIndex() {
    }

    public static Path resolveClassFile(String className) {
        return resolveClassFile(className, null);
    }

    public static Path resolveClassFile(String className, Integer preferJarId) {
        String key = normalizeClassName(className);
        if (key == null) {
            return null;
        }
        ensureFresh();
        IndexSnapshot snapshot = SNAPSHOT;
        if (preferJarId != null) {
            Path preferred = resolvePreferred(snapshot, key, preferJarId);
            if (preferred != null) {
                return preferred;
            }
            logger.debug("preferred jarId {} miss for {}", preferJarId, key);
        }
        ClassLocation location = snapshot.index.get(key);
        if (location != null && location.path != null && Files.exists(location.path)) {
            return location.path;
        }
        return null;
    }

    public static Integer resolveJarId(Path classFilePath) {
        if (classFilePath == null) {
            return null;
        }
        ensureFresh();
        IndexSnapshot snapshot = SNAPSHOT;
        Path normalized = safePath(classFilePath.toString());
        if (normalized == null) {
            return null;
        }
        ClassLocation location = snapshot.pathIndex.get(normalized.toString());
        if (location == null) {
            return null;
        }
        if (location.jarId == Integer.MAX_VALUE) {
            return null;
        }
        return location.jarId;
    }

    public static void refresh() {
        synchronized (LOCK) {
            rebuild(ProjectRuntimeContext.stateVersion());
        }
    }

    private static void ensureFresh() {
        long stateVersion = ProjectRuntimeContext.stateVersion();
        IndexSnapshot snapshot = SNAPSHOT;
        if (snapshot.initialized && stateVersion == snapshot.stateVersion) {
            return;
        }
        synchronized (LOCK) {
            snapshot = SNAPSHOT;
            if (snapshot.initialized && stateVersion == snapshot.stateVersion) {
                return;
            }
            rebuild(stateVersion);
        }
    }

    private static void rebuild(long stateVersion) {
        Map<String, ClassLocation> next = new HashMap<>();
        Map<String, List<ClassLocation>> dup = new HashMap<>();
        Map<String, ClassLocation> pathIndex = new HashMap<>();
        List<ClassFileEntity> rows = loadClassFiles();
        if (rows != null) {
            for (ClassFileEntity row : rows) {
                if (row == null) {
                    continue;
                }
                String key = normalizeClassName(row.getClassName());
                if (key == null) {
                    continue;
                }
                Path path = row.resolvePath();
                if (path != null) {
                    path = path.toAbsolutePath().normalize();
                }
                if (path == null) {
                    continue;
                }
                int jarId = row.getJarId() == null ? Integer.MAX_VALUE : row.getJarId();
                if (jarId < 0) {
                    jarId = Integer.MAX_VALUE;
                }
                ClassLocation location = new ClassLocation(path, jarId);
                pathIndex.put(path.toString(), location);
                ClassLocation existing = next.get(key);
                if (existing == null) {
                    next.put(key, location);
                } else if (jarId < existing.jarId) {
                    addDuplicate(dup, key, existing);
                    next.put(key, location);
                } else {
                    addDuplicate(dup, key, location);
                }
            }
        }
        SNAPSHOT = new IndexSnapshot(
                Collections.unmodifiableMap(next),
                freezeDuplicates(dup),
                Collections.unmodifiableMap(pathIndex),
                stateVersion,
                true
        );
        logger.debug("class index loaded: {}", next.size());
    }

    private static Path resolvePreferred(IndexSnapshot snapshot, String key, int preferJarId) {
        ClassLocation primary = snapshot.index.get(key);
        if (primary != null && primary.jarId == preferJarId
                && primary.path != null && Files.exists(primary.path)) {
            return primary.path;
        }
        List<ClassLocation> list = snapshot.duplicateIndex.get(key);
        if (list == null || list.isEmpty()) {
            return null;
        }
        for (ClassLocation location : list) {
            if (location != null && location.jarId == preferJarId
                    && location.path != null && Files.exists(location.path)) {
                return location.path;
            }
        }
        return null;
    }

    private static void addDuplicate(Map<String, List<ClassLocation>> dup,
                                     String key,
                                     ClassLocation location) {
        if (key == null || location == null) {
            return;
        }
        List<ClassLocation> list = dup.get(key);
        if (list == null) {
            list = new ArrayList<>();
            dup.put(key, list);
        }
        list.add(location);
    }

    private static Map<String, List<ClassLocation>> freezeDuplicates(Map<String, List<ClassLocation>> dup) {
        if (dup == null || dup.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<ClassLocation>> out = new HashMap<>();
        for (Map.Entry<String, List<ClassLocation>> entry : dup.entrySet()) {
            List<ClassLocation> list = entry.getValue();
            if (list == null || list.isEmpty()) {
                continue;
            }
            out.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(list)));
        }
        return Collections.unmodifiableMap(out);
    }

    private static List<ClassFileEntity> loadClassFiles() {
        try {
            return DatabaseManager.getClassFiles();
        } catch (Exception ex) {
            logger.debug("load class index failed: {}", ex.toString());
            return Collections.emptyList();
        }
    }

    private static Path safePath(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Paths.get(raw.trim()).toAbsolutePath().normalize();
        } catch (RuntimeException ex) {
            logger.debug("invalid path: {}: {}", raw, ex.toString());
            return null;
        }
    }

    private static String normalizeClassName(String raw) {
        if (raw == null) {
            return null;
        }
        String name = raw.trim();
        if (name.isEmpty()) {
            return null;
        }
        if (name.startsWith("L") && name.endsWith(";") && name.length() > 2) {
            name = name.substring(1, name.length() - 1);
        }
        name = name.replace('\\', '/');
        if (name.endsWith(".class")) {
            name = name.substring(0, name.length() - ".class".length());
        }
        while (name.startsWith("/")) {
            name = name.substring(1);
        }
        if (name.indexOf('/') < 0 && name.indexOf('.') >= 0) {
            name = name.replace('.', '/');
        }
        return name.isEmpty() ? null : name;
    }

    private static final class ClassLocation {
        private final Path path;
        private final int jarId;

        private ClassLocation(Path path, int jarId) {
            this.path = path;
            this.jarId = jarId;
        }
    }

    private static final class IndexSnapshot {
        private final Map<String, ClassLocation> index;
        private final Map<String, List<ClassLocation>> duplicateIndex;
        private final Map<String, ClassLocation> pathIndex;
        private final long stateVersion;
        private final boolean initialized;

        private IndexSnapshot(Map<String, ClassLocation> index,
                              Map<String, List<ClassLocation>> duplicateIndex,
                              Map<String, ClassLocation> pathIndex,
                              long stateVersion,
                              boolean initialized) {
            this.index = index == null ? Collections.emptyMap() : index;
            this.duplicateIndex = duplicateIndex == null ? Collections.emptyMap() : duplicateIndex;
            this.pathIndex = pathIndex == null ? Collections.emptyMap() : pathIndex;
            this.stateVersion = stateVersion;
            this.initialized = initialized;
        }

        private static IndexSnapshot empty() {
            return new IndexSnapshot(
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    -1L,
                    false
            );
        }
    }
}
