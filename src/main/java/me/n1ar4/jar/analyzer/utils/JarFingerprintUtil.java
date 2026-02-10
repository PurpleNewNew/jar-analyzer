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
import me.n1ar4.jar.analyzer.core.cache.BuildScopedLru;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;

public final class JarFingerprintUtil {
    private static final Logger logger = LogManager.getLogger();
    private static final int DEFAULT_CAPACITY = 512;
    private static final BuildScopedLru<String, CacheEntry> CACHE =
            new BuildScopedLru<>(resolveCapacity(), DatabaseManager::getBuildSeq);

    private JarFingerprintUtil() {
    }

    public static String sha256(String absPath) {
        if (StringUtil.isNull(absPath)) {
            return "";
        }
        String path = absPath.trim();
        if (path.isEmpty()) {
            return "";
        }
        try {
            Path p = Paths.get(path);
            if (!Files.exists(p)) {
                return "";
            }
            long size = safeSize(p);
            long mtime = safeMtime(p);
            CacheEntry cached = CACHE.get(path);
            if (cached != null && cached.isValid(size, mtime)) {
                return cached.fingerprint;
            }
            String fp = sha256File(path);
            if (!fp.isEmpty()) {
                CACHE.put(path, new CacheEntry(fp, size, mtime));
            }
            return fp;
        } catch (Exception ex) {
            logger.debug("sha256 fingerprint failed: {}: {}", absPath, ex.toString());
            return "";
        }
    }

    private static String sha256File(String absPath) {
        try {
            Path path = Paths.get(absPath);
            if (!Files.exists(path)) {
                return "";
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return toHex(digest.digest());
        } catch (Exception ex) {
            logger.debug("sha256 file failed: {}: {}", absPath, ex.toString());
            return "";
        }
    }

    private static long safeSize(Path path) {
        try {
            return Files.size(path);
        } catch (Exception ex) {
            logger.debug("read file size failed: {}: {}", path, ex.toString());
            return -1L;
        }
    }

    private static long safeMtime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception ex) {
            logger.debug("read file mtime failed: {}: {}", path, ex.toString());
            return -1L;
        }
    }

    private static int resolveCapacity() {
        String raw = System.getProperty("jar.analyzer.fingerprint.cache.max");
        if (raw == null || raw.trim().isEmpty()) {
            return DEFAULT_CAPACITY;
        }
        try {
            int v = Integer.parseInt(raw.trim());
            if (v < 16) {
                return DEFAULT_CAPACITY;
            }
            return v;
        } catch (NumberFormatException ex) {
            logger.debug("invalid fingerprint cache max: {}", raw);
            return DEFAULT_CAPACITY;
        }
    }

    private static final class CacheEntry {
        private final String fingerprint;
        private final long size;
        private final long mtime;

        private CacheEntry(String fingerprint, long size, long mtime) {
            this.fingerprint = fingerprint;
            this.size = size;
            this.mtime = mtime;
        }

        private boolean isValid(long size, long mtime) {
            return this.size == size && this.mtime == mtime;
        }
    }

    private static String toHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
