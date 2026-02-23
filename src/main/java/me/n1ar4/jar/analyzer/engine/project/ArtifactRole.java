/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.engine.project;

import java.util.Locale;

public enum ArtifactRole {
    INPUT,
    SOURCE,
    RESOURCE,
    DEPENDENCY;

    public static ArtifactRole fromValue(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        try {
            return ArtifactRole.valueOf(value);
        } catch (Exception ignored) {
            return DEPENDENCY;
        }
    }
}
