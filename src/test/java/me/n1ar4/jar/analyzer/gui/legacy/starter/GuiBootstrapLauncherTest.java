/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.legacy.starter;

import me.n1ar4.jar.analyzer.cli.StartCmd;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.runtime.GuiLauncher;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class GuiBootstrapLauncherTest {
    @Test
    void resolveLauncherReturnsFirstNonNull() {
        MainForm.setFrame(null);
        GuiLauncher first = new NoopLauncher("first");
        GuiLauncher second = new NoopLauncher("second");
        GuiLauncher resolved = GuiBootstrap.resolveLauncher(Arrays.asList(null, first, second));
        assertSame(first, resolved);
        assertNull(MainForm.getFrame());
    }

    @Test
    void resolveLauncherReturnsNullWhenMissing() {
        MainForm.setFrame(null);
        GuiLauncher resolved = GuiBootstrap.resolveLauncher(Collections.emptyList());
        assertNull(resolved);
        assertNull(MainForm.getFrame());
    }

    private static final class NoopLauncher implements GuiLauncher {
        @SuppressWarnings("unused")
        private final String name;

        private NoopLauncher(String name) {
            this.name = name;
        }

        @Override
        public void launch(StartCmd startCmd) {
            // noop
        }
    }
}
