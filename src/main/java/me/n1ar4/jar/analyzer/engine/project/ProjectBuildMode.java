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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Build entry mode.
 */
public enum ProjectBuildMode {
    ARTIFACT("artifact"),
    PROJECT("project");

    private static final Map<String, ProjectBuildMode> LOOKUP = buildLookup();

    private final String value;

    ProjectBuildMode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ProjectBuildMode fromValue(String raw) {
        if (raw == null) {
            return ARTIFACT;
        }
        ProjectBuildMode mode = LOOKUP.get(raw.trim().toLowerCase(Locale.ROOT));
        return mode == null ? ARTIFACT : mode;
    }

    private static Map<String, ProjectBuildMode> buildLookup() {
        Map<String, ProjectBuildMode> out = new HashMap<>();
        for (ProjectBuildMode mode : values()) {
            out.put(mode.value, mode);
            out.put(mode.name().toLowerCase(Locale.ROOT), mode);
        }
        return out;
    }
}
