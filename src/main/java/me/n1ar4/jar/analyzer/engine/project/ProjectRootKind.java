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
 * Unified root kinds aligned with project model semantics.
 */
public enum ProjectRootKind {
    CONTENT_ROOT("content-root", ProjectOrigin.APP),
    SOURCE_ROOT("source-root", ProjectOrigin.APP),
    RESOURCE_ROOT("resource-root", ProjectOrigin.APP),
    LIBRARY("library", ProjectOrigin.LIBRARY),
    SDK("sdk", ProjectOrigin.SDK),
    GENERATED("generated", ProjectOrigin.GENERATED),
    EXCLUDED("excluded", ProjectOrigin.EXCLUDED);

    private static final Map<String, ProjectRootKind> LOOKUP = buildLookup();

    private final String value;
    private final ProjectOrigin defaultOrigin;

    ProjectRootKind(String value, ProjectOrigin defaultOrigin) {
        this.value = value;
        this.defaultOrigin = defaultOrigin;
    }

    public String value() {
        return value;
    }

    public ProjectOrigin defaultOrigin() {
        return defaultOrigin;
    }

    public static ProjectRootKind fromValue(String raw) {
        if (raw == null) {
            return CONTENT_ROOT;
        }
        ProjectRootKind kind = LOOKUP.get(raw.trim().toLowerCase(Locale.ROOT));
        return kind == null ? CONTENT_ROOT : kind;
    }

    private static Map<String, ProjectRootKind> buildLookup() {
        Map<String, ProjectRootKind> out = new HashMap<>();
        for (ProjectRootKind kind : values()) {
            out.put(kind.value, kind);
            out.put(kind.name().toLowerCase(Locale.ROOT), kind);
        }
        return out;
    }
}
