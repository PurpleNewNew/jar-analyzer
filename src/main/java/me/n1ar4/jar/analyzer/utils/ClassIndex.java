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
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jStore;
import me.n1ar4.jar.analyzer.storage.neo4j.repo.Neo4jReadRepository;
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
    private static final Neo4jReadRepository READ_REPO = new Neo4jReadRepository(Neo4jStore.getInstance());

    private static volatile Map<String, ClassLocation> INDEX = Collections.emptyMap();
    private static volatile Map<String, List<ClassLocation>> DUP_INDEX = Collections.emptyMap();
    private static volatile Map<String, ClassLocation> PATH_INDEX = Collections.emptyMap();
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
        Path normalized = safePath(classFilePath.toString());
        if (normalized == null) {
            return null;
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
                Path path = safePath(row.getPathStr());
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
        INDEX = Collections.unmodifiableMap(next);
        DUP_INDEX = freezeDuplicates(dup);
        PATH_INDEX = Collections.unmodifiableMap(pathIndex);
        lastBuildSeq = buildSeq;
        initialized = true;
        logger.debug("class index loaded: {}", INDEX.size());
    }

    private static Path resolvePreferred(String key, int preferJarId) {
        ClassLocation primary = INDEX.get(key);
        if (primary != null && primary.jarId == preferJarId
                && primary.path != null && Files.exists(primary.path)) {
            return primary.path;
        }
        List<ClassLocation> list = DUP_INDEX.get(key);
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
            List<Map<String, Object>> rows = READ_REPO.list(
                    "MATCH (c:ClassFile) " +
                            "RETURN coalesce(c.cfid,-1) AS cfid, coalesce(c.className,'') AS className, coalesce(c.pathStr,'') AS pathStr, " +
                            "coalesce(c.jarName,'') AS jarName, coalesce(c.jarId,-1) AS jarId " +
                            "ORDER BY c.jarId ASC, c.className ASC",
                    Collections.emptyMap(),
                    60_000L
            );
            List<ClassFileEntity> out = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                ClassFileEntity entity = new ClassFileEntity();
                entity.setCfId(asInt(row.get("cfid"), -1));
                entity.setClassName(safe(row.get("className")));
                entity.setPathStr(safe(row.get("pathStr")));
                entity.setJarName(safe(row.get("jarName")));
                entity.setJarId(asInt(row.get("jarId"), -1));
                out.add(entity);
            }
            return out;
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

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int asInt(Object value, int def) {
        if (value == null) {
            return def;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return def;
        }
    }

    private static final class ClassLocation {
        private final Path path;
        private final int jarId;

        private ClassLocation(Path path, int jarId) {
            this.path = path;
            this.jarId = jarId;
        }
    }
}
