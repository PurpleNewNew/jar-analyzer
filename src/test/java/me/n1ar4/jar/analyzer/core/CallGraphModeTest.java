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

import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CallGraphModeTest {
    private static final String PROP = "jar.analyzer.callgraph.mode";

    @Test
    public void shouldRespectChaModeProperty() {
        String old = System.getProperty(PROP);
        try {
            System.setProperty(PROP, "cha");
            Path jar = FixtureJars.callbackTestJar();
            WorkspaceContext.setResolveInnerJars(false);
            CoreRunner.BuildResult result = CoreRunner.run(jar, null, false, false, true, null, true);
            assertEquals("cha", result.getCallGraphMode());
        } finally {
            restoreProperty(old);
        }
    }

    @Test
    public void shouldRespectPtaModeProperty() {
        String old = System.getProperty(PROP);
        try {
            System.setProperty(PROP, "pta");
            Path jar = FixtureJars.callbackTestJar();
            WorkspaceContext.setResolveInnerJars(false);
            CoreRunner.BuildResult result = CoreRunner.run(jar, null, false, false, true, null, true);
            assertEquals("pta", result.getCallGraphMode());
        } finally {
            restoreProperty(old);
        }
    }

    @Test
    public void shouldDefaultToRtaMode() {
        String old = System.getProperty(PROP);
        try {
            System.clearProperty(PROP);
            Path jar = FixtureJars.callbackTestJar();
            WorkspaceContext.setResolveInnerJars(false);
            CoreRunner.BuildResult result = CoreRunner.run(jar, null, false, false, true, null, true);
            assertEquals("rta", result.getCallGraphMode());
        } finally {
            restoreProperty(old);
        }
    }

    private static void restoreProperty(String old) {
        if (old == null) {
            System.clearProperty(PROP);
        } else {
            System.setProperty(PROP, old);
        }
    }
}
