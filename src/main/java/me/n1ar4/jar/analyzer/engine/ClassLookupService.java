/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.engine;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.utils.BytecodeCache;
import me.n1ar4.jar.analyzer.utils.ClassIndex;
import me.n1ar4.jar.analyzer.utils.ExternalClassIndex;
import me.n1ar4.jar.analyzer.utils.IOUtil;
import me.n1ar4.jar.analyzer.utils.JarUtil;
import me.n1ar4.jar.analyzer.utils.RuntimeClassResolver;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ClassLookupService {
    private static final Logger logger = LogManager.getLogger();
    private static final String FALLBACK_PROP = "jar.analyzer.decompile.classpath.fallback";
    private static final int NEGATIVE_MAX = 16384;
    private static final long POSITIVE_MIN_BYTES = 32L * 1024 * 1024;
    private static final long POSITIVE_MAX_BYTES = 256L * 1024 * 1024;
    private static final Map<String, Boolean> NEGATIVE =
            new LinkedHashMap<String, Boolean>(NEGATIVE_MAX, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > NEGATIVE_MAX;
                }
            };
    private static final Map<String, CachedLookup> POSITIVE =
            new LinkedHashMap<String, CachedLookup>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CachedLookup> eldest) {
                    return false;
                }
            };
    private static final Object LOCK = new Object();
    private static volatile long lastBuildSeq = -1;
    private static volatile long lastRootSeq = -1;
    private static volatile long positiveBytes = 0L;
    private static volatile long positiveMaxBytes = resolvePositiveMaxBytes();

    private ClassLookupService() {
    }

    public static LookupResult find(String nameOrPath) {
        return find(nameOrPath, DecompileLookupContext.preferredJarId());
    }

    public static LookupResult find(String nameOrPath, Integer preferJarId) {
        if (nameOrPath == null) {
            return null;
        }
        String raw = nameOrPath.trim();
        if (raw.isEmpty()) {
            return null;
        }
        ensureFresh();
        int bang = raw.indexOf('!');
        if (bang > 0 && looksLikePath(raw)) {
            String left = raw.substring(0, bang);
            String entry = raw.substring(bang + 1);
            if (!entry.isEmpty()) {
                Path archive = safePath(left);
                LookupResult fromJar = readArchiveEntry(archive, entry);
                if (fromJar != null) {
                    return fromJar;
                }
            }
        }
        if (looksLikePath(raw)) {
            Path direct = safePath(raw);
            if (direct != null && Files.exists(direct)) {
                byte[] data = BytecodeCache.read(direct);
                if (data != null && data.length > 0) {
                    return new LookupResult(data, direct.toString(), null, direct.toString());
                }
            }
        }
        String className = normalizeClassName(raw);
        if (className == null && looksLikePath(raw)) {
            className = JarUtil.resolveClassNameFromPath(raw);
        }
        if (className == null) {
            return null;
        }
        String key = negativeKey(className, preferJarId);
        LookupResult cached = getPositive(key);
        if (cached != null) {
            return cached;
        }
        if (isNegative(key)) {
            return null;
        }
        LookupResult result = findClassInternal(className, preferJarId);
        if (result != null) {
            putPositive(key, result);
            return result;
        }
        markNegative(key);
        return null;
    }

    public static LookupResult findClass(String className, Integer preferJarId) {
        if (className == null) {
            return null;
        }
        String normalized = normalizeClassName(className);
        if (normalized == null) {
            return null;
        }
        ensureFresh();
        String key = negativeKey(normalized, preferJarId);
        LookupResult cached = getPositive(key);
        if (cached != null) {
            return cached;
        }
        if (isNegative(key)) {
            return null;
        }
        LookupResult result = findClassInternal(normalized, preferJarId);
        if (result != null) {
            putPositive(key, result);
            return result;
        }
        markNegative(key);
        return null;
    }

    private static LookupResult findClassInternal(String className, Integer preferJarId) {
        Path path = ClassIndex.resolveClassFile(className, preferJarId);
        if (path != null && Files.exists(path)) {
            byte[] data = BytecodeCache.read(path);
            if (data != null && data.length > 0) {
                return new LookupResult(data, path.toString(), null, path.toString());
            }
        }
        boolean isJdk = isJdkClass(className);
        if (isJdk) {
            LookupResult runtime = fallbackRuntime(className);
            if (runtime != null) {
                return runtime;
            }
        }
        if (isClasspathFallbackEnabled()) {
            ExternalClassIndex.ClassLocation external = ExternalClassIndex.findClass(className);
            if (external != null) {
                LookupResult externalResult = readExternal(external);
                if (externalResult != null) {
                    logger.debug("class lookup fallback external: {}", className);
                    return externalResult;
                }
            }
        }
        if (!isJdk) {
            return fallbackRuntime(className);
        }
        return null;
    }

    private static LookupResult fallbackRuntime(String className) {
        RuntimeClassResolver.ResolvedClass runtime = RuntimeClassResolver.resolve(className);
        if (runtime != null && runtime.getClassFile() != null) {
            Path runtimePath = runtime.getClassFile();
            if (Files.exists(runtimePath)) {
                byte[] data = BytecodeCache.read(runtimePath);
                if (data != null && data.length > 0) {
                    logger.debug("class lookup fallback runtime: {}", className);
                    return new LookupResult(data, runtimePath.toString(), null, runtimePath.toString());
                }
            }
        }
        return null;
    }

    private static LookupResult readExternal(ExternalClassIndex.ClassLocation external) {
        if (external == null || external.getArchive() == null) {
            return null;
        }
        Path archive = external.getArchive();
        String entryName = external.getEntryName();
        if (entryName == null) {
            byte[] data = BytecodeCache.read(archive);
            if (data == null || data.length == 0) {
                return null;
            }
            return new LookupResult(data, archive.toString(), null, archive.toString());
        }
        return readArchiveEntry(archive, entryName);
    }

    private static LookupResult readArchiveEntry(Path archive, String entryName) {
        if (archive == null || entryName == null || entryName.isEmpty()) {
            return null;
        }
        if (!Files.exists(archive)) {
            return null;
        }
        String cacheKey = buildArchiveCacheKey(archive, entryName);
        LookupResult cached = getPositive(cacheKey);
        if (cached != null) {
            return cached;
        }
        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            ZipEntry entry = zipFile.getEntry(entryName);
            if (entry == null) {
                return null;
            }
            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                byte[] data = IOUtil.readBytes(inputStream);
                if (data == null || data.length == 0) {
                    return null;
                }
                String trace = archive.toString() + "!" + entryName;
                LookupResult result = new LookupResult(data, archive.toString(), entryName, trace);
                putPositive(cacheKey, result);
                return result;
            }
        } catch (Exception ex) {
            logger.debug("read archive entry failed: {}!{}: {}", archive, entryName, ex.toString());
            return null;
        }
    }

    private static boolean isClasspathFallbackEnabled() {
        String raw = System.getProperty(FALLBACK_PROP);
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        String v = raw.trim().toLowerCase();
        if ("0".equals(v) || "false".equals(v) || "no".equals(v) || "off".equals(v)) {
            return false;
        }
        return true;
    }

    private static void ensureFresh() {
        long buildSeq = DatabaseManager.getBuildSeq();
        long rootSeq = RuntimeClassResolver.getRootSeq();
        if (buildSeq == lastBuildSeq && rootSeq == lastRootSeq) {
            return;
        }
        synchronized (LOCK) {
            if (buildSeq == lastBuildSeq && rootSeq == lastRootSeq) {
                return;
            }
            NEGATIVE.clear();
            POSITIVE.clear();
            positiveBytes = 0L;
            positiveMaxBytes = resolvePositiveMaxBytes();
            lastBuildSeq = buildSeq;
            lastRootSeq = rootSeq;
        }
    }

    private static boolean isNegative(String key) {
        synchronized (LOCK) {
            return NEGATIVE.containsKey(key);
        }
    }

    private static void markNegative(String key) {
        if (key == null) {
            return;
        }
        synchronized (LOCK) {
            NEGATIVE.put(key, Boolean.TRUE);
        }
    }

    private static String negativeKey(String className, Integer preferJarId) {
        if (preferJarId == null) {
            return className + "#any";
        }
        return className + "#" + preferJarId;
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

    private static boolean looksLikePath(String value) {
        if (value == null) {
            return false;
        }
        return value.contains(":") || value.contains("\\") || value.startsWith("/");
    }

    private static String normalizeClassName(String raw) {
        if (raw == null) {
            return null;
        }
        String name = raw.trim();
        if (name.isEmpty()) {
            return null;
        }
        if (looksLikePath(name)) {
            return null;
        }
        String normalizedPath = name.replace('\\', '/');
        if (normalizedPath.contains("BOOT-INF/classes/")
                || normalizedPath.contains("WEB-INF/classes/")) {
            String resolved = JarUtil.resolveClassNameFromPath(normalizedPath);
            if (resolved != null && !resolved.isEmpty()) {
                return resolved;
            }
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

    private static boolean isJdkClass(String normalized) {
        if (normalized == null) {
            return false;
        }
        return normalized.startsWith("java/")
                || normalized.startsWith("javax/")
                || normalized.startsWith("jdk/")
                || normalized.startsWith("sun/")
                || normalized.startsWith("com/sun/")
                || normalized.startsWith("org/w3c/")
                || normalized.startsWith("org/xml/");
    }

    private static String buildArchiveCacheKey(Path archive, String entryName) {
        if (archive == null || entryName == null) {
            return null;
        }
        return archive.toString() + "!" + entryName;
    }

    private static LookupResult getPositive(String key) {
        if (key == null) {
            return null;
        }
        synchronized (LOCK) {
            CachedLookup cached = POSITIVE.get(key);
            if (cached == null || cached.result == null) {
                return null;
            }
            return cached.result;
        }
    }

    private static void putPositive(String key, LookupResult result) {
        if (key == null || result == null || result.getBytes() == null) {
            return;
        }
        long maxBytes = positiveMaxBytes;
        int size = result.getBytes().length;
        if (size <= 0 || size > maxBytes) {
            return;
        }
        synchronized (LOCK) {
            CachedLookup prev = POSITIVE.put(key, new CachedLookup(result, size));
            if (prev != null) {
                positiveBytes -= prev.size;
            }
            positiveBytes += size;
            evictPositive();
        }
    }

    private static void evictPositive() {
        if (positiveBytes <= positiveMaxBytes) {
            return;
        }
        synchronized (LOCK) {
            if (positiveBytes <= positiveMaxBytes) {
                return;
            }
            for (java.util.Iterator<Map.Entry<String, CachedLookup>> it = POSITIVE.entrySet().iterator();
                 it.hasNext() && positiveBytes > positiveMaxBytes; ) {
                Map.Entry<String, CachedLookup> entry = it.next();
                CachedLookup cached = entry.getValue();
                if (cached != null) {
                    positiveBytes -= cached.size;
                }
                it.remove();
            }
        }
    }

    private static long resolvePositiveMaxBytes() {
        long half = BytecodeCache.getMaxBytes() / 2;
        if (half < POSITIVE_MIN_BYTES) {
            return POSITIVE_MIN_BYTES;
        }
        if (half > POSITIVE_MAX_BYTES) {
            return POSITIVE_MAX_BYTES;
        }
        return half;
    }

    private static final class CachedLookup {
        private final LookupResult result;
        private final int size;

        private CachedLookup(LookupResult result, int size) {
            this.result = result;
            this.size = size;
        }
    }

    public static final class LookupResult {
        private final byte[] bytes;
        private final String externalPath;
        private final String internalPath;
        private final String tracePath;

        private LookupResult(byte[] bytes, String externalPath, String internalPath, String tracePath) {
            this.bytes = bytes;
            this.externalPath = externalPath;
            this.internalPath = internalPath;
            this.tracePath = tracePath;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public String getExternalPath() {
            return externalPath;
        }

        public String getInternalPath() {
            return internalPath;
        }

        public String getTracePath() {
            return tracePath == null ? externalPath : tracePath;
        }
    }
}
