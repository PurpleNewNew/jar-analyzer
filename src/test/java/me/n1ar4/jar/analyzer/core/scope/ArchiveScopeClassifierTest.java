package me.n1ar4.jar.analyzer.core.scope;

import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchiveScopeClassifierTest {

    @Test
    void shouldPrioritizeCommonLibraryOverAppHeuristic() throws Exception {
        Path root = Files.createTempDirectory("ja-scope-root");
        Path appJar = root.resolve("biz-app.jar");
        Path commonJar = root.resolve("commons-lang3-3.15.0.jar");
        Files.createFile(appJar);
        Files.createFile(commonJar);

        ArchiveScopeClassifier.ScopeSummary summary = ArchiveScopeClassifier.classifyArchives(
                List.of(appJar, commonJar),
                root,
                null
        );

        assertEquals(ProjectOrigin.APP, summary.originsByArchive().get(appJar.toAbsolutePath().normalize()));
        assertEquals(ProjectOrigin.LIBRARY, summary.originsByArchive().get(commonJar.toAbsolutePath().normalize()));
        assertEquals(1, summary.targetArchiveCount());
        assertEquals(1, summary.libraryArchiveCount());
    }

    @Test
    void shouldHonorForceTargetJarPrefixFirst() throws Exception {
        List<String> backup = new ArrayList<>(AnalysisScopeRules.getForceTargetJarPrefixes());
        try {
            AnalysisScopeRules.saveForceTargetJarPrefixes(List.of("spring-"));
            Path root = Files.createTempDirectory("ja-scope-force");
            Path jar = root.resolve("spring-core-test.jar");
            Files.createFile(jar);

            ArchiveScopeClassifier.ScopeSummary summary = ArchiveScopeClassifier.classifyArchives(
                    List.of(jar),
                    root,
                    null
            );

            assertEquals(ProjectOrigin.APP, summary.originsByArchive().get(jar.toAbsolutePath().normalize()));
            assertEquals(1, summary.targetArchiveCount());
        } finally {
            AnalysisScopeRules.saveForceTargetJarPrefixes(backup);
        }
    }
}
