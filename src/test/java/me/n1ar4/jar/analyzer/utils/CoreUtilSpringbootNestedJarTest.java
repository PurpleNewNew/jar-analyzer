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

import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreUtilSpringbootNestedJarTest {
    @Test
    void shouldAvoidRescanningSpringBootNestedLibsWhenClasspathAlreadyExpanded() throws Exception {
        Path jar = FixtureJars.springbootTestJar().toAbsolutePath().normalize();
        List<String> archives = ClasspathResolver.resolveInputArchives(jar, null, true, true);
        assertTrue(archives.size() > 1);

        LinkedHashMap<String, Integer> jarIdMap = new LinkedHashMap<>();
        for (int i = 0; i < archives.size(); i++) {
            jarIdMap.put(archives.get(i), i + 1);
        }

        List<ClassFileEntity> classFiles = CoreUtil.getAllClassesFromJars(
                archives,
                jarIdMap,
                new ArrayList<>(),
                true
        );

        int expectedRootClasses = countRootArchiveClasses(jar);
        int expectedNestedClasses = countNestedArchiveClasses(jar);
        Integer rootJarId = jarIdMap.get(jar.toString());
        assertNotNull(rootJarId);

        long rootJarClasses = classFiles.stream()
                .filter(classFile -> classFile != null && rootJarId.equals(classFile.getJarId()))
                .count();

        assertEquals(expectedRootClasses, rootJarClasses);
        assertEquals(expectedRootClasses + expectedNestedClasses, classFiles.size());
    }

    private static int countRootArchiveClasses(Path jar) throws Exception {
        int count = 0;
        try (ZipFile zipFile = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry == null || entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (!isBuildClassEntry(name) || isNestedLibEntry(name)) {
                    continue;
                }
                count++;
            }
        }
        return count;
    }

    private static int countNestedArchiveClasses(Path jar) throws Exception {
        int count = 0;
        try (ZipFile zipFile = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry == null || entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (!name.endsWith(".jar") || !isNestedLibEntry(name)) {
                    continue;
                }
                try (InputStream inputStream = zipFile.getInputStream(entry);
                     JarInputStream nestedJar = new JarInputStream(inputStream)) {
                    JarEntry nestedEntry;
                    while ((nestedEntry = nestedJar.getNextJarEntry()) != null) {
                        if (!nestedEntry.isDirectory() && isBuildClassEntry(nestedEntry.getName())) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    private static boolean isNestedLibEntry(String entryName) {
        if (entryName == null) {
            return false;
        }
        String normalized = entryName.replace('\\', '/');
        if (normalized.startsWith("BOOT-INF/lib/") || normalized.startsWith("WEB-INF/lib/")) {
            return true;
        }
        if (normalized.startsWith("lib/") || normalized.contains("/lib/")) {
            return true;
        }
        return normalized.startsWith("META-INF/lib/");
    }

    private static boolean isBuildClassEntry(String entryName) {
        if (entryName == null || !entryName.endsWith(".class")) {
            return false;
        }
        String normalized = entryName.strip().replace('\\', '/');
        if (normalized.endsWith("/module-info.class")) {
            return false;
        }
        return !"module-info.class".equalsIgnoreCase(normalized);
    }
}
