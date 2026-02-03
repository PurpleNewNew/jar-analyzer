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
    private static final int NEGATIVE_MAX = 4096;
    private static final Map<String, Boolean> NEGATIVE =
            new LinkedHashMap<String, Boolean>(NEGATIVE_MAX, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > NEGATIVE_MAX;
                }
            };
    private static final Object LOCK = new Object();
    private static volatile long lastBuildSeq = -1;
    private static volatile long lastRootSeq = -1;

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
        if (isNegative(key)) {
            return null;
        }
        LookupResult result = findClassInternal(className, preferJarId);
        if (result != null) {
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
        if (isNegative(key)) {
            return null;
        }
        LookupResult result = findClassInternal(normalized, preferJarId);
        if (result != null) {
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
        if (!isClasspathFallbackEnabled()) {
            return null;
        }
        ExternalClassIndex.ClassLocation external = ExternalClassIndex.findClass(className);
        if (external == null) {
            return null;
        }
        LookupResult externalResult = readExternal(external);
        if (externalResult != null) {
            logger.debug("class lookup fallback external: {}", className);
        }
        return externalResult;
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
                return new LookupResult(data, archive.toString(), entryName, trace);
            }
        } catch (Exception ignored) {
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
        } catch (Exception ignored) {
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
