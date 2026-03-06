/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.sca;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScaScanServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void resolveJarListShouldFilterOutNonRuntimeArchives() throws Exception {
        Path appJar = Files.createFile(tempDir.resolve("app.jar"));
        Files.createFile(tempDir.resolve("app-sources.jar"));
        Files.createFile(tempDir.resolve("app-javadoc.jar"));
        Files.createFile(tempDir.resolve("app-test.jar"));
        Files.createFile(tempDir.resolve("demo.war"));
        Files.createFile(tempDir.resolve("readme.txt"));

        List<String> jars = ScaScanService.resolveJarList(tempDir.toString());

        assertEquals(2, jars.size());
        assertTrue(jars.contains(appJar.toAbsolutePath().toString()));
        assertTrue(jars.contains(tempDir.resolve("demo.war").toAbsolutePath().toString()));
    }

    @Test
    void enabledRulesShouldOnlyIncludeSelectedKinds() {
        EnumSet<ScaScanService.RuleKind> enabled = ScaScanService.enabledRules(true, false, true);

        assertEquals(EnumSet.of(ScaScanService.RuleKind.LOG4J, ScaScanService.RuleKind.SHIRO), enabled);
    }
}
