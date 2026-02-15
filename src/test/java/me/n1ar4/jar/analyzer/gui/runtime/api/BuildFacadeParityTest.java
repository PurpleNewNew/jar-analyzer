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

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildFacadeParityTest {
    @Test
    void applyShouldRoundTripSettings() {
        boolean[] bools = new boolean[]{false, true};
        for (boolean fixClassPath : bools) {
            for (boolean fixMethodImpl : bools) {
                for (boolean quickMode : bools) {
                    BuildSettingsDto settings = new BuildSettingsDto(
                            "/tmp/input.jar",
                            "/tmp/rt.jar",
                            true,
                            false,
                            true,
                            true,
                            fixClassPath,
                            fixMethodImpl,
                            quickMode
                    );
                    RuntimeFacades.build().apply(settings);
                    BuildSettingsDto snapshot = RuntimeFacades.build().snapshot().settings();
                    assertEquals("/tmp/input.jar", snapshot.inputPath());
                    assertEquals("/tmp/rt.jar", snapshot.runtimePath());
                    assertEquals(true, snapshot.resolveNestedJars());
                    assertEquals(true, snapshot.addRuntimeJar());
                    assertEquals(true, snapshot.deleteTempBeforeBuild());
                    assertEquals(fixClassPath, snapshot.fixClassPath());
                    assertEquals(fixMethodImpl, snapshot.fixMethodImpl());
                    assertEquals(quickMode, snapshot.quickMode());
                }
            }
        }
    }
}
