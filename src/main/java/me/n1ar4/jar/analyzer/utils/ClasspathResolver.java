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

import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ClasspathResolver {
    private static final Logger logger = LogManager.getLogger();
    private static final String EXTRA_PROP = "jar.analyzer.classpath.extra";
    private static final String INCLUDE_MANIFEST_PROP = "jar.analyzer.classpath.includeManifest";
    private static final String INCLUDE_SIBLING_LIB_PROP = "jar.analyzer.classpath.includeSiblingLib";
    private static final String INCLUDE_NESTED_LIB_PROP = "jar.analyzer.classpath.includeNestedLib";
    private static final String SCAN_DEPTH_PROP = "jar.analyzer.classpath.scanDepth";
    private static final String NESTED_CACHE_DIR = "classpath-nested";
    private static final java.util.Map<String, Path> NESTED_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private ClasspathResolver() {
    }

    public static List<String> resolveInputArchives(Path inputPath, Path rtPath, boolean extended) {
        return resolveInputArchives(inputPath, rtPath, extended, isNestedLibEnabled());
    }

    public static List<String> resolveInputArchives(Path inputPath,
                                                    Path rtPath,
                                                    boolean extended,
                                                    boolean includeNested) {
        if (inputPath == null) {
            return Collections.emptyList();
        }
        Set<Path> result = new LinkedHashSet<>();
        collectInputPath(inputPath, result, extended, includeNested);
        if (extended) {
            collectExtraClasspath(result);
        }
        if (rtPath != null && Files.exists(rtPath)) {
            result.add(rtPath.toAbsolutePath().normalize());
        }
        List<String> out = new ArrayList<>();
        for (Path p : result) {
            out.add(p.toAbsolutePath().toString());
        }
        return out;
    }

    public static List<Path> resolveUserArchives(String rootPath) {
        if (rootPath == null || rootPath.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Set<Path> result = new LinkedHashSet<>();
        collectInputPath(Paths.get(rootPath), result, true, isNestedLibEnabled());
        collectExtraClasspath(result);
        return new ArrayList<>(result);
    }

    private static void collectInputPath(Path inputPath,
                                         Set<Path> result,
                                         boolean extended,
                                         boolean includeNested) {
        if (inputPath == null) {
            return;
        }
        Path normalized = inputPath.toAbsolutePath().normalize();
        if (!Files.exists(normalized)) {
            return;
        }
        if (Files.isDirectory(normalized)) {
            scanDirectory(normalized, result, resolveScanDepth());
            return;
        }
        if (isArchive(normalized) || isClassFile(normalized)) {
            result.add(normalized);
            if (extended && isArchive(normalized)) {
                if (isManifestEnabled()) {
                    collectManifestClasspath(normalized, result);
                }
                if (isSiblingLibEnabled()) {
                    collectSiblingLibs(normalized, result);
                }
                if (includeNested) {
                    collectNestedLibs(normalized, result);
                }
            }
        }
    }

    private static void collectExtraClasspath(Set<Path> result) {
        String extra = System.getProperty(EXTRA_PROP);
        if (extra == null || extra.trim().isEmpty()) {
            return;
        }
        String sep = File.pathSeparator;
        String[] parts = extra.split(java.util.regex.Pattern.quote(sep));
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) {
                continue;
            }
            Path path = Paths.get(part.trim());
            if (!Files.exists(path)) {
                continue;
            }
            if (Files.isDirectory(path)) {
                scanDirectory(path, result, resolveScanDepth());
            } else if (isArchive(path) || isClassFile(path)) {
                result.add(path.toAbsolutePath().normalize());
            }
        }
    }

    private static void collectManifestClasspath(Path archive, Set<Path> result) {
        try (JarFile jarFile = new JarFile(archive.toFile())) {
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                return;
            }
            String raw = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
            if (raw == null || raw.trim().isEmpty()) {
                return;
            }
            String[] parts = raw.trim().split("\\s+");
            Path base = archive.getParent();
            for (String part : parts) {
                if (part == null || part.trim().isEmpty()) {
                    continue;
                }
                if (part.startsWith("http:") || part.startsWith("https:") || part.startsWith("file:")) {
                    continue;
                }
                Path dep = base == null ? Paths.get(part) : base.resolve(part);
                if (Files.exists(dep) && (isArchive(dep) || isClassFile(dep))) {
                    result.add(dep.toAbsolutePath().normalize());
                }
            }
        } catch (Exception ex) {
            logger.debug("manifest classpath parse failed: {}", ex.toString());
        }
    }

    private static void collectSiblingLibs(Path archive, Set<Path> result) {
        Path parent = archive.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            return;
        }
        Path lib = parent.resolve("lib");
        Path libs = parent.resolve("libs");
        if (Files.isDirectory(lib)) {
            scanDirectory(lib, result, 2);
        }
        if (Files.isDirectory(libs)) {
            scanDirectory(libs, result, 2);
        }
    }

    private static void collectNestedLibs(Path archive, Set<Path> result) {
        if (archive == null || !Files.exists(archive)) {
            return;
        }
        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry == null) {
                    continue;
                }
                String name = entry.getName();
                if (name == null || !name.endsWith(".jar")) {
                    continue;
                }
                if (!isLikelyNestedLib(name)) {
                    continue;
                }
                Path nested = extractNestedJar(archive, zipFile, entry);
                if (nested != null && Files.exists(nested)) {
                    result.add(nested.toAbsolutePath().normalize());
                }
            }
        } catch (Exception ex) {
            logger.debug("collect nested libs failed: {}", ex.toString());
        }
    }

    private static boolean isLikelyNestedLib(String name) {
        if (name.startsWith("BOOT-INF/lib/") || name.startsWith("WEB-INF/lib/")) {
            return true;
        }
        if (name.startsWith("lib/") || name.contains("/lib/")) {
            return true;
        }
        return name.startsWith("META-INF/lib/");
    }

    private static Path extractNestedJar(Path archive, ZipFile zipFile, ZipEntry entry) {
        String key = archive.toAbsolutePath() + "!" + entry.getName();
        Path cached = NESTED_CACHE.get(key);
        if (cached != null && Files.exists(cached)) {
            return cached;
        }
        Path out = buildNestedJarPath(key, entry.getName());
        try {
            Path parent = out.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                Files.copy(inputStream, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            NESTED_CACHE.put(key, out);
            return out;
        } catch (Exception ex) {
            logger.debug("extract nested jar failed: {}", ex.toString());
            return null;
        }
    }

    private static Path buildNestedJarPath(String key, String entryName) {
        String fileName = entryName == null ? "nested.jar" : Paths.get(entryName).getFileName().toString();
        String safe = Integer.toHexString(key.hashCode());
        return Paths.get(Const.tempDir, NESTED_CACHE_DIR, safe + "-" + fileName);
    }

    private static void scanDirectory(Path root, Set<Path> result, int depth) {
        if (root == null || !Files.isDirectory(root)) {
            return;
        }
        try (java.util.stream.Stream<Path> stream = Files.walk(root, Math.max(1, depth))) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> isArchive(p) || isClassFile(p))
                    .map(p -> p.toAbsolutePath().normalize())
                    .forEach(result::add);
        } catch (Exception ex) {
            logger.debug("scan classpath dir failed: {}", ex.toString());
        }
    }

    private static boolean isArchive(Path path) {
        if (path == null) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".jar") || name.endsWith(".war");
    }

    private static boolean isClassFile(Path path) {
        if (path == null) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".class");
    }

    private static boolean isManifestEnabled() {
        String raw = System.getProperty(INCLUDE_MANIFEST_PROP);
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    private static boolean isSiblingLibEnabled() {
        String raw = System.getProperty(INCLUDE_SIBLING_LIB_PROP);
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    private static boolean isNestedLibEnabled() {
        String raw = System.getProperty(INCLUDE_NESTED_LIB_PROP);
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    private static int resolveScanDepth() {
        String raw = System.getProperty(SCAN_DEPTH_PROP);
        if (raw == null || raw.trim().isEmpty()) {
            return 6;
        }
        try {
            int val = Integer.parseInt(raw.trim());
            if (val < 1) {
                return 1;
            }
            if (val > 12) {
                return 12;
            }
            return val;
        } catch (Exception ignored) {
            return 6;
        }
    }
}
