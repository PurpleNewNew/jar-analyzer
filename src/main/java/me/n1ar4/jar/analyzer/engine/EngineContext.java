/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.engine;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Global CoreEngine holder decoupled from GUI classes.
 * <p>
 * Historically many modules referenced {@code MainForm.getEngine()}, which tightly coupled
 * server/engine code to Swing UI. This holder provides the same capability without that dependency.
 */
public final class EngineContext {
    private static final AtomicReference<CoreEngine> ENGINE = new AtomicReference<>();

    private EngineContext() {
    }

    public static CoreEngine getEngine() {
        return ENGINE.get();
    }

    public static void setEngine(CoreEngine engine) {
        ENGINE.set(engine);
    }
}

