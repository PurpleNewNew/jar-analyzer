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
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ExternalClassIndex {
    private static final Logger logger = LogManager.getLogger();
    private static volatile Index cached;
    private static volatile long lastRootSeq = -1;
    private static volatile long lastBuildSeq = -1;

    private ExternalClassIndex() {
    }

    public static List<String> getClassesInPackage(String packageName) {
        Index index = ensureIndex();
        if (index == null) {
            return Collections.emptyList();
        }
        String normalized = normalizePackage(packageName);
        List<String> classes = index.packageIndex.get(normalized);
        if (classes == null || classes.isEmpty()) {
            return Collections.emptyList();
        }
        return classes;
    }

    public static ClassLocation findClass(String className) {
        Index index = ensureIndex();
        if (index == null) {
            return null;
        }
        String normalized = normalizeClassName(className);
        if (normalized.isEmpty()) {
            return null;
        }
        return index.classIndex.get(normalized);
    }

    private static Index ensureIndex() {
        long rootSeq = RuntimeClassResolver.getRootSeq();
        long buildSeq = DatabaseManager.getBuildSeq();
        Index snapshot = cached;
        if (snapshot != null && rootSeq == lastRootSeq && buildSeq == lastBuildSeq) {
            return snapshot;
        }
        synchronized (ExternalClassIndex.class) {
            if (cached != null && rootSeq == lastRootSeq && buildSeq == lastBuildSeq) {
                return cached;
            }
            cached = buildIndex();
            lastRootSeq = rootSeq;
            lastBuildSeq = buildSeq;
            return cached;
        }
    }

    private static Index buildIndex() {
        List<Path> archives = ClasspathRegistry.getClasspathArchives();
        if (archives == null || archives.isEmpty()) {
            return new Index(Collections.emptyMap(), Collections.emptyMap());
        }
        Map<String, ClassLocation> classIndex = new LinkedHashMap<>();
        Map<String, Set<String>> pkgIndex = new LinkedHashMap<>();
        for (Path archive : archives) {
            if (archive == null || !Files.exists(archive)) {
                continue;
            }
            String name = archive.getFileName() == null ? "" : archive.getFileName().toString().toLowerCase(Locale.ROOT);
            if (name.endsWith(".class")) {
                indexClassFile(archive, classIndex, pkgIndex);
            } else if (name.endsWith(".jar") || name.endsWith(".war") || name.endsWith(".jmod")) {
                indexArchive(archive, classIndex, pkgIndex);
            }
        }
        Map<String, List<String>> packages = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : pkgIndex.entrySet()) {
            List<String> list = new ArrayList<>(entry.getValue());
            Collections.sort(list);
            packages.put(entry.getKey(), list);
        }
        return new Index(classIndex, packages);
    }

    private static void indexClassFile(Path classFile,
                                       Map<String, ClassLocation> classIndex,
                                       Map<String, Set<String>> pkgIndex) {
        if (classFile == null) {
            return;
        }
        String internal = readClassInternalName(classFile);
        if (internal == null || internal.isEmpty()) {
            return;
        }
        if (CommonFilterUtil.isModuleInfoClassName(internal)) {
            return;
        }
        if (!classIndex.containsKey(internal)) {
            classIndex.put(internal, new ClassLocation(classFile, null));
        }
        addPackageEntry(internal, pkgIndex);
    }

    private static void indexArchive(Path archive,
                                     Map<String, ClassLocation> classIndex,
                                     Map<String, Set<String>> pkgIndex) {
        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry == null || entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                String internal = toInternalName(name);
                if (internal == null || internal.isEmpty()) {
                    continue;
                }
                if (CommonFilterUtil.isModuleInfoClassName(internal)) {
                    continue;
                }
                if (!classIndex.containsKey(internal)) {
                    classIndex.put(internal, new ClassLocation(archive, name));
                }
                addPackageEntry(internal, pkgIndex);
            }
        } catch (Exception ex) {
            logger.debug("index archive failed: {} {}", archive, ex.toString());
        }
    }

    private static void addPackageEntry(String internalName, Map<String, Set<String>> pkgIndex) {
        String pkg = packageOf(internalName);
        String dotPkg = pkg.replace('/', '.');
        if (!pkgIndex.containsKey(dotPkg)) {
            pkgIndex.put(dotPkg, new LinkedHashSet<>());
        }
        pkgIndex.get(dotPkg).add(internalName);
    }

    private static String readClassInternalName(Path classFile) {
        if (classFile == null || !Files.exists(classFile)) {
            return null;
        }
        try (InputStream in = Files.newInputStream(classFile)) {
            ClassReader reader = new ClassReader(in);
            return reader.getClassName();
        } catch (Exception ex) {
            return null;
        }
    }

    private static String toInternalName(String entryName) {
        if (entryName == null || entryName.isEmpty()) {
            return null;
        }
        String normalized = entryName.replace('\\', '/');
        if (normalized.startsWith("META-INF/versions/")) {
            return null;
        }
        if (normalized.startsWith("classes/")) {
            normalized = normalized.substring("classes/".length());
        }
        if (normalized.startsWith("BOOT-INF/classes/")) {
            normalized = normalized.substring("BOOT-INF/classes/".length());
        }
        if (normalized.startsWith("WEB-INF/classes/")) {
            normalized = normalized.substring("WEB-INF/classes/".length());
        }
        if (!normalized.endsWith(".class")) {
            return null;
        }
        if (normalized.endsWith("module-info.class")) {
            return null;
        }
        String internal = normalized.substring(0, normalized.length() - ".class".length());
        if (internal.startsWith("/")) {
            internal = internal.substring(1);
        }
        return internal;
    }

    private static String normalizePackage(String packageName) {
        if (packageName == null) {
            return "";
        }
        String normalized = packageName.trim();
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        normalized = normalized.replace('/', '.');
        return normalized;
    }

    private static String normalizeClassName(String className) {
        if (className == null) {
            return "";
        }
        String normalized = className.trim();
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - 6);
        }
        normalized = normalized.replace('.', '/');
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static String packageOf(String internalName) {
        if (internalName == null) {
            return "";
        }
        int idx = internalName.lastIndexOf('/');
        if (idx <= 0) {
            return "";
        }
        return internalName.substring(0, idx);
    }

    public static final class ClassLocation {
        private final Path archive;
        private final String entryName;

        private ClassLocation(Path archive, String entryName) {
            this.archive = archive;
            this.entryName = entryName;
        }

        public Path getArchive() {
            return archive;
        }

        public String getEntryName() {
            return entryName;
        }
    }

    private static final class Index {
        private final Map<String, ClassLocation> classIndex;
        private final Map<String, List<String>> packageIndex;

        private Index(Map<String, ClassLocation> classIndex,
                      Map<String, List<String>> packageIndex) {
            this.classIndex = classIndex == null ? Collections.emptyMap() : classIndex;
            this.packageIndex = packageIndex == null ? Collections.emptyMap() : packageIndex;
        }
    }
}
