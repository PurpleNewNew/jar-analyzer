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

public record ArtifactEntry(
        ArtifactRole role,
        ArtifactIndexPolicy indexPolicy,
        ProjectOrigin origin,
        String displayName,
        String locator,
        Path path
) {
    public ArtifactEntry {
        role = role == null ? ArtifactRole.DEPENDENCY : role;
        indexPolicy = indexPolicy == null ? ArtifactIndexPolicy.DISPLAY_ONLY : indexPolicy;
        origin = origin == null ? ProjectOrigin.UNKNOWN : origin;
        displayName = normalize(displayName);
        locator = normalize(locator);
        path = normalizePath(path);
        if (displayName.isBlank()) {
            if (path != null && path.getFileName() != null) {
                displayName = path.getFileName().toString();
            } else if (!locator.isBlank()) {
                displayName = locator;
            } else {
                displayName = role.name().toLowerCase();
            }
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static Path normalizePath(Path value) {
        if (value == null) {
            return null;
        }
        try {
            return value.toAbsolutePath().normalize();
        } catch (Exception ex) {
            return Objects.requireNonNull(value).normalize();
        }
    }
}
