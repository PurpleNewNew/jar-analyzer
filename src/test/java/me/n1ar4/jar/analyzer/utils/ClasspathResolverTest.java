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

import me.n1ar4.jar.analyzer.engine.project.ProjectBuildMode;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.jar.analyzer.engine.project.ProjectRoot;
import me.n1ar4.jar.analyzer.engine.project.ProjectRootKind;
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
    @TempDir
    Path tempDir;

    @Test
    void resolveUserArchivesShouldRecurseDirectoryInput() throws Exception {
        Path inputDir = tempDir.resolve("input");
        Path deepClass = writeClassFile(inputDir, "a/b/c/d/e/f/g/h/Deep.class");

        List<Path> archives = ClasspathResolver.resolveUserArchives(inputDir.toString(), false);

        assertTrue(archives.contains(deepClass));
    }

    @Test
    void resolveClasspathGraphShouldRecurseDirectoryInput() throws Exception {
        Path inputDir = tempDir.resolve("input");
        Path deepClass = writeClassFile(inputDir, "a/b/c/d/e/f/g/h/Deep.class");

        ClasspathResolver.ClasspathGraph graph = ClasspathResolver.resolveClasspathGraph(inputDir, false);

        assertTrue(graph.getOrderedArchives().contains(deepClass));
    }

    @Test
    void resolveClasspathGraphShouldKeepDefaultDependencyDepthLimit() throws Exception {
        Path rootJar = createJar(tempDir.resolve("root.jar"), "mid-1.jar");
        Path mid1 = createJar(tempDir.resolve("mid-1.jar"), "mid-2.jar");
        createJar(tempDir.resolve("mid-2.jar"), "mid-3.jar");
        createJar(tempDir.resolve("mid-3.jar"), "mid-4.jar");
        createJar(tempDir.resolve("mid-4.jar"), "mid-5.jar");
        createJar(tempDir.resolve("mid-5.jar"), "mid-6.jar");
        Path mid6 = createJar(tempDir.resolve("mid-6.jar"), "leaf.jar");
        Path leafJar = createJar(tempDir.resolve("leaf.jar"));

        ClasspathResolver.ClasspathGraph graph = ClasspathResolver.resolveClasspathGraph(rootJar, false);

        assertTrue(graph.getOrderedArchives().contains(mid1));
        assertTrue(graph.getOrderedArchives().contains(mid6));
        assertFalse(graph.getOrderedArchives().contains(leafJar));
    }

    @Test
    void resolveInputArchivesShouldIncludeProjectLibraryRootsWithoutSystemProperty() throws Exception {
        Path inputDir = Files.createDirectories(tempDir.resolve("input"));
        Path libraryJar = createJar(tempDir.resolve("external-lib.jar"));
        ProjectModel model = ProjectModel.builder()
                .buildMode(ProjectBuildMode.PROJECT)
                .primaryInputPath(inputDir)
                .addRoot(new ProjectRoot(
                        ProjectRootKind.CONTENT_ROOT,
                        ProjectOrigin.APP,
                        inputDir,
                        "",
                        false,
                        false,
                        10
                ))
                .addRoot(new ProjectRoot(
                        ProjectRootKind.LIBRARY,
                        ProjectOrigin.LIBRARY,
                        libraryJar,
                        "",
                        true,
                        false,
                        20
                ))
                .build();

        List<String> archives = ClasspathResolver.resolveInputArchives(
                inputDir,
                null,
                true,
                false,
                ClasspathResolver.resolveProjectLibraryRoots(model)
        );

        assertTrue(archives.contains(libraryJar.toString()));
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
