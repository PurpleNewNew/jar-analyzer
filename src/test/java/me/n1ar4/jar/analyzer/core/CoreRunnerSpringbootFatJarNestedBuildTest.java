package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreRunnerSpringbootFatJarNestedBuildTest {
    @AfterEach
    void tearDown() {
        ProjectRuntimeContext.clear();
    }

    @Test
    void shouldBuildSpringbootFatJarWithNestedLibsWithoutCallgraphStall() throws Exception {
        Path jar = FixtureJars.springbootTestJar().toAbsolutePath().normalize();
        int expectedClassFiles = countRootArchiveClasses(jar) + countNestedArchiveClasses(jar);
        List<Integer> progress = new ArrayList<>();

        CoreRunner.BuildResult result = CoreRunner.run(
                jar,
                null,
                false,
                false,
                progress::add,
                true
        );

        assertTrue(result.getBuildSeq() > 0L);
        assertTrue(result.getJarCount() > 1);
        assertEquals(1, result.getTargetJarCount());
        assertTrue(result.getLibraryJarCount() >= 28);
        assertEquals(expectedClassFiles, result.getClassFileCount());
        assertNotNull(result.getStageMetric("callgraph"));
        assertNotNull(result.getStageMetric("neo4j_commit"));
        assertTrue(result.getBuildWallMs() < 120_000L,
                "springboot nested build took too long: " + result.getBuildWallMs() + " ms");
        assertTrue(result.getPeakHeapUsedBytes() < 4L * 1024L * 1024L * 1024L,
                "springboot nested build peak heap too high: " + result.getPeakHeapUsedBytes());
        assertTrue(progress.contains(55), "progress never entered callgraph stage: " + progress);
        assertTrue(progress.stream().anyMatch(value -> value >= 68),
                "progress never advanced through callgraph sub stages: " + progress);
        assertEquals(100, progress.get(progress.size() - 1));
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
