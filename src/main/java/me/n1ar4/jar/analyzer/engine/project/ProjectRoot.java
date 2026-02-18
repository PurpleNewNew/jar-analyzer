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

import java.nio.file.Path;
import java.util.Objects;

/**
 * Root descriptor used by all workspace-aware pipelines.
 */
public record ProjectRoot(
        ProjectRootKind kind,
        ProjectOrigin origin,
        Path path,
        String presentableName,
        boolean archive,
        boolean test,
        int priority
) {
    public ProjectRoot {
        kind = kind == null ? ProjectRootKind.CONTENT_ROOT : kind;
        origin = origin == null ? kind.defaultOrigin() : origin;
        path = normalizePath(path);
        String name = presentableName == null ? "" : presentableName.trim();
        if (name.isEmpty()) {
            Path fileName = path.getFileName();
            name = fileName == null ? path.toString() : fileName.toString();
        }
        presentableName = name;
    }

    private static Path normalizePath(Path path) {
        Objects.requireNonNull(path, "path");
        try {
            return path.toAbsolutePath().normalize();
        } catch (Exception ex) {
            return path.normalize();
        }
    }
}
