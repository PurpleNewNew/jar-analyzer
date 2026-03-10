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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathResolverTest {
    private static final String SCAN_DEPTH_PROP = "jar.analyzer.classpath.scanDepth";

    @TempDir
    Path tempDir;

    @AfterEach
    void cleanup() {
        System.clearProperty(SCAN_DEPTH_PROP);
    }

    @Test
    void resolveUserArchivesShouldRecurseDirectoryInputRegardlessOfScanDepth() throws Exception {
        System.setProperty(SCAN_DEPTH_PROP, "1");
        Path inputDir = tempDir.resolve("input");
        Path deepClass = writeClassFile(inputDir, "a/b/c/d/e/f/g/h/Deep.class");

        List<Path> archives = ClasspathResolver.resolveUserArchives(inputDir.toString(), false);

        assertTrue(archives.contains(deepClass));
    }

    @Test
    void resolveClasspathGraphShouldRecurseDirectoryInputRegardlessOfScanDepth() throws Exception {
        System.setProperty(SCAN_DEPTH_PROP, "1");
        Path inputDir = tempDir.resolve("input");
        Path deepClass = writeClassFile(inputDir, "a/b/c/d/e/f/g/h/Deep.class");

        ClasspathResolver.ClasspathGraph graph = ClasspathResolver.resolveClasspathGraph(inputDir, false);

        assertTrue(graph.getOrderedArchives().contains(deepClass));
    }

    @Test
    void resolveClasspathGraphShouldKeepDependencyDepthLimit() throws Exception {
        Path rootJar = createJar(tempDir.resolve("root.jar"), "mid.jar");
        createJar(tempDir.resolve("mid.jar"), "leaf.jar");
        Path leafJar = createJar(tempDir.resolve("leaf.jar"));

        System.setProperty(SCAN_DEPTH_PROP, "1");
        ClasspathResolver.ClasspathGraph shallowGraph = ClasspathResolver.resolveClasspathGraph(rootJar, false);
        assertFalse(shallowGraph.getOrderedArchives().contains(leafJar));

        System.setProperty(SCAN_DEPTH_PROP, "2");
        ClasspathResolver.ClasspathGraph deepGraph = ClasspathResolver.resolveClasspathGraph(rootJar, false);
        assertTrue(deepGraph.getOrderedArchives().contains(leafJar));
    }

    private Path writeClassFile(Path root, String relativePath) throws Exception {
        Path file = root.resolve(relativePath).toAbsolutePath().normalize();
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[]{0x1});
        return file;
    }

    private Path createJar(Path jarPath, String... manifestEntries) throws Exception {
        Path normalized = jarPath.toAbsolutePath().normalize();
        Files.createDirectories(normalized.getParent());
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (manifestEntries != null && manifestEntries.length > 0) {
            attributes.put(Attributes.Name.CLASS_PATH, String.join(" ", manifestEntries));
        }
        try (OutputStream out = Files.newOutputStream(normalized);
             JarOutputStream jar = new JarOutputStream(out, manifest)) {
            // Manifest-only jar is enough for classpath expansion tests.
        }
        return normalized;
    }
}
