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

import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.JTextField;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class RuntimeClassResolver {
    private static final Logger logger = LogManager.getLogger();
    private static final String CACHE_DIR = "runtime-cache";
    private static final String NESTED_DIR = "runtime-nested";
    private static final Map<String, ResolvedClass> RUNTIME_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, ResolvedClass> USER_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> NEGATIVE = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<String, Path> NESTED_JAR_CACHE = new ConcurrentHashMap<>();
    private static final AtomicLong ROOT_SEQ = new AtomicLong(0);

    private static volatile String lastRootKey = "";
    private static volatile List<Path> cachedUserArchives;
    private static volatile ClasspathResolver.ClasspathGraph cachedGraph;

    private RuntimeClassResolver() {
    }

    public static ResolvedClass resolve(String className) {
        String normalized = normalizeClassName(className);
        if (StringUtil.isNull(normalized)) {
            return null;
        }
        ensureRootContext();
        ResolvedClass cached = USER_CACHE.get(normalized);
        if (cached != null && Files.exists(cached.classFile)) {
            return cached;
        }
        cached = RUNTIME_CACHE.get(normalized);
        if (cached != null && Files.exists(cached.classFile)) {
            return cached;
        }
        if (NEGATIVE.contains(normalized)) {
            return null;
        }
        ResolvedClass resolved;
        boolean preferRuntime = isJdkClass(normalized);
        if (preferRuntime) {
            resolved = resolveFromRuntimeArchives(normalized);
            if (resolved != null) {
                RUNTIME_CACHE.put(normalized, resolved);
                return resolved;
            }
            resolved = resolveFromUserArchives(normalized);
            if (resolved != null) {
                USER_CACHE.put(normalized, resolved);
                return resolved;
            }
        } else {
            resolved = resolveFromUserArchives(normalized);
            if (resolved != null) {
                USER_CACHE.put(normalized, resolved);
                return resolved;
            }
            resolved = resolveFromRuntimeArchives(normalized);
            if (resolved != null) {
                RUNTIME_CACHE.put(normalized, resolved);
                return resolved;
            }
        }
        NEGATIVE.add(normalized);
        return null;
    }

    public static String getJarName(String className) {
        String normalized = normalizeClassName(className);
        if (StringUtil.isNull(normalized)) {
            return null;
        }
        ResolvedClass cached = USER_CACHE.get(normalized);
        if (cached == null) {
            cached = RUNTIME_CACHE.get(normalized);
        }
        if (cached == null) {
            return null;
        }
        return cached.jarName;
    }

    private static void ensureRootContext() {
        String key = buildRootKey();
        if (key == null) {
            key = "";
        }
        if (key.equals(lastRootKey)) {
            return;
        }
        USER_CACHE.clear();
        NEGATIVE.clear();
        cachedUserArchives = null;
        cachedGraph = null;
        NESTED_JAR_CACHE.clear();
        ROOT_SEQ.incrementAndGet();
        lastRootKey = key;
    }

    public static long getRootSeq() {
        ensureRootContext();
        return ROOT_SEQ.get();
    }

    private static String buildRootKey() {
        String root = safeGetRootPath();
        String rt = safeGetRtPath();
        if (root == null) {
            root = "";
        }
        if (rt == null) {
            rt = "";
        }
        String extra = System.getProperty("jar.analyzer.classpath.extra", "");
        String includeManifest = System.getProperty("jar.analyzer.classpath.includeManifest", "");
        String includeSibling = System.getProperty("jar.analyzer.classpath.includeSiblingLib", "");
        String includeNested = System.getProperty("jar.analyzer.classpath.includeNestedLib", "");
        String scanDepth = System.getProperty("jar.analyzer.classpath.scanDepth", "");
        String conflict = System.getProperty(ClasspathResolver.CONFLICT_PROP, "");
        return root + "|" + rt + "|" + extra + "|" + includeManifest + "|" + includeSibling + "|"
                + includeNested + "|" + scanDepth + "|" + conflict;
    }

    private static ResolvedClass resolveFromRuntimeArchives(String className) {
        List<Path> archives = resolveRuntimeArchives();
        if (archives.isEmpty()) {
            return null;
        }
        for (Path archive : archives) {
            String jarName = archive.getFileName().toString();
            if (isClassFile(archive) && matchesClassFile(archive, className)) {
                return new ResolvedClass(archive, jarName);
            }
            String entryName = className + ".class";
            if (jarName.endsWith(".jmod")) {
                entryName = "classes/" + entryName;
            }
            ResolvedClass resolved = extractFromArchive(archive, className, entryName, jarName, false);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    private static ResolvedClass resolveFromUserArchives(String className) {
        List<Path> archives = resolveUserArchives();
        if (archives.isEmpty()) {
            return null;
        }
        String entryName = className + ".class";
        for (Path archive : archives) {
            String jarName = archive.getFileName().toString();
            if (isClassFile(archive) && matchesClassFile(archive, className)) {
                return new ResolvedClass(archive, jarName);
            }
            ResolvedClass resolved = extractFromArchive(archive, className, entryName, jarName, true);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    private static ResolvedClass extractFromArchive(Path archive,
                                                    String className,
                                                    String entryName,
                                                    String jarName,
                                                    boolean allowNested) {
        if (archive == null || entryName == null) {
            return null;
        }
        if (!Files.exists(archive)) {
            return null;
        }
        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            String multiReleaseEntry = resolveMultiReleaseEntry(zipFile, entryName);
            Path extracted = null;
            if (multiReleaseEntry != null) {
                extracted = extractClassEntry(zipFile, multiReleaseEntry, className);
            }
            if (extracted == null) {
                extracted = extractClassEntry(zipFile, entryName, className);
            }
            if (extracted != null) {
                return new ResolvedClass(extracted, jarName);
            }
            if (!entryName.startsWith("BOOT-INF/classes/")) {
                extracted = extractClassEntry(zipFile, "BOOT-INF/classes/" + entryName, className);
                if (extracted != null) {
                    return new ResolvedClass(extracted, jarName);
                }
            }
            if (!entryName.startsWith("WEB-INF/classes/")) {
                extracted = extractClassEntry(zipFile, "WEB-INF/classes/" + entryName, className);
                if (extracted != null) {
                    return new ResolvedClass(extracted, jarName);
                }
            }
            if (allowNested) {
                ResolvedClass nested = searchNestedJars(zipFile, archive, entryName, className);
                if (nested != null) {
                    return nested;
                }
            }
        } catch (Exception ex) {
            logger.warn("runtime extract failed: {} {}", archive, ex.getMessage());
        }
        return null;
    }

    private static ResolvedClass searchNestedJars(ZipFile zipFile,
                                                 Path archive,
                                                 String entryName,
                                                 String className) {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry == null) {
                continue;
            }
            String name = entry.getName();
            if (name == null || !name.endsWith(".jar")) {
                continue;
            }
            if (!isLikelyLibJar(name)) {
                continue;
            }
            Path nested = extractNestedJar(archive, zipFile, entry);
            if (nested == null) {
                continue;
            }
            String nestedJarName = nested.getFileName().toString();
            ResolvedClass resolved = extractFromArchive(nested, className, entryName, nestedJarName, false);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    private static String resolveMultiReleaseEntry(ZipFile zipFile, String entryName) {
        if (zipFile == null || entryName == null || entryName.trim().isEmpty()) {
            return null;
        }
        if (!isMultiRelease(zipFile)) {
            return null;
        }
        int runtime = resolveRuntimeMajor();
        if (runtime < 9) {
            return null;
        }
        for (int v = runtime; v >= 9; v--) {
            String candidate = "META-INF/versions/" + v + "/" + entryName;
            if (zipFile.getEntry(candidate) != null) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isMultiRelease(ZipFile zipFile) {
        try {
            ZipEntry entry = zipFile.getEntry("META-INF/MANIFEST.MF");
            if (entry == null) {
                return false;
            }
            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                java.util.jar.Manifest manifest = new java.util.jar.Manifest(inputStream);
                String val = manifest.getMainAttributes().getValue("Multi-Release");
                return val != null && "true".equalsIgnoreCase(val.trim());
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int resolveRuntimeMajor() {
        String spec = System.getProperty("java.specification.version");
        if (spec == null || spec.trim().isEmpty()) {
            return 8;
        }
        String trimmed = spec.trim();
        if (trimmed.startsWith("1.")) {
            trimmed = trimmed.substring(2);
        }
        int dot = trimmed.indexOf('.');
        if (dot > 0) {
            trimmed = trimmed.substring(0, dot);
        }
        try {
            int val = Integer.parseInt(trimmed);
            return val > 0 ? val : 8;
        } catch (Exception ignored) {
            return 8;
        }
    }

    private static boolean isLikelyLibJar(String name) {
        if (name.startsWith("BOOT-INF/lib/") || name.startsWith("WEB-INF/lib/")) {
            return true;
        }
        return name.startsWith("lib/") || name.contains("/lib/");
    }

    private static Path extractNestedJar(Path archive, ZipFile zipFile, ZipEntry entry) {
        String key = archive.toAbsolutePath() + "!" + entry.getName();
        Path cached = NESTED_JAR_CACHE.get(key);
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
                Files.copy(inputStream, out, StandardCopyOption.REPLACE_EXISTING);
            }
            NESTED_JAR_CACHE.put(key, out);
            return out;
        } catch (Exception ex) {
            logger.warn("extract nested jar failed: {} {}", archive, ex.getMessage());
            return null;
        }
    }

    private static Path extractClassEntry(ZipFile zipFile, String entryName, String className) {
        if (zipFile == null || entryName == null) {
            return null;
        }
        ZipEntry entry = zipFile.getEntry(entryName);
        if (entry == null) {
            return null;
        }
        Path out = buildCachePath(className);
        if (Files.exists(out)) {
            return out;
        }
        try {
            Path parent = out.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                Files.copy(inputStream, out, StandardCopyOption.REPLACE_EXISTING);
            }
            logger.info("runtime class extracted: {} -> {}", className, out);
            return out;
        } catch (Exception ex) {
            logger.warn("runtime extract failed: {} {}", entryName, ex.getMessage());
            return null;
        }
    }

    private static Path buildCachePath(String className) {
        String normalized = normalizeClassName(className);
        String pathPart = normalized.replace("/", String.valueOf(java.io.File.separatorChar));
        return Paths.get(Const.tempDir, CACHE_DIR, pathPart + ".class");
    }

    private static Path buildNestedJarPath(String key, String entryName) {
        String fileName = entryName == null ? "nested.jar" : Paths.get(entryName).getFileName().toString();
        String safe = Integer.toHexString(key.hashCode());
        return Paths.get(Const.tempDir, CACHE_DIR, NESTED_DIR, safe + "-" + fileName);
    }

    private static List<Path> resolveRuntimeArchives() {
        Set<Path> result = new LinkedHashSet<>();
        String rtPath = safeGetRtPath();
        if (!StringUtil.isNull(rtPath)) {
            Path rt = Paths.get(rtPath);
            if (Files.exists(rt)) {
                result.add(rt);
            }
        }

        String javaHome = System.getProperty("java.home");
        if (!StringUtil.isNull(javaHome)) {
            Path home = Paths.get(javaHome);
            Path rtJar = home.resolve(Paths.get("lib", "rt.jar"));
            if (Files.exists(rtJar)) {
                result.add(rtJar);
            }
            Path jmods = home.resolve("jmods");
            if (!Files.isDirectory(jmods) && home.getParent() != null) {
                jmods = home.getParent().resolve("jmods");
            }
            if (Files.isDirectory(jmods)) {
                try (java.util.stream.Stream<Path> stream = Files.list(jmods)) {
                    stream.filter(p -> p.getFileName().toString().endsWith(".jmod"))
                            .forEach(result::add);
                } catch (Exception ex) {
                    logger.warn("list jmods failed: {}", ex.getMessage());
                }
            }
        }
        return new ArrayList<>(result);
    }

    private static List<Path> resolveUserArchives() {
        List<Path> cached = cachedUserArchives;
        if (cached != null) {
            return cached;
        }
        String rootPath = safeGetRootPath();
        if (StringUtil.isNull(rootPath)) {
            cachedUserArchives = Collections.emptyList();
            return cachedUserArchives;
        }
        ClasspathResolver.ConflictStrategy strategy = ClasspathResolver.resolveConflictStrategy();
        if (strategy == ClasspathResolver.ConflictStrategy.FIRST) {
            List<Path> resolved = ClasspathResolver.resolveUserArchives(rootPath);
            cachedUserArchives = resolved.isEmpty() ? Collections.emptyList() : new ArrayList<>(resolved);
            cachedGraph = null;
            return cachedUserArchives;
        }
        ClasspathResolver.ClasspathGraph graph = cachedGraph;
        if (graph == null) {
            graph = ClasspathResolver.resolveClasspathGraph(Paths.get(rootPath));
            cachedGraph = graph;
        }
        List<Path> resolved = graph == null ? Collections.emptyList() : graph.getOrderedArchives();
        cachedUserArchives = resolved.isEmpty() ? Collections.emptyList() : new ArrayList<>(resolved);
        return cachedUserArchives;
    }

    private static boolean isArchiveFile(Path path) {
        if (path == null) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".jar") || name.endsWith(".war");
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

    private static boolean isClassFile(Path path) {
        if (path == null) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".class");
    }

    private static boolean matchesClassFile(Path path, String className) {
        if (path == null || className == null || className.trim().isEmpty()) {
            return false;
        }
        String normalized = normalizeClassName(className);
        if (normalized == null || normalized.trim().isEmpty()) {
            return false;
        }
        String target = normalized + ".class";
        String full = path.toAbsolutePath().normalize().toString().replace('\\', '/');
        return full.endsWith(target);
    }

    private static String safeGetRootPath() {
        try {
            JTextField fileText = MainForm.getInstance().getFileText();
            if (fileText == null) {
                return null;
            }
            return fileText.getText();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String safeGetRtPath() {
        try {
            JTextField rtText = MainForm.getInstance().getRtText();
            if (rtText == null) {
                return null;
            }
            return rtText.getText();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String normalizeClassName(String className) {
        if (className == null) {
            return null;
        }
        String normalized = className.trim();
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - 6);
        }
        if (normalized.contains(".")) {
            normalized = normalized.replace('.', '/');
        }
        return normalized;
    }

    public static final class ResolvedClass {
        private final Path classFile;
        private final String jarName;

        private ResolvedClass(Path classFile, String jarName) {
            this.classFile = classFile;
            this.jarName = jarName;
        }

        public Path getClassFile() {
            return classFile;
        }

        public String getJarName() {
            return jarName;
        }
    }
}
