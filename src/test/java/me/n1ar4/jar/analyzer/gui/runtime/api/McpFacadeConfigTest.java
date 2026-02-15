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

import me.n1ar4.jar.analyzer.gui.runtime.model.McpConfigDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpFacadeConfigTest {
    @Test
    void currentConfigShouldBeReadable() {
        McpConfigDto cfg = RuntimeFacades.apiMcp().currentConfig();
        assertNotNull(cfg);
        assertNotNull(cfg.bind());
        assertNotNull(cfg.token());
        assertNotNull(cfg.lines());
        assertFalse(cfg.lines().isEmpty());
        assertTrue(cfg.lines().size() >= 6);
    }
}
