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
import me.n1ar4.jar.analyzer.core.mapper.JarMapper;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.JarEntity;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.apache.ibatis.session.SqlSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PathResolver {
    private static final Logger logger = LogManager.getLogger();
    private static final Object LOCK = new Object();
    private static volatile long lastBuildSeq = -1L;
    private static volatile Map<Integer, JarEntity> jarCache = Collections.emptyMap();
    private static volatile Map<String, Path> classPathCache = Collections.emptyMap();

    private PathResolver() {
    }

    public static Path resolveClassFile(String className, Integer jarId) {
        String entry = normalizeClassEntry(className);
        if (entry == null) {
            return null;
        }
        ensureFresh();
        if (jarId != null && jarId >= 0) {
            Path byJar = classPathCache.get(classPathKey(entry, jarId));
            if (byJar != null && Files.exists(byJar)) {
                return byJar;
            }
        }
        Path any = classPathCache.get(classPathKeyAny(entry));
        if (any != null && Files.exists(any)) {
            return any;
        }
        String rawClass = entry.substring(0, entry.length() - ".class".length());
        return JarUtil.resolveClassFileInTemp(rawClass);
    }

    public static Path resolveResourceFile(ResourceEntity resource) {
        if (resource == null || StringUtil.isNull(resource.getResourcePath())) {
            return null;
        }
        ensureFresh();
        Integer jarId = resource.getJarId();
        if (jarId == null || jarId < 0) {
            return null;
        }
        JarEntity jar = jarCache.get(jarId);
        if (jar == null) {
            return null;
        }
        String key = resolveResourceJarKey(jar, jarId);
        Path root = Paths.get(Const.tempDir, Const.resourceDir, key).toAbsolutePath().normalize();
        Path candidate = root.resolve(resource.getResourcePath()).toAbsolutePath().normalize();
        if (!candidate.startsWith(root)) {
            logger.warn("resolve resource path reject traversal: {}", resource.getResourcePath());
            return null;
        }
        if (Files.exists(candidate)) {
            return candidate;
        }
        return null;
    }

    public static Path resolveClassFileByJar(String className, int jarId) {
        String entry = normalizeClassEntry(className);
        if (entry == null) {
            return null;
        }
        ensureFresh();
        Path byJar = classPathCache.get(classPathKey(entry, jarId));
        if (byJar != null && Files.exists(byJar)) {
            return byJar;
        }
        return resolveClassFile(className, Integer.valueOf(jarId));
    }

    private static String normalizeClassEntry(String className) {
        if (StringUtil.isNull(className)) {
            return null;
        }
        String normalized = className.trim().replace('\\', '/');
        if (normalized.startsWith("L") && normalized.endsWith(";") && normalized.length() > 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized.indexOf('/') < 0 && normalized.indexOf('.') >= 0) {
            normalized = normalized.replace('.', '/');
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        String bootPrefix = "BOOT-INF/classes/";
        int boot = normalized.indexOf(bootPrefix);
        if (boot >= 0) {
            normalized = normalized.substring(boot + bootPrefix.length());
        }
        String webPrefix = "WEB-INF/classes/";
        int web = normalized.indexOf(webPrefix);
        if (web >= 0) {
            normalized = normalized.substring(web + webPrefix.length());
        }
        if (!normalized.endsWith(".class")) {
            normalized += ".class";
        }
        if (normalized.contains("..")) {
            return null;
        }
        return normalized;
    }

    private static String resolveResourceJarKey(JarEntity jar, int jarId) {
        String base = sanitizeResourceKey(jar == null ? null : jar.getJarName());
        if (base.isEmpty()) {
            base = "unknown";
        }
        return base + "-" + jarId;
    }

    private static String sanitizeResourceKey(String name) {
        if (name == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        String value = sb.toString();
        while (value.startsWith("_")) {
            value = value.substring(1);
        }
        while (value.endsWith("_")) {
            value = value.substring(0, value.length() - 1);
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static String classPathKey(String classEntry, int jarId) {
        return classEntry + "#" + jarId;
    }

    private static String classPathKeyAny(String classEntry) {
        return classEntry + "#*";
    }

    private static void ensureFresh() {
        long buildSeq = DatabaseManager.getBuildSeq();
        if (lastBuildSeq == buildSeq && (!jarCache.isEmpty() || !classPathCache.isEmpty())) {
            return;
        }
        synchronized (LOCK) {
            if (lastBuildSeq == buildSeq && (!jarCache.isEmpty() || !classPathCache.isEmpty())) {
                return;
            }
            jarCache = loadJars();
            classPathCache = loadClassPaths();
            lastBuildSeq = buildSeq;
        }
    }

    private static Map<Integer, JarEntity> loadJars() {
        Map<Integer, JarEntity> out = new HashMap<>();
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            JarMapper mapper = session.getMapper(JarMapper.class);
            List<JarEntity> jars = mapper.selectAllJarMeta();
            if (jars != null) {
                for (JarEntity jar : jars) {
                    if (jar == null) {
                        continue;
                    }
                    out.put(jar.getJid(), jar);
                }
            }
        } catch (Exception ex) {
            logger.debug("load path resolver jar cache fail: {}", ex.toString());
        }
        return out;
    }

    private static Map<String, Path> loadClassPaths() {
        Map<String, Path> out = new HashMap<>();
        try (SqlSession session = SqlSessionFactoryUtil.getSqlSessionFactory().openSession(true)) {
            ClassFileMapper mapper = session.getMapper(ClassFileMapper.class);
            List<ClassFileEntity> rows = mapper.selectAllClassPaths();
            if (rows == null) {
                return out;
            }
            for (ClassFileEntity row : rows) {
                if (row == null) {
                    continue;
                }
                String entry = normalizeClassEntry(row.getClassName());
                if (entry == null) {
                    continue;
                }
                Path path = resolvePathFromRow(row, entry);
                if (path == null) {
                    continue;
                }
                Integer jarId = row.getJarId();
                if (jarId != null && jarId >= 0) {
                    out.put(classPathKey(entry, jarId), path);
                }
                out.putIfAbsent(classPathKeyAny(entry), path);
            }
        } catch (Exception ex) {
            logger.debug("load path resolver class cache fail: {}", ex.toString());
        }
        return out;
    }

    private static Path resolvePathFromRow(ClassFileEntity row, String entry) {
        String rel = row.getPathStr();
        Path base = Paths.get(Const.tempDir).toAbsolutePath().normalize();
        if (!StringUtil.isNull(rel)) {
            Path parsed = safePath(rel);
            if (parsed != null) {
                if (!parsed.isAbsolute()) {
                    parsed = base.resolve(parsed).toAbsolutePath().normalize();
                }
                if (parsed.startsWith(base)) {
                    return parsed;
                }
            }
        }
        Path fallback = base.resolve("classes").resolve(entry).toAbsolutePath().normalize();
        if (fallback.startsWith(base)) {
            return fallback;
        }
        return null;
    }

    private static Path safePath(String raw) {
        if (StringUtil.isNull(raw)) {
            return null;
        }
        try {
            return Paths.get(raw.trim()).normalize();
        } catch (Exception ignored) {
            return null;
        }
    }
}
