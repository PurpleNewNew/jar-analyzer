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

import me.n1ar4.jar.analyzer.config.ConfigFile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiStartupOptionsTest {
    @Test
    void defaultsWhenConfigMissing() {
        GuiStartupOptions options = GuiStartupOptions.fromConfig(null);
        assertEquals("0.0.0.0", options.apiBind());
        assertEquals(10032, options.apiPort());
        assertFalse(options.apiAuthEnabled());
        assertEquals("JAR-ANALYZER-API-TOKEN", options.apiToken());
    }

    @Test
    void normalizeInvalidConfigValues() {
        ConfigFile cfg = new ConfigFile();
        cfg.setApiBind("   ");
        cfg.setApiPort(70000);
        cfg.setApiAuth(true);
        cfg.setApiToken("   ");

        GuiStartupOptions options = GuiStartupOptions.fromConfig(cfg);
        assertEquals("0.0.0.0", options.apiBind());
        assertEquals(10032, options.apiPort());
        assertTrue(options.apiAuthEnabled());
        assertEquals("JAR-ANALYZER-API-TOKEN", options.apiToken());
    }

    @Test
    void keepConfiguredValues() {
        ConfigFile cfg = new ConfigFile();
        cfg.setApiBind("127.0.0.1");
        cfg.setApiPort(18080);
        cfg.setApiAuth(true);
        cfg.setApiToken("token-1");

        GuiStartupOptions options = GuiStartupOptions.fromConfig(cfg);
        assertEquals("127.0.0.1", options.apiBind());
        assertEquals(18080, options.apiPort());
        assertTrue(options.apiAuthEnabled());
        assertEquals("token-1", options.apiToken());
    }
}
