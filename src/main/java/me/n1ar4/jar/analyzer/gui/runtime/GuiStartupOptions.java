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

public record GuiStartupOptions(
        String apiBind,
        int apiPort,
        boolean apiAuthEnabled,
        String apiToken
) {
    private static final String DEFAULT_BIND = "0.0.0.0";
    private static final int DEFAULT_PORT = 10032;
    private static final String DEFAULT_TOKEN = "JAR-ANALYZER-API-TOKEN";

    public static GuiStartupOptions defaults() {
        return new GuiStartupOptions(DEFAULT_BIND, DEFAULT_PORT, false, DEFAULT_TOKEN);
    }

    public static GuiStartupOptions fromConfig(ConfigFile config) {
        if (config == null) {
            return defaults();
        }
        String bind = normalizeBind(config.getApiBind());
        int port = normalizePort(config.getApiPort(), DEFAULT_PORT);
        String token = normalizeToken(config.getApiToken());
        return new GuiStartupOptions(bind, port, config.isApiAuth(), token);
    }

    private static String normalizeBind(String value) {
        if (value == null) {
            return DEFAULT_BIND;
        }
        String v = value.trim();
        return v.isEmpty() ? DEFAULT_BIND : v;
    }

    private static String normalizeToken(String value) {
        if (value == null) {
            return DEFAULT_TOKEN;
        }
        String v = value.trim();
        return v.isEmpty() ? DEFAULT_TOKEN : v;
    }

    private static int normalizePort(int value, int fallback) {
        if (value < 1 || value > 65535) {
            return fallback;
        }
        return value;
    }
}
