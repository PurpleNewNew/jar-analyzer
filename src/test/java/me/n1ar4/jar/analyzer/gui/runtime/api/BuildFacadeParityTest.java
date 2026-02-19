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
                            BuildSettingsDto.MODE_ARTIFACT,
                            "/tmp/input.jar",
                            "",
                            "/tmp/rt.jar",
                            true,
                            true,
                            false,
                            true,
                            fixClassPath,
                            fixMethodImpl,
                            quickMode
                    );
                    RuntimeFacades.build().apply(settings);
                    BuildSettingsDto snapshot = RuntimeFacades.build().snapshot().settings();
                    assertEquals("/tmp/input.jar", snapshot.activeInputPath());
                    assertEquals("/tmp/rt.jar", snapshot.sdkPath());
                    assertEquals(true, snapshot.resolveNestedJars());
                    assertEquals(true, snapshot.includeSdk());
                    assertEquals(true, snapshot.deleteTempBeforeBuild());
                    assertEquals(fixClassPath, snapshot.fixClassPath());
                    assertEquals(fixMethodImpl, snapshot.fixMethodImpl());
                    assertEquals(quickMode, snapshot.quickMode());
                }
            }
        }
    }
}
