/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.runtime;

import me.n1ar4.jar.analyzer.cli.StartCmd;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class GuiRuntimeBootstrapTest {
    @Test
    void resolveLauncherReturnsFirstNonNull() {
        GuiLauncher first = new NoopLauncher("first");
        GuiLauncher second = new NoopLauncher("second");
        GuiLauncher resolved = GuiRuntimeBootstrap.resolveLauncher(Arrays.asList(null, first, second));
        assertSame(first, resolved);
    }

    @Test
    void resolveLauncherReturnsNullWhenMissing() {
        GuiLauncher resolved = GuiRuntimeBootstrap.resolveLauncher(Collections.emptyList());
        assertNull(resolved);
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
