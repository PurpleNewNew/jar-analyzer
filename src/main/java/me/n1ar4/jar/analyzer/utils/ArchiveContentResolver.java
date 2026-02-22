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
import me.n1ar4.jar.analyzer.core.mapper.JarMapper;
import me.n1ar4.jar.analyzer.entity.JarEntity;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.apache.ibatis.session.SqlSession;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ArchiveContentResolver {
    private static final Logger logger = LogManager.getLogger();
    private static final int MATERIALIZED_OWNER_MAX = 32768;
    private static final Object REFRESH_LOCK = new Object();
    private static final Map<String, Integer> MATERIALIZED_OWNER =
            Collections.synchronizedMap(new LinkedHashMap<>(512, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
                    return size() > MATERIALIZED_OWNER_MAX;
                }
            });

    private static volatile long lastBuildSeq = -1L;
    private static volatile Map<Integer, Path> jarPathById = Map.of();
    private static final Map<String, Path> classPathCache = new ConcurrentHashMap<>();

    private ArchiveContentResolver() {
    }

    public static Path resolveClassPath(String pathOrLocator) {
        String raw = safe(pathOrLocator).trim();
        if (raw.isEmpty()) {
            return null;
        }
        ArchiveVirtualPath.Locator locator = ArchiveVirtualPath.parseClass(raw);
        if (locator == null) {
            return normalizeFsPath(raw);
        }
        String cacheKey = locator.jarId() + "|" + locator.entryPath();
        Path cached = classPathCache.get(cacheKey);
        if (cached != null && Files.exists(cached)) {
            registerOwner(cached, locator.jarId());
            return cached;
        }
        Path materialized = materializeClass(locator);
        if (materialized != null) {
            classPathCache.put(cacheKey, materialized);
        }
        return materialized;
    }

    public static byte[] readClassBytes(String className, Integer jarId, String pathStr) {
        String locator = safe(pathStr).trim();
        ArchiveVirtualPath.Locator parsed = ArchiveVirtualPath.parseClass(locator);
        if (parsed != null) {
            return readArchiveEntry(parsed.jarId(), parsed.entryPath(), 0, Integer.MAX_VALUE);
        }
        String normalized = normalizeClassName(className);
        if (normalized == null || jarId == null || jarId <= 0) {
            return null;
        }
        String entryName = normalized + ".class";
        byte[] bytes = readArchiveEntry(jarId, entryName, 0, Integer.MAX_VALUE);
        if (bytes != null) {
            return bytes;
        }
        bytes = readArchiveEntry(jarId, "BOOT-INF/classes/" + entryName, 0, Integer.MAX_VALUE);
        if (bytes != null) {
            return bytes;
        }
        return readArchiveEntry(jarId, "WEB-INF/classes/" + entryName, 0, Integer.MAX_VALUE);
    }

    public static byte[] readResourceBytes(ResourceEntity resource, int offset, int limit) {
        if (resource == null) {
            return null;
        }
        String locator = safe(resource.getPathStr()).trim();
        byte[] fromLocator = readResourceBytes(locator, offset, limit);
        if (fromLocator != null) {
            return fromLocator;
        }
        Integer jarId = resource.getJarId();
        String entry = safe(resource.getResourcePath()).trim();
        if (jarId == null || jarId <= 0 || entry.isEmpty()) {
            return null;
        }
        return readArchiveEntry(jarId, entry, offset, limit);
    }

    public static byte[] readResourceBytes(String pathOrLocator, int offset, int limit) {
        String raw = safe(pathOrLocator).trim();
        if (raw.isEmpty()) {
            return null;
        }
        ArchiveVirtualPath.Locator locator = ArchiveVirtualPath.parseResource(raw);
        if (locator != null) {
            return readArchiveEntry(locator.jarId(), locator.entryPath(), offset, limit);
        }
        Path path = normalizeFsPath(raw);
        if (path == null || Files.notExists(path) || !Files.isRegularFile(path)) {
            return null;
        }
        int safeOffset = Math.max(0, offset);
        int safeLimit = limit <= 0 ? Integer.MAX_VALUE : limit;
        try (InputStream inputStream = Files.newInputStream(path)) {
            skipFully(inputStream, safeOffset);
            if (safeLimit == Integer.MAX_VALUE) {
                return IOUtil.readBytes(inputStream);
            }
            return IOUtils.readNBytes(inputStream, safeLimit);
        } catch (Exception ex) {
            logger.debug("read fs resource failed: {}: {}", path, ex.toString());
            return null;
        }
    }

    public static Integer resolveJarIdByClassPath(Path classPath) {
        if (classPath == null) {
            return null;
        }
        try {
            String key = classPath.toAbsolutePath().normalize().toString();
            return MATERIALIZED_OWNER.get(key);
        } catch (Exception ex) {
            return null;
        }
    }

    private static Path materializeClass(ArchiveVirtualPath.Locator locator) {
        if (locator == null) {
            return null;
        }
        int jarId = locator.jarId();
        String entry = locator.entryPath();
        byte[] bytes = readArchiveEntry(jarId, entry, 0, Integer.MAX_VALUE);
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        Path base = Paths.get(Const.tempDir, "runtime-cache", "gui-class", String.valueOf(jarId))
                .toAbsolutePath().normalize();
        Path output = safeResolve(base, entry);
        if (output == null) {
            return null;
        }
        try {
            Path parent = output.getParent();
            if (parent != null && Files.notExists(parent)) {
                Files.createDirectories(parent);
            }
            Files.write(output, bytes);
            BytecodeCache.preload(output, bytes);
            registerOwner(output, jarId);
            return output;
        } catch (Exception ex) {
            logger.warn("materialize class fail: {} {}", entry, ex.getMessage());
            return null;
        }
    }

    private static byte[] readArchiveEntry(int jarId, String entryName, int offset, int limit) {
        if (jarId <= 0 || StringUtil.isNull(entryName)) {
            return null;
        }
        Path jarPath = resolveJarPath(jarId);
        if (jarPath == null || Files.notExists(jarPath)) {
            return null;
        }
        int safeOffset = Math.max(0, offset);
        int safeLimit = limit <= 0 ? Integer.MAX_VALUE : limit;
        try (ZipFile zipFile = new ZipFile(jarPath.toFile())) {
            ZipEntry entry = zipFile.getEntry(entryName);
            if (entry == null) {
                return null;
            }
            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                skipFully(inputStream, safeOffset);
                if (safeLimit == Integer.MAX_VALUE) {
                    return IOUtil.readBytes(inputStream);
                }
                return IOUtils.readNBytes(inputStream, safeLimit);
            }
        } catch (Exception ex) {
            logger.debug("read jar entry failed: {}!{}: {}", jarPath, entryName, ex.toString());
            return null;
        }
    }

    private static Path resolveJarPath(int jarId) {
        refreshJarPathMap();
        return jarPathById.get(jarId);
    }

    private static void refreshJarPathMap() {
        long buildSeq = DatabaseManager.getBuildSeq();
        if (buildSeq == lastBuildSeq && !jarPathById.isEmpty()) {
            return;
        }
        synchronized (REFRESH_LOCK) {
            if (buildSeq == lastBuildSeq && !jarPathById.isEmpty()) {
                return;
            }
            Map<Integer, Path> next = new HashMap<>();
            try (SqlSession session = SqlSessionFactoryUtil.sqlSessionFactory.openSession(true)) {
                JarMapper mapper = session.getMapper(JarMapper.class);
                for (JarEntity row : mapper.selectAllJarMeta()) {
                    if (row == null) {
                        continue;
                    }
                    Path jarPath = normalizeFsPath(row.getJarAbsPath());
                    if (jarPath != null) {
                        next.put(row.getJid(), jarPath);
                    }
                }
            } catch (Exception ex) {
                logger.debug("refresh jar path map failed: {}", ex.toString());
            }
            jarPathById = Collections.unmodifiableMap(next);
            classPathCache.clear();
            lastBuildSeq = buildSeq;
        }
    }

    private static void skipFully(InputStream inputStream, long bytes) throws Exception {
        long remaining = Math.max(0L, bytes);
        while (remaining > 0) {
            long skipped = inputStream.skip(remaining);
            if (skipped <= 0) {
                if (inputStream.read() == -1) {
                    break;
                }
                remaining--;
            } else {
                remaining -= skipped;
            }
        }
    }

    private static String normalizeClassName(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().replace("\\", "/");
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - 6);
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.indexOf('/') < 0 && normalized.indexOf('.') >= 0) {
            normalized = normalized.replace('.', '/');
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private static Path normalizeFsPath(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Paths.get(raw.trim()).toAbsolutePath().normalize();
        } catch (Exception ex) {
            return null;
        }
    }

    private static Path safeResolve(Path base, String entry) {
        if (base == null || StringUtil.isNull(entry)) {
            return null;
        }
        try {
            Path resolved = base.resolve(entry).toAbsolutePath().normalize();
            if (!resolved.startsWith(base)) {
                return null;
            }
            return resolved;
        } catch (Exception ex) {
            return null;
        }
    }

    private static void registerOwner(Path path, int jarId) {
        if (path == null || jarId <= 0) {
            return;
        }
        try {
            String key = path.toAbsolutePath().normalize().toString();
            MATERIALIZED_OWNER.put(key, jarId);
        } catch (Exception ignored) {
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
