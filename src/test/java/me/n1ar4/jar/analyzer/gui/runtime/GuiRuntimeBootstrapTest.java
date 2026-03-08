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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiRuntimeBootstrapTest {
    private static final Path ERROR_LOG = Path.of("JAR-ANALYZER-ERROR.txt").toAbsolutePath().normalize();

    @AfterEach
    void cleanup() throws Exception {
        Files.deleteIfExists(ERROR_LOG);
    }

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

    @Test
    void runtimeExceptionHandlerShouldLogEdtArrayIndexOutOfBounds() throws Exception {
        Files.deleteIfExists(ERROR_LOG);
        AtomicReference<Thread> edtRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> edtRef.set(Thread.currentThread()));

        Class<?> handlerClass = Class.forName(
                "me.n1ar4.jar.analyzer.gui.runtime.GuiRuntimeBootstrap$RuntimeExceptionHandler");
        var constructor = handlerClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Thread.UncaughtExceptionHandler handler =
                (Thread.UncaughtExceptionHandler) constructor.newInstance();

        Thread edt = edtRef.get();
        assertNotNull(edt);

        handler.uncaughtException(edt, new ArrayIndexOutOfBoundsException("boom"));

        assertTrue(Files.exists(ERROR_LOG));
        String content = Files.readString(ERROR_LOG, StandardCharsets.UTF_8);
        assertTrue(content.contains("ArrayIndexOutOfBoundsException"));
        assertTrue(content.contains("boom"));
    }

    private static final class NoopLauncher implements GuiLauncher {
        @SuppressWarnings("unused")
        private final String name;

        private NoopLauncher(String name) {
            this.name = name;
        }

        @Override
        public void launch() {
            // noop
        }
    }
}
