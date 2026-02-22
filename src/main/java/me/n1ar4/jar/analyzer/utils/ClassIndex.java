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
import me.n1ar4.jar.analyzer.core.SqlSessionFactoryUtil;
import me.n1ar4.jar.analyzer.core.mapper.ClassFileMapper;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.apache.ibatis.session.SqlSession;

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
    private static volatile Map<String, ClassLocation> INDEX = Collections.emptyMap();
    private static volatile Map<String, List<ClassLocation>> DUP_INDEX = Collections.emptyMap();
    private static volatile Map<String, ClassLocation> PATH_INDEX = Collections.emptyMap();
    private static final Map<String, Integer> MATERIALIZED_PATH_JAR = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile long lastBuildSeq = -1L;
    private static volatile boolean initialized = false;

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
        if (preferJarId != null) {
            Path preferred = resolvePreferred(key, preferJarId);
            if (preferred != null) {
                return preferred;
            }
            logger.debug("preferred jarId {} miss for {}", preferJarId, key);
        }
        ClassLocation location = INDEX.get(key);
        Path resolved = resolveLocationPath(location);
        if (resolved != null && Files.exists(resolved)) {
            return resolved;
        }
        return null;
    }

    public static Integer resolveJarId(Path classFilePath) {
        if (classFilePath == null) {
            return null;
        }
        ensureFresh();
        Path normalized = normalizePath(classFilePath);
        if (normalized == null) {
            return null;
        }
        Integer fromMaterialized = MATERIALIZED_PATH_JAR.get(normalized.toString());
        if (fromMaterialized != null) {
            return fromMaterialized <= 0 ? null : fromMaterialized;
        }
        ClassLocation location = PATH_INDEX.get(normalized.toString());
        if (location == null) {
            return null;
        }
        if (location.jarId == Integer.MAX_VALUE) {
            return null;
        }
        return location.jarId;
    }

    public static void refresh() {
        rebuild(DatabaseManager.getBuildSeq());
    }

    private static void ensureFresh() {
        long buildSeq = DatabaseManager.getBuildSeq();
        if (initialized && buildSeq == lastBuildSeq) {
            return;
        }
        synchronized (LOCK) {
            if (initialized && buildSeq == lastBuildSeq) {
                return;
            }
            rebuild(buildSeq);
        }
    }

    private static void rebuild(long buildSeq) {
        Map<String, ClassLocation> next = new HashMap<>();
        Map<String, List<ClassLocation>> dup = new HashMap<>();
        Map<String, ClassLocation> pathIndex = new HashMap<>();
        MATERIALIZED_PATH_JAR.clear();
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
                String rawPath = safeRawPath(row.getPathStr());
                if (rawPath == null) {
                    continue;
                }
                int jarId = row.getJarId() == null ? Integer.MAX_VALUE : row.getJarId();
                if (jarId < 0) {
                    jarId = Integer.MAX_VALUE;
                }
                ClassLocation location = new ClassLocation(rawPath, jarId);
                pathIndex.put(rawPath, location);
                Path normalizedPath = normalizePath(rawPath);
                if (normalizedPath != null) {
                    pathIndex.put(normalizedPath.toString(), location);
                }
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
        INDEX = Collections.unmodifiableMap(next);
        DUP_INDEX = freezeDuplicates(dup);
        PATH_INDEX = Collections.unmodifiableMap(pathIndex);
        lastBuildSeq = buildSeq;
        initialized = true;
        logger.debug("class index loaded: {}", INDEX.size());
    }

    private static Path resolvePreferred(String key, int preferJarId) {
        ClassLocation primary = INDEX.get(key);
        Path primaryPath = resolveLocationPath(primary);
        if (primary != null && primary.jarId == preferJarId
                && primaryPath != null && Files.exists(primaryPath)) {
            return primaryPath;
        }
        List<ClassLocation> list = DUP_INDEX.get(key);
        if (list == null || list.isEmpty()) {
            return null;
        }
        for (ClassLocation location : list) {
            Path resolved = resolveLocationPath(location);
            if (location != null && location.jarId == preferJarId
                    && resolved != null && Files.exists(resolved)) {
                return resolved;
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
        try (SqlSession session = SqlSessionFactoryUtil.sqlSessionFactory.openSession(true)) {
            ClassFileMapper mapper = session.getMapper(ClassFileMapper.class);
            return mapper.selectAllClassPaths();
        } catch (Exception ex) {
            logger.debug("load class index failed: {}", ex.toString());
            return Collections.emptyList();
        }
    }

    private static Path normalizePath(Path path) {
        if (path == null) {
            return null;
        }
        try {
            return path.toAbsolutePath().normalize();
        } catch (RuntimeException ex) {
            logger.debug("invalid path: {}: {}", path, ex.toString());
            return null;
        }
    }

    private static Path normalizePath(String raw) {
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

    private static String safeRawPath(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }

    private static Path resolveLocationPath(ClassLocation location) {
        if (location == null || location.rawPath == null) {
            return null;
        }
        Path resolved = ArchiveContentResolver.resolveClassPath(location.rawPath);
        if (resolved == null) {
            return null;
        }
        Path normalized = normalizePath(resolved);
        if (normalized == null) {
            return null;
        }
        if (location.jarId != Integer.MAX_VALUE) {
            MATERIALIZED_PATH_JAR.put(normalized.toString(), location.jarId);
        }
        return normalized;
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
        private final String rawPath;
        private final int jarId;

        private ClassLocation(String rawPath, int jarId) {
            this.rawPath = rawPath;
            this.jarId = jarId;
        }
    }
}
