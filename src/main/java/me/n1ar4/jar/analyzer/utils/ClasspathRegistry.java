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
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;

import javax.swing.JTextField;
import java.io.File;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class ClasspathRegistry {
    private static final Logger logger = LogManager.getLogger();
    private static final String EXTRA_PROP = "jar.analyzer.classpath.extra";
    private static final String BOOT_INF = "BOOT-INF";
    private static final String WEB_INF = "WEB-INF";
    private static final String CLASSES_DIR = "classes";
    private static final String RUNTIME_CACHE_DIR = "runtime-cache";
    private static volatile List<Path> cachedArchives = Collections.emptyList();
    private static volatile List<Path> cachedEntries = Collections.emptyList();
    private static volatile String cachedClasspath = "";
    private static volatile long lastRootSeq = -1;
    private static volatile long lastBuildSeq = -1;

    private ClasspathRegistry() {
    }

    public static List<Path> getClasspathArchives() {
        ensureFresh();
        return cachedArchives;
    }

    public static List<Path> getClasspathEntries() {
        ensureFresh();
        return cachedEntries;
    }

    public static String getClasspathString() {
        ensureFresh();
        return cachedClasspath;
    }

    private static void ensureFresh() {
        long rootSeq = RuntimeClassResolver.getRootSeq();
        long buildSeq = DatabaseManager.getBuildSeq();
        if (rootSeq == lastRootSeq && buildSeq == lastBuildSeq && cachedArchives != null) {
            return;
        }
        synchronized (ClasspathRegistry.class) {
            if (rootSeq == lastRootSeq && buildSeq == lastBuildSeq && cachedArchives != null) {
                return;
            }
            rebuild(rootSeq, buildSeq);
        }
    }

    private static void rebuild(long rootSeq, long buildSeq) {
        List<Path> archives = resolveArchives();
        cachedArchives = archives.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(archives);
        List<Path> entries = resolveClasspathEntries(archives);
        cachedEntries = entries.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(entries);
        cachedClasspath = buildClasspathString(entries);
        lastRootSeq = rootSeq;
        lastBuildSeq = buildSeq;
    }

    private static List<Path> resolveArchives() {
        Set<Path> out = new LinkedHashSet<>();
        Path root = resolveRootPath();
        if (root != null && Files.exists(root)) {
            try {
                ClasspathResolver.ClasspathGraph graph = ClasspathResolver.resolveClasspathGraph(root);
                if (graph != null) {
                    out.addAll(graph.getOrderedArchives());
                }
            } catch (Throwable t) {
                logger.debug("resolve classpath graph failed: {}", t.toString());
            }
        }
        List<Path> runtime = RuntimeClassResolver.resolveRuntimeArchivesForClasspath();
        if (runtime != null && !runtime.isEmpty()) {
            out.addAll(runtime);
        }
        return new ArrayList<>(out);
    }

    private static List<Path> resolveClasspathEntries(List<Path> archives) {
        LinkedHashSet<Path> out = new LinkedHashSet<>();
        Path root = resolveRootPath();
        addRootEntries(out, root);
        if (archives != null) {
            for (Path path : archives) {
                if (isClassFile(path)) {
                    continue;
                }
                addClasspathEntry(out, path);
            }
        }
        collectExtraEntries(out);
        Set<String> allowedHashes = buildArchiveHashes(archives, root);
        boolean includeRuntimeCache = containsRuntimeArchives(archives);
        collectTempClassRoots(out, allowedHashes, includeRuntimeCache);
        return new ArrayList<>(out);
    }

    private static Path resolveRootPath() {
        try {
            JTextField field = MainForm.getInstance().getFileText();
            if (field == null) {
                return null;
            }
            String text = field.getText();
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            return Paths.get(text.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String buildClasspathString(List<Path> entries) {
        if (entries == null || entries.isEmpty()) {
            return "";
        }
        String sep = File.pathSeparator;
        StringBuilder sb = new StringBuilder();
        for (Path path : entries) {
            if (path == null) {
                continue;
            }
            String value;
            try {
                value = path.toAbsolutePath().toString();
            } catch (Exception ex) {
                continue;
            }
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            if (!shouldIncludeClasspathEntry(path)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(sep);
            }
            sb.append(value);
        }
        return sb.toString();
    }

    private static void addRootEntries(Set<Path> out, Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        if (Files.isDirectory(root)) {
            addPath(out, root);
            addPath(out, root.resolve(BOOT_INF).resolve(CLASSES_DIR));
            addPath(out, root.resolve(WEB_INF).resolve(CLASSES_DIR));
            collectLibArchives(out, root);
            return;
        }
        addClasspathEntry(out, root);
        Path parent = root.getParent();
        if (parent != null) {
            addPath(out, parent.resolve(BOOT_INF).resolve(CLASSES_DIR));
            addPath(out, parent.resolve(WEB_INF).resolve(CLASSES_DIR));
            collectLibArchives(out, parent);
        }
    }

    private static void collectExtraEntries(Set<Path> out) {
        String extra = System.getProperty(EXTRA_PROP);
        if (extra == null || extra.trim().isEmpty()) {
            return;
        }
        String sep = File.pathSeparator;
        String[] parts = extra.split(Pattern.quote(sep));
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) {
                continue;
            }
            Path path = Paths.get(part.trim());
            if (!Files.exists(path)) {
                continue;
            }
            if (Files.isDirectory(path)) {
                addPath(out, path);
                addPath(out, path.resolve(BOOT_INF).resolve(CLASSES_DIR));
                addPath(out, path.resolve(WEB_INF).resolve(CLASSES_DIR));
                collectLibArchives(out, path);
            } else {
                addClasspathEntry(out, path);
            }
        }
    }

    private static void collectTempClassRoots(Set<Path> out,
                                              Set<String> allowedHashes,
                                              boolean includeRuntimeCache) {
        Path tempRoot = Paths.get(Const.tempDir);
        if (!Files.isDirectory(tempRoot)) {
            return;
        }
        if (includeRuntimeCache) {
            Path runtimeCache = tempRoot.resolve(RUNTIME_CACHE_DIR);
            addPath(out, runtimeCache);
        }
        if (allowedHashes == null || allowedHashes.isEmpty()) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempRoot)) {
            for (Path child : stream) {
                if (!Files.isDirectory(child)) {
                    continue;
                }
                String name = child.getFileName().toString();
                if (!isJarRootName(name)) {
                    continue;
                }
                if (!matchesAllowedHash(name, allowedHashes)) {
                    continue;
                }
                addPath(out, child);
                addPath(out, child.resolve(BOOT_INF).resolve(CLASSES_DIR));
                addPath(out, child.resolve(WEB_INF).resolve(CLASSES_DIR));
                collectLibArchives(out, child);
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean containsRuntimeArchives(List<Path> archives) {
        if (archives == null || archives.isEmpty()) {
            return false;
        }
        for (Path path : archives) {
            if (path == null) {
                continue;
            }
            String name = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
            if (name.endsWith(".jmod") || "rt.jar".equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> buildArchiveHashes(List<Path> archives, Path root) {
        LinkedHashSet<String> hashes = new LinkedHashSet<>();
        if (archives != null) {
            for (Path path : archives) {
                addArchiveHash(hashes, path);
            }
        }
        if (root != null && Files.exists(root) && !Files.isDirectory(root)) {
            addArchiveHash(hashes, root);
        }
        return hashes;
    }

    private static void addArchiveHash(Set<String> hashes, Path path) {
        if (hashes == null || path == null) {
            return;
        }
        try {
            Path abs = path.toAbsolutePath().normalize();
            String text = abs.toString();
            if (text.isEmpty()) {
                return;
            }
            hashes.add(Integer.toHexString(text.hashCode()));
        } catch (Exception ignored) {
        }
    }

    private static boolean matchesAllowedHash(String dirName, Set<String> allowedHashes) {
        if (dirName == null || dirName.isEmpty() || allowedHashes == null || allowedHashes.isEmpty()) {
            return false;
        }
        int lastDash = dirName.lastIndexOf('-');
        if (lastDash < 0 || lastDash >= dirName.length() - 1) {
            return false;
        }
        String suffix = dirName.substring(lastDash + 1);
        if (suffix.isEmpty()) {
            return false;
        }
        return allowedHashes.contains(suffix);
    }

    private static void collectLibArchives(Set<Path> out, Path baseDir) {
        if (baseDir == null || !Files.isDirectory(baseDir)) {
            return;
        }
        collectArchiveChildren(out, baseDir.resolve("lib"), 2);
        collectArchiveChildren(out, baseDir.resolve("libs"), 2);
        collectArchiveChildren(out, baseDir.resolve(BOOT_INF).resolve("lib"), 2);
        collectArchiveChildren(out, baseDir.resolve(WEB_INF).resolve("lib"), 2);
    }

    private static void collectArchiveChildren(Set<Path> out, Path dir, int depth) {
        if (dir == null || !Files.isDirectory(dir)) {
            return;
        }
        int maxDepth = Math.max(1, depth);
        try (java.util.stream.Stream<Path> stream = Files.walk(dir, maxDepth)) {
            stream.filter(Files::isRegularFile)
                    .filter(ClasspathRegistry::isArchiveFile)
                    .forEach(path -> addPath(out, path));
        } catch (Exception ignored) {
        }
    }

    private static void addClasspathEntry(Set<Path> out, Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        if (Files.isDirectory(path)) {
            addPath(out, path);
            return;
        }
        if (isClassFile(path)) {
            Path root = resolveClassRoot(path);
            if (root != null) {
                addPath(out, root);
            } else {
                Path parent = path.getParent();
                if (parent != null) {
                    addPath(out, parent);
                }
            }
            return;
        }
        if (isArchiveFile(path)) {
            addPath(out, path);
        }
    }

    private static void addPath(Set<Path> out, Path path) {
        if (path == null) {
            return;
        }
        Path normalized;
        try {
            normalized = path.toAbsolutePath().normalize();
        } catch (Exception ex) {
            return;
        }
        if (!Files.exists(normalized)) {
            return;
        }
        out.add(normalized);
    }

    private static boolean isArchiveFile(Path path) {
        if (path == null) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".jar") || name.endsWith(".war")
                || name.endsWith(".jmod") || name.endsWith(".zip")
                || name.endsWith(".ear");
    }

    private static boolean isClassFile(Path path) {
        if (path == null) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".class");
    }

    private static boolean shouldIncludeClasspathEntry(Path path) {
        if (path == null) {
            return false;
        }
        if (Files.isDirectory(path)) {
            return true;
        }
        return isArchiveFile(path);
    }

    private static Path resolveClassRoot(Path classFile) {
        if (classFile == null || !Files.exists(classFile)) {
            return null;
        }
        String internal = readClassInternalName(classFile);
        if (internal == null || internal.isEmpty()) {
            return null;
        }
        int segments = internal.split("/").length - 1;
        Path root = classFile.toAbsolutePath().normalize().getParent();
        for (int i = 0; i < segments && root != null; i++) {
            root = root.getParent();
        }
        return root;
    }

    private static String readClassInternalName(Path classFile) {
        try (InputStream in = Files.newInputStream(classFile)) {
            return new ClassReader(in).getClassName();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isJarRootName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (name.startsWith("jar-")) {
            return true;
        }
        int start = 0;
        if (name.charAt(0) == '-') {
            if (name.length() == 1) {
                return false;
            }
            start = 1;
        }
        for (int i = start; i < name.length(); i++) {
            if (!Character.isDigit(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
