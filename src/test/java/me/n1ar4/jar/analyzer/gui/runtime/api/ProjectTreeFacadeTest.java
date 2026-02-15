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

import me.n1ar4.jar.analyzer.engine.EngineContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectTreeFacadeTest {
    @Test
    void snapshotShouldBeEmptyWhenEngineMissing() {
        EngineContext.setEngine(null);
        assertTrue(RuntimeFacades.projectTree().snapshot().isEmpty());
        assertTrue(RuntimeFacades.projectTree().search("abc").isEmpty());
    }
}
