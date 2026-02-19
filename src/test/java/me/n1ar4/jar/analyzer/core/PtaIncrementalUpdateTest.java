/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.core.pta.ContextSensitivePtaEngine;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PtaIncrementalUpdateTest {
    private static final String MODE_PROP = "jar.analyzer.callgraph.mode";
    private static final String INCREMENTAL_PROP = "jar.analyzer.pta.incremental";

    @Test
    public void shouldReuseIncrementalStateAcrossBuilds() {
        String oldMode = System.getProperty(MODE_PROP);
        String oldIncremental = System.getProperty(INCREMENTAL_PROP);
        try {
            ContextSensitivePtaEngine.clearIncrementalCache();
            System.setProperty(MODE_PROP, "pta");
            System.setProperty(INCREMENTAL_PROP, "true");
            Path jar = FixtureJars.callbackTestJar();
            WorkspaceContext.updateResolveInnerJars(false);

            CoreRunner.BuildResult first = CoreRunner.run(jar, null, false, false, true, null, true);
            CoreRunner.BuildResult second = CoreRunner.run(jar, null, false, false, true, null, true);

            assertTrue(first.getPtaContextMethodsProcessed() > 0, "first run should process context methods");
            assertTrue(second.getPtaContextMethodsProcessed() > 0, "second run should still process context methods");
            assertTrue(second.getPtaIncrementalSkips() > first.getPtaIncrementalSkips(),
                    "second run should skip more unchanged units");
        } finally {
            ContextSensitivePtaEngine.clearIncrementalCache();
            restoreProperty(MODE_PROP, oldMode);
            restoreProperty(INCREMENTAL_PROP, oldIncremental);
        }
    }

    private static void restoreProperty(String key, String oldValue) {
        if (oldValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, oldValue);
        }
    }
}
