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

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BytecodeCache {
    private static final Logger logger = LogManager.getLogger();
    private static final long MIN_BYTES = 32L * 1024 * 1024;
    private static final long MAX_BYTES = 512L * 1024 * 1024;
    private static final LinkedHashMap<Path, byte[]> CACHE =
            new LinkedHashMap<>(256, 0.75f, true);

    private static long maxBytes = resolveDefaultMaxBytes();
    private static long currentBytes = 0L;

    private BytecodeCache() {
    }

    public static byte[] read(Path path) {
        if (path == null) {
            return null;
        }
        Path key = normalize(path);
        byte[] cached = getCached(key);
        if (cached != null) {
            return cached;
        }
        byte[] data;
        try {
            data = Files.readAllBytes(key);
        } catch (Exception e) {
            logger.error("read class bytes error: {}", e.toString());
            return null;
        }
        if (data == null) {
            return null;
        }
        putCached(key, data);
        return data;
    }

    public static void clear() {
        synchronized (CACHE) {
            CACHE.clear();
            currentBytes = 0L;
        }
    }

    public static long getMaxBytes() {
        return maxBytes;
    }

    public static void setMaxBytes(long bytes) {
        if (bytes <= 0) {
            return;
        }
        maxBytes = clamp(bytes, MIN_BYTES, MAX_BYTES);
        evictIfNeeded();
    }

    private static byte[] getCached(Path key) {
        synchronized (CACHE) {
            return CACHE.get(key);
        }
    }

    private static void putCached(Path key, byte[] data) {
        if (data.length <= 0 || data.length > maxBytes) {
            return;
        }
        synchronized (CACHE) {
            byte[] prev = CACHE.put(key, data);
            if (prev != null) {
                currentBytes -= prev.length;
            }
            currentBytes += data.length;
            evictIfNeeded();
        }
    }

    private static void evictIfNeeded() {
        synchronized (CACHE) {
            if (currentBytes <= maxBytes) {
                return;
            }
            Iterator<Map.Entry<Path, byte[]>> it = CACHE.entrySet().iterator();
            while (it.hasNext() && currentBytes > maxBytes) {
                Map.Entry<Path, byte[]> entry = it.next();
                byte[] data = entry.getValue();
                if (data != null) {
                    currentBytes -= data.length;
                }
                it.remove();
            }
        }
    }

    private static long resolveDefaultMaxBytes() {
        long maxMem = Runtime.getRuntime().maxMemory();
        if (maxMem <= 0) {
            return MIN_BYTES;
        }
        long target = maxMem / 8;
        return clamp(target, MIN_BYTES, MAX_BYTES);
    }

    private static long clamp(long value, long min, long max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static Path normalize(Path path) {
        try {
            return path.toAbsolutePath().normalize();
        } catch (Exception ignored) {
            return path;
        }
    }
}
