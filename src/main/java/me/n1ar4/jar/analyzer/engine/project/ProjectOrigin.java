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
 * Unified semantic origin used by build/tree/search.
 */
public enum ProjectOrigin {
    APP("app"),
    LIBRARY("library"),
    SDK("sdk"),
    GENERATED("generated"),
    EXCLUDED("excluded"),
    UNKNOWN("unknown");

    private static final Map<String, ProjectOrigin> LOOKUP = buildLookup();

    private final String value;

    ProjectOrigin(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ProjectOrigin fromValue(String raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        ProjectOrigin origin = LOOKUP.get(raw.trim().toLowerCase(Locale.ROOT));
        return origin == null ? UNKNOWN : origin;
    }

    private static Map<String, ProjectOrigin> buildLookup() {
        Map<String, ProjectOrigin> out = new HashMap<>();
        for (ProjectOrigin origin : values()) {
            out.put(origin.value, origin);
            out.put(origin.name().toLowerCase(Locale.ROOT), origin);
        }
        return out;
    }
}
