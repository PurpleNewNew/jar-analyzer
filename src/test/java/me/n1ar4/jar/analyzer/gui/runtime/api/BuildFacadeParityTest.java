/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSettingsDto;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BuildFacadeParityTest {
    @Test
    void applyShouldRoundTripSettings() {
        boolean[] bools = new boolean[]{false, true};
        for (boolean fixClassPath : bools) {
            for (boolean fixMethodImpl : bools) {
                for (boolean quickMode : bools) {
                    BuildSettingsDto settings = new BuildSettingsDto(
                            "p-test",
                            BuildSettingsDto.INPUT_FILE,
                            "/tmp/input.jar",
                            "",
                            "rt-default",
                            true,
                            true,
                            fixClassPath,
                            fixMethodImpl,
                            quickMode
                    );
                    RuntimeFacades.build().apply(settings);
                    BuildSettingsDto snapshot = RuntimeFacades.build().snapshot().settings();
                    String expectedInput = Paths.get("/tmp/input.jar").toAbsolutePath().normalize().toString();
                    assertFalse(snapshot.projectId().isBlank());
                    assertEquals(BuildSettingsDto.INPUT_FILE, snapshot.buildInputMode());
                    assertEquals(expectedInput, snapshot.activeInputPath());
                    assertEquals(expectedInput, snapshot.artifactPath());
                    assertEquals("", snapshot.projectPath());
                    assertFalse(snapshot.runtimeProfileId().isBlank());
                    assertEquals(true, snapshot.resolveNestedJars());
                    assertEquals(true, snapshot.deleteTempBeforeBuild());
                    assertEquals(fixClassPath, snapshot.fixClassPath());
                    assertEquals(fixMethodImpl, snapshot.fixMethodImpl());
                    assertEquals(quickMode, snapshot.quickMode());
                }
            }
        }
    }
}
